package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.bean.UserInfo;
import com.atguigu.service.UserService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @Reference
   UserService userservie;
    @GetMapping("trade")
    public UserInfo trade(@RequestParam("userid") String userid){
        UserInfo userInfo = userservie.getUserInfoById(userid);
        return userInfo;
    }
}
