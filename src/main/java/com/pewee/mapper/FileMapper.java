package com.pewee.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.pewee.bean.File;

/**
 * 文件Mapper接口
 * 
 * @author pewee
 * @date 2022-04-13
 */
public interface FileMapper extends com.pewee.util.MyMapper<File>
{
    /**
     * 查询文件
     * 
     * @param id 文件ID
     * @return 文件
     */
    public File selectFileById(Long id);
    
    /**
     * 查询文件
     * 
     * @param code code
     * @return 文件
     */
    public File selectFileByCode(@Param("code") String code);

    /**
     * 查询文件列表
     * 
     * @param file 文件
     * @return 文件集合
     */
    public List<File> selectFileList(File file);

    /**
     * 新增文件
     * 
     * @param file 文件
     * @return 结果
     */
    public int insertFile(File file);
    
    /**
     * 批量新增文件
     * 
     * @param List<file> 文件 List
     * @return 结果
     */
    public int insertFileListSelective(List<File> fileList);

    /**
     * 根据主键修改文件
     * 
     * @param file 文件
     * @return 结果
     */
    public int updateFile(File file);
    
    /**
     * 根据code批量修改文件
     * 
     * @param List<file> 文件 List
     * @return 结果
     */
    public int updateFileListSelectiveByCode(List<File> fileList);

    /**
     * 删除文件
     * 
     * @param id 文件ID
     * @return 结果
     */
    public int deleteFileById(Long id);

    /**
     * 批量删除文件
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteFileByIds(Long[] ids);

    List<File> selectFileListByBatchCodes(@Param("batchCodes") List<String> batchCodes);
}
