package com.hrtpayment.xpay.quickpay.common.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.ManageService;

/**
 * 快捷支付
 * 
 * @author songbeibei 2017年10月26日
 */
@Service
public class ManageQuickPayService {
	Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao;
	
	
	@Autowired
	ManageService manageService;
	
	@Value("${quick.merccbid}")
	private String merchantCcbId;
	@Value("${quick.limitamt}")
	private double limitamt;

	 
	public void  manageUpdate(String merInfo) {
		JSONObject merInfoJson= JSONObject.parseObject(merInfo);
		
	}
	
	
	
	
	public void  manageUpdateForBatch(String mid,String fiid) {
		
	}
	
}
