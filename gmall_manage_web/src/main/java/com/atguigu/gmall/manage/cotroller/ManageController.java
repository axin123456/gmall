package com.atguigu.gmall.manage.cotroller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.bean.*;
import com.atguigu.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {

   @Reference
    ManageService manageService;

    @PostMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        List<BaseCatalog1> baseCatalog1List = manageService.getCatalog1();
        return baseCatalog1List;
    }


    @PostMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        List<BaseCatalog2> baseCatalog2List = manageService.getCatalog2(catalog1Id);
        return baseCatalog2List;
    }

    @PostMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        List<BaseCatalog3> baseCatalog3List = manageService.getCatalog3(catalog2Id);
        return baseCatalog3List;
    }

    @GetMapping("attrInfoList")
    public List<BaseAttrInfo> getBaseAttrInfoList(String catalog3Id){
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(catalog3Id);
        return baseAttrInfoList;
    }


    @PostMapping("saveAttrInfo")
    public String saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return "success";
    }
    @PostMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList (String attrId){
        BaseAttrInfo baseAttrInfo = manageService.getBaseAttrInfo(attrId);
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        return attrValueList;
    }

    @PostMapping("baseSaleAttrList")
    public List<BaseSaleAttr> getbaseSaleAttrList(){
       return manageService.getbaseSaleAttrList();
    }

    @PostMapping("saveSpuInfo")
    public String saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return "success";
    }
    @GetMapping("spuList")
    public List<SpuInfo> getSpuList(String catalog3Id){
       return manageService.getSpuList(catalog3Id);
    }


    @GetMapping("spuImageList")
    public List<SpuImage> getSpuImageList(String spuId){
      return   manageService.getSpuImageList(spuId);
    }


    @GetMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId){
       return manageService.getSpuSaleAttrList(spuId);
    }
}

