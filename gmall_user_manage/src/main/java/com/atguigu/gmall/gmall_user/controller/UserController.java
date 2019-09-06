package com.atguigu.gmall.gmall_user.controller;


import com.atguigu.bean.UserInfo;
import com.atguigu.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserService userService;
    @GetMapping("allusers")
    public List<UserInfo> getAllUsers(){
        List<UserInfo> userInfoListAll = userService.getUserInfoListAll();
        return userInfoListAll;
    }

    @PostMapping("addUser")
    public String addUser(UserInfo userInfo){
        userService.addUser(userInfo);
        return "success";
    }


    @PostMapping("updateUser")
    public String updateUser(UserInfo userInfo){
        userService.updateUser(userInfo);
        return "success";
    }

    @PostMapping("updateUserByName")
    public String updateUserByName(UserInfo userInfo){
        userService.updateUserByName(userInfo.getName(),userInfo);
        return "success";
    }

    @PostMapping("delUser")
    public String delUser(UserInfo userInfo){
        userService.delUser(userInfo);
        return "success";
    }

}
