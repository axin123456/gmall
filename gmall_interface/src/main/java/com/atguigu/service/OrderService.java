package com.atguigu.service;

import com.atguigu.bean.OrderInfo;
import com.atguigu.bean.enums.ProcessStatus;

public interface OrderService {

    public  String saveOrder(OrderInfo orderInfo);

    public String genToken(String userId);

    public boolean verifyToken(String userId,String token);

    public OrderInfo getOrderInfo(String OrderId);

    public void updateStatus(String orderId, ProcessStatus processStatus,OrderInfo... orderInfo);


}
