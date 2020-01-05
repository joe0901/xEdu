package com.xuecheng.manage_cms.dao;


import com.xuecheng.framework.domain.cms.CmsPage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CmsPageRepository extends MongoRepository<CmsPage,String> {
    //根据页面名称查询
    CmsPage findByPageName(String pageName);

    //自定义查询条件
    CmsPage findBySiteIdAndPageNameAndPageWebPath(String siteId, String pageName, String pageWebPath);
}
