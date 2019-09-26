package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.bean.CartInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {

    @Reference
    CartService cartService;

    @PostMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addCart(@RequestParam String skuId, @RequestParam("num") int num, HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            userId = CookieUtil.getCookieValue(request, "user_tmp_id", false);
            if (userId == null) {
                userId = UUID.randomUUID().toString();

                CookieUtil.setCookie(request, response, "user_tmp_id", userId, 60 * 60 * 24 * 7, false);
            }
        }
        CartInfo cartInfo = cartService.addCart(userId, skuId, num);
        request.setAttribute("cartInfo", cartInfo);
        request.setAttribute("num", num);
        return "success";
    }

    @GetMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request) {

        String userId = (String) request.getAttribute("userId");
        if (userId != null) {//有登录
            List<CartInfo> cartList = null;//如果未登录时,考虑购物车要合并
            cartList = cartService.cartList(userId);

        String userTmpId = CookieUtil.getCookieValue(request, "user_tmp_id", false);//取临时的id
            if (userTmpId != null) {//如果有临时的id要查临时的购物车
                List<CartInfo> cartTmpList = cartService.cartList(userTmpId);
                if(cartTmpList!=null&&cartTmpList.size()>0){//如果有临时的购物车合并临时的购物车,
                    cartList=  cartService.mergeCartList(userId,userTmpId);//得到合并后的购物车列表
                }
            }
            if(cartList==null&&cartList.size()==0){//如果不需要合并,再去登录后的购物车
                cartList =  cartService.cartList(  userId);
            }
        }else{//未登录去临时购物车
            String userTmpId=CookieUtil.getCookieValue(request, "user_tmp_id", false);
            if(userTmpId!=null){
                List<CartInfo> cartTmpList = cartService.cartList(userTmpId);
            }
        }


        return "cartList";
    }

    @PostMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(@RequestParam("isChecked") String isChecked,@RequestParam("skuId")String skuId ,HttpServletRequest request){
        String userId = (String)request.getAttribute("userId");
        if(userId==null){
            userId = CookieUtil.getCookieValue(request, "user_tmp_id", false);
        }
        cartService.checkCart(userId,skuId,isChecked);
    }
}
