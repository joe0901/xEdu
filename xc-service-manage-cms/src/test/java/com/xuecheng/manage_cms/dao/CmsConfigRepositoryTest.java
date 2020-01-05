package com.xuecheng.manage_cms.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;


/**
 * @author Administrator
 * @version 1.0@SpringBootTest
 * @create 2018-09-12 18:11
 **/
@RunWith(SpringRunner.class)
public class CmsConfigRepositoryTest {

    @Autowired
    RestTemplate restTemplate;

    @Test
    public void testRestGet() {
        ResponseEntity<Map> forEntity = restTemplate.getForEntity("http://127.0.0.1:31001/cms/config/get/5a791725dd573c3574ee333f", Map.class);
        Map body = forEntity.getBody();
        System.out.println(body);

    }

}