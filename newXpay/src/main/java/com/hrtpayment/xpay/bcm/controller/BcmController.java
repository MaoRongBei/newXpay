package com.hrtpayment.xpay.bcm.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.bcm.service.BcmPayService;

/**
 * @author lvjianyu 交通银行 扫码 被扫 2017-11-07
 */
@Controller
@RequestMapping("xpay")
public class BcmController {
	private static final Logger logger = LoggerFactory.getLogger(BcmController.class);

	@Autowired
	BcmPayService service;
	
	
	/**
	 * 处理[交易|退款]异步通知
	 * ：交通银行异步通知为json形式
	 * @throws IOException 
	 */
	@ResponseBody
	@RequestMapping("bcmPayCallBack")
	public String dealCallBack(HttpServletRequest request) throws IOException {
		BufferedReader br = request.getReader();
		String str, wholeStr = "";
		while ((str = br.readLine()) != null) {
			wholeStr += str;
		}
		@SuppressWarnings("unchecked")
		Map<String, String> requestMap = JSONObject.toJavaObject(JSONObject.parseObject(wholeStr), Map.class);

		logger.info("[交通银行]-异步通知 接收到的消息:" + requestMap);
		JSONObject json = new JSONObject();
		json.put("rspCode", "SUCCESS");
		try {
			String noticeType = requestMap.get("noticeType"); //1交易通知 2退货通知 3撤销通知
			if("1".equals(noticeType)){
				service.payCallBack(requestMap);
			}else if("2".equals(noticeType)){
				service.refundCallBack(requestMap);
			}else{
				logger.error("[交通银行]-异步通知未处理，noticeType:{}" + noticeType);
			}
		} catch (Exception e) {
			logger.error("[交通银行]-异步通知处理失败，{}" + e);
			json.put("rspCode", "FAIL");
		}
		return json.toJSONString();
	}

}
