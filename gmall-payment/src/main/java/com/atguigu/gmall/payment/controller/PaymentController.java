package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    AlipayClient alipayClient;

    @RequestMapping(path = "alipay/callback/return")
    @ResponseBody
    public String alipayCallbackReturn(String outTradeNo, String totalAmount, HttpServletRequest request, ModelMap modelMap) {

        // 回调请求中获取支付宝参数
        String sign = request.getParameter("sign");
        String trade_no = request.getParameter("trade_no");
        String out_trade_no = request.getParameter("out_trade_no");
        String trade_status = request.getParameter("trade_status");
        String total_amount = request.getParameter("total_amount");
        String subject = request.getParameter("subject");
        String call_back_content = request.getQueryString();

        // 通过支付宝的paramsMap进行签名验证，2.0版本的接口将paramsMap参数去掉了，导致同步请求没法验签
        if (StringUtils.isNotBlank(sign)) {
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(outTradeNo);
            paymentInfo.setAlipayTradeNo(trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent(call_back_content);

            paymentService.updatePaymentInfo(paymentInfo);
        }
        return "finish";
    }

    @RequestMapping(path = "alipay/submit")
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount, HttpServletRequest request, ModelMap modelMap) {
        //获得支付宝请求的客户端，他不是一个连接，而是一个封装好的表单请求
        String form = null;
        try {

            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
            alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
            alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

            Map<String, Object> map = new HashMap<>();
            map.put("out_trade_no", outTradeNo);
            map.put("product_code", "FAST_INSTANT_TRADE_PAY");
            map.put("total_amount", totalAmount);
            map.put("subject", "南哪儿大学神州一号支付测验phone");

            String alipayRequestStr = JSON.toJSONString(map);

            alipayRequest.setBizContent(alipayRequestStr);

            form = alipayClient.pageExecute(alipayRequest).getBody();

            //创建并保存支付信息,创建信息从订单中获得
            OmsOrder omsOrder = orderService.getOmsOrderByOutTradeNo(outTradeNo);
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderId(omsOrder.getId());
            paymentInfo.setOrderSn(outTradeNo);
            paymentInfo.setSubject("南哪儿商城商品支付");
            paymentInfo.setCreateTime(new Date());
            paymentInfo.setPaymentStatus("未支付");
            paymentInfo.setTotalAmount(totalAmount);
            paymentService.savePaymentInfo(paymentInfo);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return form;
    }

    @RequestMapping(path = "mx/submit")
    public String mx(String outTradeNo, String totalAmount, HttpServletRequest request, ModelMap modelMap) {
        return null;
    }

    @RequestMapping(path = "index")
    public String index(String outTradeNo, String totalAmount, HttpServletRequest request, ModelMap modelMap) {
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");
        memberId = "1";
        nickname = "windir";

        modelMap.put("outTradeNo", outTradeNo);
        modelMap.put("totalAmount", totalAmount);
        modelMap.put("nickname", nickname);

        return "index";
    }
}
