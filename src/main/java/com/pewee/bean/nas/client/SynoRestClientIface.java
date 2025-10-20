package com.pewee.bean.nas.client;

import com.pewee.bean.nas.api.genericresponses.Data;
import com.pewee.bean.nas.api.genericresponses.NasResponse;
import com.pewee.bean.nas.api.helper.SynologyAPIVersions;
import com.pewee.bean.nas.client.exception.NasAuthenticationFailureException;
import com.pewee.bean.nas.client.exception.NasCompatibilityException;
import com.pewee.bean.nas.client.exception.NasResponseException;

import java.util.List;


public interface SynoRestClientIface {
    void init();

    String getVersion();

    void checkAPICompatibility(SynologyAPIVersions apiName) throws NasCompatibilityException;

    void authenticate(String login, String password) throws NasAuthenticationFailureException, NasCompatibilityException;

    Data listShares() throws NasResponseException, NasCompatibilityException;

    Data getFolder(String path) throws NasResponseException, NasCompatibilityException;

    void logout() throws NasAuthenticationFailureException, NasCompatibilityException;

    boolean createFolder(String foldername, String parentPath) throws NasResponseException, NasCompatibilityException;

    NasResponse uploadFile(String filename, String filePath, byte[] arr) throws NasCompatibilityException;

    /**
     * 传入待删除的文件在nas上的文件路径的集合
     * @param files
     * @return
     * @throws NasCompatibilityException
     */
    NasResponse deleteFile(List<String> files) throws NasCompatibilityException;

    /**
     * 传入要下载的文件路径(在Nas上的路径),获取这些文件的共享url(顺序一一对应)
     * @return
     * @throws NasCompatibilityException
     */
    List<String> createSharingUrls(List<String> filePaths) throws NasCompatibilityException;

    byte[] downFile(String path) throws NasCompatibilityException;
}
