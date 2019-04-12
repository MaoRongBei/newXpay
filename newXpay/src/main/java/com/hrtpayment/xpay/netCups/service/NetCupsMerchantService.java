package com.hrtpayment.xpay.netCups.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.NettyClientService;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.HttpConnectService;
import com.hrtpayment.xpay.utils.HttpConnectService2;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Service
public class NetCupsMerchantService {
	private final Logger logger = LogManager.getLogger();

	@Autowired
	JdbcDao dao;
	@Autowired
	NotifyService notify;
	@Autowired
	NetCupsService cupsService;
	@Autowired
	NettyClientService client;

	@Value("${netcups.wx.appid}")
	String appid;
//	@Value("${netcups.wx.mchId}")
//	String mchid;
//	@Value("${cupsWx.channelId}")
//	String channelid;
	@Value("${netcups.wx.url}")
    String url;
	String addMerUrl=NetCupsProperties.netCupsAddMerUrl;
	String queryMerUrl=NetCupsProperties.netCupsQueryMerUrl;
	@Value("${netcups.ali.url}")
	String aliMerUrl;
	@Value("${cupsWx.md5Key}")
	String cupsWxMd5Key;
	
	

	/**
	 * 网联-支付宝 商户入驻
	 * 
	 * @param map
	 * @return
	 * @throws BusinessException
	 */
	public String addAliMerchants(Map<String, Object> map) throws BusinessException {
		String reqUrl = aliMerUrl;
		map.put("method", "ant.merchant.expand.indirect.create");
		Map<String, String> req = cupsService.getPackMessage(map);
		
		logger.info("请求信息----->" + req);
		String res = null;
		try {
			logger.info("[网联-支付宝]商户入驻请求信息----->" + req);
			res = HttpConnectService.postForm(req, reqUrl);
			logger.info("[网联-支付宝]商户入驻响应信息----->" + res);
		} catch (Exception e) {
			logger.info("[网联-支付宝]商户{}:{}入驻异常，错误 原因{}----->", map.get("hrid"), map.get("merchantid"), e.getMessage());
			throw new BusinessException(8000, "商户入驻失败");
		}
		JSONObject respJson = JSONObject.parseObject(res);
		@SuppressWarnings("unchecked")
		Map<String, String> resMap = JSONObject.toJavaObject(
				JSONObject.parseObject(respJson.get("ant_merchant_expand_indirect_create_response").toString()),
				Map.class);
		logger.info("respMap:" + resMap);
		String rtnCode = resMap.get("code").toString();
		String rtnMsg = resMap.get("msg").toString();

		if ("10000".equals(rtnCode)) {
			String sub_merchant_id = resMap.get("sub_merchant_id");
			if (sub_merchant_id == null || "".equals(sub_merchant_id)) {
				RedisUtil.addFailCountByRedis(1);
				logger.error("[网联-支付宝]商户入驻失败，订单号{},子商户号未返回", resMap.get("out_trade_no"));
				throw new BusinessException(8000, "商户入驻失败");
			}
			String updateSql = "update Bank_MerRegister set merchantcode=?, approvestatus='Y' ,lmdate=sysdate,rtnmsg=?  where hrid=? ";
			dao.update(updateSql, sub_merchant_id, getRtMsg("",rtnMsg), map.get("hrid"));
			return "入驻成功";
		} else {
			String updateSql = "update Bank_MerRegister set rtnmsg=?,approvestatus='N'  where hrid=? ";
			dao.update(updateSql,getRtMsg(rtnCode,rtnMsg), map.get("hrid"));
			logger.error("[网联-支付宝]商户入驻失败-通信状态失败，hrid{}：商户号{},返回{}，{}", map.get("hrid"), map.get("merchantid"), rtnCode,
					rtnMsg);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "商户入驻失败");
		}
	}

	/**
	 * 网联-微信 商户入驻
	 * 
	 * 微信入驻 同步结果为准？？？？？？？？？？？？？？？？？
	 * 
	 * @param map
	 * @return
	 * @throws BusinessException
	 */
	public String addWxMerchants(Map<String, Object> map) throws BusinessException {
		String reqUrl = url+addMerUrl;
		Map<String, String> merMsg = new HashMap<String, String>();
		merMsg.put("appid",String.valueOf(map.get("appid")));// appid);// 银行端提供 appid
		merMsg.put("mch_id",String.valueOf(map.get("mch_id")));// mchid);// 银行端提供 微信商户号
		merMsg.put("channel_id",String.valueOf(map.get("channel_id")));// channelid);// 银行端提供 渠道商编号
		merMsg.put("business", String.valueOf(map.get("category")));// 经营类目
		merMsg.put("merchant_name", String.valueOf(map.get("merchantname")));// 商户名称
		merMsg.put("merchant_shortname", String.valueOf(map.get("shortname")));// 商户简称
		merMsg.put("service_phone", String.valueOf(map.get("servicephone")));// 联系电话
		merMsg.put("merchant_remark", String.valueOf(map.get("merchantid")));// bank_merregister 中merchantid
		Map<String, String> req = cupsService.sign(merMsg);
		String reqStr=null;
		try {
		     reqStr=cupsService.mapToXml(req) ;
		} catch (Exception e1) {
			 logger.info("[网联-微信]商户入驻  请求报文转化异常：{}",e1.getMessage());
			 throw new BusinessException(8000, "商户入驻异常");
		}
		logger.info("请求信息----->" + req);
		String res = null;
		try {
			logger.info("[网联-微信]商户入驻请求信息----->" + reqStr);
			res=HttpConnectService.sendMessage(reqStr, reqUrl);
			logger.info("[网联-微信]商户入驻响应信息----->" + res);
		} catch (Exception e) {
			logger.info("[网联-微信]商户{}:{}入驻异常，错误 原因{}----->", map.get("hrid"), map.get("merchantid"), e.getMessage());
			throw new BusinessException(8000, "商户入驻失败");
		}
		Map<String, String> respJson=null;
		try {
			respJson = cupsService.xmlToMap(res);
		} catch (Exception e) {
			 logger.info("[网联-微信]商户入驻 响应报文转化异常：{}",e.getMessage());
			 throw new BusinessException(8000, "商户入驻响应报文处理异常");
		}
		if (!"SUCCESS".equals(respJson.get("return_code"))) {
			String updateSql = "update Bank_MerRegister set rtnmsg=?  where approvestatus<>'Z' and  merchantid=? ";
			dao.update(updateSql, getRtMsg( respJson.get("return_code"), respJson.get("return_msg")), map.get("merchantid"));
			RedisUtil.addFailCountByRedis(1); 
			logger.error("[网联 -微信] 商户入驻 失败{}：{}", respJson.get("return_code"), respJson.get("return_msg"));
			throw new BusinessException(1002, "商户入驻失败");
		} else {
			logger.info("respMap:" + respJson);
			String rtnCode = respJson.get("result_code"); 
			String rtnMsg = respJson.get("result_msg")==null?"入驻成功":respJson.get("result_msg"); 
	
			if ("SUCCESS".equals(rtnCode)) {
				String sub_merchant_id = respJson.get("sub_mch_id");
				if (sub_merchant_id == null || "".equals(sub_merchant_id)) {
					logger.error("[网联-微信]商户入驻失败，订单号{},子商户号未返回", respJson.get("mch_id"));
					throw new BusinessException(8000, "商户入驻失败");
				}
				String updateSql = "update Bank_MerRegister set merchantcode=?, approvestatus='Z' ,lmdate=sysdate,rtnmsg=?  "
						+ " where (approvestatus<>'Z' or approvestatus is null )  and  merchantid=? ";
				dao.update(updateSql, sub_merchant_id,  getRtMsg(rtnCode , rtnMsg), map.get("merchantid"));
				return rtnMsg;
			} else {
				String updateSql = "update Bank_MerRegister set rtnmsg=?  where approvestatus<>'Z' and  merchantid=? ";
				dao.update(updateSql, getRtMsg(rtnCode , rtnMsg), map.get("merchantid"));
				logger.error("[网联-微信]商户入驻失败-通信状态失败， 商户号{},返回{}，{}", map.get("merchantid"), rtnCode, rtnMsg);
				throw new BusinessException(1002, "商户入驻失败");
			}
		}
	}

	/**
	 * 网联-支付宝 商户入驻查询
	 * 
	 * @param merchantid
	 * @return
	 * @throws BusinessException
	 */
	public String queryAliMerchants(String merchantid, BigDecimal fiid, String merchantName,String mchId) throws BusinessException {
		String reqUrl = aliMerUrl;

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("method", "ant.merchant.expand.indirect.query");
		map.put("merchantid", merchantid);
		map.put("mch_id", mchId);
		Map<String, String> req = cupsService.getPackMessage(map);

		logger.info("请求信息----->" + req);
		String res = null;
		try {
			logger.info("[网联-支付宝]商户入驻查询请求信息----->" + req);
			res = HttpConnectService.postForm(req, reqUrl);
			logger.info("[网联-支付宝]商户入驻查询响应信息----->" + res);
		} catch (Exception e) {
			logger.info("[网联-支付宝]商户 {}入驻查询异常，错误 原因{}----->", merchantid, e.getMessage());
			throw new BusinessException(8000, "商户入驻查询失败");
		}

		JSONObject respJson = JSONObject.parseObject(res);
		Map<String, String> resMap = JSONObject.toJavaObject(
				JSONObject.parseObject(respJson.get("ant_merchant_expand_indirect_query_response").toString()),
				Map.class);
		logger.info("respMap:" + resMap);
		String rtnCode = resMap.get("code").toString();
		String rtnMsg = resMap.get("msg").toString();

		if ("10000".equals(rtnCode)) {
			String sub_merchant_id = resMap.get("sub_merchant_id");
			if (sub_merchant_id == null || "".equals(sub_merchant_id)) {
				logger.error("[网联-支付宝]商户入驻查询失败，订单号{},子商户号未返回", resMap.get("out_trade_no"));
				throw new BusinessException(8000, "商户入驻查询失败");
			}
			String indirect_level = resMap.get("indirect_level");
			String indirect_level_msg = "";
			Integer indirect_optstatus=0;
			if (!"INDIRECT_LEVEL_M3".equals(indirect_level)) {
				indirect_level_msg=resMap.get("memo");
			}else{
				indirect_level_msg="M3商户信息已完善";
				indirect_optstatus=1;
			}
			String updateSql = "update Bank_MerRegister set merchantcode=?, approvestatus='Y' ,lmdate=sysdate,rtnmsg=? ,indirect_level=? ,indirect_level_msg=?,"
					+ " indirect_optstatus=? "
					+ " where   merchantid=? ";
			dao.update(updateSql, sub_merchant_id, getRtMsg(rtnCode,rtnMsg),indirect_level ,indirect_level_msg,indirect_optstatus, map.get("merchantid"));
			return rtnMsg;
		} else {
			String updateSql = "update Bank_MerRegister set rtnmsg=?  where approvestatus<>'Y' and  merchantid=? ";
			dao.update(updateSql, getRtMsg(rtnCode,rtnMsg), merchantid);
			logger.error("[网联-支付宝]商户入驻失败-通信状态失败， 商户号{},返回{}，{}", merchantid, rtnCode, rtnMsg);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "商户入驻失败");
		}
	}

	/**
	 * 网联-微信 商户入驻查询
	 * 
	 * @param merchantid
	 * @return
	 * @throws BusinessException
	 */
	public String queryWxMerchants(Map<String, Object> map) throws BusinessException {
		String reqUrl = url+queryMerUrl;
		Map<String, String> merMsg = new HashMap<String, String>();
		merMsg.put("appid",String.valueOf(map.get("appid")));// appid);// 银行端提供 appid
		merMsg.put("mch_id", String.valueOf(map.get("mch_id")));//mchid);// 银行端提供 微信商户号
		merMsg.put("channel_id", String.valueOf(map.get("channel_id")));//channelid);// 银行端提供 渠道商编号
	    merMsg.put("merchant_remark", String.valueOf(map.get("merchantid")));// 订单编号
		Map<String, String> req = cupsService.sign(merMsg);
		String reqStr=null;
		try {
		     reqStr=cupsService.mapToXml(req) ;
		} catch (Exception e1) {
			 logger.info("[网联-微信]商户入驻查询  请求报文转化异常：{}",e1.getMessage());
			 throw new BusinessException(8000, "商户入驻查询异常");
		}
		logger.info("请求信息----->" + req);
		String res = null;
		try {
			logger.info("[网联-微信]商户入驻查询请求信息----->" + reqStr);
			res=HttpConnectService.sendMessage(reqStr, reqUrl);
			logger.info("[网联-微信]商户入驻查询响应信息----->" + res);
		} catch (Exception e) {
			logger.info("[网联-微信]商户{}:{}入驻查询异常，错误 原因{}----->", map.get("hrid"), map.get("merchantid"), e.getMessage());
			throw new BusinessException(8000, "商户入驻查询失败");
		}
		Map<String, String> respJson=null;
		try {
			respJson = cupsService.xmlToMap(res);
		} catch (Exception e) {
			 logger.info("[网联-微信]商户入驻查询    响应报文转化异常：{}",e.getMessage());
			 throw new BusinessException(8000, "商户入驻查询    响应报文处理异常");
		}
		if ("SUCCESS".equals(respJson.get("return_code"))) {
			String updateSql = "update Bank_MerRegister set approvestatus='Z' ,lmdate=sysdate,rtnmsg=?  "
					+ " where  (approvestatus<>'Z' or approvestatus is null )  and merchantid=? ";
			int count=dao.update(updateSql,  getRtMsg(respJson.get("return_code") , "success"), map.get("merchantid"));
			logger.info("[网联-微信] 商户入驻查询：商户{}成功 {}",map.get("merchantid"),count);
			return "入驻成功" ;
		}else{
			String updateSql = "update Bank_MerRegister set rtnmsg=?  where approvestatus<>'Z' and  merchantid=? ";
			dao.update(updateSql, getRtMsg( respJson.get("return_code") ,  respJson.get("return_msg")), map.get("merchantid"));
			logger.error("[网联 -微信] 商户入驻查询  失败{}：{}", respJson.get("return_code"), respJson.get("return_msg"));
			throw new BusinessException(1002, "商户入驻查询失败");
		}
		}
	
	
 

	/**
	 * 网联-AT 商户入驻修改
	 * 
	 * @param merchantid
	 * @return
	 * @throws BusinessException
	 */
	public String updateAliMerchants(String hrid) throws BusinessException {

		String sql = "select * from bank_merregister where hrid =?";
		List<Map<String, Object>> list = dao.queryForList(sql, hrid);
		Map<String, Object> map = list.get(0);

		String reqUrl = aliMerUrl;

		map.put("method", "ant.merchant.expand.indirect.modify");
		Map<String, String> req = cupsService.getPackMessage(map);

		logger.info("请求信息----->" + req);
		String res = null;
		try {
			logger.info("[网联-AT]商户入驻修改请求信息----->" + req);
			res = HttpConnectService.postForm(req, reqUrl);
			logger.info("[网联-AT]商户入驻修改响应信息----->" + res);
		} catch (Exception e) {
			logger.info("[网联-AT]商户{}:{}入驻修改异常，错误 原因{}----->", map.get("hrid"), map.get("merchantid"), e.getMessage());
			throw new BusinessException(8000, "商户入驻修改失败");
		}

		JSONObject respJson = JSONObject.parseObject(res);
		Map<String, String> resMap = JSONObject.toJavaObject(
				JSONObject.parseObject(respJson.get("ant_merchant_expand_indirect_modify_response").toString()),
				Map.class);
		logger.info("respMap:" + resMap);
		String rtnCode = resMap.get("code").toString();
		String rtnMsg = resMap.get("msg").toString();

		if ("10000".equals(rtnCode)) {
			String sub_merchant_id = resMap.get("sub_merchant_id");
			if (sub_merchant_id == null || "".equals(sub_merchant_id)) {
				logger.error("[网联-AT]商户入驻修改失败，订单号{},子商户号未返回", resMap.get("out_trade_no"));
				throw new BusinessException(8000, "商户入驻修改失败");
			}
			String updateSql = "update Bank_MerRegister set merchantcode=?  ,lmdate=sysdate,rtnmsg=?  where  merchantid=? ";
			dao.update(updateSql, sub_merchant_id, "商户入驻修改||" + rtnCode + "||" + rtnMsg, map.get("merchantid"));
			return rtnMsg;
		} else {
			String updateSql = "update Bank_MerRegister set rtnmsg=?   where  merchantid=? ";
			dao.update(updateSql, rtnCode + "||" + rtnMsg, map.get("merchantid"));
			logger.error("[网联-AT]商户入驻修改失败-通信状态失败，hrid{}：商户号{},返回{}，{}", map.get("hrid"), map.get("merchantid"), rtnCode,
					rtnMsg);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "商户入驻修改失败");
		}
	}
	
	
		
	/**
	 *  appid  是  String(32)  wx931386123456789e 银行服务商的公众账号 ID 
		商户号  mch_id 是  String(32) 1451234567 银行服务商的商户号 
		特约商户号  sub_mch_id 是  String(32) 10000101 银行服务商报备的特约商户识别码 
		授权目录  jsapi_path  是 String(256) http://www.qq.com/wechat/ 银行特约商户公众账号JS API支付授权目录 ，要求符合URI格式规范，每次添加一个支付目录，最多5个 
		签名  sign  

	 * @param respCode
	 * @param respMsg
	 * @return
	 * @throws BusinessException 
	 */
		
	public String addSubMerchant(String merchantCode,String zdlx,Map<String, Object> merchantInfo) throws BusinessException{
		Map<String, String> subMer=new HashMap<String,String>();
		String reqUrl="";
		String respMsg="";
		if ("1".equals(zdlx)) {
			// 配置授权目录
			subMer.put("appid", appid);
			subMer.put("mch_id", String.valueOf(merchantInfo.get("mch_id")));//mchid);
			subMer.put("sub_mch_id", merchantCode);
			subMer.put("jsapi_path", String.valueOf(merchantInfo.get("authpath")));
			reqUrl="https://api.mch.weixin.qq.com/secapi/mch/addsubdevconfig"; 
		}else if ("2".equals(zdlx)) {
			// 绑定appid
			subMer.put("appid", appid);
			subMer.put("mch_id",  String.valueOf(merchantInfo.get("mch_id")));//mchid);
			subMer.put("sub_mch_id", merchantCode);
			subMer.put("sub_appid", String.valueOf(merchantInfo.get("appid")));
			reqUrl="https://api.mch.weixin.qq.com/secapi/mch/addsubdevconfig"; 
		}else if ("3".equals(zdlx)) {
			// 配置推荐关注公众号
			subMer.put("mch_id",  String.valueOf(merchantInfo.get("mch_id")));//mchid);
			subMer.put("sub_mch_id", merchantCode);
			subMer.put("sub_appid", String.valueOf(merchantInfo.get("appid")));
//			subMer.put("subscribe_appid", String.valueOf(merchantInfo.get("appid")));
			if (!"".equals(merchantInfo.get("sub_appid"))&&null!=merchantInfo.get("sub_appid")) {
				subMer.put("subscribe_appid", String.valueOf(merchantInfo.get("sub_appid")));
			}else{
				subMer.put("subscribe_appid", String.valueOf(merchantInfo.get("appid")));
			}
//			subMer.put("receipt_appid", String.valueOf(merchantInfo.get("appid")));
			subMer.put("sign_type", "MD5");
			subMer.put("nonce_str",  cupsService.getRandomString(32));
			reqUrl="https://api.mch.weixin.qq.com/secapi/mkt/addrecommendconf"; 
		}
		String signstr =SimpleXmlUtil.getMd5Sign(subMer, cupsWxMd5Key);
		subMer.put("sign", signstr);
		String reqStr=null;
		try {
		     reqStr=cupsService.mapToXml(subMer) ;
		} catch (Exception e1) {
			 logger.info("[网联-微信]{}子商户配置【{}】  请求报文转化异常：{}",merchantCode,zdlx,e1.getMessage());
			 throw new BusinessException(8000, "商户入驻查询异常");
		}
		String res = null;
		try {
			logger.info("[网联-微信]子商户{}配置步骤【{}】请求信息----->{}" ,merchantCode,zdlx,reqStr);
			res = HttpConnectService2.sendMessage(reqStr, reqUrl,String.valueOf(merchantInfo.get("mch_id")));
			logger.info("[网联-微信]子商户{}配置步骤【{}】响应信息----->{}" ,merchantCode,zdlx, res);
		} catch (Exception e) {
			logger.info("[网联-微信]子商户{}配置步骤【{}】异常，错误 原因{}----->",merchantCode,zdlx, e.getMessage());
			throw new BusinessException(8000, "子商户配置步骤【"+zdlx+"】失败");
		}
		Map<String, String> respJson=null;
		try {
			respJson = cupsService.xmlToMap(res);
			if("SUCCESS".equals(respJson.get("return_code"))){
				// 通讯成功
				String updateSql = "update Bank_MerRegister set approveStatus=? , RTNMSG=?,LMDATE=sysdate where merchantCode=?";
				String updateStatus="Z";
				
				if("1".equals(zdlx)){
					if("SUCCESS".equals(respJson.get("result_code"))){
						updateStatus="M";
						respMsg="SUCCESS";
					}else{
						updateStatus="Z";
						respMsg=respJson.get("err_code_msg");
					}
				}else if("2".equals(zdlx)){
					if("SUCCESS".equals(respJson.get("result_code"))){
						updateStatus="Y";
						respMsg="SUCCESS";
					}else{
						updateStatus="N";
						respMsg=respJson.get("err_code_msg");
					}
				}else if("3".equals(zdlx)){
					updateStatus="Y";
					if("SUCCESS".equals(respJson.get("result_code"))){
						respMsg="SUCCESS";
					}else{
						respMsg=respJson.get("err_code_msg");
					}
				}
				respMsg=respMsg.substring(0,respMsg.length()>=25?25:respMsg.length());
				dao.update(updateSql, updateStatus,respMsg,merchantCode);
			}else{
				respMsg="FAIL";
			}
		} catch (Exception e) {
			 logger.info("[网联-微信]子商户{}配置步骤【{}】    响应报文转化异常：{}",merchantCode,zdlx,e.getMessage());
			 throw new BusinessException(8000, "子商户配置步骤【"+zdlx+"】 响应报文处理异常");
		}
		return respMsg;
		
	}
	
	
	private String getRtMsg(String respCode,String respMsg) {
		if (respCode != null && respMsg!=null) {
			String msg=respCode+respMsg;
			return msg.substring(0,msg.length()>=25?25:msg.length());
		} else return respCode;
	}
}