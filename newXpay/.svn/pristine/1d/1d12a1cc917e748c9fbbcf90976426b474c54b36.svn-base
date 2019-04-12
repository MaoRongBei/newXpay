package com.hrtpayment.xpay.baidu.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.baidu.bean.BaiduCalbackBean;
import com.hrtpayment.xpay.baidu.bean.BaiduRefundCallBackBaen;
import com.hrtpayment.xpay.baidu.service.BaiduPayService;
import com.hrtpayment.xpay.utils.exception.BusinessException;

/**
 * @author lvjianyu
 * 百度钱包 2017-07-20
 */
@Controller
@RequestMapping("xpay")
public class BaiduPayController {
	
	Logger logger = LogManager.getLogger();
	
	@Autowired
    private BaiduPayService service;
	
	//mid:864001094020651
	
	/**
	 * 百度钱包支付-异步通知
	 * @param formStr
	 * @return
	 */
	@RequestMapping("hrtbfbcallback")
	@ResponseBody
	public String callback(BaiduCalbackBean bean) {
		logger.info("接收到的异步通知消息:" + bean.toString());
		String result="";
		try {
			result = service.payCallBack(bean);
		} catch (BusinessException e) {
			logger.error("返回异步通知消息出错:" + result);
		}
		logger.info("返回异步通知消息:" + result);
		return result;
	}
	
	
	/**
	 * 百度钱包-异步退款通知
	 * @param refundback
	 * @return
	 * @throws UnsupportedEncodingException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	@RequestMapping(value="hrtBfbRefundCallback")
	@ResponseBody
	public String refundCallback(BaiduRefundCallBackBaen refundback, HttpServletRequest request,
			HttpServletResponse response) throws UnsupportedEncodingException {
		request.setCharacterEncoding("GBK");
	    String getStrPre=request.getQueryString();
	    String getStr=URLDecoder.decode(getStrPre, "GBK");
		
		logger.info("接收到的退款 异步通知消息:"+getStr);
		try {
			String result =service.refundCallBack(refundback,getStr);
			logger.info("退款异步返回消息:"+result);
			return result;
		} catch (BusinessException e) {
			logger.error("退款异步返回消息解析出错:"+e.getMessage());
			return e.getMessage();
		}
	}
	
}
