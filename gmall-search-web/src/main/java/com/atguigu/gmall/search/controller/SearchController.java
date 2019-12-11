package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping(path = "list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {

        //调用搜素服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", pmsSearchSkuInfoList);

        //用java集合将属性id从搜索结果中抽取出来
        Set<String> pmsSkuAttrValueIdSet = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                pmsSkuAttrValueIdSet.add(pmsSkuAttrValue.getValueId());
            }
        }

        //根据valueId查询属性列表
        List<PmsBaseAttrInfo> pmsBaseAttrInfoList = attrService.getAttrListByValueId(pmsSkuAttrValueIdSet);
        //对平台属性做进一步处理，去掉当前valueId所在的属性组
        //同时：当有一个valueId 就要相应的制作一个面包屑
        //制作面包屑和删除valueId所在的属性组是同步的
        String[] DelValueIds = pmsSearchParam.getValueId();
        if (DelValueIds != null) {
            List<PmsSearchCrumb> crumbList = new ArrayList<>();
            for (String delValueId : DelValueIds) {
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfoList.iterator();
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                pmsSearchCrumb.setValueId(delValueId);
                pmsSearchCrumb.setUrlParam(getUrlParmForCromb(pmsSearchParam, delValueId));
                while (iterator.hasNext()) {
                    PmsBaseAttrInfo attrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = attrInfo.getAttrValueList();
                    for (PmsBaseAttrValue attrValue : attrValueList) {
                        String valueId = attrValue.getId();
                        if (delValueId.equals(valueId)) {
                            //为当前制作的面包屑设置属性名称
                            pmsSearchCrumb.setValueName(attrInfo.getAttrName() + ":" + attrValue.getValueName());
                            //删除该属性所在的属性组
                            iterator.remove();
                        }
                    }
                }
                crumbList.add(pmsSearchCrumb);
            }
            //面包屑
            modelMap.put("attrValueSelectedList", crumbList);
        }
        modelMap.put("attrList", pmsBaseAttrInfoList);

        //构造urlParm
        String urlParm = urlParm = getUrlParm(pmsSearchParam);
        modelMap.put("urlParam", urlParm);


        return "list";
    }

    private String getUrlParm(PmsSearchParam pmsSearchParam) {

        String urlParm = "";
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String keyword = pmsSearchParam.getKeyword();
        String[] valueIds = pmsSearchParam.getValueId();

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParm)) {
                urlParm = urlParm + "&";
            }
            urlParm = urlParm + "catalog3Id=" + catalog3Id;
        }
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParm)) {
                urlParm = urlParm + "&";
            }
            urlParm = urlParm + "keyword=" + keyword;
        }
        if (valueIds != null) {
            for (String valueId : valueIds) {
                urlParm = urlParm + "&valueId=" + valueId;
            }
        }
        return urlParm;
    }

    private String getUrlParmForCromb(PmsSearchParam pmsSearchParam, String delValueId) {

        String urlParm = "";
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String keyword = pmsSearchParam.getKeyword();
        String[] valueIds = pmsSearchParam.getValueId();

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParm)) {
                urlParm = urlParm + "&";
            }
            urlParm = urlParm + "catalog3Id=" + catalog3Id;
        }
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParm)) {
                urlParm = urlParm + "&";
            }
            urlParm = urlParm + "keyword=" + keyword;
        }
        if (valueIds != null) {
            for (String valueId : valueIds) {
                if (!valueId.equals(delValueId)) {
                    urlParm = urlParm + "&valueId=" + valueId;
                }

            }
        }
        return urlParm;
    }

    @RequestMapping(path = "index")
    public String index() {
        return "index";
    }
}
