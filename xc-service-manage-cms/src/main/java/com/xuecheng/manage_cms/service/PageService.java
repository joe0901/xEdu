package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.exception.ExceptionUtil;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitmqConfig;
import com.xuecheng.manage_cms.dao.CmsConfigRepository;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Administrator
 * @version 1.0
 * @create 2018-09-12 18:32
 **/
@Service
public class PageService {

    @Autowired
    CmsPageRepository cmsPageRepository;


    /**
     * 页面查询方法
     *
     * @param page             页码，从1开始记数
     * @param size             每页记录数
     * @param queryPageRequest 查询条件
     * @return
     */
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest) {

        if (queryPageRequest == null) {
            queryPageRequest = new QueryPageRequest();
        }
        CmsPage cmsPage = new CmsPage();
        if (!StringUtils.isEmpty(queryPageRequest.getSiteId())) {
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        if (!StringUtils.isEmpty(queryPageRequest.getPageAliase())) {
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
        if (!StringUtils.isEmpty(queryPageRequest.getTemplateId())) {
            cmsPage.setTemplateId(queryPageRequest.getTemplateId());
        }

        //分页参数
        if (page <= 0) {
            page = 1;
        }
        page = page - 1;
        if (size <= 0) {
            size = 10;
        }
        Pageable pageable = PageRequest.of(page, size);
        ExampleMatcher matcher = ExampleMatcher.matching();
        matcher = matcher.withMatcher("pageAliase",
                ExampleMatcher.GenericPropertyMatchers.contains());
        Example<CmsPage> example = Example.of(cmsPage, matcher);
        Page<CmsPage> all = cmsPageRepository.findAll(example, pageable);
        QueryResult queryResult = new QueryResult();
        queryResult.setList(all.getContent());//数据列表
        queryResult.setTotal(all.getTotalElements());//数据总记录数
        QueryResponseResult queryResponseResult = new QueryResponseResult(CommonCode.SUCCESS, queryResult);
        return queryResponseResult;
    }

    /**
     * 页面新增方法
     */
    public CmsPageResult add(CmsPage cmsPage) {

        //根据联合主键查此页面是否存在
        CmsPage page1 = cmsPageRepository.findBySiteIdAndPageNameAndPageWebPath(cmsPage.getSiteId(), cmsPage.getPageName(), cmsPage.getPageWebPath());
        if (page1 != null) {
            //ExceptionUtil.throwExc(CommonCode.FAIL);
            ExceptionUtil.throwExc(CmsCode.CMS_ADDPAGE_EXISTSNAME);

        }
        //防止前端将主键赋值
        cmsPage.setPageId(null);
        cmsPageRepository.save(cmsPage);
        return new CmsPageResult(CommonCode.SUCCESS, cmsPage);
    }

    public CmsPage findById(String id) {

        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()) {
            CmsPage cmsPage = optional.get();
            return cmsPage;
        }
        return null;
    }

    public CmsPageResult edit(String id, CmsPage cmsPage) {
        CmsPage one = findById(id);
        if (one != null) {
            one.setTemplateId(cmsPage.getTemplateId());
            //更新所属站点
            one.setSiteId(cmsPage.getSiteId());
            //更新页面别名
            one.setPageAliase(cmsPage.getPageAliase());
            //更新页面名称
            one.setPageName(cmsPage.getPageName());
            //更新访问路径
            one.setPageWebPath(cmsPage.getPageWebPath());
            //更新物理路径
            one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            //day04 更新dataIr;
            one.setDataUrl(cmsPage.getDataUrl());
            //执行更新
            CmsPage save = cmsPageRepository.save(one);
            if (save != null) {
                //返回成功
                CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS, save);
                return cmsPageResult;
            }
        }
        //返回失败
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    public ResponseResult delete(String id) {
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()) {
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    @Autowired
    CmsConfigRepository cmsConfigRepository;


    public CmsConfig getById(String id) {
        Optional<CmsConfig> option = cmsConfigRepository.findById(id);
        if (option.isPresent()) {
            return option.get();
        }
        return null;
    }

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    GridFSBucket gridFSBucket;

    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    //页面静态化
    public String getPageHtml(String pageId) {
        //获取页面模型数据
        Map model = this.getModelByPageId(pageId);
        if (model == null) {
            //获取页面模型数据为空
            ExceptionUtil.throwExc(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        //获取页面模板
        String templateContent = getTemplateByPageId(pageId);
        if (StringUtils.isEmpty(templateContent)) {
            //页面模板为空
            ExceptionUtil.throwExc(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //执行静态化
        String html = generateHtml(templateContent, model);
        if (StringUtils.isEmpty(html)) {
            ExceptionUtil.throwExc(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return html;

    }

    //执行静态化
    private String generateHtml(String templateContent, Map model) {
        try {
            //生成配置类
            Configuration configuration = new Configuration(Configuration.getVersion());
            //模板加载器
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template"
                    , templateContent);
            //配置模板加载器
            configuration.setTemplateLoader(stringTemplateLoader);
            //获取模板
            Template template1 = configuration.getTemplate("template");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template1, model);
            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getTemplateByPageId(String pageId) {
        CmsPage cmsPage = this.findById(pageId);
        if (cmsPage == null) {
            ExceptionUtil.throwExc(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        String templateId = cmsPage.getTemplateId();
        String s = null;
        Optional<CmsTemplate> option = cmsTemplateRepository.findById(templateId);
        if (option.isPresent()) {
            CmsTemplate cmsTemplate = option.get();
            String templateFileId = cmsTemplate.getTemplateFileId();
            //根据id查询文件
            GridFSFile gridFSFile =
                    gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
            //打开下载流对象
            GridFSDownloadStream gridFSDownloadStream =
                    gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            //创建gridFsResource，用于获取流对象
            GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
            //获取流中的数据
            try {
                s = IOUtils.toString(gridFsResource.getInputStream(),
                        "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return s;
    }

    private Map getModelByPageId(String pageId) {
        CmsPage cmsPage = this.findById(pageId);
        if (cmsPage == null) {
            ExceptionUtil.throwExc(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        String dataUrl = cmsPage.getDataUrl();
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = forEntity.getBody();
        return body;
    }

    public ResponseResult post(String pageId) {
        //执行静态化
        String pageHtml = this.getPageHtml(pageId);
        if(StringUtils.isEmpty(pageHtml)){
            ExceptionUtil.throwExc(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        //保存静态化文件
        CmsPage cmsPage = saveHtml(pageId, pageHtml);
        //发送消息
        sendPostPage(cmsPage);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private void sendPostPage(CmsPage cmsPage) {
        Map<String,String> msgMap = new HashMap<>();
        msgMap.put("pageId",cmsPage.getPageId());
        //消息内容
        String msg = JSON.toJSONString(msgMap);
        //获取站点id作为routingKey
        String siteId = cmsPage.getSiteId();
        //发布消息
        this.rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,siteId, msg);
    }

    private CmsPage saveHtml(String pageId, String pageHtml) {
        Optional<CmsPage> option = cmsPageRepository.findById(pageId);
        if (option.isPresent()) {
            CmsPage cmsPage = option.get();
            ObjectId objectId =null;
            try {
                InputStream inputStream = IOUtils.toInputStream(pageHtml, "utf-8");
                objectId = gridFsTemplate.store(inputStream, cmsPage.getPageName());
            } catch (IOException e) {
                e.printStackTrace();
            }
            cmsPage.setHtmlFileId(objectId.toString());
            cmsPageRepository.save(cmsPage);
            return cmsPage;

        }
        return null;
    }
}
