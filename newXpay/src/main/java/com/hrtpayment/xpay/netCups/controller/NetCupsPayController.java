package com.hrtpayment.xpay.netCups.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal; 
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

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.netCups.service.NetCupsMerchantService;
import com.hrtpayment.xpay.netCups.service.NetCupsPayService;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Controller
@RequestMapping("xpay")
public class NetCupsPayController {

	Logger logger = LogManager.getLogger();
	
	@Autowired
	NetCupsPayService netCupsPayService;
	
	@Autowired
	NetCupsMerchantService netCupsMerchantService;
	
  
	/**
	 *  网联 -- 支付宝 异步通知接口 
	 * @param request
	 */
	@RequestMapping("netcupalicallback")
	@ResponseBody
	public void netCupAliCallBank(HttpServletRequest request){
		
		 Map<String,String> requestMap = new HashMap<String,String>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[网联-支付宝]异步通知-接收到的消息:"+requestMap);

		 try {
			 netCupsPayService.cupsAliCallBack(requestMap);
		} catch (BusinessException e) {
		    logger.info("[网联-支付宝]异步通知处理异常{}",e.getMessage());
		}
	}
	
	
	
	
	
	/**
	 * 网联 -- 微信 异步通知接口 
	 * @param request
	 * @return 
	 */
	@RequestMapping("netcupwxcallback")
	@ResponseBody
	public String netCupWxCallBank(HttpServletRequest request){
		
		
		try {
			BufferedReader br= request.getReader();
			String str, wholeStr = "";
			while ((str = br.readLine()) != null) {
				wholeStr += str;
			}
 			logger.info("[网联-微信]异步通知-接收到的消息:{}",wholeStr);
 			netCupsPayService.cupsWxCallBack(wholeStr);
		} catch (IOException e) {
			 logger.info("[网联-微信]异步通知  接收异常{}", e);
		} catch (BusinessException e) {
			 logger.info("[网联-微信]异步通知  处理异常{}", e);
		}
		
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><xml><return_code>SUCCESS</return_code><return_msg>OK</return_msg></xml>";
	}

 
	
	@RequestMapping("netcupUpdatesAliMerchant")
	@ResponseBody
	public void netCupUpdatesAliMerchant(@RequestParam String hrid){
		 try {
			 netCupsMerchantService.updateAliMerchants(hrid) ;
		} catch (BusinessException e) {
		    logger.info("[银联-支付宝]商户入驻修改    处理异常{}",e.getMessage());
		}
	}
	
 
	
	
	@RequestMapping("netcupAliClosed")
	@ResponseBody
	public void netCupAliClosed(@RequestParam String orderid){
		 try {
			 netCupsPayService.cupsAliClosed(orderid);
		} catch (BusinessException e) {
		    logger.info("[银联-支付宝]订单关闭    处理异常{}",e.getMessage());
		}
	}
//	@RequestMapping("netcupWxCancel")
//	@ResponseBody
//	public void cupWxCancel(@RequestParam String bankmid, @RequestParam String orderid){
//		 try {
//			 cupsPayService.cupsWxCancel (bankmid,orderid);
//		} catch (BusinessException e) {
//		    logger.info("[银联-微信]订单撤销    处理异常{}",e.getMessage());
//		}
//	}
//	
//	@RequestMapping("netcupWxClose")
//	@ResponseBody
//	public void cupWxClose(@RequestParam String bankmid,@RequestParam String orderid){
//		 try { 
//			 cupsPayService.cupsWxClose(bankmid, orderid);
//		} catch (BusinessException e) {
//		    logger.info("[银联-支付宝]订单撤销    处理异常{}",e.getMessage());
//		}
//	}
	
	/**
	 * 
	 * 撤销 同笔交易可以 执行多次    但是只有第一次成功撤销 会进行退款
	 * 
	 * @param orderid
	 */
	@RequestMapping("netcupAliCancel")
	@ResponseBody
	public void netCupAliCancel(@RequestParam String orderid){
		 try { 
			 netCupsPayService.cupsAliCancel(orderid);
		} catch (BusinessException e) {
		    logger.info("[银联-支付宝]订单撤销    处理异常{}",e.getMessage());
		}
	}

}
