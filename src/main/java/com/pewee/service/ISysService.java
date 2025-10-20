package com.pewee.service;

import com.pewee.bean.Sys;
import com.pewee.bean.dto.SysDto;

import java.util.List;

/**
 * 接入系统注册Service接口
 * 
 * @author pewee
 * @date 2022-04-13
 */
public interface ISysService  extends com.pewee.service.IService<Sys> {

	/**
     * 查询接入系统注册列表
     * 
     * @param sys 接入系统注册
     * @return 接入系统注册集合
     */
    public List<Sys> selectSysList(Sys sys);
    
    /**
     * 根据code批量修改接入系统注册
     * 
     * @param sysList 接入系统注册 List
     * @return 结果
     */
    public int updateSysListSelectiveByCode(List<Sys> sysList);  
    
    /**
     * 批量删除接入系统注册
     * 
     * @param ids 需要删除的接入系统注册ID
     * @return 结果
     */
    public int deleteSysByIds(Long[] ids);

    /**
     * 注册接入系统
     * @param sysDto
     * @return
     */
    public Sys insertSysDto(SysDto sysDto);

    public Sys selectSysByCodeAndSecret(String code,String secret);

    public Sys selectSysByCode(String code);
}
