package com.pewee.engine.meta;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import com.alibaba.fastjson.JSON;
import com.pewee.bean.File;
import com.pewee.bean.LogicFile;
import com.pewee.bean.dto.DownloadFileDto;
import com.pewee.config.EngineNameSpaceConfig;
import com.pewee.config.EngineNameSpaceConfig.EngineNamespace;
import com.pewee.engine.DynamicLink;
import com.pewee.engine.EngineAccess;
import com.pewee.engine.FileContext;
import com.pewee.engine.EngineInfoEnum;
import com.pewee.service.IFileService;
import com.pewee.util.GenerateCodeUtils;
import com.pewee.util.StringUtils;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 引擎定义
 * @author pewee
 *
 */
@Getter
@Setter
@Component
@Slf4j
public abstract class EngineDefinition implements EngineAccess{
	
	@Autowired
	private DynamicLink dynamicLink;
	
	@Value("${obs.ip}")
    private String ip;

    @Value("${server.port}")
    private int port;
	
	private String name;
	
	private Integer type;
	
	@Autowired
    private IFileService fileService;
	
	@Autowired
	private EngineNameSpaceConfig engineNameSpaceConfig;
	
	/**
	 * 存放 sysCode -> EngineNamespace 的map
	 */
	private static final Map<String,EngineNamespace> namespaceBySysCodeCache = new HashMap<>();
	
	protected void linkDefinition(){
		this.setName(getDefinition().getName());
		this.setType(getDefinition().getType());
	}
	
	public void init() throws Exception {
		linkDefinition();
		load();
		loginAndAutoRefreshSession();
	}
	
	/**
	 * 做动态链接转换为静态链接
	 * 且对该token的下载次数减1
	 */
	@Override
	public List<InputStream> getStreamDynamic(List<String> codes) throws IOException {
		
		List<String> transformCodes = codes.stream().map( code ->  dynamicLink.transformCode(code)).collect(Collectors.toList());
		List<File> files;
		try {
			files = fileService.selectFilesCodes(transformCodes);
		} catch (Exception e) {
			log.error("nas下载文件... 根据codes查询Files信息失败", e);
            throw new ServiceException(CommonRespInfo.SYS_ERROR);
		}
		Assert.isTrue(files.size() == codes.size(),"送入的fileCode有误!部分code非法!");
		files.sort( new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {
				return o1.getSequence().compareTo(o2.getSequence());
			}
		});
		List<InputStream> doGetStream = doGetStream(files);
		codes.forEach(c -> dynamicLink.countDown(c) );
		return doGetStream;
	}
	
	/**
	 * 根据文件生成其唯一token,这个token也是文件存入网盘的名字
	 * @param context
	 * @return
	 */
	protected String getFileToken(FileContext context) {
		StringBuilder builder = new StringBuilder("");
		//文件不分片,才回去使用其文件名 只留25位防止文件名过长
		if (0 == context.getSplit()) {
			String nextId = GenerateCodeUtils.nextId();
			if (StringUtils.isNotBlank(context.getFileName())) {
				//防止重名文件
				builder.append(nextId);
				String fileName = context.getFileName();
				if (fileName.length() > 30) {
					builder.append("-" + fileName.substring(fileName.length()-30, fileName.length()));
				} else {
					builder.append("-" + fileName );
				}
			} else {
				builder.append(nextId);
			}
		} else {
			builder.append( context.getFileName() );
		}
		return builder.toString();
	}
	
	/**
	 * 初始化资源
	 * @throws Exception 
	 */
	abstract public void load() throws Exception;
	/**
	 * 登錄刷新
	 */
	abstract protected void loginAndAutoRefreshSession();
	/**
	 * 返回存儲類型
	 * @return
	 */
	abstract public EngineInfoEnum getDefinition();
	
	/**
	 * 返回下载链接,新版本更新为生成统一的下载链接,不再由各个引擎生成!!
	 * 生成规则: 
	 */
	@Override
	public List<DownloadFileDto> getDownloadUrls(LogicFile logicFile, List<File> flies) {
		return flies.stream().map( f ->{
			DownloadFileDto dto = new DownloadFileDto();
			BeanUtils.copyProperties(f, dto);
			//生成info信息
	        String logicFileJson = JSON.toJSONString(logicFile);
	        //将logicFileJson通过base64进行编码
	        StringBuilder sb = new StringBuilder("");
	        String info = Base64Utils.encodeToUrlSafeString(logicFileJson.getBytes(java.nio.charset.Charset.forName("UTF-8")));
	        sb.append("http://" + ip +  ":" + getPort() + "/obs/logicfile/download/part/" 
	        + getDynamicLink().generateDynamicLink(f.getCode()) + "?");
	        if(0 == logicFile.getSplit()) {
	        	sb.append("info=" + info);
	        	sb.append("&");
			}
	        sb.append("sequence=" + f.getSequence());
	        sb.append("&dynamicLink=" + dynamicLink.isDynamicLink());
	        sb.append("&length=" + f.getLength());
	        sb.append("&storageType=" + f.getStorageType());
	        dto.setUrl(sb.toString());
			return dto;
		} ).collect(Collectors.toList());
	}


	@Override
	public String getNamespaceBySysCodeOrDefault(String sysCode) {
		if (namespaceBySysCodeCache.containsKey(sysCode)) {
			return namespaceBySysCodeCache.get(sysCode).getNamespace();
		} else {
			List<EngineNamespace> list = engineNameSpaceConfig.getNamespaces().stream().filter( c -> c.getSysCode().equals(sysCode) ).collect(Collectors.toList());
			if (!list.isEmpty()) {
				EngineNamespace engineNamespace = list.get(0);
				namespaceBySysCodeCache.put(sysCode, engineNamespace);
				return engineNamespace.getNamespace();
			}
		}
		return defaultNamespace();
	}
	
}
