package com.hrtpayment.xpay.cups.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.cups.sdk.AcpService;
import com.hrtpayment.xpay.cups.sdk.CertUtil;
import com.hrtpayment.xpay.cups.sdk.DemoBase;
import com.hrtpayment.xpay.cups.sdk.SDKConfig;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;



@Service
public class CupsPayService {

	private final Logger logger = LogManager.getLogger();
	
	@Autowired
	JdbcDao dao;
	@Autowired
	NotifyService notify;
	
	@Value("${cups.notify_url}")
	String notifyUrl;
	
	@Value("${cups.acqcode}")
	String acqCode;
//	@Value("${cups.c2b_backUrl}")
//	String c2bBackUrl;
	
	public static final String refundZsReqType="0150000903";
	
	public static final String refundBsReqType="0340000903";
	
	public static final String undoZsReqType="0170000903";
	public static final String undoBsReqType="0330000903";
	
	
	/**
	 * 银联二维码被扫模式
	 * @param bean
	 */
	public void cupsPay(HrtPayXmlBean bean) throws BusinessException{
		
		if (bean.getOrderid() == null || "".equals(bean.getOrderid())
				|| !bean.getOrderid().startsWith(bean.getUnno()) || bean.getOrderid().length()>32) {
			throw new HrtBusinessException(9005);
		}
		/**
		 * 1
		 * 判断订单号是否已存在
		 */
		List<Map<String, Object>> list = dao.queryForList("select mer_orderid from pg_wechat_txn where mer_orderid=?", bean.getOrderid());
		if (list.size()>0){
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("订单号重复");
			return ;
		}
	    /**
		 *  2018-09-06 新增限制
		 *  unno=886600 除外
		 *  
		 * 1.其它unno下 同一商户号日交易限笔≤10笔；
		 * 
		 */
		Integer orderCon=0;
		Map<String, Object> merchantMap;
		if (!"886600".equals(bean.getUnno())) {
			String queryOrderConSql=" select bankmid from pg_wechat_txn pw ,hrt_merchacc  hm,hrt_merchfinacc hmf "
					+ " where pw.lmdate between trunc(sysdate,'dd')and sysdate  and pw.mer_id=? "
					+ " and  pw.mer_id=hm.hrt_mid  and hm.maid=hmf.maid "
					+ " and hm.status='1'  and hmf.ifcard=1 "
					+ " and fiid =18 and pw.unno <>'886600' and respcode in ('00','04')    ";
			List<Map<String , Object>> orderConList= dao.queryForList(queryOrderConSql, bean.getMid());
	
			orderCon=orderConList.size();
			if (orderCon>=10) {
				throw new HrtBusinessException(8000,"当日交易笔数已达到上限!");
			}else  {
				/*
				 * 2. 同一个商户号一天24小时不跳号。
				 *  交易为失败交易的时候 且没有成功交易时会发生跳号
				 *  如果交易的bankmid 为空会选择新的银行商户号
				 */
			    try {
			    	if (orderCon!=0&&null!=orderConList.get(0).get("BANKMID")) {
			    		 bean.setBankMid(String.valueOf(orderConList.get(0).get("BANKMID")));
					}
					merchantMap=getMerchantCode2(bean,orderCon);
				} catch (Exception e) {
					logger.info("[银联二维码] {}:{}", bean.getMid(),e.getMessage());
					throw new  HrtBusinessException(9009);
				}
			} 
		}else{
		    try {
				merchantMap= getMerchantCode2(bean,orderCon);
			} catch (Exception e) {
				throw new  HrtBusinessException(9009);
			}
		}
		//插入订单号
		String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ "status, cdate, lmdate,qrcode,unno,mer_tid,bank_type,trantype) values"
				+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,'0',sysdate,sysdate,?,?,?,'BS','3')";
		dao.update(insertSql, 18, bean.getOrderid(), "", bean.getAmount(), bean.getMid(), bean.getQrcode(), bean.getUnno(),
				bean.getTid());
		
		/**
		 * 2
		 * 组装下单报文并发送
		 */
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");

		Map<String, String> reqMap = new HashMap<String, String>();
		reqMap.put("version", "1.0.0");
		reqMap.put("reqType", "0310000903");
		reqMap.put("acqCode", acqCode);
		String bankMid = String.valueOf(merchantMap.get("MERCHANTCODE"));
		reqMap.put("merId", bankMid);
		reqMap.put("merCatCode", String.valueOf(merchantMap.get("MERCATCODE")));
		reqMap.put("merName", String.valueOf(merchantMap.get("MERCHANTNAME")));
		reqMap.put("termId", "79052157");
		reqMap.put("qrNo", bean.getQrcode());
		reqMap.put("currencyCode", "156");
		BigDecimal bd2 = new BigDecimal(bean.getAmount());
		reqMap.put("txnAmt", String.valueOf(bd2.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));
		reqMap.put("orderNo", bean.getOrderid());
		String orderTime = sf.format(new Date());
		reqMap.put("orderTime", orderTime);
		reqMap.put("backUrl", notifyUrl);
		reqMap.put("areaInfo", "1561000");

		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		Map<String, String> respMap = sendMsg(reqMap, requestUrl);

		if (respMap != null) {
			/**
			 * 3 判断是否下单成功，成功则插入订单表
			 */
			String respCode = respMap.get("respCode");
			String respMsg = respMap.get("respMsg");
			if (respCode.equals("00")) {

				String updateSql = "update pg_wechat_txn set bankmid=?, respcode=?, respmsg=?,time_end=? where mer_orderid=? ";
				int count = dao.update(updateSql, bankMid, respCode, respMsg, orderTime, bean.getOrderid());
				logger.info("[银联二维码]-被扫交易订单号{}，下单更新记录{}", bean.getOrderid(), count);
				bean.setPaymode("3");
				bean.setStatus("R");
			} else {
				RedisUtil.addFailCountByRedis(1);
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc(respMsg);
			}
		} else {
			RedisUtil.addFailCountByRedis(1);
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("系统错误");
		}
	}
	
	/**
	 * 银联二维码被扫模式
	 * @param bean
	 */
	public void cupsPayNew(HrtPayXmlBean bean) throws BusinessException{
		
		if (bean.getOrderid() == null || "".equals(bean.getOrderid())
				|| !bean.getOrderid().startsWith(bean.getUnno()) || bean.getOrderid().length()>32) {
			throw new HrtBusinessException(9005);
		}
		/**
		 * 1
		 * 判断订单号是否已存在
		 */
		List<Map<String, Object>> list = dao.queryForList("select mer_orderid from pg_wechat_txn where mer_orderid=?", bean.getOrderid());
		if (list.size()>0){
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("订单号重复");
			return ;
		}
		
		//插入订单号
		String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ "status, cdate, lmdate,qrcode,unno,mer_tid,bank_type,trantype) values"
				+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,'0',sysdate,sysdate,?,?,?,'BS','10')";
		dao.update(insertSql, 18, bean.getOrderid(), "", bean.getAmount(), bean.getMid(), bean.getQrcode(), bean.getUnno(),
				bean.getTid());
		
		/**
		 * 2
		 * 组装下单报文并发送
		 */
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
		//此处调用非挂号模式，mcc名称则为实际轮询组名称
		Map<String, Object> merchantMap = getMerchantCodeForCups(new BigDecimal(bean.getAmount()),bean.getGroupName(),bean.getBankMid());

		Map<String, String> reqMap = new HashMap<String, String>();
		reqMap.put("version", "1.0.0");
		reqMap.put("reqType", "0310000903");
		reqMap.put("acqCode", acqCode);
		String bankMid = String.valueOf(merchantMap.get("MERCHANTCODE"));
		reqMap.put("merId", bankMid);
		reqMap.put("merCatCode", String.valueOf(merchantMap.get("MERCATCODE")));
		reqMap.put("merName", String.valueOf(merchantMap.get("MERCHANTNAME")));
		reqMap.put("termId", "79052157");
		reqMap.put("qrNo", bean.getQrcode());
		reqMap.put("currencyCode", "156");
		BigDecimal bd2 = new BigDecimal(bean.getAmount());
		reqMap.put("txnAmt", String.valueOf(bd2.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));
		reqMap.put("orderNo", bean.getOrderid());
		String orderTime = sf.format(new Date());
		reqMap.put("orderTime", orderTime);
		reqMap.put("backUrl", notifyUrl);
		reqMap.put("areaInfo", "1561000");
		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		Map<String, String> respMap = sendMsg(reqMap, requestUrl);
		logger.info("[银联绑卡]交易返回{}",respMap);
		if (respMap != null) {
			/**
			 * 3 判断是否下单成功，成功则插入订单表
			 */
			String respCode = respMap.get("respCode");
			String respMsg = respMap.get("respMsg");
			if (respCode.equals("00")) {
				String updateSql = "update pg_wechat_txn set bankmid=?, respcode=?, respmsg=?,time_end=? where mer_orderid=? ";
				int count = dao.update(updateSql, bankMid, respCode, respMsg, orderTime, bean.getOrderid());
				logger.info("[银联二维码]-被扫交易订单号{}，下单更新记录{}", bean.getOrderid(), count);
				bean.setStatus("R");
			} else {
				RedisUtil.addFailCountByRedis(1);
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc(respMsg);
			}
		} else {
			RedisUtil.addFailCountByRedis(1);
			bean.setStatus("R");
			bean.setErrcode("8000");
			bean.setErrdesc("系统错误");
		}
		bean.setBankMid(bankMid);
	}
	
	
	
	/**
	 * 银联二维码被扫模式
	 * @param bean
	 */
	public String cupsPayNewBy2(String orderid,String mid,String unno,String qrno,
			String amtount,String tid) throws BusinessException{
		
		if (orderid == null || "".equals(orderid) || orderid.length()>32) {
			throw new HrtBusinessException(9005);
		}
		/**
		 * 1
		 * 判断订单号是否已存在
		 */
		List<Map<String, Object>> list = dao.queryForList("select mer_orderid from pg_wechat_txn where mer_orderid=?", orderid);
		if (list.size()>0){
			throw new HrtBusinessException(9007);
		}
		
		//插入订单号
		String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,"
				+ "status, cdate, lmdate,qrcode,unno,mer_tid,bank_type,trantype,ISPOINT) values"
				+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,'0',sysdate,sysdate,?,?,?,'BS','8',1)";
		dao.update(insertSql, 18, orderid, "", amtount, mid, qrno, unno,tid);
		
		/**
		 * 2
		 * 组装下单报文并发送
		 */
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
		//此处调用非挂号模式，mcc名称则为实际轮询组名称
		Map<String, Object> merchantMap = getMerchantCodeForCups2(new BigDecimal(amtount),"");

		Map<String, String> reqMap = new HashMap<String, String>();
		reqMap.put("version", "1.0.0");
		reqMap.put("reqType", "0310000903");
		reqMap.put("acqCode", acqCode);
		String bankMid = String.valueOf(merchantMap.get("MERCHANTCODE"));
		reqMap.put("merId", bankMid);
		reqMap.put("merCatCode", String.valueOf(merchantMap.get("MERCATCODE")));
		reqMap.put("merName", String.valueOf(merchantMap.get("MERCHANTNAME")));
		reqMap.put("termId", "79052157");
		reqMap.put("qrNo", qrno);
		reqMap.put("currencyCode", "156");
		BigDecimal bd2 = new BigDecimal(amtount);
		reqMap.put("txnAmt", String.valueOf(bd2.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));
		reqMap.put("orderNo", orderid);
		String orderTime = sf.format(new Date());
		reqMap.put("orderTime", orderTime);
		reqMap.put("backUrl", notifyUrl);
		reqMap.put("areaInfo", "1561000");
		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		Map<String, String> respMap = sendMsg(reqMap, requestUrl);
		logger.info("[银联绑卡]交易返回{}",respMap);
		if (respMap != null) {
			/**
			 * 3 判断是否下单成功，成功则插入订单表
			 */
			String respCode = respMap.get("respCode");
			String respMsg = respMap.get("respMsg");
			if (respCode.equals("00")) {
				String updateSql = "update pg_wechat_txn set bankmid=?, respcode=?, respmsg=?,time_end=? where mer_orderid=? ";
				int count = dao.update(updateSql, bankMid, respCode, respMsg, orderTime, orderid);
				logger.info("[银联二维码]-被扫交易订单号{}，下单更新记录{}",orderid, count);
				return "R";
			} else {
				RedisUtil.addFailCountByRedis(1);
				logger.error("[银联二维码]-被扫交易失败{}",respMsg);
				throw new BusinessException(8000,respMsg );
			}
		} else {
			RedisUtil.addFailCountByRedis(1);
			return "R";
		}
	}

	/**
	 * 银联二维码主扫模式（获取二维码）
	 * @param bean
	 */
	public String getCupsPayQrCode(HrtPayXmlBean bean) throws HrtBusinessException {
		
		if (bean.getOrderid() == null || "".equals(bean.getOrderid())
				|| bean.getOrderid().length()>32) {
			throw new HrtBusinessException(9005);
		}
		if(!"110000".equals(bean.getUnno())){
			if (!bean.getOrderid().startsWith(bean.getUnno())) {
				throw new HrtBusinessException(9005);
			}
		}

		/**
		 * 1
		 * 判断订单号是否已存在
		 */
		List<Map<String, Object>> list = dao.queryForList("select mer_orderid from pg_wechat_txn where mer_orderid=?", bean.getOrderid());
		if (list.size()>0){
			throw new HrtBusinessException(8000,"订单号重复!");
		}
		/**
		 * 2018-09-06 新增限制
		 *  unno=886600 除外
		 *  
		 * 1.其它unno下 同一商户号日交易限笔≤10笔；
		 * 
		 */
		Integer orderCon=0;
		Map<String, Object> merchantMap;
		if (!"886600".equals(bean.getUnno())) {
			String queryOrderConSql=" select bankmid from pg_wechat_txn pw ,hrt_merchacc  hm,hrt_merchfinacc hmf "
					+ " where pw.lmdate between trunc(sysdate,'dd')and sysdate  and pw.mer_id=? "
					+ " and  pw.mer_id=hm.hrt_mid  and hm.maid=hmf.maid "
					+ " and hm.status='1'  and hmf.ifcard=1 "
					+ " and fiid =18 and pw.unno <>'886600' and respcode in ('00','04')    ";
			List<Map<String , Object>> orderConList= dao.queryForList(queryOrderConSql, bean.getMid());
	
			orderCon=orderConList.size();
			if (orderCon>=10) {
				throw new HrtBusinessException(8000,"当日交易笔数已达到上限!");
			}else  {
				/*
				 * 2. 同一个商户号一天24小时不跳号。
				 *  交易为失败交易的时候 且没有成功交易时会发生跳号
				 *  如果交易的bankmid 为空会选择新的银行商户号
				 */
			    try {
			    	if (orderCon!=0&&null!=orderConList.get(0).get("BANKMID")) {
			    		 bean.setBankMid(String.valueOf(orderConList.get(0).get("BANKMID")));
					}
					merchantMap=getMerchantCode2(bean,orderCon);
				} catch (Exception e) {
					throw new  HrtBusinessException(9009);
				}
			} 
		}else{
		    try {
				merchantMap= getMerchantCode2(bean,orderCon);
			} catch (Exception e) {
				throw new  HrtBusinessException(9009);
			}
		}
		
		/**
		 * 2
		 * 组装下单报文并发送
		 */
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
		
		 Map<String,String> reqMap= new HashMap<String,String>();
		 reqMap.put("version", "1.0.0");
//		 reqMap.put("certId", "");
		 reqMap.put("reqType", "0510000903");
		 reqMap.put("acqCode", acqCode);
		 reqMap.put("orderNo", bean.getOrderid());
		 String orderTime=sf.format(new Date());
		 reqMap.put("orderTime", orderTime);
		 reqMap.put("orderType", "10");
		//如果配置了敏感信息加密，必须加上这句
		 reqMap.put("encryptCertId", CertUtil.getEncryptCertId());
		 Map<String, String> payeeInfo = new HashMap<String, String>();
		 payeeInfo.put("name", String.valueOf(merchantMap.get("MERCHANTNAME")));
		 String bankMid=String.valueOf(merchantMap.get("MERCHANTCODE"));
		 payeeInfo.put("id", bankMid);
		 payeeInfo.put("merCatCode", String.valueOf(merchantMap.get("MERCATCODE")));
		 reqMap.put("payeeInfo", DemoBase.getPayeeInfoWithEncrpyt(payeeInfo,"UTF-8"));
		 reqMap.put("currencyCode", "156");
		 BigDecimal bd2 = new BigDecimal(bean.getAmount());
		 reqMap.put("txnAmt", String.valueOf(bd2.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));
		 reqMap.put("backUrl", notifyUrl);
		 reqMap.put("areaInfo", "1561000");
		 
		 String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		 Map<String,String> respMap=sendMsg(reqMap,requestUrl);
		 if(respMap!=null){
			String respCode=respMap.get("respCode");
			String respMsg=respMap.get("respMsg");
			String qrCode=respMap.get("qrCode");
			if(("00").equals(respCode)){
				
				/**
				 * 把订单插入数据库，付款状态为0，待收到异步通知后 状态更改为1
				 */
				String sql = "insert into pg_wechat_txn (pwid,fiid, txntype, "+
						"mer_orderid, detail, txnamt, mer_id, bankmid, respcode, respmsg, "
						+ "status, cdate, lmdate,qrcode,unno,mer_tid,time_end,bank_type,trantype) values"
						+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,?,'0',sysdate,sysdate,?,?,?,?,'ZS','3')";
				int count=dao.update(sql,18,bean.getOrderid(),"",bean.getAmount(),bean.getMid(),bankMid,
						respCode,respMsg,qrCode,bean.getUnno(),bean.getTid(),orderTime);
				return qrCode;
			}else{
				RedisUtil.addFailCountByRedis(1);
				throw new HrtBusinessException(9000,respMsg);
			}
		 }else{
			 logger.info("[银联二维码]-主扫模式{}，返回信息为空", bean.getOrderid());
			 throw new HrtBusinessException(9000,"网络通信失败");
		 }
	}
	
	/**
	 * 银联二维码被扫查询
	 * @param reqParam
	 * @return
	 */
	public String cupsPayQuery(String orderid) throws BusinessException{
		
		JSONObject json= new JSONObject();
		String querySql="select t.qrcode,t.time_end,t.status,t.accno,t.bk_orderid,t.ISSCODE, "
					+ " t.unno from pg_wechat_txn t where t.mer_orderid=?";
		List<Map<String, Object>> list=dao.queryForList(querySql, orderid);
		
		if (list.size()<1){
			throw new BusinessException(8000, "订单号不存在！");
		}
		String status = String.valueOf(list.get(0).get("STATUS"));
		if("1".equals(status)){
			json.put("errcode", "S");
			json.put("time_end", String.valueOf(list.get(0).get("TIME_END")));
			if("110000".equals(String.valueOf(list.get(0).get("UNNO")))){
				json.put("accNo", String.valueOf(list.get(0).get("ACCNO")));
				json.put("issCode", String.valueOf(list.get(0).get("ISSCODE")));
				json.put("voucherNum", String.valueOf(list.get(0).get("BK_ORDERID")));
				json.put("acqCode", "48640000");
				json.put("paymode", "3");
			}
			json.put("orderid", orderid);
			json.put("rtmsg", "成功!");
			return json.toJSONString();
		}
		 
		String qrNo = String.valueOf(list.get(0).get("QRCODE"));
		String orderTime =String.valueOf(list.get(0).get("TIME_END"));

		Map<String, String> contentData = new HashMap<String, String>();
		contentData.put("version", "1.0.0");
		contentData.put("reqType", "0350000903");
		contentData.put("acqCode", acqCode);  
		
		contentData.put("qrNo", qrNo);
		contentData.put("orderNo", orderid);
		contentData.put("orderTime", orderTime);
		contentData.put("areaInfo", "1561000");
		
		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		Map<String,String> respMap=sendMsg(contentData,requestUrl);
		if(respMap!=null){
			String respCode=respMap.get("respCode");
			String respMsg=respMap.get("respMsg");
			String origRespCode=respMap.get("origRespCode");
			String origRespMsg =respMap.get("origRespMsg");
			if("00".equals(respCode) && "00".equals(origRespCode)){
				String voucherNum=respMap.get("voucherNum");	
				String settleKey=respMap.get("settleKey");
				String payerInfo=respMap.get("payerInfo");
				Map<String,String> payerMap =decryptPayerInfo(payerInfo);
				String accNo="";
				String issCode="";
				String cardAttr="";
				if(payerMap!=null){
					accNo=payerMap.get("accNo");
					issCode=payerMap.get("issCode");
					cardAttr=payerMap.get("cardAttr").replace("0", "");
				}
				String updateSql="update pg_wechat_txn t set t.status='1',t.lmdate=sysdate,"
								+ " t.bk_orderid=?,t.settlekey=?,t.respcode=?,t.respmsg=?,"
								+ " t.accno=?,t.isscode=?,t.paytype=?"
								+ " where status<>'1' and t.time_end=? and t.mer_orderid=? ";
				int count=dao.update(updateSql, voucherNum,settleKey,origRespCode,origRespMsg,
						accNo,issCode,cardAttr,orderTime,orderid);
				json.put("errcode", "S");
				json.put("time_end", orderTime);
				json.put("orderid", orderid);
				if("110000".equals(String.valueOf(list.get(0).get("UNNO")))){
					json.put("accNo", accNo);
					json.put("issCode", issCode);
					json.put("voucherNum", voucherNum);
					json.put("acqCode", "48640000");
					json.put("paymode", "3");
				}
				json.put("rtmsg", "成功!");
				return json.toJSONString();
			} else if ("00".equals(respCode) && "04".equals(origRespCode)) {
				//对应报文：origRespCode=04&origRespMsg=交易状态未明，请查询对账结果[8090001]&respCode=00&respMsg=成功[0000000]
				String updateSql = "update pg_wechat_txn t set t.lmdate=sysdate,t.respcode=?,"
						+ " t.respmsg=? where status<>'1' and  t.mer_orderid=? ";
				int count = dao.update(updateSql, origRespCode, origRespMsg, orderid);
				json.put("errcode", "R");
				json.put("rtmsg", respMsg);
				return json.toJSONString();
			} else {
				String updateSql="update pg_wechat_txn t set t.lmdate=sysdate,t.respcode=?,"
						+ " t.respmsg=? where status<>'1' and  t.mer_orderid=? ";
				int count=dao.update(updateSql, respCode,respMsg,orderid);
				json.put("errcode", "E");
				json.put("rtmsg", respMsg);
				return json.toJSONString();
			}
		} else {
			RedisUtil.addFailCountByRedis(1);
			json.put("errcode", "E");
			json.put("rtmsg", "查询异常，请重新查询！");
		}
		return json.toJSONString();
	}
	
	/**
	 * 银联二维码被扫查询
	 * @param reqParam
	 * @return
	 */
	public String cupsPayQueryCups(String orderid) throws BusinessException{
		
		JSONObject json= new JSONObject();
		String querySql="select t.qrcode,t.time_end,t.status,t.accno,t.bk_orderid,t.ISSCODE, "
				+ " t.unno from pg_wechat_txn t where t.mer_orderid=?";
		List<Map<String, Object>> list=dao.queryForList(querySql, orderid);
		
		if (list.size()<1){
			throw new BusinessException(8000, "订单号不存在！");
		}
		String status = String.valueOf(list.get(0).get("STATUS"));
		if("1".equals(status)){
			json.put("errcode", "S");
			json.put("time_end", String.valueOf(list.get(0).get("TIME_END")));
			if("110000".equals(String.valueOf(list.get(0).get("UNNO")))){
				json.put("accNo", String.valueOf(list.get(0).get("ACCNO")));
				json.put("issCode", String.valueOf(list.get(0).get("ISSCODE")));
				json.put("voucherNum", String.valueOf(list.get(0).get("BK_ORDERID")));
				json.put("acqCode", "48640000");
			}
			json.put("orderid", orderid);
			json.put("rtmsg", "成功!");
			return json.toJSONString();
		}
		
		String qrNo = String.valueOf(list.get(0).get("QRCODE"));
		String orderTime =String.valueOf(list.get(0).get("TIME_END"));
		
		Map<String, String> contentData = new HashMap<String, String>();
		contentData.put("version", "1.0.0");
		contentData.put("reqType", "0350000903");
		contentData.put("acqCode", acqCode);  
		
		contentData.put("qrNo", qrNo);
		contentData.put("orderNo", orderid);
		contentData.put("orderTime", orderTime);
		contentData.put("areaInfo", "1561000");
		
		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		Map<String,String> respMap=sendMsg(contentData,requestUrl);
		logger.info("[银联绑卡]查询返回{}",respMap);
		if(respMap!=null){
			String respCode=respMap.get("respCode");
			String respMsg=respMap.get("respMsg");
			String origRespCode=respMap.get("origRespCode");
			String origRespMsg =respMap.get("origRespMsg");
			if("00".equals(respCode) && "00".equals(origRespCode)){
				String voucherNum=respMap.get("voucherNum");	
				String settleKey=respMap.get("settleKey");
				String payerInfo=respMap.get("payerInfo");
				Map<String,String> payerMap =decryptPayerInfo(payerInfo);
				String accNo="";
				String issCode="";
				String cardAttr="";
				if(payerMap!=null){
					accNo=payerMap.get("accNo");
					issCode=payerMap.get("issCode");
					cardAttr=payerMap.get("cardAttr").replace("0", "");
				}
				String updateSql="update pg_wechat_txn t set t.status='1',t.lmdate=sysdate,"
						+ " t.bk_orderid=?,t.settlekey=?,t.respcode=?,t.respmsg=?,"
						+ " t.accno=?,t.isscode=?,t.paytype=?"
						+ " where status<>'1' and t.time_end=? and t.mer_orderid=? ";
				int count=dao.update(updateSql, voucherNum,settleKey,origRespCode,origRespMsg,
						accNo,issCode,cardAttr,orderTime,orderid);
				json.put("errcode", "S");
				json.put("time_end", orderTime);
				json.put("orderid", orderid);
				if("110000".equals(String.valueOf(list.get(0).get("UNNO")))){
					json.put("accNo", accNo);
					json.put("issCode", issCode);
					json.put("voucherNum", voucherNum);
					json.put("acqCode", "48640000");
				}
				json.put("rtmsg", "成功!");
				return json.toJSONString();
			} else if ("00".equals(respCode) && "04".equals(origRespCode)) {
				//对应报文：origRespCode=04&origRespMsg=交易状态未明，请查询对账结果[8090001]&respCode=00&respMsg=成功[0000000]
				String updateSql = "update pg_wechat_txn t set t.lmdate=sysdate,t.respcode=?,"
						+ " t.respmsg=? where status<>'1' and  t.mer_orderid=? ";
				int count = dao.update(updateSql, origRespCode, origRespMsg, orderid);
				json.put("errcode", "R");
				json.put("rtmsg", respMsg);
				return json.toJSONString();
			} else if("35".equals(respCode)){ //针对网络失败 新增的订单不存在状态
//				respCode=35&respMsg=原交易不存在或状态不正确[3102031]&
				String updateSql = "update pg_wechat_txn t set status='6',t.lmdate=sysdate,t.respcode=?,"
						+ " t.respmsg=? where status<>'1' and  t.mer_orderid=? ";
				int count = dao.update(updateSql, respCode, respMsg, orderid);
				json.put("errcode", "NOTEXIT");
				json.put("rtmsg", respMsg);
				return json.toJSONString();
			} else {
				String updateSql="update pg_wechat_txn t set t.lmdate=sysdate,t.respcode=?,"
						+ " t.respmsg=? where status<>'1' and  t.mer_orderid=? ";
				int count=dao.update(updateSql, respCode+"-"+origRespCode,respMsg+"-"+origRespMsg,orderid);
				json.put("errcode", "E");
				json.put("rtmsg", respMsg+"-"+origRespMsg);
				return json.toJSONString();
			}
		} else {
			logger.info("map为空");
			RedisUtil.addFailCountByRedis(1);
			json.put("errcode", "E");
			json.put("rtmsg", "查询异常，请重新查询！");
		}
		return json.toJSONString();
	}
	
	
//	/**
//	 * 银联二维码被扫撤销
//	 * @param reqParam
//	 * @return
//	 */
//	public String cupsBsUndo(String orderid) throws BusinessException{
//		
//		String orderNo = "cx11000020170220000001";
//		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
//		String orderTime = sf.format(new Date());
////		String txnAmt ="1";
//		String voucherNum ="20170220506202113786";
//		String origOrderNo ="11000020170220100747407221";
//		String origOrderTime ="20170220100747";
//
//		Map<String, String> contentData = new HashMap<String, String>();
//		contentData.put("version", "1.0.0");
//		contentData.put("reqType", "0330000903");
//		contentData.put("acqCode", acqCode); 
//		
//		contentData.put("origOrderNo", origOrderNo); 
//		contentData.put("origOrderTime", origOrderTime); 
//		
//		contentData.put("orderNo",orderNo);
//		contentData.put("orderTime",orderTime);
//		
////		contentData.put("currencyCode", "156"); 
////		contentData.put("txnAmt", txnAmt); 
//		
//		contentData.put("voucherNum", voucherNum);
//		
//		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
//		 Map<String,String> respMap=sendMsg(contentData,requestUrl);
//		 if(respMap!=null){
//			String respCode=respMap.get("respCode");
//			String respMsg=respMap.get("respMsg");
//			if(("00").equals(respCode)){
//				logger.info("被扫撤销****************"+respMap);
//			}else{
//				//其他应答码为失败请排查原因
//			}
//		 }
//		
//		
//		return null;
//		
//	}
	
	
	
	/**
	 * 银联二维码撤销(主扫，被扫)
	 * @param reqParam
	 * @return
	 */
	public String cupsUndo(String orderid) throws BusinessException{
		
		JSONObject json = new JSONObject();
		String querySql="select * "
						//+ "t.bk_orderid,t.time_end,t.status,t.bank_type "
						+ " from pg_wechat_txn t where t.cdate between trunc(sysdate) "
						+ " and trunc(sysdate+1) and t.mer_orderid=?";
		List<Map<String, Object>> list=dao.queryForList(querySql, orderid);
		
		if (list.size()<1){
			throw new BusinessException(8000, "订单号不存在！");
		}
		String orderNo = "cx"+orderid;
		
		//插入退款订单
		if (dao.queryForList("select * from Pg_Wechat_Txn t where t.mer_orderid=?", orderNo).size()>0) {
			throw new BusinessException(8000, "订单号重复撤销！");
		}
		
		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
		String orderTime = sf.format(new Date());
		String voucherNum =String.valueOf(list.get(0).get("BK_ORDERID"));
		String origOrderNo =orderid;
		String origOrderTime =String.valueOf(list.get(0).get("TIME_END"));
		String mid=String.valueOf(list.get(0).get("MER_ID"));
		String bank_mid=String.valueOf(list.get(0).get("BANKMID"));
		String txnamt=String.valueOf(list.get(0).get("TXNAMT"));
		String unno=String.valueOf(list.get(0).get("UNNO"));
		String pwid=String.valueOf(list.get(0).get("PWID"));
		String bankType=String.valueOf(list.get(0).get("BANK_TYPE"));
		
		String reqType="";
		if("BS".equals(bankType)){
			reqType=undoBsReqType;
		}else if("ZS".equals(bankType)){
			reqType=undoZsReqType;
		}

		Map<String, String> contentData = new HashMap<String, String>();
		contentData.put("version", "1.0.0");
		contentData.put("reqType", reqType);
		contentData.put("acqCode", acqCode);
		
		contentData.put("origOrderNo", origOrderNo); 
		contentData.put("origOrderTime", origOrderTime); 
		
		contentData.put("orderNo",orderNo);
		contentData.put("orderTime",orderTime);
		
		
		contentData.put("voucherNum", voucherNum);
		
		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		 Map<String,String> respMap=sendMsg(contentData,requestUrl);
		 if(respMap!=null){
			String respCode=respMap.get("respCode");
			String respMsg=respMap.get("respMsg");
			if(("00").equals(respCode)){
				logger.info("银联二维码撤销成功****************"+respMap);
				
				BigDecimal cxpwid = getNewPwid();
				String insertSql = "insert into pg_wechat_txn (pwid,oripwid,fiid, txntype,cdate,status,"
						+ "mer_orderid, txnamt,mer_id,unno,bankmid,lmdate,respcode,respmsg,bank_type,time_end,trantype) values"
						+ "(?,?,?,'2',sysdate,'1',?,?,?,?,?,sysdate,?,?,?,?,'3')";
				
				dao.update(insertSql, cxpwid,pwid,18,orderNo,txnamt,mid,unno,bank_mid,respCode,respMsg,bankType,orderTime);
				json.put("errcode", "S");
				json.put("rtmsg", "成功!");
				return json.toJSONString();
			}else{
				//其他应答码为失败请排查原因
				logger.info("银联二维码撤销失败****************订单号:"+orderid+"---->"+respMap);
				json.put("errcode", "E");
				json.put("rtmsg", "撤销失败："+respMsg);
				return json.toJSONString();
			}
		 }
		json.put("errcode", "E");
		json.put("rtmsg", "撤销失败！");
		return json.toJSONString();
		
	}
	
	private BigDecimal getNewPwid() {
		List<Map<String, Object>> list = dao.queryForList("select S_PG_Wechat_Txn.nextval pwid from dual");
		if (list.size()>0) {
			Map<String, Object> map = list.get(0);
			if (map.containsKey("PWID")) {
				return (BigDecimal) map.get("PWID");
			}
		}
		logger.info("获取pwid(S_PG_Wechat_Txn.nextval)失败");
		throw new HrtBusinessException(8000);
	}
	
	
	
	/**
	 * 银联二维码退货(主扫,被扫)
	 * @param reqParam
	 * @return
	 */
	public String cupsRefund(String orderid,BigDecimal amount,Map<String, Object> oriMap){
		
		JSONObject json = new JSONObject();
		try {
			SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
			String orderTime = sf.format(new Date());
			String voucherNum =String.valueOf(oriMap.get("BK_ORDERID"));
			String origOrderNo =String.valueOf(oriMap.get("MER_ORDERID"));
			String origOrderTime =String.valueOf(oriMap.get("TIME_END"));
			String reqType="";
			if("BS".equals(oriMap.get("BANK_TYPE"))){
				reqType=refundBsReqType;
			}else if("ZS".equals(oriMap.get("BANK_TYPE"))){
				reqType=refundZsReqType;
			}
			
			Map<String, String> contentData = new HashMap<String, String>();

			contentData.put("version", "1.0.0");
			contentData.put("reqType", reqType);
			contentData.put("acqCode", acqCode); 
			
			contentData.put("origOrderNo", origOrderNo); 
			contentData.put("origOrderTime", origOrderTime); 
			
			contentData.put("orderNo",orderid);
			contentData.put("orderTime",orderTime);
			
			contentData.put("currencyCode", "156"); 
			contentData.put("txnAmt", amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()+""); 
			
			contentData.put("voucherNum", voucherNum);
			
			String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
			 Map<String,String> respMap=sendMsg(contentData,requestUrl);
			 if(respMap!=null){
				String respCode=respMap.get("respCode");
				String respMsg=respMap.get("respMsg");
				if(("00").equals(respCode)){
					String updateSql="update pg_wechat_txn t set t.status=1,t.lmdate=sysdate,"
									+ " t.respcode=?,t.respmsg=?,t.time_end=? where txntype='1' "
									+ " and t.status='A' and mer_orderid=?";
					dao.update(updateSql, respCode,respMsg,orderTime,orderid);
					json.put("errcode", "S");
					json.put("time_end", orderTime);
					json.put("orderid", orderid);
					json.put("rtmsg", "退款成功!");
					return json.toJSONString();
				}else{
					//其他应答码为失败请排查原因
					logger.info("银联二维码退货失败---------------->"+respMap);
					json.put("errcode", "E");
					json.put("rtmsg", "退款失败!");
					return json.toJSONString();
				}
			 }
		} catch (Exception e) {
			logger.error("银联二维码退货异常："+e);
			json.put("errcode", "E");
			json.put("rtmsg", "退款失败!");
		}
		return json.toJSONString();
		
	}
	
	
	
//	/**
//	 * 银联二维码被扫退货
//	 * @param reqParam
//	 * @return
//	 */
//	public String cupsBsRefund(String orderid) throws BusinessException{
//		
//		String orderNo = "th11000020170220000001";
//		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
//		String orderTime = sf.format(new Date());
//		String txnAmt ="50";
//		String voucherNum ="20170220059525323967";
//		String origOrderNo ="11000020170220101220723444";
//		String origOrderTime ="20170220101220";
//		
//		Map<String, String> contentData = new HashMap<String, String>();
//
//		contentData.put("version", "1.0.0");
//		contentData.put("reqType", "0340000903");
//		contentData.put("acqCode", acqCode); 
//		
//		contentData.put("origOrderNo", origOrderNo); 
//		contentData.put("origOrderTime", origOrderTime); 
//		
//		contentData.put("orderNo",orderNo);
//		contentData.put("orderTime",orderTime);
//		
//		contentData.put("currencyCode", "156"); 
//		contentData.put("txnAmt", txnAmt); 
//		
//		contentData.put("voucherNum", voucherNum);
//		
//		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
//		 Map<String,String> respMap=sendMsg(contentData,requestUrl);
//		 if(respMap!=null){
//			String respCode=respMap.get("respCode");
//			String respMsg=respMap.get("respMsg");
//			if(("00").equals(respCode)){
//				logger.info("被扫退货****************"+respMap);
//			}else{
//				//其他应答码为失败请排查原因
//			}
//		 }
//		
//		
//		return null;
//		
//	}
	
	
	/**
	 * 银联二维码主扫收款查询
	 * @param reqParam
	 * @return
	 */
	public String cupsZsShoukuanQuery(String  orderid) throws BusinessException{
		JSONObject json = new JSONObject();
		String querySql="select t.bk_orderid,t.time_end,t.status,t.accno,t.bk_orderid,t.ISSCODE,"
						+ "  t.unno from pg_wechat_txn t where t.mer_orderid=?";
		List<Map<String, Object>> list=dao.queryForList(querySql, orderid);
		
		if (list.size()<1){
			throw new BusinessException(8000, "订单号不存在！");
		}
		String status = String.valueOf(list.get(0).get("STATUS"));
		if("1".equals(status)){
			json.put("errcode", "S");
			json.put("time_end", String.valueOf(list.get(0).get("TIME_END")));
			if("110000".equals(String.valueOf(list.get(0).get("UNNO")))){
				json.put("accNo", String.valueOf(list.get(0).get("ACCNO")));
				json.put("issCode", String.valueOf(list.get(0).get("ISSCODE")));
				json.put("voucherNum", String.valueOf(list.get(0).get("BK_ORDERID")));
				json.put("acqCode", "48640000");
			}
			json.put("orderid", orderid);
			json.put("rtmsg", "成功!");
			return json.toJSONString();
		}
		
		String orderTime = String.valueOf(list.get(0).get("TIME_END"));
		Map<String, String> contentData = new HashMap<String, String>();

		contentData.put("version", "1.0.0");
		contentData.put("reqType", "0540000903");
		contentData.put("acqCode", acqCode); 
		
		contentData.put("orderNo", orderid); //获取二维码的订单号
		contentData.put("orderTime", orderTime);//获取二维码的订单时间 
		contentData.put("areaInfo", "1561000");
//		contentData.put("voucherNum", voucherNum);
		
		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		Map<String,String> respMap=sendMsg(contentData,requestUrl);
		 if(respMap!=null){
			String respCode=respMap.get("respCode");
			String respMsg=respMap.get("respMsg");
			String origRespCode=respMap.get("origRespCode");
			String origRespMsg =respMap.get("origRespMsg");
			if("00".equals(respCode) && "00".equals(origRespCode)){
				String voucherNum =respMap.get("voucherNum");
				String settleKey=respMap.get("settleKey");
				String payerInfo=respMap.get("payerInfo");
				Map<String,String> payerMap =decryptPayerInfo(payerInfo);
				String accNo="";
				String issCode="";
				String cardAttr="";
				if(payerMap!=null){
					accNo=payerMap.get("accNo");
					issCode=payerMap.get("issCode");
					cardAttr=payerMap.get("cardAttr").replace("0", "");
				}
				String updateSql="update pg_wechat_txn t set t.status='1',t.lmdate=sysdate,"
								+ " t.bk_orderid=?,t.settlekey=?,t.respcode=?,t.respmsg=?,"
								+ " t.accno=?,t.isscode=?,t.paytype=?"
								+ " where t.time_end=? and t.mer_orderid=? ";
				dao.update(updateSql, voucherNum,settleKey,origRespCode,origRespMsg,
						accNo,issCode,cardAttr,orderTime,orderid);
				json.put("errcode", "S");
				json.put("time_end", orderTime);
				if("110000".equals(String.valueOf(list.get(0).get("UNNO")))){
					json.put("accNo", accNo);
					json.put("issCode", issCode);
					json.put("voucherNum", voucherNum);
					json.put("acqCode", "48640000");
				}
				json.put("orderid", orderid);
				json.put("rtmsg", "成功!");
				return json.toJSONString();
			}else{
				String updateSql="update pg_wechat_txn t set t.lmdate=sysdate,t.respcode=?,"
						+ " t.respmsg=? where t.mer_orderid=? ";
				dao.update(updateSql, respCode,respMsg,orderid);
				
				json.put("errcode", "E");
				json.put("rtmsg", respMsg);
				return json.toJSONString();
			}
		} else {
			RedisUtil.addFailCountByRedis(1);
			json.put("errcode", "E");
			json.put("rtmsg", "查询异常，请重新查询！");
		}
		return json.toJSONString();
	}
	
	
//	/**
//	 * 银联二维码主扫退款
//	 * @param reqParam
//	 * @return
//	 */
//	public String cupsZsRefund(String orderid) throws BusinessException{
//		
//		String orderNo = "zth11000020170220000001";
//		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
//		String orderTime = sf.format(new Date());
//		String txnAmt ="50";
//		String voucherNum ="20170220218372384079";
//		String origOrderNo ="110000201702201010119267601";
//		String origOrderTime ="20170220101012";
//		
//		Map<String, String> contentData = new HashMap<String, String>();
//
//		contentData.put("version", "1.0.0");
//		contentData.put("reqType", "0150000903");
//		contentData.put("acqCode", acqCode); 
//		
//		contentData.put("origOrderNo", origOrderNo); 
//		contentData.put("origOrderTime", origOrderTime); 
//		
//		contentData.put("orderNo",orderNo);
//		contentData.put("orderTime",orderTime);
//		
//		contentData.put("currencyCode", "156"); 
//		contentData.put("txnAmt", txnAmt); 
//		
//		contentData.put("voucherNum", voucherNum);
//		
//		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
//		 Map<String,String> respMap=sendMsg(contentData,requestUrl);
//		 if(respMap!=null){
//			String respCode=respMap.get("respCode");
//			String respMsg=respMap.get("respMsg");
//			if(("00").equals(respCode)){
//
//			}else{
//				//其他应答码为失败请排查原因
//			}
//		 }
//		
//		
//		return null;
//		
//	}
	
//	/**
//	 * 银联二维码主扫撤销
//	 * @param reqParam
//	 * @return
//	 */
//	public String cupsZsUndo(String orderid) throws BusinessException{
//		
//		String orderNo = "zcx11000020170220000001";
//		SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
//		String orderTime = sf.format(new Date());
////		String txnAmt ="1";
//		String voucherNum ="20170220416007680924";
//		String origOrderNo ="110000201702200936000001";
//		String origOrderTime ="20170220093703";
//
//		Map<String, String> contentData = new HashMap<String, String>();
//		contentData.put("version", "1.0.0");
//		contentData.put("reqType", "0170000903");
//		contentData.put("acqCode", acqCode); 
//		
//		contentData.put("origOrderNo", origOrderNo); 
//		contentData.put("origOrderTime", origOrderTime); 
//		
//		contentData.put("orderNo",orderNo);
//		contentData.put("orderTime",orderTime);
//		
////		contentData.put("currencyCode", "156"); 
////		contentData.put("txnAmt", txnAmt); 
//		
//		contentData.put("voucherNum", voucherNum);
//		
//		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
//		Map<String,String> respMap=sendMsg(contentData,requestUrl);
//		 if(respMap!=null){
//			String respCode=respMap.get("respCode");
//			String respMsg=respMap.get("respMsg");
//			if(("00").equals(respCode)){
//				logger.info("主扫消费撤销****************"+respMap);
//			}else{
//				//其他应答码为失败请排查原因
//			}
//		 }
//		
//		
//		return null;
//		
//	}
	
	/**
	 * 银联二维码主扫(撤销二维码)
	 * @param reqParam
	 * @return
	 */
	public String getCupsPayQrcodeCancel(Map<String,String> reqParam){
		
		String qrCode = reqParam.get("qrCode");

		Map<String, String> contentData = new HashMap<String, String>();
		contentData.put("version", "1.0.0");
		contentData.put("reqType", "0570000903");
		contentData.put("acqCode", acqCode); 
		
		contentData.put("qrCode",qrCode);//被撤销的二维码
		
		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		Map<String,String> respMap=sendMsg(contentData,requestUrl);
		 if(respMap!=null){
			String respCode=respMap.get("respCode");
			String respMsg=respMap.get("respMsg");
			if(("00").equals(respCode)){

			}else{
				//其他应答码为失败请排查原因
			}
		 }
		 
		return null;
		
	}
	

	/**
	 * 银联通讯
	 * @param map
	 * @param requestUrl
	 * @return
	 */
	public Map<String, String> sendMsg(Map<String,String> map, String requestUrl){
		
		/**
		 * 加签 发送
		 */

		Map<String, String> reqData = AcpService.sign(map,DemoBase.encoding);			 //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
		Map<String, String> rspData = AcpService.post(reqData,requestUrl,DemoBase.encoding);  //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

		if(!rspData.isEmpty()){
			if(AcpService.validate(rspData, DemoBase.encoding)){
				logger.info("验证签名成功");
				
				return rspData;


			}else{
				logger.info("验证签名失败");
			}
		}else{
			//未返回正确的http状态
			logger.info("未获取到返回报文或返回http状态码非200");
		}
		
		return null;
	}
	
	/**
	 * 轮询挂号
	 * @param bean
	 * @return
	 */
	public Map<String, Object> getMerchantCode(HrtPayXmlBean bean) throws BusinessException{
		
		/**
		 * bean
		 * 根据hrt商户号 查询所挂银联商户号以及商户类别
		 * 
		 */
		List<Map<String, Object>> list;
		if (bean.getUnno()==null || "110000".equals(bean.getUnno())|| "880000".equals(bean.getUnno())) {
			String sql = "select a.merchantcode,a.category mercatcode,a.merchantname from Bank_MerRegister a,HRT_MerBanksub b,"
					+ "Hrt_Merchacc ma where ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and a.approvestatus='Y' and a.status=1 "
					+ "and ma.hrt_MID =? and a.fiid=?";
			list = dao.queryForList(sql, bean.getMid(),18);
		} else {
			String sql = "select a.merchantcode,a.category mercatcode ,a.merchantname from Bank_MerRegister a,HRT_MerBanksub b,"
					+ "Hrt_Merchacc ma where ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and a.approvestatus='Y' and a.status=1"
					+ "and ma.hrt_MID =? and a.fiid=? and ma.unno=?";
			list = dao.queryForList(sql, bean.getMid(),18,bean.getUnno());
		}
//		if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
		if (list.size()<1){
			 Map<String, Object> payeeInfo = new HashMap<String, Object>();
			 payeeInfo.put("MERCHANTNAME", String.valueOf("和融通"));
			 payeeInfo.put("MERCHANTCODE", String.valueOf("301290070110001"));
			 payeeInfo.put("MERCATCODE", "7011");
			 return payeeInfo;
		};
		Map<String,Object> map = list.get(0);
		logger.info("商户信息---------------------->"+map.toString());
		return map;
		
	}
	
	
	/**
	 * 轮询挂号
	 * @param bean
	 * @return
	 */
	public Map<String, Object> getMerchantCode2(HrtPayXmlBean bean,Integer orderCon) throws BusinessException{
		BigDecimal amt = new BigDecimal(bean.getAmount());
		List<Map<String, Object>> list;
		/**
		 * 2018-09-06 新增限制
		 * 同一个商户号一天24小时不跳号。
		 * 判断 
		 * if  orderCo!=0 
		 * then 取原有的银行商户号和商户信息  并在扣减  对应的金额
		 * else 按照原有逻辑获取银行商户号和商户信息
		 */
		if (orderCon!=0) {
			String bankmid=bean.getBankMid();
			String sql="select merchantcode,merchantname,category  mercatcode,ht.hpid,ht.txnmaxcount,ht.groupname"
					+ " from bank_merregister bm ,hrt_termaccpool ht ,hrt_fi hf "
					+ " where bm.hrid=ht.btaid  and bm.fiid=hf.fiid   and bm.merchantcode=? "
					+ "  and ht.status ='1'  and hf.status='1' and hf.fiinfo2 is not null "
					+ "  and bm.approvestatus='Y'  ";
			list = dao.queryForList(sql,bankmid);
			if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");

			for (Map<String, Object> bankMidInfo :list) { 
				Integer hpid = Integer.parseInt(String.valueOf(bankMidInfo.get("HPID")));
				Integer txnmaxcount = Integer.parseInt(String.valueOf(bankMidInfo.get("TXNMAXCOUNT")));
				String gorupName=String.valueOf(bankMidInfo.get("groupname"));
				if (txnmaxcount<=1 ) {
					String updateTxnmaxcountSql=" update HRT_TERMACCPOOL t  set  t.txnmaxcount = '99999999' " 
							+ " where t.status=1  and t.groupname=?";
					dao.update(updateTxnmaxcountSql, gorupName);
				}
				String updateSql=" update HRT_TERMACCPOOL t set t.txnamt=nvl(t.txnamt,0)+?,"
								+ " t.txncount=t.txncount+1 , txnmaxcount = to_char(txnmaxcount-1) " //(case txnmaxcount when 0 then  '9999999' else to_char(txnmaxcount-1) end) "
								+ " where t.status=1 and "
								+ " t.txnmaxamt>=nvl(t.txnamt,0)+? and t.hpid=?";
				Integer count=dao.update(updateSql, amt,amt,hpid);
				if(count>0){
					return bankMidInfo;
				}
			}
		}
		if (bean.getUnno()==null || "110000".equals(bean.getUnno())|| "880000".equals(bean.getUnno())) {
			String sql = "select a.merchantcode,a.category mercatcode,a.merchantname,a.fiid,a.merchantid from Bank_MerRegister a,"
						+ "(select hrid,status,hrt_MID  from Hrt_Merbanksub where hrt_MID =?) b,"
						+ "Hrt_Merchacc ma,hrt_fi f where "
						+ "ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and "
						+ "a.fiid=f.fiid and a.approvestatus='Y' and a.status=1 "
						+ "and b.status=1 and ma.status=1 and f.fiinfo2 like ? "
						+ "order by a.fiid desc";
			
			list = dao.queryForList(sql, bean.getMid(),"%LMF%");
		} else {
			String sql = "select a.merchantcode,a.category mercatcode,a.merchantname,a.fiid,a.merchantid from Bank_MerRegister a,"
						+ "(select hrid,status,hrt_MID  from Hrt_Merbanksub where hrt_MID =?) b,"
						+ "Hrt_Merchacc ma,hrt_fi f where "
						+ "ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and "
						+ "a.fiid=f.fiid and a.approvestatus='Y' and a.status=1 "
						+ "and b.status=1 and ma.status=1 and f.fiinfo2 like ? and ma.unno=? "
						+ "order by a.fiid desc";
			
			list = dao.queryForList(sql,bean.getMid(),"%LMF%",bean.getUnno());
		}
		if (list.size()<1){
			throw new BusinessException(8001,"指定通道未开通");
		}
		Map<String,Object> map = list.get(0);
		String fiid =String.valueOf(map.get("FIID"));
		if("99".equals(fiid)){
//			logger.info("哈哈哈哈哈。。。。。。走轮序组了！");
			String gorupName= String.valueOf(map.get("MERCHANTID"));
			String poolSql=" select * from (select t1.hpid,t2.merchantcode,t2.fiid,t2.category mercatcode,"
							+ " t2.merchantname,T1.txnmaxcount  from hrt_termaccpool T1,"
							+ " bank_merregister T2,hrt_fi f WHERE t1.btaid=t2.hrid"
							+ " and T2.fiid=f.fiid and f.fiinfo2 like ? and T1.status=1 "
							+ " and t1.txnmaxamt>=nvl(t1.txnamt,0)+? and t1.groupname=? "
							+ " and T2.status=1 order by T1.txnamt asc,txnmaxcount desc) where rownum=1 ";
			list = dao.queryForList(poolSql, "%LMF%",amt,gorupName);
			if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
			
			for(Map<String, Object> mm :list){
				Integer hpid = Integer.parseInt(String.valueOf(mm.get("HPID")));
				Integer txnmaxcount = Integer.parseInt(String.valueOf(mm.get("TXNMAXCOUNT")));
				if (txnmaxcount<=1 ) {
					String updateTxnmaxcountSql=" update HRT_TERMACCPOOL t  set  t.txnmaxcount = '99999999' " 
							+ " where t.status=1  and t.groupname=?";
					dao.update(updateTxnmaxcountSql, gorupName);
				}
				String updateSql=" update HRT_TERMACCPOOL t set t.txnamt=nvl(t.txnamt,0)+?,"
								+ " t.txncount=t.txncount+1 , txnmaxcount = to_char(txnmaxcount-1) " //(case txnmaxcount when 0 then  '9999999' else to_char(txnmaxcount-1) end) "
								+ " where t.status=1 and "
								+ " t.txnmaxamt>=nvl(t.txnamt,0)+? and t.hpid=?";
				Integer count=dao.update(updateSql, amt,amt,hpid);
				if(count>0){
					return mm;
				}
			}
			throw new BusinessException(8001,"指定通道未开通");
		}
		return map;
		
	}
	
	
	/**
	 * 本方法无需挂号，只针对信用卡代还使用
	 * 2018-01-29
	 * @param groupName
	 * @return
	 * @throws BusinessException 
	 */
	private Map<String, Object> getMerchantCodeForCups(BigDecimal amt,String groupName,String bankMid) throws BusinessException{
		String poolSql="";
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		if (null != bankMid && !"".equals(bankMid) && !"null".equals(bankMid)) {
			poolSql= "select t2.merchantcode,t2.fiid,t2.category mercatcode,t2.merchantname "
					 +" from  bank_merregister T2 "
					 +" WHERE t2.merchantcode=? and T2.status=1 ";
			list = dao.queryForList(poolSql,bankMid);
			if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
			return list.get(0);
		}else{
			poolSql=" select * from (select t1.hpid,t2.merchantcode,t2.fiid,t2.category mercatcode,"
					+ " t2.merchantname from hrt_termaccpool T1,"
					+ " bank_merregister T2,hrt_fi f WHERE t1.btaid=t2.hrid"
					+ " and T2.fiid=f.fiid"
					+ " and T1.status=1 "
					+ " and T1.starttime < to_char(sysdate, 'HH24MI') and T1.endtime >= to_char(sysdate, 'HH24MI')"
					+ " and t1.txnmaxamt>=nvl(t1.txnamt,0)+? and t1.groupname=? "
					+ " and T2.status=1 order by T1.txnamt asc) where rownum=1 ";
			 list = dao.queryForList(poolSql,amt,groupName);
			 if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
			 for(Map<String, Object> mm :list){
			      Integer hpid = Integer.parseInt(String.valueOf(mm.get("HPID")));
				   String updateSql=" update HRT_TERMACCPOOL t set t.txnamt=nvl(t.txnamt,0)+?,"
						+ " t.txncount=t.txncount+1 where t.status=1 and "
						+ " t.txnmaxamt>=nvl(t.txnamt,0)+? and t.hpid=?";
				  Integer count=dao.update(updateSql, amt,amt,hpid);
				  if(count>0){
					return mm;
				  }else{
					  logger.error("[信用卡代还]轮询组选号 未选中{}",hpid);
					  throw new BusinessException(8001,"指定通道未开通");
				  }
			 }
		}
		return null;
	}
	
	
	/**
	 * 本方法无需挂号，只针对快捷支付使用
	 * 2018-10-16
	 * @param groupName
	 * @return
	 * @throws BusinessException 
	 */
	private Map<String, Object> getMerchantCodeForCups2(BigDecimal amt,String bankMid) throws BusinessException{
		
		String groupName="";
		Double amt_double =amt.doubleValue();
		if(amt_double>=5&&amt_double<=1000 ){
			groupName="QPAY1";
		}else if(amt_double>1000 && amt_double<=5000){
			groupName="QPAY2";
		}else if(amt_double>5000 && amt_double<=10000){
			groupName="QPAY3";
		}else{
			throw new BusinessException(9001,"金额错误！");
		}
		
		String poolSql="";
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		if (null != bankMid && !"".equals(bankMid) && !"null".equals(bankMid)) {
			poolSql= "select t2.merchantcode,t2.fiid,t2.category mercatcode,t2.merchantname "
					 +" from  bank_merregister T2 "
					 +" WHERE t2.merchantcode=? and T2.status=1 ";
			list = dao.queryForList(poolSql,bankMid);
			if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
			return list.get(0);
		}else{
			poolSql=" select * from (select t1.hpid,t2.merchantcode,t2.fiid,t2.category mercatcode,"
					+ " t2.merchantname from hrt_termaccpool T1,"
					+ " bank_merregister T2,hrt_fi f WHERE t1.btaid=t2.hrid"
					+ " and T2.fiid=f.fiid"
					+ " and T1.status=1 "
					+ " and T1.starttime < to_char(sysdate, 'HH24MI') and T1.endtime >= to_char(sysdate, 'HH24MI')"
					+ " and t1.txnmaxamt>=nvl(t1.txnamt,0)+? and t1.groupname=? "
					+ " and T2.status=1 order by T1.txnamt asc) where rownum=1 ";
			 list = dao.queryForList(poolSql,amt,groupName);
			 if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
			 for(Map<String, Object> mm :list){
			      Integer hpid = Integer.parseInt(String.valueOf(mm.get("HPID")));
				   String updateSql=" update HRT_TERMACCPOOL t set t.txnamt=nvl(t.txnamt,0)+?,"
						+ " t.txncount=t.txncount+1 where t.status=1 and "
						+ " t.txnmaxamt>=nvl(t.txnamt,0)+? and t.hpid=?";
				  Integer count=dao.update(updateSql, amt,amt,hpid);
				  if(count>0){
					return mm;
				  }else{
					  logger.error("[信用卡代还]轮询组选号 未选中{}",hpid);
					  throw new BusinessException(8001,"指定通道未开通");
				  }
			 }
		}
		return null;
	}
	
	@Deprecated
	private Map<String, Object> getMerchantCodeForCups(BigDecimal amt,String groupName) throws BusinessException{
		String poolSql=" select * from (select t1.hpid,t2.merchantcode,t2.fiid,t2.category mercatcode,"
						+ " t2.merchantname from hrt_termaccpool T1,"
						+ " bank_merregister T2,hrt_fi f WHERE t1.btaid=t2.hrid"
						+ " and T2.fiid=f.fiid"
//						+ " and f.fiinfo2 like ?"
						+ " and T1.status=1 "
						+ " and T1.starttime < to_char(sysdate, 'HH24MI') and T1.endtime >= to_char(sysdate, 'HH24MI')"
						+ " and t1.txnmaxamt>=nvl(t1.txnamt,0)+? and t1.groupname=? "
						+ " and T2.status=1 order by T1.txnamt asc) where rownum=1 ";
//		List<Map<String, Object>> list = dao.queryForList(poolSql, "%LMF%",amt,groupName);
		List<Map<String, Object>> list = dao.queryForList(poolSql,amt,groupName);
		if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
		
		for(Map<String, Object> mm :list){
			Integer hpid = Integer.parseInt(String.valueOf(mm.get("HPID")));
			String updateSql=" update HRT_TERMACCPOOL t set t.txnamt=nvl(t.txnamt,0)+?,"
							+ " t.txncount=t.txncount+1 where t.status=1 and "
							+ " t.txnmaxamt>=nvl(t.txnamt,0)+? and t.hpid=?";
			Integer count=dao.update(updateSql, amt,amt,hpid);
			if(count>0){
				return mm;
			}
		}
		throw new BusinessException(8001,"指定通道未开通");
	}
	
	
	/**
	 * 银联二维码主扫、被扫异步通知
	 * @param map
	 * @return
	 */
	public String updatePayCupsAsyncCallBack(Map<String,String> map){
		
		try {
			Map<String, String> valideData = null;
			if (null != map && !map.isEmpty()) {
				Iterator<Entry<String, String>> it = map.entrySet().iterator();
				valideData = new HashMap<String, String>(map.size());
				while (it.hasNext()) {
					Entry<String, String> e = it.next();
					String key = (String) e.getKey();
					String value = (String) e.getValue();
					valideData.put(key, value);
				}
			}

			//重要！验证签名前不要修改reqParam中的键值对的内容，否则会验签不过
			if (!AcpService.validate(valideData, DemoBase.encoding)) {
				logger.info("验证签名结果[失败].");
				//验签失败，需解决验签问题
				
			} else {
				logger.info("验证签名结果[成功].");
				//【注：为了安全验签成功才应该写商户的成功处理逻辑】交易成功，更新商户订单状态
				String orderNo =valideData.get("orderNo"); //获取后台通知的数据，其他字段也可用类似方式获取
				String querySql = "select * from pg_wechat_txn t where t.mer_orderid=?";
				List<Map<String, Object>> list = dao.queryForList(querySql, orderNo);
				if (list.size()<1) {
					logger.info("异步通知,未查询到订单号为{}的订单",orderNo);
					return null;
				}else{
					
					Map<String, Object> orderMap = list.get(0);
					BigDecimal pwid = (BigDecimal) orderMap.get("PWID");
					String oriStatus = (String) orderMap.get("STATUS");
					if ("1".equals(oriStatus)) {
						logger.info("原订单状态:{}",oriStatus);
						return "success";
					}
					String reqType=valideData.get("reqType");
					if("0360000903".equals(reqType)){
						/**
						 * 被扫异步通知
						 */
						logger.info("收到被扫模式异步通知");
						String respcode=valideData.get("respCode");
						String respMsg=valideData.get("respMsg");
						if("00".equals(respcode)){
							String orderTime =valideData.get("orderTime"); //
							String qrNo =valideData.get("qrNo"); //
							String voucherNum=valideData.get("voucherNum");
							String payerInfo=valideData.get("payerInfo");
							String settleKey=valideData.get("settleKey");
							Map<String,String> payerMap =decryptPayerInfo(payerInfo);
							String accNo="";
							String issCode="";
							String cardAttr="";
							if(payerMap!=null){
								accNo=payerMap.get("accNo");
								orderMap.put("ACCNO", accNo);
								issCode=payerMap.get("issCode");
								orderMap.put("ISSCODE", issCode);
								cardAttr=payerMap.get("cardAttr").replace("0", "");
								orderMap.put("BK_ORDERID", voucherNum);
							}
							String updateSql="update pg_wechat_txn t set t.respcode=?,t.respmsg=?,t.status='1',"
											+ " t.lmdate=sysdate,t.bk_orderid=? ,t.settlekey=?,t.txnlevel=1, "
											+ " t.accno=?,t.isscode=?,t.paytype=? where t.qrcode=?"
											+ " and t.time_end=? and t.mer_orderid=?  and status<>'1' and t.pwid=?";
							Integer count=dao.update(updateSql, respcode,respMsg,voucherNum,settleKey,
									accNo,issCode,cardAttr,qrNo,orderTime,orderNo,pwid);
							if (count==1) {
								orderMap.put("TXNLEVEL", BigDecimal.ONE);
								notify.sendNotify(orderMap);	
								logger.info("[银联]异步通知   订单{}更新状态成功",orderNo);
							}else if (count==0) {
								logger.info("[银联]异步通知   订单{}原状态为成功，无需更改",orderNo);
							}else {
								logger.info("[银联]异步通知   订单{}状态更新异常",orderNo);
							}
							 
						}
					}else if("0530000903".equals(reqType)){
						/**
						 * 主扫异步通知
						 */
						logger.info("收到主扫模式异步通知");
//							String orderNo =valideData.get("orderNo"); //获取后台通知的数据，其他字段也可用类似方式获取
						String orderTime =valideData.get("orderTime"); //
						String voucherNum=valideData.get("voucherNum");
						String settleKey=valideData.get("settleKey");
						String payerInfo=valideData.get("payerInfo");
						Map<String,String> payerMap =decryptPayerInfo(payerInfo);
						String accNo="";
						String issCode="";
						String cardAttr="";
						if(payerMap!=null){
							accNo=payerMap.get("accNo");
							orderMap.put("ACCNO", accNo);
							issCode=payerMap.get("issCode");
							orderMap.put("ISSCODE", issCode);
							cardAttr=payerMap.get("cardAttr").replace("0", "");
							orderMap.put("BK_ORDERID", voucherNum);
						}
						String updateSql="update pg_wechat_txn t set t.respcode=?,t.respmsg=?,t.status='1',"
								+ " t.lmdate=sysdate,t.bk_orderid=?,t.settlekey=?,t.txnlevel=1,"
								+ " t.accno=?,t.isscode=?,t.paytype=? where t.time_end=?"
								+ " and t.mer_orderid=? and status<>'1' and t.pwid=?";
						Integer count=dao.update(updateSql, "00","成功",voucherNum,settleKey,accNo,issCode,cardAttr,orderTime,orderNo,pwid);
						if (count==1) {
							orderMap.put("TXNLEVEL", BigDecimal.ONE);
							notify.sendNotify(orderMap);	
							logger.info("[银联]异步通知   订单{}更新状态成功",orderNo);
						}else if (count==0) {
							logger.info("[银联]异步通知   订单{}原状态为成功，无需更改",orderNo);
						}else {
							logger.info("[银联]异步通知   订单{}状态更新异常",orderNo);
						}
					}
				}
			}
			
		} catch (Exception e) {
			logger.info("异步通知处理异常："+e);
		}
		
		return null;
		
	}
	
	
	
	public Map<String,String> decryptPayerInfo(String payerInfo){
		try {
			String payer =AcpService.decryptData(payerInfo, "UTF-8");
			//{accNo=6217920680109514&issCode=03101000&cardAttr=01}
			String data[] =payer.replace("{", "").replace("}", "").split("&");
			Map<String,String> map = new HashMap<String,String>();
			for(int i=0;i<data.length;i++){
				String dataChild [] =data[i].split("=");
				map.put(dataChild[0], dataChild[1]);
			}
			return map;
		} catch (Exception e) {
			logger.error("付款方信息解密异常："+e);
		}
		return null;
	}
	
	/**
	 * 银联二维码被扫冲正-new
	 * @param reqParam
	 * @return
	 */
	public String cupsBsReversal(String orderid)  throws BusinessException{
		logger.info("冲正订单号" + orderid);
		JSONObject json= new JSONObject();
		//1.查询订单，判断原订单是否存在
		String querySql="select t.qrcode,t.time_end,t.status,t.bankmid,t.bank_type,t.cdate"
				+ " from pg_wechat_txn t where t.mer_orderid=?";
		List<Map<String, Object>> list=dao.queryForList(querySql, orderid);
		
		if (list.size()<1){
			throw new BusinessException(8000, "订单号不存在！");
		}
		//1.1验证冲正 被扫
		String bankType = String.valueOf(list.get(0).get("BANK_TYPE"));
		if(bankType==null||!"BS".equalsIgnoreCase(bankType)){
			throw new BusinessException(1001, "冲正交易只支持被扫模式！");
		}
		//1.2校验冲正 是否属于当天
		Date createDate = (Date)list.get(0).get("CDATE");
		if(!compareZero(createDate)){
			throw new BusinessException(1001, "冲正交易只支持当天交易！");
		}
		//1.3 验证-是否已经冲正成功
		String status = String.valueOf(list.get(0).get("status"));
		if (status == null && "9".equalsIgnoreCase(status)) {
			json.put("errcode", "S");
			json.put("rtmsg", "冲正成功!");
			return json.toJSONString();
		}
		
		//2.拼接请求内容
		String qrNo = String.valueOf(list.get(0).get("QRCODE"));
		String orderTime =String.valueOf(list.get(0).get("TIME_END"));
		String merID =String.valueOf(list.get(0).get("BANKMID"));

		Map<String, String> contentData = new HashMap<String, String>();
		contentData.put("version", "1.0.0");
		contentData.put("reqType", "0320000903");
		contentData.put("qrNo", qrNo);
		contentData.put("acqCode", acqCode);  //这里使用光大银行机构号
		contentData.put("orderNo", orderid);
		contentData.put("orderTime", orderTime);
		contentData.put("merId", merID); //商户代码
		
		//3.发送请求
		String requestUrl = SDKConfig.getConfig().getQrcB2cMerBackTransUrl();
		Map<String, String> respMap = sendMsg(contentData, requestUrl);
		// 3.1解析返回内容,更新数据库
		
		if (respMap != null) {
			String respCode = respMap.get("respCode");
			String respMsg = respMap.get("respMsg");
			if (("00").equals(respCode)) {
				logger.info("订单号{}冲正成功", orderid);
				String updateSql = "update pg_wechat_txn t set t.lmdate=sysdate,t.respcode=?,"
						+ " t.respmsg=?,status=9 where t.mer_orderid=? ";
				dao.update(updateSql, respCode, respMsg, orderid);
				
				json.put("errcode", "S");
				json.put("rtmsg", "冲正成功!");
				return json.toJSONString();
			} else {
				String updateSql = "update pg_wechat_txn t set t.lmdate=sysdate,t.respcode=?,"
						+ " t.respmsg=? where t.mer_orderid=? ";
				dao.update(updateSql, respCode, respMsg, orderid);

				json.put("errcode", "E");
				json.put("rtmsg", "冲正失败："+respMsg);
				return json.toJSONString();
			}
		}

		json.put("errcode", "E");
		json.put("rtmsg", "冲正异常，请重新冲正！");

		return json.toJSONString();
	}
	
	/**
	 * 校验日期是否属于当天
	 * @param date
	 * @return
	 */
	private boolean compareZero(Date date){
		Calendar now=Calendar.getInstance();
		now.set(Calendar.HOUR_OF_DAY, 0);
		now.set(Calendar.MINUTE, 0);
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);
		if(date.compareTo(now.getTime())>=0){
			return true;
		}else {
			return false;
		}
	}

	public boolean querCupsMidAmtLimit(String unno, String mid, BigDecimal amount) {
		
		String querySql="select count(1) limitCount from pg_wechat_txn s where "
				+ " s.cdate between trunc(sysdate) and trunc(sysdate + 1)"
				+ " and (sysdate-s.cdate)*24*60<10"
				+ " and s.txnamt=? and s.mer_id=? and s.unno =?"
				+ " and s.fiid = 18";
		try {
			List<Map<String, Object>> list =dao.queryForList(querySql, amount,mid,unno);
			Integer count=Integer.parseInt(list.get(0).get("LIMITCOUNT")+"");
			if(count>0){
				return false;
			}
			return true;
		} catch (Exception e) {
			logger.info("查询银联二维码金额限制异常："+e);
			return false;
		}
	}
	
}
