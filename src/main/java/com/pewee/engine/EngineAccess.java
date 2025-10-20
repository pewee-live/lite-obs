package com.pewee.engine;

import com.pewee.bean.File;
import com.pewee.bean.LogicFile;
import com.pewee.bean.dto.DownloadFileDto;
import com.pewee.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 用于存储的访问
 * @author pewee
 *
 */
public interface EngineAccess {
	
	
	/**
	 * 保存字节流
	 * @param eontext
	 * @return
	 */
	public String save(FileContext eontext); 
	
	/**
	 * 通过token获取下载链接
	 * @param logicFile
	 * @param flies
	 * @return
	 */
	public List<DownloadFileDto> getDownloadUrls(LogicFile logicFile,List<File> flies);
	
	/**
	 * 通过token删除文件
	 * @param file
	 * @return
	 */
	public int deleteFile(File file);

	/**
	 * 通过File的集合去删除文件
	 * 返回值代表删除的文件数量,如果失败会抛异常ServiceException
	 * @param files
	 * @return
	 */
	public int deleteFiles(List<File> files);
	
	/**
	 * 通过code获取文件的下载流,这个code可以使动态链接也可以是静态的
	 * @param codes
	 * @return
	 * @throws IOException 
	 */
	public List<InputStream> getStreamDynamic(List<String> codes) throws IOException;
	
	/**
	 * 通过File list获取文件的下载流
	 * @param fs 文件列表
	 * @return
	 * @throws IOException 
	 */
	public List<InputStream> doGetStream(List<File> fs) throws IOException;
	
	/**
	 * 获取存储引擎默认的命名空间
	 * 如果该引擎没有多实例,不需要考虑这个方法的实现,直接返回""
	 * @return 获取当前存储引擎的namespace
	 */
	default String defaultNamespace() {
		return StringUtils.EMPTY;
	}
	
	/**
	 * 通过上传者的SysCode获取存储引擎的命名空间.若没找到就获取默认Namespace(调用引擎实现的defaultNamespace)
	 * 在engineDefinition中统一通过账号配置来切换对应的该引擎下的存储实例
	 * 如果该引擎实现中没有配置多实例/不支持多实例;或者支持多实例的引擎实现下,用户请求的该账号没有通过配置文件指定对应的实例时.都将会调用defaultNamespace()
	 * @return 获取当前SysCode对应存储引擎的namespace
	 */
	default String getNamespaceBySysCodeOrDefault(String sysCode) {
		return StringUtils.EMPTY;
	}
}
