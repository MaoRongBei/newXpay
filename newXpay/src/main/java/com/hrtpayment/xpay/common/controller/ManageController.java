package com.hrtpayment.xpay.common.controller;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest; 
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.FileItem;
import com.hrtpayment.xpay.channel.service.ChannelService;
import com.hrtpayment.xpay.utils.DateUtil;
import com.hrtpayment.xpay.utils.HttpConnectService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.service.impl.BankMerchantService;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.utils.crypto.Md5Util;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;
/**
 * 内部管理接口
 * @author
 * 2016年11月18日
 */
@Controller
@RequestMapping("manage")
public class ManageController {
	private Logger logger = LogManager.getLogger();
	private String key = "hrt1234.";

	@Autowired
	ManageService service;
	@Autowired
	BankMerchantService bankMer;
	@Autowired
	NotifyService notify;
	@Autowired
	ChannelService ch;
	
	public String queryOrder(@RequestParam(required=false) String pwid,@RequestParam(required=false) String orderid){
		return pwid;
	}
	@RequestMapping("refund")
	@ResponseBody
	public String refund(@RequestParam String orderid,@RequestParam String oriOrderid,@RequestParam BigDecimal amount,@RequestParam String sign) {
		StringBuilder sb = new StringBuilder();
		sb.append("amount=").append(amount.toPlainString())
			.append("&orderid=").append(orderid).append("&oriOrderid=").append(oriOrderid).append("&key=")
			.append(key);
		if (!Md5Util.digestUpperHex(sb.toString()).equals(sign)){
			return "签名错误";
		}
		try{
			String result = service.refund(orderid, oriOrderid, amount);
			logger.info("{}退款结果:{}",orderid,result);
			return result;
		}catch(HrtBusinessException e) {
			logger.info("{}退款结果:{}",orderid,e.getMessage());
			return e.getMessage();
		}catch(BusinessException e) {
			logger.info("{}退款结果:{}",orderid,e.getMessage());
			return e.getMessage();
		}catch(RuntimeException e) {
			return e.getMessage();
		}
	}
	
	
	@RequestMapping("refundByPos")
	@ResponseBody
	public String refundByPos(@RequestParam String orderid,@RequestParam String oriOrderid,@RequestParam BigDecimal amount, @RequestParam String unno, @RequestParam String sign) {
		JSONObject jsonRes =new JSONObject();
		jsonRes.put("amount", amount);
		jsonRes.put("orderid", orderid);
		jsonRes.put("refundtime", DateUtil.getStringFromDate(new Date(),DateUtil.FORMAT_TRADETIME));
		Map<String,String> map=new HashMap<String,String>();
		map.put("amount", amount.toPlainString());
		map.put("orderid",orderid);
		map.put("oriOrderid",  oriOrderid);
		map.put("unno", unno);
		map.put("sign", sign);
		
		try{
			ch.checkChannelInfoForPos(map);
			String result = service.refund(orderid, oriOrderid, amount);
			logger.info("{}退款结果:{}",orderid,result);
			if (result.contains("errcode")) {
				JSONObject  jObject=JSONObject.parseObject(result) ;
				jsonRes.put("message",jObject.get("rtmsg"));
				jsonRes.put("status", jObject.get("errcode"));
			}else{
				jsonRes.put("message",result);
				jsonRes.put("status","E");
			}
			logger.info("{}退款结果:{}",orderid,jsonRes.toJSONString());
			return jsonRes.toJSONString();
		}catch(HrtBusinessException e) {
			logger.info("{}退款结果:{}",orderid,e.getMessage());
			jsonRes.put("message", e.getMessage());
			jsonRes.put("status", "E");
			return jsonRes.toJSONString();
		}catch(BusinessException e) {
			logger.info("{}退款结果:{}",orderid,e.getMessage());
			jsonRes.put("message", e.getMessage());
			jsonRes.put("status", "E");
			return jsonRes.toJSONString();
		}catch(RuntimeException e) {
			logger.info("{}退款结果:{}",orderid,e.getMessage());
			jsonRes.put("message", e.getMessage());
			jsonRes.put("status", "E");
			return jsonRes.toJSONString();
		}
	}
	
	@RequestMapping("refundqueryByPos")
	@ResponseBody
	public String refundQueryByPos(@RequestParam String orderid,@RequestParam BigDecimal amount) {
		JSONObject jsonRes =new JSONObject();
		try{
			
			jsonRes.put("orderid", orderid);
			jsonRes.put("amount", amount);
			jsonRes.put("refundtime", DateUtil.getStringFromDate(new Date(),DateUtil.FORMAT_TRADETIME));
			String result=service.refundQuery(orderid);
			if ("SUCCESS".equals(result)||"退款成功".equals(result)) {
				jsonRes.put("message","退款成功");
				jsonRes.put("status", "S"); 
			}else if ("DOING".equals(result)) {
				jsonRes.put("message","退款处理中");
				jsonRes.put("status", "R"); 
			}else{
				jsonRes.put("message","退款失败");
				jsonRes.put("status", "E");
			}
			logger.info("{}退款查询结果:{}",orderid,jsonRes.toJSONString());
			return jsonRes.toJSONString();
		}catch(HrtBusinessException e) {
			logger.info("{}退款查询结果:{}",orderid,e.getMessage());
			jsonRes.put("message", e.getMessage());
			jsonRes.put("status", "E");
			return jsonRes.toJSONString();
		}catch(BusinessException e) {
			logger.info("{}退款查询结果:{}",orderid,e.getMessage());
			jsonRes.put("message", e.getMessage());
			jsonRes.put("status", "E");
			return jsonRes.toJSONString();
		}catch(RuntimeException e) {
			logger.info("{}退款查询结果:{}",orderid,e.getMessage());
			jsonRes.put("message", e.getMessage());
			jsonRes.put("status", "E");
			return jsonRes.toJSONString();
		}
	}
	@RequestMapping("refundquery")
	@ResponseBody
	public String refundQuery(@RequestParam String orderid) {
		try{
			return service.refundQuery(orderid);
		}catch(HrtBusinessException e) {
			logger.info("{}退款查询结果:{}",orderid,e.getMessage());
			return e.getMessage();
		}catch(BusinessException e) {
			logger.info("{}退款查询结果:{}",orderid,e.getMessage());
			return e.getMessage();
		}catch(RuntimeException e) {
			return e.getMessage();
		}
	}
	@RequestMapping("queryorder")
	@ResponseBody
	public String queryorder(@RequestParam String orderid) {
		try{
			String status = service.queryOrder(orderid);
			logger.info("[订单查询]订单号{}，状态{}",orderid,status);
			return status;
		}catch(HrtBusinessException e) {
			logger.info("{}查询订单结果:{}",orderid,e.getMessage());
			return e.getMessage();
		}catch(RuntimeException e) {
			return e.getMessage();
		}
	}
	
	@RequestMapping("closeOrder")
	@ResponseBody
	public String closeOrder(@RequestParam String orderid) {
		try{
			String status = service.closeOrder(orderid);
			logger.info("[订单关闭]订单号{}，状态{}",orderid,status);
			return status;
		}catch(HrtBusinessException e) {
			logger.info("{}关闭订单结果:{}",orderid,e.getMessage());
			return e.getMessage();
		}catch(RuntimeException e) {
			return e.getMessage();
		}
	}
	
	@RequestMapping("cancelOrder")
	@ResponseBody
	public String cancelOrder(@RequestParam String orderid) {
		try{
			String status = service.cancelOrder(orderid);
			logger.info("[订单撤销]订单号{}，状态{}",orderid,status);
			return status;
		}catch(HrtBusinessException e) {
			logger.info("{}撤销订单结果:{}",orderid,e.getMessage());
			return e.getMessage();
		}catch(RuntimeException e) {
			return e.getMessage();
		}
	}
	
	@RequestMapping("/createmer")
	@ResponseBody
	public String createmer(@RequestBody String content){
		
		logger.info(content);
		String[] hrids = content.split(",");
		for (String hrid : hrids) {
			try { 
				bankMer.registerBankMer(hrid); 
			} catch (Exception e) {
				logger.info("{}查询订单结果:{}",e.getMessage());
			}
		}
		return "OK";
	}
	
	@RequestMapping("/uploadspecialimagemer")
	@ResponseBody
	public String uploadSpecialImageMer(HttpServletRequest request){//Param String merchantcode,@RequestParam byte[] image,@RequestParam String imageName,@RequestParam String  image_type
		 Map<String,Object> requestMap = new HashMap<String,Object>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[和融通-特殊商户报备]照片上送接口  -接收到的消息:{}");//,requestMap
		 String jsonMsg=bankMer.uploadImage(requestMap);
		 return  jsonMsg;
	}
	
	@RequestMapping("/applyspecialmerForQuarz")
	@ResponseBody
	public String applyspecialmerForQuarz(HttpServletRequest request){//Param String merchantcode,@RequestParam byte[] image,@RequestParam String imageName,@RequestParam String  image_type
		 Map<String,Object> requestMap = new HashMap<String,Object>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[和融通-特殊商户报备]特殊商户申请  -接收到的消息:{}",requestMap);

		 String jsonMsg=bankMer.applySpecialMer(requestMap);
	  
		 return  jsonMsg;
	}
	
	@RequestMapping("/applyspecialmer")
	@ResponseBody
	public String applyspecialmer(HttpServletRequest request){//Param String merchantcode,@RequestParam byte[] image,@RequestParam String imageName,@RequestParam String  image_type
		 Map<String,Object> requestMap = new HashMap<String,Object>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[和融通-特殊商户报备]特殊商户申请  -接收到的消息:{}",requestMap);

		 String jsonMsg=bankMer.applySpecialMer(requestMap);
	  
		 return  jsonMsg;
	}
	
	@RequestMapping("/confirmspecialmer")
	@ResponseBody
	public String confirmSpecialMer(HttpServletRequest request){//Param String merchantcode,@RequestParam byte[] image,@RequestParam String imageName,@RequestParam String  image_type
		 Map<String,Object> requestMap = new HashMap<String,Object>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[和融通-特殊商户报备]特殊商户确认  -接收到的消息:{}",requestMap);

		 String jsonMsg=bankMer.confirmSpecialMer(requestMap);
		 return  jsonMsg;
	}
	
	@RequestMapping("/activityCallback")
	@ResponseBody
	public String activityCallback(HttpServletRequest request){//Param String merchantcode,@RequestParam byte[] image,@RequestParam String imageName,@RequestParam String  image_type

		 Map<String,String> requestMap = new HashMap<String,String>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[和融通-特殊商户报备]特殊商户通知信息  -接收到的消息:{}",requestMap);
		 bankMer.applyCallBackMsg(requestMap);
		 return "SUCCESS";
	}
	

	@RequestMapping("/queryspecialstatus")
	@ResponseBody
	public String querySpecialStatus(HttpServletRequest request){//Param String merchantcode,@RequestParam byte[] image,@RequestParam String imageName,@RequestParam String  image_type
		 Map<String,Object> requestMap = new HashMap<String,Object>();
		 Enumeration<String> parameterEnum = request.getParameterNames();
		 
		 while(parameterEnum.hasMoreElements()){
			 String keyName = parameterEnum.nextElement();
			 requestMap.put(keyName, request.getParameter(keyName));
		 }
		 logger.info("[和融通-特殊商户报备]特殊商户报备状态查询  -接收到的消息:{}",requestMap);

		 String jsonMsg=bankMer.confirmSpecialMer(requestMap);
		 return  jsonMsg;
	}
	
	
	
	
	
	
	@RequestMapping("/createsubmer")
	@ResponseBody
	public String createsubmer(@RequestParam String hrid, @RequestParam String zdlx){
		logger.info("------------------>子商户配置进行步骤 【{}】操作的商户hrid：{}",zdlx,hrid);
		String[] hrids = hrid.split(",");
		for (String shrid : hrids) { 
			try {
			    bankMer.registerSubBankMer(shrid, zdlx); 
			} catch (Exception e) {
				logger.error("------------------>子商户配置进行步骤 【{}】操作的商户hrid：{}操作异常{}",zdlx,hrid,e.getMessage());
			}
		}
		return "OK";
	}
	
	@RequestMapping("/querymer")
	@ResponseBody
	public String querymer(@RequestParam String hrid){
		
		return bankMer.queryBankMer(hrid);
	}
	
	@RequestMapping("/queryIsCheckMer")
	@ResponseBody
	public String queryIsCheckMer(@RequestBody String content){
		logger.info(content);
		String[] hrids = content.split(",");
		for (String hrid : hrids) {
			try {
				bankMer.queryIsCheckMer(hrid); 
			} catch (Exception e) {
				logger.info("{}查询订单结果:{}",e.getMessage());
			}
		}
		return "受理成功";
	}
	
	
	@RequestMapping("/querysubmer")
	@ResponseBody
	public String querysubmer(@RequestParam String hrid){
		logger.info("------------------>进行子商户配置查询操作的商户hrid：{}",hrid);
		String[] hrids = hrid.split(",");
		for (String shrid : hrids) { 
			try {
				bankMer.querySubBankMer(shrid);
			} catch (Exception e) {
				logger.error("------------------>进行子商户配置查询操作的商户hrid：{}操作异常{}",hrid,e.getMessage());
			}
		}
		return "OK";
	}
	
	
	@RequestMapping("notify")
	@ResponseBody
	public String notify(@RequestParam String pwid) {
		logger.info("发送推送:{}",pwid);
		notify.sendNotify(pwid);
		return "OK";
	}

}
