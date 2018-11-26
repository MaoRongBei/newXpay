package com.hrtpayment.xpay.cups.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.cups.service.CupsXwPayService;

@Controller
@RequestMapping("xpay")
public class CupsXwPayController {

	Logger logger = LogManager.getLogger();
	@Autowired
	CupsXwPayService cupsXwPayService;

	/**
	 * 接收银联返回的Controller
	 * 
	 * @param request
	 */
	@RequestMapping("cupxwcallback")
	@ResponseBody
	public String cupXwCallBank(HttpServletRequest request) {
		BufferedReader in;
		StringBuilder jsonNotifyStr = new StringBuilder();
		try {
			in = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
				jsonNotifyStr.append(line);
			}
			logger.info("[银联小微商户]接收到的消息:{}",jsonNotifyStr.toString());
		} catch (IOException e) {
			logger.info("[银联小微商户]接收信息有误:{}",e.getMessage());
		}
		cupsXwPayService.callBack(jsonNotifyStr.toString());
		return "ok";
	}

//	/**
//	 * 批量获取支付码
//	 * 
//	 * @param count
//	 */
//	@RequestMapping("getBatchQrcode")
//	@ResponseBody
//	public void getBatchQrcode(@RequestParam Integer count) {
//		try {
//			String rString = cupsXwPayService.getBatchQrcode(count);
//			logger.info("批量生成的付款码：{}", rString);
//		} catch (BusinessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	@RequestMapping("queryBatchQrcode")
//	@ResponseBody
//	public void queryBatchQrcode(@RequestParam String batchNo) {
//		try {
//			String rString = cupsXwPayService.queryBatchQrcode(batchNo);
//			logger.info("批量生成的付款码：{}", rString);
//		} catch (BusinessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	@RequestMapping("sendMessageCode")
//	@ResponseBody
//	public void sendMessageCode(@RequestParam String phoneNo) {
//		try {
//			String rString = cupsXwPayService.sendMessageCode(phoneNo);
//			logger.info("发送短信 获取验证码：{}", rString);
//		} catch (BusinessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	@RequestMapping("bindCode")
//	@ResponseBody
//	public void bindCode(@RequestParam String qrcode, @RequestParam String smsId,@RequestParam String smsCode) {
//		try {
//			String rString = cupsXwPayService.bindCode(qrcode, "18945296532", smsId, "6222600910064035874",//6222600910064035874
//					"320381198909230074", "陈国承", "301290000007",smsCode);
//			logger.info("绑定二维码：{}", rString);
//		} catch (BusinessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	@RequestMapping("imgUpload")
//	@ResponseBody
//	public void imgUpload(String merId, String imgType, String imgPath) {
//		try {
//			String rString = cupsXwPayService.imgUpLoad(merId, imgType, "D:\\u01\\upload\\11.png");
//			logger.info("上传照片：{}", rString);
//		} catch (BusinessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
