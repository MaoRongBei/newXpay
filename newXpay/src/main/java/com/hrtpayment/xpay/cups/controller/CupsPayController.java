package com.hrtpayment.xpay.cups.controller;

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
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Controller
@RequestMapping("xpay")
public class CupsPayController {

	Logger logger = LogManager.getLogger();
	
	@Autowired
	CupsPayService cupsPayService;
	
	/**
	 * 接收银联返回的Controller
	 * 
	 * @param request
	 */
	@RequestMapping("cupcallback")
	@ResponseBody
	public void cupCallBank(HttpServletRequest request){
		
		 Map<String,String> requestMap = new HashMap<String,String>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("接收到的消息:"+requestMap);

		 cupsPayService.updatePayCupsAsyncCallBack(requestMap);
	}
	
	
	/**
	 * 银联二维码(银行卡前置模式)被扫查询
	 * @param orderid
	 * @return
	 */
	@RequestMapping("cupsbsquery")
	@ResponseBody
	public String cupsBsQuery(@RequestParam String orderid ){
		String resp="";
		try {
			resp=cupsPayService.cupsPayQuery(orderid);
		} catch (BusinessException e) {
			logger.info("订单号"+orderid+":"+e.getMessage());
			JSONObject json = new JSONObject();
			json.put("errcode", "E");
			json.put("rtmsg", "查询异常，请重新查询！");
			resp=json.toJSONString();
		}
		return resp;
		
	}
	
	/**
	 * 银联二维码(银行卡前置模式)被扫查询
	 * @param orderid
	 * @return
	 */
	@RequestMapping("cupsBsQueryCups")
	@ResponseBody
	public String cupsBsQueryCups(@RequestParam String orderid ){
		String resp="";
		try {
			resp = cupsPayService.cupsPayQueryCups(orderid);
		} catch (BusinessException e) {
			logger.info("订单号"+orderid+":"+e.getMessage());
			JSONObject json = new JSONObject();
			json.put("errcode", "E");
			json.put("rtmsg", "查询异常，请重新查询！");
			resp=json.toJSONString();
		}
		return resp;
		
	}
	
	/**
	 * 银联二维码(银行卡前置模式)主扫查询
	 * @param orderid
	 * @return
	 */
	@RequestMapping("cupszsquery")
	@ResponseBody
	public String cupsZsQuery(@RequestParam String orderid ){
		String resp="";
		try {
			resp=cupsPayService.cupsZsShoukuanQuery(orderid);
		} catch (BusinessException e) {
			logger.info("订单号"+orderid+":"+e.getMessage());
			JSONObject json = new JSONObject();
			json.put("errcode", "E");
			json.put("rtmsg", "查询异常，请重新查询！");
			resp=json.toJSONString();
		}
		return resp;
		
	}
	
	/**
	 * 银联二维码(银行卡前置模式)撤销
	 * @param orderid
	 * @return
	 */
	@RequestMapping("cupsundo")
	@ResponseBody
	public String cupsUndo(@RequestParam String orderid ){
		String resp="";
		try {
			resp=cupsPayService.cupsUndo(orderid);
		} catch (BusinessException e) {
			logger.info("订单号"+orderid+":"+e.getMessage());
			JSONObject json = new JSONObject();
			json.put("errcode", "E");
			json.put("rtmsg", e.getMessage());
			resp=json.toJSONString();
		}
		return resp;
	}
	
	/**
	 * 银联二维码-冲正(被扫)
	 * @param orderid
	 * @return
	 */
	@RequestMapping("cupsBsReversal")
	@ResponseBody
	public String cupsBsReversal(@RequestParam String orderid ){
		String resp="";
		try {
			resp = cupsPayService.cupsBsReversal(orderid);
			logger.error("冲正订单号返回"+orderid+":"+resp);
		} catch (BusinessException e) {
			logger.error("冲正订单号异常"+orderid+":"+e.getMessage());
			JSONObject json = new JSONObject();
			json.put("errcode", "E");
			json.put("rtmsg", e.getMessage());
			resp=json.toJSONString();
		}
		return resp;
	}
	
}
