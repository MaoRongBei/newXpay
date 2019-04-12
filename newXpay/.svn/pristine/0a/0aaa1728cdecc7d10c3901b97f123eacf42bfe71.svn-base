package com.hrtpayment.xpay.common.service.impl;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.AlipayService;
import com.hrtpayment.xpay.utils.UrlCodec;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

@Service
public class AliJspayService {

	Logger logger = LogManager.getLogger();
	@Autowired
	private JdbcDao dao;
	@Autowired
	NettyClientService client;
	@Resource(name="bcmPayService")
	AlipayService bcm;
	@Resource(name="cupsATPayService")
	AlipayService cups;
	@Resource(name="netCupsPayService")
	AlipayService netCups;
	@Value("${xpay.host}")
	private String host;
	@Value("${alipay.hrt.privateKey}")
	private String privateKey;
	
	@Value("${alipay.publicKey}")
	private String publicKey;
	
	@Value("${xpay.host2}")
	private String host2;
 
	public String getPubaccPayUrl(int fiid, String orderid, String mch_id) {

		AlipayService ali = getAlipayService(fiid);
		String codeUrl = String.format("%s/xpay/alipay_%s_%s%s", host, ali.getAlipayFiid(), orderid,host2);
        /**
         * 支付宝沙箱
         */
//		String authUrl = "https://openauth.alipaydev.com/oauth2/publicAppAuthorize.htm?app_id=%s&scope=auth_base&redirect_uri=%s";
 		/**
 		 * 支付宝正式
 		 */
		String appid="";
//		if (fiid==54||fiid==61) {
//			appid=mch_id;
//		}else{
			appid=ali.getAliAppid();
//		}
		String authUrl = "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?app_id=%s&scope=auth_base&redirect_uri=%s";
		String url = String.format(authUrl, appid, UrlCodec.encodeWithUtf8(codeUrl));
		return url;
	}
	
	public Map<String, String> getOpenid(int fiid,String code,String orderid) throws BusinessException{
		AlipayService ali = getAlipayService(fiid);
		String appid="";
//		if (fiid==54) {
//			appid=cups.getAliAppidByOrder(orderid);
//		}else if (fiid==61) {
//			appid=netCups.getAliAppidByOrder(orderid);
//		}else{
			appid=ali.getAliAppid();
//		}
		
		//2018040202490999"https://openapi.alipaydev.com/gateway.do",沙箱 "https://openapi.alipaydev.com/gateway.do",//
		//生产https://openapi.alipay.com/gateway.do
		AlipayClient alipayClient = new DefaultAlipayClient ("https://openapi.alipay.com/gateway.do",
				appid, privateKey, "json", "utf-8", publicKey, "RSA2");
		AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
		request.setCode(code);
		request.setGrantType("authorization_code");
		AlipaySystemOauthTokenResponse oauthTokenResponse =null;
		try {
			oauthTokenResponse=alipayClient.execute(request);
		} catch (AlipayApiException e) {
			logger.error("支付宝获取token异常： "+e);
			throw new BusinessException(9001, "支付宝下单失败");
		}
		Map< String, String> usrInfo =new HashMap<String,String>();
		usrInfo.put("authcode", oauthTokenResponse.getAccessToken());
		usrInfo.put("userid", oauthTokenResponse.getUserId());
		return usrInfo;
	 
	}
	
	public String getJsPayInfo(int fiid, String orderid, String openid,String userid) {
		return getAlipayService(fiid).getAlipayPayInfo(orderid, openid,userid);
	}
	private AlipayService getAlipayService (int fiid) {
	    if (fiid == 43) {
			return bcm;
		} else   if (fiid == 54) {
			return cups;
		} else   if (fiid == 61) {
			return netCups;
		} else{
			throw new HrtBusinessException(9009,"指定通道未开通");
		}
	}

}
