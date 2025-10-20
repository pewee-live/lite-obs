package com.pewee.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.pewee.bean.Sys;

/**
 * 接入系统注册Mapper接口
 * 
 * @author pewee
 * @date 2022-04-13
 */
public interface SysMapper extends com.pewee.util.MyMapper<Sys>
{
    /**
     * 查询接入系统注册
     * 
     * @param id 接入系统注册ID
     * @return 接入系统注册
     */
    public Sys selectSysById(Long id);
    
    /**
     * 查询接入系统注册
     * 
     * @param code code
     * @return 接入系统注册
     */
    public Sys selectSysByCode(@Param("code") String code);

    /**
     * 查询接入系统注册列表
     * 
     * @param sys 接入系统注册
     * @return 接入系统注册集合
     */
    public List<Sys> selectSysList(Sys sys);

    /**
     * 新增接入系统注册
     * 
     * @param sys 接入系统注册
     * @return 结果
     */
    public int insertSys(Sys sys);
    
    /**
     * 批量新增接入系统注册
     * 
     * @param List<sys> 接入系统注册 List
     * @return 结果
     */
    public int insertSysListSelective(List<Sys> sysList);

    /**
     * 根据主键修改接入系统注册
     * 
     * @param sys 接入系统注册
     * @return 结果
     */
    public int updateSys(Sys sys);
    
    /**
     * 根据code批量修改接入系统注册
     * 
     * @param List<sys> 接入系统注册 List
     * @return 结果
     */
    public int updateSysListSelectiveByCode(List<Sys> sysList);

    /**
     * 删除接入系统注册
     * 
     * @param id 接入系统注册ID
     * @return 结果
     */
    public int deleteSysById(Long id);

    /**
     * 批量删除接入系统注册
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteSysByIds(Long[] ids);

    Sys selectSysByCodeAndSecret(@Param("code")String code,@Param("secret") String secret);
}
