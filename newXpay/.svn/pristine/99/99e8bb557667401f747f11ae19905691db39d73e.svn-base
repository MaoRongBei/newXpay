package com.hrtpayment.xpay.pos.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.channel.service.SmzfService;
import com.hrtpayment.xpay.cmbc.service.CmbcPayService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.pos.server.PosMsg;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Service
public class PosService {
	
	protected static final Logger logger = LogManager.getLogger();
	
	private ExecutorService executor = Executors.newFixedThreadPool(10);
	@Autowired
	private JdbcDao dao;
	@Autowired
	private CmbcPayService cmbc;
	@Autowired
	SmzfService smzf;
	@Autowired
	ManageService service;
	
	@Autowired
	CupsPayService cupsPayService;
	
	
	@Value("${pos.payurl}/posgetpayurlaaa_%s")
	private String payUrl;
	
	public void execute(Runnable command){
		executor.execute(command);
	}
	
//	public String getPayUrl(String mid,String orderid,String subject,BigDecimal amt,int fiid) throws BusinessException {
//		String merchantCode = cmbc.getMerchantCode(null, mid, fiid);
//		return cmbc.getQrCode(null, mid, merchantCode, subject, amt, fiid, orderid);
//	}
	
//	public String getPayUrl(String mid,String tid,String orderid,String subject,BigDecimal amt,String payway) {
//		String qrCode = xpay.getPayUrl(null, mid, amt, subject, payway,orderid);
//		dao.update("update pg_wechat_txn t set t.MER_TID=? where t.mer_orderid=? and t.MER_ID=? and t.DETAIL=?", 
//					tid,orderid,mid,subject);
//		return qrCode;
//	}
	
	public String getPayUrl2(String mid,String tid,String orderid,String subject,BigDecimal amt) throws BusinessException {
		String qrCode ="";
		String haveSql ="select mer_orderid from pg_wechat_txn where mer_orderid=? ";
		List<Map<String, Object>> list = dao.queryForList(haveSql, mid);
		if (list.size()>1)
			throw new BusinessException(7000,"订单号已存在！");
		
		String sql = "insert into pg_wechat_txn (pwid,fiid, txntype, "+
				"mer_orderid, detail, txnamt, mer_id, bankmid, respcode, respmsg, "
				+ "status, cdate, lmdate,qrcode,bk_orderid,unno,mer_tid) values"
				+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,?,'A',sysdate,sysdate,?,?,?,?)";
		int count =dao.update(sql,"",orderid,subject,amt,mid,"",
				"","","","","",tid);
		if(count>0){
			qrCode=String.format(payUrl,orderid);
		}
		return qrCode;
	}
	
	public void posBarcodePay(HrtPayXmlBean bean,PosMsg pmsg)throws BusinessException{
		String status =smzf.barcodePay(bean);
		if("S".equals(status)){
			pmsg.setBit39("00");
		}else if("R".equals(status)){
			pmsg.setBit39("01");
		}else if("E".equals(status)){
			pmsg.setBit39("25");
		}else{
			pmsg.setBit39("01");
		}
	}
	
	
	public String getCupsPayUrl(String mid,String tid,String orderid,String subject,BigDecimal amt) throws BusinessException {
		String qrCode ="";
		try {
			HrtPayXmlBean bean = new HrtPayXmlBean();
			bean.setMid(mid);
			bean.setTid(tid);
			bean.setOrderid(orderid);
			bean.setAmount(String.valueOf(amt));
			bean.setSubject(subject);
			bean.setUnno("110000");
			qrCode=cupsPayService.getCupsPayQrCode(bean);
		} catch (Exception e) {
			logger.error("银联二维码下单异常："+e);
		}

		return qrCode;
	}
	
	public String queryMerchantName(String mid) throws BusinessException {
		String sql = "select tname from Hrt_Merchacc where hrt_MID =?";
		List<Map<String, Object>> list = dao.queryForList(sql, mid);
		if (list.size()<1) throw new BusinessException(7000,"未查询到商户名称");
		Map<String, Object> map = list.get(0);
		return (String) map.get("TNAME");
	}

	public Map<String, Object> queryOrder(String orderid,String payway) throws BusinessException {
		String res =service.queryOrder(orderid);
		if("S".equals(res)||"SUCCESS".equals(res)){
			if("LMF".equals(payway)){
				String sql = "select * from pg_wechat_txn where mer_orderid=?";
				List<Map<String, Object>> list = dao.queryForList(sql, orderid);
				if (list.size()<1) throw new BusinessException(7000, "查询结果数为0");
				return list.get(0);
			}else{
				Map<String, Object> rtnMap= new HashMap<String, Object>();
				rtnMap.put("STATUS", "1");
				return rtnMap;
			}
		}else if("FAIL".equals(res)){
			throw new BusinessException(8000, "交易失败");
		}else{
			Map<String, Object> rtnMap= new HashMap<String, Object>();
			rtnMap.put("STATUS", "0");
			return rtnMap;
		}

	}
	
	public void addPosDaySumAmt(String merid,Double amt)throws BusinessException{
		service.addDayMerAmt(merid, amt);
	}

	public boolean undoTxn(String orderId, String mid, String tid, BigDecimal amt, String payway)throws BusinessException {
		try {
			String resp=cupsPayService.cupsUndo(orderId);
			JSONObject json= JSONObject.parseObject(resp);
			if("S".equals(json.get("errcode"))){
				return true;
			}else{
				return false;
			}
		} catch (Exception e) {
			logger.error("撤销交易异常："+e);
		}
		return false;
	}
}
