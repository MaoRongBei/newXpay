package com.hrtpayment.xpay.baidu.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.baidu.bean.BaiduCalbackBean;
import com.hrtpayment.xpay.baidu.bean.BaiduRefundCallBackBaen;
import com.hrtpayment.xpay.baidu.bean.BaiduRefundQueryRetBean;
import com.hrtpayment.xpay.baidu.bean.BaiduRefundRetMsgBean;
import com.hrtpayment.xpay.baidu.util.HttpRequester;
import com.hrtpayment.xpay.baidu.util.HttpRespons;
import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;

/**
 * @author lvjianyu
 * 百度钱包直连-2017/07/20
 */
@Service("BaiduPayService")
public class BaiduPayService {

	private final Logger logger = LogManager.getLogger();

	@Autowired
	private JdbcDao dao;

	@Autowired
	private BaiduService service;

	@Value("${baidu.barPayUrl}")
	private String barPayUrl;

	@Value("${baidu.QrCodeUrl}")
	private String qrCodeUrl;

	@Value("${baidu.queryOrderUrl}")
	private String queryOrderUrl;

	@Value("${baidu.refundUrl}")
	private String refundUrl;

	@Value("${baidu.refundQueryUrl}")
	private String refundQueryUrl;

	@Value("${baidu.key}")
	private String key;

	@Autowired
	private NotifyService notify;

	//百度同步返回支付状态错误码
	private static String BUSINESS_ERROR_CODE ="|65215|65235|65236|69441|69506|69510|69511|69515|69552|69557|";
	
	/**
	 * 百度钱包-条码支付
	 * @return
	 * @throws BusinessException
	 */
	public String barCodePay(String unno, String mid, int fiid, String payway, String orderid, String merchantCode,
			String scene, String authCode, BigDecimal totalAmount, String subject, String tid)
			throws BusinessException {
		try {
			// 1.验证该订单号是否存在
			List<Map<String, Object>> list = dao.queryForList("select pwid from pg_wechat_txn t where t.mer_orderid=?",
					orderid);
			if (list.size() > 0) {
				logger.error("[百度条码]下单失败，订单号已经存在，订单号：{}",orderid);
				throw new BusinessException(8000, "订单号已存在！");
			}
			// 2.插入新记录
			String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid, detail, txnamt, mer_id,bankmid,"
					+ "status, cdate, lmdate,unno,mer_tid,trantype) values((S_PG_Wechat_Txn.nextval),?,'0',?,?,?,?,?,'0',sysdate,sysdate,?,?,'5')";
			dao.update(insertSql, fiid, orderid, subject, totalAmount, mid, merchantCode, unno, tid);

			// 3.获取发送报文、发送、接收返回报文
			Map<String, String> reqMap = service.getBarCPReqMessage(orderid, authCode, totalAmount, subject,
					merchantCode);
			logger.info("[百度条码]发送请求报文:" + reqMap);
			HttpRequester reqUtil = new HttpRequester();
			HttpRespons response = null;
			try{
				response=reqUtil.sendGet(barPayUrl, reqMap);
			}catch(Exception e){
				logger.info("[百度直连]条码支付：{} 网络异常",orderid);
				RedisUtil.addFailCountByRedis(1);
				return "R";
			}
			
			String resp = response.getContent();

			JSONObject json = JSONObject.parseObject(resp);
			logger.info("[百度条码]返回报文:{},msg:{}" , resp,json.get("msg"));
			String rtnCode = json.getString("ret");
			String rtnMsg = json.getString("msg");
			String bk_orderid = "";
			String status = "0";
			String retStatus = "E";
			
			if ("0".equals(rtnCode)) {// 成功
				JSONObject content= json.getJSONObject("content");
				bk_orderid = content.getString("bfb_order_no");
				String payResult =content.getString("pay_result");
//				1	等待支付2	付款成功5	交易取消10	支付失败
				switch (payResult) {
				case "1":
					retStatus = "R";
					break;
				case "2":
					retStatus = "S";
					status = "1";
					break;
				case "5":
				case "10":
				default:
					retStatus = "E";
					break;
				}
			} else if ("69556".equals(rtnCode)) {// 等待输入密码
				retStatus = "R";
			} else if (BUSINESS_ERROR_CODE.indexOf(rtnCode)>-1){
				retStatus = "E";
				status = "6";
				RedisUtil.addFailCountByRedis(1);
			}
			String sql = "update pg_wechat_txn set status=?,respcode=?,respmsg=?,txnlevel=1,"
					+ "lmdate=sysdate,bk_orderid=? where status='0' and mer_orderid=?";
			dao.update(sql, status, rtnCode, rtnMsg,bk_orderid, orderid);
			return retStatus;
		}catch(BusinessException e){
			logger.error("[百度条码]下单失败，订单号：{},{}", orderid, e.getMessage());
			throw new BusinessException(8000, e.getMessage());
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[百度条码]下单失败，订单号：{},{}", orderid, e.getMessage());
			throw new BusinessException(8000, e.getMessage());
		}
	}

	/**
	 * 获取百度钱包二维码连接
	 * 
	 * @param unno
	 * @param mid
	 * @param merchantCode
	 * @param subject
	 * @param amount
	 * @param fiid
	 * @param orderid
	 * @param tid
	 * @return
	 * @throws BusinessException
	 */
	public String getQrCode(String unno, String mid, String merchantCode, String subject, BigDecimal amount, int fiid,
			String orderid, String tid,String hybRate,String hybType) throws BusinessException {
		try {
			// 1.校验是否重复订单号
			List<Map<String, Object>> list = dao
					.queryForList("select pwid from pg_wechat_txn where mer_orderid=? and rownum=1", orderid);
			if (list.size() > 0) {
				logger.error("[百度二维码]支付订单号重复{}", orderid);
				throw new BusinessException(9007, "订单号重复");
			}

			// 2.获取二维码
			Map<String, String> reqMap = service.getQrReqMessage(subject, amount, orderid, merchantCode);
			logger.info("[百度二维码]获取二维码请求参数：" + reqMap);
			HttpRequester reqUtil = new HttpRequester();
			HttpRespons response = reqUtil.sendGet(qrCodeUrl, reqMap);
			String resp = response.getContent();
			logger.info("[百度二维码]返回报文:" + resp );
			JSONObject json = JSONObject.parseObject(resp);

			// 3.插入数据库 保存订单
			String qrCode = json.getString("content");
			if (!StringUtils.isEmpty(qrCode)) {
				String sql = "insert into pg_wechat_txn (pwid,fiid, txntype, "
						+ "mer_orderid, detail, txnamt, mer_id, bankmid, respcode, respmsg, "
						+ "status, cdate, lmdate,qrcode,unno,mer_tid,trantype,hybType,hybRate) values"
						+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,?,'0',sysdate,sysdate,?,?,?,'5',?,?)";
				dao.update(sql, fiid, orderid, subject, amount, mid, merchantCode, json.get("ret"), json.get("msg"),
						qrCode, unno, tid,hybType,hybRate);
				return qrCode;
			} else {
				RedisUtil.addFailCountByRedis(1);
				logger.error("[百度二维码]交易失败:" +  json.get("msg"));
				throw new BusinessException(8000, "交易失败");
			}
		}catch(BusinessException e){
			throw new BusinessException(e.getCode(), e.getMessage());
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[百度二维码]交易失败:" ,e.getMessage());
			throw new BusinessException(8000, e.getMessage());
		}
	}
	
	/** 
	 * 百度直连  pos专用
	 * @param unno
	 * @param mid
	 * @param merchantCode
	 * @param subject
	 * @param amount
	 * @param fiid
	 * @param orderid
	 * @param tid
	 * @return
	 * @throws BusinessException
	 */
	public String posGetQrCode(String unno, String mid, String merchantCode, String subject, BigDecimal amount, int fiid,
			String orderid, String tid, String txnType) throws BusinessException {
		try {
			// 1.获取二维码
			Map<String, String> reqMap = service.getQrReqMessage(subject, amount, orderid, merchantCode);
			logger.info("[百度二维码]获取二维码请求参数：" + reqMap);
			HttpRequester reqUtil = new HttpRequester();
			HttpRespons response = reqUtil.sendGet(qrCodeUrl, reqMap);
			String resp = response.getContent();
			logger.info("[百度二维码]返回报文:" + resp );
			JSONObject json = JSONObject.parseObject(resp);

			// 3.插入数据库 保存订单
			String qrCode = json.getString("content");
			if (!StringUtils.isEmpty(qrCode)) {
				String sql = "update pg_wechat_txn t set t.fiid=?,t.qrcode=?,"
						+ " t.status=?,t.bankmid=?,t.respcode=?,t.respmsg=?,"
						+ " t.lmdate=sysdate,t.trantype=5 where status='A' and t.mer_orderid=? ";
				dao.update(sql,fiid,qrCode,"0",merchantCode,json.get("ret"), json.get("msg"),orderid);
				return qrCode;
			} else {
				RedisUtil.addFailCountByRedis(1);
				logger.error("[百度二维码]交易失败:" +  json.get("msg"));
				throw new BusinessException(8000, "交易失败");
			}
		}catch(BusinessException e){
			throw new BusinessException(e.getCode(), e.getMessage());
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[百度二维码]交易失败:" ,e.getMessage());
			throw new BusinessException(8000, e.getMessage());
		}
	}
	
	

	/**
	 * 百度钱包回调地址
	 * 
	 * @param formStr
	 * @throws BusinessException
	 */
	public String payCallBack(BaiduCalbackBean bean) throws BusinessException {
		// 1. 验签
		boolean verifySuccess = service.verifyCallback(bean.getSignMap());
		if(!verifySuccess){
			return renderHtml(bean.getOrder_no(), verifySuccess);
		}
		// 2. 查询原订单
		String orderId = bean.getOrder_no();
		String querySql = "select * from pg_wechat_txn t where t.mer_orderid=?";
		List<Map<String, Object>> list = dao.queryForList(querySql, orderId);
		if (list.size() < 1) {
			logger.info("[百度钱包]支付异步通知,未查询到订单号为{}的订单", orderId);
			throw new BusinessException(9008, "订单不存在");
		} else if (list.size() == 1) {
			BigDecimal bDecimal = new BigDecimal(
					list.get(0).get("TXNLEVEL") == null ? "0" : String.valueOf(list.get(0).get("TXNLEVEL")));
			String status = String.valueOf(list.get(0).get("STATUS"));
			if ("1".equals(status)) {
				logger.info("原订单{}状态:{},未更新", orderId, status);
			} else {
				String retCode = bean.getPay_result();
				String retMsg = "";
				String payTime = bean.getPay_time();
				String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,lmdate=nvl(to_date(?,'yyyymmddhh24miss'),sysdate) ,txnLevel=?,time_end=?,bk_orderid=? ";
				// 1支付成功 2等待支付
				if ("1".equals(retCode)) {
					updateSql = updateSql + " ,status=1";
					retMsg = "支付成功";
				} else if ("2".equals(retCode)){
					retMsg = "等待支付";
				}

				updateSql = updateSql + " where status!='1' and mer_orderid=?";
				int size= dao.update(updateSql, retCode, retMsg,payTime ,1,payTime,bean.getBfb_order_no(), orderId);
				logger.info("百度钱包支付异步通知，更新数据{},订单编号{}，返回码{}，返回信息{}",size, orderId, retCode, retMsg);
				BigDecimal txnLevel = list.get(0).get("TXNLEVEL") == null || "".equals(list.get(0).get("TXNLEVEL"))
						? BigDecimal.ONE : bDecimal;
				list.get(0).put("TXNLEVEL", txnLevel);
				if ("1".equals(retCode)&&size==1) {
					notify.sendNotify(list.get(0));
				}
			}
		} else {
			// 不应该出现的重复多条记录
			logger.error("异步通知,查询到订单号为{}的订单重复记录", orderId);
		}

		// 3. 构造返回给百度的特殊标签 页面
		return renderHtml(orderId, verifySuccess);
	}

	private String renderHtml(String orderId, boolean verifySuccess) {
		StringBuffer str = new StringBuffer();
		if (verifySuccess) {
			// 百度要求 返回体中必须包含 {<meta name="VIP_BFB_PAYMENT" content="BAIFUBAO">}
			str.append("<HTML><head>");
			str.append("<meta name=\"VIP_BFB_PAYMENT\" content=\"BAIFUBAO\">");
			str.append("</head>");
			str.append("<body>");
			str.append("验签通过" + "订单号：" + orderId);
			str.append("</body></html>");
		} else {
			str.append("验签失败" + "<br/>");
		}
		return str.toString();
	}
	
	/**
	 * 查询订单状态--（条码支付与二维码支付接口参数相同，顾可复用同一接口）
	 * 
	 * @param orderid
	 * @return
	 * @throws BusinessException
	 */
	public String queryOrder(Map<String, Object> map) {
		String orderid=String.valueOf(map.get("MER_ORDERID"));
		
		// 发送查询请求
		if (map.get("BANKMID") == null){
			logger.error("[百度钱包]查询订单{}，商户号对应为空，查询失败",orderid);
			return "ERROR";
		}
		String merchantCode = map.get("BANKMID").toString();
		Map<String, String> reqMap;
		try {
			reqMap = service.getQueryReqMessage(orderid, merchantCode);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("[百度钱包]查询订单{}，获取发送信息失败",orderid);
			return "ERROR";
		}
		logger.info("获取百度钱包查单请求参数：" + reqMap);
		HttpRequester reqUtil = new HttpRequester();
		HttpRespons response;
		try {
			response = reqUtil.sendGet(queryOrderUrl, reqMap);
		} catch (IOException e) {
			logger.error("[百度钱包]查询订单{}，发送查单请求失败",orderid);
			return "ERROR";
		}
		// 解析返回数据
		String resp = response.getContent();
		logger.info("百度钱包查单返回报文：" + resp);
		JSONObject json = JSONObject.parseObject(resp);
		
		if (!"0".equals(json.getString("ret"))){
			return "ERROR";
		}
		
		// 根据返回结果 respBean 内容进行处理查询 与异步通知返回处理相同 并进行推送
		// 1 等待支付 2 支付成功 3 交易成功 10 支付失败
		String status = String.valueOf(map.get("STATUS"));
		String tradeCode="";
		if ("1".equals(status)) {
			logger.info("原订单{}状态:{},未更新", orderid, status);
			tradeCode= "SUCCESS";
		} else {
			String retCode = json.getString("ret");
			String retMsg = json.getString("msg");
			JSONObject content=json.getJSONObject("content");
			String payTime = content.getString("pay_time").startsWith("0000")?"":content.getString("pay_time");
			String payResult  = content.getString("pay_result");
			
			String  bkOrderid=content.getString("bfb_order_no");
			
			String updateSql = "update pg_wechat_txn set respcode=?, respmsg=?,bk_orderid=?,lmdate=nvl(to_date(?,'yyyy/mm/dd hh24:mi:ss'),sysdate) ,txnLevel=? ";
			//1 等待支付  2 支付成功 10 支付失败  3 交易成功(3为二维码交易查询状态)
			if ("2".equals(payResult) || "3".equals(payResult)) {
				updateSql = updateSql + " ,status=1 ";
				tradeCode= "SUCCESS";
			} else if ("10".equals(payResult)) {
				updateSql = updateSql + " ,status=6 ";
				tradeCode= "ERROR";
			} else if ("1".equals(payResult)) {
				updateSql = updateSql + " ,status=0 ";
				tradeCode= "DOING";
			}
			updateSql = updateSql + "where status <>'1' and  mer_orderid=?";
			int count=dao.update(updateSql, retCode, retMsg,bkOrderid,payTime,0, orderid);
			logger.info("百度钱包查单接口更新状态，订单号{}，更新查询回复信息{}", orderid, retMsg);
			
			//推送信息
//			String querySql = "select PWID,BANKMID from pg_wechat_txn where mer_orderid=?";
//			List<Map<String, Object>> list = dao.queryForList(querySql, orderid);
//			BigDecimal txnLevel = new BigDecimal(list.get(0).get("TXNLEVEL") == null ? "1" : String.valueOf(map.get("TXNLEVEL")));
//			list.get(0).put("TXNLEVEL", txnLevel);
			
			BigDecimal txnLevel = new BigDecimal(map.get("TXNLEVEL") == null ? "1" : String.valueOf(map.get("TXNLEVEL")));
			map.put("TXNLEVEL", txnLevel); 
			if (("2".equals(payResult) || "3".equals(payResult))&&1==count) {
				notify.sendNotify(map);
			}
		}
		return tradeCode;
	}


	/**
	 * 百度钱包-退款接口
	 * 
	 * @param orderid
	 * @return
	 * @throws BusinessException
	 */
	public String refund(String orderid, BigDecimal amount, Map<String, Object> oriMap){
		String merchantCode=String.valueOf(oriMap.get("BANKMID"));
		String oriOrderid=String.valueOf(oriMap.get("MER_ORDERID"));
		JSONObject jsonRes = new JSONObject();
		
		
		// 2.发起退款请求
		Map<String, String> reqMap = new LinkedHashMap<String, String>();
		try {
			reqMap = service.getRefundReqMsg(merchantCode, orderid, oriOrderid, amount);
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[百度钱包]-退款{}，获取发送退款信息失败",oriOrderid);
			jsonRes.put("errcode", "E");
			jsonRes.put("rtmsg", "退款失败");
		}
		// 2.1获取发送数据
		logger.info("[百度钱包]获取退款请求参数：------->" + reqMap);
		HttpRequester reqUtil = new HttpRequester();
		HttpRespons response;
		try {
			response = reqUtil.sendGet(refundUrl, reqMap);
		} catch (IOException e) {
			logger.error("[百度钱包]-退款{}，发送退款请求失败",oriOrderid);
			jsonRes.put("errcode", "E");
			jsonRes.put("rtmsg", "退款失败");
			return jsonRes.toJSONString();
		}
		// 2.2解析返回结果
		String resp = response.getContent();
		logger.info("[百度钱包]返回报文" + resp);
		BaiduRefundRetMsgBean retBean = BaiduRefundRetMsgBean.parseXmlFromStr(resp);

		// 3.更新退款表
		String status = "2";
		String errorCode = null;
		String errorCodeDes = null;
		String refundId = null;
		if ("1".equals(retBean.getRet_code())) {
			refundId = retBean.getBfb_order_no();
			jsonRes.put("errcode", "S");
			jsonRes.put("rtmsg", "退款操作成功");
		} else {
			status = "6";
			errorCode = retBean.getRet_code();
			errorCodeDes = retBean.getRet_detail();
			jsonRes.put("errcode", "E");
			jsonRes.put("rtmsg", "退款失败");
		}
		dao.update("update pg_wechat_txn t set t.status=?,lmdate=sysdate,respcode=?,respmsg=?,bk_orderid=?,trantype=5 "
				+ " where mer_orderid=? and txntype='1'", status, errorCode, errorCodeDes, refundId, orderid);
		return jsonRes.toJSONString();
	}

	/**
	 * 百度钱包-退款异步通知
	 * 
	 * @param string
	 * @param response
	 * @return
	 * @throws BusinessException
	 */
	public String refundCallBack(BaiduRefundCallBackBaen refundback, String queryStr) throws BusinessException {
		// 1.验签
		boolean verifySuccess = service.verifyRefundSign( refundback,  queryStr);
		if(!verifySuccess)
			throw new BusinessException(9004, "签名校验失败");
		
		// 2.查询原订单，以及退款订单
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?",
				refundback.getSp_refund_no());
		if (list == null || list.isEmpty()) {
			throw new BusinessException(7001, "原订单不存在");
		}
		Map<String, Object> map = list.get(0);
		//退款已经成功，后续不更新
		if("1".equals(map.get("STATUS"))){
			return  renderHtml(refundback.getOrder_no(), verifySuccess);
		}
		/*1	原路退款成功(已从商户账户扣款)
		2	退款失败
		3	退款至用户余额(已从商户账户扣款)
		4	退银行卡失败,已退至用户余额，请用户联系百度钱包(已从商户账户扣款)
		5	退银行卡失败，失败金额已退至商户余额*/
		
		// 3.更新状态
		String code = refundback.getRet_code();
		String msg = refundback.getRet_detail();

		String pwid = map.get("PWID").toString();
		String refundorderid = map.get("MER_ORDERID").toString();
		if ("1".equals(code)  || "3".equals(code)) {
			String sql = "update pg_wechat_txn set status='1',bk_orderid=?,respcode=?,respmsg=?,lmdate=sysdate where pwid=? and mer_orderid=?";
			int n = dao.update(sql, refundback.getBfb_order_no(), code, msg, pwid, refundorderid);
			logger.info("[百度钱包]退款异步通知,更新记录{}结果:1,条数{}", refundorderid,n);
		} else if ("4".equals(code) || "2".equals(code) || "5".equals(code)) {
			String sql = "update pg_wechat_txn set status='6',bk_orderid=?,respcode=?,respmsg=?,lmdate=sysdate where pwid=? and mer_orderid=?";
			int n = dao.update(sql, refundback.getBfb_order_no(), code, msg, pwid, refundorderid);
			logger.info("[百度钱包]退款异步通知,更新记录{}结果:6,条数{}", refundorderid, n);
		}

		// 4.渲染返回值
		return renderHtml(refundback.getOrder_no(), verifySuccess);
	}

	/**
	 * 百度钱包-退款查单
	 * 
	 * @param orderid
	 * @return
	 */
	public String refundQuery(Map<String, Object> refundMap)  {
		String refundorderid = refundMap.get("MER_ORDERID").toString();
		
		List<Map<String, Object>> list = dao.queryForList(
				"select x.mer_orderid as ORI_MER_ORDERID, t.* from (select t.* from pg_wechat_txn t "
				+ "where mer_orderid = ?) t join pg_wechat_txn x on t.oripwid = x.pwid",refundorderid);
		Map<String, Object> map = list.get(0);
		//防止已经是1之后还会更新
		if("1".equals(map.get("STATUS"))){
			return String.valueOf(map.get("RESPMSG"));
		}
		
		// 2.1获取发送数据
		Map<String, String> reqMap = new LinkedHashMap<String, String>();
		try {
			reqMap = service.getRefundQueryMsg(map);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("[百度钱包]退款-获取发送退款信息失败,订单号{}，{}",refundorderid,e.getCause());
			return "获取发送退款信息失败";
		}
		// 2.2发送数据
		logger.info("[百度钱包]获取查询退款请求参数：" + reqMap);
		HttpRequester reqUtil = new HttpRequester();
		HttpRespons response;
		try {
			response = reqUtil.sendGet(refundQueryUrl, reqMap);
		} catch (IOException e) {
			RedisUtil.addFailCountByRedis(1);
			logger.error("[百度钱包]退款-发送退款请求失败,订单号{}",refundorderid);
			return "发送退款请求失败";
		}
		// 2.3解析返回结果
		String resp = response.getContent();
		logger.info("[百度钱包]获取查询退款返回报文:{}", resp);
		BaiduRefundQueryRetBean retBean = BaiduRefundQueryRetBean.parseXmlFromStr(resp);
		
		// 3.更新数据库信息
		String code = retBean.getRet_code();
		String msg = retBean.getRet_details();
//		String pwid = refundMap.get("PWID").toString();
		String sql = "";
		
//		1	已退款至用户余额	
//		2	退回银行卡处理中	
//		3	原路退款成功（如果是余额支付，表示已退至余额；如果是银行卡支付，表示已退至银行卡）	有退款记录
//		5	退银行卡失败,已退至用户余额，请用户联系百度钱包	
		
//		4	退款失败	
//		6	退银行卡失败，失败金额已退至商户余额	
		
		if ("1".equals(code) || "2".equals(code) || "3".equals(code) || "5".equals(code)) {
			sql = "update pg_wechat_txn set status='1',respcode=?,respmsg=?,lmdate=sysdate where  mer_orderid=?";
		} else {
			sql = "update pg_wechat_txn set status='6',respcode=?,respmsg=?,lmdate=sysdate where  mer_orderid=?";
		}
		dao.update(sql, code, msg, refundorderid);
		return msg;
	}
}
