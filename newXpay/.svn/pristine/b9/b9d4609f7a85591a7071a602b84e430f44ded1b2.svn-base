package com.hrtpayment.xpay.quickpay.ldpay.controller;

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

import com.hrtpayment.xpay.quickpay.ldpay.service.LdPayCashService;
import com.hrtpayment.xpay.quickpay.ldpay.service.LdPayServerServce;
import com.hrtpayment.xpay.quickpay.ldpay.service.LdPayService; 
import com.hrtpayment.xpay.utils.exception.BusinessException;

/**
 * 联动优势快捷支付
 * @author xuxiaoxiao
 *
 */
@Controller
@RequestMapping("xpay")
public class LdQuickPayController {

	Logger logger = LogManager.getLogger();
	
	@Autowired 
	LdPayService  ldPayService;
	@Autowired 
	LdPayServerServce  serverService;
	
	@Autowired 
	LdPayCashService ldPayCashService;
	
	@RequestMapping("ldqkpay")
	@ResponseBody
	public void ldQuickPay(@RequestParam String formStr){
 
		try {
//			ldPayService.authPay("HRT2017120600000002","0.01","CMB");
//			ldPayService.getMessage("HRT2017120600000001", "18945296530", "443", "1224", "6225768763161211", "IDENTITY_CARD", "23232419930107092X", "宋贝贝");
			Map<String, Object> map=new HashMap<String, Object>();
			map.put("MER_ORDERID", "pk2017120616512351311418");
			ldPayService.queryQuickPay(map);
		} catch (BusinessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@RequestMapping("ldquerybalance")
	@ResponseBody
	public void ldQueryBalance(){
		try {
			ldPayCashService.queryBalance();
		} catch (BusinessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@RequestMapping("ldqkpayquery")
	@ResponseBody
	public void ldQuickPayQuery(@RequestParam String formStr){
 
		try {
//			ldPayService.authPay("HRT2017120600000002","0.01","CMB");
//			ldPayService.getMessage("HRT2017120600000001", "18945296530", "443", "1224", "6225768763161211", "IDENTITY_CARD", "23232419930107092X", "宋贝贝");
			Map<String, Object> map=new HashMap<String, Object>();
			map.put("CASH_ORDERID", "pk2017122217001480376732");
			map.put("TXNTIME", "20171222");
			map.put("MER_ID", "864000353110159");
			map.put("TXNAMT", "7.31");
			
			ldPayCashService.queryCashQuickPay(map);//.queryQuickPay(map);
		} catch (BusinessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	@RequestMapping("ldqkpaycash")
	@ResponseBody
	public void ldQuickPayCash(@RequestParam String formStr){
 
		try {
//			ldPayService.authPay("HRT2017120600000002","0.01","CMB");
//			ldPayService.getMessage("HRT2017120600000001", "18945296530", "443", "1224", "6225768763161211", "IDENTITY_CARD", "23232419930107092X", "宋贝贝");
//			Map<String, Object> map=new HashMap<String, Object>();
//			map.put("MER_ORDERID", "pk2017120616512351311418");
//			ldPayService.queryQuickPay(map);
			
			ldPayCashService.cashQuickPay("54020");//, "宋贝贝","18945296530"
		} catch (BusinessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/**
	 * 接收联动优势快捷支付异步通知
	 * 
	 * @param request
	 * @throws BusinessException 
	 */
	@RequestMapping("ldPayCallback")
	@ResponseBody
	public String ldPayCallBack(HttpServletRequest request) throws BusinessException{
		Map<String, String> requestMap = null;
		 String orderid="";

		 try {
			 requestMap = serverService.parseRequest2JsonStr(request);
			 logger.info("[联动优势快捷支付]异步通知 接收到的消息:{}" , requestMap);
			 orderid=ldPayService.ldPayCallback(requestMap);
			
		} catch (BusinessException e) {
			logger.info("[联动优势快捷支付]异步通知处理异常:{}" , e.getMessage());
		}catch (Exception e) { 
			logger.info("[联动优势快捷支付]异步通知处理异常:{}" , e.getMessage());
		}
		 if ("".equals(orderid)||null==orderid||"null".equals(orderid)) {
			 logger.info("[联动优势快捷支付]异步通知未接收到订单号");
			 return "";
		}else{
			return ldPayService.rtnCallBack(orderid);
		} 
		  
	}
	
	
	/**
	 * 接收联动优势快捷支付异步通知
	 * 
	 * @param request
	 * @throws BusinessException 
	 */
	@RequestMapping("ldPayCashCallback")
	@ResponseBody
	public String ldPayCashCallback(HttpServletRequest request) throws BusinessException{
		Map<String, String> requestMap = null;
		 String orderid="";
		 try {
			 requestMap = serverService.parseRequest2JsonStr(request);
			 logger.info("[联动优势快捷支付]提现异步通知 接收到的消息:{}" , requestMap);
			 orderid=ldPayCashService.ldPayCallback(requestMap);
		} catch (BusinessException e) {
			logger.info("[联动优势快捷支付]提现异步通知处理异常:{}" , e.getMessage());
		}catch (Exception e) { 
			logger.info("[联动优势快捷支付]提现异步通知处理异常:{}" , e.getMessage());
		}
		 if ("".equals(orderid)||null==orderid||"null".equals(orderid)) {
			 logger.info("[联动优势快捷支付]异步通知未接收到订单号");
			 return "";
		}else{
			return ldPayService.rtnCallBack(orderid);
		} 
		  
	}
}
