package com.hrtpayment.xpay.cupsAT.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.cupsAT.service.CupsATMerchantService;
import com.hrtpayment.xpay.cupsAT.service.CupsATPayService;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Controller
@RequestMapping("xpay")
public class CupsATPayController {

	Logger logger = LogManager.getLogger();
	
	@Autowired
	CupsATPayService cupsPayService;
	
	@Autowired
	CupsATMerchantService cupsMerchantService;
	

 
	/**
	 * 接收银联返回的Controller
	 * 
	 * @param request
	 */
	@RequestMapping("cupalicallback")
	@ResponseBody
	public String cupAliCallBank(HttpServletRequest request){
		
		 Map<String,String> requestMap = new HashMap<String,String>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[银联-支付宝]异步通知-接收到的消息:"+requestMap);

		 try {
			cupsPayService.cupsAliCallBack(requestMap);
		} catch (BusinessException e) {
		    logger.info("[银联-支付宝]异步通知处理异常{}",e.getMessage());
		}
		 return "SUCCESS";
	}
	
	
	
	
	
	/**
	 * 接收银联返回的Controller
	 * 
	 * @param request
	 * @return 
	 */
	@RequestMapping("cupwxcallback")
	@ResponseBody
	public String cupWxCallBank(HttpServletRequest request){
		
		
		try {
			BufferedReader br= request.getReader();
			String str, wholeStr = "";
			while ((str = br.readLine()) != null) {
				wholeStr += str;
			}
 			logger.info("[银联-微信]异步通知-接收到的消息:{}",wholeStr);
 			cupsPayService.cupsWxCallBack(wholeStr);
		} catch (IOException e) {
			 logger.info("[银联-微信]异步通知  接收异常{}", e);
		} catch (BusinessException e) {
			 logger.info("[银联-微信]异步通知  处理异常{}", e);
		}
		
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><xml><return_code>SUCCESS</return_code><return_msg>OK</return_msg></xml>";
	}

	@RequestMapping("cupUpdatesAliMerchant")
	@ResponseBody
	public void cupUpdatesAliMerchant(HttpServletRequest request){
		 Map<String,String> requestMap = new HashMap<String,String>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[和融通-特殊商户报备]特殊商户通知信息  -接收到的消息:{}",requestMap);

		 if ("".equals(requestMap.get("merchantid"))) {
			 logger.info("[银联-支付宝]商户入驻修改    处理异常 merchantid 为空{}");
			return ;
		 }
		 try {
			 cupsMerchantService.updateAliMerchants(requestMap.get("merchantid")) ;
		} catch (BusinessException e) {
		    logger.info("[银联-支付宝]商户入驻修改    处理异常{}",e.getMessage());
		}
	}

	
//	@RequestMapping("cupAliClosed")
//	@ResponseBody
//	public void cupAliClosed(@RequestParam String orderid){
//		 try {
//			 cupsPayService.cupsAliClosed(orderid);
//		} catch (BusinessException e) {
//		    logger.info("[银联-支付宝]订单关闭    处理异常{}",e.getMessage());
//		}
//	}
	
//	@RequestMapping("cupAliCancel")
//	@ResponseBody
//	public void cupAliCancel(@RequestParam String orderid,@RequestParam String channel_id){
//		 try { 
//			 cupsPayService.cupsAliCancel(orderid,channel_id);
//		} catch (BusinessException e) {
//		    logger.info("[银联-支付宝]订单撤销    处理异常{}",e.getMessage());
//		}
//	}
	
	
	
//	@RequestMapping("cupWxCancel")
//	@ResponseBody
//	public void cupWxCancel(@RequestParam String bankmid, @RequestParam String orderid,String mchId,String channelId){
//		 try {
//			 cupsPayService.cupsWxCancel (bankmid,orderid,mchId,channelId);
//		} catch (BusinessException e) {
//		    logger.info("[银联-支付宝]订单关闭    处理异常{}",e.getMessage());
//		}
//	}
//	
//	@RequestMapping("cupWxClose")
//	@ResponseBody
//	public void cupWxClose(@RequestParam String bankmid,@RequestParam String orderid){
//		 try { 
//			 cupsPayService.cupsWxClose(bankmid, orderid);
//		} catch (BusinessException e) {
//		    logger.info("[银联-支付宝]订单撤销    处理异常{}",e.getMessage());
//		}
//	}
	
}
