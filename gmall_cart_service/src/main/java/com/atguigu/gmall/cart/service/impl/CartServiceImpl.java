package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.bean.CartInfo;
import com.atguigu.bean.OrderInfo;
import com.atguigu.bean.SkuInfo;
import com.atguigu.bean.enums.OrderStatus;
import com.atguigu.bean.enums.ProcessStatus;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.util.RedisUtil;
import com.atguigu.service.CartService;
import com.atguigu.service.ManageService;
import com.atguigu.service.OrderService;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    CartInfoMapper cartInfoMapper;

    @Reference
    ManageService manageService;

    @Reference
    OrderService orderService;

    @Override
    public CartInfo addCart(String userId, String skuId, Integer num) {
        //加数据库
        CartInfo cartInfoQuery = new CartInfo();
        cartInfoQuery.setSkuId(skuId);
        cartInfoQuery.setUserId(userId);
        CartInfo cartInfoExists = null;
        cartInfoExists = cartInfoMapper.selectOne(cartInfoQuery);
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        if (cartInfoExists != null) {
            cartInfoExists.setSkuName(skuInfo.getSkuName());
            cartInfoExists.setCartPrice(skuInfo.getPrice());
            cartInfoExists.setSkuNum(cartInfoExists.getSkuNum() + num);
            cartInfoExists.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExists);
        } else {
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(num);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfoMapper.insertSelective(cartInfo);
            cartInfoExists = cartInfo;
        }
        //加缓冲
        Jedis jedis = redisUtil.getJedis();
        String cartKey = "cart:" + userId + ":info";
        String cartinfoJson = JSON.toJSONString(cartInfoExists);
        jedis.hset(cartKey, skuId, cartinfoJson);
        jedis.close();



        loadCartCache(userId);
        return cartInfoExists;
    }

    @Override
    public List<CartInfo> cartList(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String cartKey = "cart:" + userId + ":info";
        List<String> cartJsonList = jedis.hvals(cartKey);
        List<CartInfo> cartList = new ArrayList<>();
        if (cartJsonList != null && cartJsonList.size() > 0) {
            for (String cartJson : cartJsonList) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartList.add(cartInfo);
            }
            cartList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o2.getId().compareTo(o1.getId());
                }
            });
            return cartList;
        } else {
            return loadCartCache(userId);
        }


    }

    @Override
    public List<CartInfo> mergeCartList(String userIdDest, String userIdOrig) {
        cartInfoMapper.mergeCartList(userIdDest, userIdOrig);
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userIdOrig);
        cartInfoMapper.delete(cartInfo);
        Jedis jedis = redisUtil.getJedis();
        jedis.del("cart:"+userIdOrig+":info");
        jedis.close();
        List<CartInfo> cartInfoList = loadCartCache(userIdDest);
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, String skuId, String isChecked) {
        loadCartCacheIfNotExists(userId);
        //isChecked 保存在缓存中
        String cartKey = "cart:" + userId + ":info";
        Jedis jedis = redisUtil.getJedis();
        String cartInfoJson = jedis.hget(cartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String cartInfoJsonNew = JSON.toJSONString(cartInfo);
        jedis.hset(cartKey, skuId, cartInfoJsonNew);

        String cartcheckedKey = "cart:" + userId + ":checked";
        if (isChecked.equals("1")) {
            jedis.hset(cartcheckedKey, skuId, cartInfoJsonNew);
            jedis.expire(cartcheckedKey,60*60);
        } else {
            jedis.hdel(cartcheckedKey, skuId);
        }
        jedis.close();
    }

    @Override
    public List<CartInfo> getCheckedCartList(String userId) {
        String cartcheckedKey = "cart:" + userId + ":checked";
        Jedis jedis = redisUtil.getJedis();
        List<String> checkedCartList = jedis.hvals(cartcheckedKey);
        List<CartInfo> cartInfoList=new ArrayList<>();
        for (String cartInfoJson : checkedCartList) {
            CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            cartInfoList.add(cartInfo);
        }
        jedis.close();
        return cartInfoList;
    }

    public List<CartInfo> loadCartCache(String userId) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithSkuPrice(userId);
        if (cartInfoList != null && cartInfoList.size() > 0) {
            Map<String, String> cartMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                cartMap.put(cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
            }
            Jedis jedis = redisUtil.getJedis();
            String cartKey = "cart:" + userId + ":info";
            jedis.del(cartKey);
            jedis.hmset(cartKey, cartMap);
            jedis.expire(cartKey, 60 * 60 * 24);
            jedis.close();
        }
        return cartInfoList;
    }

    public void loadCartCacheIfNotExists(String userId) {
        String cartKey = "cart:" + userId + ":info";
        Jedis jedis = redisUtil.getJedis();
        Long ttl = jedis.ttl(cartKey);
        int intValue = ttl.intValue();
        jedis.expire(cartKey, 10+intValue);
        Boolean exists = jedis.exists(cartKey);
        jedis.close();
        if (!exists) {
            loadCartCache(userId);
        }
    }

}
