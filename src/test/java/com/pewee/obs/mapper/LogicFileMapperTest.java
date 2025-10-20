package com.pewee.obs.mapper;

import com.pewee.bean.LogicFile;
import com.pewee.mapper.LogicFileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Classname LogicFileMapperTest
 * @Description TODO
 * @Version 1.0.0
 * @Date 2022/4/19 16:31
 * @Created by Mr.GongRan
 */
@SpringBootTest
public class LogicFileMapperTest {
    @Autowired
    private LogicFileMapper logicFileMapper;

    @Test
    public void testLogicallyDelete(){
        List<String> codes = new ArrayList<>();
        codes.add("547797022192463872");
        codes.add("547831280948768768");
        logicFileMapper.logicallyDeleteLogicFileByCodes(codes);
    }

    @Test
    public void testSelectLogicFileListByCodes() throws IOException, InterruptedException {
        List<String> codes = new ArrayList<>();
        codes.add("547797022192463872");
        codes.add("547831280948768768");
        List<LogicFile> logicFiles = logicFileMapper.selectLogicFileListByCodes(codes);
        for (LogicFile logicFile : logicFiles) {
            System.out.println(logicFile);
        }
        //Thread.sleep(20*1000);
    }
}
