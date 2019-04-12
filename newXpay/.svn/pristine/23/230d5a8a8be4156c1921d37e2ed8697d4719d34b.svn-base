package com.hrtpayment.xpay.quickpay.common.controller;


import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.channel.service.ChannelService;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.quickpay.common.service.ManageQuickPayService;
/**
 * 和融通快捷支付入口 APP传入参数有：卡号、mid、手机号、金额 根据规则判断走哪个快捷支付通道
 * 
 * @author songbeibei 2017-10-26
 */
@Controller
@RequestMapping("xpay")
public class ManageQuickPayController {

	Logger logger = LogManager.getLogger();

	@Autowired
	MerchantService merService;
	@Autowired
	ManageService manageService;
	@Autowired
	ManageQuickPayService service;
	@Autowired
	ChannelService ch;
	
	/**
	 * 
	 * 商户入网修改
	  * @param request
	  */
	@RequestMapping("quickPayMerchantUpdate")
	@ResponseBody
	public  String updateMerchantFor(@RequestBody String content )throws IOException{ 
		logger.info("[商户入网信息修改]接收到的消息:" + content );
		service.manageUpdate(content); 
		return "SUCCESS";
	}
    	
	
	/**
	 * 
	 * 商户入网修改
	  * @param request
	  */
	@RequestMapping("quickPayMerchantUpdateForBatch")
	@ResponseBody
	public  String updateMerchantForBatch(@RequestParam String mid, @RequestParam String fiid)throws IOException{
		
		logger.info("[快捷支付商户入网批量信息修改]接收到的消息:" + mid );
			String[] mids = mid.split(",");
			for (String smid : mids) { 
				try {
					service.manageUpdateForBatch(smid, fiid);
				} catch (Exception e) {
					logger.error("[快捷支付商户入网批量信息修改]商户{}信息修改 操作异常：{}",smid,e.getMessage());
				}
			} 	
	
		return "SUCCESS";
	}
    	
}
