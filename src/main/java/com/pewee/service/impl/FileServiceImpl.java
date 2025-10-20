package com.pewee.service.impl;

import com.google.common.collect.Lists;
import com.pewee.bean.File;
import com.pewee.bean.LogicFile;
import com.pewee.bean.SingleUploadFileContext;
import com.pewee.engine.EngineAccess;
import com.pewee.engine.EngineRegistor;
import com.pewee.engine.FileContext;
import com.pewee.mapper.FileMapper;
import com.pewee.service.IFileService;
import com.pewee.util.GenerateCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件Service业务层处理
 *
 * @author pewee
 * @date 2022-04-13
 */
@Service
@Transactional
public class FileServiceImpl implements IFileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private EngineRegistor engineRegistor;    

    @Override
    public File selectByKey(Object key) {
        return fileMapper.selectByPrimaryKey(key);
    }

    @Override
    public int save(File entity) {
        return fileMapper.insertSelective(entity);
    }

    @Override
    public int delete(Object key) {
        return fileMapper.deleteByPrimaryKey(key);
    }

    @Override
    public int updateAll(File entity) {
        return fileMapper.updateByPrimaryKey(entity);
    }

    @Override
    public int updateNotNull(File entity) {
        return fileMapper.updateByPrimaryKeySelective(entity);
    }

    @Override
    public List<File> selectByExample(Object example) {
        return fileMapper.selectByExample(example);
    }

    @Override
    public int batchInsert(List<File> entities) {
        return fileMapper.insertFileListSelective(entities);
    }

    @Override
    public List<File> selectAll() {
        return fileMapper.selectAll();
    }

    @Override
    public int batchInsertSelective(List<File> entities) {
        return fileMapper.insertFileListSelective(entities);
    }

    /**
     * 根据code批量修改文件
     *
     * @param fileList 文件 List
     * @return 结果
     */
    @Override
    public int updateFileListSelectiveByCode(List<File> fileList) {
        return fileMapper.updateFileListSelectiveByCode(fileList);
    }

    /**
     * 查询文件列表
     *
     * @param file 文件
     * @return 文件集合
     */
    @Override
    public List<File> selectFileList(File file) {
        return fileMapper.selectFileList(file);
    }

    /**
     * 批量删除文件
     *
     * @param ids 需要删除的文件ID
     * @return 结果
     */
    @Override
    public int deleteFileByIds(Long[] ids) {
        return fileMapper.deleteFileByIds(ids);
    }

    @Override
    public File selectFileByCode(String code) {
        Assert.hasText(code,"待查询文件的code不能为空");
        return fileMapper.selectFileByCode(code);
    }

    @Override
    public List<File> selectFilesByBatchCode(String batchCode) {
        Example example = new Example(File.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("batchCode", batchCode);
        return selectByExample(example);
    }

    /**
     * 上傳文件
     *
     * @param engine
     * @param singleFile
     * @param file
     * @return 
     */
    @Transactional
    public String upload(EngineAccess engine, SingleUploadFileContext singleFile, byte[] file) {
        File f = new File();
        f.setCode(GenerateCodeUtils.nextId());
        f.setBatchCode(singleFile.getBatchCode());
        f.setSysCode(singleFile.getSysCode());
        f.setLength(singleFile.getLength());
        f.setCrc32(singleFile.getCrc32());
        f.setSequence(singleFile.getSeq());
        f.setCreateTime(new Date());
        int storageType = engineRegistor.getType(engine);
        f.setStorageType(storageType);
        f.setEngineNamespace(engine.getNamespaceBySysCodeOrDefault(singleFile.getSysCode()));
        f.setToken(engine.save( FileContext.builder().now( f.getCreateTime()).fileName(singleFile.getFileName()).split(singleFile.getSplit()).sysCode(singleFile.getSysCode()).content(file).engineNamespace(f.getEngineNamespace()).build()));
		fileMapper.insertSelective(f);
		return f.getCode();
    }

    /**
     * 当前的删除逻辑还是同步的,即不会去远程异步调用删除
     *
     * @param logicFiles
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public void deleteFileByLogicFiles(List<LogicFile> logicFiles) {
        //根据logicFiles 找打所有关联的File信息
        List<String> batchCodes = logicFiles.stream().map(LogicFile::getCode).distinct().collect(Collectors.toList());
        List<File> files = fileMapper.selectFileListByBatchCodes(batchCodes);
        Map<String, List<File>> grByBatch = files.stream().collect(Collectors.groupingBy(File::getBatchCode));
        grByBatch.forEach((k, b) -> {
            if (b.size() > 0) {
                Integer storageType = b.stream().map(File::getStorageType).filter(Objects::nonNull).findFirst().orElseThrow(()-> new IllegalArgumentException("未找到存储类型!"));
                engineRegistor.switchTo(storageType).deleteFiles(b);
            }
        });
        List<Long> idList = files.stream().map(File::getId).collect(Collectors.toList());
        if (idList.size() > 0){
            Long[] idArray = new Long[idList.size()];
            idList.toArray(idArray);
            fileMapper.deleteFileByIds(idArray);
        }
        //当前是后台线程调用,不用考虑flag为0(异常)的情况,flag为0的话(异常或者调用远程服务器操作失败),会打日志
    }

	@Override
	public InputStream getSingleStream(String code, int storageType) throws IOException {
		return engineRegistor.switchTo(storageType).getStreamDynamic(Lists.newArrayList(code)).get(0);
	}

	public List<File> selectByLogicFileCode(String batchCode) {
		Example example = new Example(File.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("batchCode",batchCode);
        return selectByExample(example);
	}

	@Override
	public List<File> selectFilesCodes(List<String> codes) {
		Example example = new Example(File.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("code", codes);
        return selectByExample(example);
	}
}
