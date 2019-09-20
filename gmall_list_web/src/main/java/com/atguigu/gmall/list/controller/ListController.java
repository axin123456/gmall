package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.bean.BaseAttrInfo;
import com.atguigu.bean.BaseAttrValue;
import com.atguigu.bean.SkuLsParams;
import com.atguigu.bean.SkuLsResult;
import com.atguigu.service.ListService;
import com.atguigu.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    ListService listService;

    @Reference
    ManageService manageService;

    @RequestMapping("list.html")
    public String list(SkuLsParams skuLsParams, Model model){
        skuLsParams.setPageSize(3);
        SkuLsResult skuLsResult = listService.getSkuLsInfoList(skuLsParams);
        model.addAttribute("skuLsResult",skuLsResult);
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
        model.addAttribute("attrList",attrList);
        String paramUrl = makeParamUrl(skuLsParams);

        //已选择的平台属性值信息列表
        List<BaseAttrValue> selectedValueList=new ArrayList<>();

        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0) {
            for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo = iterator.next();
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                        String selectedValueId = skuLsParams.getValueId()[i];
                        if (baseAttrValue.getId().equals(selectedValueId)) {
                            iterator.remove();
                            String selectParamUrl = makeParamUrl(skuLsParams, selectedValueId);
                            baseAttrValue.setParamUrl(selectParamUrl);
                            selectedValueList.add(baseAttrValue);
                        }
                    }
                }

            }
        }
        model.addAttribute("paramUrl",paramUrl);
        model.addAttribute("selectedValueList",selectedValueList);
        model.addAttribute("keyword",skuLsParams.getKeyword());
        model.addAttribute("pageNO",skuLsParams.getPageNo());
        model.addAttribute("totalPages",skuLsResult.getTotalPages());
        return "list";
    }

    /**
     * 把页面传入的参数转成url路径
     * @param skuLsParams
     * @return
     */
    public String makeParamUrl(SkuLsParams skuLsParams,String... excludeValueId){
        String paramUrl="";
        if(skuLsParams.getKeyword()!=null){
            paramUrl+="keyword="+skuLsParams.getKeyword();
        }else if(skuLsParams.getCatalog3Id()!=null){
            paramUrl+="cataLog3Id="+skuLsParams.getCatalog3Id();
        }
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];
                if(excludeValueId!=null&&excludeValueId.length>0){
                    String exValue = excludeValueId[0];
                    if(valueId.equals(exValue)){
                        continue;
                    }
                }
                if(paramUrl.length()>0){
                    paramUrl+="&";
                }
                paramUrl+="valueId="+valueId;
            }
        }
        return paramUrl;
    }
}
