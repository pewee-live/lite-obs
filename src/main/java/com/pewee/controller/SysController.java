package com.pewee.controller;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pewee.bean.Sys;
import com.pewee.bean.dto.SysDto;
import com.pewee.bean.vo.SysVo;
import com.pewee.service.ISysService;
import com.pewee.service.impl.TokenService;
import com.pewee.service.schedule.ScheduleService;
import com.pewee.util.RedisLock;
import com.pewee.util.StringUtils;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.RespEntity;
import com.pewee.util.resp.ServiceException;

/**
 * 接入系统注册Controller
 *
 * @author pewee
 * @date 2022-04-13
 */
@RestController
@RequestMapping("/obs/sys")
public class SysController {

    private static final Logger log = LoggerFactory.getLogger(SysController.class);
   

    @Autowired
    private ISysService sysService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RedisLock redisLock;

    /**
     * 查询接入系统注册列表
     */
    @GetMapping("/list")
    public RespEntity<List<Sys>> list(Sys sys) {
        List<Sys> list = sysService.selectSysList(sys);
        return new RespEntity<List<Sys>>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(list);
    }


    /**
     * 获取接入系统注册详细信息
     */
    @GetMapping(value = "/{id}")
    public RespEntity<Sys> getInfo(@PathVariable("id") Long id) {
        return new RespEntity<Sys>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(sysService.selectByKey(id));
    }

    /**
     * 删除接入系统注册
     */
    @DeleteMapping("/{ids}")
    public RespEntity<Integer> remove(@PathVariable Long[] ids) {
        return new RespEntity<Integer>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(sysService.deleteSysByIds(ids));
    }

    @PostMapping("/register")
    public RespEntity<SysVo> register(@RequestBody @Valid SysDto sysDto, HttpServletRequest request) {
        String remoteIp = request.getRemoteAddr();
        if (isAuthenticationByGateway(request)  || isInternalIp(remoteIp)) {
            //如果是网关验证过  或者是 内网IP,那么直接注册即可
            Sys sys = sysService.insertSysDto(sysDto);
            SysVo sysVo = new SysVo();
            BeanUtils.copyProperties(sys, sysVo);
            return new RespEntity<SysVo>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(sysVo);
        } else {
           // 如果是OPEN API的方式 需要验证(具体未定,有可能是在请求头中添加给予它的秘钥加密过的约定信息,然后OBS进行非对称解密)
            throw new ServiceException(CommonRespInfo.ILLEGAL_TOKEN);
        }
    }
    
    /**
     * 获取token
     * 当同一个账号密码的配置在多个系统时,会导致互相争抢导致刚生成的token失效;
     * 加入锁机制,生成token时先看缓存,如果ttl很长,就直接返回当前redis的toekn
     * @param sysCode
     * @param secretKey
     * @param grandType
     * @return
     */
    @PostMapping("/authorize")
    public RespEntity<String> token(@RequestParam("sysCode")  String sysCode,@RequestParam("secretKey")  String secretKey,
    		@RequestParam(name = "grandType",defaultValue="client_credentials")  String grandType) {
    	Assert.notNull(sysCode, "sysCode不能为空");
    	Assert.notNull(secretKey, "secretKey不能为空");
    	log.info("OBS登录-开始,sysCode:{},secretKey:{}",sysCode,secretKey);
    	Sys sys = checkSystem(sysCode,secretKey);
    	checkGrandType(sys,grandType);
    	//校验通过可以先去直接获取,若获取不到则去刷新
    	String token = tokenService.checkTtlAndGetToken(sysCode);
    	if (StringUtils.isNotBlank(token)) {
    		log.info("登录-完成,token:{}",token);
    		return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(token);
    	}
    	//获取锁
    	String requestId = UUID.randomUUID().toString();
    	if (redisLock.tryLock(sysCode, requestId, 5000,false)) {
    		//再去拿一次token,因为在等待锁的时候可能其他线程已经更新了
    		try {
    			token = tokenService.checkTtlAndGetToken(sysCode);
    			if (StringUtils.isNotBlank(token)) {
            		log.info("登录-完成,token:{}",token);
            		return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(token);
            	}
            	//还是没拿到那就去刷新token
            	token = tokenService.generateAccessToken(sys);
    		} finally {
    			redisLock.releaseLock(sysCode, requestId, 5000);
    		}
        	log.info("登录-完成,token:{}",token);
        	return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(token);
    	} else {
    		log.info("OBS登录-超时!");
        	return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.LOCK_REQUEST_TIMEOUT);
    	}
    }

	/**
     * 检查系统给与的授权类型
     * 现在我们的系统只有1种类型: client_credentials
     * @param sys
     * @param grandType
     */
    private void checkGrandType(Sys sys, String grandType) {
		if (!"client_credentials".equals(grandType)) {
			throw new ServiceException(CommonRespInfo.USER_UN_AUTH, null);
		}
	}

    /**
     * 检查系统类型
     * @param sysCode
     * @param secretKey
     * @return
     */
	private Sys checkSystem(String sysCode, String secretKey) {
    	Sys sys = null;
    	try {
    		sys = sysService.selectSysByCodeAndSecret(sysCode, secretKey);
        } catch (Exception e) {
            log.error("ObsAccessInterceptor 通过sysCode和secretKey查询注册过的系统错误 ", e);
            throw new ServiceException(CommonRespInfo.SYS_CODE_KEY_INVALID, null);
        }
        if (sys == null) {
        	 throw new ServiceException(CommonRespInfo.SYS_CODE_KEY_INVALID, null);
        }
        return sys;
	}


	/**
     * 注册系统是否被网关验证过
     * todo
     *
     * @param request
     * @return
     */
    private boolean isAuthenticationByGateway(HttpServletRequest request) {
        return false;
    }

    /**
     * 判断一个ip地址是否是
     *
     * @param ip
     * @return
     */
    private boolean isInternalIp(String ip) {
        if (ip == null) {
            return false;
        }
        final String localIp = "127.0.0.1";
        if (ip.contains(localIp)) {
            return true;
        }
        byte[] addr = convertNumericFormatV4(ip);
        byte addr0 = addr[0];
        byte addr1 = addr[1];
        //10开头内网网段
        final byte SECTION_10 = 0x0A;
        //172.16 - 172.31网段
        final byte SECTION_172 = (byte) 0xAC;
        final byte SECTION_16 = (byte) 0x10;
        final byte SECTION_31 = (byte) 0x1F;
        //192.168网段
        final byte SECTION_192 = (byte) 0xC0;
        final byte SECTION_168 = (byte) 0xA8;
        switch (addr0) {
            case SECTION_10:
                return true;
            case SECTION_172:
                if (addr1 >= SECTION_16 && addr1 <= SECTION_31) {
                    return true;
                } else {
                    return false;
                }
            case SECTION_192:
                return addr1 == SECTION_168;
            default:
                return false;
        }
    }

    /**
     * 将ip字符串转为byte数组
     * @param ip
     * @return
     */
    private byte[] convertNumericFormatV4(String ip){
        byte[] var1 = new byte[4];
        long var2 = 0L;
        int var4 = 0;
        int var5 = ip.length();
        if (var5 != 0 && var5 <= 15) {
            for(int var6 = 0; var6 < var5; ++var6) {
                char var7 = ip.charAt(var6);
                if (var7 == '.') {
                    if (var2 < 0L || var2 > 255L || var4 == 3) {
                        return null;
                    }

                    var1[var4++] = (byte)((int)(var2 & 255L));
                    var2 = 0L;
                } else {
                    int var8 = Character.digit(var7, 10);
                    if (var8 < 0) {
                        return null;
                    }

                    var2 *= 10L;
                    var2 += (long)var8;
                }
            }

            if (var2 >= 0L && var2 < 1L << (4 - var4) * 8) {
                switch(var4) {
                    case 0:
                        var1[0] = (byte)((int)(var2 >> 24 & 255L));
                    case 1:
                        var1[1] = (byte)((int)(var2 >> 16 & 255L));
                    case 2:
                        var1[2] = (byte)((int)(var2 >> 8 & 255L));
                    case 3:
                        var1[3] = (byte)((int)(var2 >> 0 & 255L));
                    default:
                        return var1;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
