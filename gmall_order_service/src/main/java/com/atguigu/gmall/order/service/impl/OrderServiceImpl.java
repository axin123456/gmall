package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.bean.OrderDetail;
import com.atguigu.bean.OrderInfo;
import com.atguigu.bean.enums.ProcessStatus;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderMapper;
import com.atguigu.gmall.util.RedisUtil;
import com.atguigu.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import javax.persistence.Transient;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderDetailMapper orderDetailMapper;

    @Override
    @Transient
    public String saveOrder(OrderInfo orderInfo) {
        orderMapper.insertSelective(orderInfo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        return orderInfo.getId();
    }

    @Override
    public String genToken(String userId) {
        String token = UUID.randomUUID().toString();
        String tokenKey = "user:" + userId + ":trade_code";
        Jedis jedis = redisUtil.getJedis();
        jedis.setex(tokenKey, 10 * 60, token);
        jedis.close();
        return token;
    }

    @Override
    public boolean verifyToken(String userId, String token) {
        String tokenKey = "user:" + userId + ":trade_code";
        Jedis jedis = redisUtil.getJedis();
        String tokenExists = jedis.get(tokenKey);
        jedis.watch(tokenKey);
        Transaction transaction = jedis.multi();
        if (tokenExists != null && tokenExists.equals(token)) {
            transaction.del(tokenKey);
        }

        List<Object> list = transaction.exec();
        if (list != null && list.size() > 0 && (Long) list.get(0) == 1L) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderMapper.selectByPrimaryKey(orderId);
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateStatus(String orderId, ProcessStatus processStatus,OrderInfo... orderInfos) {
        OrderInfo orderInfo = new OrderInfo();
        if(orderInfos!=null&&orderInfos.length>0){
            orderInfo = orderInfos[0];
        }
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfo.setId(orderId);
        orderMapper.updateByPrimaryKeySelective(orderInfo);
    }


}
