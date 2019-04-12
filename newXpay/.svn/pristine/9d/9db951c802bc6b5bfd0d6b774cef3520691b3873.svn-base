package com.hrtpayment.xpay.netCups.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.AlipayService;
import com.hrtpayment.xpay.common.service.WxpayService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.common.service.impl.NettyClientService;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.DateUtil;
import com.hrtpayment.xpay.utils.HttpConnectService;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

@Service
public class NetCupsPayService implements WxpayService,AlipayService {

	private final Logger logger = LogManager.getLogger();

	@Autowired
	JdbcDao dao;
	@Autowired
	NotifyService notify;
	@Autowired
	NettyClientService netty;
	@Autowired
	NetCupsService cupsService;
	@Autowired
	MerchantService merService;

	@Value("${netcups.wx.secret}")
	private String secret;
	@Value("${netcups.wx.appid}")
	private String appid;
//	@Value("${netcups.wx.channelId}")
//	private String channelId;
//	@Value("${netcups.wx.mchId}")
//	private String mchId;

	@Value("${netcups.wx.notifyUrl}")
	private String wxNotifyUrl;
	@Value("${netcups.wx.url}")
	private String url;
	private String queryUrl=NetCupsProperties.netCupsQueryUrl;
	private String bsUrl=NetCupsProperties.netCupsBsUrl;
	private String zsUrl=NetCupsProperties.netCupsZsUrl;
	private String refundUrl=NetCupsProperties.netCupsRefundUrl;
	private String refundQueryUrl=NetCupsProperties.netCupsRefundQueryUrl;
	private String idcQueryUrl=NetCupsProperties.netCupsQueryIdcUrl;
	private String cancelUrl=NetCupsProperties.netCupsCancelUrl;
	private String closeUrl=NetCupsProperties.netCupsCloseUrl;
	

	@Value("${netcups.ali.appid}")
	private String aliAppid;
	@Value("${netcups.ali.url}")
	private String aliUrl;

	@Value("${netcups.idc}")
	private String idc;
	
	
    private String getPayType(String bankType){
    	/**
		 * TO-DO 处理交易卡类别
		 */
		String paytype = "";
		if (bankType.contains("DEBIT")) {
			paytype = "1";
		} else if (bankType.contains("CREDIT")) {
			paytype = "2";
		} else {
			paytype = "3";
		}
		return paytype;
    }
	
    public  void  aliIdcQuery(){
    	Map<String, Object> merMsg =new HashMap<String,Object>();
    	merMsg.put("mch_id","2088921593485132W03");
		merMsg.put("method","alipay.other.idc.query");
		Map<String, String> req=cupsService.getPackMessage(merMsg); 
		
		logger.info("请求信息----->"+req);
		String res = null;
		try {
			logger.info("[网联-支付宝]IDC状态查询发送报文：{},\r\n{}", aliUrl, req);
			res = HttpConnectService.postForm(req, aliUrl);
			logger.info("[网联-支付宝]IDC状态查询返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-支付宝]IDC状态查询网络错误:{}", e);
		}
    }
    
    @Override
	public String getAliAppidByOrder(String orderid) {
    	String sql = "select channel_id  "
 				+ "  from bank_merregister t "
 				+ " where t.merchantcode=(select bankmid from pg_wechat_txn where mer_orderid=?) "
 				+ " and t.fiid=61";
 		List<Map<String, Object>> list = dao.queryForList(sql, orderid);
 		if (list.size() > 0) {
 			String newAppid = String.valueOf(list.get(0).get("CHANNEL_ID"));
 			if (!StringUtils.isEmpty(newAppid) && !"null".equals(newAppid)) {
 				return newAppid;
 			} else {
 				return getAliAppid();
 			}
 		} else {
 			return getAliAppid();
 		}
	}
	@Override
	public String getAliAppid() {
		return aliAppid;
	}


	@Override
	public String getAlipayPayInfo(String orderid, String openid, String userid) {
		String querySql="select w.bankmid, w.detail,w.txnamt,w.detail,w.mer_tid,w.status,w.trantype,w.detail,bm.mch_id,bm.channel_id "
				+ " from bank_merregister bm, pg_wechat_txn w"
 				+ " where bm.merchantcode=w.bankmid and bm.fiid=w.fiid and  mer_orderid =?";
 	List<Map<String, Object>> list = dao.queryForList(querySql, orderid);
 	if (list.size() < 0) {
 		logger.error("[网联-支付宝]公众号支付-查不到原交易：{}",orderid);
 		throw new HrtBusinessException(7001, "查不到原交易");
 	} else if (list.size() > 1) {
 		logger.error("[网联-支付宝]公众号支付-订单号重复：{}",orderid);
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
	Map<String, Object> merMsg =new HashMap<String,Object>();
		merMsg.put("bankmid", map.get("bankmid"));
		merMsg.put("amount", map.get("txnamt"));
		merMsg.put("orderid",orderid);
		merMsg.put("subject", map.get("detail"));
		merMsg.put("buyer_id", userid);
		merMsg.put("channel_id", map.get("channel_id"));
		merMsg.put("method","alipay.trade.create");
		Map<String, String> req=cupsService.getPackMessage(merMsg); 
		
		String res=null;
		try {
	 		logger.info("[网联-支付宝]获取创建订单 发送报文：{},\r\n{}",aliUrl,req);
	 		res = HttpConnectService.postForm(req, aliUrl);
			logger.info("[网联-支付宝]获取创建订单返回报文：{}",res);
		} catch (Exception e) {
			logger.error("[网联-支付宝]获取创建订单网络错误:{}，订单号{}", e,orderid);
			throw new HrtBusinessException(1002, "获取信息网络错误");
		} 
		
		Map< String, String> checkMap=JSONObject.parseObject(res, Map.class);
//		if (!cupsService.checkSign(checkMap)) {
//		logger.info("【网联-支付宝】 返回报文 验签失败");
//	}
		JSONObject respJson=JSONObject.parseObject(res);
		Map<String, String> resMap=JSONObject.toJavaObject(JSONObject.parseObject(respJson.get("alipay_trade_create_response").toString()), Map.class);
		String rtnCode=resMap.get("code").toString();
		/*
  		 * 验签开始
  		 */	
  		String sign=respJson.getString("sign");
  		Map<String, String> vertifySign=new  HashMap<String, String>();
  		vertifySign.put("sign", sign);
  		String verSign=net.sf.json.JSONObject.fromObject(res).get("alipay_trade_create_response").toString();
  		vertifySign.put("verSign", verSign);
  		if (!cupsService.checkSign(vertifySign,"2")) {
			logger.info("【网联-支付宝】 创建订单返回报文 验签失败");
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000,"交易失败");
		}
  		/*
  		 * 验签结束
  		 */
  		String rtnMsg=resMap.get("msg").toString();
		
		if ("10000".equals(rtnCode)) {
			String trade_no=resMap.get("trade_no").toString();
			if (trade_no==null||"".equals(trade_no)) {
				RedisUtil.addFailCountByRedis(1);
				logger.error("[网联-支付宝]获取创建订单失败，订单号{},qrcode未返回", resMap.get("out_trade_no"));
				throw new HrtBusinessException(8000, "订单已失效");
			}
			String updateSql="update pg_wechat_txn set respcode=?,respmsg=?,qrcode=?,lmdate=sysdate,isscode=?  where  mer_orderid=? and status<>'1' ";
			dao.update(updateSql, rtnCode,rtnMsg,resMap.get("trade_no"),idc,resMap.get("out_trade_no"));
			return trade_no;
		}else{
			rtnMsg=resMap.get("sub_msg");
			String updateSql="update pg_wechat_txn set respcode=?,respmsg=? ,status='6'  where mer_orderid=?  and status<>'1'  ";
			dao.update(updateSql, rtnCode,rtnMsg, resMap.get("out_trade_no"));
			logger.error("[网联-支付宝]获取创建订单失败，订单号{},返回{}，{}", resMap.get("out_trade_no"), rtnCode, rtnMsg);
			RedisUtil.addFailCountByRedis(1);
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
		return 61;
	}
 
	/**
	 * 网联-支付宝-被扫模式
	 * 
	 * @param authcode
	 * @return
	 */
	public void cupsAliCallBack(Map<String, String> requestMap) throws BusinessException {
		String order_id = requestMap.get("out_trade_no");
		String queryOrder = "select * from pg_wechat_txn where mer_orderid=? ";
		List<Map<String, Object>> list = dao.queryForList(queryOrder, order_id);
		if (0 == list.size()) {
			logger.info("[网联-支付宝] 未找到原订单{}", order_id);
			return;
		}

		if ("1".equals(list.get(0).get("STATUS").toString())) {
			logger.info("[网联-支付宝]  订单{}已经成功，无需进行更新操作。", order_id);
			return;
		}
		String bk_orderid = requestMap.get("trade_no");
		String rtncode = requestMap.get("trade_status");

		String buyer_logon_id = requestMap.get("buyer_logon_id");
		String time_end = null;
		String lmdate="";
		try {
			time_end = DateUtil.formartDate(
					requestMap.get("gmt_payment") == null ? new Date().toString() : requestMap.get("gmt_payment"),
					DateUtil.FORMAT_DATETIME, DateUtil.FORMAT_TRADETIME);
			lmdate=requestMap.get("gmt_payment") == null ? new Date().toString() : requestMap.get("gmt_payment");
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(8000, "时间格式转化错误");
		}

		/**
		 * 判断交易状态
		 */
        try {
        	if ("TRADE_SUCCESS".equals(rtncode)) {
    			String bankType = "";
    			if (requestMap.get("fund_bill_list") != null) {
    				JSONArray json = (JSONArray) JSONArray.parse(requestMap.get("fund_bill_list"));
    				JSONObject fund_bill_list = (JSONObject) json.get(0);
    				bankType = fund_bill_list.getString("fund_channel");
    				if ("BANKCARD".equals(bankType)) {
    					bankType = fund_bill_list.getString("fund_type");
    				}
    			}
    			/**
    			 * TO-DO 判断bankType类型 包含DEBIT paytype=1 借记卡 包含CREDIT paytype=2 贷记卡
    			 * 其它不为空的情况 paytype=3
    			 */
    			String payType = "";
    			payType = getPayType(bankType);
    			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,time_end=?,bank_type=?,"
    					+ "paytype=?,bk_orderid=?,userid=?,status='1',lmdate=to_date(?,'yyyy-mm-dd HH24:mi:ss') ,txnlevel='1' where  status<>'1' and mer_orderid=? ";
    			int count = dao.update(updateSql, rtncode, rtncode, time_end, bankType, payType, bk_orderid, buyer_logon_id,
    					"".equals(lmdate)?"sysdate":lmdate,order_id);
    			if (count == 1) {
    				logger.info("[网联-支付宝]异步通知-交易{}成功，更新{}条", order_id, count);
    				BigDecimal txnLevel = list.get(0).get("TXNLEVEL") == null || "".equals(list.get(0).get("TXNLEVEL"))
    						? BigDecimal.ONE : new BigDecimal("1");
    				list.get(0).put("TXNLEVEL", txnLevel);
    				list.get(0).put("respcode", rtncode);
    				list.get(0).put("respmsg", rtncode);
    				list.get(0).put("banktype", payType);
    				list.get(0).put("userid", buyer_logon_id);
    				notify.sendNotify(list.get(0));
    			} else if (count == 0) {
    				logger.info("[网联-支付宝]异步通知-交易{}未做更新操作，原交易可能成功", order_id);
    			} else {
    				logger.info("[网联-支付宝]异步通知-交易{}更新异常。", order_id);
    			}
    		} else if ("TRADE_FINIISHED".equals(rtncode)) {
    			logger.info("[网联-支付宝]异步通知-交易{}完结。", order_id);
    		} else if ("TRADE_CLOSED".equals(rtncode)) {
    			logger.info("[网联-支付宝]异步通知-交易{}关闭。", order_id);
    		} else if ("WAIT_BUYER_PAY".equals(rtncode)) {
    			logger.info("[网联-支付宝]异步通知-交易{}需要进行查询操作。", order_id);
    			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?, status='0' where  status<>'1' and mer_orderid=? ";
    			dao.update(updateSql, "USERPAYING", rtncode, order_id);
    		}
		} catch (Exception e) {
			logger.error("[网联-支付宝] 异步通知处理异常：{}",e.getMessage());
			RedisUtil.addFailCountByRedis(1);
		}

	}

	/**
	 * 网联支付宝-被扫模式
	 * 
	 * @param authcode
	 * @return
	 */
	public String cupsAliBsPay(HrtPayXmlBean bean, String unno, String mid, int fiid, String payway, String orderid,
			String merchantCode, String authCode, BigDecimal amount, String subject, String tid, String channelId)
			throws BusinessException {

		/**
		 * 1.验重 订单号
		 * 
		 */
		List<Map<String, Object>> list = dao
				.queryForList("select mer_orderid from pg_wechat_txn where mer_orderid=? and rownum=1", orderid);
		if (list.size() > 0)
			throw new BusinessException(9007, "订单号重复");
		/**
		 * 
		 * 2、插入一条初始交易
		 * 
		 */

		String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ "status, cdate, lmdate,unno,mer_tid,trantype,bankmid,isscode ) values((S_PG_Wechat_Txn.nextval),?,'0',?,?,?,?,'0',sysdate,sysdate,?,?,?,?,?)";
		dao.update(insertSql, fiid, orderid, subject, amount, mid, unno, tid, 2, merchantCode,idc);

		/**
		 * 3、组装报文并加密 方法内需要bankmid，amount，orderid,method
		 */
		Map<String, Object> merMsg = new HashMap<String, Object>();
		merMsg.put("bankmid", merchantCode);
		merMsg.put("authcode", authCode);
		merMsg.put("amount", amount);
		merMsg.put("orderid", orderid);
		merMsg.put("subject", subject);
		merMsg.put("channel_id", channelId);
		merMsg.put("method", "alipay.trade.pay");
		Map<String, String> req = cupsService.getPackMessage(merMsg);

		String res = null;
		try {
			logger.info("[网联-支付宝]条码支付发送报文：{},\r\n{}", aliUrl, req);
			res = HttpConnectService.postForm(req, aliUrl);
			logger.info("[网联-支付宝]条码支付返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-支付宝]条码支付网络错误:{}，订单号{}", e, orderid);
			RedisUtil.addFailCountByRedis(1);
			return "R";
		}
		JSONObject respJson = JSONObject.parseObject(res);
		@SuppressWarnings("unchecked")
		Map<String, Object> resMap = JSONObject
				.toJavaObject(JSONObject.parseObject(respJson.get("alipay_trade_pay_response").toString()), Map.class);
		String rtnCode = resMap.get("code").toString();
		String rtnMsg = resMap.get("msg").toString();
		/*
  		 * 验签开始
  		 */	
  		String sign=respJson.getString("sign");
  		Map<String, String> vertifySign=new  HashMap<String, String>();
  		vertifySign.put("sign", sign);
  		String verSign=net.sf.json.JSONObject.fromObject(res).get("alipay_trade_pay_response").toString();
  		vertifySign.put("verSign", verSign);
  		if (!cupsService.checkSign(vertifySign,"2")) {
			logger.info("【网联-支付宝】 条码支付返回报文 验签失败");
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000,"交易失败");
		}
  		/*
  		 * 验签结束
  		 */
		/**
		 * 无论成功失败 都更新 数据 返回R 支持去做查询 以查询结果为准
		 * 
		 * 具体处理逻辑 根据最终实际测试结果为准
		 * 
		 */
		if (bean != null) {
			bean.setRtnCode(rtnCode);
			bean.setRtnMsg(rtnMsg);
			bean.setBankType("");
		}
		try{
			if ("10000".equals(rtnCode)) {
				String updateSql = "update pg_wechat_txn set respcode=?,respmsg=?, lmdate=sysdate ,bk_orderid=?  where mer_orderid=? ";
				dao.update(updateSql, rtnCode, rtnMsg, resMap.get("trade_no"), resMap.get("out_trade_no"));
				return "R";
			} else {
				rtnMsg = (resMap.get("sub_msg") == null ? resMap.get("msg") : resMap.get("sub_msg")).toString();
				String updateSql = "update pg_wechat_txn set respcode=?,respmsg=? ,status='6'  where mer_orderid=? ";
				dao.update(updateSql, rtnCode, rtnMsg, orderid);
				RedisUtil.addFailCountByRedis(1);
				return "E";
			}
		}catch(Exception e){
			logger.error("[网联-支付宝]扫码交易异常：{}",e.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}
	}

	/**
	 * 网联支付宝-获取二维码链接 String orderid,BigDecimal amount
	 * 
	 * @param authcode
	 * @return
	 *
	 */
	public String cupsAliPay(String unno, String mid, String bankMid, String subject, BigDecimal amount, int fiid,
			String payWay, String orderid, String tid, String hybRate, String hybType,String channelId) throws BusinessException {
		// String reqUrl=aliUrl;

		/**
		 * 1.验重 订单号
		 * 
		 */
		List<Map<String, Object>> list = dao
				.queryForList("select mer_orderid from pg_wechat_txn where mer_orderid=? and rownum=1", orderid);
		if (list.size() > 0)
			throw new BusinessException(9007, "订单号重复");
		/**
		 * 
		 * 2、插入一条初始交易
		 * 
		 */
		String sql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ " bankmid ,status, cdate, lmdate,trantype,unno,mer_tid,hybType,hybRate,isscode) values "
				+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,sysdate,sysdate,'2',?,?,?,?,?)";
		dao.update(sql, fiid, orderid, subject, amount, mid, bankMid, 0, unno, tid, hybType, hybRate,idc);

		/**
		 * 3、组装报文并加密 方法内需要bankmid，amount，orderid,method
		 */
		Map<String, Object> merMsg = new HashMap<String, Object>();
		merMsg.put("bankmid", bankMid);
		merMsg.put("amount", amount);
		merMsg.put("orderid", orderid);
		merMsg.put("subject", subject);
		merMsg.put("channel_id", channelId);
		merMsg.put("method", "alipay.trade.precreate");
		Map<String, String> req = cupsService.getPackMessage(merMsg);

		String res = null;
		try {
			logger.info("[网联-支付宝]获取二维码发送报文：{},\r\n{}", aliUrl, req);
			res = HttpConnectService.postForm(req, aliUrl);
			logger.info("[网联-支付宝]获取二维码返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-支付宝]获取二维码网络错误:{}，订单号{}", e, orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}

		JSONObject respJson = JSONObject.parseObject(res);
		Map<String, String> resMap = JSONObject.toJavaObject(
				JSONObject.parseObject(respJson.get("alipay_trade_precreate_response").toString()), Map.class);
		String rtnCode = resMap.get("code").toString();
		String rtnMsg = resMap.get("msg").toString();
		/*
  		 * 验签开始
  		 */	
  		String sign=respJson.getString("sign");
  		Map<String, String> vertifySign=new  HashMap<String, String>();
  		vertifySign.put("sign", sign);
  		String verSign=net.sf.json.JSONObject.fromObject(res).get("alipay_trade_precreate_response").toString();
  		vertifySign.put("verSign", verSign);
  		if (!cupsService.checkSign(vertifySign,"2")) {
			logger.info("【网联-支付宝】 获取二维码返回报文 验签失败");
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000,"交易失败");
		}
  		/*
  		 * 验签结束
  		 */
		try {
			if ("10000".equals(rtnCode)) {
				String qrCode = resMap.get("qr_code").toString();
				if (qrCode == null || "".equals(qrCode)) {
					RedisUtil.addFailCountByRedis(1);
					logger.error("[网联-支付宝]获取二维码失败，订单号{},qrcode未返回", resMap.get("out_trade_no"));
					throw new BusinessException(8000, "订单已失效");
				}
				String updateSql = "update pg_wechat_txn set respcode=?,respmsg=?,qrcode=?,lmdate=sysdate  where mer_orderid=? ";
				dao.update(updateSql, rtnCode, rtnMsg, resMap.get("qr_code"), resMap.get("out_trade_no"));
				return qrCode;
			} else {
				rtnMsg = resMap.get("sub_msg");
				String updateSql = "update pg_wechat_txn set respcode=?,respmsg=? ,status='6'  where mer_orderid=? ";
				dao.update(updateSql, rtnCode, rtnMsg, resMap.get("out_trade_no"));
				logger.error("[网联-支付宝]获取二维码失败，订单号{},返回{}，{}", resMap.get("out_trade_no"), rtnCode, rtnMsg);
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(1002, "下单失败");
			}
		} catch (Exception e) {
			logger.error("[网联-支付宝]获取二维码异常：{}",e.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}

	}

	/**
	 * 网联支付宝-查询
	 * 
	 * @param
	 * @return
	 */
	public String cupsAliQuery(HrtPayXmlBean bean, Map<String, Object> orderInfo) throws BusinessException {

		Map<String, Object> merMsg = new HashMap<String, Object>();
		merMsg.put("orderid", orderInfo.get("mer_orderid"));
		merMsg.put("channel_id", orderInfo.get("channel_id"));
		merMsg.put("method", "alipay.trade.query");
		Map<String, String> req = cupsService.getPackMessage(merMsg);
		String res = null;
		try {
			logger.info("[网联-支付宝]交易查询请求信息----->" + req);
			res = HttpConnectService.postForm(req, aliUrl);
			logger.info("[网联-支付宝]交易查询响应信息----->" + res);
		} catch (Exception e) {
			logger.error("[网联-支付宝]获取交易查询错误{}，订单号{}", e, orderInfo.get("mer_orderid"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}
		JSONObject respJson = JSONObject.parseObject(res);
		@SuppressWarnings("unchecked")
		Map<String, String> resMap = JSONObject.toJavaObject(
				JSONObject.parseObject(respJson.get("alipay_trade_query_response").toString()), Map.class);

		String rtnCode = resMap.get("code");
		String rtnMsg =  resMap.get("msg");
		String trade_status = resMap.get("trade_status");
		String buyer_logon_id =resMap.get("buyer_logon_id");
		/*
  		 * 验签开始
  		 */	
  		String sign=respJson.getString("sign");
  		Map<String, String> vertifySign=new  HashMap<String, String>();
  		vertifySign.put("sign", sign);
  		String verSign=net.sf.json.JSONObject.fromObject(res).get("alipay_trade_query_response").toString();
  		vertifySign.put("verSign", verSign);
  		if (!cupsService.checkSign(vertifySign,"2")) {
			logger.info("【网联-支付宝】查询返回报文 验签失败");
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000,"交易失败");
		}
  		/*
  		 * 验签结束
  		 */
		if ("10000".equals(rtnCode)) {
			String bankType = "";
			if ("TRADE_SUCCESS".equals(trade_status)) {// 交易成功
				try {
					if (resMap.get("fund_bill_list") != null) {
						JSONArray json = JSONArray.parseArray(JSONArray.toJSONString(resMap.get("fund_bill_list")));
						JSONObject fund_bill_list = (JSONObject) json.get(0);
						bankType = fund_bill_list.getString("fund_channel");
						if ("BANKCARD".equals(bankType)) {
							bankType = fund_bill_list.getString("fund_type");
						}
					}
				} catch (Exception e) {
					 logger.error("[网联-支付宝]查询获取banktype异常：{}",e.getMessage());
				}
				
				/**
				 * TO-DO 判断bankType类型 包含DEBIT paytype=1 借记卡 包含CREDIT paytype=2
				 * 贷记卡 其它不为空的情况 paytype=3
				 */
				String payType = "";
				payType = getPayType(bankType);
				String tranTime = "";
				try {
					tranTime = DateUtil.formartDate(String.valueOf(resMap.get("send_pay_date")), DateUtil.FORMAT_DATETIME,
							DateUtil.FORMAT_TRADETIME);
				} catch (Exception e) {
					logger.info("[网联-支付宝]时间转换异常：{}",e.getMessage());
					RedisUtil.addFailCountByRedis(1);
					throw new HrtBusinessException(1001,"查询异常");
				}
				String updateSql = "update pg_wechat_txn set   respmsg=?,bk_orderid=? , lmdate=nvl(to_date(?,'yyyy-mm-dd HH24:mi:ss'),sysdate),time_end=?,txnLevel=?"
						+ " ,userid=?,bank_type=?,paytype=?,status=1  where status!='1' and mer_orderid=?";
				int count = dao.update(updateSql, trade_status, resMap.get("trade_no"), tranTime,tranTime, 1, buyer_logon_id,
						bankType, payType, resMap.get("out_trade_no"));
				if (count > 0) {
					// 推送
					BigDecimal txnLevel = orderInfo.get("TXNLEVEL") == null || "".equals(orderInfo.get("TXNLEVEL"))
							? BigDecimal.ONE : new BigDecimal(String.valueOf(orderInfo.get("TXNLEVEL")));
					orderInfo.put("TXNLEVEL", txnLevel);
					orderInfo.put("respcode", trade_status);
					orderInfo.put("respmsg", trade_status);
					orderInfo.put("banktype", payType);
					orderInfo.put("userid", buyer_logon_id);
					if(bean!=null){
						bean.setUserId(buyer_logon_id);
					}
					notify.sendNotify(orderInfo);
				}
				trade_status = "SUCCESS";
			} else if ("WAIT_BUYER_PAY".equals(trade_status)) {// 等待付款
				String updateSql = "update pg_wechat_txn set respcode=?,   respmsg=?     where  mer_orderid=?";
				dao.update(updateSql, "USERPAYING", trade_status, resMap.get("out_trade_no"));
				trade_status = "DOING";
			} else if ("TRADE_CLOSED".equals(trade_status)) {// 交易超时，或完全退款
				String updateSql = "update pg_wechat_txn set respcode=?,   respmsg=?     where   mer_orderid=?";
				dao.update(updateSql, trade_status, trade_status, resMap.get("out_trade_no"));
				trade_status = "FAIL";
			} else if ("TRADE_FINISHED".equals(trade_status)) {// 交易结束，不可退款
				String updateSql = "update pg_wechat_txn set respcode=?,   respmsg=?    where   mer_orderid=?";
				dao.update(updateSql, trade_status, trade_status, resMap.get("out_trade_no"));
				trade_status = "FAIL";
			} else {
				trade_status = "DOING";
			}

			try {
				if (bean != null) {
					bean.setRtnCode(rtnCode);
					bean.setRtnMsg(rtnMsg);
					bean.setBankType(bankType);
					bean.setUserId(buyer_logon_id == null || "".equals(buyer_logon_id) || "null".equals(buyer_logon_id)
							? "" : buyer_logon_id);
				}
			} catch (Exception e) {
				RedisUtil.addFailCountByRedis(1);
				logger.info("装入bean出错" + e.getMessage());
			}
			return trade_status;
		} else {
			rtnMsg = String.valueOf(resMap.get("sub_msg"));
//			rtnMsg = resMap.get("sub_msg");
			String updateSql = "update pg_wechat_txn set   respmsg=?,bk_orderid=?  "
					+ " ,status=6  where status!='1' and mer_orderid=?";
			dao.update(updateSql, trade_status, resMap.get("trade_no"),   resMap.get("out_trade_no"));
			logger.error("[网联-支付宝]交易查询，订单号{},返回{}，{}", resMap.get("out_trade_no"), rtnCode, rtnMsg);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "查询失败");
		}
	}

	/**
	 * 网联-支付宝 pos专用
	 */
	public String posGetQrCode(String bankMid, String subject, BigDecimal amount, int fiid, String orderid,
			String payWay,String channelId) throws BusinessException {
		/**
		 * 3、组装报文并加密 方法内需要bankmid，amount，orderid,method
		 */
		Map<String, Object> merMsg = new HashMap<String, Object>();
		merMsg.put("bankmid", bankMid);
		merMsg.put("amount", amount);
		merMsg.put("orderid", orderid);
		merMsg.put("subject", subject);
		merMsg.put("channel_id",channelId);
		merMsg.put("method", "alipay.trade.precreate");
		Map<String, String> req = cupsService.getPackMessage(merMsg);

		String res = null;
		try {
			logger.info("[网联-支付宝]获取二维码发送报文：{},\r\n{}", aliUrl, req);
			res = HttpConnectService.postForm(req, aliUrl);
			logger.info("[网联-支付宝]获取二维码返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-支付宝]获取二维码网络错误:{}，订单号{}", e, orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}

		JSONObject respJson = JSONObject.parseObject(res);
		Map<String, String> resMap = JSONObject.toJavaObject(
				JSONObject.parseObject(respJson.get("alipay_trade_precreate_response").toString()), Map.class);
		String rtnCode = resMap.get("code").toString();
		String rtnMsg = resMap.get("msg").toString();
		/*
  		 * 验签开始
  		 */	
  		String sign=respJson.getString("sign");
  		Map<String, String> vertifySign=new  HashMap<String, String>();
  		vertifySign.put("sign", sign);
  		String verSign=net.sf.json.JSONObject.fromObject(res).get("alipay_trade_precreate_response").toString();
  		vertifySign.put("verSign", verSign);
  		if (!cupsService.checkSign(vertifySign,"2")) {
			logger.info("【网联-支付宝】获取二维码返回报文 验签失败");
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000,"交易失败");
		}
  		/*
  		 * 验签结束
  		 */
		if ("10000".equals(rtnCode)) {
			String qrCode = resMap.get("qr_code").toString();
			if (qrCode == null || "".equals(qrCode)) {
				RedisUtil.addFailCountByRedis(1);
				logger.error("[网联-支付宝]获取二维码失败，订单号{},qrcode未返回", resMap.get("out_trade_no"));
				throw new BusinessException(8000, "订单已失效");
			}
			String updateSql = "update pg_wechat_txn set respcode=?,respmsg=?,qrcode=?,lmdate=sysdate  where mer_orderid=? ";
			dao.update(updateSql, rtnCode, rtnMsg, resMap.get("qr_code"), resMap.get("out_trade_no"));
			return qrCode;
		} else {
			rtnMsg = resMap.get("sub_msg");
			String updateSql = "update pg_wechat_txn set respcode=?,respmsg=? ,status='6'  where mer_orderid=? ";
			dao.update(updateSql, rtnCode, rtnMsg, resMap.get("out_trade_no"));
			logger.error("[网联-支付宝]获取二维码失败，订单号{},返回{}，{}", resMap.get("out_trade_no"), rtnCode, rtnMsg);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "下单失败");
		}

	}

	/**
	 * 网联-支付宝 退款
	 * 
	 * @param orderId
	 * @param amount
	 * @param oriOrderInfo
	 * @return
	 * @throws BusinessException
	 */
	public String refundAli(String orderId, BigDecimal amount, Map<String, Object> oriOrderInfo)
			throws BusinessException {

		oriOrderInfo.put("method", "alipay.trade.refund");
		oriOrderInfo.put("channel_id", oriOrderInfo.get("channel_id"));
		oriOrderInfo.put("orderid", orderId);
		Iterator<String> iter = oriOrderInfo.keySet().iterator();
	    while(iter.hasNext()){
	        String key = iter.next();
	        if("mch_id".equals(key)||"MCH_ID".equals(key)){
	            iter.remove();
	        }
	    }
		Map<String, String> req = cupsService.getPackMessage(oriOrderInfo);

		String res = null;
		try {
			logger.info("[网联-支付宝]退款请求信息----->" + req);
			res = HttpConnectService.postForm(req, aliUrl);
			logger.info("[网联-支付宝]退款响应信息----->" + res);
		} catch (Exception e) {
			logger.error("[网联-支付宝]退款请求异常{}，订单号{}", e, oriOrderInfo.get("mer_orderid"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}
		JSONObject jsonRes = new JSONObject();
		JSONObject respJson = JSONObject.parseObject(res);
		@SuppressWarnings("unchecked")
		Map<String, String> resMap = JSONObject.toJavaObject(
				JSONObject.parseObject(respJson.get("alipay_trade_refund_response").toString()), Map.class);
		String rtnCode = resMap.get("code").toString();
		String rtnMsg = resMap.get("msg").toString();
		/*
  		 * 验签开始
  		 */	
  		String sign=respJson.getString("sign");
  		Map<String, String> vertifySign=new  HashMap<String, String>();
  		vertifySign.put("sign", sign);
  		String verSign=net.sf.json.JSONObject.fromObject(res).get("alipay_trade_refund_response").toString();
  		vertifySign.put("verSign", verSign);
  		if (!cupsService.checkSign(vertifySign,"2")) {
			logger.info("【网联-支付宝】退款返回报文 验签失败");
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000,"交易失败");
		}
  		/*
  		 * 验签结束
  		 */
		int count = 0;
		if ("10000".equals(rtnCode)) {
			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,time_end"
					+ "=to_char(sysdate,'yyyymmddhh24miss'),lmdate=sysdate,txnLevel=? ,bk_orderid=? "
					+ ",status='0' where mer_orderid=?";
			count = dao.update(updateSql, rtnCode, rtnMsg, 0, resMap.get("trade_no").toString(), orderId);

			jsonRes.put("errcode", "R");
			jsonRes.put("rtmsg", "退款成功-等待查询 ");
		} else {
			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,"
					+ "time_end=to_char(sysdate,'yyyymmddhh24miss'),lmdate=sysdate," + " txnLevel=? ,status='6'"
					+ " where mer_orderid=? ";
			rtnCode = resMap.get("code");
			rtnMsg = resMap.get("sub_msg");
			count = dao.update(updateSql, rtnCode, rtnMsg, 0, orderId);
			jsonRes.put("errcode", "E");
			jsonRes.put("rtmsg", "退款失败");
			logger.error("[网联-支付宝]退款失败状态订单号{},状态：{},{}", orderId, rtnCode, rtnMsg);
		}

		logger.error("[网联-支付宝]-退款订单号更新{}，订单号{}", count, orderId);
		return jsonRes.toJSONString();
	}

	/**
	 * 网联-支付宝 退款查询
	 * 
	 * @param orderInfo
	 * @return
	 * @throws BusinessException
	 */
	public String refundAliQuery(Map<String, Object> orderInfo) throws BusinessException {
		String reqUrl = null;

		String queryOriOrder = "select mer_orderid from pg_wechat_txn where pwid=? ";
		List<Map<String, Object>> oriOrder = dao.queryForList(queryOriOrder, orderInfo.get("oripwid"));
		if (oriOrder.size() == 0) {
			logger.error("[网联-支付宝]退款查询  退款单号：{}，找不到对应的原交易", orderInfo.get("mer_orderid"));
			throw new BusinessException(8000, "原交易不存在");
		}
		orderInfo.put("method", "alipay.trade.fastpay.refund.query");
		orderInfo.put("oriorderid", oriOrder.get(0).get("mer_orderid"));
		orderInfo.put("channel_id", orderInfo.get("channel_id"));
		orderInfo.put("orderid", orderInfo.get("mer_orderid"));
		Iterator<String> iter = orderInfo.keySet().iterator();
	    while(iter.hasNext()){
	        String key = iter.next();
	        if("mch_id".equals(key)||"MCH_ID".equals(key)){
	            iter.remove();
	        }
	    }
		Map<String, String> req = cupsService.getPackMessage(orderInfo);

		String res = null;
		try {
			logger.info("[网联-支付宝]退款查询请求信息----->" + req);
			res = HttpConnectService.postForm(req, aliUrl);
			logger.info("[网联-支付宝]退款查询响应信息----->" + res);
		} catch (Exception e) {
			logger.error("[网联-支付宝]退款查询请求异常{}，订单号{}", e, orderInfo.get("mer_orderid"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}
		JSONObject jsonRes = new JSONObject();
		JSONObject respJson = JSONObject.parseObject(res);
		@SuppressWarnings("unchecked")
		Map<String, String> resMap = JSONObject.toJavaObject(
				JSONObject.parseObject(respJson.get("alipay_trade_fastpay_refund_query_response").toString()),
				Map.class);
		String rtnCode = resMap.get("code").toString();
		String rtnMsg = resMap.get("msg").toString();
		/*
  		 * 验签开始
  		 */	
  		String sign=respJson.getString("sign");
  		Map<String, String> vertifySign=new  HashMap<String, String>();
  		vertifySign.put("sign", sign);
  		String verSign=net.sf.json.JSONObject.fromObject(res).get("alipay_trade_fastpay_refund_query_response").toString();
  		vertifySign.put("verSign", verSign);
  		if (!cupsService.checkSign(vertifySign,"2")) {
			logger.info("【网联-支付宝】退款查询返回报文 验签失败");
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000,"交易失败");
		}
  		/*
  		 * 验签结束
  		 */
		if ("10000".equals(rtnCode)) {
			String updateSql = "update pg_wechat_txn set respcode=?,respmsg=?, status=1 ,lmdate=sysdate"
					+ "  where  mer_orderid=? ";
			dao.update(updateSql, rtnCode, rtnMsg, orderInfo.get("mer_orderid"));
			return "SUCCESS";
		} else {
			String code = resMap.get("code");
			String subMsg = resMap.get("sub_msg");
			String updateSql = "update pg_wechat_txn set respcode=?,respmsg=?, status=0 ,lmdate=sysdate"
					+ "  where  mer_orderid=? ";
			dao.update(updateSql, code, subMsg, orderInfo.get("mer_orderid"));
			return "FAIL";
		}
	}

	public  void  wxIdcQuery() throws BusinessException{

		Map<String, String> merMsg = new HashMap<String, String>();
		merMsg.put("mch_id", "1900008751");//mchId);// 银行端提供 微信商户号
		merMsg.put("nonce_str", cupsService.getRandomString(32));// 随机字符串
		Map<String, String> req = cupsService.sign(merMsg);
		String reqStr = null;
		try {
			reqStr = cupsService.mapToXml(req);
		} catch (Exception e1) {
			logger.info("[网联-微信] 查询idc状态   请求报文转化异常：{}", e1.getMessage());
			throw new BusinessException(8000, "交易失败");
		}
		String res = null;
		try {
			logger.info("[网联-微信] 查询idc状态 发送报文：{},\r\n{}", url+idcQueryUrl, req);
			res = HttpConnectService.sendMessage(reqStr, url+refundQueryUrl);
			logger.info("[网联-微信] 查询idc状态 返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-微信 ] 查询idc状态 网络错误:{}", e);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}
	}
    
	/**
	 * 公众号支付 先插入订单 
	 * @param unno
	 * @param mid
	 * @param orderid
	 * @param subject
	 * @param amount
	 * @param fiid
	 * @param bankMid
	 * @throws BusinessException
	 */
	public void insertPubaccOrder(String unno, String mid, String orderid, String subject, BigDecimal amount, int fiid,
			String bankMid) throws BusinessException {
		if (subject == null || "".equals(subject)) {
			subject = merService.queryMerName(mid);
		}
		try {
			List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?",
					orderid);
			if (list.size() > 0) {
				throw new BusinessException(8000, "订单号重复");
			}
			String sql = "insert into pg_wechat_txn (pwid,fiid, txntype,cdate,status,"
					+ "mer_orderid, detail, txnamt, mer_id,unno,bankmid,trantype,isscode) values"
					+ "(S_PG_Wechat_Txn.nextval,?,'0',sysdate,'A',?,?,?,?,?,?,'1',?)";
			dao.update(sql, fiid, orderid, subject, amount, mid, unno, bankMid,idc);
		} catch (Exception e) {
			logger.error("插入公众号支付订单失败", e);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(8000, "交易失败");
		}
	}

  
	/**
	 *  
	 *公众号支付上送交易 
	 */
	@Override
	public String getWxpayPayInfo(String orderid, String openid) {
		String querySql = "select w.bankmid, w.detail,w.txnamt,w.detail,w.mer_tid,w.status,w.trantype,w.detail , bm.mch_id,bm.channel_id "
				+ " from pg_wechat_txn w,bank_merregister bm"
				+ " where bm.merchantcode=w.bankmid  and bm.fiid=w.fiid and mer_orderid =?";
		List<Map<String, Object>> list = dao.queryForList(querySql, orderid);
		if (list.size() < 0) {
			logger.error("[网联-微信]公众号支付-查不到原交易：{}", orderid);
			throw new HrtBusinessException(7001, "查不到原交易");
		} else if (list.size() > 1) {
			logger.error("[网联-微信]公众号支付-订单号重复：{}", orderid);
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
		/**
		 * 3、组装报文并加密 方法内需要bankmid，amount，orderid,method
		 */
		Map<String, String> merMsg = new HashMap<String, String>();
		merMsg.put("appid", appid);// 银行端提供 appid
		merMsg.put("mch_id", String.valueOf(map.get("mch_id")));//);// 银行端提供 微信商户号
		merMsg.put("sub_mch_id", String.valueOf(map.get("bankmid"))); // 微信子商户号
		merMsg.put("channel_id", String.valueOf(map.get("channel_id")));//);// 银行端提供 渠道商编号
		merMsg.put("nonce_str", cupsService.getRandomString(32));// 随机字符串
		merMsg.put("trade_type", "JSAPI");// 条码
		merMsg.put("body", String.valueOf(map.get("detail")));// 订单描述
		BigDecimal total_fee = (BigDecimal) map.get("txnamt");
		merMsg.put("total_fee", String.valueOf(total_fee.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));// 订单金额
		merMsg.put("out_trade_no", orderid);// 订单编号
		merMsg.put("notify_url", wxNotifyUrl);
		merMsg.put("spbill_create_ip", "127.0.0.1");// 调用方法的机器的ip
		merMsg.put("idc_flag", idc);
		String subAppid = getWxAppid(String.valueOf(map.get("bankmid")));
		if (!appid.equals(subAppid)) {
			merMsg.put("sub_appid", subAppid);// 银行端提供 微信商户号
			merMsg.put("sub_openid", openid);
		} else {
			merMsg.put("openid", openid);// 进行获取
		}

		Map<String, String> req = cupsService.sign(merMsg);

		String reqStr = null;
		try {
			reqStr = cupsService.mapToXml(req);
		} catch (Exception e1) {
			logger.info("[网联-微信]公众号支付  请求报文转化异常：{}", e1.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易异常");
		}
		String res = null;
		try {
			logger.info("[网联-微信]公众号支付  发送报文：{},\r\n{}", url+zsUrl, req);
//			res = netty.sendFormData(zsUrl, reqStr);
			res=HttpConnectService.sendMessage(reqStr, url+zsUrl);
			logger.info("[网联-微信]公众号支付  返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-微信]公众号支付  网络错误:{}，订单号{}", e, orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(1002, "获取信息网络错误");
		}

		Map<String, String> respJson = null;
		try {
			respJson = cupsService.xmlToMap(res);
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			logger.info("[网联-微信]公众号支付   响应报文转化异常：{}", e.getMessage());
			throw new HrtBusinessException(8000, "公众号支付响应报文处理异常");
		}


		if ("SUCCESS".equals(respJson.get("return_code"))) {
			boolean checkSign = cupsService.checkSign(respJson,"1"); //true;//
			if (!checkSign) {
				logger.info("[网联-微信] 公众号支付 验签失败");
				RedisUtil.addFailCountByRedis(1);
				throw new HrtBusinessException(8000, "验签失败");
			}
			if ("SUCCESS".equals(respJson.get("result_code"))) {
				String updateSql = "update pg_wechat_txn set respcode=?,respmsg=?,status='0',lmdate=sysdate,isscode=?  "
						+ " where status='A' and mer_orderid=? ";
				dao.update(updateSql, respJson.get("result_code"), "公众号下单成功",idc, orderid);
				logger.info("[网联-微信]{}公众号下单成功", orderid);
				return respJson.get("wc_pay_data");
			} else {
				String updateSql = "update pg_wechat_txn set respcode=?,respmsg=?,lmdate=sysdate,status='6' "
						+ " where status='A' and mer_orderid=? ";
				dao.update(updateSql, respJson.get("err_code"), respJson.get("err_code_des"), orderid);
				logger.info("[网联-微信]{}公众号下单失败{}：{}", orderid, respJson.get("err_code"), respJson.get("err_code_des"));
				RedisUtil.addFailCountByRedis(1);
				throw new HrtBusinessException(8000, "公众号下单失败");
			}
		} else {
			String updateSql = "update pg_wechat_txn set respcode=?,respmsg=?,lmdate=sysdate,status='6' "
					+ " where status='A' and mer_orderid=? ";
			dao.update(updateSql, respJson.get("return_code"), respJson.get("return_code_msg"), orderid);
			logger.info("[网联-微信]{}公众号下单失败{}：{}", orderid, respJson.get("return_code"), respJson.get("return_code_msg"));
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "公众号下单失败");

		}
	}
	
    //////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * 
	 * 网联-微信  交易
	 * 
	 */
	//////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * 
	 * 获取 bank_merregister 内的appid
	 * 当bank_merregister表内没有该appid的时候 
	 * 默认获取配置文件内的appid
	 * 
	 * @param merchantCode
	 * @return
	 */
	private String getWxAppid(String merchantCode) {

		String sql = "select APPID from bank_merregister t where t.merchantcode=? and t.fiid=60 ";
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
		// TODO Auto-generated method stub
		return appid;
	}

	@Override
	public String getWxpaySecret() {
		return secret;
	}

	@Override
	public int getWxpayFiid() {
		return 60;
	}

	/**
	 * 网联-微信 异步通知处理
	 * 
	 * @param xml
	 * @throws BusinessException
	 */
	public void cupsWxCallBack(String xml) throws BusinessException {

		Map<String, String> respJson = null;
		try {
			respJson = cupsService.xmlToMap(xml);
		} catch (Exception e) {
			logger.info("[网联-微信]异步通知   响应报文转化异常：{}", e.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(8000, "异步通知报文处理异常");
		}
		logger.info("respMap:" + respJson);
		/*
		 * 验签开始
		 */
		if (!cupsService.checkSign(respJson,"1")) {
			logger.info("[银联-微信] 退款 响应报文验签失败，订单号：{}",respJson.get("out_trade_no"));
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}
		/*
		 * 验签结束
		 */
		String order_id = respJson.get("out_trade_no");
		String queryOrder = "select * from pg_wechat_txn where mer_orderid=? ";
		List<Map<String, Object>> list = dao.queryForList(queryOrder, order_id);
		if (0 == list.size()) {
			logger.info("[网联-微信] 未找到原订单{}", order_id);
			return;
		}

		if ("1".equals(list.get(0).get("STATUS").toString())) {
			logger.info("[网联-微信]  订单{}已经成功，无需进行更新操作。", order_id);
			return;
		}
		/**
		 * TO-DO 处理交易卡类别
		 */
		String paytype = "";
		paytype=getPayType(respJson.get("bank_type"));

		Map<String, Object> orderInfo = list.get(0);
		if ("SUCCESS".equals(respJson.get("return_code"))) {
			if ("SUCCESS".equals(respJson.get("result_code"))) {
				String updateSql = "update pg_wechat_txn set  respcode=?, respmsg=?,time_end=?,"
						+ "lmdate=nvl(to_date(?,'yyyy-mm-dd hh24:mi:ss'),sysdate), bk_orderid=? ,bank_type=?,paytype=?,userid=?,"
						+ " status='1',txnlevel='1' where mer_orderid=?";
				int count = dao.update(updateSql, respJson.get("return_code"), respJson.get("return_code"),
						respJson.get("time_end"),respJson.get("time_end"), respJson.get("transaction_id"), respJson.get("bank_type"), paytype,
						respJson.get("openid"), respJson.get("out_trade_no"));
				if (count == 1) {
					// 推送
					BigDecimal txnLevel = orderInfo.get("TXNLEVEL") == null || "".equals(orderInfo.get("TXNLEVEL"))
							? BigDecimal.ONE : new BigDecimal(String.valueOf(orderInfo.get("TXNLEVEL")));
					orderInfo.put("TXNLEVEL", txnLevel);
					orderInfo.put("RTNCODE", respJson.get("return_code"));
					orderInfo.put("RTNMSG", respJson.get("return_code"));
					orderInfo.put("BANKTYPE", paytype);
					orderInfo.put("USERID", respJson.get("openid"));
					notify.sendNotify(orderInfo);
				}
				logger.info("[网联 -微信] 订单{}交易成功", orderInfo.get("mer_orderid"));
			}
		} else {
			logger.info("[网联 -微信] 订单{} 交易失败 {}：{}", orderInfo.get("return_code"), orderInfo.get("return_msg"));
		}

	}

	/**
	 * 网联微信-退款
	 * 
	 * @param
	 * @return
	 */
	public String cupsWxRefund(String orderId, BigDecimal amount, Map<String, Object> oriOrderInfo)
			throws BusinessException {
		Map<String, String> merMsg = new HashMap<String, String>();
		merMsg.put("appid", appid);// getWxAppid(String.valueOf(oriOrderInfo.get("bankmid"))));//银行端提供
									// appid
		merMsg.put("mch_id", String.valueOf(oriOrderInfo.get("mch_id")));//mchId);// 银行端提供 微信商户号
		merMsg.put("sub_mch_id", String.valueOf(oriOrderInfo.get("bankmid")));//
		merMsg.put("channel_id", String.valueOf(oriOrderInfo.get("channel_id")));//channelId);// 银行端提供 渠道商编号
		merMsg.put("nonce_str", cupsService.getRandomString(32));// 随机字符串
		merMsg.put("out_trade_no", String.valueOf(oriOrderInfo.get("mer_orderid")));// 订单编号
		merMsg.put("out_refund_no", orderId);// 退款订单号
		BigDecimal total_fee = (BigDecimal) oriOrderInfo.get("txnamt");
		merMsg.put("total_fee", String.valueOf(total_fee.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));// 原订单金额
		merMsg.put("refund_fee", String.valueOf(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));// 退款金额
		merMsg.put("idc_flag", idc);// 退款订单号
		Map<String, String> req = cupsService.sign(merMsg);

		String reqStr = null;
		try {
			reqStr = cupsService.mapToXml(req);
		} catch (Exception e1) {
			logger.info("[网联-微信] 退款   请求报文转化异常：{}", e1.getMessage());
			throw new BusinessException(8000, "退款失败");
		}
		String res = null;
		try {
			logger.info("[网联-微信] 退款 发送报文：{},\r\n{}", url+refundUrl, req);
//			res = netty.sendFormData(refundUrl, reqStr);
			res = HttpConnectService.sendMessage(reqStr, url+refundUrl);
			logger.info("[网联-微信] 退款 返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-微信 ] 退款 网络错误:{}，订单号{}", e, oriOrderInfo.get("mer_orderid"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}

		Map<String, String> respJson = null;
		try {
			respJson = cupsService.xmlToMap(res);
		} catch (Exception e) {
			logger.info("[网联-微信]退款   响应报文转化异常：{}", e.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(8000, "退款 响应报文处理异常");
		}
		/*
		 * 验签开始
		 */
		if (!cupsService.checkSign(respJson,"1")) {
			logger.info("[银联-微信] 退款 响应报文验签失败，订单号：{}",oriOrderInfo.get("mer_orderid"));
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}
		/*
		 * 验签结束
		 */
		logger.info("respMap:" + respJson); 
		JSONObject jsonRes = new JSONObject();
		if (!"SUCCESS".equals(respJson.get("return_code"))) {
			String updateSql = "update pg_wechat_txn set status='6', respcode=?,respmsg=? "
					+ " where status<>'1' and  mer_orderid=?";
			dao.update(updateSql, respJson.get("return_code"), respJson.get("return_msg"),
					oriOrderInfo.get("mer_orderid"));

			logger.error("[网联 -微信]  订单{} 退款 失败{}：{}", oriOrderInfo.get("mer_orderid"), respJson.get("return_code"),
					respJson.get("return_msg"));
			throw new BusinessException(1002, "退款失败");
		} else {
			if ("SUCCESS".equals(respJson.get("result_code"))) {
				String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,time_end"
						+ "=to_char(sysdate,'yyyymmddhh24miss'),lmdate=sysdate,txnLevel=? ,bk_orderid=? "
						+ ",status='0' where mer_orderid=?";
				dao.update(updateSql, respJson.get("result_code"), "退款成功", 0, oriOrderInfo.get("mer_orderid"), orderId);
				jsonRes.put("errcode", "R");
				jsonRes.put("rtmsg", "退款成功-等待查询");
				logger.info("[网联 -微信] 订单{} 退款成功", orderId);
			} else {
				String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,"
						+ "time_end=to_char(sysdate,'yyyymmddhh24miss'),lmdate=sysdate,  status='6' "
						+ " where mer_orderid=? ";
				dao.update(updateSql, respJson.get("err_code"), respJson.get("err_code_des"),  orderId);
				logger.info("[网联 -微信] 订单{} 退款 失败{}：{}", orderId, respJson.get("error_code"),
						respJson.get("error_code_des"));
				jsonRes.put("errcode", "E");
				jsonRes.put("rtmsg", "退款失败");
			}
		}
		return jsonRes.toJSONString();

	}

	/**
	 * 网联微信-退款查询
	 * 
	 * @param
	 * @return
	 */
	public String cupsWxRefundQuery(Map<String, Object> orderInfo) throws BusinessException {
		Map<String, String> merMsg = new HashMap<String, String>();
		merMsg.put("appid", appid);// getWxAppid(String.valueOf(orderInfo.get("bankmid"))));//银行端提供
									// appid
		merMsg.put("mch_id", String.valueOf(orderInfo.get("mch_id")));//mchId);// 银行端提供 微信商户号
		merMsg.put("sub_mch_id", String.valueOf(orderInfo.get("bankmid")));
		merMsg.put("nonce_str", cupsService.getRandomString(32));// 随机字符串
		merMsg.put("out_refund_no", String.valueOf(orderInfo.get("mer_orderid")));// 订单编号
		merMsg.put("out_trade_no", String.valueOf(orderInfo.get("bk_orderid")));// 订单编号
		merMsg.put("channel_id", String.valueOf(orderInfo.get("channel_id")));//channelId);// 银行端提供 渠道商编号
		merMsg.put("idc_flag", idc);// 银行端提供 渠道商编号

		Map<String, String> req = cupsService.sign(merMsg);

		String reqStr = null;
		try {
			reqStr = cupsService.mapToXml(req);
		} catch (Exception e1) {
			logger.info("[网联-微信] 退款查询   请求报文转化异常：{}", e1.getMessage());
			throw new BusinessException(8000, "退款失败");
		}
		String res = null;
		try {
			logger.info("[网联-微信] 退款查询 发送报文：{},\r\n{}", url+refundQueryUrl, req);
			res = HttpConnectService.sendMessage(reqStr, url+refundQueryUrl);
			logger.info("[网联-微信] 退款查询 返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-微信 ] 退款查询 网络错误:{}，订单号{}", e, orderInfo.get("mer_orderid"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}

		Map<String, String> respJson = null;
		try {
			respJson = cupsService.xmlToMap(res);
		} catch (Exception e) {
			logger.info("[网联-微信]退款查询   响应报文转化异常：{}", e.getMessage());
			throw new BusinessException(8000, "退款 响应报文处理异常");
		}
		/*
		 * 验签开始
		 */
		if (!cupsService.checkSign(respJson,"1")) {
			logger.info("[银联-微信] 退款查询 响应报文验签失败，订单号：{}",orderInfo.get("mer_orderid"));
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}
		/*
		 * 验签结束
		 */
		if (!"SUCCESS".equals(respJson.get("return_code"))) {
			String updateSql = "update pg_wechat_txn set status='6', respcode=?,respmsg=? "
					+ " where status<>'1' and  mer_orderid=?";
			dao.update(updateSql, respJson.get("return_code"), respJson.get("return_msg"),
					orderInfo.get("mer_orderid"));

			logger.error("[网联 -微信]  订单{} 退款 失败{}：{}", orderInfo.get("mer_orderid"), respJson.get("return_code"),
					respJson.get("return_msg"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "退款失败");
		} else {
			if ("SUCCESS".equals(respJson.get("result_code"))) {
				if ("SUCCESS".equals(respJson.get("refund_status"))) {
					  String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,time_end"
								+ "=to_char(sysdate,'yyyymmddhh24miss'),lmdate=sysdate,txnLevel=? ,bk_orderid=? "
								+ ",status='1' where mer_orderid=?";
					  dao.update(updateSql, respJson.get("result_code"), "退款成功", 0, respJson.get("out_refund_no").toString(), orderInfo.get("mer_orderid"));
	                  logger.info("[网联 -微信] 订单{} 退款成功", orderInfo);
	                  return "SUCCESS";
					}else if ("PROCESSING".equals(respJson.get("refund_status"))){
					  logger.info("[网联 -微信] 订单{} 退款处理中", orderInfo);
	                  return "DOING";
					}else{
					   String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,"
								+ "time_end=to_char(sysdate,'yyyymmddhh24miss'),lmdate=sysdate,"
								+ " txnLevel=? ,status='6'"
								+ " where mer_orderid=? "; 
					   dao.update(updateSql, respJson.get("err_code"), respJson.get("err_code_des"), 0,  orderInfo);
					   logger.info("[网联 -微信] 订单{} 退款 失败{}：{}", orderInfo, respJson.get("error_code"), respJson.get("error_code_des"));
					   return "FAIL";
					}
			} else {
				String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,"
						+ "time_end=to_char(sysdate,'yyyymmddhh24miss'),lmdate=sysdate, status='6'"
						+ " where mer_orderid=? ";
				dao.update(updateSql, respJson.get("err_code"), respJson.get("err_code_des"),  orderInfo);
				logger.info("[网联 -微信] 订单{} 退款 失败{}：{}", orderInfo, respJson.get("error_code"),
						respJson.get("error_code_des"));
				return "FAIL";
			}
		}

	}

	/**
	 * 网联微信-获取二维码链接 String orderid,BigDecimal amount
	 * 
	 * @param authcode
	 * @return
	 *
	 */
	public String cupsWxPay(String unno, String mid, String bankMid, String subject, BigDecimal amount, int fiid,
			String payWay, String orderid, String tid, String hybRate, String hybType,String mchId,String channelId) throws BusinessException {
		/**
		 * 1.验重 订单号
		 * 
		 */
		List<Map<String, Object>> list = dao
				.queryForList("select mer_orderid from pg_wechat_txn where mer_orderid=? and rownum=1", orderid);
		if (list.size() > 0)
			throw new BusinessException(9007, "订单号重复");
		/**
		 * 
		 * 2、插入一条初始交易
		 * 
		 */
		String sql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ " bankmid ,status, cdate, lmdate,trantype,unno,mer_tid,hybType,hybRate,isscode) values "
				+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,sysdate,sysdate,'1',?,?,?,?,?)";
		dao.update(sql, fiid, orderid, subject, amount, mid, bankMid, 0, unno, tid, hybType, hybRate,idc);
		/**
		 * 3、组装报文并加密 方法内需要bankmid，amount，orderid,method
		 */
		Map<String, String> merMsg = new HashMap<String, String>();
		merMsg.put("appid", appid);// getWxAppid(bankMid));//银行端提供 appid
		merMsg.put("mch_id", mchId);// 银行端提供 微信商户号
		merMsg.put("sub_mch_id", bankMid); // 微信子商户号
		merMsg.put("channel_id", channelId);// 银行端提供 渠道商编号
		merMsg.put("nonce_str", cupsService.getRandomString(32));// 随机字符串
		merMsg.put("trade_type", "NATIVE");// 条码
		merMsg.put("body", subject);// 订单描述
		merMsg.put("total_fee", String.valueOf(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));// 订单金额
		merMsg.put("out_trade_no", orderid);// 订单编号
		merMsg.put("notify_url", wxNotifyUrl);
		merMsg.put("spbill_create_ip", "127.0.0.1");// 调用方法的机器的ip
		merMsg.put("device_info", "T" + bankMid + "001");
		merMsg.put("idc_flag", idc);
		Map<String, String> req = cupsService.sign(merMsg);

		String reqStr = null;
		try {
			reqStr = cupsService.mapToXml(req);
		} catch (Exception e1) {
			logger.info("[网联-微信]获取二维码  请求报文转化异常：{}", e1.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(8000, "交易异常");
		}
		String res = null;
		try {
			logger.info("[网联-微信]获取二维码发送报文：{},\r\n{}", url+zsUrl, req);
//			res = netty.sendFormData(zsUrl, reqStr);
//			res =netty.sendXml(zsUrl, reqStr);
			res =HttpConnectService.sendMessage(reqStr, url+zsUrl);
			logger.info("[网联-微信]获取二维码返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-微信]获取二维码网络错误:{}，订单号{}", e, orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}

		Map<String, String> respJson = null;
		try {
			respJson = cupsService.xmlToMap(res);
		} catch (Exception e) {
			logger.info("[网联-微信]获取二维码   响应报文转化异常：{}", e.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(8000, "获取二维码响应报文处理异常");
		}
		
		/*
		 * 验签开始
		 */
		if (!cupsService.checkSign(respJson,"1")) {
			logger.info("[银联-微信] 获取二维码   响应报文验签失败，订单号：{}",orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}
		/*
		 * 验签结束
		 */
		if (!"SUCCESS".equals(respJson.get("return_code"))) {
			String updateSql = "update pg_wechat_txn set status='6', respcode=?,respmsg=? "
					+ " where status<>'1' and  mer_orderid=?";
			dao.update(updateSql, respJson.get("return_code"), respJson.get("return_msg"), orderid);

			logger.error("[网联 -微信] 获取二维码失败{}：{}", respJson.get("return_code"), respJson.get("return_msg"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取二维码失败");
		} else {
			if ("SUCCESS".equals(respJson.get("result_code"))) {
				String qrcode = "";
				String updateSql = "update pg_wechat_txn set respcode=?,respmsg=? ,qrcode=?, lmdate=sysdate "
						+ " where status<>'1' and  mer_orderid=?";
				dao.update(updateSql, respJson.get("result_code"), respJson.get("result_code"),
						respJson.get("code_url"), orderid);
				logger.info("[网联-微信]获取二维码成功，订单号{}", orderid);
				qrcode = respJson.get("code_url");
				if (null == qrcode || "".equals(qrcode)) {
					RedisUtil.addFailCountByRedis(1);
					throw new BusinessException(1002, "下单失败");
				}
				return qrcode;
			} else {
				String updateSql = "update pg_wechat_txn set status='6', respcode=?,respmsg=? "
						+ " where status<>'1' and  mer_orderid=?";
				dao.update(updateSql, respJson.get("err_code"), respJson.get("err_code_des"), orderid);
				logger.error("[网联-微信]获取二维码失败，订单号{},返回{}，{}", orderid, respJson.get("err_code"),
						respJson.get("err_code_des"));
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(1002, "下单失败");
			}
		}

	}

	/**
		 * 网联微信-查询
		 * 
		 * @param
		 * @return
		 */
		public String cupsWxQuery(HrtPayXmlBean bean, Map<String, Object> orderInfo) throws BusinessException {
			Map<String, String> merMsg = new HashMap<String, String>();
			merMsg.put("appid", appid);// getWxAppid(String.valueOf(orderInfo.get("bankmid"))));//银行端提供
										// appid
			merMsg.put("mch_id", String.valueOf(orderInfo.get("mch_id")));//mchId);// 银行端提供 微信商户号
			merMsg.put("sub_mch_id", String.valueOf(orderInfo.get("bankmid"))); // 微信子商户号
			merMsg.put("channel_id", String.valueOf(orderInfo.get("channel_id")));//channelId);// 银行端提供 渠道商编号
			merMsg.put("nonce_str", cupsService.getRandomString(32));// 随机字符串
			merMsg.put("idc_flag",idc);// 随机字符串
			merMsg.put("out_trade_no", String.valueOf(orderInfo.get("mer_orderid")));// 订单编号
			Map<String, String> req = cupsService.sign(merMsg);
	
			String reqStr = null;
			try {
				reqStr = cupsService.mapToXml(req);
			} catch (Exception e1) {
				logger.info("[网联-微信]订单查询   请求报文转化异常：{}", e1.getMessage());
				throw new BusinessException(8000, "查询失败");
			}
			String res = null;
			try {
				logger.info("[网联-微信] 订单查询发送报文：{},\r\n{}", url+queryUrl, req);
	//			res = netty.sendFormData(queryUrl, reqStr);
				res =HttpConnectService.sendMessage(reqStr, url+queryUrl);
				logger.info("[网联-微信] 订单查询返回报文：{}", res);
			} catch (Exception e) {
				logger.error("[网联-微信 ] 订单查询网络错误:{}，订单号{}", e, orderInfo.get("mer_orderid"));
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(1002, "获取信息网络错误");
			}
	
			Map<String, String> respJson = null;
			try {
				respJson = cupsService.xmlToMap(res);
			} catch (Exception e) {
				logger.info("[网联-微信]订单查询   响应报文转化异常：{}", e.getMessage());
				throw new BusinessException(8000, "订单查询响应报文处理异常");
			}

			/*
			 * 验签开始
			 */
			if (!cupsService.checkSign(respJson,"1")) {
				logger.info("[银联-微信] 获取二维码   响应报文验签失败，订单号：{}",orderInfo.get("mer_orderid"));
				RedisUtil.addFailCountByRedis(1);
				throw new HrtBusinessException(8000, "交易失败");
			}
			/*
			 * 验签结束
			 */
			if (!"SUCCESS".equals(respJson.get("return_code"))) {
				String updateSql = "update pg_wechat_txn set status='6', respcode=?,respmsg=? "
						+ " where status<>'1' and  mer_orderid=?";
				dao.update(updateSql, respJson.get("return_code"), respJson.get("return_msg"),
						orderInfo.get("mer_orderid"));
	
				logger.error("[网联 -微信]  订单查询 失败{}：{}", respJson.get("return_code"), respJson.get("return_msg"));
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(1002, "查询失败");
			} else {
				if ("SUCCESS".equals(respJson.get("result_code"))) {
					if ("SUCCESS".equals(respJson.get("trade_state"))) {
						/**
						 * TO-DO 处理交易卡类别
						 */
						String paytype = "";
						paytype=getPayType(respJson.get("bank_type"));
						String updateSql = "update pg_wechat_txn set status='1', respcode=?,respmsg=? ,qrcode=?, lmdate=nvl(to_date(?,'yyyy-mm-dd hh24:mi:ss'),sysdate),bk_orderid=?,time_end=?, "
								+ " bank_type=?,paytype=? ,userid=?,txnlevel='1' where status<>'1' and  mer_orderid=?";
						int count = dao.update(updateSql, respJson.get("trade_state"), respJson.get("trade_state_desc"),
								respJson.get("code_url"), respJson.get("time_end"), respJson.get("transaction_id"), respJson.get("time_end"),
								respJson.get("bank_type"), paytype, respJson.get("openid"), orderInfo.get("mer_orderid"));
						if (count == 1) {
							// 推送
							BigDecimal txnLevel = orderInfo.get("TXNLEVEL") == null || "".equals(orderInfo.get("TXNLEVEL"))
									? BigDecimal.ONE : new BigDecimal(String.valueOf(orderInfo.get("TXNLEVEL")));
							orderInfo.put("TXNLEVEL", txnLevel);
							orderInfo.put("RTNCODE", respJson.get("trade_state"));
							orderInfo.put("RTNMSG", respJson.get("trade_state_desc"));
							orderInfo.put("BANKTYPE", paytype);
							orderInfo.put("USERID", respJson.get("openid"));
							if(bean!=null){
								bean.setUserId(respJson.get("openid"));
							}
							logger.info("[网联 -微信] 查询订单{} ：{}||{}", orderInfo.get("mer_orderid"),
									respJson.get("trade_state"), respJson.get("trade_state_desc"));
							notify.sendNotify(orderInfo);
						}
						logger.info("[网联-微信]订单查询  成功，订单号{}", orderInfo.get("mer_orderid"));
						return "SUCCESS";
					} else if ("USERPAYING".equals(respJson.get("trade_state"))) {
						String updateSql = "update pg_wechat_txn set respcode=?,respmsg=? ,qrcode=?, lmdate=sysdate "
								+ " where status<>'1' and  mer_orderid=?";
						dao.update(updateSql, respJson.get("trade_state"), respJson.get("trade_state_desc"),
								respJson.get("code_url"), orderInfo.get("mer_orderid"));
						logger.info("[网联 -微信] 查询订单{} ：{}||{}", orderInfo.get("mer_orderid"), respJson.get("trade_state"),
								respJson.get("trade_state_desc"));
						return "DOING";
					} else if ("PAYERROR".equals(respJson.get("trade_state"))) {
						String updateSql = "update pg_wechat_txn set respcode=?,respmsg=? ,status=?, lmdate=sysdate "
								+ " where status<>'1' and  mer_orderid=?";
						dao.update(updateSql, respJson.get("trade_state"), respJson.get("trade_state_desc"),
								"6", orderInfo.get("mer_orderid"));
						logger.info("[网联 -微信] 查询订单{} ：{}||{}", orderInfo.get("mer_orderid"), respJson.get("trade_state"),
								respJson.get("trade_state_desc"));
						return "FAIL";
					}else {
						String updateSql = "update pg_wechat_txn set  respcode=?,respmsg=? ,qrcode=?, lmdate=sysdate "
								+ " where status<>'1' and  mer_orderid=?";
						dao.update(updateSql, respJson.get("trade_state"), respJson.get("trade_state_desc"),
								respJson.get("code_url"), orderInfo.get("mer_orderid"));
						logger.info("[网联 -微信] 查询订单{}状态{}||{}", orderInfo.get("mer_orderid"), respJson.get("trade_state"),
								respJson.get("trade_state_desc"));
						return "FAIL";
					}
				} else {
					String updateSql = "update pg_wechat_txn set status='6', respcode=?,respmsg=? "
							+ " where status<>'1' and  mer_orderid=?";
					dao.update(updateSql, respJson.get("err_code"), respJson.get("err_code_des"),
							orderInfo.get("mer_orderid"));
					logger.error("[网联-微信]订单查询 失败，订单号{},返回{}，{}", orderInfo.get("mer_orderid"), respJson.get("err_code"),
							respJson.get("err_code_des"));
					RedisUtil.addFailCountByRedis(1);
					throw new BusinessException(8000, "查询异常");
				}
			}
	
		}



	/**
	 * 网联微信-被扫模式
	 * 
	 * @param authcode
	 * @return
	 */
	public String cupsWxBsPay(HrtPayXmlBean bean, String unno, String mid, int fiid, String payway, String orderid,
			String merchantCode, String authCode, BigDecimal amount, String subject, String tid,String mchId,String channelId)
			throws BusinessException {
		/**
		 * 1.验重 订单号
		 * 
		 */
		List<Map<String, Object>> list = dao
				.queryForList("select mer_orderid from pg_wechat_txn where mer_orderid=? and rownum=1", orderid);
		if (list.size() > 0)
			throw new BusinessException(9007, "订单号重复");
		/**
		 * 
		 * 2、插入一条初始交易
		 * 
		 */
		String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ "status, cdate, lmdate,unno,mer_tid,trantype,bankmid,isscode) values((S_PG_Wechat_Txn.nextval),?,'0',?,?,?,?,'0',sysdate,sysdate,?,?,?,?,?)";
		dao.update(insertSql, fiid, orderid, subject, amount, mid, unno, tid, 1, merchantCode,idc);

		/**
		 * 3、组装报文并加密
		 * 
		 */
		Map<String, String> merMsg = new HashMap<String, String>();
		merMsg.put("appid", appid);// getWxAppid(merchantCode));//银行端提供 appid
		merMsg.put("mch_id", mchId);// 银行端提供 微信商户号
		merMsg.put("sub_mch_id", merchantCode); // 微信子商户号
		merMsg.put("channel_id", channelId);// 银行端提供 渠道商编号
		merMsg.put("nonce_str", cupsService.getRandomString(32));// 随机字符串
		merMsg.put("auth_code", authCode);// 条码
		merMsg.put("body", subject);// 订单描述
		merMsg.put("total_fee", String.valueOf(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));// 订单金额
		merMsg.put("out_trade_no", orderid);// 订单编号
		merMsg.put("spbill_create_ip", "127.0.0.1");// 调用方法的机器的ip
		merMsg.put("idc_flag", idc);
		merMsg.put("device_info", "T" + merchantCode + "001");
		Map<String, String> req = cupsService.sign(merMsg);// cupsService.getPackMessageForWx(merMsg);

		String reqStr = null;
		try {
			reqStr = cupsService.mapToXml(req);
		} catch (Exception e1) {
			logger.info("[网联-微信]条码交易  请求报文转化异常：{}", e1.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(8000, "商户入驻异常");
		}
		String res = null;
		try {
			logger.info("[网联-微信]条码交易  发送报文：{},\r\n{}", url+bsUrl, req);
//			res = netty.sendFormData(bsUrl, reqStr);
			res = HttpConnectService.sendMessage(reqStr, url+bsUrl);
			logger.info("[网联-微信]条码交易  返回报文：{}", res);
		} catch (Exception e) {
			logger.error("[网联-微信]条码交易  网络错误:{}，订单号{}", e, orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "获取信息网络错误");
		}

		Map<String, String> respJson = null;
		try {
			respJson = cupsService.xmlToMap(res);
		} catch (Exception e) {
			logger.error("[网联-微信]条码交易  响应报文转化异常：{}", e.getMessage());
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(8000, "条码交易响应报文处理异常");
		}
		/*
		 * 验签开始
		 */
		if (!cupsService.checkSign(respJson,"1")) {
			logger.info("[银联-微信] 条码交易     响应报文验签失败，订单号：{}",orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}
		/*
		 * 验签结束
		 */
		/**
		 * 无论成功失败 都更新 数据 返回R 支持去做查询 以查询结果为准
		 * 
		 * 具体处理逻辑 根据最终实际测试结果为准
		 * 
		 */
		if (!"SUCCESS".equals(respJson.get("return_code"))) {
			String updateSql = "update pg_wechat_txn set status='6', respcode=?,respmsg=? "
					+ " where status<>'1' and  mer_orderid=?";
			dao.update(updateSql, respJson.get("return_code"), respJson.get("return_msg"), orderid);

			logger.error("[网联 -微信]条码交易 失败{}：{}", respJson.get("return_code"), respJson.get("return_msg"));
			RedisUtil.addFailCountByRedis(1);
			throw new BusinessException(1002, "条码交易失败");
		} else {
			if ("SUCCESS".equals(respJson.get("result_code"))) { 
				/**
				 * TO-DO 测试点
				 */
				String updateSql = "update pg_wechat_txn set status='1',bk_orderid=?, respcode=?,respmsg=? ,userid=?,bank_type=?,paytype=?,time_end=? "
						+ " where status<>'1' and  mer_orderid=?";
				dao.update(updateSql, respJson.get("transaction_id"),respJson.get("return_code"), respJson.get("result_code"),respJson.get("openid"),respJson.get("bank_type"),  getPayType(respJson.get("bank_type")),respJson.get("time_end") ,orderid);
				if (bean != null) {
					bean.setRtnCode(respJson.get("err_code"));
					bean.setRtnMsg(respJson.get("err_code_des"));
					bean.setUserId(respJson.get("openid"));
					bean.setBankType(getPayType(respJson.get("bank_type")));
				}
				logger.info("[网联-微信]条码交易 成功，订单号{},返回{}，{}", orderid, respJson.get("return_code"), respJson.get("result_code"));
				return "S";
			} else {
				if ("USERPAYING".equals(respJson.get("err_code"))) {
					String updateSql = "update pg_wechat_txn set   respcode=?,respmsg=? ,qrcode=?, lmdate=sysdate,time_end=? "
							+ " where status<>'1' and  mer_orderid=?";
					dao.update(updateSql, respJson.get("err_code"), "订单支付中", respJson.get("code_url"),
							respJson.get("time_end"), orderid);
					logger.info("[网联-微信]条码交易等待付款，订单号{}", orderid);
					return "R";
				}
				String updateSql = "update pg_wechat_txn set status='6', respcode=?,respmsg=? "
						+ " where status<>'1' and  mer_orderid=?";
				dao.update(updateSql, respJson.get("err_code"), respJson.get("err_code_des"), orderid);
				logger.error("[网联-微信]条码交易 失败，订单号{},返回{}，{}", orderid, respJson.get("err_code"),
						respJson.get("err_code_des"));
				RedisUtil.addFailCountByRedis(1);
				if (bean != null) {
					bean.setRtnCode(respJson.get("err_code"));
					bean.setRtnMsg(respJson.get("err_code_des"));
					bean.setBankType("");
					bean.setUserId("");
				}
				return "E";
			}
		}

	}

	//////////////////////////////////////////////////////////////////////////////
	/*
	 * 下述接口 暂不启用 1、cupsAliClosed 2、cupsAliCancel 3、cupsWxClosed 4、cupsWxCancel
	 */
	//////////////////////////////////////////////////////////////////////////////

	/**
	 * 网联微信-撤销
	 * 
	 * @param
	 * @return
	 */
	 public String cupsWxCancel(String bankmid,String orderid,String mchId,String channelId) throws
	 BusinessException {
	 Map<String, String> merMsg =new HashMap<String,String>();
	 merMsg.put("appid",appid);//getWxAppid(String.valueOf(orderInfo.get("bankmid"))));//银行端提供
	 merMsg.put("mch_id",mchId);//银行端提供 微信商户号
	 merMsg.put("sub_mch_id",bankmid); //微信子商户号
	 merMsg.put("channel_id",channelId);//银行端提供 渠道商编号
	 merMsg.put("nonce_str", cupsService.getRandomString(32));//随机字符串
	 merMsg.put("out_trade_no",orderid);//订单编号
	 merMsg.put("idc_flag",idc);//订单编号
	 Map<String, String> req=cupsService.sign(merMsg);
	
	 String reqStr=null;
	 try {
	 reqStr=cupsService.mapToXml(req) ;
	 } catch (Exception e1) {
	 logger.info("[网联-微信]订单撤销 请求报文转化异常：{}",e1.getMessage());
	 throw new BusinessException(8000, "查询失败");
	 }
	 logger.info("请求信息----->"+reqStr);
	 String res=null;
	 try {
	 logger.info("[网联-微信] 订单撤销发送报文：{},\r\n{}",url+cancelUrl,req);
//	 res = netty.sendFormData(cancelUrl, reqStr);
	 res=HttpConnectService.sendMessage(reqStr, url+cancelUrl);
	 logger.info("[网联-微信] 订单撤销返回报文：{}",res);
	 } catch (Exception e) {
	 logger.error("[网联-微信 ] 订单撤销网络错误:{}，订单号{}", e,orderid);
	 RedisUtil.addFailCountByRedis(1);
	 throw new BusinessException(1002, "获取信息网络错误");
	 }
	
	
	 Map<String, String> respJson=null;
	 try {
	 respJson = cupsService.xmlToMap(res);
	 } catch (Exception e) {
	 logger.info("[网联-微信]订单撤销 响应报文转化异常：{}",e.getMessage());
	 throw new BusinessException(8000, "订单撤销响应报文处理异常");
	 }
	 logger.info("respMap:"+respJson);
	 /*
		 * 验签开始
		 */
		if (!cupsService.checkSign(respJson,"1")) {
			logger.info("[银联-微信] 订单撤销   响应报文验签失败，订单号：{}",orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}
		/*
		 * 验签结束
		 */
	 return "";
	
	 }
	/**
	 * 网联微信-关闭
	 * 
	 * @param
	 * @return
	 */
	 public String cupsWxClose(String bankmid,String orderid,String mchId,String channelId) throws
	 BusinessException {
	 Map<String, String> merMsg =new HashMap<String,String>();
	 merMsg.put("appid",appid);//getWxAppid(String.valueOf(orderInfo.get("bankmid"))));//银行端提供
	 merMsg.put("mch_id",mchId);//银行端提供 微信商户号
	 merMsg.put("sub_mch_id",bankmid); //微信子商户号
	 merMsg.put("channel_id",channelId);//银行端提供 渠道商编号
	 merMsg.put("nonce_str", cupsService.getRandomString(32));//随机字符串
	 merMsg.put("out_trade_no",orderid);//订单编号
	 merMsg.put("idc_flag",idc);//订单编号
	 Map<String, String> req=cupsService.sign(merMsg);
	
	 String reqStr=null;
	 try {
	 reqStr=cupsService.mapToXml(req) ;
	 } catch (Exception e1) {
	 logger.info("[网联-微信]订单关闭 请求报文转化异常：{}",e1.getMessage());
	 throw new BusinessException(8000, "查询失败");
	 }
	 logger.info("请求信息----->"+reqStr);
	 String res=null;
	 try {
	 logger.info("[网联-微信] 订单关闭发送报文：{},\r\n{}",url+closeUrl,req);
//	 res = netty.sendFormData(closeUrl, reqStr);
	 res=HttpConnectService.sendMessage(reqStr, url+closeUrl);
	 logger.info("[网联-微信] 订单关闭返回报文：{}",res);
	 } catch (Exception e) {
	 logger.error("[网联-微信 ] 订单关闭网络错误:{}，订单号{}", e,orderid);
	 RedisUtil.addFailCountByRedis(1);
	 throw new BusinessException(1002, "获取信息网络错误");
	 }
	
	
	 Map<String, String> respJson=null;
	 try {
	 respJson = cupsService.xmlToMap(res);
	 } catch (Exception e) {
	 logger.info("[网联-微信]订单关闭 响应报文转化异常：{}",e.getMessage());
	 throw new BusinessException(8000, "订单关闭响应报文处理异常");
	 }
	 logger.info("respMap:"+respJson);
	 /*
		 * 验签开始
		 */
		if (!cupsService.checkSign(respJson,"1")) {
			logger.info("[银联-微信] 订单关闭   响应报文验签失败，订单号：{}",orderid);
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000, "交易失败");
		}
		/*
		 * 验签结束
		 */
	 return "";
	
	 }

	
	 /**
	 * 网联-支付宝 撤销
	 * 搁置 暂时停用
	 * @param orderInfo
	 * @return
	 * @throws BusinessException
	 */
	 public void cupsAliCancel(String orderid) throws BusinessException {
	 String reqUrl=aliUrl;
	 Map<String, Object> orderInfo=new HashMap<String, Object>();
	 orderInfo.put("method", "alipay.trade.cancel");
	 /*
	  * TO-DO 
	  * 测试写死 “2088921593485132W03”
	  * 生产若启用  本值为bank_merregister表内channel_id
	  */
	 orderInfo.put("channel_id", "2088921593485132W03");
	 orderInfo.put("mer_orderid",orderid);
	
	 Map<String, String> req=cupsService.getPackMessage(orderInfo);
	 String res=null;
	 try {
	 logger.info("[网联-支付宝]撤销请求信息----->"+req);
	 res =HttpConnectService.postForm(req, reqUrl);
	 logger.info("[网联-支付宝]撤销响应信息----->"+res);
	 } catch (Exception e) {
	 logger.error("[网联-支付宝]撤销请求异常{}，订单号{}", e,orderInfo.get("mer_orderid"));
	 throw new BusinessException(1002, "获取信息网络错误");
	 }
	 JSONObject jsonRes =new JSONObject();
	 JSONObject respJson=JSONObject.parseObject(res);
	 @SuppressWarnings("unchecked")
	 Map<String, String>
	 resMap=JSONObject.toJavaObject(JSONObject.parseObject(respJson.get("alipay_trade_cancel_response").toString()),
	 Map.class);
	 logger.info("respMap:"+resMap);
	 String rtnCode=resMap.get("code").toString();
	 String rtnMsg=resMap.get("msg").toString();
	 }
	
	 /**
	 * 网联-支付宝 关闭
	 * 搁置 暂时停用
	 * @param orderInfo
	 * @return
	 * @throws BusinessException
	 */
	 public void cupsAliClosed(String orderid) throws BusinessException {
	 String reqUrl=aliUrl;
	 Map<String, Object> orderInfo=new HashMap<String, Object>();
	 orderInfo.put("method", "alipay.trade.close");
	 /*
	  * TO-DO 
	  * 测试写死 “2088921593485132W03”
	  * 生产若启用  本值为bank_merregister表内channel_id
	  */
	 orderInfo.put("channel_id", "2088921593485132W03");
	 orderInfo.put("mer_orderid",orderid);
	
	 Map<String, String> req=cupsService.getPackMessage(orderInfo);
	 String res=null;
	 try {
	 logger.info("[网联-支付宝]关闭请求信息----->"+req);
	 res =HttpConnectService.postForm(req, reqUrl);
	 logger.info("[网联-支付宝]关闭响应信息----->"+res);
	 } catch (Exception e) {
	 logger.error("[网联-支付宝]关闭请求异常{}，订单号{}", e,orderInfo.get("mer_orderid"));
	 throw new BusinessException(1002, "获取信息网络错误");
	 }
	 JSONObject jsonRes =new JSONObject();
	 JSONObject respJson=JSONObject.parseObject(res);
	 @SuppressWarnings("unchecked")
	 Map<String, String>
	 resMap=JSONObject.toJavaObject(JSONObject.parseObject(respJson.get("alipay_trade_close_response").toString()),
	 Map.class);
	 logger.info("respMap:"+resMap);
	 String rtnCode=resMap.get("code").toString();
	 String rtnMsg=resMap.get("msg").toString();
	 }



	

 
}
