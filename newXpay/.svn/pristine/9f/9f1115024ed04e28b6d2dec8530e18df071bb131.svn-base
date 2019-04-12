package com.hrtpayment.xpay.cib.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.cib.bean.CibMsg;
import com.hrtpayment.xpay.cib.service.CibPayService;
import com.hrtpayment.xpay.utils.exception.BusinessException;

/**
 * 兴业银行-威富通接口
 * @author lvjianyu
 *
 */
@Controller
@RequestMapping(value={"xpay"})
public class CibPayController {
	Logger logger = LogManager.getLogger();
	
	@Autowired
	CibPayService cibService;
	
	@RequestMapping("hrtcibcallback")
	@ResponseBody
	public String callback(@RequestBody String formStr) {
		logger.info("兴业银行异步通知:"+formStr);
		CibMsg bean = CibMsg.parseXmlFromStr(formStr);
		String rtn=cibService.callback(bean);
		return rtn;
	}

	
	@RequestMapping("cibdownload")
	@ResponseBody
	public String download(String date) {
		String rtn="";
		try {
			rtn = cibService.downloadDzFile(date);
		} catch (BusinessException e) {
			logger.info("兴业银行下载对账文件错误:"+e.getMessage());
		}
		return rtn;
	}
}
