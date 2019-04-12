package com.hrtpayment.xpay.quickpay.ldpay.service;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.quickpay.common.service.QuickpayService;
import com.hrtpayment.xpay.utils.HttpConnectService;
import com.hrtpayment.xpay.utils.RSAUtil;
import com.hrtpayment.xpay.utils.crypto.RsaUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Service
public class LdPayCashService implements InitializingBean {
	Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao;
	@Autowired
	ManageService manageService;
	@Autowired
	QuickpayService quickpayService;
	@Autowired
	LdPayServerServce serverService;
	@Autowired
	NotifyService sendNotify;

	@Value("${ldpay.privateKey}")
	private String privateKey;
	@Value("${ldpay.privatekeypath}")
	private String privateKeyPath;
	@Value("${ldpay.publickeypath}")
	private String publicKeyPath;
	@Value("${ldpay.sendurl}")
	private String sendUrl;
	@Value("${ldpay.merid}")
	private String ldmerid;

	private PrivateKey hrtPrivateKey;

	
	private String createOrderId() {
		String base = "0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 8; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String orderid = "pk" + sdf.format(d) + sb.toString();
		return orderid;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		hrtPrivateKey = RsaUtil.getPrivateKey(privateKeyPath, "PEM", "");
	}
	/**
	 * 联动优势异步通知响应
	 * 
	 * @return
	 * @throws BusinessException
	 */
	public String rtnCallBack(String order_id) throws BusinessException {
		/**
		 * 联动优势 异步通知 响应信息
		 */
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("<META NAME=\"MobilePayPlatform\" CONTENT=\"");
		Map<String, String> req = new HashMap<String, String>();
		req.put("mer_id", ldmerid);
		req.put("order_id", order_id);
		req.put("mer_date", new SimpleDateFormat("yyyyMMdd").format(new Date()));
		req.put("version", "4.0");
		req.put("ret_code", "0000");
		req.put("ret_msg", "异步通知接收成功");
		String signMsg = serverService.getSignBlock(req);
		// RSA加密
		try {
			String signData = RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("加密失败：", e);
			throw new BusinessException(9001, "加密失败");
		}
		sBuilder.append(serverService.getSignBlock(req));
		sBuilder.append("\">");
		return sBuilder.toString();
	}

	/**
	 * 联动优势快捷支付 异步通知处理
	 * 
	 * @param respMap
	 * @throws BusinessException
	 */
	public String ldPayCallback(Map<String, String> respMap) throws BusinessException {
		String order_id = respMap.get("order_id");
		String sql = "select pc.pwid, pc.cash_orderid,pw.txnlevel,pc.TXNTIME,pw.unno, pw.mer_id,pw.mer_tid ,pw.txnamt "
				+ " from  pg_wechat_txn pw,hrt_qk_cash pc "
				+ " where pw.pwid=pc.pwid and pw.status='1' and cash_orderid=? ";
		List<Map<String, Object>> list = dao.queryForList(sql, order_id);
		if (list.size() == 0) {
			logger.info("[联动优势快捷  ] 交易提现异步通知， 订单{}不符合提现要求", order_id);
			throw new BusinessException(8000, "订单"+order_id + "不符合提现要求");
		}
		Map<String, Object> map = list.get(0);
		String status = String.valueOf(map.get("STATUS"));
		if ("1".equals(status)) {
			logger.info("[联动优势快捷]交易提现 异步通知，订单{}状态不需要更新。", order_id);
			return order_id;
		}
		String trade_state = respMap.get("trade_state");
		String ret_code = respMap.get("ret_code");
		String ret_msg = respMap.get("ret_msg");
		if ("4".equals(trade_state) && "0000".equals(ret_code)) {
			String updateSql = "update hrt_qk_cash set respcode=?,respmsg=?,status='1' where  status<>'1' and cash_orderid=?";
			int count = dao.update(updateSql, ret_code, ret_msg, order_id);
			if (count == 1) {
				manageService.addQuickPayDayCount(order_id, null, "0");
				quickpayService.addDayLimit(String.valueOf(map.get("mer_id")), String.valueOf(map.get("txnamt")));
			} else {
				logger.info("[联动优势快捷支付 ]交易提现 订单 {}无需更新状态", order_id);
			}
		}
		return order_id;

	}

	/**
	 * E秒付 提现方法
	 * 
	 * @param pwid
	 * @param isPoint
	 * @param accName
	 * @return
	 * @throws BusinessException
	 */
	public String cashQuickPay(String pwid) throws BusinessException {
		String querySql = "select pwid, mer_orderid,txnamt,bk_orderid ,ispoint ,"
				+ "(select mf.bankaccno from hrt_merchfinacc mf,hrt_merchacc m where m.maid=mf.maid and hrt_mid= mer_id) accno,"
				+ "(select m.phone from hrt_merchacc m where  hrt_mid= mer_id) mobile, "
				+ "(select m.tname from hrt_merchacc m where  hrt_mid= mer_id) accName "
				+ " from pg_wechat_txn where status='1' and  PWID=?";
		List<Map<String, Object>> list = dao.queryForList(querySql, pwid);

		if (list.size() == 0) {
			logger.error("[联动优势快捷支付 交易提现]不存在订单{}", pwid);
			throw new BusinessException(8000, "原订单不存在");
		}
		String orderId = createOrderId();
		String isPoint = String.valueOf(list.get(0).get("ISPOINT"));
		String accno = String.valueOf(list.get(0).get("ACCNO"));
		String accName = String.valueOf(list.get(0).get("ACCNAME"));
		String mobile = String.valueOf(list.get(0).get("MOBILE"));
		BigDecimal amount = (BigDecimal) list.get(0).get("TXNAMT");
		BigDecimal fee = amount.multiply(new BigDecimal("0.00335"));
		// BigDecimal fee=0.00;
		// if ("0".equals(isPoint)) {
		// fee=amount.multiply(new BigDecimal("0.0038"));
		// amount=amount.subtract(fee);
		// amount=amount.subtract(BigDecimal.ONE).subtract(BigDecimal.ONE).subtract(BigDecimal.ONE).add(new BigDecimal("0.7"));
		// }else if ("1".equals(isPoint)) {
		// fee=amount.multiply(new BigDecimal("0.0049"));
		// amount=amount.subtract(fee);
		// amount=amount.subtract(BigDecimal.ONE).subtract(BigDecimal.ONE).subtract(BigDecimal.ONE).add(new BigDecimal("0.7"));
		// }
		amount = amount.subtract(fee);
		amount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
		
		if (amount.compareTo(new BigDecimal(0.00))<0) {
		   logger.info("[联动优势] 可提现金额小于手续费金额 ");	
		}
		
		Map<String, String> req = new HashMap<String, String>();
		req.put("service", "epay_direct_req");
		req.put("charset", "UTF-8");
		req.put("mer_id", ldmerid);
		req.put("notify_url", "http://58.83.189.105:9083/xpay/ldPayCashCallback");
		req.put("res_format", "HTML");
		req.put("version", "4.0");
		req.put("order_id", orderId);
		req.put("mer_date", new SimpleDateFormat("yyyyMMdd").format(new Date()));
		req.put("amount", String.valueOf(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));// 分为单位
																												// 处理数据
		req.put("recv_account_type", "00");
		req.put("recv_bank_acc_pro", "0");
		req.put("recv_account", serverService.enctry(publicKeyPath, accno));// GBK编码后使用联动公钥进行RSA加密，最后使用BASE64编码.
		req.put("recv_user_name", serverService.enctry(publicKeyPath, accName));// GBK编码后使用联动公钥进行RSA加密，最后使用BASE64编码.
		req.put("cut_fee_type", "2");
		req.put("mobile_no", mobile);

		String signMsg = serverService.getSignBlock(req);
		// RSA加密
		try {
			String signData = RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("[联动优势快捷支付 ]订单{}交易提现加密失败：{}", orderId, e.getMessage());
			throw new BusinessException(9001, "交易失败");
		}

		/*
		 * 发送交易
		 */
		String response = "";
		try {
			logger.info("[联动优势快捷支付 ] 订单{} 交易提现请求报文", orderId);
			response = HttpConnectService.postForm(req, sendUrl);
			logger.info("[联动优势快捷支付 ] 订单{} 交易提现响应报文", orderId);
		} catch (Exception e) {
			logger.info("[联动优势快捷支付 ] 订单{} 交易提现 请求异常{}", orderId, e.getMessage());
			throw new BusinessException(8000, "交易异常");
		}

		Pattern pattern = Pattern.compile("CONTENT=\".*.\"");
		Matcher m = pattern.matcher(response);
		boolean found = m.find();
		String content = "";
		if (found)
			content = m.group();
		Map<String, Object> respMap = new HashMap<String, Object>();
		String rtnStatus = "E";
		if (!"".equals(content)) {
			content = content.substring(9, content.length() - 1);
			respMap = serverService.getResp(content);
			String ret_code = String.valueOf(respMap.get("ret_code"));
			String ret_msg = String.valueOf(respMap.get("ret_msg"));
			String trade_state = String.valueOf(respMap.get("trade_state"));
			String trade_no = String.valueOf(respMap.get("trade_no"));
			String txntime = String.valueOf(respMap.get("transfer_settle_date"));
			String insertSql = "insert into hrt_qk_cash (qcid,pwid,cdate,lmdate,status,txntime,cashamt,cash_orderid,bk_cashorderid,respcode,respmsg) "
					+ "values (s_hrt_qk_cash.nextval,?,sysdate,sysdate,'0',?,?,?,?,?,?)";
			if ("4".equals(trade_state) && "0000".equals(ret_code)) {
				rtnStatus = "R";
			} else if ("00131098".equals(ret_code)) {
				rtnStatus = "E";
				ret_msg = "不在E秒付运营时间内";
			} else {
				logger.info("[联动优势快捷支付 ]订单{}交易提现异常，{}", orderId, String.valueOf(respMap.get("ret_msg")));
			}
			try{
				dao.update(insertSql, pwid, txntime, amount, orderId, trade_no, ret_code, ret_msg);
			}catch(Exception e){
				logger.error("[联动优势] 提现交易插入异常{}",e.getMessage());
				throw new BusinessException(8000, "提现异常");
			}
		} else {
			logger.info("[联动优势快捷支付  ]订单{}交易提现异常，返回content为空", orderId);
			throw new BusinessException(8000, "交易异常");
		}
		return rtnStatus;
	}

	/**
	 * 查询提现交易状态
	 * 
	 * @param map
	 * @return
	 * @throws BusinessException
	 */
	public String queryCashQuickPay(Map<String, Object> map) throws BusinessException {

		String cash_orderid = String.valueOf(map.get("CASH_ORDERID"));
		String txntime = String.valueOf(map.get("TXNTIME"));
		Map<String, String> req = new HashMap<String, String>();
		req.put("service", "transfer_query");
		req.put("charset", "UTF-8");
		req.put("mer_id", ldmerid);
		req.put("order_id", cash_orderid);
		req.put("mer_date", txntime);
		req.put("res_format", "HTML");
		req.put("version", "4.0");
		req.put("order_type", "1");

		String signMsg = serverService.getSignBlock(req);
		// RSA加密
		try {
			String signData = RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("[联动优势快捷支付]订单{} 交易提现查询加密失败：", cash_orderid, e.getMessage());
			throw new BusinessException(9001, "交易失败");
		}
		/*
		 * 发送交易
		 */
		String response = "";
		try {
			logger.info("[联动优势快捷支付 ] 订单{} 交易提现查询请求报文", cash_orderid);
			response = HttpConnectService.postForm(req, sendUrl);
			logger.info("[联动优势快捷支付 ] 订单{} 交易提现查询响应报文", cash_orderid);
		} catch (Exception e) {
			logger.info("[联动优势快捷支付] 订单{} 交易提现查询异常{}", cash_orderid, e.getMessage());
			throw new BusinessException(8000, "交易异常");
		}

		Pattern pattern = Pattern.compile("CONTENT=\".*.\"");
		Matcher m = pattern.matcher(response);
		boolean found = m.find();
		String content = "";
		if (found)
			content = m.group();
		Map<String, Object> respMap = new HashMap<String, Object>();
		if (!"".equals(content)) {
			content = content.substring(9, content.length() - 1);
			respMap = serverService.getResp(content);
			String ret_code = String.valueOf(respMap.get("ret_code"));
			String ret_msg = String.valueOf(respMap.get("ret_msg"));
			String trade_state = String.valueOf(respMap.get("trade_state"));
			// lmdate=sysdate,
			String updateSql = " update hrt_qk_cash set status=?,respcode=?,respmsg=? where  status<>'1' and  cash_orderid=? ";
			String returnCode = "E"; 
			/**
			 * amount=3&fee=100&mer_date=20130909&mer_id=9995&order_id=
			 * 20130909259566&purpose=测试请通过&ret_code=0000&ret_msg=付款查询&sign_type
			 * =RSA& trade_no=1309091546246111&trade_state=11&transfer_date=&
			 * transfer_settle_date=&version=4.0
			 */
			if ("0000".equals(ret_code)) {
				trade_state = String.valueOf(respMap.get("trade_state"));
				if ("4".equals(trade_state)) {
					int count = dao.update(updateSql, "1", ret_code, ret_msg, cash_orderid);
					if (count == 1) {
						// 累增快捷支付成功次数
						logger.info("[联动优势快捷支付]订单{} 交易提现状态更新完成，更新数量{}", cash_orderid, count);
						manageService.addQuickPayDayCount(cash_orderid, null, "0");
						quickpayService.addDayLimit(String.valueOf(map.get("MER_ID")),
								String.valueOf(map.get("TXNAMT")));
//						BigDecimal bDecimal = new BigDecimal(
//								map.get("TXNLEVEL") == null ? "0" : String.valueOf(map.get("TXNLEVEL")));
//						BigDecimal txnLevel = map.get("TXNLEVEL") == null || "".equals(map.get("TXNLEVEL"))
//								? BigDecimal.ONE : bDecimal;
//						map.put("TXNLEVEL", txnLevel);
//						sendNotify.sendNotify(map);
					} else {
						logger.info("[联动优势快捷支付]交易提现订单{}状态为1，无需更新状态", cash_orderid);
					}
					returnCode = "S";
				} else if ("1".equals(trade_state) || "11".equals(trade_state) || "12".equals(trade_state)
						|| "14".equals(trade_state) || "16".equals(trade_state)|| "0".equals(trade_state)) {
					returnCode = "R";
				} else {
					dao.update(updateSql, "6", ret_code, ret_msg, cash_orderid);
					returnCode = "E";
				}
			} else if ("00131013".equals(ret_code) || "00020000".equals(ret_code) || "00009999".equals(ret_code)) {
				returnCode = "R";
			} else {
				logger.info("[联动优势快捷支付]查询交易提现订单{}异常，{}", cash_orderid, String.valueOf(respMap.get("ret_msg")));
				dao.update(updateSql, "6", ret_code, ret_msg, cash_orderid);
				returnCode = "E";
			}
			return returnCode;
		} else {
			logger.info("[联动优势快捷支付]订单{}请求交易提现异常，返回content为空", cash_orderid);
			throw new BusinessException(8000, "交易异常");
		}

	}

	public void queryBalance() throws BusinessException {
		/**
		 * service 接口名称 变长32 query_account_balance charset 参数字符编码集 变长16
		 * 商户网站使用的编码格式，支持 UTF-8、GBK、GB2312、GB18030 mer_id 商户编号 变长8
		 * 由平台统一分配合作商户唯一标识 res_format 响应数据格式 变长16 暂支持HTML version 版本号 定长3 定值4.0
		 * sign_type 签名方式 变长8 暂只支持RSA必须大写 sign 签名 变长256 参见附录-签名机制 acc_type 账户类型
		 * 定长2 2：结算现金账户 9：过渡账户（付款专用户） 7：手续费账户 20：查询可借余额（e秒付帐户） 默认值2：结算现金账户
		 */
		Map<String, String> req = new HashMap<String, String>();
		req.put("service", "query_account_balance");
		req.put("charset", "UTF-8");
		req.put("mer_id", ldmerid);
		req.put("res_format", "HTML");
		req.put("version", "4.0");
		req.put("acc_type", "20");

		String signMsg = serverService.getSignBlock(req);
		// RSA加密
		try {
			String signData = RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("[联动优势快捷支付 ]查询余额异常：{}", e.getMessage());
			throw new BusinessException(9001, "交易失败");
		}
		/*
		 * 发送交易
		 */
		String response = "";
		try {
			logger.info("[联动优势快捷支付 ] 查询余额 请求报文");
			response = HttpConnectService.postForm(req, sendUrl);
			logger.info("[联动优势快捷支付 ] 查询余额 响应报文");
		} catch (Exception e) {
			logger.error("[联动优势快捷支付 ] 查询余额 请求异常{}", e.getMessage());
			throw new BusinessException(8000, "交易异常");
		}
        
	}

}
