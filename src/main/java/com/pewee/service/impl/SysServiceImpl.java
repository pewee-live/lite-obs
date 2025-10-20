package com.pewee.service.impl;

import com.pewee.bean.Sys;
import com.pewee.bean.dto.SysDto;
import com.pewee.mapper.SysMapper;
import com.pewee.service.ISysService;
import com.pewee.util.DistributedAtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 接入系统注册Service业务层处理
 *
 * @author pewee
 * @date 2022-04-13
 */
@Service
@Transactional
public class SysServiceImpl implements ISysService {

    private static final Logger log = LoggerFactory.getLogger(SysServiceImpl.class);

    private static final String SYS_CODE_PATTERN = "system_no_%s";

    private static final String SYS_CODE_REGISTRY_KEY = "com.pewee:obs:sys:register";

    @Autowired
    private SysMapper sysMapper;

    @Autowired
    private DistributedAtomicInteger distributedAtomicInteger;


    @Override
    public Sys selectByKey(Object key) {
        return sysMapper.selectByPrimaryKey(key);
    }

    @Override
    public int save(Sys entity) {
        return sysMapper.insertSelective(entity);
    }

    @Override
    public int delete(Object key) {
        return sysMapper.deleteByPrimaryKey(key);
    }

    @Override
    public int updateAll(Sys entity) {
        return sysMapper.updateByPrimaryKey(entity);
    }

    @Override
    public int updateNotNull(Sys entity) {
        return sysMapper.updateByPrimaryKeySelective(entity);
    }

    @Override
    public List<Sys> selectByExample(Object example) {
        return sysMapper.selectByExample(example);
    }

    @Override
    public int batchInsert(List<Sys> entities) {
        return sysMapper.insertSysListSelective(entities);
    }

    @Override
    public List<Sys> selectAll() {
        return sysMapper.selectAll();
    }

    @Override
    public int batchInsertSelective(List<Sys> entities) {
        return sysMapper.insertSysListSelective(entities);
    }

    /**
     * 根据code批量修改接入系统注册
     *
     * @param sysList 接入系统注册 List
     * @return 结果
     */
    @Override
    public int updateSysListSelectiveByCode(List<Sys> sysList) {
        return sysMapper.updateSysListSelectiveByCode(sysList);
    }

    /**
     * 查询接入系统注册列表
     *
     * @param sys 接入系统注册
     * @return 接入系统注册集合
     */
    @Override
    public List<Sys> selectSysList(Sys sys) {
        return sysMapper.selectSysList(sys);
    }

    /**
     * 批量删除接入系统注册
     *
     * @param ids 需要删除的接入系统注册ID
     * @return 结果
     */
    @Override
    public int deleteSysByIds(Long[] ids) {
        return sysMapper.deleteSysByIds(ids);
    }

    /**
     * 业务系统注册,如下信息需要自动生成:
     * code-系统编码
     * createTime-当前时间(获取系统时间)
     * updateTime-更新时间(获取系统时间)
     * enabled-是否可用(1)
     * secret-系统秘钥
     *
     * @param sysDto
     * @return
     */
    @Override
    public Sys insertSysDto(SysDto sysDto) {
        Sys sys = new Sys();
        BeanUtils.copyProperties(sysDto, sys);
        Date now = new Date();
        String secretKey = generateSecretKey();
        int generateSysNo = distributedAtomicInteger.incrAndGetWithoutExpire(SYS_CODE_REGISTRY_KEY);
        String seq = StringUtils.leftPad("" + generateSysNo, 5, "0");
        String code = String.format(SYS_CODE_PATTERN, seq);
        sys.setCode(code)
                .setCreateTime(now)
                .setUpdateTime(now)
                .setEnabled(1)
                .setSecret(secretKey);
        save(sys);
        return sys;
    }

    @Override
    public Sys selectSysByCodeAndSecret(String code, String secret) {
         Sys sys = sysMapper.selectSysByCodeAndSecret(code,secret);
        return sys;
    }

    @Override
    public Sys selectSysByCode(String code) {
        return  sysMapper.selectSysByCode(code);
    }

    /**
     * 生成系统秘钥的方法
     * 使用UUID生成的字符串
     * 一般情况下,不会有重复(单机环境),同时,如果是多机器部署,存在重复秘钥也不会对业务造成影响(该秘钥仅用来验证当前系统是否注册)
     *
     * @return
     */
    private String generateSecretKey() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
