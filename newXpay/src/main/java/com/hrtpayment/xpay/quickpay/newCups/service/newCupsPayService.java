package com.hrtpayment.xpay.quickpay.newCups.service;


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger; 
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.quickpay.common.bean.QuickPayBean;
import com.hrtpayment.xpay.quickpay.common.service.PayService;
import com.hrtpayment.xpay.quickpay.common.service.QuickpayService;
import com.hrtpayment.xpay.quickpay.cups.util.CTime;
import com.hrtpayment.xpay.quickpay.cups.util.HttpXmlClient;
import com.hrtpayment.xpay.quickpay.newCups.bean.ChannelIssrInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayMsgBodyBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayMsgHeaderBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.CupsQuickPayRootBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.MrchntInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.OrdrInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.OriTrxInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.PyeeInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.PyerInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.RcverInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.SderInfBean;
import com.hrtpayment.xpay.quickpay.newCups.bean.TrxInfBean;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.DateUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;
import com.thoughtworks.xstream.XStream;



@Service
public class newCupsPayService  implements InitializingBean,PayService{ 

	Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao; 
	@Autowired
	ManageService manageService;
	@Autowired
	QuickpayService quickpayService;
	@Autowired
	NotifyService sendNotify;
	@Value("${cupsquickpay.Url}")
	private String Url;
	@Autowired 
	newCupsService  service;
	
//	@Value("${cupsquickpay.privateKey}")
//	private String privateKey;
	@Value("${cupsquickpay.privatekey.path}")
	private String privateKeyPath;
	
	@Value("${cupsquickpay.publickey.path}")
	private String publickeypath;
	
	
	@Value("${cupsquickpay.privateKey.passwd}")
	private String priPasswd;
	
	@Value("${cupsquickpay.privateKey.IssrId}")
	private String IssrId ;
	
	@Value("${cupsquickpay.privateKey.SignSN}")
	private String SignSN ;
	
	
//	private PrivateKey hrtPrivateKey; 
	
	@Override
	public void afterPropertiesSet() throws Exception {
//		hrtPrivateKey = RsaUtil.getPrivateKey(privateKeyPath, "PEM", "");
	}

	@Override
	public Map<String, String> getMessage(QuickPayBean bean ) throws BusinessException {
		
		String getMerInfo ="select * from (select  merchantcode,category,merchantname from  bank_merregister where fiid=62 and status='1'  order by  dbms_random.value ) where rownum =1";
		List<Map<String , Object>> merInfo=dao.queryForList(getMerInfo);
	  
		if (merInfo.size()==0) {
			logger.error("[银联快捷]订单支付，获取银行商户号失败");
			throw new HrtBusinessException(8000,"交易失败");
		}
		
		XStream xstream = new XStream(); 
		xstream.autodetectAnnotations(true);  
		 /**
		  * 
		  */
		CupsQuickPayMsgHeaderBean headerBean=new CupsQuickPayMsgHeaderBean();
		String msgTp="0003";
		headerBean.setMsgVer("1000");
		headerBean.setSndDt(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		headerBean.setTrxtyp(msgTp);
		headerBean.setIssrId(IssrId);
		headerBean.setDrctn("11");
		headerBean.setSignSN(SignSN);
//		headerBean.setEncSN("");
//		headerBean.setEncKey("");
		headerBean.setMDAlgo("0");
		headerBean.setSignEncAlgo("0");
//		headerBean.setEncAlgo("");
		
		CupsQuickPayMsgBodyBean bodyBean =new CupsQuickPayMsgBodyBean();
		/**
		 * 交易信息
		 */
		TrxInfBean trxInfBean=new TrxInfBean();
		trxInfBean.setTrxId(service.createOrder(16));//16位的序列号
		trxInfBean.setTrxDtTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
//		trxInfBean.setSettlmDt("");
		trxInfBean.setTrxAmt("CNY"+bean.getAmount());
		/**
		 * 接收方信息
		 */
		RcverInfBean rcverInfBean= new RcverInfBean();
		rcverInfBean.setRcverAcctIssrId("");//机构方无需填写
		rcverInfBean.setRcverAcctId(bean.getAccNo());//"6224243000000011");//接收方账户号6212143000000000011//
		rcverInfBean.setRcverNm(bean.getAccName());////"com");//接收方名称//
		rcverInfBean.setIDTp("01");//接收方证件类型
		rcverInfBean.setIDNo(bean.getIdCard());//"310115197803261111");//接收方证件号//
		rcverInfBean.setMobNo(bean.getMobile());//"13222222222");//接收方手机号//
		
		//接收方证件号
		//接收方预留手机号
		/**
		 * 发起方 信息
		 */
		SderInfBean sderInfBean=new SderInfBean();
		sderInfBean.setSderIssrId(IssrId);
		sderInfBean.setSderAcctIssrId(IssrId);
		
		
		MrchntInfBean mrchntBean=new MrchntInfBean();
		mrchntBean.setMrchntNo(String.valueOf(merInfo.get(0).get("merchantcode")));//"QC4864100000101");//商戶編碼 mid 
		mrchntBean.setMrchntTpId(String.valueOf(merInfo.get(0).get("category")));//商户类别 mer_type ?
		mrchntBean.setMrchntPltfrmNm(String.valueOf(merInfo.get(0).get("merchantname")));//商户名称 mer_name ?
		
		bodyBean.setBizTp("300001");//生产上 具体选择一个TP
		bodyBean.setTrxInf(trxInfBean);
		bodyBean.setRcverInf(rcverInfBean);
		bodyBean.setSderInf(sderInfBean);
		bodyBean.setMrchntInf(mrchntBean);
		CupsQuickPayRootBean rootBean=new CupsQuickPayRootBean();
		rootBean.setMsgHeader(headerBean);
		rootBean.setMsgBody(bodyBean); 
		
		/**
		 * 生成摘要
		 */
		String root=xstream.toXML(rootBean);
		String encode = service.getEncodeSHA(root);
		
		String SmsKey="";
		String send ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+root+"{S:"+encode+"}";
		try {
			logger.info("[银联快捷]获取短信验证码发送报文 ",send);
			String resp=HttpXmlClient.post(Url, send,msgTp);
			logger.info("[银联快捷]获取短信验证码响应报文",resp);
			
			Map<String, String> respMap=service.xmlToMap(resp);
			SmsKey=respMap.get("Smskey"); 
		} catch (Exception e) {
			logger.info("[银联快捷]获取短信验证码发送异常返回："+e );
			RedisUtil.addFailCountByRedis(1);
			throw new HrtBusinessException(8000,"获取验证码失败");
		}
		if ("".equals(SmsKey)) {
			throw new HrtBusinessException(8000,"获取验证码失败");
		}
		Map<String, String> rtnMap=new HashMap<String, String>();
		rtnMap.put("SmsKey", SmsKey);
		rtnMap.put("bankMid", String.valueOf(merInfo.get(0).get("merchantcode")));
		return  rtnMap;
	}



	@Override
	public String queryOrder(Map<String, Object> orderInfo,String type) throws BusinessException {
		
		String order="";
		String orderid=String.valueOf(orderInfo.get("mer_orderid"));
		int locate=orderid.indexOf(DateUtil.getOtherDate("yyyy"));
	    order=orderid.substring(locate+4,locate+20);
		
		XStream xstream = new XStream();  
		xstream.autodetectAnnotations(true);  
		 /**
		  * 
		  */
		CupsQuickPayMsgHeaderBean headerBean=new CupsQuickPayMsgHeaderBean();
		String msgTp="3101";
		headerBean.setMsgVer("1000");
		headerBean.setSndDt(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		headerBean.setTrxtyp(msgTp);
		headerBean.setIssrId(IssrId);
		headerBean.setDrctn("11");
		headerBean.setSignSN(SignSN);
		headerBean.setMDAlgo("0");
		headerBean.setSignEncAlgo("0");
		
		CupsQuickPayMsgBodyBean bodyBean =new CupsQuickPayMsgBodyBean();
		/**
		 * 交易信息 
		 */
		TrxInfBean trxInfBean=new TrxInfBean();
		trxInfBean.setTrxId(order);
		trxInfBean.setTrxDtTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		/**
		 * 发起方 信息
		 */

	    OriTrxInfBean oriTrxInfBean=new OriTrxInfBean();
	    oriTrxInfBean.setOriTrxId(order);
	    try {   
	    	
	    	String date =DateUtil.formartDate(String.valueOf(orderInfo.get("time_end")), "yyyyMMddHHmmss", "yyyy-MM-dd'T'HH:mm:ss");
			oriTrxInfBean.setOriTrxDtTm(date);
		} catch (Exception e1) {
			logger.info("[银联快捷]订单查询获取日期异常{}",e1.getMessage());
			throw new HrtBusinessException(1002,"获取日期异常");
		}
	    oriTrxInfBean.setOriBizTp("300001");
	    oriTrxInfBean.setOriTrxTp(type);//2001 直接付款 "1002"  退款 "1101"
		
		SderInfBean sderInfBean =new SderInfBean();
		sderInfBean.setSderIssrId(IssrId);
		sderInfBean.setSderAcctIssrId(IssrId);
		bodyBean.setBizTp("300001");
		bodyBean.setTrxInf(trxInfBean);
		bodyBean.setOriTrxInf(oriTrxInfBean);
		bodyBean.setSderInf(sderInfBean); 
		
		CupsQuickPayRootBean rootBean=new CupsQuickPayRootBean();
		rootBean.setMsgHeader(headerBean);
		rootBean.setMsgBody(bodyBean);
		
		/**
		 * 生成摘要
		 */
		String root=xstream.toXML(rootBean);
		String encode = service.getEncodeSHA(root);
 
		
		String send ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+root+"{S:"+encode+"}";
		 
		try {
 			logger.info("[银联快捷]订单查询发送报文");
			String resp=HttpXmlClient.post(Url, send,msgTp);
			logger.info("[银联快捷]订单查询响应报文",resp);
			Map<String, String> respMap=service.xmlToMap(resp);
			String SysRtnCd=respMap.get("SysRtnCd");//响应码
			String SysRtnDesc=respMap.get("SysRtnDesc");//响应原因
		
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date date=formatter.parse(respMap.get("SysRtnTm"));//("2018-06-28T16:46:59");
			SimpleDateFormat result_form = new SimpleDateFormat("yyyyMMddHHmmss");
			String timeend=result_form.format(date);
			if ("00000000".equals(SysRtnCd) ) {
				logger.info("[银联快捷支付]订单{}查询成功 ", orderid);
				String updateOrder="update pg_wechat_txn set time_end=?,status=?,lmdate=sysdate,bk_orderid=?,respcode=?,respmsg=? where status<>'1' and mer_orderid=?";
				int count=dao.update(updateOrder, timeend,"1",respMap.get("TrxId"),SysRtnCd,SysRtnDesc,orderid);
				if (count==1) {
					// 累增快捷支付成功次数
					manageService.addQuickPayDayCount(orderid, null, "0");
				    quickpayService.addDayLimit(String.valueOf(orderInfo.get("mer_id")), String.valueOf(orderInfo.get("txnamt")));
				    BigDecimal bDecimal=new BigDecimal(orderInfo.get("TXNLEVEL")==null?"0":String.valueOf(orderInfo.get("TXNLEVEL")));
					BigDecimal txnLevel=orderInfo.get("TXNLEVEL")==null||"".equals(orderInfo.get("TXNLEVEL"))?BigDecimal.ONE:bDecimal ;
					orderInfo.put("TXNLEVEL", txnLevel );
					sendNotify.sendNotify(orderInfo);
				}
				return "S";
			}else if ("ES000033".equals(SysRtnCd) ) {
				String updateOrder="update pg_wechat_txn set status=?,lmdate=sysdate,bk_orderid=?,respcode=?,respmsg=? where status<>'1' and mer_orderid=?";
				dao.update(updateOrder,  "0",respMap.get("TrxId"),SysRtnCd,SysRtnDesc,orderid);
				return "R";
			}else {
				String updateOrder="update pg_wechat_txn set status=?,lmdate=sysdate,bk_orderid=?,respcode=?,respmsg=? where status<>'1' and mer_orderid=?";
				dao.update(updateOrder, "6",respMap.get("TrxId"),SysRtnCd,SysRtnDesc,orderid);
				return "E";
			}
		} catch (Exception e) {
			logger.info("[银联快捷支付]订单{}查询异常，异常原因：{}",orderid,e.getMessage() );
			RedisUtil.addFailCountByRedis(1);
			return  "R";
		}
		
	}

	@Override
	public String refundQueryOrder(Map<String, Object> orderInfo) throws BusinessException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String refund(String orderid, BigDecimal amount, Map<String, Object> oriMap) {
		
		String getAccno="select accno_encrypt eaccno from hrt_qkpaycard  where qpcid=? ";
		List<Map<String, Object>>  accNoList=dao.queryForList(getAccno, oriMap.get("isscode"));
		int locate=orderid.indexOf(DateUtil.getOtherDate("yyyy"));
		String order=orderid.substring(locate+4,locate+20);
		String accno=service.decodeByAES(String.valueOf(accNoList.get(0).get("eaccno")));
		
		String getMerInfo ="select * from (select  category,merchantname from  bank_merregister where fiid=62 and merchantcode=?  ) where rownum =1";
		List<Map<String , Object>> merInfo=dao.queryForList(getMerInfo,oriMap.get("bankmid"));
	  
		if (merInfo.size()==0) {
			logger.error("[银联快捷]订单支付，获取银行商户号失败");
			throw new HrtBusinessException(8000,"交易失败");
		}
		XStream xstream = new XStream();  
		xstream.autodetectAnnotations(true);  
		 /**
		  * 
		  */
		CupsQuickPayMsgHeaderBean headerBean=new CupsQuickPayMsgHeaderBean();
		String msgTp="1101";
		headerBean.setMsgVer("1000");
		headerBean.setSndDt(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		headerBean.setTrxtyp(msgTp);
		headerBean.setIssrId(IssrId);
		headerBean.setDrctn("11");
		headerBean.setSignSN(SignSN);
		headerBean.setMDAlgo("0");
		headerBean.setSignEncAlgo("0");
		
		CupsQuickPayMsgBodyBean bodyBean =new CupsQuickPayMsgBodyBean();
		/**
		 * 交易信息
		 */
		TrxInfBean trxInfBean=new TrxInfBean();
		trxInfBean.setTrxId(order);//"0131202721000000");//
		trxInfBean.setTrxDtTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		String s_amount=String.valueOf(oriMap.get("txnamt"));
		BigDecimal b_amount = BigDecimal.valueOf(Double.parseDouble(s_amount)); 
	    DecimalFormat df = new DecimalFormat("#0.00");
		trxInfBean.setTrxAmt("CNY"+df.format(amount));//退款金额
		//
		trxInfBean.setTrxTrmTp("08");//交易終端類型  08 手機 
		/**
		 *  原订单信息
		 */ 
 
		OriTrxInfBean oriTrxInfBean=new OriTrxInfBean();
		oriTrxInfBean.setOriTrxId(String.valueOf(oriMap.get("bk_orderid")));
		oriTrxInfBean.setOriTrxAmt("CNY"+df.format(b_amount));//
		oriTrxInfBean.setOriOrdrId(String.valueOf(oriMap.get("bk_orderid")));
//		oriTrxInfBean.setOriTrxDtTm("2018-05-18T18:05:07");
		try {
			String date =DateUtil.formartDate(String.valueOf(oriMap.get("time_end")), "yyyyMMddHHmmss", "yyyy-MM-dd'T'HH:mm:ss");
			oriTrxInfBean.setOriTrxDtTm(date);
		} catch (Exception e1) {
			logger.info("[银联快捷]订单查询获取日期异常{}",e1.getMessage());
			throw new HrtBusinessException(1002,"获取日期异常");
		}
		
		PyerInfBean pyerInfBean=new PyerInfBean();
//		pyerInfBean.setPyerAcctId("6212143000000000010");//
		pyerInfBean.setPyerAcctIssrId(IssrId);
		pyerInfBean.setPyeeIssrId(IssrId);
		
		PyeeInfBean pyeeInfBean=new PyeeInfBean();
		pyeeInfBean.setPyeeAcctIssrId(IssrId);
		pyeeInfBean.setPyeeIssrId(IssrId);
		pyeeInfBean.setPyeeAcctId(accno);//"6224243000000011");//?卡号怎么取？
		
		OrdrInfBean ordrInfBean=new OrdrInfBean();
		ordrInfBean.setOrdrId(orderid);//"201803261000000002");//
		
		MrchntInfBean mrchntBean=new MrchntInfBean();
		mrchntBean.setMrchntNo(String.valueOf(oriMap.get("bankmid")));//"QC4864100000101");//商戶編碼 
		mrchntBean.setMrchntTpId(String.valueOf(merInfo.get(0).get("category")));//"0020");//商户类别
		mrchntBean.setMrchntPltfrmNm(String.valueOf(merInfo.get(0).get("merchantname")));//"和融通");//商户名称
		
		ChannelIssrInfBean channelIssrInfBean=new ChannelIssrInfBean();
		channelIssrInfBean.setSgnNo("");//签约协议号
		
		
		bodyBean.setBizTp("300001");
		bodyBean.setTrxInf(trxInfBean); 
		bodyBean.setPyerInf(pyerInfBean);
		bodyBean.setPyeeInf(pyeeInfBean);
		bodyBean.setOriTrxInf(oriTrxInfBean);
		bodyBean.setOrdrInf(ordrInfBean);
		bodyBean.setMrchntInf(mrchntBean);
		bodyBean.setChannelIssrInf(channelIssrInfBean);
		
		CupsQuickPayRootBean rootBean=new CupsQuickPayRootBean();
		rootBean.setMsgHeader(headerBean);
		rootBean.setMsgBody(bodyBean);
		
		
		/**
		 * 生成摘要
		 */
		String root=xstream.toXML(rootBean);
		String encode = service.getEncodeSHA(root);
		
		String send ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+root+"{S:"+encode+"}";
		 
		try {
			logger.info("[银联快捷]订单{}退款发送报文",orderid);
			String resp=HttpXmlClient.post(Url, send,msgTp);
			logger.info("[银联快捷]订单{}退款接收报文 ",orderid);
			Map<String, String> respMap=service.xmlToMap(resp);
			String SysRtnCd=respMap.get("SysRtnCd");//TrxId
			String SysRtnDesc=respMap.get("SysRtnDesc");//TrxId
			String updateOrder="update pg_wechat_txn set time_end=?,status='0',lmdate=sysdate,bk_orderid=?,respcode=?,respmsg=? where status<>'1' and mer_orderid=?";
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date date=formatter.parse(respMap.get("SysRtnTm"));
			SimpleDateFormat result_form = new SimpleDateFormat("yyyyMMddHHmmss");
			String timeend=result_form.format(date);
			int count=dao.update(updateOrder,timeend,respMap.get("TrxId"),SysRtnCd,SysRtnDesc,orderid);
			if (count==1) {
				if ("00000000".equals(SysRtnCd)||"ES000033".equals(SysRtnCd) ) {
					return "R"; 
				}else {
					return "E";
				}
			}else if (count==0){
				logger.info("[银联快捷]退款 订单{}没有进行更新，返回Map={},",order,respMap );
				return "R";
			}else{
				return "E";
			}
		} catch (Exception e) {
			logger.error("[银联快捷]退款 订单{}发送异常返回：{}",orderid,e.getMessage());
			RedisUtil.addFailCountByRedis(1);
			return "E";
		}
	}
 
	
	/**
	 * 
	 * 
	 * 
	 */
	@Override
	public String authPay(String orderId, String amount, String gateId, String accno, String mer_id, String unno,
			String qpcid, String ispoint) throws BusinessException {
 
		String checkOrder="select pwid from pg_wechat_Txn where mer_orderid=?";
		List<Map<String, Object>> checkOrdList=dao.queryForList(checkOrder, orderId);
		if (checkOrder.length()==0) {
			logger.info("[银联快捷支付]{}:订单号已存在,请重新下单",orderId);
			return "E";
		}
		
		String initOrder="insert into pg_wechat_txn (pwid,cdate,lmdate,status,txntype,fiid,detail,trantype,ispoint,mer_orderid,txnamt,mer_id,mer_tid,accno,isscode,unno)"
				+ " values (S_PG_Wechat_Txn.nextval,sysdate,sysdate,'A',0,62,'快捷支付',8,1,?,?,?,?,?,?,?) ";
		int count=dao.update(initOrder, orderId,amount,mer_id,gateId==null||"".equals(gateId)||"null".equals(gateId)?"":gateId,accno,qpcid,unno);
		if (count!=0) {
			logger.info("[银联快捷支付]订单：{}初始化完成",orderId);
			return "S";
		}else{
			logger.info("[银联快捷支付]订单：{}初始化失败",orderId);
			return "E";
		} 
	}

	@Override
	public String pay(QuickPayBean bean, String orderid, String authcode,String accno) throws BusinessException {
		logger.info(bean.getBankmid());
		String getMerInfo ="select * from (select  category,merchantname from  bank_merregister where fiid=62 and merchantcode=?  ) where rownum =1";
		List<Map<String , Object>> merInfo=dao.queryForList(getMerInfo,bean.getBankmid());
	  
		if (merInfo.size()==0) {
			logger.error("[银联快捷]订单支付，获取银行商户号失败");
			throw new HrtBusinessException(8000,"交易失败");
		}
		
		XStream xstream = new XStream();  
		xstream.autodetectAnnotations(true);  
		 /**
		  * 
		  */
		CupsQuickPayMsgHeaderBean headerBean=new CupsQuickPayMsgHeaderBean();
		String msgTp="1002";
		headerBean.setMsgVer("1000");
		headerBean.setSndDt(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		headerBean.setTrxtyp(msgTp);
		headerBean.setIssrId(IssrId);
		headerBean.setDrctn("11");
		headerBean.setSignSN(SignSN);
		headerBean.setMDAlgo("0");
		headerBean.setSignEncAlgo("0");
		
		CupsQuickPayMsgBodyBean bodyBean =new CupsQuickPayMsgBodyBean();
		/**
		 * 交易信息
		 */
		TrxInfBean trxInfBean=new TrxInfBean();
		String order="";
		int locate=orderid.indexOf(DateUtil.getOtherDate("yyyy"));
	    order=orderid.substring(locate+4,locate+20);
		trxInfBean.setTrxId(order);
		trxInfBean.setTrxDtTm(new CTime().formatDate(new Date(), "yyyy-mm-ddTHH:MM:SS"));
		trxInfBean.setTrxAmt("CNY"+bean.getAmount());
		trxInfBean.setTrxTrmTp("08");//交易終端類型  08 手機 
		/**
		 * 发起方 信息
		 */

		OriTrxInfBean oriTrxInfBean=new OriTrxInfBean();
		oriTrxInfBean.setOriTrxId(order);
		
		PyerInfBean pyerInfBean=new PyerInfBean();
		pyerInfBean.setPyerAcctId(accno);//"6224243000000011");// 
		pyerInfBean.setAuthMsg(authcode);
		pyerInfBean.setSmskey(bean.getPreSerial());
		
		PyeeInfBean pyeeInfBean=new PyeeInfBean();
		pyeeInfBean.setPyeeAcctIssrId(IssrId);
		pyeeInfBean.setPyeeIssrId(IssrId);
		
		OrdrInfBean ordrInfBean=new OrdrInfBean();
		ordrInfBean.setOrdrId(orderid);
		
		MrchntInfBean mrchntBean=new MrchntInfBean();
		mrchntBean.setMrchntNo(bean.getBankmid());//"864001158121883");//"QC4864100000101");//商戶編碼 mid 
		mrchntBean.setMrchntTpId(String.valueOf(merInfo.get(0).get("category")));//"5812");//商户类别 mer_type ?
		mrchntBean.setMrchntPltfrmNm(String.valueOf(merInfo.get(0).get("merchantname")));//"北京肥得捞餐饮管理有限公司");//商户名称 mer_name ?	

		bodyBean.setBizTp("300001");//改100003
		bodyBean.setTrxInf(trxInfBean);
		bodyBean.setPyerInf(pyerInfBean);
		bodyBean.setPyeeInf(pyeeInfBean);
		bodyBean.setOriTrxInf(oriTrxInfBean);
		bodyBean.setOrdrInf(ordrInfBean);
		bodyBean.setMrchntInf(mrchntBean);
		
		CupsQuickPayRootBean rootBean=new CupsQuickPayRootBean();
		rootBean.setMsgHeader(headerBean);
		rootBean.setMsgBody(bodyBean);
		
		/**
		 * 生成摘要
		 */
		String root=xstream.toXML(rootBean);
		String encode = service.getEncodeSHA(root);
		
		String send ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+root+"{S:"+encode+"}";
		try {
 			logger.info("[银联快捷]订单支付发送报文");
			String resp=HttpXmlClient.post(Url, send,msgTp);
			logger.info("[银联快捷]订单支付接收报文 ");
			Map<String, String> respMap=service.xmlToMap(resp);
			String SysRtnCd=respMap.get("SysRtnCd");
			String SysRtnDesc=respMap.get("SysRtnDesc");
			String updateOrder="update pg_wechat_txn set bankmid=?,time_end=?,status='0',lmdate=sysdate,bk_orderid=?,respcode=?,respmsg=? where status='A' and mer_orderid=?";
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date date=formatter.parse(respMap.get("SysRtnTm"));//formatter.parse("2018-06-28T16:46:59");//
			SimpleDateFormat result_form = new SimpleDateFormat("yyyyMMddHHmmss");
			String timeend=result_form.format(date);
			int count=dao.update(updateOrder, bean.getBankmid(),timeend,respMap.get("TrxId"),SysRtnCd,SysRtnDesc,orderid);
			if (count==1) {
				if ("00000000".equals(SysRtnCd)||"ES000033".equals(SysRtnCd) ) {
					return "R"; 
				}else {
					return "E";
				}
			}else if (count==0){
				logger.error("[银联快捷支付] 订单{}没有进行更新，返回Map={},",order,respMap );
				return "R";
			}else{
				return "E";
			}
		} catch (Exception e) {
			logger.error("[银联快捷支付] 订单{}发送异常返回：{}",order,e.getMessage());
			RedisUtil.addFailCountByRedis(1);
			return "E";
		}
	}



	
	
	
}
