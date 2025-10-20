package com.pewee.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.pewee.bean.LogicFile;

/**
 * 逻辑文件Mapper接口
 * 
 * @author pewee
 * @date 2022-04-13
 */
public interface LogicFileMapper extends com.pewee.util.MyMapper<LogicFile>
{
    /**
     * 查询逻辑文件
     * 
     * @param id 逻辑文件ID
     * @return 逻辑文件
     */
    public LogicFile selectLogicFileById(Long id);
    
    /**
     * 查询逻辑文件
     * 
     * @param code code
     * @return 逻辑文件
     */
    public LogicFile selectLogicFileByCode(@Param("code") String code);

    /**
     * 通过codes查询逻辑文件列表
     *
     * @param codes 逻辑文件cdoe集合
     * @return 逻辑文件集合
     */
    public List<LogicFile> selectLogicFileListByCodes(@Param("codes") List<String> codes);

    /**
     * 查询逻辑文件列表
     * 
     * @param logicFile 逻辑文件
     * @return 逻辑文件集合
     */
    public List<LogicFile> selectLogicFileList(LogicFile logicFile);

    /**
     * 新增逻辑文件
     * 
     * @param logicFile 逻辑文件
     * @return 结果
     */
    public int insertLogicFile(LogicFile logicFile);
    
    /**
     * 批量新增逻辑文件
     * 
     * @param List<logicFile> 逻辑文件 List
     * @return 结果
     */
    public int insertLogicFileListSelective(List<LogicFile> logicFileList);

    /**
     * 根据主键修改逻辑文件
     * 
     * @param logicFile 逻辑文件
     * @return 结果
     */
    public int updateLogicFile(LogicFile logicFile);
    
    /**
     * 根据code批量修改逻辑文件
     * 
     * @param List<logicFile> 逻辑文件 List
     * @return 结果
     */
    public int updateLogicFileListSelectiveByCode(List<LogicFile> logicFileList);

    /**
     * 删除逻辑文件
     * 
     * @param id 逻辑文件ID
     * @return 结果
     */
    public int deleteLogicFileById(Long id);

    /**
     * 批量删除逻辑文件
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLogicFileByIds(Long[] ids);

    /**
     * 批量逻辑删除 逻辑文件
     * @param codes
     * @return
     */
    public int logicallyDeleteLogicFileByCodes(@Param("codes") List<String> codes);
}
