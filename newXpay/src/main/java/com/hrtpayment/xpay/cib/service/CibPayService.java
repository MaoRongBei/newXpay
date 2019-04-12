package com.hrtpayment.xpay.cib.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.cib.bean.CibMsg;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.WxpayService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.common.service.impl.NettyClientService;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.CommonUtils;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

/**
 * 兴业银行——威富通
 * @author Administrator
 */
@Service
public class CibPayService implements WxpayService {
	private Logger logger = LogManager.getLogger("CibPayService");
	
	
	@Value("${cib.wx.appid}")
	private String appid;
	@Value("${cib.wx.secret}")
	private String secret;
	
	@Value("${cib.payhost}")
	private String payHost;
	@Value("${cib.notifyurl}")
	private String notifyUrl;
	
	
	//下载对账文件参数
	@Value("${cib.dzurl}")
	private String dzUrl ;
	@Value("${cib.mchId}")
	private String dzMchId;
	@Value("${cib.key}")
	private	String unnoKey ;
	@Value("${cib.dzpath}")
	private	String dzPath ;
	
	@Autowired
	NettyClientService client;
	@Autowired
	NotifyService notify;
	@Autowired
	MerchantService merService;
	@Autowired
	JdbcDao dao;
	
	
	/**
	 * 扫码支付下单,返回微信支付二维码地址
	 * @param unno
	 * @param mid
	 * @param amount
	 * @param merchantCode
	 * @param subject
	 * @param fiid
	 * @param orderid
	 * @return 二维码地址,weixin://xxx
	 * @throws BusinessException
	 */
	public String getcibPayUrl(String unno,String mid,BigDecimal amount,String merchantCode,String subject,
			int fiid,String orderid,String tid,String key,String payway,String hybRate,String hybType) throws BusinessException{
		List<Map<String, Object>> list = dao.queryForList("select mer_orderid from pg_wechat_txn t where t.mer_orderid=?",
				orderid);
		if (list.size() > 0) {
			throw new BusinessException(8000, "订单号已存在！");
		}
		
		CibMsg cibMsg = new CibMsg();
		String tranType="";
		if("WXZF".equals(payway)){
			cibMsg.setService("pay.weixin.native");
			tranType="1";
		}else if("ZFBZF".equals(payway)){
			cibMsg.setService("pay.alipay.native");
			tranType="2";
		}else if("QQZF".equals(payway)){
			cibMsg.setService("pay.tenpay.native");
			tranType="4";
		}else if("JDZF".equals(payway)){
			cibMsg.setService("pay.jdpay.native");
			tranType="6";
		}else{
			throw new BusinessException(9001, "指定通道未开通");
		}
		String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ "status, cdate, lmdate,unno,mer_tid,trantype,bankmid,hybtype,hybrate)"
				+ " values((S_PG_Wechat_Txn.nextval),?,'0',?,?,?,?,'0',sysdate,sysdate,?,?,?,?,?,?)";
		dao.update(insertSql, fiid, orderid, subject, amount, mid, unno, tid,tranType, merchantCode,hybType,hybRate);
		
		BigDecimal amount1 = amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN);
		
		cibMsg.setMch_id(merchantCode);
		cibMsg.setTotal_fee(String.valueOf(amount1.longValue()));
		cibMsg.setNonce_str(CommonUtils.getRandomHexStr(32));
		cibMsg.setMch_create_ip("127.0.0.1");
		cibMsg.setBody(subject);
		cibMsg.setNotify_url(notifyUrl);
		cibMsg.setOut_trade_no(orderid);
		/**
		 * 新增二维码生效时间 和 失效时间
		 */
		long startTime=System.currentTimeMillis();
		long endTime=System.currentTimeMillis()+30*60*1000;
		SimpleDateFormat sd=new SimpleDateFormat("yyyyMMddHHmmss");
		cibMsg.setTime_start(sd.format(new Date(startTime)));
		cibMsg.setTime_expire(sd.format(new Date(endTime)));
		cibMsg.setSign(cibMsg.calSign(key));
		String sendXml=cibMsg.toXmlString();
		logger.info("发送兴业银行的消息:" + sendXml);
		try {
			String res=client.sendFormData(payHost, sendXml);
			logger.info("接受到兴业银行的消息:" + res);
			CibMsg resp =CibMsg.parseXmlFromStr(res);
			logger.info(resp.getSign());
			logger.info(resp.calSign(key));
			if("0".equals(resp.getStatus()) && "0".equals(resp.getResult_code())){
				if (resp.getCode_url()!=null) {
					
					String sqlUpdate = "update pg_wechat_txn set qrcode=?,respcode=?,respmsg=?,"
							+ "trantype=?,lmdate=sysdate where mer_orderid=?";
					dao.update(sqlUpdate, resp.getCode_url(), resp.getResult_code(),"", tranType, orderid);
				}
				return resp.getCode_url();
			}else{
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(8000, "交易失败");
			}
			
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("发送兴业银行订单失败：", e);
			throw new BusinessException(8000, "交易失败");
		}
	}
	
	
	
	
	/**
	 * 扫码支付下单,返回二维码地址 传统POS专用
	 * @param unno
	 * @param mid
	 * @param amount
	 * @param merchantCode
	 * @param subject
	 * @param fiid
	 * @param orderid
	 * @return 二维码地址
	 * @throws BusinessException
	 */
	public String getcibPosPayUrl(BigDecimal amount,String merchantCode,String subject,
			int fiid,String orderid,String key,String payway) throws BusinessException{
		
		BigDecimal amount1 = amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN);
		CibMsg cibMsg = new CibMsg();
		String tranType="";
		if("WXZF".equals(payway)){
			cibMsg.setService("pay.wxpay.native");
			tranType="1";
		}else if("ZFBZF".equals(payway)){
			cibMsg.setService("pay.alipay.native");
			tranType="2";
		}else{
			throw new BusinessException(9001, "指定通道未开通");
		}
		//银行编号
//		cibMsg.setSign_agentno(agentNo);
		cibMsg.setMch_id(merchantCode);
		cibMsg.setTotal_fee(amount1.longValue()+"");
		cibMsg.setNonce_str(CommonUtils.getRandomHexStr(32));
		cibMsg.setMch_create_ip("127.0.0.1");
		cibMsg.setBody(subject);
		cibMsg.setNotify_url(notifyUrl);
		cibMsg.setOut_trade_no(orderid);
		/**
		 * 新增二维码生效时间 和 失效时间
		 */
		long startTime=System.currentTimeMillis();
		long endTime=System.currentTimeMillis()+30*60*1000;
		SimpleDateFormat sd=new SimpleDateFormat("yyyyMMddHHmmss");
		cibMsg.setTime_start(sd.format(new Date(startTime)));
		cibMsg.setTime_expire(sd.format(new Date(endTime)));
		
		
		cibMsg.setSign(cibMsg.calSign(key));
		String sendXml=cibMsg.toXmlString();
		logger.info("发送兴业银行的消息:" + sendXml);
		try {
			String res=client.sendFormData(payHost, sendXml);
			logger.info("接受到兴业银行的消息:" + res);
			CibMsg resp =CibMsg.parseXmlFromStr(res);
			logger.info(resp.getSign());
			logger.info(resp.calSign(key));
			if("0".equals(resp.getStatus()) && "0".equals(resp.getResult_code())){
				if (resp.getCode_url()!=null){
						String updateSql="update pg_wechat_txn set fiid=?,tranType=?,bankmid=?,bk_orderid=?,"
								+ " lmdate=sysdate,qrcode=?,status='0' where status='A' and mer_orderid=? ";
					int count =dao.update(updateSql,fiid,tranType,merchantCode,"",resp.getCode_url(),orderid);
					if (count > 0) {
						return resp.getCode_url();
					}
					throw new BusinessException(8000, "订单已失效,请重新下单!");
				}
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(8000, "交易失败");
			}else{
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(8000, "交易失败");
			}
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("发送兴业银行订单失败：", e);
			throw new BusinessException(8000, "交易失败");
		}
	}
	
	/**
	 * 兴业银行条形码支付
	 * @author xuxiaoxiao
	 * @param unno
	 * @param mid
	 * @param fiid
	 * @param payway
	 * @param orderid
	 * @param merchantCode
	 * @param scene
	 * @param authCode
	 * @param totalAmount
	 * @param subject
	 * @param tid
	 * @param string 
	 * @return
	 * @throws BusinessException
	 */
	
	public String barcodePay(HrtPayXmlBean bean,String unno,String mid,int fiid,String payway,String orderid,String merchantCode,String scene,
			String authCode,BigDecimal totalAmount,String subject,String tid,String key,String tranType) throws BusinessException {
		List<Map<String,Object>> list=dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?", orderid);
		if(list.size()>0){
			throw new BusinessException(8000, "订单号已存在！");
		}
		
		String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ "status, cdate, lmdate,unno,mer_tid,bankmid,tranType) values("
				+ "(S_PG_Wechat_Txn.nextval),?,'0',?,?,?,?,'0',sysdate,sysdate,?,?,?,?)";
		dao.update(insertSql,fiid,orderid,subject,totalAmount,mid,unno,tid,merchantCode,tranType);
		
		int amount2=totalAmount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue();

		CibMsg cibMsg = new CibMsg();
//		cibMsg.setSign_agentno(agentNo);
		cibMsg.setService("unified.trade.micropay");
		cibMsg.setMch_id(merchantCode);
		cibMsg.setTotal_fee(String.valueOf(amount2));
		cibMsg.setNonce_str(CommonUtils.getRandomHexStr(32));
		cibMsg.setBody(subject);
		cibMsg.setOut_trade_no(orderid);
		cibMsg.setAuth_code(authCode);
		cibMsg.setMch_create_ip("127.0.0.1");
		cibMsg.setSign(cibMsg.calSign(key));
		String sendXml = cibMsg.toXmlString();
		logger.info("发送兴业银行的消息:" + sendXml);
		try {
			String res=client.sendFormData(payHost, sendXml);
			logger.info("接受到兴业银行的消息:" + res);
			CibMsg resp =CibMsg.parseXmlFromStr(res);
			/**
			 * 判断bankType类型 
			 * 包含DEBIT  paytype=1   借记卡
			 * 包含CREDIT  paytype=2  贷记卡
			 * 其它不为空的情况  paytype=3  
			 */
			String bankType=resp.getBank_type();
			String paytype="";
			if (null!=bankType&&!"".equals(bankType)) {
				if (bankType.contains("DEBIT")) {
					paytype = "1";
				} else if (bankType.contains("CREDIT")) {
					paytype = "2";
				} else {
					paytype = "3";
				}
			}
			if("0".equals(resp.getStatus()) && "0".equals(resp.getResult_code()) && "0".equals(resp.getPay_result())){
				String pay_info=resp.getPay_info();
				String transaction_id=resp.getTransaction_id();
				String time_end=resp.getTime_end();
				String updateSql="update pg_wechat_txn t set t.status=?,t.bk_orderid=?,time_end=?,"
						+ " t.lmdate=sysdate,t.respcode=?,t.respmsg=? ,trantype=?,paytype=?,bank_Type=?"
						+ ",userid=? where t.mer_orderid=? ";
				dao.update(updateSql,"1",transaction_id,time_end,"0000","success",tranType,paytype
						,bankType,resp.getBuyer_user_id(),orderid);
				if (bean!=null) {
					bean.setRtnCode(resp.getStatus());
					bean.setRtnMsg("");
					bean.setBankType(paytype);
					bean.setUserId(resp.getBuyer_user_id());
				}
				return "S";
			}else if("0".equals(resp.getStatus()) && "1".equals(resp.getResult_code())){
				String errCode=resp.getErr_code();
				String errMsg=resp.getErr_msg();
				if("Y".equals(resp.getNeed_query()) || null==resp.getNeed_query()
						|| "".equals(resp.getNeed_query())){
					String updateSql="update pg_wechat_txn t set t.status=?,"
							+ " t.lmdate=sysdate,t.respcode=?,t.respmsg=? where t.mer_orderid=? ";
					dao.update(updateSql,"0","USERPAYING",errMsg,orderid);
					if (bean!=null) {
						bean.setRtnCode("USERPAYING");
						bean.setRtnMsg(errMsg);
						bean.setBankType("");
					}
					return "R";
				}else{
					if (bean!=null) {
						bean.setRtnCode(errCode);
						bean.setRtnMsg(errMsg);
						bean.setBankType("");
					}
					RedisUtil.addFailCountByRedis(1);
					throw new BusinessException(8000, "交易失败");
				}
			}else{
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(8000, "交易失败");
			}
			
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("发送兴业银行订单失败：", e);
			throw new BusinessException(8000, "交易失败");
		}
	}

	public String callback(CibMsg bean) {
		
		String key="";
		try {
			key = selectKey(bean.getMch_id());
		} catch (BusinessException e) {
			logger.info("兴业异步返回未找到商户号秘钥:{}", bean.getMch_id());
			return "fail";
		}
		
		String orderid = bean.getOut_trade_no();
		double  txnamt = Double.parseDouble(bean.getTotal_fee())/100;
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn where mer_orderid=? and txnamt=?", orderid,txnamt);
		if (list.size() < 1) {
			logger.info("兴业异步返回成功订单未找到:{}", orderid);
			return "fail";
		}
		logger.info(bean.getSign());
		logger.info(bean.calSign(key));
		Map<String, Object> map = list.get(0);
		BigDecimal pwid = (BigDecimal) map.get("PWID");
		String dbstatus = (String) map.get("STATUS");
		if ("1".equals(dbstatus)) {
			logger.info("订单{}状态为成功,忽略重复异步通知", orderid);
			return "success";
		}
		/**
		 * TO-DO
		 * 判断bankType类型 
		 * 包含DEBIT  paytype=1   借记卡
		 * 包含CREDIT  paytype=2  贷记卡
		 * 其它不为空的情况  paytype=3  
		 */
		String bankType=bean.getBank_type();
		String payType="";
		if(null!=bankType&&!"".equals(bankType)){
			if (bankType.contains("DEBIT")) {
				payType="1";
			}else if (bankType.contains("CREDIT")) {
				payType="2";
			}else{
				payType="3";
			}
		}

		if ("0".equals(bean.getStatus()) && "0".equals(bean.getResult_code())) {
			if ("0".equals(bean.getPay_result())) {
				int n = dao.update(
						"update pg_wechat_txn set status='1',respcode='000000',respmsg='success',lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate),"
						+ " bk_orderid=?,BANKMID=?,BANK_TYPE=?,payType=?,TXNLEVEL='1',userid=? where status<>'1'  and pwid=? ",
						bean.getTime_end(),bean.getTransaction_id(), bean.getMch_id(),
						bean.getBank_type(),payType,bean.getBuyer_user_id(), pwid);
				if (n ==1) {
					map.put("TXNLEVEL", BigDecimal.ONE);
					map.put("RTNCODE", "000000");
					map.put("RTNMSG", "success");
					map.put("BANKTYPE", payType);
					map.put("USERID", bean.getBuyer_user_id());
					notify.sendNotify(map);
					logger.info("[兴业银行]异步通知    订单{}更新订单状态为成功", orderid);
				} else if (n ==0) {
					logger.info("[兴业银行]异步通知   订单{}原状态为成功，不进行更新 ", orderid);
				} else {
					logger.info("[兴业银行]异步通知   订单{}状态更新异常",orderid);
				}
			}
			return "success" ;
		}
		RedisUtil.addFailCountByRedis(1);
		return "fail";
	}
	
	
	/**
	 * 查询订单
	 * @param orderid
	 * @return
	 */
	
	public String queryOrder(HrtPayXmlBean bean,Map<String, Object> map) {
		String  orderid=String.valueOf(map.get("MER_ORDERID"));
		/**
		 * 根据商户号自动切换加密key
		 */
		String key="";
		try {
			key = selectKey(String.valueOf(map.get("BANKMID")));
		} catch (BusinessException e1) {
			logger.info("兴业银行查询订单-子商户不存在:{}", map.get("BANKMID"));
		}
		CibMsg msg = new CibMsg();
		msg.setOut_trade_no(orderid);
		msg.setMch_create_ip("10.51.64.73");
		msg.setService("unified.trade.query");
		msg.setMch_id(String.valueOf(map.get("BANKMID")));
		msg.setNonce_str(CommonUtils.getRandomHexStr(32));
		String sign = msg.calSign(key);
		msg.setSign(sign);
		String xml = msg.toXmlString();
		logger.info("兴业银行查询订单:{}", xml);

		try {
			String response = client.sendFormData(payHost, xml);
			logger.info("[兴业银行]查询返回日志{}", response);
			CibMsg resp = CibMsg.parseXmlFromStr(response);
			if (!"0".equals(resp.getStatus())) {
				return resp.getMessage();
			}
			if (!"0".equals(resp.getResult_code())) {
				return resp.getErr_code() + resp.getErr_msg();
			}
			String tradeState = resp.getTrade_state();

			String txnStatus = "0";
			if ("SUCCESS".equals(tradeState) || "REFUND".equals(tradeState)){
				txnStatus = "1";
			} else if ("NOTPAY".equals(tradeState) || "NOPAY".equals(tradeState)) {
				txnStatus = "8";
			} else if ("CLOSED".equals(tradeState)) {
				txnStatus = "5";
			} else if ("REVOKED".equals(tradeState)) {
				txnStatus = "7";
			} else if ("PAYERROR".equals(tradeState)) {
				txnStatus = "6";
			}
			
			String bankType= resp.getBank_type();
			/**
			 * TO-DO
			 * 判断bankType类型 
			 * 包含DEBIT  paytype=1   借记卡
			 * 包含CREDIT  paytype=2  贷记卡
			 * 其它不为空的情况  paytype=3  
			 */
			String payType="";
			if (!"".equals(bankType)&&null!=bankType) {
				if (bankType.contains("DEBIT")) {
					payType="1";
				}else if(bankType.contains("CREDIT")){
					payType="2";
				}else {
					payType="3";
				}
			}
			
			if (!"0".equals(txnStatus)) {
				String sql = "update pg_wechat_txn set status=?,respcode=?,respmsg=?,lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate),"
						+ " bk_orderid=?,bank_type=?,payType=?,userid=? where status<>'1' and  pwid=?";
				int n = dao.update(sql, txnStatus,tradeState,resp.getTrade_state(),resp.getTime_end(),
						resp.getTransaction_id(),resp.getBank_type(),payType,resp.getBuyer_user_id(),map.get("PWID"));
				logger.info("更新待查询交易状态为:{},数据库更新结果:{}",txnStatus,n);
				/**
				 * 查询订单成功之后，重新推送。
				 */
				if("1".equals(txnStatus) && "SUCCESS".equals(tradeState)&&n==1){
					BigDecimal txnLevel = map.get("TXNLEVEL") == null || "".equals(map.get("TXNLEVEL"))
							? BigDecimal.ONE : new BigDecimal(String.valueOf(map.get("TXNLEVEL")));
//					notify.sendQueryNotify(orderid);
					map.put("TXNLEVEL",txnLevel);
					map.put("RTNCODE", tradeState);
					map.put("RTNMSG", "支付成功");
					map.put("BANKTYPE", payType);
					map.put("USERID", resp.getBuyer_user_id());
					if(bean!=null){
						bean.setUserId(resp.getBuyer_user_id());	
					}
					notify.sendNotify(map);
				}
			} else {
				if (bean!=null) {
					bean.setRtnCode("0000");
					bean.setRtnMsg("等待支付");
					bean.setBankType("");
				}
				logger.info("交易状态为等待支付:{}",txnStatus);
			}
			return tradeState;
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			if (bean!=null) {
				bean.setRtnCode("8000");
				bean.setRtnMsg("请求错误");
				bean.setBankType("");
			}
			logger.error("兴业银行初始化请求出错{}", e.getMessage());
			throw new HrtBusinessException(8000);
		}
	}
	
	/**
	 * 根据商户号自动切换加密key
	 */
	public String selectKey(String merchantCode)throws BusinessException {
		logger.info("银行子商户号为:" + merchantCode);
		String querySql="select MINFO2 from bank_merregister t where t.merchantcode=? and t.fiid=34 ";
		List<Map<String, Object>> list =dao.queryForList(querySql, merchantCode);
		if(list.size()<1){
			throw new BusinessException(9006,"未找到与商户对应的秘钥！");
		}else{
			return String.valueOf(list.get(0).get("MINFO2"));
		}
	}
	

	public String refund(String orderid, BigDecimal amount, Map<String, Object> oriMap) throws BusinessException {
		Map<String,String> req = new HashMap<String, String>();
		req.put("service", "unified.trade.refund");
		req.put("mch_id", String.valueOf(oriMap.get("BANKMID")));
		req.put("out_trade_no", String.valueOf(oriMap.get("MER_ORDERID")));
		req.put("out_refund_no", orderid);
		req.put("total_fee", ((BigDecimal)oriMap.get("TXNAMT")).multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()+"");
		req.put("refund_fee", amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()+"");
		req.put("op_user_id", String.valueOf(oriMap.get("BANKMID")));
		req.put("nonce_str", CommonUtils.getRandomHexStr(32));
		logger.info("兴业银行退款请求报文:------------------>"+req);
		Map<String, String> resp = request(req);
		logger.info("兴业银行退款响应报文:------------------>"+resp);
		
		String status = "2";
		String errorCode = null;
		String errorCodeDes = null;
		String refundId = null;
		String resStatus="R";
		JSONObject jsonRes = new JSONObject();
		if ("0".equals(resp.get("result_code"))) {
			refundId = resp.get("refund_id");
			resStatus="R";
			jsonRes.put("errcode", resStatus);
			jsonRes.put("rtmsg",  "退款成功-待查询");
		} else {
			status = "6";
			errorCode = resp.get("err_code");
			errorCodeDes = resp.get("err_code_des");
			resStatus="E";
			jsonRes.put("errcode",resStatus);
			jsonRes.put("rtmsg", errorCode);
		}
		dao.update("update pg_wechat_txn t set t.status=?,lmdate=sysdate,respcode=?,respmsg=?,bk_orderid=? where mer_orderid=? and txntype='1'", 
				status,errorCode,errorCodeDes,refundId,orderid);
	
		return jsonRes.toJSONString();
	}

	public String refundQuery(Map<String, Object> refundMap) throws BusinessException {
		String orderid = (String) refundMap.get("mer_orderid");
		Map<String,String> req = new HashMap<String,String>();
		req.put("service", "unified.trade.refundquery");
		req.put("mch_id", String.valueOf(refundMap.get("BANKMID")));
		req.put("out_refund_no", orderid);
		req.put("nonce_str", CommonUtils.getRandomHexStr(32));
		
		logger.info("兴业退款报文：{}",req);
		Map<String, String> resp = request(req);
		logger.info("兴业退款返回报文：{}",resp);
		if (!"0".equals(resp.get("status"))) {
			return (String)resp.get("message");
		}

		if (!"0".equals(resp.get("result_code"))) {
			return "退款失败，原因码："+ resp.get("err_code");
		}
		int rfCnt = Integer.valueOf(resp.get("refund_count"));
		if (rfCnt<1) {
			return "微信返回退款条数为0";
		}
		for (int i = 0; i < rfCnt; i++) {
			String outRefundNo = resp.get("out_refund_no_"+i);
			String refundId = resp.get("refund_id_"+i);
			String refundFee = resp.get("refund_fee_"+i);
			String refundStatus = resp.get("refund_status_"+i);
			if (outRefundNo.equals(orderid)) {
				if ("SUCCESS".equals(refundStatus)) {
					if (new BigDecimal(refundFee).divide(BigDecimal.TEN).divide(BigDecimal.TEN)
							.compareTo((BigDecimal) refundMap.get("TXNAMT")) !=0) {
						return "数据库退款金额与返回金额不一致,"+(BigDecimal) refundMap.get("TXNAMT")+" "+refundFee;
					}
					dao.update("update pg_wechat_txn t set t.status='1',lmdate=sysdate,bk_orderid=? where status<>'1' and  mer_orderid=? and txntype='1'", 
							refundId,orderid);
				} else if ("FAIL".equals(refundStatus)) {
					dao.update("update pg_wechat_txn t set t.status='6',lmdate=sysdate,bk_orderid=? where status<>'1' and  mer_orderid=? and txntype='1'", 
							refundId,orderid);
				}
				return refundStatus;
			}
		}
		return "未找到订单号对应退款记录";
	}
	
	private Map<String,String> request(Map<String,String> req){
		
		String key="";
		try {
			key = selectKey(req.get("mch_id"));
		} catch (BusinessException e1) {
			logger.error("兴业退款查询 子商户秘钥不存在",e1);
		}
		
		req.put("sign", SimpleXmlUtil.getMd5Sign(req, key));
		String xml = SimpleXmlUtil.map2xml(req);
		String res = null;
		try {
			res = client.sendFormData(payHost, xml);
		} catch (SSLException | InterruptedException | URISyntaxException e) {
			logger.error("访问兴业出错",e);
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(1002);
		}
		
		Map<String, String> resp = null;
		try{
			resp = SimpleXmlUtil.xml2map(res);
		} catch(RuntimeException e) {
			logger.info("兴业返回:{}",res);
			logger.error("解析兴业返回报文出错",e);
		}
		
		String sign = SimpleXmlUtil.getMd5Sign(resp, key);
		if (!sign.equals(resp.get("sign"))) {
			RedisUtil.addFailCountByRedis(1);
			logger.info("兴业验签失败:\n报文:{}\nsign:{}\n本地sign:{}",res,resp.get("sign"),sign);
			throw new HrtBusinessException(2001);
		}
		return resp;
	}

	@Override
	public String getWxpayAppid() {
		return appid;
	}

	/**
	 * 调起公众号支付
	 */
	@Override
	public String getWxpayPayInfo(String orderid, String openid,String isCredit) {
		try {
			CibMsg resp = getcibResp(orderid, openid);
			if (resp.getPay_info() != null) {
				return resp.getPay_info();
			} else {
				RedisUtil.addFailCountByRedis(1);
				logger.info("兴业银行获取支付参数失败:{}", resp.getErr_msg());
				throw new BusinessException(8000, "交易失败");
			}
		} catch (BusinessException e) {
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(e.getCode(), e.getMessage());
		}

	}
	
	
	private CibMsg getcibResp(String orderid, String openid) throws BusinessException {
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn where mer_orderid=?", orderid);
		if (list.size() < 1) throw new BusinessException(9008, "订单不存在");
		if (list.size() > 1) throw new BusinessException(9007, "订单号重复");
		Map<String, Object> map = list.get(0);
		/**
		 * 根据商户号自动切换加密key
		 */
		String key = selectKey(String.valueOf(map.get("BANKMID")));
		String dbstatus = (String) map.get("STATUS");
		if (!"A".equals(dbstatus)) {
			throw new BusinessException(8000, "订单已失效");
		}
		String subject = (String) map.get("DETAIL");
		BigDecimal amount = (BigDecimal) map.get("TXNAMT");
		CibMsg msg = new CibMsg();
		msg.setService("pay.weixin.jspay");
		msg.setMch_id(String.valueOf(map.get("BANKMID")));
		msg.setIs_raw("1");
		msg.setOut_trade_no(orderid);
		msg.setBody(subject);
		
		msg.setSub_openid(openid);
		msg.setSub_appid(getWxAppid(String.valueOf(map.get("BANKMID"))));
		
		msg.setTotal_fee(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue() + "");
		msg.setMch_create_ip("127.0.0.1");
		msg.setNotify_url(notifyUrl);
		msg.setNonce_str(CommonUtils.getRandomHexStr(32));
		/**
		 * 新增二维码生效时间 和 失效时间
		 */
		long startTime=System.currentTimeMillis();
		long endTime=System.currentTimeMillis()+30*60*1000;
		SimpleDateFormat sd=new SimpleDateFormat("yyyyMMddHHmmss");
		msg.setTime_start(sd.format(new Date(startTime)));
		msg.setTime_expire(sd.format(new Date(endTime)));
		
		String sign = msg.calSign(key);
		msg.setSign(sign);
		String xml = msg.toXmlString();
		logger.info("兴业银行公众号下单:{}", xml);

		try {
			String response = client.sendFormData(payHost, xml);
			logger.info("兴业银行公众号下单返回:{}", response);
			CibMsg resp = CibMsg.parseXmlFromStr(response);
			return resp;
		} catch (Exception e) {
			logger.error("兴业银行初始化请求出错{}", e.getMessage());
			throw new BusinessException(8000, "交易失败");
		}
	}

	@Override
	public String getWxpaySecret() {
		return secret;
	}

	@Override
	public int getWxpayFiid() {
		return 34;
	}

	
	public void insertPubaccOrder(String unno, String mid, String orderid, String subject, BigDecimal amount, int fiid,
			String bankMid) throws BusinessException {
		if (subject == null || "".equals(subject)) {
			subject = merService.queryMerName(mid);
		}
		try {
			List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?", orderid);
			if (list.size()>0) {
				throw new BusinessException(8000, "订单号重复");
			}
			String sql = "insert into pg_wechat_txn (pwid,fiid, txntype,cdate,status,"
					+ "mer_orderid, detail, txnamt, mer_id,unno,bankmid,trantype) values"
					+ "(S_PG_Wechat_Txn.nextval,?,'0',sysdate,'A',?,?,?,?,?,?,'1')";
			dao.update(sql, fiid, orderid, subject, amount, mid, unno,bankMid);
		} catch (Exception e) {
			logger.error("插入公众号支付订单失败", e);
			throw new BusinessException(8000, "交易失败");
		}
	}

	/**
	 * 兴业银行对账文件下载
	 * 20170526
	 * @param settleDate
	 * @return
	 * @throws BusinessException
	 */
	public String downloadDzFile(String settleDate) throws BusinessException {
		if (!settleDate.matches("\\d{8}")) {
			throw new BusinessException(9000, "对账日期格式错误");
		}

		// param
		CibMsg dzMsg = new CibMsg();
		dzMsg.setService("pay.bill.agent");
		dzMsg.setCharset("UTF-8");
		dzMsg.setBill_date(settleDate);
		dzMsg.setBill_type("ALL");
		dzMsg.setSign_type("MD5");
		dzMsg.setMch_id(dzMchId);
		dzMsg.setNonce_str(CommonUtils.getRandomHexStr(32));
		dzMsg.setSign(dzMsg.calSign(unnoKey));

		String sendXml = dzMsg.toXmlString();
		logger.info("发送兴业银行的消息:" + sendXml);

		String content = "";
		try {
			content = client.sendFormData(dzUrl, sendXml);
			if (content.length() < 200 && content.indexOf("message") > -1 && content.indexOf("status") > -1) {
				// 判断返回失败,如果正产返回数据量很大，判断长度节省index时间{"message":"Signature error","status":400}
				logger.error("收到兴业银行对账出错！：返回内容{}", content);
				throw new BusinessException(1002, content);
			}
		} catch (SSLException | InterruptedException | URISyntaxException e) {
			logger.error("发送兴业银行对账请求失败：", e);
			throw new BusinessException(1002, "下载对账文件失败");
		}
		if (StringUtils.isNotEmpty(content)) {
			try {
				// 验证路径是否存在，创建文件夹
				String path = dzPath;
				if (!path.endsWith(File.separator))
					path += File.separator;
				String realPath = path + "cib_"+settleDate + ".txt";
				File dir = new File(path);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				FileWriter fw = new FileWriter(new File(realPath), false);
				fw.write(content);
				fw.close();
				logger.info("写入兴业对账文件成功，路径为：{}", realPath);
			} catch (IOException e) {
				logger.error("写入对账文件出错:", e);
				return "写入对账文件出错";
			}
		}
		return "写入对账文件成功";
	}
	
	
	
	public String getWxAppid(String merchantCode){
		String sql="select APPID from bank_merregister t where t.merchantcode=? and fiid=?";
		List<Map<String, Object>> list = dao.queryForList(sql, merchantCode, getWxpayFiid());
		if(list.size()>0){
			String newAppid=String.valueOf(list.get(0).get("APPID"));
			if(newAppid!=null && !"".equals(newAppid) && !"null".equals(newAppid)){
				return newAppid;
			}else{
				return getWxpayAppid();
			}
		}else{
			return getWxpayAppid();
		}
	}
	
		
}
