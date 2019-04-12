package com.hrtpayment.xpay.bcm.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.bcm.util.MsgUtil;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.AlipayService;
import com.hrtpayment.xpay.common.service.WxpayService;
import com.hrtpayment.xpay.common.service.impl.NettyClientService;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.DateUtil;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

/**
 * @author lvjianyu 交通银行 无卡 2017-11-07
 */
@Service
public class BcmPayService implements WxpayService , AlipayService{
	private final Logger logger = LogManager.getLogger();
	
	@Value("${bcmpay.payUrl}")
	private String payUrl; // 二维码测试支付地址
	@Value("${bcmpay.key}")
	private String key; // 验签key
	@Value("${bcmpay.merNo}")
	private String merNo; // 大商户号 子商户需要上送
	@Value("${bcmpay.noticeUrl}")
	private String noticeUrl;//异步通知地址 【退款，交易】
	@Value("${bcmpay.requestIp}")
	private String requestIp; //上送交易ip地址
	
	@Value("${bcmpay.secret}")
	private String secret;
	@Value("${bcmpay.appid}")
	private String appid;
	
	@Value("${alipay.app_id}")
	private String aliAppid;
	
	@Autowired
	NettyClientService netty;
	@Autowired
	private JdbcDao dao;
	@Autowired
	private NotifyService notify;
	
	/**
	 * 获取付款二维码.
	 * 上生产需要更改发送订单ip requestIp
	 * @throws BusinessException
	 */
	public String getQrCode(String unno, String mid, String bankMid, String subject, BigDecimal amount,
			int fiid, String payWay,String orderid, String tid,  String hybRate, String hybType)
			throws BusinessException {
		// 1.验重 订单号
		List<Map<String, Object>> list = dao
				.queryForList("select * from pg_wechat_txn where mer_orderid=? and rownum=1", orderid);
		if (list.size() > 0)
			throw new BusinessException(9007, "订单号重复");
		
		String sql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ " bankmid ,status, cdate, lmdate ,unno,mer_tid,hybType,hybRate) values "
				+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,sysdate,sysdate,?,?,?,?)";
		dao.update(sql, fiid, orderid, subject, amount, mid, bankMid, 0, unno, tid, hybType, hybRate);
		
		//2.拼接参数
		Map<String, String> params=new HashMap<>(20);
		params.put("version", "v1.0");
		params.put("unicode", "UTF-8");
		params.put("signType", "MD5");
		params.put("tranCode", "scanpay");
		String payChannel = "";
		String trantype = "";
		if ("WXZF".equals(payWay)) {
			payChannel = "2";
			trantype = "1";
		} else if ("ZFBZF".equals(payWay)) {
			payChannel = "1";
			trantype = "2";
		}
		params.put("payChannel",payChannel); //1:支付宝,2:微信
		params.put("merNo",merNo);
		params.put("subMerNo",bankMid);
		params.put("merOrderNo",orderid);
		params.put("tranTime",DateUtil.getCompleteTime()); //YYYYMMDDHHMMSS
		BigDecimal amountParam = amount.multiply(new BigDecimal(100));
		params.put("tranAmt",amountParam.intValue()+""); //金额：分
		params.put("tranType","NATIVE");
		params.put("detail",subject);
		params.put("currency","156");
		params.put("requestIp",requestIp);
		params.put("noticeUrl",noticeUrl);
		params.put("terminal","00000000");
//		params.put("timeExpire","10"); //过期时间 10分钟 默认5 最大30
		
		String sign = SimpleXmlUtil.getMd5Sign(params, key);
		params.put("sign",sign);
		String result = "";
		
		try {
			logger.info("[交通银行]获取二维码发送报文：{},\r\n{}",payUrl,params);
			result = netty.sendJson(payUrl, JSONObject.toJSONString(params));
			logger.info("[交通银行]获取二维码返回报文：{}",result);
		} catch (Exception e) {
			logger.error("[交通银行]获取二维码网络错误:{}，订单号{}", e,orderid);
			throw new BusinessException(1002, "获取信息网络错误");
		}
		JSONObject json= JSONObject.parseObject(result);
		
		//验签
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = JSONObject.toJavaObject(json, Map.class);
		if (!MsgUtil.verifySign(itemMap, key)) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[交通银行]获取二维码返回信息验签失败，订单号{}",orderid);
			throw new BusinessException(1002, "获取信息验签失败");
		}
		
		//先判通信状态retCode
		String retCode=itemMap.get("retCode");
		if(!"0000".equals(retCode)){
			RedisUtil.addFailCountByRedis(1);
			logger.error("[交通银行]获取二维码返回-通信状态失败，订单号{},返回{}，{}",orderid,retCode,itemMap.get("retMsg"));
			throw new BusinessException(1002, "网络通信失败");
		}
		//再判交易状态rspCode
		String rspCode=itemMap.get("rspCode");
		if ("00".equals(rspCode)) {
			String qrCode = itemMap.get("qrcodeUrl");
			String sqlUpdate = "update pg_wechat_txn set mer_orderid=?,qrcode=?,respcode=?,respmsg=?,"
					+ "trantype=?,lmdate=sysdate where mer_orderid=?";
			dao.update(sqlUpdate, itemMap.get("merOrderNo"), qrCode, rspCode, itemMap.get("rspDesc"), trantype,
					orderid);
			if (null == qrCode || "".equals(qrCode)) {
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(8000, "订单已失效");
			}
			return qrCode;
		} else {
			logger.error("[交通银行]获取二维码返回-通信状态失败，订单号{},返回{}，{}", orderid, rspCode, itemMap.get("rspDesc"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "下单失败");
		}
	}

	/**
	 * 条码支付
	 * @param string 
	 * @throws BusinessException
	 */
	public String barCodePay(HrtPayXmlBean bean,String unno, String mid, int fiid, String payway, String orderid, String merchantCode,
			 String authCode, BigDecimal amount, String subject, String tid,String trantype) throws BusinessException {
		//1.验证订单是否存在
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?",
				orderid);
		if (list.size() > 0) {
			throw new BusinessException(8000, "订单号已存在！");
		}
		String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ "status, cdate, lmdate,unno,mer_tid,trantype,bankmid) values((S_PG_Wechat_Txn.nextval),?,'0',?,?,?,?,'0',to_date(?,'yyyyMMddHH24miss'),sysdate,?,?,?,?)";
		dao.update(insertSql, fiid, orderid, subject, amount, mid, bean.getPayOrderTime(),unno, tid,trantype, merchantCode);
		
		//拼接参数
		Map<String, String> params=new HashMap<>();
		params.put("version","v1.0");
		params.put("unicode","UTF-8");
		params.put("signType","MD5");
		params.put("tranCode","pay");
		
		params.put("merNo",merNo);
		params.put("subMerNo",merchantCode);
		params.put("merOrderNo",orderid);
		params.put("tranTime",DateUtil.getCompleteTime());
		amount = amount.multiply(new BigDecimal(100));
		params.put("tranAmt",amount.intValue()+"");
		params.put("currency","156");
		params.put("detail",subject);
		params.put("requestIp",requestIp);
		params.put("authCode",authCode);
		String sign = SimpleXmlUtil.getMd5Sign(params, key);
		params.put("sign",sign);
		
		String result = "";
		try {
			logger.info("[交通银行]条码支付发送报文：{},\r\n{}",payUrl,params);
			result = netty.sendJson(payUrl, JSONObject.toJSONString(params));
			logger.info("[交通银行]条码支付返回报文：{}",result);
		} catch (Exception e) {
			logger.error("[交通银行]条码支付网络错误:{}，订单号{}", e,orderid);
			String sql = "update pg_wechat_txn set lmdate=sysdate,respcode=?,respmsg=? where mer_orderid=?";
			int count = dao.update(sql, "USERPAYING", "网络超时", orderid);
			logger.error("[交通银行]条码支付网络错误，更入记录{}，订单号{}", count, orderid);
			return "R";
		}
		JSONObject json= JSONObject.parseObject(result);
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = JSONObject.toJavaObject(json, Map.class);
		if (!MsgUtil.verifySign(itemMap, key)) {
			logger.error("[交通银行]条码交易验签失败，订单号{}",orderid);
			RedisUtil.addFailCountByRedis(1);
		}
		
		String retCode=itemMap.get("retCode");
		String rspCode=itemMap.get("rspCode");
		
		if ((retCode != null && "9999".equals(retCode)) || (rspCode != null && rspCode.equals("96"))) { 
			//对方返回参数不稳定，根据部分状态判定网络超时,执行轮询查询
			dao.update("update pg_wechat_txn set respcode=?,respmsg=?,status=0,lmdate=sysdate where mer_orderid=?", 
					"USERPAYING",itemMap.get("rspDesc"), orderid);
			logger.error("[交通银行]条码支付返回-通信状态失败，订单号{},返回{}，{}", orderid, retCode, itemMap.get("rspDesc"));
		    if (bean!=null ) {
		    	bean.setRtnCode("USERPAYING");
		    	bean.setRtnMsg(itemMap.get("rspDesc"));
			}
			
			RedisUtil.addFailCountByRedis(1);
			return "R";
		} else if (!"0000".equals(retCode)) {
			dao.update("update pg_wechat_txn set bk_orderid=?,respcode=?,respmsg=?,status=6 where mer_orderid=?", 
					itemMap.get("orderNo"),itemMap.get("retCode"),itemMap.get("retMsg"),itemMap.get("merOrderNo"));
			logger.error("[交通银行]条码支付返回-通信状态失败，订单号{},返回{}，{}",orderid,retCode,itemMap.get("retMsg"));
			RedisUtil.addFailCountByRedis(1);
			if (bean!=null ) {
				bean.setRtnCode(itemMap.get("retCode"));
				bean.setRtnMsg(itemMap.get("retMsg"));
			}
			throw new BusinessException(1002, "网络通信失败");
		}
		//再判rspStatus 订单状态 10r  11s  12e
		String rspStatus=itemMap.get("rspStatus");
		String returnStatus="";
		String updateStatus="0";
		if ("10".equals(rspStatus)) { //IP
			returnStatus = "R";
			rspStatus="USERPAYING";
		} else if ("11".equals(rspStatus)) { //00
			returnStatus = "S";
			updateStatus = "1";
		} else {
			if (rspCode != null && rspCode.equals("IP")) { //IP	对方网络中断 需要执行查询
				returnStatus = "R";
				updateStatus = "0";
			} else {
				returnStatus = "E";
				updateStatus = "6";
				RedisUtil.addFailCountByRedis(1);
			}
			logger.error("[交通银行]条码支付，订单号{},返回{}，{}", orderid, rspStatus, itemMap.get("rspDesc"));
			// 失败时，没有rspStatus 需要取值 rspCode，防止轮训一直查询
			rspStatus = rspCode;
		}
		
		String sql = "update pg_wechat_txn set status=?,time_end=?,lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate),"
				+ "bk_orderid=?,respcode=?,respmsg=?,paytype=? ,userid=? where mer_orderid=?";
		int count = dao.update(sql, updateStatus, itemMap.get("completeTime"), itemMap.get("completeTime"),
				itemMap.get("orderNo"), rspStatus, itemMap.get("rspDesc"),formatPayType(itemMap.get("payMentCardType")),
				itemMap.get("payMentUserId"),orderid);
		logger.info("[交通银行]条码支付更新记录数{},订单号{}", count, orderid); 
		if (bean!=null) {
			bean.setRtnCode(rspStatus);
			bean.setRtnMsg(itemMap.get("rspDesc"));
			bean.setBankType(formatPayType(itemMap.get("payMentCardType")));
			bean.setUserId(itemMap.get("payMentUserId"));
		}
 
		return returnStatus;
	}

	public String queryOrder(HrtPayXmlBean bean,Map<String, Object> orderInfo) {
		String orderId=String.valueOf(orderInfo.get("MER_ORDERID"));
		Map<String, String> params=new HashMap<>();
		params.put("version","v1.0");
		params.put("unicode","UTF-8");
		params.put("signType","MD5");
		params.put("tranCode", "payrequest");
		
		params.put("merNo", merNo);
		params.put("subMerNo", String.valueOf(orderInfo.get("BANKMID")));
		if(orderInfo.get("BK_ORDERID")!=null)
			params.put("orderNo", String.valueOf(orderInfo.get("BK_ORDERID"))); // 支付订单号
		params.put("merOrderNo", orderId); // 商户订单号
		params.put("tranTime", DateUtil.getStringFromDate((Date) orderInfo.get("CDATE"), DateUtil.FORMAT_TRADETIME)); // cdate
		params.put("requestIp", requestIp);
		String sign = SimpleXmlUtil.getMd5Sign(params, key);
		params.put("sign", sign);
		
		String result = "";
		try {
			logger.info("[交通银行]订单查询发送报文：{},\r\n{}",payUrl, params);
			result = netty.sendJson(payUrl, JSONObject.toJSONString(params));
			logger.info("[交通银行]订单查询返回报文：{}", result);
		} catch (Exception e) {
			logger.error("[交通银行]订单查询网络错误:{}，订单号{}", e, orderId);
			/**
			 *   立码富 需要返回 银行返回码、返回信息、借贷标识
			 */
			if (bean!=null ) {
				bean.setRtnCode("ERROR");
				bean.setRtnMsg("网络异常");
			}
			return "ERROR";
		}
		JSONObject json= JSONObject.parseObject(result);
		
		//验签
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = JSONObject.toJavaObject(json, Map.class);
		if (!MsgUtil.verifySign(itemMap, key)) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[交通银行]订单查询-返回信息验签失败，订单号{}",orderId);
			/**
			 *   立码富 需要返回 银行返回码、返回信息、借贷标识
			 */
			if (bean!=null ) {
				bean.setRtnCode("ERROR");
				bean.setRtnMsg("验签失败");
			}
			return "ERROR";
		}
		
		String retCode=itemMap.get("retCode"); //0000
		String reqRspCode=itemMap.get("reqRspCode"); //00
		if (!"0000".equals(retCode) || !"00".equals(reqRspCode)) {
			dao.update("update pg_wechat_txn set bk_orderid=?,respcode=?,respmsg=? where status !='1' and mer_orderid=?",
					itemMap.get("orderNo"), retCode + "-" + reqRspCode,
					itemMap.get("retMsg") + "-" + itemMap.get("reqRspDesc"), itemMap.get("merOrderNo"));
			logger.error("[交通银行]订单查询-通信状态失败，订单号{},返回{}，{}", orderId, retCode, itemMap.get("retMsg"));
			return "DOING";
		}
		String rspStatus = itemMap.get("rspStatus");
		String rspCode = itemMap.get("rspCode");
		
		//更新数据库
		String tradeState = "DOING";
		if ("11".equals(rspStatus)) {
			String bk_orderId = itemMap.get("orderNo");
			String tradeTime = itemMap.get("completeTime");
			
			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,bk_orderid=?,time_end=nvl(?,to_char(sysdate,'yyyymmddhh24miss'))"
					+ " ,lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate),txnLevel=?,status=1,paytype=?,userid=? where status!='1' and mer_orderid=?";
			int count = dao.update(updateSql, rspStatus, itemMap.get("rspDesc"), bk_orderId,tradeTime, 
					tradeTime, 1,formatPayType(itemMap.get("payMentCardType")), itemMap.get("payMentUserId"),orderId);
			if (count > 0) {
				// 推送
				BigDecimal txnLevel = orderInfo.get("TXNLEVEL") == null || "".equals(orderInfo.get("TXNLEVEL"))
						? BigDecimal.ONE : new BigDecimal(String.valueOf(orderInfo.get("TXNLEVEL")));
				orderInfo.put("TXNLEVEL", txnLevel);
				orderInfo.put("time", tradeTime);
				//立码富需要的四个字段
				orderInfo.put("RTNCODE", rspStatus);
				orderInfo.put("RTNMSG", itemMap.get("rspDesc")); 
				orderInfo.put("BANKTYPE", formatPayType(itemMap.get("payMentCardType")));
				orderInfo.put("USERID", itemMap.get("payMentUserId"));
				if(bean!=null){
					bean.setUserId(itemMap.get("payMentUserId"));
				}
				notify.sendNotify(orderInfo);
			}
			tradeState = "SUCCESS";
		} else if ("10".equals(rspStatus)) {
			tradeState = "DOING";
			String updateSql = "update pg_wechat_txn set respcode='USERPAYING', respmsg=?"
					+ " where status!='1' and mer_orderid=?";
			dao.update(updateSql, itemMap.get("rspDesc"), orderId);
		} else {
			tradeState = "FAIL";
			// 失败时，没有rspStatus 需要取值 rspCode，防止轮训一直查询
			rspStatus = rspCode; //eg：25 找不到原始记录
			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?"
					+ " ,lmdate=sysdate,status=6 where status!='1' and mer_orderid=?";
			int count = dao.update(updateSql, rspStatus, itemMap.get("rspDesc"), orderId);
			logger.warn("[北京交通]查询订单返回失败：{}，{}，订单号：{},更新记录{}条", rspStatus, itemMap.get("rspDesc"), orderId, count);
		}
		
		/**
		 *   立码富 需要返回 银行返回码、返回信息、借贷标识
		 */
		if (bean!=null ) {
			bean.setRtnCode(rspCode);
			bean.setRtnMsg("DOING".equals(tradeState)?"USERPAYING":itemMap.get("rspDesc"));
			bean.setBankType(formatPayType(itemMap.get("payMentCardType")));
			bean.setUserId(itemMap.get("payMentUserId"));
		}
		
		return tradeState;
	}

	/**
	 * 交易通知
	 * @param params
	 */
	public void payCallBack(Map<String, String> params) {
		String orderId = params.get("outOrderNo");
		if (!MsgUtil.verifySign(params, key)) {
			logger.error("[交通银行]支付异步通知,验签失败，订单号{}",orderId);
			return ;
		}
		
		//1.查询原订单是否存在
		String querySql = "select mer_orderid,status ,unno,mer_id, txnamt,mer_tid,trantype,lmdate from pg_wechat_txn t where t.mer_orderid=?";
		List<Map<String, Object>> list = dao.queryForList(querySql, orderId);
		if (list.size() < 1) {
			logger.info("[交通银行]支付异步通知,未查询到订单号为{}的订单", orderId);
			return;
		}
		Map<String, Object> orderMap=list.get(0);
		
		String status = String.valueOf(orderMap.get("STATUS"));
		if ("1".equals(status)) {
			logger.info("[交通银行]--支付原订单{}--状态为成功不再进行更新！", orderId);
			return;
		}
		
		String orderStatus = params.get("orderStatus");
		if ("11".equals(orderStatus)) {
			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?"
					+ ",lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate),"
					+ "txnLevel=? ,status=1,time_end=?,bk_orderid=?,paytype=?,userid=? where status<>'1' and mer_orderId=?";

			Integer count = dao.update(updateSql, orderStatus,
					params.get("rspDesc") == null ? "交易成功" : params.get("rspDesc"), params.get("tranTime"), 1,
					params.get("tranTime"), params.get("orderNo"), formatPayType(params.get("payMentCardType")),
					params.get("payMentUserId"), orderId);
			if (count == 1) {
				BigDecimal txnLevel = list.get(0).get("TXNLEVEL") == null || "".equals(list.get(0).get("TXNLEVEL"))
						? BigDecimal.ONE : new BigDecimal("1");
				list.get(0).put("TXNLEVEL", txnLevel);
				list.get(0).put("RTNCODE", orderStatus);
				list.get(0).put("RTNMSG",  "SUCCESS");
				list.get(0).put("BANKTYPE", formatPayType(params.get("payMentCardType")));
				list.get(0).put("USERID", params.get("payMentUserId"));
				notify.sendNotify(list.get(0));
			} else {
				logger.info("[交通银行]--支付原订单{}--状态为成功不再进行更新！", orderId);
			}
		} else {
			logger.warn("[交通银行]支付异步通知，订单号{},返回状态{}", orderId, orderStatus);
		}
	}
	
	private String formatPayType(String string){
		if(string!=null&&!string.isEmpty()){
			//借记卡，贷记卡，余额
			string=string.trim();
			if("借记卡".equals(string)){
				return "1";
			}else if("信用卡".equals(string)||"贷记卡".equals(string)){
				return "2";
			}else {
				return "3";
			}
		}
		return "";
	}
	
	/**
	 * 处理退款异步通知
	 * @param params
	 */
	public void refundCallBack(Map<String, String> params) {
		String orderId = params.get("outOrderNo");
		if (!MsgUtil.verifySign(params, key)) {
			logger.error("[交通银行]退款异步通知,验签失败，订单号{}",orderId);
			return ;
		}
		
		//1.查询原订单是否存在
		String querySql = "select mer_orderid,status ,unno,mer_id, txnamt,mer_tid from pg_wechat_txn t where t.mer_orderid=?";
		List<Map<String, Object>> list = dao.queryForList(querySql, orderId);
		if (list.size() < 1) {
			logger.info("[交通银行]退款异步通知,未查询到订单号为{}的订单", orderId);
			return;
		}
		Map<String, Object> orderMap=list.get(0);
		
		String status = String.valueOf(orderMap.get("STATUS"));
		if ("1".equals(status)) {
			logger.info("[交通银行]--退款原订单{}--状态为成功不再进行更新！", orderId);
			return;
		}
		
		String orderStatus = params.get("orderStatus");
		if ("21".equals(orderStatus)) {
			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?"
					+ ",lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate),"
					+ "txnLevel=? ,status=1,time_end=?,bk_orderid=? where status<>'1' and mer_orderId=?";
			Integer count =dao.update(updateSql, orderStatus, params.get("rspDesc"), params.get("tranTime"), 1,
					params.get("tranTime"), params.get("orderNo"), orderId);
			if (count == 1) {
				BigDecimal txnLevel = list.get(0).get("TXNLEVEL") == null || "".equals(list.get(0).get("TXNLEVEL"))
						? BigDecimal.ONE : new BigDecimal("1");
				list.get(0).put("TXNLEVEL", txnLevel);
				notify.sendNotify(list.get(0));
			} else {
				logger.info("[交通银行]--退款原订单{}--状态为成功不再进行更新！", orderId);
			}
		} else {
			logger.warn("[交通银行]退款异步通知，订单号{},返回状态{}", orderId, orderStatus);
		}
	}

	/**
	 * @param orderId
	 * @param amount
	 * @param oriOrderInfo
	 * @return
	 */
	public String refund(String orderId, BigDecimal amount, Map<String, Object> oriOrderInfo) {
		Map<String, String> params = new HashMap<>();
		params.put("version", "v1.0");
		params.put("unicode", "UTF-8");
		params.put("signType", "MD5");
		params.put("tranCode", "payrefund"); //统一上送条码交易，交通银行整合了
		
		params.put("merNo", merNo);
		params.put("subMerNo", String.valueOf(oriOrderInfo.get("BANKMID")));
		params.put("orgOrderNo", String.valueOf(oriOrderInfo.get("BK_ORDERID"))); // 原订单银行订单号
		params.put("merOrderNo", orderId); // 商户订单号 new
		params.put("tranTime", DateUtil.getStringFromDate((Date) oriOrderInfo.get("CDATE"),
				DateUtil.FORMAT_TRADETIME)); // cdate
		BigDecimal amountParam = amount.multiply(new BigDecimal(100));
		params.put("tranAmt", amountParam.intValue() + ""); // 金额：分
		params.put("currency", "156");
		params.put("requestIp", requestIp);
		String sign = SimpleXmlUtil.getMd5Sign(params, key);
		params.put("sign", sign);
		
		String result = "";
		try {
			logger.info("[交通银行]订单退款发送报文：{},\r\n{}",payUrl, params);
			result = netty.sendJson(payUrl, JSONObject.toJSONString(params));
			logger.info("[交通银行]订单退款返回报文：{}", result);
		} catch (Exception e) {
			logger.error("[交通银行]订单退款网络错误:{}，订单号{}", e, orderId);
		}
		JSONObject json= JSONObject.parseObject(result);
		JSONObject jsonRes = new JSONObject();
		//验签
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = JSONObject.toJavaObject(json, Map.class);
		if (!MsgUtil.verifySign(itemMap, key)) {
			logger.error("[交通银行]订单退款-返回信息验签失败，订单号{}",orderId);
			return "E";
		}
		
		String retCode = itemMap.get("retCode"); // 0000
		if (!"0000".equals(retCode)) {
			dao.update("update pg_wechat_txn set bk_orderid=?,respcode=?,respmsg=?,status=6 where mer_orderid=?",
					itemMap.get("orderNo"), retCode, itemMap.get("retMsg"), itemMap.get("merOrderNo"));
			logger.error("[交通银行]订单查询-通信状态失败，订单号{},返回{}，{}", orderId, retCode, itemMap.get("retMsg"));
			return "E";
		}
		
		String rspStatus = itemMap.get("rspStatus");
		String rspCode = itemMap.get("rspCode");
		String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,time_end=?,lmdate=nvl("
				+ "to_date(?,'yyyymmddhh24miss'),sysdate),txnLevel=? ,bk_orderid=? ";
		if ("00".equals(itemMap.get("rspCode"))) {
			// 20退款处理中  21退款成功   22退款失败
			if ("21".equals(rspStatus)) {
				updateSql+=",status=1 ";
				jsonRes.put("errcode", "S");
				jsonRes.put("rtmsg",  "退款成功");
			} else if("20".equals(rspStatus)){
				jsonRes.put("errcode", "R");
				jsonRes.put("rtmsg",  "退款处理中");
			} else {
				updateSql+=",status=6 ";
				jsonRes.put("errcode", "E");
				jsonRes.put("rtmsg",  "退款失败");
				rspStatus = rspCode;
				logger.error("[交通银行]退款未处理状态订单号{},状态：{},{}", orderId, rspStatus, itemMap.get("rspDesc"));
			} 
		}else{
			updateSql+=",status=6 ";
			jsonRes.put("errcode", "E");
			jsonRes.put("rtmsg",  "退款失败");
			rspStatus = rspCode;
			logger.error("[交通银行]退款未处理状态订单号{},状态：{},{}", orderId, rspStatus, itemMap.get("rspDesc"));
		}
		updateSql+=" where mer_orderid=?";
		dao.update(updateSql, rspStatus, itemMap.get("rspDesc"), itemMap.get("completeTime"),
				itemMap.get("completeTime"), 0, itemMap.get("orderNo"), orderId);

		return jsonRes.toJSONString();
	}

	public String posGetQrCode(String mid, String bankMid, String subject, BigDecimal amount, int fiid, String orderid,
			String tid, String payWay) throws BusinessException {

		//2.拼接参数
		Map<String, String> params=new HashMap<>(20);
		params.put("version","v1.0");
		params.put("unicode","UTF-8");
		params.put("signType","MD5");
		params.put("tranCode","scanpay");
		String payChannel="";
		String trantype="";
		if ("WXZF".equals(payWay)) {
			payChannel = "2";
			trantype="1";
		} else if ("ZFBZF".equals(payWay)) {
			payChannel = "1";
			trantype="2";
		}
		params.put("payChannel",payChannel); //1:支付宝,2:微信
		params.put("merNo",merNo);
		params.put("subMerNo",bankMid);
		params.put("merOrderNo",orderid);
		params.put("tranTime",DateUtil.getCompleteTime()); //YYYYMMDDHHMMSS
		BigDecimal amountParam = amount.multiply(new BigDecimal(100));
		params.put("tranAmt",amountParam.intValue()+""); //金额：分
		params.put("tranType","NATIVE");
		params.put("detail",subject);
		params.put("currency","156");
		params.put("requestIp",requestIp);
		params.put("noticeUrl",noticeUrl);
		params.put("terminal","00000000"); 
		
		String sign = SimpleXmlUtil.getMd5Sign(params, key);
		params.put("sign",sign);
		String result = "";
		
		try {
			logger.info("[交通银行]获取二维码发送报文：{},\r\n{}",payUrl,params);
			result = netty.sendJson(payUrl, JSONObject.toJSONString(params));
			logger.info("[交通银行]获取二维码返回报文：{}",result);
		} catch (Exception e) {
			logger.error("[交通银行]获取二维码网络错误:{}，订单号{}", e,orderid);
			throw new BusinessException(1002, "网络通信失败");
		}
		JSONObject json= JSONObject.parseObject(result);
		
		//验签
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = JSONObject.toJavaObject(json, Map.class);
		if (!MsgUtil.verifySign(itemMap, key)) {
			logger.error("[交通银行]获取二维码返回信息验签失败，订单号{}",orderid);
		}
		
		//先判通信状态retCode
		String retCode=itemMap.get("retCode");
		if(!"0000".equals(retCode)){
			logger.error("[交通银行]获取二维码返回-通信状态失败，订单号{},返回{}，{}",orderid,retCode,itemMap.get("retMsg"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "网络通信失败");
		}
		//再判交易状态rspCode
		String rspCode=itemMap.get("rspCode");
		if ("00".equals(rspCode)) {
			String qrCode = itemMap.get("qrcodeUrl");
			String sql = "update pg_wechat_txn t set t.fiid=?,t.qrcode=?,t.status=?,t.bankmid=?,t.respcode=?,t.respmsg=?"
					+ ",t.lmdate=sysdate,t.trantype=? ,bk_orderid=? where status='A' and t.mer_orderid=? ";
			int count = dao.update(sql, fiid, qrCode, "0", bankMid, rspCode, itemMap.get("rspDesc"), trantype,
					itemMap.get("orderNo"), orderid);
			if (count < 1) {
				throw new BusinessException(8000, "订单已失效,请重新下单");
			}
			if (null == qrCode || "".equals(qrCode)) {
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(8000, "订单已失效");
			}
			return qrCode;
		} else {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[交通银行]获取二维码返回-通信状态失败，订单号{},返回{}，{}", orderid, rspCode, itemMap.get("rspDesc"));
			throw new BusinessException(1002, "下单失败");
		}
	}
	
	
	/**
	 * 退款查询
	 * @param orderInfo
	 * @param orderid
	 * @return
	 */
	public String refundQueryOrder(Map<String, Object> orderInfo) {
		
		String orderId=String.valueOf(orderInfo.get("MER_ORDERID"));
		Map<String, String> params=new HashMap<>();
		params.put("version","v1.0");
		params.put("unicode","UTF-8");
		params.put("signType","MD5");
		params.put("tranCode", "refundrequest");
		
		params.put("merNo", merNo);
		params.put("subMerNo", String.valueOf(orderInfo.get("BANKMID")));
		params.put("merOrderNo", orderId); // 商户订单号
		params.put("tranTime", DateUtil.getStringFromDate((Date) orderInfo.get("CDATE"), DateUtil.FORMAT_TRADETIME)); // cdate
		params.put("requestIp", requestIp);
		String sign = SimpleXmlUtil.getMd5Sign(params, key);
		params.put("sign", sign);
		
		String result = "";
		try {
			logger.info("[交通银行]退货查询发送报文：{},\r\n{}",payUrl, params);
			result = netty.sendJson(payUrl, JSONObject.toJSONString(params));
			logger.info("[交通银行]退货查询返回报文：{}", result);
		} catch (Exception e) {
			logger.error("[交通银行]退货查询网络错误:{}，订单号{}", e, orderId);
			return "ERROR";
		}
		JSONObject json= JSONObject.parseObject(result);
		
		//验签
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = JSONObject.toJavaObject(json, Map.class);
		if (!MsgUtil.verifySign(itemMap, key)) {
			logger.error("[交通银行]退货查询-返回信息验签失败，订单号{}",orderId);
			return "ERROR";
		}
		
		String retCode=itemMap.get("retCode"); //0000
		if (!"0000".equals(retCode)) {
			dao.update("update pg_wechat_txn set bk_orderid=?,respcode=?,respmsg=?,status=6 where mer_orderid=?",
					itemMap.get("orderNo"), retCode , itemMap.get("retMsg") , itemMap.get("merOrderNo"));
			logger.error("[交通银行]退货查询-通信状态失败，订单号{},返回{}，{}", orderId, retCode, itemMap.get("retMsg"));
			return "DOING";
		}
		
		//更新数据库
		String tradeState = "DOING";
		// 20 退款处理中21退款成功22 退款失败
		String rspStatus = itemMap.get("rspStatus");
		if ("21".equals(rspStatus)) {
			String bk_orderId = itemMap.get("orderNo");
			String tradeTime = itemMap.get("completeTime");

			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,bk_orderid=?,time_end=?"
					+ " ,lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate),txnLevel=?"
					+ " ,status=1 where status!='1' and mer_orderid=?";
			int count = dao.update(updateSql, rspStatus, itemMap.get("rspDesc"), bk_orderId,tradeTime, tradeTime, 1, orderId);
			if (count > 0) {
				// 推送
				BigDecimal txnLevel = orderInfo.get("TXNLEVEL") == null || "".equals(orderInfo.get("TXNLEVEL"))
						? BigDecimal.ONE : new BigDecimal(String.valueOf(orderInfo.get("TXNLEVEL")));
				orderInfo.put("TXNLEVEL", txnLevel);
				notify.sendNotify(orderInfo);
			}
			tradeState = "SUCCESS";
		} else if ("20".equals(rspStatus)) {
			tradeState = "DOING";
		} else {
			tradeState = "FAIL";
		}
		return tradeState;
	}
	
	
	@Override
	public String getWxpayPayInfo(String orderId, String openid,String isCredit) {
		String querySql="select w.bankmid, w.detail,w.txnamt,w.detail,w.mer_tid,w.status,w.trantype from pg_wechat_txn w"
				+ " where mer_orderid =?";
		List<Map<String, Object>> list = dao.queryForList(querySql, orderId);
		if (list.size() < 0) {
			logger.error("[交通银行]公众号支付-查不到原交易：{}",orderId);
			throw new HrtBusinessException(7001, "查不到原交易");
		} else if (list.size() > 1) {
			logger.error("[交通银行]公众号支付-订单号重复：{}",orderId);
			throw new HrtBusinessException(8000, "订单号重复");
		}

		Map<String, Object> map = list.get(0);
		String status = (String) map.get("STATUS");
		if (!"A".equals(status)) {
			if ("1".equals(status)) {
				throw new HrtBusinessException(6001, "交易已经完成支付,请勿重复支付");
			}
			throw new HrtBusinessException(6001, "订单已失效,请重新下单");
		}
		
		String tranType = map.get("TRANTYPE") + "";
		String bankMid = map.get("BANKMID") + "";
		BigDecimal amount = (BigDecimal) map.get("TXNAMT");
		String subject = map.get("DETAIL") + "";
		String tid = map.get("MER_TID") + "";
		
		//2.拼接参数
		Map<String, String> params=new HashMap<>(20);
		params.put("version", "v1.0");
		params.put("unicode", "UTF-8");
		params.put("signType", "MD5");
		params.put("tranCode", "scanpublicpay");
		String payChannel = tranType;
		if ("1".equals(tranType)) {//1 微信
			payChannel = "2";
		} else if ("2".equals(tranType)) {//2 支付宝
			payChannel = "1";
		}
		params.put("payChannel",payChannel); //1:支付宝,2:微信
		params.put("merNo",merNo);
		params.put("subMerNo",bankMid); 
		params.put("merOrderNo",orderId);
		params.put("tranTime",DateUtil.getCompleteTime()); //YYYYMMDDHHMMSS
		BigDecimal amountParam = amount.multiply(new BigDecimal(100));
		params.put("tranAmt",amountParam.intValue()+""); //金额：分
		params.put("detail",subject);
		params.put("currency","156");
		params.put("requestIp",requestIp);
		params.put("noticeUrl",noticeUrl);
		params.put("tranType","JSAPI");
		params.put("subAppid",getWxAppid(bankMid));
		params.put("subOpenid",openid);
		
		String sign = SimpleXmlUtil.getMd5Sign(params, key);
		params.put("sign",sign);
		String result = "";
		
		try {
			logger.info("[交通银行]获取支付信息发送报文：{},\r\n{}",payUrl,params);
			result = netty.sendJson(payUrl, JSONObject.toJSONString(params));
			logger.info("[交通银行]获取支付信息返回报文：{}",result);
		} catch (Exception e) {
			logger.error("[交通银行]获取支付信息网络错误:{}，订单号{}", e,orderId);
			throw new HrtBusinessException(1002, "获取信息网络错误");
		}
		JSONObject json= JSONObject.parseObject(result);
		
		//验签
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = JSONObject.toJavaObject(json, Map.class);
		if (!MsgUtil.verifySign(itemMap, key)) {
			logger.error("[交通银行]获取支付信息返回信息验签失败，订单号{}",orderId);
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(1002, "获取信息验签失败");
		}
		
		//先判通信状态retCode
		String retCode=itemMap.get("retCode");
		if(!"0000".equals(retCode)){
			logger.error("[交通银行]获取支付信息返回-通信状态失败，订单号{},返回{}，{}",orderId,retCode,itemMap.get("retMsg"));
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(1002, "网络通信失败");
		}
		
		String rspCode = itemMap.get("rspCode");
		if ("00".equals(rspCode)) {
			status="0";//待支付
			String sql = "update pg_wechat_txn set status=?,respcode=?,respmsg=?,bankmid=?,"
					+ "bk_orderid=?,lmdate=sysdate where status='A' and mer_orderid=?";
			int count = dao.update(sql, status, rspCode, itemMap.get("rspDesc"), bankMid, itemMap.get("orderNo"),
					orderId);
			if (count < 1) {
				throw new HrtBusinessException(8000, "订单已失效,请重新下单");
			}

			return itemMap.get("payInfo");
		} else {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[交通银行]获取支付信息失败，订单号{},返回{}，{}", orderId, retCode, itemMap.get("retMsg"));
			throw new HrtBusinessException(1002, "下单失败");
		}
	}
	
	
	public String getWxAppid(String merchantCode) {

		String sql = "select APPID from bank_merregister t where t.merchantcode=? and t.fiid=46 ";
		List<Map<String, Object>> list = dao.queryForList(sql, merchantCode);
		if (list.size() > 0) {
			String newAppid = String.valueOf(list.get(0).get("APPID"));
			if (!StringUtils.isEmpty(newAppid) && !"null".equals(newAppid)) {
				return newAppid;
			} else {
				return getWxpayAppid();
			}
		} else {
			return getWxpayAppid();
		}
	}
	
	@Override
	public String getWxpayAppid() {
		return appid;
	}
	
	@Override
	public String getWxpaySecret() {
		return secret;
	}

	@Override
	public int getWxpayFiid() {
		return 46;
	}

	@Override
	public String getAliAppid() {
		// TODO Auto-generated method stub
		return aliAppid;
	}

	@Override
	public String getAlipayPayInfo(String orderId, String openid,String userid,String isCredit) {
		/**
		 * TO-DO
		 * 支付宝 商家支付 （JSAPI）
		 * 
		 */
		String querySql="select w.bankmid, w.detail,w.txnamt,w.detail,w.mer_tid,w.status,w.trantype from pg_wechat_txn w"
				+ " where mer_orderid =?";
		List<Map<String, Object>> list = dao.queryForList(querySql, orderId);
		if (list.size() < 0) {
			logger.error("[交通银行]支付宝公众号支付-查不到原交易：{}",orderId);
			throw new HrtBusinessException(7001, "查不到原交易");
		} else if (list.size() > 1) {
			logger.error("[交通银行]支付宝公众号支付-订单号重复：{}",orderId);
			throw new HrtBusinessException(8000, "订单号重复");
		}

		Map<String, Object> map = list.get(0);
		String status = (String) map.get("STATUS");
		if (!"A".equals(status)) {
			if ("1".equals(status)) {
				throw new HrtBusinessException(6001, "交易已经完成支付,请勿重复支付");
			}
			throw new HrtBusinessException(6001, "订单已失效,请重新下单");
		}
		
		String tranType = map.get("TRANTYPE") + "";
		String bankMid = map.get("BANKMID") + "";
		BigDecimal amount = (BigDecimal) map.get("TXNAMT");
		String subject = map.get("DETAIL") + "";
		String tid = map.get("MER_TID") + "";
		
		//2.拼接参数
		Map<String, String> params=new HashMap<>(20);
		params.put("version", "v1.0");
		params.put("unicode", "UTF-8");
		params.put("signType", "MD5");
		params.put("tranCode", "ordercreate");
		String payChannel = tranType;
		if ("1".equals(tranType)) {//1 微信
			payChannel = "2";
		} else if ("2".equals(tranType)) {//2 支付宝
			payChannel = "1";
		}
		params.put("payChannel","1"); //1:支付宝,2:微信
		params.put("merNo",merNo);
		params.put("subMerNo",bankMid); 
		params.put("merOrderNo",orderId);
		params.put("tranTime",DateUtil.getCompleteTime()); //YYYYMMDDHHMMSS
		BigDecimal amountParam = amount.multiply(new BigDecimal(100));
		params.put("tranAmt",amountParam.intValue()+""); //金额：分
		params.put("detail",subject);
		params.put("currency","156");
		params.put("requestIp",requestIp);
		params.put("noticeUrl",noticeUrl);
		params.put("tranType","JSAPI");
//		params.put("subAppid",userid);//getWxAppid(bankMid)
		params.put("subOpenid",userid);//openid);
		
		String sign = SimpleXmlUtil.getMd5Sign(params, key);
		params.put("sign",sign);
		String result = "";
		
		try {
			logger.info("[交通银行]获取支付信息发送报文：{},\r\n{}",payUrl,params);
			result = netty.sendJson(payUrl, JSONObject.toJSONString(params));
			logger.info("[交通银行]获取支付信息返回报文：{}",result);
		} catch (Exception e) {
			logger.error("[交通银行]获取支付信息网络错误:{}，订单号{}", e,orderId);
			throw new HrtBusinessException(1002, "获取信息网络错误");
		}
		JSONObject json= JSONObject.parseObject(result);
		
		//验签
		@SuppressWarnings("unchecked")
		Map<String, String> itemMap = JSONObject.toJavaObject(json, Map.class);
		if (!MsgUtil.verifySign(itemMap, key)) {
			logger.error("[交通银行]获取支付信息返回信息验签失败，订单号{}",orderId);
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(1002, "获取信息验签失败");
		}
		
		//先判通信状态retCode
		String retCode=itemMap.get("retCode");
		if(!"0000".equals(retCode)){
			logger.error("[交通银行]获取支付信息返回-通信状态失败，订单号{},返回{}，{}",orderId,retCode,itemMap.get("retMsg"));
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(1002, "网络通信失败");
		}
		
		String rspCode = itemMap.get("rspCode");
		if ("00".equals(rspCode)) {
			status="0";//待支付
			String sql = "update pg_wechat_txn set status=?,respcode=?,respmsg=?,bankmid=?,"
					+ "bk_orderid=?,lmdate=sysdate,trantype='2' where status='A' and mer_orderid=?";
			int count = dao.update(sql, status, rspCode, itemMap.get("rspDesc"), bankMid, itemMap.get("orderNo"),
					orderId);
			if (count < 1) {
				throw new HrtBusinessException(8000, "订单已失效,请重新下单");
			}
			return itemMap.get("prepayId");
		} else {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[交通银行]获取支付信息失败，订单号{},返回{}，{}", orderId, retCode, itemMap.get("retMsg"));
			throw new HrtBusinessException(1002, "下单失败");
		}
	}

	@Override
	public String getAlipaySecret() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getAlipayFiid() {
		// TODO Auto-generated method stub
		return 43;
	}

	@Override
	public String getAliAppidByOrder(String orderid) {
		// TODO Auto-generated method stub
		return null;
	}
}
