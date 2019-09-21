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
        List<CartInfo> cartList = null;
        if (userId != null) {
            cartList = cartService.cartList(userId);
        }
        String userTmpId = CookieUtil.getCookieValue(request, "user_tmp_id", false);
        List<CartInfo> cartTmpList = null;
        if (userTmpId != null) {
            cartTmpList = cartService.cartList(userTmpId);
            cartList=cartTmpList;
        }
        if (userId != null && cartTmpList != null && cartTmpList.size() > 0) {
            cartList = cartService.mergeCartList(userId,userTmpId);
        }
        request.setAttribute("cartList", cartList);
        return "cartList";
    }

}
