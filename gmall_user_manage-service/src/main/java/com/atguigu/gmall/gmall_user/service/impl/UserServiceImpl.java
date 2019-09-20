package com.atguigu.gmall.gmall_user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.bean.UserInfo;
import com.atguigu.gmall.gmall_user.mapper.UserMapper;

import com.atguigu.gmall.util.RedisUtil;
import com.atguigu.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;


import java.util.List;

@Service
public class UserServiceImpl implements UserService {


    @Autowired
    UserMapper userMapper;

    @Autowired
    RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    @Override
    public List<UserInfo> getUserInfoListAll() {
        List<UserInfo> userInfoList = userMapper.selectAll();
        return userInfoList;
    }

    @Override
    public void addUser(UserInfo userInfo) {
        userMapper.insertSelective(userInfo);

    }

    @Override
    public void updateUser(UserInfo userInfo) {
        userMapper.updateByPrimaryKeySelective(userInfo);
    }

    @Override
    public void updateUserByName(String name, UserInfo userInfo) {

        Example example = new Example(UserInfo.class);

        example.createCriteria().andEqualTo("name",name);

        userMapper.updateByExampleSelective(userInfo,example);
    }

    @Override
    public void delUser(UserInfo userInfo) {
        userMapper.deleteByPrimaryKey(userInfo.getId());
    }

    @Override
    public UserInfo getUserInfoById(String id) {
        return userMapper.selectByPrimaryKey(id);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        String passwd = userInfo.getPasswd();
        String passwdMd5 = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(passwdMd5);
        UserInfo userInfo1 = userMapper.selectOne(userInfo);
        if(userInfo1!=null){
            Jedis jedis = redisUtil.getJedis();
            String userKey=userKey_prefix+userInfo1.getId()+userinfoKey_suffix;
            String userInfoJson = JSON.toJSONString(userInfo1);
            jedis.setex(userKey,userKey_timeOut,userInfoJson);
            jedis.close();
            return userInfo1;
        }
        return null;
    }

    @Override
    public Boolean verify(String userId) {
        Jedis jedis = redisUtil.getJedis();
        String userKey=userKey_prefix+userId+userinfoKey_suffix;
        Boolean isLogin = jedis.exists(userKey);
        if(isLogin){
            jedis.expire(userKey,userKey_timeOut);
        }
        jedis.close();
        return isLogin;
    }
}
