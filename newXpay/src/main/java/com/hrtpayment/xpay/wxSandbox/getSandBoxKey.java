package com.hrtpayment.xpay.wxSandbox;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hrtpayment.xpay.common.service.impl.NettyClientService;
import com.hrtpayment.xpay.cupsAT.service.CupsATService;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.HttpConnectService;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Controller
@RequestMapping("xpay")
public class getSandBoxKey {
	@Autowired
	CupsATService cupsService;
	@Autowired
	NettyClientService netty;

	private final Logger logger = LogManager.getLogger();
	
	
	@RequestMapping("getKey")
	public void getSandBoxKey(){
		Map<String, String> map= new HashMap<String,String>();
		map.put("mch_id", "1507834131");
		map.put("nonce_str", cupsService.getRandomString(32));//随机字符串
		Map<String, String> req=cupsService.sign(map);
		String reqStr="";
		 logger.info("[银联-微信]获取沙箱秘钥：{}",req);
		try {
		     reqStr=cupsService.mapToXml(req) ;
		} catch (Exception e1) {
			 logger.info("[银联-微信]获取沙箱秘钥 请求报文转化异常：{}",e1.getMessage());
			 RedisUtil.addFailCountByRedis(1); 
		}
		String res=null;
		try { 
	 		res = netty.sendFormData("https://api.mch.weixin.qq.com/sandboxnew/pay/getsignkey", reqStr); 
			logger.info("[银联-微信]获取沙箱秘钥返回报文：{}",res);
		} catch (Exception e) {
			 
			RedisUtil.addFailCountByRedis(1); 
		} 
		
	}
	
}
