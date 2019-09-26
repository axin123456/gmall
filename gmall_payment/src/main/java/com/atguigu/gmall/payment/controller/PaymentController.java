package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.bean.OrderInfo;
import com.atguigu.bean.PaymentInfo;
import com.atguigu.bean.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.service.OrderService;
import com.atguigu.service.PaymentInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;

    @Autowired
    AlipayClient alipayClient;

    @Reference
    PaymentInfoService paymentInfoService;

    @GetMapping("index")
    public String index(String orderId, HttpServletRequest request) {
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        request.setAttribute("orderId", orderId);
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());
        return "index";
    }

    @PostMapping("/alipay/submit")
    public String alipaySubmit(String orderId, HttpServletResponse response) {
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        long currentTimeMillis = System.currentTimeMillis();
        String outTradeNo = "ATGUIGU-" + orderId + "-" + currentTimeMillis;
        String productNo = "FAST_INSTANT_TRADE_PAY";
        BigDecimal totalAmount = orderInfo.getTotalAmount();
        String subject = orderInfo.genSubject();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no", outTradeNo);
        jsonObject.put("product_code", productNo);
        jsonObject.put("total_amount", totalAmount);
        jsonObject.put("subject", subject);
        alipayRequest.setBizContent(jsonObject.toJSONString());

        //组织参数
        String submitHtml = "";
        try {
            submitHtml = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");

        //把提交操作保存起来
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setSubject(subject);
        paymentInfo.setTotalAmount(totalAmount);
        paymentInfoService.savePaymentInfo(paymentInfo);
        return submitHtml;
    }

    @PostMapping("alipay/callback/notify")
    public String notify(@RequestParam Map<String,String> paramMap, HttpServletRequest request) throws AlipayApiException {
        String sign = paramMap.get("sign");
        boolean ifPass = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);
        if(ifPass){
            String trade_status = paramMap.get("trade_status");
            String total_amount = paramMap.get("total_amount");
            String out_trade_no = paramMap.get("out_trade_no");
            if("TRADE_SUCCESS".equals(trade_status)){

                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(paymentInfoQuery);
                if(paymentInfo.getTotalAmount().compareTo(new BigDecimal(total_amount))==0){
                    if(paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID)){
                        PaymentInfo paymentInfoUpdate = new PaymentInfo();
                        paymentInfoUpdate.setPaymentStatus(PaymentStatus.PAID);
                        paymentInfoUpdate.setCreateTime(new Date());
                        paymentInfoUpdate.setCallbackContent(JSON.toJSONString(paramMap));
                        paymentInfoUpdate.setAlipayTradeNo(paramMap.get("trade_no"));
                        paymentInfoService.updatePaymentInfoByOutTradeNO(out_trade_no,paymentInfoUpdate);
                        return "success";

                    }else if(paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED)){
                        return "fail";
                    }else if(paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID)){
                        return "success";
                    }
                }

            }

        }
        return "fail";
    }

    @GetMapping("sendPayment")
    @ResponseBody
    public String sendPayment(String orderId){
        paymentInfoService.sendPaymentToOrder(orderId,"success");
        return "success";
    }

    @GetMapping("alipay/callback/return")
    @ResponseBody
    public String alipayReturn(){
        return "交易成功";
    }

    @GetMapping("refund")
    @ResponseBody
    public String refund(String orderId) throws AlipayApiException {
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOrderId(orderId);
        PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(paymentInfoQuery);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no",paymentInfo.getAlipayTradeNo());
        jsonObject.put("refund_amount",paymentInfo.getTotalAmount().add(new BigDecimal(2)));
        request.setBizContent(jsonObject.toJSONString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("退款成功");
            System.out.println("业务退款成功");
            PaymentInfo paymentInfoForUpdate = new PaymentInfo();
            paymentInfoForUpdate.setPaymentStatus(PaymentStatus.PAY_REFUND);
            paymentInfoService.updatePaymentInfoByOutTradeNO(paymentInfo.getAlipayTradeNo(),paymentInfoForUpdate);
            return "success";
        }else {
            return response.getSubCode()+":"+response.getSubMsg();
        }

    }

}
