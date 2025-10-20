package com.pewee.service;

import com.pewee.bean.LogicFile;
import com.pewee.bean.dto.DownloadLogicFileRespDto;
import com.pewee.bean.dto.MultiStreamDto;
import com.pewee.bean.dto.SingleStreamDto;
import com.pewee.engine.DynamicLink;

import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 逻辑文件Service接口
 * 
 * @author pewee
 * @date 2022-04-13
 */
public interface ILogicFileService  extends com.pewee.service.IService<LogicFile> {
	
	/**
     * 查询逻辑文件
     * 
     * @param code code
     * @return 逻辑文件
     */
    public LogicFile selectLogicFileByCode(@Param("code") String code);

	/**
     * 查询逻辑文件列表
     * 
     * @param logicFile 逻辑文件
     * @return 逻辑文件集合
     */
    public List<LogicFile> selectLogicFileList(LogicFile logicFile);
    
    /**
     * 根据code批量修改逻辑文件
     * 
     * @param logicFileList 逻辑文件 List
     * @return 结果
     */
    public int updateLogicFileListSelectiveByCode(List<LogicFile> logicFileList);  
    
    /**
     * 批量删除逻辑文件
     * 
     * @param codes 需要删除的逻辑文件code的集合
     * @return 结果
     */
    public int deleteLogicFileByCodes(List<String> codes);

    /**
     * 獲取文件的下載鏈接
     * @param logicFile
     * @param forceStaticLink 是否强制生成静态链接
     * @return
     */
	public DownloadLogicFileRespDto getDownloadInfo(LogicFile logicFile, Boolean forceStaticLink);
	
	/**
	 * 上傳
	 * @param logicFile
	 * @param file
	 * @param seq 只有在非快速上传流程时才需要传
	 * @param crc32 只有在非快速上传流程时才需要传
	 * @return 
	 */
	public String upload(LogicFile logicFile, MultipartFile file,Integer seq,String crc32);

	/**
	 * 获取某个动态/静态链接的逻辑文件对应的全部分片的输入流
	 * @param code
	 * @param dynamicLink 
	 */
	@Deprecated
	public MultiStreamDto getMultiStream(String code, Boolean dynamicLink);
    



    LogicFile preupload(LogicFile logicFile,List<String> subCrcList);

	/**
	 * 通过 逻辑文件code集合,找到其中已经分片上传完成的,将逻辑文件状态改为已上传
	 * @param logicFileCodes 逻辑文件 code集合
	 */
	void settingStatusToUploadedByCodes(List<String> logicFileCodes);

	void deleteDelayLogicFile(List<String> logicFileCodes);

	/**
	 * 逻辑文件预上传的时候,缓存其切片crc
	 * @param logicFileCode
	 * @param subCrcList
	 * @return
	 */
	boolean cacheSplitCrc(String logicFileCode,List<String> subCrcList);

	/**
	 * 通过逻辑文件code 清除 该逻辑文件预上传时缓存的切片crc
	 * @param logicFileCode 逻辑文件code
	 */
	boolean clearSplitCrcCache(String logicFileCode);
	
	/**
	 * 通过逻辑文件code 获取 该逻辑文件预上传时缓存的切片crc
	 * @param logicFileCode 逻辑文件code
	 * @return
	 */
	public List<String> getSplitCrcCache(String logicFileCode);
	
	/**
	 * 获取某个分片的流
	 * @param singleStreamDto
	 */
	public void getSingleStream(SingleStreamDto singleStreamDto);
	
	/**
	 * 获取merge文件下载信息
	 * @param code
	 * @param dynamicLink
	 * @return
	 */
	public SingleStreamDto getMergeFileInfo(String code, Boolean dynamicLink);

	public DynamicLink getDynamicLink();
}
