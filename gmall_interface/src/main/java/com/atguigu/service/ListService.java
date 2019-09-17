package com.atguigu.service;

import com.atguigu.bean.SkuLsInfo;
import com.atguigu.bean.SkuLsParams;
import com.atguigu.bean.SkuLsResult;

public interface ListService {

    public void  saveSkuLsInfo(SkuLsInfo skuLsInfo);

    public SkuLsResult getSkuLsInfoList(SkuLsParams skuLsParams);
}
