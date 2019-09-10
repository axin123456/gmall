package com.atguigu.service;

import com.atguigu.bean.*;

import java.util.List;

public interface ManageService {
    //查询一级分类
    public List<BaseCatalog1> getCatalog1();

    //查询二级分类  根据一级分类id
    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    //查询三级分类 根据二级分类id
    public List<BaseCatalog3> getCatalog3(String catalog2Id);

    //根据三级分类查询平台属性
    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    //保存平台属性
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    //根据平台属性id 查询平台属性详情
    public BaseAttrInfo getBaseAttrInfo(String attrId);


    //获取基本的销售属性
    public List<BaseSaleAttr> getbaseSaleAttrList();

    //保存spu信息
    public  void saveSpuInfo(SpuInfo spuInfo);


    //根据三级分类查询spu列表
     public List<SpuInfo>  getSpuList(String catalog3Id);

    //根据spuId查询图片列表
     public List<SpuImage> getSpuImageList(String spuId);

    //根据spuId查询销售属性
     public List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    //保存sku
    public  void saveSkuInfo(SkuInfo skuInfo);
}
