package com.hrtpayment.xpay.quickpay.cups.service;

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

import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.quickpay.common.service.QuickpayService;
import com.hrtpayment.xpay.quickpay.cups.util.Base64;
import com.hrtpayment.xpay.quickpay.cups.util.HttpClientUtil;
import com.hrtpayment.xpay.quickpay.cups.util.MapUtil;
import com.hrtpayment.xpay.quickpay.cups.util.QuickPayUtil;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.DateUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
/**
 * 北京银联快捷支付
 * @author xuxiaoxiao
 *
 */
@Service
public class CupsQuickPayService {

	private final Logger logger = LogManager.getLogger();
	
	@Value("${quick.payurl}")
	private String payUrl;
	
	@Value("${quick.presign_url}")
	private String presignUrl;
	
	@Value("${quick.sign_url}")
	private String signUrl;
	
	@Value("${quick.cancelsign_url}")
	private String cancelSignUrl;
	
	@Value("${quick.query_url}")
	private String queryUrl;
	
	@Value("${quick.refund_url}")
	private String refundUrl;
	
	@Value("${quick.key}")
	private String key;
	
	@Value("${quick.merid}")
	private String merchantId;
	
	@Value("${quick.ccbkey}")
	private String ccbKey;
	
	@Value("${quick.merccbid}")
	private String merchantCcbId;
	
	@Value("${quick.notify}")
	private String notify;
	
	@Autowired
	JdbcDao dao;
	
	@Autowired
	ManageService manageService;
	
	@Autowired
	QuickpayService quickpayService;
	
	@Autowired
	NotifyService sendNotify;

	/**
	 * 快捷支付
	 * @return 
	 */
	public String pay(String orderid,String amt,String mobileNum,String bankPidName,String bankNum,
			String bankType,String idcardType,String idCardNum,
			String mid,String mer_tid,String unno,boolean isCcb,String isPoint,String qpcid)throws BusinessException {
		checkRepeatOrderid(orderid);
		String merId="";
		String keys="";
		
		if (isCcb==true ) {
			merId= merchantCcbId;
			keys=ccbKey;
		}else{
			merId= merchantId;
			keys=key;
		}
		// 1 首先判断是否是贷记卡，
		// 2 建设银行贷记卡  
				// 判断是否第一次交易   是  ---预签约----签约----交易
		// 非建设银行贷记卡
//		checkQuickPayWay(mobileNum,bankPidName,bankNum,idCardNum);
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("application", "SubmitOrderAndPay");
			map.put("merchantId", merId);
			map.put("merchantOrderId",orderid);
			String orderTime=DateUtil.getStringFromDate(new Date(), "yyyyMMddHHmmss");
			map.put("merchantOrderTime", orderTime);
			BigDecimal bd2 = new BigDecimal(amt);
			map.put("merchantOrderAmt", String.valueOf(bd2.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));
			map.put("gwType", "04");
			map.put("backUrl", notify);
			map.put("mobileNum", mobileNum);
			map.put("idCardType", idcardType);
			map.put("idCardNum", idCardNum);
			map.put("bankType", bankType);
			map.put("bankNum", bankNum);
			map.put("bankPidName", bankPidName);
			map.put("cvn2", QuickPayUtil.getCvn2("419", keys));
			map.put("panDate", QuickPayUtil.getPanDate("0816", keys));
			map.put("signature", QuickPayUtil.getSignature(map, keys));
			String postXml = MapUtil.map2String(map);
			logger.info("快捷支付请求报文："+postXml);
			postXml = Base64.encode(postXml);
			String resp = HttpClientUtil.httpPost(payUrl, postXml);
			logger.info("快捷支付响应报文："+Base64.decode(resp));
			Map<String, String> resMap =MapUtil.string2Map(Base64.decode(resp));
			
			String respCode =resMap.get("respCode");
			String respDesc =resMap.get("respDesc");
			String status =resMap.get("orderPayStatus");
			String orderPayDesc =resMap.get("orderPayDesc");
			String flag="0";
			String resStatus="E";
			String dbBankNo=bankNum.substring(0, 6)+"******"+bankNum.substring(bankNum.length()-4,bankNum.length());
			if("1111".equals(respCode)&& ("3".equals(status)||"1".equals(status))){
				// 通信成功
				flag="0";
				resStatus="R";
				String sql = "insert into pg_wechat_txn (pwid,fiid, txntype, "
						+ "mer_orderid, detail, txnamt, mer_id, bankmid,  "
						+ "status, cdate, lmdate ,unno,mer_tid,qrcode,respcode,respmsg,trantype,hybType,hybRate,accno,time_end,ispoint,isscode) values "
						+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,sysdate,sysdate,?,?,?,?,?,?,?,?,?,?,?,?)";
				dao.update(sql, 40, orderid, "快捷支付", amt, mid, merId, flag ,unno, mer_tid, "", respCode, respDesc,
						8,"","",dbBankNo,orderTime,isPoint,qpcid);
				return resStatus;
			}else if("6002".equals(respCode)){
				// 通信超时判定为处理中
				flag="0";
				resStatus="R";
				String sql = "insert into pg_wechat_txn (pwid,fiid, txntype, "
						+ "mer_orderid, detail, txnamt, mer_id, bankmid,  "
						+ "status, cdate, lmdate ,unno,mer_tid,qrcode,respcode,respmsg,trantype,hybType,hybRate,accno,time_end,ispoint,isscode) values "
						+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,sysdate,sysdate,?,?,?,?,?,?,?,?,?,?,?,?)";
				dao.update(sql, 40, orderid, "快捷支付", amt, mid, merId, flag ,unno, mer_tid, "", "USERPAYING", respDesc,
						8,"","",dbBankNo,orderTime,isPoint,qpcid);
				return resStatus;
			}else{
				flag="6";
				String sql = "insert into pg_wechat_txn (pwid,fiid, txntype, "
						+ "mer_orderid, detail, txnamt, mer_id, bankmid,  "
						+ "status, cdate, lmdate ,unno,mer_tid,qrcode,respcode,respmsg,trantype,hybType,hybRate,accno,time_end,ispoint,isscode) values "
						+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,sysdate,sysdate,?,?,?,?,?,?,?,?,?,?,?,?)";
				dao.update(sql, 40, orderid, "快捷支付", amt, mid,merId, flag ,unno, mer_tid, "", respCode, respDesc,
						8,"","",dbBankNo,orderTime,isPoint,qpcid);
				RedisUtil.addFailCountByRedis(1);
				throw new BusinessException(8000,respDesc);
			}
		} catch (Exception e) {
			throw new BusinessException(8000,e.getMessage());
		}
	}
	
	/**
	 * 快捷支付查询
	 * @param orderid
	 */
	public String  queryOrder(Map<String,Object > orderInfo)throws BusinessException{
		try {
			Map<String, String> map = new HashMap<String, String>();
			String orderid=orderInfo.get("mer_orderid").toString();
			String merId=String.valueOf(orderInfo.get("bankmid"));
			String keys="";
			if (merchantCcbId.equals(merId)) {
				keys=ccbKey;
			}else if (merchantId.equals(merId)){
				keys=key;
			}else{
				logger.info("[北京银联]订单：{}查询交易失败，失败原因：获取银行商户号{}及key{}异常", orderid, merId, keys);
				throw new BusinessException(8000, "交易查询失败");
			}
			map.put("application", "GetOrderInfo");
			map.put("merchantId",merId);
			map.put("merchantOrderId", orderid);
			map.put("merchantOrderTime", orderInfo.get("time_end").toString());
			map.put("gwType", "04");
//			map.put("bpSerialNum", "990000000028346");	//不是必须上送
			// 签名
			map.put("signature", QuickPayUtil.getSignature(map, keys));
			// 组装请求参数
			String postStr = MapUtil.map2String(map);
			logger.info("查询请求明文：" + postStr);
			// 加密报文
			postStr = Base64.encode(postStr);
			// 发送请求
			String resp = HttpClientUtil.httpPost(queryUrl, postStr);
			logger.info("响应后明文：" + Base64.decode(resp));
			Map<String, String> resMap =MapUtil.string2Map(Base64.decode(resp));
			String respCode =resMap.get("respCode");
			String status =resMap.get("orderStatus");
			String orderDesc =resMap.get("orderDesc");
			String bkOrderid =resMap.get("bpSerialNum");
			String transType=resMap.get("transType");
			String flag="0";
			String resStatus="R";
			if("1111".equals(respCode)){
				if("1".equals(status)){
					// 支付成功
					flag="1";
					resStatus="S";
					respCode="0000";
				}else if("3".equals(status)||"0".equals(status)){
					flag="0";
					resStatus="R";
				}else{
					flag="6";
					resStatus="E";
				}
			}else{
				orderDesc=resMap.get("respDesc");
				resStatus="E";
			}
			String updateSql="update pg_wechat_txn t set t.status=?,t.respcode=?,"
							+ " t.respmsg=?,t.lmdate=sysdate,t.bk_orderid=? where t.status<>'1'"
							+ " and t.mer_orderid=? ";
			int count =dao.update(updateSql, flag,respCode,orderDesc,bkOrderid,orderid);
			if("1".equals(status)&&count==1&&"01".equals(transType)){
				// 累增快捷支付成功次数
				manageService.addQuickPayDayCount(orderid, null, "0");
			    quickpayService.addDayLimit(String.valueOf(orderInfo.get("mer_id")), String.valueOf(orderInfo.get("txnamt")));
			    BigDecimal bDecimal=new BigDecimal(orderInfo.get("TXNLEVEL")==null?"0":String.valueOf(orderInfo.get("TXNLEVEL")));
				BigDecimal txnLevel=orderInfo.get("TXNLEVEL")==null||"".equals(orderInfo.get("TXNLEVEL"))?BigDecimal.ONE:bDecimal ;
				orderInfo.put("TXNLEVEL", txnLevel );
				sendNotify.sendNotify(orderInfo);
			}
			return resStatus;
		} catch (Exception e) {
			logger.error("快捷支付查询异常:",e);
			throw new BusinessException(8000, e.getMessage());
		}
	}
	
	
	
	/**
	 * 快捷支付退款查询
	 * @param orderid
	 */
	public String  refundQueryOrder(Map<String,Object > orderInfo)throws BusinessException{
		try {
			Map<String, String> map = new HashMap<String, String>();
			String orderid=orderInfo.get("mer_orderid").toString();
			String merId=String.valueOf(orderInfo.get("bankmid"));
			String keys="";
			if (merchantCcbId.equals(merId)) {
				keys=ccbKey;
			}else if (merchantId.equals(merId)){
				keys=key;
			}else{
				logger.info("[北京银联]订单：{}退款查询交易失败，失败原因：获取银行商户号{}及key{}异常", orderid, merId, keys);
				throw new BusinessException(8000, "退款交易查询失败");
			}
			map.put("application", "GetOrderInfo");
			map.put("merchantId",merId);
			map.put("merchantOrderId", orderid);
			map.put("merchantOrderTime", orderInfo.get("time_end").toString());
			map.put("gwType", "04");
			map.put("transType", "02");
//			map.put("bpSerialNum", "990000000028346");	//不是必须上送
			// 签名
			map.put("signature", QuickPayUtil.getSignature(map, keys));
			// 组装请求参数
			String postStr = MapUtil.map2String(map);
			logger.info("退款查询请求明文：" + postStr);
			// 加密报文
			postStr = Base64.encode(postStr);
			// 发送请求
			String resp = HttpClientUtil.httpPost(queryUrl, postStr);
			logger.info("退款查询响应明文：" + Base64.decode(resp));
			Map<String, String> resMap =MapUtil.string2Map(Base64.decode(resp));
			String respCode =resMap.get("respCode");
			String status =resMap.get("orderStatus");
			String orderDesc =resMap.get("orderDesc");
			String bkOrderid =resMap.get("bpSerialNum");
			String flag="0";
			String resStatus="R";
			if("1111".equals(respCode)){
				if("1".equals(status)){
					// 支付成功
					flag="1";
					resStatus="S";
					respCode="0000";
				}else if("3".equals(status)||"0".equals(status)){
					flag="0";
					resStatus="R";
				}else{
					flag="6";
					resStatus="E";
				}
			}else{
				orderDesc=resMap.get("respDesc");
				resStatus="E";
			}
			String updateSql="update pg_wechat_txn t set t.status=?,t.respcode=?,"
							+ " t.respmsg=?,t.lmdate=sysdate,t.bk_orderid=? where t.status<>'1'"
							+ " and t.mer_orderid=? ";
			int count =dao.update(updateSql, flag,respCode,orderDesc,bkOrderid,orderid);
			return resStatus;
		} catch (Exception e) {
			logger.error("快捷支付退款查询异常:",e);
			throw new BusinessException(8000, e.getMessage());
		}
	}
	
	/**
	 * 快捷支付预签约
	 */
	@Deprecated
	public boolean preSign(String accName,String idNo,String mobileNo,String cardNo){
		Map<String, String> map = new HashMap<String, String>();
		map.put("application", "PreSign");
		map.put("merchantId", merchantCcbId);
		map.put("custName", accName);		//陈国承
		map.put("identityType","01");	//01：身份证
		map.put("identityNo", idNo); //320381198909230074
		map.put("mobileNo", mobileNo);		//18301410756
		map.put("cardType", "02");
		map.put("cardNo", cardNo);	//6217000010097051976
		// 签名
		map.put("signature", HttpClientUtil.getSignature(map,ccbKey));
		// 组装请求参数
		String postStr = MapUtil.map2String(map);
		logger.info("预签约请求明文：" + postStr);
		// 加密报文
		postStr = Base64.encode(postStr);
		// 发送请求
		String resp = null;
		try {
			resp = HttpClientUtil.httpPost(presignUrl, postStr);
			logger.info("预签约响应明文：" + Base64.decode(resp));
			Map<String, String> resMap =MapUtil.string2Map(Base64.decode(resp));
			String respCode =resMap.get("respCode");
			if("0000".equals(respCode)){
				return true;
			}
		} catch (Exception e) {
			logger.error("预签约异常",e);
		}
		RedisUtil.addFailCountByRedis(1);
		return false;
	}
	
	/**
	 * 快捷支付签约
	 */
	@Deprecated
	public boolean sign(String accName,String idNo,String mobileNo,String cardNo,String smsCode){
		Map<String, String> map = new HashMap<String, String>();
		map.put("application", "Sign");
		map.put("merchantId", merchantCcbId);
		map.put("custName", accName);
		map.put("identityType","01");	//01：身份证
		map.put("identityNo", idNo);
		map.put("mobileNo", mobileNo);
		map.put("cardType", "02");
		map.put("cardNo", cardNo);
		map.put("smsCode", smsCode);
		// 签名
		map.put("signature", HttpClientUtil.getSignature(map,ccbKey));
		// 组装请求参数
		String postStr = MapUtil.map2String(map);
		logger.info("签约请求明文：" + postStr);
		// 加密报文
		postStr = Base64.encode(postStr);
		// 发送请求
		String resp = null;
		try {
			resp = HttpClientUtil.httpPost(signUrl, postStr);
			logger.info("签约响应明文：" + Base64.decode(resp));
			Map<String, String> resMap =MapUtil.string2Map(Base64.decode(resp));
			String respCode =resMap.get("respCode");
			String respDesc =resMap.get("respDesc");
			if("0000".equals(respCode)){
				return true;
			}
		} catch (Exception e) {
			logger.error("签约异常",e);
		}
		RedisUtil.addFailCountByRedis(1);
		return false;
	}
	
	
	/**
	 * 快捷支付取消签约
	 */
	@Deprecated
	public boolean cancelSign(String accName,String idNo,String mobileNo,String cardNo){
		Map<String, String> map = new HashMap<String, String>();
		map.put("application", "CancelSign");
		map.put("merchantId", merchantCcbId);
		map.put("bankPIDName", accName);
		map.put("idCardType","01");	//01：身份证
		map.put("idCardNum", idNo);
		map.put("cardNo", cardNo);
		// 签名
		map.put("signature", HttpClientUtil.getSignature(map,ccbKey));
		// 组装请求参数
		String postStr = MapUtil.map2String(map);
		logger.info("签约请求明文：" + postStr);
		// 加密报文
		postStr = Base64.encode(postStr);
		// 发送请求
		String resp = null;
		try {
			resp = HttpClientUtil.httpPost(cancelSignUrl, postStr);
			logger.info("取消签约响应明文：" + Base64.decode(resp));
			Map<String, String> resMap =MapUtil.string2Map(Base64.decode(resp));
			String respCode =resMap.get("respCode");
			String respDesc =resMap.get("respDesc");
			if("0000".equals(respCode)){
				return true;
			}
		} catch (Exception e) {
			logger.error("取消签约异常",e);
		}
		RedisUtil.addFailCountByRedis(1);
		return false;
	}
		
	/**
	 * 快捷支付退款
	 */
	public String refund(String orderid, BigDecimal amount, Map<String, Object> oriMap){
		try {
			String rtnStatus="E";
			Map<String, String> map = new HashMap<String, String>();
			String merId=String.valueOf(oriMap.get("bankmid"));
			String keys="";
			if (merchantCcbId.equals(merId)) {
				keys=ccbKey;
			}else if (merchantId.equals(merId)){
				keys=key;
			}else{
				logger.info("[北京银联]订单：{}查询交易失败，失败原因：获取银行商户号{}及key{}异常", orderid, merId, keys);
				throw new BusinessException(8000, "交易查询失败");
			}
			map.put("application", "Refund");
			map.put("merchantId", merId);
			map.put("merchantOrderId", orderid);	//退款订单号不能与支付的订单号相同
			String orderTime=DateUtil.getStringFromDate(new Date(), "yyyyMMddHHmmss");
			map.put("merchantOrderTime", orderTime);	//退款时间不能与支付的订单时间相同
			map.put("merchantOrderAmt", String.valueOf(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));
			
			String bk_orderid=oriMap.get("BK_ORDERID").toString();
			map.put("orgBpSerialNum", bk_orderid);
			map.put("frontUrl", "");
			map.put("backUrl", "");
			map.put("gwType", "04");
			map.put("clientIp", "127.1.1.1");
			// 签名
			map.put("signature", HttpClientUtil.getSignature(map,keys));
			// 组装请求参数
			String postStr = MapUtil.map2String(map);
			logger.info("退款请求明文：" + postStr);
			// 加密报文
			postStr = Base64.encode(postStr);
			// 发送请求
			String resp = null;
			resp = HttpClientUtil.httpPost(refundUrl, postStr);
			logger.info("退款响应明文：" + Base64.decode(resp));
			Map<String, String> resMap =MapUtil.string2Map(Base64.decode(resp));
			String respCode =resMap.get("respCode");
			String respDesc =resMap.get("respDesc");
			String status="A";
			String orderRefundStatus =resMap.get("orderRefundStatus");
			String orderRefundDesc =resMap.get("orderRefundDesc");
			String transTime=resMap.get("transTime");
			String bpSerialNum=resMap.get("bpSerialNum");
			if("1111".equals(respCode)){
				if("1".equals(orderRefundStatus)){
					rtnStatus="S";
					status="1";
				}else if("3".equals(orderRefundStatus)){
					rtnStatus="R";
					status="0";
				}else{
					rtnStatus="E";
					status="A";
				}
				respDesc=orderRefundDesc;

			}else{
				rtnStatus="E";
			}
			String updateSql="update pg_wechat_txn t set t.status=?,t.respcode=?,"
					+ " t.respmsg=?,t.lmdate=sysdate,t.time_end=?,t.bk_orderid=? "
					+ " where t.status<>'1' and t.mer_orderid=? ";
			dao.update(updateSql, status,respCode,respDesc,orderTime,bpSerialNum,orderid);
			
			return rtnStatus;
		} catch (Exception e) {
			logger.error("北京银联快捷支付退款异常:",e);
		}
		return "E";

	}
	
	
	
	/**
	 * 检测订单号是否重复
	 * 
	 * @param orderid
	 * @throws BusinessException
	 */
	private void checkRepeatOrderid(String orderid) throws BusinessException {
		List<Map<String, Object>> list = dao
				.queryForList("select * from pg_wechat_txn where mer_orderid=? and rownum=1", orderid);
		if (list.size() > 0)
			throw new BusinessException(9007, "订单号重复");
	}
	
	/**
	 * 检测是否贷记卡 
	 * 是否建设银行贷记卡  是否首次交易
	 * @param mobileNum
	 * @param bankPidName
	 * @param bankNum
	 * @param idCardNum
	 */
	@SuppressWarnings("unused")
	private void checkQuickPayWay(String mobileNum,String bankPidName,String bankNum,
			String idCardNum)throws BusinessException{
		String querySql="select t.institutions,t.cardtype from cardtable t "
						+ " where t.cardand like ? ";
		logger.info("------------------>"+bankNum.substring(0,6));
		List<Map<String, Object>> list = dao.queryForList(querySql, bankNum.substring(0,6)+"%");
		if(list.size()!=0){
			Map<String, Object> data=list.get(0);
			String bankName=data.get("INSTITUTIONS").toString();
			String cardType=data.get("CARDTYPE").toString();
			// 判断是否贷记卡
			if(!"2".equals(cardType)){
				throw new BusinessException(8000, "请使用贷记卡进行交易！");
			}
			// 判断是否是建设银行贷记卡
			if(bankName.contains("建设银行")){
				// 判断是否做过签约
				String signSql="select * from hrt_qkpaysign t where t.status=2 and t.cardno=?";
				List<Map<String, Object>> signList = dao.queryForList(signSql,bankNum );
				if(signList.size()==0){
					// 进行预签约  签约
					boolean preSignFlag=preSign(bankPidName,idCardNum,mobileNum,bankNum);
					if(!preSignFlag){
						throw new BusinessException(8000, "预签约失败，请核实！");
					}
					boolean singFlag =sign(bankPidName,idCardNum,mobileNum,bankNum,"");
					if(!singFlag){
						throw new BusinessException(8000, "签约失败，请核实！");
					}
				}
			}
		}else{
			throw new BusinessException(8000, "请检查卡号是否合规！");
		}
	}

	/**
	 * 北京银联快捷支付异步通知
	 * @param formStr
	 */
	public void updateStatusByNotify(String formStr) {
		try {
			logger.info("异步通知明文：" + Base64.decode(formStr));
			Map<String, String> resMap =MapUtil.string2Map(Base64.decode(formStr));
			String respCode =resMap.get("respCode");
			String status =resMap.get("orderPayCode");
			String orderDesc =resMap.get("orderPayDesc");
			String bkOrderid =resMap.get("bpSerialNum");
			String orderid =resMap.get("merchantOrderId");
			String transTime =resMap.get("transTime");

			if("1111".equals(respCode)&&"1".equals(status)){
				String bankNum=resMap.get("bankNum");
				// 支付成功
				respCode="0000";
				String sql="select pwid, mer_id,txnamt,status,unno,mer_orderid,mer_tid from pg_wechat_txn where  mer_orderid=? ";
				List<Map<String, Object>> orderInfo =dao.queryForList(sql, orderid);
				if (orderInfo.size()==0) {
					logger.info("[北京银联快捷支付]  订单 {}不存在",orderid);
				}else if("1".equals(String.valueOf(orderInfo.get(0).get("STATUS")))){
					logger.info("[北京银联快捷支付]  订单 {}无需更新状态",orderid);
				}else{
					// 只有 成功 才会通知,所以不用判断状态
				    sql ="update pg_wechat_txn t set t.status=?,t.respcode=?,"
							+ " t.respmsg=?,t.lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate),t.bk_orderid=? where t.status<>'1'"
							+ " and t.mer_orderid=? ";
		        	int count=dao.update(sql, "1",respCode,orderDesc,transTime,bkOrderid,orderid);
					if(count==1){
						manageService.addQuickPayDayCount(orderid, bankNum, "1");
					    quickpayService.addDayLimit(String.valueOf(orderInfo.get(0).get("mer_id")), String.valueOf(orderInfo.get(0).get("txnamt")));
					    BigDecimal bDecimal=new BigDecimal(orderInfo.get(0).get("TXNLEVEL")==null?"0":String.valueOf(orderInfo.get(0).get("TXNLEVEL")));
					    BigDecimal txnLevel=orderInfo.get(0).get("TXNLEVEL")==null||"".equals(orderInfo.get(0).get("TXNLEVEL"))?BigDecimal.ONE:bDecimal ;
					    orderInfo.get(0).put("TXNLEVEL", txnLevel );
					    sendNotify.sendNotify(orderInfo.get(0)); 
					    logger.info("[北京银行]订单异步通知更新记录{}，订单编号{}",count,orderid);
					}else{
						logger.info("[北京银联快捷支付]  订单 {}无需更新状态",orderid);
					}
					
				} 
			}
		} catch (Exception e) {
			logger.error("异步通知处理异常",e);
		}

	}
}
