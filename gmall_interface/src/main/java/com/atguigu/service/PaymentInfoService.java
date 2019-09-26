package com.atguigu.service;

import com.atguigu.bean.PaymentInfo;

public interface PaymentInfoService {

    public void savePaymentInfo(PaymentInfo paymentInfo);

    public  PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    public void updatePaymentInfoByOutTradeNO(String outTradeNo,PaymentInfo paymentInfoUpdate);

    public String sendPaymentToOrder(String orderId,String result);

}
