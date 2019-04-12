package com.hrtpayment.xpay.channel.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.Callback;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.common.service.impl.NettyClientService;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.DateUtil;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.crypto.Md5Util;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Service
public class NotifyService {
	private final Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao;
	@Autowired
	ManageService manageService;
	
	@Autowired
	NettyClientService client;
	
	@Value("${xpay.special.unno}")
	String specUnno;
	@Value("${xpay.maxAmtForTerms.wx}")
	long wxMaxAmt;
	@Value("${xpay.maxAmtForTerms.zfb}")
	long zfbMaxAmt;
	@Value("${xpay.sytQt.url}")
	String sytUrl;
	

	@Scheduled(fixedRate=300000)
	private void sendNotify() {
		String sql = "select (sysdate-t.lmdate)*24*12 minutes,t.* from pg_wechat_txn t where t.cdate<sysdate "
				+ "and t.status='1' and t.txnlevel is not null and t.txnlevel<13  and t.txntype='0'  and  sysdate -1/24<t.lmdate ";
		List<Map<String, Object>> list = dao.queryForList(sql);
//		logger.info("待发送异步通知个数:{}",list.size());
		for (Map<String, Object> map : list) {
			BigDecimal pwid = (BigDecimal) map.get("PWID");
			BigDecimal minutes = (BigDecimal) map.get("MINUTES");
			BigDecimal txnLevel = (BigDecimal) map.get("TXNLEVEL");
			BigDecimal level = new BigDecimal(minutes.add(BigDecimal.ONE).intValue());
//			logger.info("minutes:{},level:{},txnlevel:{}",minutes,level,txnLevel);
			if (minutes.compareTo(txnLevel)>0 && pwid != null) {
				updateTxnLevel(pwid, level + "");
				map.put("TXNLEVEL",level);
				String sucTime="";
				try {
					sucTime=DateUtil.formartDate(String.valueOf(map.get("TIME_END")), DateUtil.FORMAT_TRADETIME, DateUtil.FORMAT_DATETIME);
				} catch (Exception e) {
					logger.error("[异步通知]--自动发送 处理时间异常{}", e.getMessage());
					sucTime=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
				}
				map.put("SUCTIME",sucTime);
				map.put("RTNCODE",map.get("RESPCODE"));
				map.put("RTNMSG",map.get("RESPMSG"));
				sendNotify(map);
			}
		}
	}
	
//	@Scheduled(fixedRate=60000)
	public void quartzQueryBarcodePay(){
		String sql="select s.mer_orderid from pg_wechat_txn s where s.respcode='USERPAYING'"
				//	+ " and s.lmdate  < sysdate - interval '1' minute"
				//	+ " and s.unno='110000'"
					+ " and s.lmdate>sysdate-interval '5' minute"
					+ " and s.lmdate between trunc(sysdate) and trunc(sysdate+1)";
		List<Map<String, Object>> list = dao.queryForList(sql);
		if(list.size()>0){
			logger.info("[定时执行条码处理中订单查询]："+list.toString());
			for(int i=0;i<list.size();i++){
				String orderid="";
				try {
					orderid=String.valueOf(list.get(i).get("mer_orderid"));
					manageService.queryOrder(orderid);
				} catch (Exception e) {
					logger.error("定时条码订单查询异常]："+orderid,e);
				}

			}
		}

	}
	/**
	 * 
	 * 2018-12-14 新增
	 * 
	 * 定时关闭20分钟前的 unno 为null的 未支付订单
	 * 
	 */
//	@Scheduled(fixedRate=60000)
	public  void quartCloseOrder(){
		String  closeOrdSql="select  mer_orderid,mer_id,cdate,trantype,txnamt,respmsg from pg_wechat_txn where 1=1"
				//+ " and lmdate >sysdate-5"
//				+ " and lmdate between trunc(sysdate,'dd') and sysdate  "
//				+ " and lmdate<sysdate -interval '90' minute "
                + " and lmdate between sysdate -interval '100' minute  and sysdate -interval '90' minute "
				+ " and fiid in (53,54,60,61) "
				+ " and ((trantype='1' and trade_type<>'BS') OR trantype='2')"
				+ " and (status='0' or status='A' ) and status not in ('5','6')  "
				+ " and (unno is null or unno in ('110001','130000','130001'))"
				+ " and txntype=0   ";
		List<Map<String, Object>> list = dao.queryForList(closeOrdSql);
		if(list.size()>0){
			logger.info("[定时执行订单关闭]："+list.toString());
			for(int i=0;i<list.size();i++){
				String orderid="";
				try {
					orderid=String.valueOf(list.get(i).get("mer_orderid"));
					String trantype= String.valueOf(list.get(i).get("trantype"));
					String status=manageService.closeOrder(orderid);
					if (("DOING".equals(status)&&"2".equals(trantype))||("SUCCESS".equals(status)&&"1".equals(trantype))) {
						String mid=list.get(i).get("mer_id")+"";
						if (mid.compareTo("HRTSYT")>0) {
//							Map<String , String>  formMap = new HashMap<String,String>();
					     	JSONObject formMap=new JSONObject();
							formMap.put("mid", mid);
							formMap.put("orderid", String.valueOf(list.get(i).get("mer_orderid")));
							formMap.put("trantype", String.valueOf(list.get(i).get("trantype")));
							formMap.put("amount", String.valueOf(list.get(i).get("txnamt")));
							formMap.put("trantime", String.valueOf(list.get(i).get("cdate")));
//							HttpConnectService.postForm(formMap, "http://10.51.130.155:7086/sytwechatservice/tempMsg/sendQuestionnaire");
							client.sendJson(sytUrl, formMap.toJSONString());
						}
					} 
				} catch (Exception e) {
					logger.error("[定时执行订单关闭]："+orderid,e);
				}

			}
		}
	}
	
	
//	@Scheduled(fixedRate=3600000)
	public void quartzQuerySuccessOrder(){
		String sql="select s.mer_orderid from pg_wechat_txn s where  s.status='0'"
				+ " and s.lmdate<sysdate-interval '5' minute"	
				+ " and s.txntype='1' and s.lmdate between trunc(sysdate-1) and trunc(sysdate+1)";
		List<Map<String, Object>> list = dao.queryForList(sql);

		if(list.size()>0){
			logger.info("[定时查询退款处理中的订单]--------->："+list.toString());
			for(int i=0;i<list.size();i++){
				String orderid="";
				try {
					orderid=String.valueOf(list.get(i).get("MER_ORDERID"));
					String resp=manageService.refundQuery(orderid);
					logger.info("退款订单查询响应结果--------->",resp);
				} catch (Exception e) {
					logger.error("定时查询退款订单异常]："+orderid,e);
				}

			}
		}
	}

	public void sendNotify(Map<String, Object> map) {
		String unno = (String) map.get("UNNO");
		String mid = (String) map.get("MER_ID");
		String tid=(String) map.get("MER_TID");
		String rtncode = (String) map.get("RTNCODE");
		String rtnmsg = (String) map.get("RTNMSG");
		String bankType=(String) map.get("BANKTYPE");
		String userId=(String) map.get("USERID");
		String trantype=String.valueOf(map.get("TRANTYPE"));
		/*
		 *2018-12-03 修改
		 *
		 * 异步通知 新增字段 交易完成时间  sucTime
		 * 格式： YYYY-MM-DD HH24:MI:SS
		 * 
		 */
		String sucTime=String.valueOf(map.get("SUCTIME"));
		//new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(map.get("LMDATE")); //
		final String orderid = (String) map.get("MER_ORDERID");
		final BigDecimal pwid = (BigDecimal) map.get("PWID");
		if (unno == null || "".equals(unno)||"130000|130001|110001".contains(unno)) {
			logger.info("unno为空,无法进行推送");
			/*
			 * 2018-12-06 修改
			 * 
			 * 秒到APP交易校验
			 * 订单交易成功后  redis内未支付笔数减一 
			 *
			 */
			try {
				String payway="";
				if ("1".equals(trantype)) {
					payway="WX";
				}else if("2".equals(trantype)){
					payway="ZFB";
				}
				RedisUtil.cutCountPayByMid(mid+payway);
			} catch (BusinessException e) {
				logger.error("未支付笔数扣减失败");
			}
			/*
			 * 2018-12-11 修改
			 * 
			 * 手刷 商户交易成功后做金额累增
			 * 
			 * 手刷订单号 开头 qrpay
			 * 按照trantype 累增金额
			 * trantype=1 微信
			 * trantype=2 支付宝
			 * 
			 * 暂缓使用
			 * wx、zfb金额限制最大值  存放在 redis内
			 * 利用redis进行操作
			 */
			Double amt=Double.valueOf(String.valueOf(map.get("TXNAMT")));
			unno=unno==null?"":unno;
			try {
				if (orderid.contains("qrpay")||"130000|130001|110001".contains(unno)) {
					if ("1".equals(trantype)) {
//						wxMaxAmt= Long.parseLong(RedisUtil.getProperties("wxLimit"));
						RedisUtil.addAmtByGroupName("WX",amt, wxMaxAmt);
						logger.info("和融通大额微信订单{}，交易金额{}",orderid,amt);
					}else if("2".equals(trantype)){
//						zfbMaxAmt= Long.parseLong(RedisUtil.getProperties("zfbLimit"));
						RedisUtil.addAmtByGroupName("ZFB",amt,zfbMaxAmt);
						logger.info("和融通大额支付宝订单{}，交易金额{}",orderid,amt);
					}
				}
				
			} catch (BusinessException e) {
				logger.error("和融通大额成功交易累增金额失败，{}",e.getMessage());
			}
			
			updateTxnLevel(pwid, "");
			return;
		}
		if (orderid == null || "".equals(orderid)) {
			logger.info("订单号为空,无法进行推送");
			updateTxnLevel(pwid, "");
			return;
		}
		if (mid == null || "".equals(mid)) {
			logger.info("mid为空,无法进行推送");
			updateTxnLevel(pwid, "");
			return;
		}
		BigDecimal amount = (BigDecimal) map.get("TXNAMT");
		if (amount == null) {
			logger.info("金额为空,无法进行推送");
			updateTxnLevel(pwid, "");
			return;
		}
		BigDecimal level = (BigDecimal) map.get("TXNLEVEL");
		if (level == null) {
			level = BigDecimal.ONE;
		}
		
//		if((unno.equals("880000")&&level.intValue()==1)||(unno.equals("962073")&&level.intValue()==1)){
		if (specUnno.contains(unno)&&level.intValue()==1) {
			try {
				manageService.addDayMerAmtForLMF(mid,amount.doubleValue());
			} catch (BusinessException e) {
				logger.error("限额累增异常----->"+mid,e);
			}
		}


		String unnoSql = "select * from HRT_XPAYORGINFO where unno=?";
		List<Map<String, Object>> list = dao.queryForList(unnoSql, map.get("UNNO"));
		if (list.size() < 1) {
			logger.info("unno:{},未查询到相应信息,无法进行推送");
			return;
		}
		Map<String, Object> unnoMap = list.get(0);
		if (!"1".equals(unnoMap.get("STATUS"))) {
			logger.info("unno:{},机构状态为不可用,无法进行推送");
			return;
		}
		String key = (String) unnoMap.get("MACKEY");
		String url = (String) unnoMap.get("CALLBACKURL");
		if (key == null || "".equals(key)) {
			logger.info("密钥为空,无法进行推送");
			return;
		}
		if (url == null || "".equals(url)) {
			logger.info("url为空,无法进行推送");
			return;
		}
		String time="";
		Map<String,String> signMap= new HashMap<String,String>();
		StringBuilder sb = new StringBuilder();
		signMap.put("amount", String.valueOf(amount));
//	    if("880000".equals(unno)||"962073".equals(unno)){
		if (specUnno.contains(unno)) {
			if (!"".equals(bankType)&&null!=bankType&&!"null".equals(bankType)) {
				signMap.put("bankType", bankType);
			}
		}
	    signMap.put("mid", mid);
	    signMap.put("orderid", orderid);
	    if(trantype!=null && !"null".equals(trantype)){
		    signMap.put("paymode", trantype);
	    }
//		if("880000".equals(unno)||"962073".equals(unno)){
	    if (specUnno.contains(unno)) {
			if (!"".equals(rtncode)&&null!=rtncode&&!"null".equals(rtncode)) {
				signMap.put("rtnCode", rtncode);
			}
			if (!"".equals(rtnmsg)&&null!=rtnmsg&&!"null".equals(rtnmsg)) {
				signMap.put("rtnMsg", rtnmsg);
			}
		}
		signMap.put("status", "S");
		if (sucTime!=null&&!"null".equals(sucTime)&&!"".equals(sucTime)) {
			signMap.put("payOrderTime", sucTime);
		}
		if("110000".equals(unno)){
			signMap.put("tid", tid);
			if(map.get("time")!=null&&!"".equals(map.get("time"))){
				time=String.valueOf(map.get("time"));
				signMap.put("time", map.get("time")+"");
			}else if(map.get("LMDATE")!=null){
				time =new SimpleDateFormat("yyyyMMddHHmmss").format(map.get("LMDATE"));
				signMap.put("time", time);
			}else{
				signMap.put("time", time);
			}
			if("18".equals(String.valueOf(map.get("FIID")))){
				String accno =(String)map.get("ACCNO");
				String issCode =(String)map.get("ISSCODE");
				String voucherNum =(String)map.get("BK_ORDERID");
				signMap.put("accNo", accno);
				signMap.put("issCode", issCode);
				signMap.put("voucherNum", voucherNum);
				signMap.put("acqCode", "48640000");
			}
		}
		signMap.put("unno", unno);
//		if("880000".equals(unno)||"962073".equals(unno)){
		if (specUnno.contains(unno)) {	
			if (!"".equals(userId)&&null!=userId&&!"null".equals(userId)) {
				signMap.put("userId", userId);
			}
			
		}
		String sign =SimpleXmlUtil.getMd5Sign(signMap,key);
		signMap.put("sign", sign);

		String xml =SimpleXmlUtil.map2xml(signMap);
		logger.info("{}第{}次推送,url:{}", orderid, level.intValue(), url);
		try {
			client.sendAsyncXml(url, xml.toString(), new Callback<String>() {

				@Override
				public void resp(String t) {
					try {
						Map<String,String> xmlMap =SimpleXmlUtil.xml2map(t);
						if ("S".equals(xmlMap.get("respstatus"))) {
							logger.info("{}推送返回成功",orderid);
							updateTxnLevel(pwid, "");
						} else {
							if (t.matches("<\\??xml.+")) {
								logger.info("{}推送返回报文错误:{}",orderid,t);
							} else {
								logger.info("{}推送返回报文错误:{}...",orderid,t.length()>30?t.substring(0, 30):t);
							}
						}
					} catch (Exception e) {
						logger.info(e.getMessage());
					}

				}

				@Override
				public void catchTimeOut(String t) {
					
					logger.info("{}通知异常错误:{}",orderid,t);
				}
			});
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
	}

	public void sendNotify(String pwid) {
		String sql = "select * from pg_wechat_txn where pwid=?";
		List<Map<String, Object>> list = dao.queryForList(sql, pwid);
		logger.info(list.size());
		if (list.size() > 0) {
			sendNotify(list.get(0));
		}
	}
	
	public void sendQueryNotify(String orderid) {
		String sql = "select * from pg_wechat_txn where mer_orderid=? and status=?";
		List<Map<String, Object>> list = dao.queryForList(sql, orderid,"1");
		logger.info(list.size());
		if (list.size() > 0) {
			sendNotify(list.get(0));
		}
	}

	public void updateTxnLevel(BigDecimal pwid, String txnLevel) {
		String sql = "update pg_wechat_txn set txnlevel=? where pwid=?";
		dao.update(sql, txnLevel, pwid);
	}
}
