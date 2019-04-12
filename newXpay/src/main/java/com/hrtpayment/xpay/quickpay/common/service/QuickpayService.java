package com.hrtpayment.xpay.quickpay.common.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.channel.service.ChannelService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.quickpay.common.bean.QuickPayBean;
import com.hrtpayment.xpay.quickpay.cups.service.CupsQuickPayService;
import com.hrtpayment.xpay.quickpay.ldpay.service.LdPayCashService;
import com.hrtpayment.xpay.quickpay.ldpay.service.LdPayService;
import com.hrtpayment.xpay.quickpay.newCups.service.newCupsPayService;
import com.hrtpayment.xpay.utils.Base64;
import com.hrtpayment.xpay.utils.HttpConnectService;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.crypto.AesUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

/**
 * 快捷支付
 * 
 * @author songbeibei 2017年10月26日
 */
@Service
public class QuickpayService {
	Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao;
	@Value("${quick.cardEncKey}") // 快捷支付加密卡号秘钥
	private String encryKey;
	@Autowired
	QuickPayRtnHtmlService htmlService;
	@Autowired
	CupsQuickPayService cupsQuickPayService;
	@Autowired
	newCupsPayService newCupsPayService;
	@Autowired
	LdPayService ldPayService;
	
	@Autowired
	LdPayCashService ldPayCashService;
	@Autowired
	ManageService manageService;
	
	@Autowired
	CupsPayService cupsPayService;
	
	@Autowired
	ChannelService ch;
	
	@Value("${quick.merccbid}")
	private String merchantCcbId;
	@Value("${quick.limitamt}")
	private double limitamt;
	@Value("${quick.defaultfiid}")
	private String defaultFiid;
	
	@Value("${auto.key}")
	private String autoKey;
	
	@Value("${auto.qkurl}")
	private String qkUrl;
	
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
		String orderid = "hrt" + sdf.format(d) + sb.toString();
		return orderid;
	}

	public String encodeByAES(String msg) {
		String encodeMsg = "";
		try {
			byte[] securityText = AesUtil.encrypt(msg.getBytes(), encryKey.getBytes(), "AES/ECB/PKCS5Padding", null);
			encodeMsg = Base64.encode(securityText);
		} catch (Exception e) {
			logger.info("[快捷支付]{}加密异常{}",msg,e.getMessage());
			return "";
		}
		return encodeMsg;
	}

	public String decodeByAES(String msg) {
		String decodeMsg = "";
		try {
			byte[] msgByte = Base64.decode(msg);
			decodeMsg = new String(AesUtil.decrypt(msgByte, encryKey.getBytes(), "AES/ECB/PKCS5Padding", null),
					"UTF-8");
		} catch (Exception e) {
			logger.info("[快捷支付]{}解密异常{}",msg,e.getMessage());
			return "";
		}
		return decodeMsg;
	}
	
	/**
	 * 累增日限额
	 * @param mid
	 * @param amount
	 */
     public void addDayLimit(String mid,String amount){
    	 try {
				manageService.addDayMerAmtForQuickPay(mid,Double.valueOf(amount));
			} catch (BusinessException e) {
				logger.error("【快捷支付】限额累增异常----->"+mid,e);
			}
     }	

	/**
	 * 安全认证后调用的支付接口 暂时支持的银行有：兴业、北京银联 输入参数：fiid、卡号、入账人、身份证号、手机号、金额、银行行号、银行名称
	 * 
	 * 
	 * 返回数据进行判断
	 * 
	 * @param fiid
	 * @param amount
	 * @param mid
	 * @param mobile
	 * @param accNo
	 * @param accName
	 * @param bankNmae
	 * @param bankNo
	 * @return
	 */
	public String setPayInfo(QuickPayBean bean, String fiid, BigDecimal amount, String mid, String mobile, String accNo, String accName,String captcha) {
 
		
		String orderId="";
		if (bean.getOrderId()==null||"".equals(bean.getOrderId())||"null".equals(bean.getOrderId())) {
			orderId=createOrderId();
		}else{
			orderId=bean.getOrderId();
		}
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String timeNow = sdf.format(d);
		String signSql = "select pc.accname, pc.bankname,pc.legalnum_encrypt idcard,pc.mobileno_encrypt mobile,pc.accno_encrypt eaccno ,pc.accno,ps.qpsid,pc.bankcode"
				+ " from  hrt_qkpaycard pc left join  ( select qpsid ,accno from hrt_qkpaysign  where status=2  and fiid =?  ) ps "
				+ "on ps.accno=pc.accno  where /* pc.accno= ? and pc.mid=?*/ qpcid=? and pc.status=1";
		List<Map<String, Object>> signList = dao.queryForList(signSql, bean.getFiid(),bean.getQpcid());// bean.getAccNo(),bean.getMid());
		if (signList.size() == 0) {
			// 注入银行卡
			logger.info("[快捷支付]{}该银行卡未做绑定操作。", bean.getAccNo());
			bean.setMsg("该银行卡未做绑定操作");
			bean.setHtnlStatus("3");
			return "";
		}
		Map<String, Object> map = signList.get(0);
		accName = String.valueOf(map.get("ACCNAME"));
		String idCardNum = String.valueOf(map.get("IDCARD"));
		String didCardNum = decodeByAES(idCardNum);
		String mobileNum = String.valueOf(map.get("MOBILE"));
		String dmobileNum = decodeByAES(mobileNum);
		String eaccno = String.valueOf(map.get("EACCNO"));
		String daccno = decodeByAES(eaccno);
		accNo = String.valueOf(map.get("ACCNO"));
//		String bankCode=String.valueOf(map.get("BANKCODE"));
		boolean isCcb = false;// 0：非建设银行 1： 建设银行
		String bankCode=String.valueOf(map.get("BANKCODE"));
//		String querySup=" select fiid , (select bankcode from hrt_qksupportinfo where fiid=? and bankcode=?) bankcode  from hrt_qksupportinfo where fiid=? and rownum=1";
//		List<Map<String, Object>>  supList=dao.queryForList(querySup,fiid,bankCode,fiid);
//		if (null!=supList.get(0).get("fiid")&&null==supList.get(0).get("bankcode")) {
//			bean.setMsg("暂不支持该行信用卡交易，请更换信用卡");
//			bean.setHtnlStatus("3");
//			return "";
//		}
		if ("40".equals(fiid)) {
			logger.info("[北京银联快捷支付]{}",mid);			// 调用交易接口
			try {
//				orderId = createOrderId();
				String repStatus = cupsQuickPayService.pay(orderId, String.valueOf(bean.getAmount()), dmobileNum,
						accName, daccno, "02", "01", didCardNum, mid, bean.getTid(), "null".equals(bean.getUnno())?"":bean.getUnno(), isCcb,
						bean.getIsPoint(),bean.getQpcid()); 
				logger.info("[北京银联快捷支付]订单{}状态为{}", orderId, repStatus);
				bean.setOrderId(orderId);
			
				String orderSql = "select pwid,mer_orderid, time_end ,bankmid ,mer_id,txnamt,unno,trantype from pg_wechat_txn where mer_orderid=?";
				if ("R".equals(repStatus)) {
					List<Map<String, Object>> orders = dao.queryForList(orderSql, orderId);
					if (orders.size() == 0) {
						logger.info("[北京银联快捷支付]查询不到对应的订单：{}", orderId);
						bean.setHtnlStatus("3");
					}
					try {
						repStatus = cupsQuickPayService.queryOrder(orders.get(0));
					} catch (Exception e) {
						logger.error("[北京银联快捷支付]查询订单：{},数据库连接异常", orderId);
						bean.setHtnlStatus("4");
					}
					if ("R".equals(repStatus)) {
						for (int i=0;i<2;i++){
							try {
								repStatus = cupsQuickPayService.queryOrder(orders.get(0));
							} catch (Exception e) {
								logger.error("[北京银联快捷支付]查询订单：{},数据库连接异常", orderId);
								bean.setHtnlStatus("4");
							}
							if ("S".equals(repStatus)) {
								bean.setHtnlStatus("2");
								break;
							} else if ("E".equals(repStatus)) {
								bean.setHtnlStatus("3");
								break;
							}
							bean.setHtnlStatus("4");
						}
					} else if ("S".equals(repStatus)) {
						bean.setHtnlStatus("2");
					} else if ("E".equals(repStatus)) {
						bean.setHtnlStatus("3");
					}
					bean.setOrderId(orderId);
					bean.setOrderTime(timeNow);
					bean.setAccName(accName);
					return "";
				} else {
					bean.setMsg("交易失败");
					bean.setHtnlStatus("3");
					return "";
				}
			} catch (BusinessException e) {
				logger.error("[北京银联快捷支付]异常{}:{}  {}",mid,orderId, e.getMessage());
				String errmsg=e.getMessage();
				int length =errmsg.lastIndexOf("：");
				if(length>0){
					errmsg=errmsg.substring(length+1);
				}
				if("".equals(errmsg)||null==errmsg){
					errmsg="交易失败，请重新支付！";
				}
				bean.setMsg(errmsg);
				bean.setHtnlStatus("3");
				return "";
			}
		}else if ("62".equals(fiid)) {
			logger.info("[银联快捷支付]订单{}:商户{}",bean.getOrderId(),mid);			// 调用交易接口
			try {
//				orderId = createOrderId();
				/**
				 * @RequestParam String authcode,@RequestParam String smsKey,
				 * @RequestParam String orderid,@RequestParam BigDecimal amt,
				 * @RequestParam String accNo, @RequestParam String accName,
				 * @RequestParam String mid, @RequestParam String mer_name,
				 * @RequestParam String mer_type
				 */
				
				String repStatus = 	newCupsPayService.pay(bean,orderId, captcha,daccno);
//				String repStatus = newCupsPayService.pay(bean, orderId, String.valueOf(bean.getAmount()), mobileNum, 
//						accName, daccno, bankType, 
//						"01", didCardNum, mid, bean.getTid(), "null".equals(bean.getUnno())?"":bean.getUnno(), isCcb, 
//						bean.getIsPoint(),bean.getQpcid());
////						.pay(orderId, String.valueOf(bean.getAmount()), dmobileNum,
////						accName, daccno, "02", "01", didCardNum, mid, bean.getTid(), "null".equals(bean.getUnno())?"":bean.getUnno(), isCcb,
////						bean.getIsPoint(),bean.getQpcid()); 
				logger.info("[银联快捷支付]订单{}状态为{}", orderId, repStatus);
				bean.setOrderId(orderId);
			
				String orderSql = "select pwid,mer_orderid, time_end ,bankmid ,mer_id,txnamt,unno,trantype from pg_wechat_txn where mer_orderid=?";
				if ("R".equals(repStatus)) {
					List<Map<String, Object>> orders = dao.queryForList(orderSql, orderId);
					if (orders.size() == 0) {
						logger.info("[银联快捷支付]查询不到对应的订单：{}", orderId);
						bean.setHtnlStatus("3");
					}
					try {
						repStatus = newCupsPayService.queryOrder(orders.get(0),"1002");
					} catch (Exception e) {
						logger.error("[银联快捷支付]查询订单：{},数据库连接异常", orderId);
						bean.setHtnlStatus("4");
					}
					if ("R".equals(repStatus)) {
						for (int i=0;i<2;i++){
							try {
								repStatus = newCupsPayService.queryOrder(orders.get(0),"1002");
							} catch (Exception e) {
								logger.error("[银联快捷支付]查询订单：{},数据库连接异常", orderId);
								bean.setHtnlStatus("4");
							}
							if ("S".equals(repStatus)) {
								bean.setHtnlStatus("2");
								break;
							} else if ("E".equals(repStatus)) {
								bean.setHtnlStatus("3");
								break;
							}
							bean.setHtnlStatus("4");
						}
					} else if ("S".equals(repStatus)) {
						bean.setHtnlStatus("2");
					} else if ("E".equals(repStatus)) {
						bean.setHtnlStatus("3");
					}
					bean.setOrderId(orderId);
					bean.setOrderTime(timeNow);
					bean.setAccName(accName);
					return "";
				} else {
					bean.setMsg("交易失败");
					bean.setHtnlStatus("3");
					return "";
				}
			} catch (BusinessException e) {
				logger.error("[银联快捷支付]异常{}:{}  {}",mid,orderId, e.getMessage());
				String errmsg=e.getMessage();
				int length =errmsg.lastIndexOf("：");
				if(length>0){
					errmsg=errmsg.substring(length+1);
				}
				if("".equals(errmsg)||null==errmsg){
					errmsg="交易失败，请重新支付！";
				}
				bean.setMsg(errmsg);
				bean.setHtnlStatus("3");
				return "";
			}
		}else if("47".equals(fiid)) {
				try {
					orderId=orderId.replace("hrt", "pk"); 
					logger.info("[联动优势快捷支付]商户：{},订单:{}",mid,orderId);	
//					String cashStatus="E";
					String repStatus=ldPayService.quickPay(orderId, dmobileNum, bean.getCvn(), bean.getEffective(), daccno, "IDENTITY_CARD", didCardNum, accName,captcha);
 					logger.info("[联动优势快捷支付]订单{}状态为{}", orderId, repStatus);
					bean.setOrderId(orderId);
					String orderSql = "select pwid, mer_orderid, time_end ,bankmid ,mer_id,txnamt,to_char(cdate,'yyyy-MM-dd') trandate ,"
							+ " cdate,unno,mer_tid,pwid,bk_orderid,qrcode,ispoint from pg_wechat_txn where mer_orderid=?";
					List<Map<String, Object>> orders = dao.queryForList(orderSql, orderId);
					if (orders.size() == 0) {
						logger.info("[联动优势快捷支付]查询不到对应的订单：{}", orderId);
						bean.setHtnlStatus("3");
					}
					String pwid=String.valueOf(orders.get(0).get("PWID"));
					if ("R".equals(repStatus)) {	
						try {
							repStatus = ldPayService.queryQuickPay(orders.get(0));
						} catch (Exception e) {
							logger.error("[联动优势快捷支付]查询订单：{},数据库连接异常", orderId);
							bean.setHtnlStatus("4");
						}
						if ("R".equals(repStatus)) {
							for (int i=0;i<2;i++){
								try {
									repStatus = ldPayService.queryQuickPay(orders.get(0));
								} catch (Exception e) {
									logger.error("[联动优势快捷支付]查询订单：{},数据库连接异常", orderId);
									bean.setHtnlStatus("4");
								}
								if ("S".equals(repStatus)) {
//									cashStatus=ldPayCashService.cashQuickPay(pwid,bean.getIsPoint(), accName,dmobileNum);
									bean.setHtnlStatus("2");
									break;
								} else if ("E".equals(repStatus)) {
									bean.setHtnlStatus("3");
									break;
								}
								bean.setHtnlStatus("4");
							}
//							bean.setOrderId(orderId);
//							bean.setOrderTime(timeNow);
//							bean.setAccName(accName);
						} else if ("S".equals(repStatus)) {
							bean.setHtnlStatus("2");
//							cashStatus=ldPayCashService.cashQuickPay(pwid,bean.getIsPoint(), accName,dmobileNum);
						} else if ("E".equals(repStatus)) {
							bean.setMsg("交易失败");
							bean.setHtnlStatus("3");
						}  
						bean.setOrderId(orderId);
						bean.setOrderTime(timeNow);
						bean.setAccName(accName);
						return "";
					}else if("S".equals(repStatus)){ 
//						cashStatus=ldPayCashService.cashQuickPay(pwid,bean.getIsPoint(), accName,dmobileNum);
						bean.setOrderId(orderId);
						bean.setOrderTime(timeNow);
						bean.setAccName(accName);
						bean.setHtnlStatus("2");
						return "";
					} else {
						bean.setMsg("交易失败");
						bean.setHtnlStatus("3");
						return "";
					} 
//					String orderCashSql = " select pc.pwid, pc.cash_orderid,pw.txnlevel,qc.TXNTIME, pw.mer_id,pw.txnamt   from pg_wechat_txn pw,hrt_qk_cash qc where  pw.pwid=qc.pwid and mer_orderid=?";

//					if ("R".equals(cashStatus)) {
//						List<Map<String, Object>> cashOrders = dao.queryForList(orderCashSql, orderId);
//						if (orders.size() == 0) {
//							logger.info("[联动优势快捷支付]查询不到对应的提现交易：{}", orderId);
//							bean.setHtnlStatus("3");
//						}
//						try {
//							cashStatus=ldPayCashService.queryCashQuickPay(cashOrders.get(0));
//						} catch (Exception e) {
//							logger.error("[联动优势快捷支付]查询提现交易：{},数据库连接异常", orderId);
//							bean.setHtnlStatus("4");
//						}
//						if ("R".equals(cashStatus)) {
//							for (int i=0;i<2;i++){
//								try {
//									cashStatus = ldPayCashService.queryCashQuickPay(cashOrders.get(0));
//								} catch (Exception e) {
//									logger.error("[联动优势快捷支付]查询订单：{},数据库连接异常", orderId);
//									bean.setHtnlStatus("4");
//								}
//								if ("S".equals(cashStatus)) {
//									bean.setHtnlStatus("2");
//									break;
//								}
//								bean.setHtnlStatus("4");
//							}
//						} else if ("S".equals(repStatus)) {
//							bean.setHtnlStatus("2");
//						} else if ("E".equals(repStatus)) {
//							bean.setMsg("交易失败");
//							bean.setHtnlStatus("4");
//						}
//						bean.setOrderId(orderId);
//						bean.setOrderTime(timeNow);
//						bean.setAccName(accName);
//						return "";
////					}else  if ("S".equals(cashStatus)) {
////						bean.setHtnlStatus("2"); 
////						bean.setOrderId(orderId);
////						bean.setOrderTime(timeNow);
////						bean.setAccName(accName);
////						return "";
//					}else {
//						bean.setMsg("交易失败");
//						bean.setOrderId(orderId);
//						bean.setOrderTime(timeNow);
//						bean.setAccName(accName);
//						bean.setHtnlStatus("4");
//						return "";
//					} 
					 
				} catch (BusinessException e) {
					logger.error("[联动优势快捷支付]提现异常{}:{}  {}",mid,orderId, e.getMessage());
					bean.setMsg(e.getMessage());
					bean.setHtnlStatus("3");
					return "";
				}
		}else if("18".equals(fiid)){
			try {
				String qpcid=bean.getQpcid();
				String qrno=getQrnoToAutoPayment(orderId,bean.getUnno(),mid,qpcid,bean.getDeviceId(),bean.getPosition());
				if(null !=qrno && !"".equals(qrno)){
					bean.setOrderTime(timeNow);
					bean.setAccName(accName);
					bean.setOrderId(orderId);
					String status =cupsPayService.cupsPayNewBy2(orderId,mid,"",qrno,amount.toString(),"");
					if("R".equals(status)){
						String againStatus="R";
						for(int i=0;i<3;i++){ 
							String jsonStr =cupsPayService.cupsPayQueryCups(orderId);
							JSONObject resStr=JSONObject.parseObject(jsonStr);
							againStatus=resStr.getString("errcode");
							if("S".equals(againStatus)){
							    addDayLimit(mid, amount.toString());
								bean.setHtnlStatus("2");
								break;
							}else if("R".equals(againStatus)){
								bean.setHtnlStatus("4");
								continue;
							}else {
								bean.setMsg("交易失败");
								bean.setHtnlStatus("3");
								break;
							}
						}
						return "";
					}else if("E".equals(status)){
						bean.setMsg("交易失败");
						bean.setHtnlStatus("3");
						return "";
					}else{
						bean.setHtnlStatus("4");
						return "";
					}
				}else{
					bean.setMsg("交易失败");
					bean.setHtnlStatus("3");
					return "";
				}
			} catch (Exception e) {
				logger.error("[银联二维码支付]异常{}:{}  {}",mid,orderId, e.getMessage());
				bean.setMsg(e.getMessage());
				bean.setHtnlStatus("3");
				return "";
			}
		} else {
			bean.setHtnlStatus("3");
			bean.setMsg("该银行卡，暂不支持快捷交易");
			return "";
		}
	}
    
	
	/**
	 * 获取短信验证码
	 * @param bean
	 */
	public void getMessage(QuickPayBean bean ) {
		String fiid =bean.getFiid(); 
		String mid =bean.getMid();
		String qpcid= bean.getQpcid();
	    String orderId=bean.getOrderId();
	    String cvn=bean.getCvn();
	    String effective=bean.getEffective();
		String accInfoSql="select pc.accname, pc.bankname,pc.legalnum_encrypt idcard,pc.mobileno_encrypt mobile,pc.accno_encrypt eaccno ,pc.accno, pc.bankcode "
				+ " from  hrt_qkpaycard pc  where pc.qpcid= ? and pc.mid= ? and pc.status=1";
		List<Map<String, Object>> accInfoList = dao.queryForList(accInfoSql, qpcid,mid);
		if (accInfoList.size() == 0) {
			// 注入银行卡
			logger.info("[快捷支付]{}该银行卡未做绑定操作。", bean.getAccNo());
			bean.setRtnHtml("该银行卡未做绑定操作");
		}
		Map<String, Object> map= accInfoList.get(0);
		String accName = String.valueOf(map.get("ACCNAME"));
		String idCardNum = String.valueOf(map.get("IDCARD"));
		String didCardNum = decodeByAES(idCardNum);
		String mobileNum = String.valueOf(map.get("MOBILE"));
		String dmobileNum = decodeByAES(mobileNum);
		String eaccno = String.valueOf(map.get("EACCNO"));
		String daccno = decodeByAES(eaccno);
//		String accNo = String.valueOf(map.get("ACCNO"));
//		String bankName = String.valueOf(map.get("BANKNAME"));
		try{
			if ("40".equals(fiid)) {
				boolean preSignFlag = cupsQuickPayService.preSign(accName, didCardNum, dmobileNum, daccno);
				if (!preSignFlag) {
					logger.info("[北京银联快捷支付]商户{}预签约失败。", bean.getMid());
					bean.setRtnHtml("预签约失败");
				}
			}else if ("62".equals(fiid)) {
				// 银联获取验证码，并返回获取验证码接口同步返回的流水号
				bean.setAccNo(daccno);
				bean.setIdCard(didCardNum);
				bean.setMobile(dmobileNum);
				bean.setAccName(accName);
				logger.info("[银联快捷支付]{}获取短信验证码", bean.getMid());
				Map< String, String> rtnMap  = newCupsPayService.getMessage(bean);
						//(orderId, dmobileNum, cvn, effective, daccno, "IDENTITY_CARD", didCardNum, accName);
				bean.setPreSerial(rtnMap.get("SmsKey"));
				bean.setBankmid(rtnMap.get("bankMid"));
			}else if ("47".equals(fiid)) {
				// 联动获取验证码，并返回获取验证码接口同步返回的流水号
				logger.info("[联动快捷支付]{}获取短信验证码", bean.getMid());
				String preno=ldPayService.getMessage(orderId, dmobileNum, cvn, effective, daccno, "IDENTITY_CARD", didCardNum, accName);
				bean.setPreSerial(preno);
			}else{
				logger.info("[快捷支付]{}获取短信验证码", bean.getMid());
 
			}
		}catch(Exception e){
			logger.info("[快捷支付]获取验证码异常：", e.getMessage());
			throw new HrtBusinessException(8000, e.getMessage());
		}
	}
	
	/**
	 * 获取快捷支付通道规则
	 * @param bean
	 * @return
	 */
	public Map< String, Object> quickPayRules(QuickPayBean bean){
		String qpcid=bean.getQpcid();
		String isPoint=bean.getIsPoint();
//		String mid=bean.getMid();
		String accNo=bean.getAccNo();
		String htmlMsg="";
		String fiid="";
		String timestart="";
		String timeend="";
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	    String timeNow = sdf.format(d);
		Map<String, Object> accInfo=new  TreeMap<String, Object>();
		String ruleSql="select pr.orid,pr.compare, pr.fiid, pr.priority, pr.ispoint, pr.threshold,pr.timestart , pr.timeend  "
				+ " from hrt_qkpayrules pr, hrt_fi ch"
				+ " where bankgroup =  nvl(( select   bankgroup  from hrt_qkbankrefercontrast  where bankcode =  (select bankcode from hrt_qkpaycard where qpcid = ?)"
				+ "  and rownum = 1), 'GROUPZ') and ispoint = ? and pr.fiid = ch.fiid and ch.status = '1' "
				+ " and pr.status = '0' and ? between nvl(ch.timestart,'00:00:00') and nvl(ch.timeend,'23:59:59')  "
				+ " and ? between  nvl(pr.timestart,'00:00:00') and nvl( pr.timeend ,'23:59:59') order by priority desc";
		List<Map< String, Object>>  ruleList=dao.queryForList(ruleSql, qpcid,isPoint,timeNow,timeNow);
		

		/**
		 *   根据qpcid 和 ispoint 查询该卡对应的规则
		 *   1、如果没有查到 对应的规则  设置默认走的银行
		 *   2、查到银行后轮寻判断规则 
		 */
		if (ruleList.size()==0) {
			 fiid=defaultFiid;
			 String sql="select timestart ,timeend from hrt_fi where fiid=? and status=1";
			 List<Map< String, Object>> list =dao.queryForList(sql, fiid);
			 if ( list.size()==0) {
				return null;
			}
			 timestart=String.valueOf(list.get(0).get("TIMESTART"));
			 timeend=String.valueOf(list.get(0).get("TIMEEND"));
			 
		}else {
			for (int i=0;i<ruleList.size();i++){
				//将金额与获取的 thershold 利用compare区分判断条件、判断出是否可以走该通道
				Map<String, Object> map=ruleList.get(i);
			    double thershold=  Double.valueOf(String.valueOf(map.get("threshold")));
			    double amount = Double.valueOf(bean.getAmount());
				if ("2".equals(map.get("compare"))) {
					if (amount>thershold) {
						 fiid=String.valueOf(map.get("fiid"));
						 timestart=String.valueOf(map.get("timestart"));
						 timeend=String.valueOf(map.get("timeend"));
						 logger.info("[快捷支付]{}金额{}大于阈值{} 走通道{}",bean.getOrderId(),  amount, thershold,fiid );
						 break;
					}else{
						continue;
					}
				}else if ("3".equals(map.get("compare"))) {
					if (amount<thershold) {
						 fiid=String.valueOf(map.get("fiid"));
						 timestart=String.valueOf(map.get("timestart"));
						 timeend=String.valueOf(map.get("timeend"));
						 logger.info("[快捷支付]{}金额{}小于阈值{} 走通道{}",bean.getOrderId(),  amount, thershold,fiid );
						 break;
					}else{
						continue;
					}
				}else if ("4".equals(map.get("compare"))) {
					if (amount!=thershold) {
						 fiid=String.valueOf(map.get("fiid"));
						 timestart=String.valueOf(map.get("timestart"));
						 timeend=String.valueOf(map.get("timeend"));
						 logger.info("[快捷支付]{}金额{}不等于阈值{} 走通道{}",bean.getOrderId(),  amount, thershold,fiid );
						 break;
					}else{
						continue;
					}
				}
			}
		}
		
		String sql="select accno, accno_encrypt eaccno, mobileno_encrypt mobile,  bankname  bankname, bankcode bankcode, accname accname, LEGALNUM_ENCRYPT idcard"
				+ "  from hrt_qkpaycard  where qpcid = ?  and status=1 ";
		List<Map< String, Object>>  accList=dao.queryForList(sql, qpcid);
		
		if (accList.size()==0) {
			logger.info("[快捷支付]{}该银行卡未做绑定操作。",accNo);
			bean.setMsg("该银行卡未做绑定操作");
		    htmlMsg = htmlService.returnHtml("3", bean);
			bean.setRtnHtml(htmlMsg);
			bean.setStatus("0");
			return null;
		}else{
			 accInfo=accList.get(0);
			 if ("".equals(fiid)||null==fiid||"null".equals(fiid)) {
				 logger.info("未查到对应规则走系统设置默认通道fiid={}",defaultFiid);
				 fiid=defaultFiid;
				 String defaultsql="select nvl(timestart,'00:00:00') ,nvl(timeend,'23:59:59') from hrt_fi where fiid=? and status=1";
				 List<Map< String, Object>> list =dao.queryForList(defaultsql, fiid);
				 if ( list.size()==0) {
					return null;
				 }
				 timestart=String.valueOf(list.get(0).get("TIMESTART"));
				 timeend=String.valueOf(list.get(0).get("TIMEEND"));
			}
			 accInfo.put("fiid", fiid);
			 accInfo.put("timestart",timestart);
			 accInfo.put("timeend",timeend);
		}		
		return accInfo;
		
		
	} 
	/**
	 * 暂定规则 
	 *  金额小于100的 无积分支付交易 北京银联允许走： 先走北京银联  ，不允许： 提示信息  此卡暂不支持所选服务
	 *  type=1为金额<100 无积分交易 
	 *  type=0为其它情况交易
	 * @param bean
	 * @param type
	 * @return
	 */
	public Map<String, Object> payRules(QuickPayBean bean,String type){
		String htmlMsg ="";
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String timeNow = sdf.format(d);
		List<Map<String, Object>> list =null;
		
		String isPoint=bean.getIsPoint();
		String qpcid=bean.getQpcid();
		String mid=bean.getMid();
		String accNo=bean.getAccNo();
		if ("1".equals(type)) {
			logger.info("[快捷支付]无积分通道金额小于100");
			String querySql = "select t.bankcode,t.priority, t.fiid, t1.accno, t1.eaccno, t1.mobile,t1.bankname,t1.accname, t1.idcard, "
					+ " (select timestart from hrt_fi where fiid = nvl(t.fiid, '41')) timestart, "
					+ " (select timeend from hrt_fi where fiid = nvl(t.fiid, '41')) timeend, "
					+ " nvl(t.ispoint, '0') ispoint  "
					+ " from (select pr.bankcode bankcode, pr.priority priority, ch.fiid, pr.ispoint "
					+ "       from hrt_fi ch, Hrt_QKsupportInfo pr " + "       where pr.status = '1' "
					+ "         and ? between ch.timestart and ch.timeend " + "         and ch.status = '1' "
					+ "         and ch.fiid = pr.fiid " + "         and pr.ispoint = '1') t"
					+ " right join (select accno, accno_encrypt  eaccno,  mobileno_encrypt mobile,  bankname  bankname, bankcode  bankcode,"
					+ "                    accname  accname, LEGALNUM_ENCRYPT idcard " + "             from hrt_qkpaycard "
					+ "             where qpcid = ? " + "               and mid = ?) t1" + " on t.bankcode = t1.bankcode  "
					+ " order by t.priority desc";
			list= dao.queryForList(querySql, timeNow, qpcid, mid);
			if (list.size() == 0) {
				logger.info("[快捷支付]{}该银行卡未做绑定操作。",accNo);
				bean.setMsg("该银行卡未做绑定操作");
			    htmlMsg = htmlService.returnHtml("3", bean);
				bean.setRtnHtml(htmlMsg);
				bean.setStatus("0");
				return null;
			}
			String fiid=String.valueOf(list.get(0).get("FIID"));
			if ("41".equals(fiid)||null==fiid ||"".equals(fiid)||"null".equals(fiid)) {
				logger.info("[快捷支付]无积分通道金额小于100，商户{}:qpcid:{}此卡{}暂不支持所选服务 fiid={}。",mid,qpcid,accNo,fiid);
				bean.setMsg("此卡暂不支持所选服务。");
			    htmlMsg = htmlService.returnHtml("3", bean);
				bean.setRtnHtml(htmlMsg);
				bean.setStatus("0");
				return null;
			}
		}else if("0".equals(type)){
			String querySql = "select t.bankcode,t.priority, t.fiid, t1.accno, t1.eaccno, t1.mobile,t1.bankname,t1.accname, t1.idcard, "
					+ " (select timestart from hrt_fi where fiid = nvl(t.fiid, '41')) timestart, "
					+ " (select timeend from hrt_fi where fiid = nvl(t.fiid, '41')) timeend, "
					+ " nvl(t.ispoint, '0') ispoint  "
					+ " from (select pr.bankcode bankcode, pr.priority priority, ch.fiid, pr.ispoint "
					+ "       from hrt_fi ch, Hrt_QKsupportInfo pr " 
					+ "       where pr.status = '1' "
					+ "         and ? between ch.timestart and ch.timeend " 
					+ "         and ch.status = '1' "
					+ "         and ch.fiid = pr.fiid " 
					+ "         and pr.ispoint = ?) t"
					+ " right join (select accno, accno_encrypt  eaccno,  mobileno_encrypt mobile,  bankname  bankname, bankcode  bankcode,"
					+ "                    accname  accname, LEGALNUM_ENCRYPT idcard " 
					+ "             from hrt_qkpaycard "
					+ "             where qpcid = ? " 
					+ "               and mid = ?) t1" 
					+ " on t.bankcode = t1.bankcode  "
					+ " order by t.priority desc";
			   list = dao.queryForList(querySql, timeNow, isPoint, qpcid, mid);
	
			if (list.size() == 0) {
				logger.info("[快捷支付]{}该银行卡未做绑定操作。", accNo);
				bean.setMsg("该银行卡未做绑定操作");
				htmlMsg = htmlService.returnHtml("3", bean);
				bean.setRtnHtml(htmlMsg);
				bean.setStatus("0");
				return null;
			} else {
				// 请重新选择付款通道
				if (!bean.getIsPoint().equals(String.valueOf(list.get(0).get("ISPOINT")))) {
					logger.info("[快捷支付]请重新选择付款通道   上送ispoint={},实际 ispoint{}",bean.getIsPoint(),String.valueOf(list.get(0).get("ISPOINT")));
					bean.setMsg("请重新选择付款通道");
					htmlMsg = htmlService.returnHtml("3", bean);
					bean.setRtnHtml(htmlMsg);
					bean.setStatus("0");
					return null;
				}
			}
		}
		return list.get(0);
	}
	
	
	/**
	 * 通道选择接口 传入数据有 金额、mid、手机号、银行账号 根据卡bin判断银行，从规则表内的规则来选取通道 根据选取的通道来判断页面的下一步进程
	 * 银收宝：直接返回银收宝端返回的页面 兴业、北京银联：返回我们的安全认证页面 等待用户做下一步的操作 bean.status=0 app渲染页面
	 * bean.status=1 app弹出提示框
	 * 
	 * @param amount
	 * @param mid
	 * @param mobile
	 * @param accNo
	 * @return
	 * @throws BusinessException
	 */
	public String getQuickPayChannel(QuickPayBean bean) throws BusinessException {
		String accNo = "";
		String eaccNo = "";
		String daccNo = "";
		String accName = "";
		String cardId = "";
		String dcardId = "";
		String mobile = "";
		String dmobile = "";
		String bankName = "";
		String bankCode = "";
		String timeStart = "";
		String timeEnd = "";
		String timeNow = "";
		String fiid = "";
		String orderId =bean.getOrderId()==null||"".equals(bean.getOrderId())||"null".equals(bean.getOrderId())? createOrderId():bean.getOrderId();
		if (bean.getOrderId()==null||"".equals(bean.getOrderId())||"null".equals(bean.getOrderId())) {
			bean.setOrderId(orderId);
		}
		String isPoint = bean.getIsPoint() == null || "".equals(bean.getIsPoint()) ? "0"
				: String.valueOf(bean.getIsPoint());
		String mid = bean.getMid();
		String qpcid = bean.getQpcid();
		String htmlMsg = null;
		Map<String, Object> map = null;
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	    timeNow = sdf.format(d);
	    //金额小于100 的无积分 送到北京银联
//		if("0".equals(isPoint)&&100.00>Double.valueOf(bean.getAmount())){
//			map=payRules(bean,"0");
//			if (map==null) {
//				return "";
//			}
//		}else{
//			//金额大于18000 无积分 送到杉德  其余送到银收宝
//			map=payRules(bean,"0");
//			if (map==null) {
//				return "";
//			}
//		}
	    map= quickPayRules(bean);
		if (map==null) {
			bean.setMsg("通道暂未开通，请使用首页的“扫码收款”功能进行收款。");
			bean.setRtnHtml(htmlService.returnHtml("3", bean));
			bean.setStatus("0");
			return "";
		}
		logger.info("[快捷支付]{}获取数据成功。",mid);
		accNo = map.get("ACCNO") == null ? "" : String.valueOf(map.get("ACCNO"));
		eaccNo = map.get("EACCNO") == null ? "" : String.valueOf(map.get("EACCNO"));
		daccNo = decodeByAES(eaccNo);
		accName = map.get("ACCNAME") == null ? "" : String.valueOf(map.get("ACCNAME"));
		cardId = map.get("IDCARD") == null ? "" : String.valueOf(map.get("IDCARD"));
		dcardId = decodeByAES(cardId);
		mobile = map.get("MOBILE") == null ? "" : String.valueOf(map.get("MOBILE"));
		dmobile = decodeByAES(mobile);
		bankName = map.get("BANKNAME") == null ? "" : String.valueOf(map.get("BANKNAME"));
		bankCode = map.get("BANKCODE") == null ? "" : String.valueOf(map.get("BANKCODE"));
		timeStart = map.get("TIMESTART") == null ? "" : String.valueOf(map.get("TIMESTART"));
		timeEnd = map.get("TIMEEND") == null ? "" : String.valueOf(map.get("TIMEEND"));
		
		
		// 判断快捷支付是否已支付成功5次
		boolean flag =manageService.checkQuickPayDayCount(daccNo);
		if(!flag){
			logger.info("[快捷支付]商户{}：{}单卡单日超5笔交易!",mid,accNo);
			bean.setRtnHtml("单卡单日超5笔交易!");
			return "";
		}
		
		/**
		 * fiid 为 空 的时候 fiid=41 走银收宝 不为空的时候 走对应的银行
		 */
		fiid = map.get("FIID") == null || "".equals(String.valueOf(map.get("FIID"))) ? defaultFiid
				: String.valueOf(map.get("FIID"));

		isPoint = bean.getIsPoint() == null || "".equals(bean.getIsPoint()) ? ""
				: String.valueOf(bean.getIsPoint());
		htmlMsg = null;
		bean.setFiid(fiid);
		bean.setAccNo(accNo);
		bean.setAccName(accName);
		bean.setBankName(bankName);
		/**
		 * 2、选择通道 根据通道表内的通道状态、通道规则 状态、优先级 进行选择交易通道
		 */
		/**
		 * 3、获取要上送的参数 根据mid获取入账人姓名、身份证号
		 * 
		 * 4、分发通道 根据通道对应的fiid进入对应的分支 传入参数有：卡号、入账人、身份证号、手机号、金额、银行行号、银行名称
		 */
		if("40".equals(fiid)) {
			logger.info("[北京银联快捷支付]{}",mid);
			if (timeNow.compareTo(timeStart) < 0 || timeNow.compareTo(timeEnd) > 0) {
				logger.info("[北京银联快捷支付]{} 当前时间不允许交易",mid);
				bean.setRtnHtml("北京银联当前时间不允许交易");
				return "";
			}
			String signSql="select pc.accname, pc.bankname,pc.legalnum_encrypt idcard,pc.mobileno_encrypt mobile,pc.accno_encrypt eaccno ,pc.accno,ps.QPSID, pc.bankcode "
					+ " from  hrt_qkpaycard pc left join  ( select qpsid  ,accno from hrt_qkpaysign  where status=2  and fiid =40  ) ps "
					+ "	on ps.accno=pc.accno  where /*pc.accno= ? and pc.mid= ?*/ qpcid=? and pc.status=1";
			List<Map<String, Object>> signList = dao.queryForList(signSql, qpcid); //accNo,mid);
			if (signList.size() == 0) {
				// 注入银行卡
				logger.info("[北京银联快捷支付]{}该银行卡未做绑定操作。", bean.getAccNo());
				bean.setMsg("该银行卡未做绑定操作");
				bean.setRtnHtml(htmlService.returnHtml("3", bean));
				bean.setStatus("0");
				return "";
			}
				try {
					htmlMsg = htmlService.returnHtml("1", bean);
					bean.setRtnHtml(htmlMsg);
					bean.setStatus("0");
					return "";
				} catch (Exception e) {
					logger.error("[北京银联快捷支付]{}交易异常{}", mid,e.getMessage());
					bean.setMsg("交易失败");
					bean.setRtnHtml(htmlService.returnHtml("3", bean));
					bean.setStatus("0");
					return "";
				}
//			}
		}else if("62".equals(fiid)) {
			logger.info("[银联快捷支付]{}",mid);
			if (timeNow.compareTo(timeStart) < 0 || timeNow.compareTo(timeEnd) > 0) {
				logger.info("[银联快捷支付]{} 当前时间不允许交易",mid);
				bean.setRtnHtml("快捷支付当前时间不允许交易");
				return "";
			}
			String signSql="select pc.accname, pc.bankname,pc.legalnum_encrypt idcard,pc.mobileno_encrypt mobile,pc.accno_encrypt eaccno ,pc.accno,ps.QPSID, pc.bankcode "
					+ " from  hrt_qkpaycard pc left join  ( select qpsid  ,accno from hrt_qkpaysign  where status=2  and fiid =62 ) ps "
					+ "	on ps.accno=pc.accno  where pc.accno=? and pc.mid=? and pc.status=1";
			List<Map<String, Object>> signList = dao.queryForList(signSql,  accNo,mid);
			if (signList.size() == 0) {
				// 注入银行卡
				logger.info("[银联快捷支付]{}该银行卡未做绑定操作。", bean.getAccNo());
				bean.setMsg("该银行卡未做绑定操作");
				bean.setRtnHtml(htmlService.returnHtml("3", bean));
				bean.setStatus("0");
				return "";
			}
			String retCode=newCupsPayService.authPay(orderId, bean.getAmount(), String.valueOf(signList.get(0).get("BANKGATE")), accNo,mid, "null".equals(bean.getUnno())?"":bean.getUnno(),qpcid,isPoint);
			if ("E".equals(retCode)) {
				logger.error("[银联快捷支付]{}下单失败", mid);
				bean.setMsg("交易失败");
				bean.setRtnHtml(htmlService.returnHtml("3", bean));
				bean.setStatus("0");
			}
			try {
				bean.setOrderId(orderId);
				htmlMsg = htmlService.returnHtml("0", bean);
				bean.setRtnHtml(htmlMsg);
				bean.setStatus("0");
				return "";
			} catch (Exception e) {
				logger.error("[银联快捷支付]{}交易异常{}", mid,e.getMessage());
				bean.setMsg("交易失败");
				bean.setRtnHtml(htmlService.returnHtml("3", bean));
				bean.setStatus("0");
				return "";
			}
		}else if("18".equals(fiid)){
			logger.info("[银联二维码支付]{}",mid);
			if (timeNow.compareTo(timeStart) < 0 || timeNow.compareTo(timeEnd) > 0) {
				logger.info("[银联二维码支付]{} 当前时间不允许交易",mid);
				bean.setRtnHtml("银联二维码支付当前时间不允许交易");
				return "";
			}
			String signSql="select pc.accname, pc.bankname,pc.legalnum_encrypt idcard,pc.mobileno_encrypt mobile,pc.accno_encrypt eaccno ,pc.accno,ps.QPSID, pc.bankcode "
					+ " from  hrt_qkpaycard pc left join  ( select qpsid  ,accno from hrt_qkpaysign  where status=2  and fiid =40  ) ps "
					+ "	on ps.accno=pc.accno  where /*pc.accno= ? and pc.mid= ?*/ qpcid=? and pc.status=1";
			List<Map<String, Object>> signList = dao.queryForList(signSql, qpcid); //accNo,mid);
			if (signList.size() == 0) {
				// 注入银行卡
				logger.info("[银联二维码支付]{}该银行卡未做绑定操作。", bean.getAccNo());
				bean.setMsg("该银行卡未做绑定操作");
				bean.setRtnHtml(htmlService.returnHtml("3", bean));
				bean.setStatus("0");
				return "";
			}
				try {
					htmlMsg = htmlService.returnHtml("1", bean);
					bean.setRtnHtml(htmlMsg);
					bean.setStatus("0");
					return "";
				} catch (Exception e) {
					logger.error("[银联二维码支付]{}交易异常{}", mid,e.getMessage());
					bean.setMsg("交易失败");
					bean.setRtnHtml(htmlService.returnHtml("3", bean));
					bean.setStatus("0");
					return "";
				}
//			}
		}
//		else if("47".equals(fiid)){
//			logger.info("[联动优势快捷支付]{}",mid);
//			String signSql="select bankgate from hrt_qkbankrefercontrast where bankcode=(select   pc.bankcode "
//					+ "  from  hrt_qkpaycard pc  where /*pc.accno= ? and pc.mid= ?*/qpcid=? and pc.status=1) and rownum=1 ";
//			List<Map<String, Object>> signList = dao.queryForList(signSql, qpcid);// accNo,mid);
//			if (signList.size() == 0) {
//				// 注入银行卡
//				logger.info("[联动优势快捷支付]{}该银行卡未做绑定操作。", bean.getAccNo());
//				bean.setMsg("该银行卡未做绑定操作");
//				bean.setRtnHtml(htmlService.returnHtml("3", bean));
//				bean.setStatus("0");
//				return "";
//			}
//			orderId=orderId.replace("hrt", "pk");
//			String retCode=ldPayService.authPay(orderId , bean.getAmount(), String.valueOf(signList.get(0).get("BANKGATE")), accNo,mid, "null".equals(bean.getUnno())?"":bean.getUnno(),qpcid,isPoint);
//			if ("E".equals(retCode)) {
//				logger.error("[联动优势快捷支付]{}下单失败", mid);
//				bean.setMsg("交易失败");
//				bean.setRtnHtml(htmlService.returnHtml("3", bean));
//				bean.setStatus("0");
//			}
//				try {
//					bean.setOrderId(orderId);
//					htmlMsg = htmlService.returnHtml("0", bean);
//					bean.setRtnHtml(htmlMsg);
//					bean.setStatus("0");
//					return "";
//				} catch (Exception e) {
//					logger.error("[联动优势快捷支付]{}交易异常{}", mid,e.getMessage());
//					bean.setMsg("交易失败");
//					bean.setRtnHtml(htmlService.returnHtml("3", bean));
//					bean.setStatus("0");
//					return "";
//				}
//
//		}
		else{

		}
		// 返回一个异常页面
		logger.info("[快捷支付]{}:{}异常{}",mid, orderId);
		bean.setMsg("指定通道未开通");
		htmlMsg = htmlService.returnHtml("3", bean);
		bean.setRtnHtml(htmlMsg);
		bean.setStatus("0");
		return "";
	}
	
	
	public String getQrnoToAutoPayment(String orderid,String unno,String mid,String qpcId,String deviceId,
			String position){
		String qrno="";
		try {
			Map<String,String> reqMap = new HashMap<String,String>();		
			reqMap.put("unno", unno);
			reqMap.put("mid", mid);
			reqMap.put("qpcId", qpcId);
			reqMap.put("deviceId", deviceId);
			reqMap.put("position", position);
			reqMap.put("orderid", orderid);
			String sign = SimpleXmlUtil.getMd5Sign(reqMap, autoKey);
			reqMap.put("sign", sign);
			
			String reqStr = SimpleXmlUtil.map2xml(reqMap);
			
			String res =HttpConnectService.sendMessage(reqStr, qkUrl);
			JSONObject json =JSON.parseObject(res);
			String status=json.getString("status");
			if("S".equals(status)){
				qrno=json.getString("qrno");
				return qrno;
			}
		} catch (Exception e) {
			logger.error("获取支付授权码失败---------->",e);
		}
		return qrno;
	}
}
