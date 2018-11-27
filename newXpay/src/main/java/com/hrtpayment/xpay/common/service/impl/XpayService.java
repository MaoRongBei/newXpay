package com.hrtpayment.xpay.common.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.baidu.service.BaiduPayService;
import com.hrtpayment.xpay.bcm.service.BcmPayService;
import com.hrtpayment.xpay.cib.service.CibPayService;
import com.hrtpayment.xpay.cmbc.service.CmbcPayService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.cupsAT.service.CupsATPayService;
import com.hrtpayment.xpay.netCups.service.NetCupsPayService;
import com.hrtpayment.xpay.utils.CommonUtils;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;
/**
 * 自营一码付
 * @author aibing
 * 2016年11月11日
 */
@Service
public class XpayService {
	Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao;
	@Autowired
	CmbcPayService cmbcService;
	@Autowired
	WechatService wechatService;
	@Autowired
	AliJspayService alipayService;
	@Autowired
	BaiduPayService baiduPayService;
	@Autowired
	BcmPayService bcmPayService;
	@Autowired
	CupsPayService cupsPayService;
	@Autowired
	CupsATPayService cupsATPayService;
	@Autowired
	NetCupsPayService netCupsPayService;
	@Autowired
	CibPayService cibService;
	
	
	public Map<String, Object> getMerchantCode (String unno, String mid, String payway) {
		List<Map<String, Object>> list;
		if (unno==null || "110000".equals(unno)) {
			String sql = "select a.merchantcode,a.FIID from Bank_MerRegister a,HRT_MerBanksub b,"
					+ "Hrt_Merchacc ma where ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and a.approvestatus='Y' and a.status=1 "
					+ "and ma.hrt_MID =? ";
			
			if("WXZF".equals(payway)){
			   sql += " and  a.fiid in(11,14,15)";
			}else if("ZFBZF".equals(payway)){
				sql += " and  a.fiid in(12,16)";
			} else if("WXPAY".equals(payway)){
				sql += " and  a.fiid in(11,14)";
			}
			list = dao.queryForList(sql, mid);
		} else {
			String sql = "select a.merchantcode,a.FIID from Bank_MerRegister a,HRT_MerBanksub b,"
					+ "Hrt_Merchacc ma where ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and a.approvestatus='Y' and a.status=1"
					+ "and ma.hrt_MID =? and ma.unno=?";
			
			if("WXZF".equals(payway)){
			   sql += " and  a.fiid in(11,14,15)";
			}else if("ZFBZF".equals(payway)){
				sql += " and  a.fiid in(12,16)";
			} else if("WXPAY".equals(payway)){
				sql += " and  a.fiid in(11,14)";
			}
			
			list = dao.queryForList(sql, mid,unno);
		}
		if (list.size()<1) throw new HrtBusinessException(8001,"指定通道未开通");
		Map<String,Object> map = list.get(0);
		return map;
	}
	/**
	 * 下单并返回支付二维码(后台生成订单号)
	 * @param unno
	 * @param mid
	 * @param amount
	 * @param bankMid
	 * @param subject
	 * @param fiid
	 * @throws HrtBusinessException
	 * @return
	 */
	public String getPayUrl(String unno,String mid,BigDecimal amount,String subject,String payway,String qrtid){
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		String date=format.format(new Date());
		String orderid = "hrt" + date+CommonUtils.getRandomDecimalStr(10);
		return getPayUrl(unno, mid, amount, subject, payway, orderid,qrtid);
	}
	/**
	 * 下单并返回支付二维码
	 * @param unno
	 * @param mid
	 * @param amount
	 * @param subject
	 * @param payway
	 * @param orderid
	 * @return
	 */
	public String getPayUrl(String unno,String mid,BigDecimal amount,String subject,String payway,String orderid,String qrtid){
		//根据MID  判断 走哪个银行
		Map<String, Object> map;
		int fiid;
		try {
			map = cmbcService.getMerchantCode3(null, mid, payway,amount,"");
			fiid =Integer.parseInt(String.valueOf(map.get("FIID")));
			checkBankTxnLimit(fiid,amount,payway);
		} catch (BusinessException e1) {
			throw new HrtBusinessException(e1.getCode(), e1.getMessage());
		}
		String bankMid = (String) map.get("MERCHANTCODE");
		
		String QrCode = null;
		if(fiid==25){
			//百度钱包直连
			try {
				QrCode=baiduPayService.getQrCode(unno, mid, bankMid, subject, amount, fiid, orderid, null,"","");
			} catch (BusinessException e) {
				throw new HrtBusinessException(e.getCode(), e.getMessage()); 
			}
		}else if(fiid==34){
			try {
				if("WXPAY".equals(payway)){
//					cncbService.insertPubaccOrder(unno, mid, orderid, subject, amount, fiid, bankMid);
//					QrCode = wechatService.getPubaccPayUrl(fiid, orderid);
				}else{
					String key=String.valueOf(map.get("MINFO2"));
					if(key==null || "null".equals(key) || "".equals(key)){
						throw new BusinessException(9006,"未找到与商户对应的秘钥！");
					}
					QrCode = cibService.getcibPayUrl(unno,mid, amount, bankMid, subject, fiid,
							orderid,qrtid,key,payway,"","");
				}
				

			} catch (BusinessException e) {
				throw new HrtBusinessException(e.getCode(), e.getMessage());
			}
		}else if(fiid ==43){
			try {
				QrCode = bcmPayService.getQrCode(unno, mid, bankMid, subject, amount, fiid, payway, orderid, qrtid, "", "");
			} catch (BusinessException e) {
				throw new HrtBusinessException(e.getCode(), e.getMessage());
			}
		 
		 }else if(fiid ==46){
			try {
				if("WXZF".equals(payway)){
					try {
						QrCode = bcmPayService.getQrCode(unno, mid, bankMid, subject, amount, fiid, payway, orderid,
								qrtid, "", "");
					} catch (BusinessException e) {
						throw new HrtBusinessException(e.getCode(), e.getMessage());
					}
				} else if ("WXPAY".equals(payway)) {
					netCupsPayService.insertPubaccOrder(unno, mid, orderid, subject, amount, fiid, bankMid);
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid);
				} 
			} catch (BusinessException e) {
				throw new HrtBusinessException(e.getCode(), e.getMessage());
			}
           }else if(fiid==54){
   			try {
  				 QrCode = cupsATPayService.cupsAliPay(unno, mid, bankMid, subject, amount, fiid, payway, orderid,
  							qrtid, "", "",String.valueOf(map.get("CHANNEL_ID")));
  			} catch (BusinessException e) {
  				throw new HrtBusinessException(e.getCode(), e.getMessage());
  			}
		}else if(fiid==53){
			if("WXPAY".equals(payway)){
				try {
					cupsATPayService.insertPubaccOrder(unno, mid, orderid, subject, amount, fiid, bankMid);
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid);
				} catch (BusinessException e) {
					throw new HrtBusinessException(e.getCode(), e.getMessage());
				}
			}else if ("WXZF".equals(payway)){
				try {
					QrCode= cupsATPayService.cupsWxPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
							 payway,orderid, qrtid,"","",String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")));
				} catch (BusinessException e) {
					throw new HrtBusinessException(e.getCode(), e.getMessage());
				}
			 }else {
				throw new HrtBusinessException(8000, "交易失败");
			}
		}else if(fiid==60){
			if ("WXPAY".equals(payway)) {
				try {
					netCupsPayService.insertPubaccOrder(unno, mid, orderid, subject, amount, fiid, bankMid);
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid);
				} catch (BusinessException e) {
					throw new HrtBusinessException(e.getCode(), e.getMessage());
				}
			}else if ("WXZF".equals(payway)){
				try {
					QrCode= netCupsPayService.cupsWxPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
							 payway,orderid, qrtid,"","",String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")));
				} catch (BusinessException e) {
					throw new HrtBusinessException(e.getCode(), e.getMessage());
				}
			}else {
				throw new HrtBusinessException(8000, "交易失败");
			}
		}else if (fiid==61) {
			if ("ZFBZF".equals(payway)){
				try {
					QrCode= netCupsPayService.cupsAliPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
							 payway,orderid, qrtid,"","", String.valueOf(map.get("CHANNEL_ID")));
				} catch (BusinessException e) {
					throw new HrtBusinessException(e.getCode(), e.getMessage());
				}
			}else {
				throw new HrtBusinessException(8000, "交易失败");
			}
		}else{
			throw new HrtBusinessException(8000, "交易失败");
		}
		
		return QrCode;
	}
	
	/**
	 * pos下单并返回支付二维码
	 * @param orderid
	 * @return
	 * @throws BusinessException 
	 */
	public String getPosPayUrl(String payway,String orderid) throws BusinessException{
		String  tranType="";
		if("WXPAY".equals(payway)){
			tranType="1";
		}else if("ZFBZF".equals(payway)){
			tranType="2";
		}else if("QQZF".equals(payway)){
			tranType="4";
		}else if("BDQB".equals(payway)){
			tranType="5";
		}else if("JDZF".equals(payway)){
			tranType="6";
		}
		
		String sql="select * from pg_wechat_txn t where t.mer_orderid=?";
		List<Map<String, Object>> list=dao.queryForList(sql, orderid);
		if(list.size()<1){
			throw new BusinessException(8000,"未查到原订单！");
		}
		Map<String, Object> orderMap=list.get(0);
		String mid=String.valueOf(orderMap.get("MER_ID"));
		String subject=String.valueOf(orderMap.get("DETAIL"));
		BigDecimal amt = (BigDecimal) orderMap.get("TXNAMT");
		String status = String.valueOf(orderMap.get("STATUS"));
		if ("0".equals(status)) {
			throw new BusinessException(8000, "订单已失效,请重新下单");
		} else if ("1".equals(status)) {
			throw new BusinessException(8000, "订单已经支付成功");
		}
		String QrCode = null;
		try {
			//根据MID  判断 走哪个银行
			Map<String, Object> map = cmbcService.getMerchantCode3(null, mid, payway,amt,"");
			int fiid =Integer.parseInt(String.valueOf(map.get("FIID")));
			checkBankTxnLimit(fiid, amt, payway);
			String bankMid = (String) map.get("MERCHANTCODE");
			int updateCount=0;
			if(fiid==25){
				QrCode = baiduPayService.posGetQrCode("", mid, bankMid, subject, amt, fiid, orderid, "", "BDQB");
			}else if(fiid==34){
				/**
				 * 兴业公众号支付
				 */
				if ("WXPAY".equals(payway)) {
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=? where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,34,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid);
				}else if("ZFBZF".equals(payway)){
					String key=String.valueOf(map.get("MINFO2"));
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=? where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,34,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode=cibService.getcibPosPayUrl( amt, bankMid, subject, fiid, orderid, key, payway);
				}
			}else if(fiid==43){
				if("ZFBZF".equals(payway)){
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=? where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid); 
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode = bcmPayService.posGetQrCode(bankMid, bankMid, subject, amt, fiid, orderid, null, payway);
				}
			}else if(fiid==46){
				if ("WXPAY".equals(payway)) {
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=? where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid);
				}
			}else if (fiid ==54||fiid ==53) {
				if("ZFBZF".equals(payway)||"WXZF".equals(payway)){
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=? where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode =cupsATPayService.posGetQrCode(bankMid, subject, amt, fiid, orderid, payway,String.valueOf(map.get("CHANNEL_ID")));
				}
				if ("WXPAY".equals(payway)) {
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=? where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid);
				}
			}else if (fiid ==60||fiid ==61){
				if("ZFBZF".equals(payway)){
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=? where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode =netCupsPayService.posGetQrCode(bankMid, subject, amt, fiid, orderid, payway, String.valueOf(map.get("CHANNEL_ID")));
				}else if ("WXPAY".equals(payway)) {
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=? where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid);
				} 
				
			}
		} catch (Exception e) {
			logger.error("pos获取支付URL异常:",e);
			throw new BusinessException(8001,e.getMessage());
		}
		
		return QrCode;
	}
	
	
	/**
	 * 校验通道无卡单笔限额，单日限额
	 * @param fiid
	 * @param amt
	 * @param payway
	 */
	public void checkBankTxnLimit(int fiid,BigDecimal amt,String payway)throws BusinessException{
		
		
		String querySql="select nvl(c.txnlimit,9900) txnlimit,nvl(c.daylimit,100000) daylimit"
						+ " from hrt_fi_limit c where c.status=1 and c.fiid=? and c.payway=? ";
		List<Map<String, Object>> list =dao.queryForList(querySql, fiid,payway);
		if(list.size()==0){
			return ;
		}
		Map<String, Object> data =list.get(0);
		BigDecimal txnlimit =(BigDecimal) data.get("TXNLIMIT");
		if(txnlimit.doubleValue()>0&&amt.doubleValue()>txnlimit.doubleValue()){
			throw new HrtBusinessException(9001,"单笔限额，不允许交易！");
		}
	}
	
}
