package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.bean.PaymentInfo;
import com.atguigu.bean.enums.PaymentStatus;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.service.PaymentInfoService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;

@Service
public class PaymentInfoServiceImpl implements PaymentInfoService {


    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery) {
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQuery);
        return paymentInfo;
    }

    @Override
    public void updatePaymentInfoByOutTradeNO(String outTradeNo, PaymentInfo paymentInfoUpdate) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo", outTradeNo);
        paymentInfoMapper.selectByExample(example);

    }

    @Override
    public String sendPaymentToOrder(String orderId, String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            MessageProducer producer = session.createProducer(session.createQueue("PAYMENT_TO_ORDER"));
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("orderId", orderId);
            mapMessage.setString("result", result);
            producer.send(mapMessage);
            session.commit();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return null;
    }

    public PaymentStatus checkAlipayPayment(PaymentInfo paymentInfo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "\"out_trade_no\":\"" + paymentInfo.getOutTradeNo() + "\"" +
                "  }");
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            if ("TRADE_SUCCESS".equals(response.getTradeStatus())) {
                return PaymentStatus.PAID;
            } else if ("WAIT_BUYER_PAY".equals(response.getTradeStatus())) {
                return PaymentStatus.UNPAID;
            }
        } else {
            System.out.println("调用失败");
            return PaymentStatus.UNPAID;
        }
        return null;
    }

    public void sendDelayPaymentResult(String outTradeNo, Long delaySec, Integer checkCount) {
        Connection connection = activeMQUtil.getConnection();
        try {
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(queue);

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo", outTradeNo);
            mapMessage.setLong("delaySec", delaySec);
            mapMessage.setInt("checkCount", checkCount);

            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySec * 1000);
            producer.send(mapMessage);
            session.commit();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE" ,containerFactory = "jmsQueueListener")
    public void consumeDelayCheck(MapMessage mapMessage) throws JMSException {
        String outTradeNo = mapMessage.getString("outTradeNo");
        long delaySec = mapMessage.getLong("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        PaymentInfo paymentInfoQuery = new PaymentInfo();
        paymentInfoQuery.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfoResult = getPaymentInfo(paymentInfoQuery);
        if(paymentInfoResult.getPaymentStatus()!=PaymentStatus.UNPAID){
            return;
        }
        PaymentStatus paymentStatus = checkAlipayPayment(paymentInfoQuery);
        if(paymentStatus==PaymentStatus.PAID){
            paymentInfoResult.setPaymentStatus(PaymentStatus.PAID);
            updatePaymentInfoByOutTradeNO(outTradeNo,paymentInfoResult);
            sendPaymentToOrder(paymentInfoResult.getId(),"success");
        }else if(paymentStatus==PaymentStatus.UNPAID){
            if(checkCount>0){
                checkCount--;
                sendDelayPaymentResult(outTradeNo,delaySec,checkCount);
            }
        }

    }
}
