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
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.cib.service.CibPayService;
import com.hrtpayment.xpay.cmbc.bean.json.HrtCmbcBean;
import com.hrtpayment.xpay.cmbc.service.CmbcPayService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.cupsAT.service.CupsATPayService;
import com.hrtpayment.xpay.netCups.service.NetCupsPayService;
import com.hrtpayment.xpay.redis.RedisUtil;
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
	MerchantService merService;	
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
	 * 
	 * 2018-12-19  修改
	 * 
	 * 更新方法 参数为  HrtCmbcBean bean
	 * 原有多参数方法弃用
	 * 
	 * @param bean
	 * @return
	 */
//	public String getPayUrl(String unno,String mid,BigDecimal amount,String subject,String payway,String orderid,String qrtid){
	public String getPayUrl(HrtCmbcBean bean){
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		String date=format.format(new Date());
		String orderid = "qrpay" + date+CommonUtils.getRandomDecimalStr(8);
		//根据MID  判断 走哪个银行
		Map<String, Object> map;
		String unno=null;
		String mid=bean.getMid();
		BigDecimal amount=bean.getAmount();
		String subject=bean.getSubject();
		String payway=bean.getPayway();
		String qrtid=bean.getQrtid();
		bean.setOrderid(orderid);
		int fiid;
		/*
		 *  2019-01-17 修改
		 *  
		 *  3min<交易间隔 <4min 计为风控交易
		 * 
		 */
		String oldOrderid="";
		try {
			Map<String,Object> midOrdInfo=RedisUtil.checkMid(bean.getMid(),orderid);
			//获取oldOrderid 如果不为空 则将newOrderid 和oldorderid 都设置成风险交易 
			if (!"".equals(String.valueOf(midOrdInfo.get("oldOrderid")))&&null!=midOrdInfo.get("oldOrderid")) {
				oldOrderid=String.valueOf(midOrdInfo.get("oldOrderid"));
			}
		} catch (Exception e) {
			logger.error("[大额交易]校验时长出错，本条交易跳过时长校验，{}",e.getMessage());
		}
		try {
			/*
			 * 2018-12-19 修改 
			 * 
			 * 手刷启用对应的商户号查询方法  getMerchantForMpos 
			 * 手刷商户号查询弃用原有 getMerchantCode3
			 * 
			 */
//			map = cmbcService.getMerchantCode3(null, mid, payway,amount,"");
			map= cmbcService.getMerchantForMpos(bean);
			fiid =Integer.parseInt(String.valueOf(map.get("FIID")));
			checkBankTxnLimit(fiid,amount,payway);
		} catch (BusinessException e1) {
			throw new HrtBusinessException(e1.getCode(), e1.getMessage());
		}
		String bankMid = (String) map.get("MERCHANTCODE");
		String isCredit= String.valueOf(map.get("isCredit")==null?1:map.get("isCredit"));
		
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
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid,isCredit);
				} 
			} catch (BusinessException e) {
				throw new HrtBusinessException(e.getCode(), e.getMessage());
			}
           }else if(fiid==54){
   			try {
  				 QrCode = cupsATPayService.cupsAliPay(unno, mid, bankMid, subject, amount, fiid, payway, orderid,
  							qrtid, "", "",String.valueOf(map.get("CHANNEL_ID")),isCredit);
  			} catch (BusinessException e) {
  				throw new HrtBusinessException(e.getCode(), e.getMessage());
  			}
		}else if(fiid==53){
			if("WXPAY".equals(payway)){
				try {
					cupsATPayService.insertPubaccOrder(unno, mid, orderid, subject, amount, fiid, bankMid);
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid,isCredit);
				} catch (BusinessException e) {
					throw new HrtBusinessException(e.getCode(), e.getMessage());
				}
			}else if ("WXZF".equals(payway)){
				try {
					QrCode= cupsATPayService.cupsWxPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
							 payway,orderid, qrtid,"","",String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")),isCredit);
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
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid,isCredit);
				} catch (BusinessException e) {
					throw new HrtBusinessException(e.getCode(), e.getMessage());
				}
			}else if ("WXZF".equals(payway)){
				try {
					QrCode= netCupsPayService.cupsWxPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
							 payway,orderid, qrtid,"","",String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")),isCredit);
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
							 payway,orderid, qrtid,"","", String.valueOf(map.get("CHANNEL_ID")),isCredit);
				} catch (BusinessException e) {
					throw new HrtBusinessException(e.getCode(), e.getMessage());
				}
			}else {
				throw new HrtBusinessException(8000, "交易失败");
			}
		}else{
			throw new HrtBusinessException(8000, "交易失败");
		}
		/*
		 * 2019-01-17  修改
		 * 
		 * 更新订单风控标识 riskflag =1 
		 * 
		 */
		if (!"".equals(oldOrderid)&&null!=oldOrderid) {
			merService.updateOrderRisk(oldOrderid,orderid);
			logger.info("[大额手刷]风控交易{}:{},{}",mid,oldOrderid,orderid);
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
			Map<String, Object> map = cmbcService.getMerchantCode3(null, mid, payway,amt,"","ZS");
			int fiid =Integer.parseInt(String.valueOf(map.get("FIID")));
			checkBankTxnLimit(fiid, amt, payway);
			String bankMid = (String) map.get("MERCHANTCODE");
			String isCredit= String.valueOf(map.get("isCredit")==null?1:map.get("isCredit"));
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
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid,isCredit);
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
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid,isCredit);
				}
			}else if (fiid ==54||fiid ==53) {
				if("ZFBZF".equals(payway)){
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=?,trade_type='ZS' where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode =cupsATPayService.posGetQrCode(bankMid, subject, amt, fiid, orderid, payway,String.valueOf(map.get("CHANNEL_ID")));
				}
				if ("WXPAY".equals(payway)) {
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=?,trade_type='ZS' where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid,isCredit);
				}
			}else if (fiid ==60||fiid ==61){
				if("ZFBZF".equals(payway)){
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=?,trade_type='ZS' where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode =netCupsPayService.posGetQrCode(bankMid, subject, amt, fiid, orderid, payway, String.valueOf(map.get("CHANNEL_ID")));
				}else if ("WXPAY".equals(payway)) {
					String updateSql=" update pg_wechat_txn t set t.bankmid=?,t.fiid=? ,t.trantype=?,trade_type='ZS' where status='A' and mer_orderid=?";
					updateCount=dao.update(updateSql, bankMid,fiid,tranType,orderid);
					if(updateCount<1){
						throw new BusinessException(8000, "订单已失效,请重新下单");
					}
					QrCode = wechatService.getPubaccPayUrl(fiid, orderid,isCredit);
				} 
				
			}
		} catch (Exception e) {
			logger.error("pos获取支付URL异常:",e);
			throw new BusinessException(8001,e.getMessage());
		}
		
		return QrCode;
	}
	
	/**
	 * 
	 * 2019-01-07 新增
	 * 
	 * 机具被扫交易 逻辑处理
	 * 
	 * @param bean
	 * @return
	 * @throws BusinessException
	 */
       public String barcodePay(HrtPayXmlBean bean) throws BusinessException {
		
    	/*
    	 * 判断条码支付类型
    	 * 根据上送的authcode判断是否与上送的payway相符合
    	 */
		checkAuthCodePayType(bean);
		//根据MID  判断 走哪个银行
		Map<String, Object> map;
		String mid=bean.getMid();
		BigDecimal amount = new BigDecimal(bean.getAmount());
		String subject = bean.getSubject();
		String payway=bean.getPayway();
		int fiid;
		try {
			//使用轮询组查号规则  getMerchantForMpos
			HrtCmbcBean cmbcBean=new HrtCmbcBean();
			cmbcBean.setAmount(amount);
			cmbcBean.setMid(mid);
			cmbcBean.setPayway(payway);
			cmbcBean.setOrderid(bean.getOrderid());
			map= cmbcService.getMerchantForMpos(cmbcBean);
			fiid =Integer.parseInt(String.valueOf(map.get("FIID")));
			checkBankTxnLimit(fiid,amount,payway);
		} catch (BusinessException e1) {
			throw new HrtBusinessException(e1.getCode(), e1.getMessage());
		}
		String isCredit= String.valueOf(map.get("isCredit")==null?1:map.get("isCredit"));
		String merchantCode = String.valueOf(map.get("MERCHANTCODE"));
		if (subject == null || "".equals(subject)) {
			subject = String.valueOf(map.get("SHORTNAME"));
		}
		String resp;
		if (fiid == 43) { 
			resp = bcmPayService.barCodePay(bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),bean.getPaymode());
		}else if (fiid == 46) { 
			resp = bcmPayService.barCodePay(bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),bean.getPaymode());
		}else if (fiid == 53 ) { 
			resp = cupsATPayService.cupsWxBsPay (bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if (fiid == 54 ) { 
			resp = cupsATPayService.cupsAliBsPay(bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if (fiid == 60 ) { 
			resp = netCupsPayService.cupsWxBsPay (bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if (fiid == 61 ) { 
			resp = netCupsPayService.cupsAliBsPay (bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),String.valueOf(map.get("CHANNEL_ID")),isCredit);
		} else{
			 throw new BusinessException(8005,"未知错误");
		}
		return resp;
	}
	
   	/**
   	 * 判断条码支付类型
   	 * @param bean
   	 * @throws BusinessException 
   	 */
   	private void checkAuthCodePayType(HrtPayXmlBean bean) throws BusinessException{
   		if ("WXZF".equals(bean.getPayway()) || "ZFBZF".equals(bean.getPayway())
   				|| "QQZF".equals(bean.getPayway()) || "BDQB".equals(bean.getPayway())|| "JDZF".equals(bean.getPayway())|| "JDPAY".equals(bean.getPayway())) {
   		}else{
   			throw new BusinessException(9010,"不支持的支付通道");
   		}
   		try {
   			if("WXZF".equals(bean.getPayway())){
   				bean.setPaymode("1");
   			}else if("ZFBZF".equals(bean.getPayway())){
   				bean.setPaymode("2");
   			}else if(("JDZF".equals(bean.getPayway())|| "JDPAY".equals(bean.getPayway()))){
   				bean.setPaymode("6");
   			}else if("BDQB".equals(bean.getPayway())){
   				bean.setPaymode("5");
   			}else if("QQZF".equals(bean.getPayway())){
   				bean.setPaymode("4");
   			}else{
   				throw new BusinessException(8005,"请扫描正确的付款码!");
   			}
   		} catch (Exception e) {
   			throw new BusinessException(8005,"请扫描正确的付款码!");
   		}
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
