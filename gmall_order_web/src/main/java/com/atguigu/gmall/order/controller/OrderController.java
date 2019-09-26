package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.bean.*;
import com.atguigu.bean.enums.OrderStatus;
import com.atguigu.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.service.CartService;
import com.atguigu.service.ManageService;
import com.atguigu.service.OrderService;
import com.atguigu.service.UserService;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Controller
public class OrderController {

    @Reference
    UserService userservie;

    @Reference
    CartService cartService;

    @Reference
    OrderService orderService;

    @Reference
    ManageService manageService;

    @GetMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request) {
        String userId = (String)request.getAttribute("userId");
        List<UserAddress> userAddressList = userservie.getUserAddressList(userId);
        request.setAttribute("userAddressList",userAddressList);
        List<CartInfo> checkedCartList = cartService.getCheckedCartList(userId);
        request.setAttribute("checkedCartList",checkedCartList);
        BigDecimal totalAmount = new BigDecimal(0);
        for (CartInfo cartInfo : checkedCartList) {
            BigDecimal multiply = cartInfo.getSkuPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
            totalAmount=totalAmount.add(multiply);
        }

        String token = orderService.genToken(userId);
        request.setAttribute("tradeNo",token);
        request.setAttribute("totalAmount",totalAmount);
        return "trade";
    }

    @PostMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo , HttpServletRequest request){
        String userId = (String)  request.getAttribute("userId");
        String tradeNo = request.getParameter("tradeNo");
        boolean isEnableToken = orderService.verifyToken(userId, tradeNo);
        if(!isEnableToken){
            request.setAttribute("errMsg","页面失效,请重新下单!");
            return "tradeFail";
        }



        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.setCreateTime(new Date());
        orderInfo.setExpireTime(DateUtils.addMinutes(new Date(),15));
        orderInfo.sumTotalAmount();
        orderInfo.setUserId((String)request.getAttribute("userId"));
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            orderDetail.setImgUrl(skuInfo.getSkuDefaultImg());
            orderDetail.setSkuName(skuInfo.getSkuName());
            if(!orderDetail.getOrderPrice().equals(skuInfo.getPrice())){
                request.setAttribute("errMsg","商品的价格已发生改变,请重新下单");
                return "tradeFail";
            }
        }
        List<OrderDetail> errList= Collections.synchronizedList(new ArrayList<>());
        Stream<CompletableFuture<String>> completableFutureStream = orderDetailList.stream().map(orderDetail ->
                CompletableFuture.supplyAsync(() -> checkSkuNum(orderDetail)).whenComplete((hasStock, ex) -> {
                    if (hasStock.equals("0")) {
                        errList.add(orderDetail);
                    }
                })
        );
        CompletableFuture[] completableFutures = completableFutureStream.toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(completableFutures).join();

        if(errList.size()>0){
            StringBuffer errStingbuffer=new StringBuffer();
            for (OrderDetail orderDetail : errList) {
                errStingbuffer.append("商品："+orderDetail.getSkuName()+"库存暂时不足！");
            }
            request.setAttribute("errMsg",errStingbuffer.toString());
            return  "tradeFail";
        }


        String orderId = orderService.saveOrder(orderInfo);
        return "redirct://payment.gmall.com/index?orderId"+orderId;
    }


    public String checkSkuNum(OrderDetail orderDetail){
        String hasStock = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + orderDetail.getSkuId() + "&num=" + orderDetail.getSkuNum());
        return  hasStock;
    }

}
