package com.hrtpayment.xpay.common.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.cupsAT.sdk.WXPayConstants.SignType;
import com.hrtpayment.xpay.cupsAT.sdk.WXPayUtil;
import com.hrtpayment.xpay.cupsAT.service.CupsATMerchantService;
import com.hrtpayment.xpay.cupsAT.service.CupsATService;
import com.hrtpayment.xpay.netCups.service.NetCupsMerchantService;

@Service
public class BankMerchantService {
	
	@Autowired
	JdbcDao dao;
	@Autowired
	CupsATMerchantService cupsMerchantService;
	@Autowired
	NetCupsMerchantService netCupsMerchantService;
	@Autowired
	CupsATService cupsService;
	@Value("${cupsWx.mchId}")
	String mchid;
	@Value("${cupsWx.channelId}")
	String channelid;
	@Value("${cupsWx.md5Key}")
	String cupsWxMd5Key;
	@Autowired
	NettyClientService client;
	
	private final Logger logger = LogManager.getLogger();
	
	/**
	 * 商户入驻
	 * @param hrid
	 * @return
	 */
	public String registerBankMer(String hrid){
		
		String sql = "select * from bank_merregister where  hrid=?";
		List<Map<String, Object>> list = dao.queryForList(sql, hrid);
		if (list.size()<1) return "入驻商户hrid不存在";
		
		Map<String, Object> map = list.get(0);
		String approveStatus = (String) map.get("APPROVESTATUS");
		if ("Y".equals(approveStatus)) return "商户入驻状态为已成功";
		
		BigDecimal fiid = (BigDecimal) map.get("FIID");
		
		try {
			 if (fiid.intValue()==54) {
				return  cupsMerchantService.addAliMerchants(map);
			}else if (fiid.intValue()==53) {
				return  cupsMerchantService.addWxMerchants(map);
			}else if (fiid.intValue()==60) {
				return  netCupsMerchantService.addWxMerchants(map);
			}else if (fiid.intValue()==61) {
				return  netCupsMerchantService.addAliMerchants(map);
			}
		} catch (Exception e) {
			logger.error("入驻商户异常：",e);
		}
		return hrid;
	}
	
	/**
	 * 子商户配置
	 * @param hrid
	 * @param zdlx
	 * @return
	 */
	public String registerSubBankMer(String hrid,String zdlx) {
		try {
			String sql = "select * from bank_merregister where  hrid=? ";
			List<Map<String, Object>> list = dao.queryForList(sql, hrid);
			if (list.size()<1) {
				logger.info("------------------->商户hrid={}的商户不符合子商户配置要求",hrid);
				return "配置商户hrid不符合子商户配置要求";
			}
			Map<String, Object> map = list.get(0);
			String approveStatus =String.valueOf( map.get("APPROVESTATUS"));
			String fiid =String.valueOf( map.get("FIID"));
			String merchantCode=(String)map.get("MERCHANTCODE");
			
			if("Z".equals(approveStatus)){
				if ("1".equals(zdlx)) {
					String resultMsg="";
					if("53".equals(fiid)){
						cupsMerchantService.addSubMerchant(merchantCode, "1",map); 
					}if("60".equals(fiid)){
						netCupsMerchantService.addSubMerchant(merchantCode, "1",map); 
					}else{
						return merchantCode+":不符合更新微信配置要求";
					}
					logger.info("------------------->{}：配置授权目录 {}",merchantCode,resultMsg);
					return merchantCode+":配置授权目录 "+ resultMsg;
				}else{
					logger.info("------------------->{}：未进行授权目录操作。",merchantCode);
					return merchantCode+":未进行授权目录操作。";
				}
			}else if("M".equals(approveStatus)){
				if ("2".equals(zdlx)) {
					String resultMsg="";
					if("53".equals(fiid)){
						cupsMerchantService.addSubMerchant(merchantCode, "2",map); 
					}if("60".equals(fiid)){
						netCupsMerchantService.addSubMerchant(merchantCode, "2",map); 
					}else{
						return merchantCode+":不符合更新微信配置要求";
					}
					logger.info("------------------->{}：配置APPID {}",merchantCode,resultMsg);
					return merchantCode+":配置APPID "+resultMsg; 
				}else{
					logger.info("------------------->{}：未进行配置APPID操作。",merchantCode);
					return merchantCode+":未进行配置APPID操作。";
				}
			}else if("Y".equals(approveStatus)){
				if ("3".equals(zdlx)) {
					String resultMsg="";
					if("53".equals(fiid)){
						cupsMerchantService.addSubMerchant(merchantCode, "3",map); 
					}if("60".equals(fiid)){
						netCupsMerchantService.addSubMerchant(merchantCode, "3",map); 
					}else{
						return merchantCode+":不符合更新微信配置要求";
					}
					logger.info("------------------->{}：关注公众号操作 {}",merchantCode,resultMsg);
					return merchantCode+":关注公众号操作  "+resultMsg;  
				}else{
					logger.info("------------------->{}：关注公众号操作有误。",merchantCode);
					return merchantCode+":关注公众号操作有误。";
				}
				
			}else{
				return merchantCode+":不符合更新微信配置要求";
			}
		} catch (Exception e) {
			logger.error("微信更改商户配置异常：",e);
		}
	     return "";	
	}
	
	/**
	 * 商户入驻信息查询
	 * @param hrid
	 * @return
	 */
	public String queryBankMer(String hrid){	
		String sql = "select * from bank_merregister where hrid=?";
		List<Map<String, Object>> list = dao.queryForList(sql, hrid);
		if (list.size()<1) return "报备商户信息不存在";
			
		Map<String, Object> map = list.get(0);
		
		BigDecimal fiid = (BigDecimal) map.get("FIID");
		try {
			if (fiid.intValue()==54){
				return  cupsMerchantService.queryAliMerchants(String.valueOf(map.get("MERCHANTID")),fiid,String.valueOf(map.get("MERCHANTNAME")),String.valueOf(map.get("CHANNEL_ID")));
			}else if (fiid.intValue()==53){
				return  cupsMerchantService.queryWxMerchants(map);
			}else if (fiid.intValue()==60){
				return  netCupsMerchantService.queryWxMerchants(map);
			}else if (fiid.intValue()==61){
				return  netCupsMerchantService.queryAliMerchants(String.valueOf(map.get("MERCHANTID")),fiid,String.valueOf(map.get("MERCHANTNAME")),String.valueOf(map.get("MCH_ID")));
			}
		} catch (Exception e) {
			logger.error("查询入驻商户状态异常：",e);
		}
		return hrid;
	}
	
	/**
	 * 子商户配置   查询
	 * @param hrid
	 * @return
	 */
	public String  querySubBankMer(String hrid){
		
		try {
			String sql = "select * from bank_merregister where hrid=?";
			List<Map<String, Object>> list = dao.queryForList(sql, hrid);
			if (list.size()<1) {
				logger.info("------------------->不存在 hrid={}的商户",hrid);
				return "hrid对应的商户不存在";
			}
			Map<String, Object> map = list.get(0);
			String reqmsgid =String.valueOf( map.get("REQMSGID"));
			String fiid =String.valueOf(map.get("FIID"));
//			if ("11".equals(fiid)) {
//				return cmbcService.querySubMerchant(reqmsgid);
//			}
			/**
			 * 根据fiid向各自上游通道发起查询
			 */
			
		} catch (Exception e) {
			logger.error("厦门民生微信查询商户配置异常：",e);
		}
		return hrid;
	}

	
	
	/**
	 * 查询微信商户是否验证通过
	 * @param hrid
	 * @return
	 */
	public void queryIsCheckMer(String hrid) {
		try {
			String sql = "select * from bank_merregister where approvestatus='Y' and hrid=?";
			List<Map<String, Object>> list = dao.queryForList(sql, hrid);
			if (list.size()<1) {
				logger.info("------------------->不存在 hrid={}的商户或未入驻成功",hrid);
				return ;
			}
			Map<String, Object> bankMer=list.get(0);
			Map<String, String> subMer = new HashMap<String, String>();
			String reqUrl="";
			subMer.put("mch_id", mchid);
			subMer.put("sub_mch_id", String.valueOf(bankMer.get("MERCHANTCODE")));
			subMer.put("channel_id", channelid);
			subMer.put("sign_type", "HMAC-SHA256");
			subMer.put("nonce_str",  cupsService.getRandomString(32));
			reqUrl="https://api.mch.weixin.qq.com/mchrisk/bankquerymchauditinfo"; 
			String reqStr =WXPayUtil.generateSignedXml(subMer, cupsWxMd5Key, SignType.HMACSHA256);
			String res = null;
			try {
				logger.info("[银联-微信]商户认证查询请求信息----->" + reqStr);
				res = client.sendFormData(reqUrl, reqStr);
				logger.info("[银联-微信]商户认证查询响应信息----->" + res);
			} catch (Exception e) {
				logger.info("[银联-微信]商户{}:{}认证查询异常，错误 原因{}----->", bankMer.get("HRID"), bankMer.get("MERCHANTID"), e.getMessage());
				return ;
			}
			Map<String, String> respJson=null;
			try {
				respJson = cupsService.xmlToMap(res);
			} catch (Exception e) {
				 logger.info("[银联-微信]商户入驻查询    响应报文转化异常：{}",e.getMessage());
				 return ;
			}
			if ("SUCCESS".equals(respJson.get("return_code"))) {
				String rtnCode = respJson.get("result_code"); 
				String resmsg=(String)bankMer.get("RTNMSG");
				if ("SUCCESS".equals(rtnCode)) {
					String checkStatus=respJson.get("audit_status");
					if("PASSED".equals(checkStatus)){
						// 认证通过
						resmsg="认证通过";
					}else if("AUDITING".equals(checkStatus)){
						// 审核中
						resmsg="认证中";
					}else{
						// 已驳回
						resmsg="已驳回";
					}
					String updateSql = "update Bank_MerRegister set rtnmsg=?,lmdate=sysdate  "
							+ " where hrid=? ";
					dao.update(updateSql, resmsg,hrid);
				}else{
					logger.error("[银联-微信]商户认证查询失败", respJson.get("rtnCode")+":"+respJson.get("return_msg"));
				}
			}
		} catch (Exception e) {
			logger.error("[银联-微信]商户认证查询失败", e);
		}
	}
}
