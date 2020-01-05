package com.xuecheng.manage_cms.dao;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@RunWith(SpringRunner.class)
public class Test2 {
    @Autowired
    CmsPageRepository cmsPageRepository;

    @Test
    public void test1() {
        List<CmsPage> list = cmsPageRepository.findAll();
        System.out.println(list);
    }

    @Test
    public void test2() {
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<CmsPage> ist = cmsPageRepository.findAll(pageable);
        System.out.println(ist);
    }

    //分页条件查询
    @Test
    public void test5() {
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        CmsPage cmsPage = new CmsPage();
//        cmsPage.setSiteId("5a751fab6abb5044e0d19ea1");
        cmsPage.setPageAliase("轮播");

        ExampleMatcher matcher = ExampleMatcher.matching();
        matcher = matcher.withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
        Example<CmsPage> sExample = Example.of(cmsPage, matcher);
        Page<CmsPage> ist = cmsPageRepository.findAll(sExample, pageable);
        System.out.println(ist);
    }

    //测试新增
    @Test
    public void test3() {
        CmsPage cmsPage = new CmsPage();
        cmsPage.setPageName("joejoeejo");
        CmsPage save = cmsPageRepository.save(cmsPage);
        System.out.println(save);
    }

    //测试更新
    @Test
    public void test4() {
        Optional<CmsPage> cmspage = cmsPageRepository.findById("5e0747a79049f90ce4ce8f36");
        if (cmspage.isPresent()) {
            CmsPage page = cmspage.get();
            page.setPageName("pang");

            CmsPage save = cmsPageRepository.save(page);
            System.out.println(save);
        }

    }

    public static void gogo(String a, int b, long c) {
        System.out.println(a + b + c);
    }


    @Autowired
    GridFsTemplate gridFsTemplate;

    @Test
    public void testGridFs() throws FileNotFoundException {
        //要存储的文件
        File file = new File("C:\\java\\source\\xuechengftl\\indexbanner.ftl");
        //定义输入流
        FileInputStream inputStram = new FileInputStream(file);
        //向GridFS存储文件
        ObjectId objectId = gridFsTemplate.store(inputStram,
                "轮播图测试文件01", "");
        //得到文件ID
        String fileId = objectId.toString();
        System.out.println(fileId);
    }

    @Autowired
    GridFSBucket gridFSBucket;

    @Test
    public void queryFile() throws IOException {
        String fileId =
                "5e119aab9049f913b46952b8";
        //根据id查询文件
        GridFSFile gridFSFile =
                gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        //打开下载流对象
        GridFSDownloadStream gridFSDownloadStream =
                gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //创建gridFsResource，用于获取流对象
        GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
        //获取流中的数据
        String s = IOUtils.toString(gridFsResource.getInputStream(),
                "UTF-8");
        System.out.println(s);
    }

}
