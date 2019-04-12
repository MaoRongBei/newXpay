package com.hrtpayment.xpay.common.service.impl;

import java.io.IOException;
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
import com.alipay.api.FileItem;
import com.alipay.api.domain.AntMerchantExpandIndirectActivityConfirmModel;
import com.alipay.api.domain.AntMerchantExpandIndirectActivityCreateModel;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AntMerchantExpandIndirectActivityConfirmRequest;
import com.alipay.api.request.AntMerchantExpandIndirectActivityCreateRequest;
import com.alipay.api.request.AntMerchantExpandIndirectImageUploadRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AntMerchantExpandIndirectActivityConfirmResponse;
import com.alipay.api.response.AntMerchantExpandIndirectActivityCreateResponse;
import com.alipay.api.response.AntMerchantExpandIndirectImageUploadResponse;
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
 
	public String getPubaccPayUrl(int fiid, String orderid, String isCredit) {

		AlipayService ali = getAlipayService(fiid);
		String codeUrl = String.format("%s/xpay/alipay_%s_%s_%s%s", host, ali.getAlipayFiid(), orderid,isCredit,host2);
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
	
	
	
	public  Map<String, Object> uploadImage( FileItem image) throws BusinessException{
		    
			AlipayClient alipayClient = new DefaultAlipayClient ("https://openapi.alipay.com/gateway.do",
					"2018062660432156", privateKey, "json", "utf-8", publicKey, "RSA2");
			AntMerchantExpandIndirectImageUploadRequest  request = new AntMerchantExpandIndirectImageUploadRequest();
		  
		  
			AntMerchantExpandIndirectImageUploadResponse imageUploadResponse =null;
			try {
				  request.setImageContent(image);
				request.setImageType((image.getMimeType()+"").split("/")[1]);
				
				imageUploadResponse=alipayClient.execute(request);
			} catch (AlipayApiException e) {
				logger.error("支付宝蓝海上传图片异常： "+e.getErrMsg());
				throw new BusinessException(9001, "支付宝蓝海上传图片");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 logger.error(imageUploadResponse.getBody());
			 Map<String, Object>  rtnMap=new HashMap<String, Object>();
			 if ("10000".equals(imageUploadResponse.getCode())) {
				 rtnMap.put("status", "S");
				 rtnMap.put("msg", "上送成功");
				 rtnMap.put("imgID", imageUploadResponse.getImageId() );
			 }else{
				 rtnMap.put("status", "E");
				 rtnMap.put("msg", imageUploadResponse.getMsg());
				 //rtnMap.put("imgID", imageUploadResponse.getImageId() );
			 }
			 
		return rtnMap;
		
	}
	   /**
     * 
     * ant.merchant.expand.indirect.activity.create
     * 
     * 间连商户活动报名
     * 
     * @param merInfo
     * @return
     * @throws BusinessException
     */
	public Map<String, Object> activityCreate(Map<String, Object> merInfo) throws BusinessException{
	    AlipayClient alipayClient=new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
				"2018062660432156", privateKey, "json", "utf-8", publicKey, "RSA2");
	    AntMerchantExpandIndirectActivityCreateRequest request=new AntMerchantExpandIndirectActivityCreateRequest();
	    AntMerchantExpandIndirectActivityCreateModel createModel=new AntMerchantExpandIndirectActivityCreateModel();
	    createModel.setSubMerchantId(merInfo.get("merchantcode")+"");
	    createModel.setActivityType(merInfo.get("bs_activity_type")+"");
	    createModel.setName(merInfo.get("merchantname")+"");
	    createModel.setAliasName(merInfo.get("shortname")+"");
	    createModel.setCheckstandPic(merInfo.get("bs_checkstand_pic")+"");//收银台照片
	    createModel.setShopEntrancePic(merInfo.get("bs_shop_entrance_pic")+"");//门头照
	    createModel.setBusinessLicensePic(merInfo.get("bs_business_license_pic")+"");//营业执照
	    createModel.setIndoorPic(merInfo.get("bs_indoor_pic")+"");//店内环境照
	    createModel.setSettledPic(merInfo.get("bs_settled_pic")+"");//主流餐饮平台入驻证明（任选一个即可）大众点评、美团、饿了么、口碑、百度外卖餐饮平台商户展示页面。 
	    request.setBizModel(createModel);
	    logger.info("request：{}",request.getBizModel());
	    AntMerchantExpandIndirectActivityCreateResponse createResponse=null;
	    try {
	    	createResponse=alipayClient.execute(request);
			logger.info("resp：{}",createResponse.getBody());
		} catch (AlipayApiException e) {
			logger.error("支付宝蓝海间连商户活动报名异常： "+e);
			throw new BusinessException(9001, "支付宝蓝海间连商户活动报名失败");
		}
	    
	     Map<String, Object>  rtnMap=new HashMap<String, Object>();
		 if ("10000".equals(createResponse.getCode())) {
			 rtnMap.put("status", "S");
			 rtnMap.put("msg", "报名成功");
			 rtnMap.put("order_id", createResponse.getOrderId());
		 }else{
			 rtnMap.put("status", "E");
			 rtnMap.put("msg", createResponse.getSubCode()+createResponse.getMsg()+createResponse.getSubMsg());
		 }
		return  rtnMap;
	}
	
	/**
	 * ant.merchant.expand.indirect.activity.confirm
	 * @param respCode
	 * @param respMsg
	 * @return
	 */
	public Map<String, Object> activityConfirm(Map<String, Object> merInfo) throws BusinessException{
	    AlipayClient alipayClient=new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
				"2018062660432156", privateKey, "json", "utf-8", publicKey, "RSA2");
	    AntMerchantExpandIndirectActivityConfirmRequest request=new AntMerchantExpandIndirectActivityConfirmRequest();
	    AntMerchantExpandIndirectActivityConfirmModel confirmModel=new AntMerchantExpandIndirectActivityConfirmModel();
	    confirmModel.setOrderId(merInfo.get("bs_activity_orderid")+"");//活动报名id
	    request.setBizModel(confirmModel);
	    AntMerchantExpandIndirectActivityConfirmResponse confirmResponse=null;
	    try {
	    	confirmResponse=alipayClient.execute(request);
	    	logger.error("支付宝蓝海商户活动确认返回： "+confirmResponse.getBody());
		} catch (AlipayApiException e) {
			logger.error("支付宝蓝海商户活动确认异常： "+e);
			throw new BusinessException(9001, "支付宝蓝海商户活动确认失败");
		}
	    Map<String, Object>  rtnMap=new HashMap<String, Object>();
		 if ("10000".equals(confirmResponse.getCode())) {
			 rtnMap.put("status", "S");
			 rtnMap.put("msg", "活动确认成功");
			 rtnMap.put("order_id", confirmResponse.getParams());
		 }else if ("40004".equals(confirmResponse.getCode())&&"ORDER_HAS_CONFIRMED".equals(confirmResponse.getSubCode())) {
			 rtnMap.put("status", "S");
			 rtnMap.put("msg", "活动确认成功");
			 rtnMap.put("order_id", confirmResponse.getParams());
		 }else{
			 rtnMap.put("status", "E");
			 rtnMap.put("msg", confirmResponse.getSubCode()+confirmResponse.getMsg()+confirmResponse.getSubMsg());
			 //rtnMap.put("imgID", imageUploadResponse.getImageId() );
		 }
		return  rtnMap;
	}
	
	
	
	
	public String getJsPayInfo(int fiid, String orderid, String openid,String userid,String iscredit) {
		return getAlipayService(fiid).getAlipayPayInfo(orderid, openid,userid,iscredit);
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
