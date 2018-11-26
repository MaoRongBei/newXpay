package com.hrtpayment.xpay.quickpay.ldpay.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger; 
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.quickpay.common.bean.QuickPayBean;
import com.hrtpayment.xpay.quickpay.common.service.QuickpayService;
import com.hrtpayment.xpay.quickpay.ldpay.util.SunBase64;
import com.hrtpayment.xpay.utils.HttpConnectService;
import com.hrtpayment.xpay.utils.RSAUtil;
import com.hrtpayment.xpay.utils.UrlCodec;
import com.hrtpayment.xpay.utils.crypto.RsaUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
 

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;



@Service
public class LdPayService  implements InitializingBean{// {

	Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao; 
	@Autowired
	ManageService manageService;
	@Autowired
	QuickpayService quickpayService;
	@Autowired
	LdPayCashService ldPayCashService;
	@Autowired
	LdPayServerServce ldPayServerServce;
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
	@Value("${ldpay.notifyurl}")
	private String notifyUrl;
	
	private PrivateKey hrtPrivateKey; 
	
	@Override
	public void afterPropertiesSet() throws Exception {
		hrtPrivateKey = RsaUtil.getPrivateKey(privateKeyPath, "PEM", "");
	}
	
	/**
	 * 解析请求参数为JSON字符串
	 * 
	 * @param request
	 *            请求
	 * @return
	 * @author
	 * @throws Exception
	 * @date 2016年10月17日 下午9:17:35
	 */
	public Map<String, String> parseRequest2JsonStr(HttpServletRequest request) throws Exception {
		// 读取参数
		Map<String, String> map =new TreeMap<String, String>();
		Enumeration<String> enu=request.getParameterNames();  
		while(enu.hasMoreElements()){  
		String paraName=(String)enu.nextElement();  
		map.put(paraName, request.getParameter(paraName));
		}
  
		return map;
	}
	
	
	/**
	 * 联动优势异步通知响应
	 * @return
	 * @throws BusinessException 
	 */
	public String  rtnCallBack(String order_id) throws BusinessException{
		/**
		 * 联动优势 异步通知 响应信息
		 */
		StringBuilder sBuilder=new StringBuilder();
		sBuilder.append("<META NAME=\"MobilePayPlatform\" CONTENT=\"");
		Map< String, String> req= new HashMap<String, String>();
		req.put("mer_id", ldmerid);
		req.put("order_id",order_id);
		req.put("mer_date",new SimpleDateFormat("yyyyMMdd").format(new Date())) ;
		req.put("version", "4.0");
		req.put("ret_code", "0000");
		req.put("ret_msg", "异步通知接收成功");
		String signMsg=ldPayServerServce.getSignBlock(req); 
		//RSA加密
		 try {
	       	String signData =RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("加密失败：",e);
			throw new BusinessException(9001,"加密失败");
		}
		sBuilder.append(ldPayServerServce.getSignBlock(req));
		sBuilder.append("\">");
		return sBuilder.toString();
	} 
	/**
	 * 联动优势快捷支付 
	 * 异步通知处理
	 * @param respMap
	 * @throws BusinessException 
	 */
	public String ldPayCallback(Map<String, String>  respMap) throws BusinessException{
		String order_id=respMap.get("order_id");
		String sql="select pwid, mer_id,txnamt,status,unno,mer_orderid,mer_tid　from pg_wechat_txn where mer_orderid=?";
		List<Map<String , Object>> list=dao.queryForList(sql, order_id);
		if (list.size()==0 ) {
			logger.info("[联动优势快捷 支付]异步通知，不存在订单{}", order_id);
			throw new BusinessException(8000, order_id+"订单不存在");
		}
		Map<String, Object> map=list.get(0);
		String status=String.valueOf(map.get("STATUS"));
		if ("1".equals(status)) {
			logger.info("[联动优势快捷 支付]异步通知，订单{}状态不需要更新。", order_id);
			return order_id;
		} 
		String trade_state=respMap.get("trade_state");
		String error_code=respMap.get("error_code");
		if ("TRADE_SUCCESS".equals(trade_state)&&"0000".equals(error_code)) {
			String updateSql="update pg_wechat_txn set respcode=?,respmsg=?,status='1',lmdate=sysdate  where  status<>'1' and mer_orderid=?";
			int count=dao.update(updateSql, error_code,trade_state,order_id);
			if (count==1) {
				manageService.addQuickPayDayCount(order_id, null, "0");
				quickpayService.addDayLimit(String.valueOf(map.get("mer_id")), String.valueOf(map.get("txnamt")));
				BigDecimal bDecimal=new BigDecimal(map.get("TXNLEVEL")==null?"0":String.valueOf(map.get("TXNLEVEL")));
				BigDecimal txnLevel=list.get(0).get("TXNLEVEL")==null||"".equals(list.get(0).get("TXNLEVEL"))?BigDecimal.ONE:bDecimal ;
				list.get(0).put("TXNLEVEL", txnLevel );
				sendNotify.sendNotify(list.get(0)); 
				/**
				 * 支付成功后，默认执行一次提现操作
				 */
				ldPayCashService.cashQuickPay(String.valueOf(map.get("pwid")));
			}else{
				logger.info("[联动优势快捷支付]  订单 {}无需更新状态",order_id);
			}
		}
		return order_id;
		
	}
	
	
	 /**
	  * 1、预下单  
	  *    判断走联动后  就向数据库内添加一条记录
	  *    将最新的一条记录 送到 h5页面 
	  * @param orderId
	  * @param amount
	  * @param gateId
	  * @param accno
	  * @param mer_id
	  * @param unno
	  * @param qpcid
	  * @param ispoint
	  * @return
	  * @throws BusinessException
	  */
	
	public String authPay(String orderId, String amount,String gateId,String accno,String mer_id,String unno,String qpcid,String ispoint) throws BusinessException{		
		
		Map< String, String> req= new HashMap<String, String>();
		req.put("service", "apply_pay_shortcut");
		req.put("charset", "UTF-8");
		req.put("mer_id", ldmerid);
		req.put("notify_url", notifyUrl);
		req.put("order_id", orderId);
		req.put("mer_date",new SimpleDateFormat("yyyyMMdd").format(new Date())) ;
		req.put("res_format", "HTML");
		req.put("version", "4.0");
		req.put("amount",  String.valueOf(BigDecimal.valueOf(Double.valueOf(amount)).multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue()));
		req.put("amt_type", "RMB");
		req.put("pay_type", "CREDITCARD");
		req.put("gate_id", gateId);
		
		
		String signMsg=ldPayServerServce.getSignBlock(req); 
		//RSA加密
		 try {
	       	String signData =RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("[联动优势快捷支付]预下单加密失败：{}",e.getMessage());
			throw new BusinessException(9001,"预下单加密失败");
		}
		 
		 /**
		  * 发送交易
		  * 
		  */
			String response = "";
			try {
				logger.info("[联动优势快捷支付] 订单{}预支付 请求报文 ",orderId );
				response = HttpConnectService.postForm(req, sendUrl);
				logger.info("[联动优势快捷支付] 订单{}预支付 响应报文 ",orderId );
			} catch (Exception e) {
				logger.error("[联动优势快捷支付] 订单{}预下单请求异常{}",orderId,e.getMessage());
				throw new BusinessException(8000,"预下单发送交易异常");
			}
			 
			Pattern pattern=Pattern.compile("CONTENT=\".*.\"");
			Matcher m = pattern.matcher(response); 
			boolean found = m.find(); 
			String content=""; 
			if(found)content= m.group();  
			Map<String, Object> respMap=new HashMap<String,Object>();
			if (!"".equals(content)) {
				content=content.substring(9, content.length()-1);
				respMap=ldPayServerServce.getResp(content);
			}else{
				logger.info("[联动优势快捷支付] 订单{}预支付 返回 content 为空",orderId);
			}
			if ("0000".equals(String.valueOf(respMap.get("ret_code")))) {
				
				String insertSql="insert into pg_wechat_txn (pwid,fiid,cdate,lmdate,status,txntype, mer_orderid,bk_orderid,txnamt,qrcode,tranType,detail,mer_id,accno,bankmid,respcode,respmsg,unno,isscode,ispoint)"
						+ " values(s_pg_wechat_txn.nextval,47,sysdate,sysdate,0,0,?,?,?,?,8,'快捷支付',?,?,?,?,?,?,?,?)";
				dao.update(insertSql, orderId,respMap.get("trade_no"),amount,respMap.get("payElements"),mer_id,ldPayServerServce.formatCardNo(accno),ldmerid,"0000","预下单成功",unno,qpcid,ispoint);
				logger.info("[联动优势快捷支付] 订单{}预支付成功",orderId);
				return "S";
			} 
			return "E";
			
	}
	
	
	
	/**
	 * 
	 * 联动优势 获取短信验证码
	 * 
	 * @param orderId  订单号
	 * @param mobile  手机号
	 * @param cvv2    cvv2
	 * @param valid_date  有效期
	 * @param card_id   卡号
	 * @param identity_type  证件类别
	 * @param identity_code  证件号
	 * @param card_holder  持卡人
	 * @return
	 * @throws BusinessException
	 */
	public  String getMessage(String orderId,String mobile,String cvv2,String valid_date,String card_id,String identity_type,String identity_code,String card_holder) throws BusinessException{
		String querySql="select bk_orderid,qrcode from pg_wechat_txn where mer_orderid=?";
		List<Map<String, Object>> list= dao.queryForList(querySql, orderId);
		
		if (list.size()==0) {
			throw new BusinessException(8000, "获取验证码失败");
		}
		String bk_orderid=String.valueOf(list.get(0).get("BK_ORDERID"));
		String pay_elements=String.valueOf(list.get(0).get("QRCODE"));
	 
		Map< String, String> req= new HashMap<String, String>();
		req.put("service", "sms_req_shortcut");
		req.put("charset", "UTF-8");
		req.put("mer_id", ldmerid);
		req.put("notify_url",notifyUrl);
		req.put("order_id", orderId);
		req.put("mer_date",new SimpleDateFormat("yyyyMMdd").format(new Date())) ;
		req.put("res_format", "HTML");
		req.put("version", "4.0");
		req.put("trade_no", bk_orderid);
		req.put("media_type", "MOBILE");
		req.put("media_id", mobile);
		 
		String[] elements=pay_elements.split(",");
		for(int i=0;i<elements.length;i++){
			try {
			if ("card_id".equals(elements[i])) {
				req.put("card_id", ldPayServerServce.enctry(publicKeyPath,card_id));
			}else if ("identity_type".equals(elements[i])) {
				req.put("identity_type",identity_type);
			}else if ("identity_code".equals(elements[i])) {
				req.put("identity_code",ldPayServerServce.enctry(publicKeyPath,identity_code));
			}else if ("card_holder".equals(elements[i])) {
				req.put("card_holder",ldPayServerServce.enctry(publicKeyPath,card_holder));
			}else if ("valid_date".equals(elements[i])) {
				req.put("valid_date",ldPayServerServce.enctry(publicKeyPath,valid_date.substring(2)+valid_date.substring(0,2)));
			}else if ("cvv2".equals(elements[i])) {
				req.put("cvv2",ldPayServerServce.enctry(publicKeyPath,cvv2));
			}
			} catch (Exception e) { 
				logger.error("[联动优势快捷支付]订单{}获取验证码 上送加密信息{}出现异常：{}",orderId,pay_elements,e.getMessage());
			}
		}
		String signMsg=ldPayServerServce.getSignBlock(req); 
		//RSA加密
		 try {
	       	String signData =RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("[联动优势快捷支付]获取验证码加密失败：",e);
			throw new BusinessException(9001,"加密失败");
		}
		 /* 发送交易
		  * http://pay.soopay.net/spay/pay/payservice.do
		  */
			String response = "";
			try {
				logger.info("[联动优势快捷支付] 订单{}获取验证码  请求报文 ",orderId );
				response = HttpConnectService.postForm(req, "http://pay.soopay.net/spay/pay/payservice.do");
				logger.info("[联动优势快捷支付] 订单{}获取验证码  响应报文",orderId );
			} catch (Exception e) {
				logger.info("[联动优势快捷支付] 订单{}获取验证码请求异常{}",orderId,e.getMessage());
				throw new BusinessException(8000,"获取验证码发送交易异常");
			}
		
			Pattern pattern=Pattern.compile("CONTENT=\".*.\"");
			Matcher m = pattern.matcher(response); 
			boolean found = m.find(); 
			String content=""; 
			if(found)content= m.group();  
			Map<String, Object> respMap=new HashMap<String,Object>();
			if (!"".equals(content)) {
				content=content.substring(9, content.length()-1);
				respMap=ldPayServerServce.getResp(content);
			}else{
				logger.info("[联动优势快捷支付] 订单{}获取验证码 返回 content 为空",orderId);
			}
			return bk_orderid;
	}

	
	/**
	 * 联动快捷  支付操作
	 * @param orderId
	 * @param mobile
	 * @param cvv2
	 * @param valid_date
	 * @param card_id
	 * @param identity_type
	 * @param identity_code
	 * @param card_holder
	 * @param verify_code
	 * @return
	 * @throws BusinessException
	 */
	public  String quickPay(String orderId,String mobile,String cvv2,String valid_date,String card_id,String identity_type,String identity_code,String card_holder,String verify_code) throws BusinessException{
		String querySql="select bk_orderid,qrcode from pg_wechat_txn where mer_orderid=?";
		List<Map<String, Object>> list= dao.queryForList(querySql, orderId);
		
		if (list.size()==0) {
			logger.error("[联动优势快捷支付]不存在订单{}",orderId);
			throw new BusinessException(8000, "订单号不存在");
		}
		String bk_orderid=String.valueOf(list.get(0).get("BK_ORDERID"));
		String pay_elements=String.valueOf(list.get(0).get("QRCODE"));
	 
		Map< String, String> req= new HashMap<String, String>();
		req.put("service", "confirm_pay_shortcut");
		req.put("charset", "UTF-8");
		req.put("mer_id", ldmerid);
		req.put("notify_url",  notifyUrl);
		req.put("order_id", orderId);
		req.put("mer_date",new SimpleDateFormat("yyyyMMdd").format(new Date())) ;
		req.put("res_format", "HTML");
		req.put("version", "4.0");
		req.put("trade_no", bk_orderid);
		req.put("media_type", "MOBILE");
		req.put("media_id", mobile);
		
		req.put("verify_code", verify_code);
		 
		String[] elements=pay_elements.split(",");
		for(int i=0;i<elements.length;i++){
			try {
			if ("card_id".equals(elements[i])) {
				req.put("card_id", ldPayServerServce.enctry(publicKeyPath,card_id));
			}else if ("identity_type".equals(elements[i])) {
				req.put("identity_type",identity_type);
			}else if ("identity_code".equals(elements[i])) {
				req.put("identity_code",ldPayServerServce.enctry(publicKeyPath,identity_code));
			}else if ("card_holder".equals(elements[i])) {
				req.put("card_holder",ldPayServerServce.enctry(publicKeyPath,card_holder));
			}else if ("valid_date".equals(elements[i])) {
				req.put("valid_date",ldPayServerServce.enctry(publicKeyPath,valid_date.substring(2)+valid_date.substring(0,2)));
			}else if ("cvv2".equals(elements[i])) {
				req.put("cvv2",ldPayServerServce.enctry(publicKeyPath,cvv2));
			}
			} catch (Exception e) { 
				logger.error("[联动优势快捷支付]订单{}上送加密信息{}出现异常：{}",orderId,pay_elements,e.getMessage());
			}
		}
		String signMsg=ldPayServerServce.getSignBlock(req); 
		//RSA加密
		 try {
	       	String signData =RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("[联动优势快捷支付]订单{}支付加密失败：{}",orderId,e.getMessage());
			throw new BusinessException(9001,"交易失败");
		}
		 /* 
		  * 发送交易
		  */
			String response = "";
			try {
				logger.info("[联动优势快捷支付] 订单{}订单支付请求报文",orderId);
				response = HttpConnectService.postForm(req, sendUrl);
				logger.info("[联动优势快捷支付] 订单{}订单支付响应报文",orderId);
			} catch (Exception e) { 
				logger.error("[联动优势快捷支付] 订单{}订单支付请求异常{}",orderId,e.getMessage());
				throw new BusinessException(8000,"交易异常");
			}
		
			Pattern pattern=Pattern.compile("CONTENT=\".*.\"");
			Matcher m = pattern.matcher(response); 
			boolean found = m.find(); 
			String content=""; 
			if(found)content= m.group();  
			Map<String, Object> respMap=new HashMap<String,Object>();
			if (!"".equals(content)) {
				logger.info("[联动优势快捷支付]订单{}请求支付响应报文{}", orderId, content);
				content=content.substring(9, content.length()-1);
				respMap=ldPayServerServce.getResp(content);
				String ret_code=String.valueOf(respMap.get("ret_code"));
				if ("0000".equals(ret_code)||"00200014".equals(ret_code)||"00080730".equals(ret_code)||"00060761".equals(ret_code)) {
					return "R";
				}else if("00060780".equals(ret_code)){
					return "S";
				}else{
					logger.info("[联动优势快捷支付]订单{}请求支付异常，{}", orderId,String.valueOf(respMap.get("ret_msg")));
					throw new BusinessException(8000, "交易异常");
				}
			}else{
				logger.info("[联动优势快捷支付]订单{}请求支付异常，返回content为空", orderId);
				throw new BusinessException(8000, "交易异常");
			}
	}
	 
	/**
	 * 联动优势订单查询
	 * @param orderInfo
	 * @throws BusinessException 
	 */
	public String queryQuickPay(Map<String, Object> orderInfo) throws BusinessException{
		String orderId= String.valueOf(orderInfo.get("MER_ORDERID"));
		String amt=String.valueOf(orderInfo.get("TXNAMT"));
		String mid=String.valueOf(orderInfo.get("MER_ID"));
		Map< String, String> req= new HashMap<String, String>();
		req.put("service", "mer_order_info_query");
		req.put("charset", "UTF-8");
		req.put("mer_id", ldmerid); 
		req.put("order_id",orderId);
		req.put("mer_date",new SimpleDateFormat("yyyyMMdd").format(new Date())) ;
		req.put("res_format", "HTML");
		req.put("version", "4.0");  
		req.put("order_type", "1");
		
		String signMsg=ldPayServerServce.getSignBlock(req); 
		//RSA加密
		 try {
	       	String signData =RSAUtil.signByPrivate(signMsg, hrtPrivateKey, "utf-8");
			req.put("sign", signData);
			req.put("sign_type", "RSA");
		} catch (Exception e) {
			logger.error("[联动优势快捷支付]订单{}查询加密失败：",orderId,e.getMessage());
			throw new BusinessException(9001,"交易失败");
		}
		 /* 
		  * 发送交易
		  */
			String response = "";
			try {
				logger.info("[联动优势快捷支付] 订单{}查询请求报文",orderId);
				response = HttpConnectService.postForm(req, sendUrl);
				logger.info("[联动优势快捷支付] 订单{}查询请求报文",orderId);
			} catch (Exception e) { 
				logger.error("[联动优势快捷支付] 订单{}查询请求异常{}",orderId,e.getMessage());
				throw new BusinessException(8000,"交易异常");
			}
		
			Pattern pattern=Pattern.compile("CONTENT=\".*.\"");
			Matcher m = pattern.matcher(response); 
			boolean found = m.find(); 
			String content=""; 
			if(found)content= m.group();  
			Map<String, Object> respMap=new HashMap<String,Object>();
			if (!"".equals(content)) {
				content=content.substring(9, content.length()-1);
				respMap=ldPayServerServce.getResp(content);
				String ret_code=String.valueOf(respMap.get("ret_code"));
				String ret_msg=String.valueOf(respMap.get("ret_msg"));
				String trade_state="";
				String updateSql=" update pg_wechat_txn set lmdate=sysdate,status=?,respcode=?,respmsg=? where  status<>'1' and  mer_id=? ";
				String returnCode="E";
				String status="0";
				if ("0000".equals(ret_code)) {
				    trade_state=String.valueOf(respMap.get("trade_state"));
					if ("TRADE_SUCCESS".equals(trade_state)) {
						int count =dao.update(updateSql,"1",trade_state,ret_msg, orderId);
						if("1".equals(status)&&count==1){
							// 累增快捷支付成功次数
							logger.info("[联动优势快捷支付]订单{}状态更新完成，更新数量{}", orderId,count);
							manageService.addQuickPayDayCount(orderId, null, "0");
						    quickpayService.addDayLimit(mid, amt);
						    BigDecimal bDecimal=new BigDecimal(orderInfo.get("TXNLEVEL")==null?"0":String.valueOf(orderInfo.get("TXNLEVEL")));
							BigDecimal txnLevel=orderInfo.get("TXNLEVEL")==null||"".equals(orderInfo.get("TXNLEVEL"))?BigDecimal.ONE:bDecimal ;
							orderInfo.put("TXNLEVEL", txnLevel );
							sendNotify.sendNotify(orderInfo);
							/**
							 * 支付成功后，默认执行一次提现操作
							 */
							ldPayCashService.cashQuickPay(String.valueOf(orderInfo.get("pwid")));
						}else{
							logger.info("[联动优势快捷支付]订单{}状态为1，无需更新状态", orderId);
						}
						returnCode="S";
						
					}else if("WAIT_BUYER_PAY".equals(trade_state)){
						returnCode= "R";
					}else { 
						returnCode= "E";
					}
				}else if("00200014".equals(ret_code)||"00060761".equals(ret_code)){
					returnCode= "R";
				}else{
					logger.info("[联动优势快捷支付]查询订单{}异常，{}", orderId,String.valueOf(respMap.get("ret_msg")));
					returnCode= "E";
				}
				return returnCode;
			}else{
				logger.info("[联动优势快捷支付]订单{}查询异常，返回content为空", orderId);
				throw new BusinessException(8000, "交易异常");
			}
			
	}

	
}
