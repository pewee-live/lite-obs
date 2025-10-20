package com.pewee.service;

import com.pewee.bean.File;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 文件Service接口
 * 
 * @author pewee
 * @date 2022-04-13
 */
public interface IFileService  extends com.pewee.service.IService<File> {
	

	/**
     * 查询文件列表
     * 
     * @param file 文件
     * @return 文件集合
     */
    public List<File> selectFileList(File file);
    
    /**
     * 根据code批量修改文件
     * 
     * @param fileList 文件 List
     * @return 结果
     */
    public int updateFileListSelectiveByCode(List<File> fileList);  
    
    /**
     * 批量删除文件
     * 
     * @param ids 需要删除的文件ID
     * @return 结果
     */
    public int deleteFileByIds(Long[] ids);

    /**
     * 根据file的code查询file信息
     * @param code
     * @return
     */
    File selectFileByCode(String code);

    /**
     * 根据code和存储类型获取文件流
     * @param code
     * @param storageType
     * @return
     * @throws IOException
     */
	public InputStream getSingleStream(String code, int storageType) throws IOException;

    /**
     * 通过LogicFile的code(File表中的batchCode) 查找所有相关的File
     * @param batchCode LogicFile的code
     * @return
     */
    List<File> selectFilesByBatchCode(String batchCode);
    
    /**
     * 通过File的code 查找所有相关的File
     * @param codes 所有要查询的codes
     * @return
     */
    List<File> selectFilesCodes(List<String> codes);
}
