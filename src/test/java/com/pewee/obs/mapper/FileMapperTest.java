package com.pewee.obs.mapper;

import com.pewee.bean.File;
import com.pewee.mapper.FileMapper;

import redis.clients.jedis.JedisCluster;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Classname FileMapperTest
 * @Description TODO
 * @Version 1.0.0
 * @Date 2022/4/20 11:44
 * @Created by Mr.GongRan
 */
@SpringBootTest
public class FileMapperTest {
    @Autowired
    FileMapper fileMapper;
    @Autowired
    JedisCluster jedisCluster;

    @Test
    public void testSelectFileListByBatchCodes() throws IOException {
        List<String> batchCodes = new ArrayList<>();
        batchCodes.add("546397312373493760");
        batchCodes.add("546400840210976768");
        batchCodes.add("546401633525829632");
        List<File> files = fileMapper.selectFileListByBatchCodes(batchCodes);
        System.out.println(files);
        //System.in.read();
    }
    
    @Test
    public void pringAccessToken() {
    	String string = jedisCluster.get("com.pewee:obs:bd:refresh");
    	System.out.println(string);
    }
}
