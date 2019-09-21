package com.atguigu.service;

import com.atguigu.bean.CartInfo;

import java.util.List;

public interface CartService {
    public CartInfo addCart(String userId, String skuId, Integer num);

     public List<CartInfo> cartList(String userId);

     public List<CartInfo> mergeCartList(String userIdDest,String userIdOrig);
}
