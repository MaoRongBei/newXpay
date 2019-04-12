package com.hrtpayment.xpay.common.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.baidu.service.BaiduPayService;
import com.hrtpayment.xpay.bcm.service.BcmPayService;
import com.hrtpayment.xpay.cib.service.CibPayService;
import com.hrtpayment.xpay.cmbc.service.CmbcPayService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.cupsAT.service.CupsATPayService;
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.netCups.service.NetCupsPayService;
import com.hrtpayment.xpay.quickpay.common.service.QuickpayService;
import com.hrtpayment.xpay.quickpay.cups.service.CupsQuickPayService;
import com.hrtpayment.xpay.quickpay.newCups.service.newCupsPayService;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.redis.RedisUtilForTest;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

@Service
public class ManageService {
	Logger logger = LogManager.getLogger();
	@Autowired
	JdbcDao dao;
	@Autowired
	CmbcPayService cmbc;
	
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
	newCupsPayService newCupsPayService;
 
	@Autowired
	CupsQuickPayService cupsQuickPayService;
	@Autowired
	QuickpayService quickPayService;
	@Autowired
	CibPayService cibPayService;
	
	@Value("${quick.signLAmt}")
	private String signLAmt;
	
	@Value("${quick.dayLAmt}")
	private String dayLAmt;
	
	@Value("${dae.unno}") 
	private String daeUnno;
	
	@Value("${xpay.special.unno}")
	String specUnno;

	/**
	 * 查询订单
	 * @param orderid
	 * @return
	 */
	public String queryOrder(String orderid) {
		/*
		 * 2019-04-08 修改
		 * 
		 * 查询次数 及查询 时间累增是redis数据库内
		 * 
		 */
		long beginTime=System.currentTimeMillis();
		List<Map<String, Object>> list = dao.queryForList("select w.*,bm.mch_id,bm.channel_id from pg_wechat_txn w,bank_merregister bm where bm.merchantcode=w.bankmid and bm.fiid=w.fiid and    mer_orderid=?", orderid);
		long endTime=System.currentTimeMillis();
		try {
			RedisUtilForTest.addCount("addCountsForCounts", endTime-beginTime);
		} catch (BusinessException e1) {
			logger.error("[数据收集]  channelService.queryOrder() 查询 所需时长获取异常 ，原因{}",e1.getMessage());
		}
		if (list.size()<1) {
			return "订单号对应订单不存在";
		} else if (list.size()>1) {
			return "订单号重复";
		}
		Map<String, Object> map = list.get(0);
		if("1".equals(map.get("STATUS"))){
			return "SUCCESS";
		}
		BigDecimal fiid = (BigDecimal) map.get("FIID");
		if (fiid == null) {
			return "fiid为空,无法判断支付通道";
		}
		String txnType = (String) map.get("TXNTYPE");
		//支付交易查询
		if ("0".equals(txnType)) {
			if (25 == fiid.intValue()) { 
				return baiduPayService.queryOrder(map);

			} else if (34 == fiid.intValue()) {
				return cibPayService.queryOrder(null,map);
			}else if(40 == fiid.intValue()){
				try {
					return cupsQuickPayService.queryOrder(map);
				} catch (BusinessException e) {
					logger.error("[北京银联快捷支付]查询异常",e.getMessage());
					return "查询异常"+e.getMessage();
				}
			}else if (43 == fiid.intValue()) {
				return bcmPayService.queryOrder(null,map);
			}else if (46 == fiid.intValue()) {
				return bcmPayService.queryOrder(null,map);
			}else if (54 == fiid.intValue()) {
				try {
					return cupsATPayService.cupsAliQuery(null,map);
				} catch (BusinessException e) {
					logger.error("[银联-支付宝]查询异常",e.getMessage());
					return "查询异常"+e.getMessage();
				}
			}else if (53 == fiid.intValue()) {
				try {
					return cupsATPayService.cupsWxQuery(null,map);
				} catch (BusinessException e) {
					logger.error("[银联-微信]查询异常",e.getMessage());
					return "查询异常"+e.getMessage();
				}
			}else if (60 == fiid.intValue()) {
				try {
					return netCupsPayService.cupsWxQuery(null,map);
				} catch (BusinessException e) {
					logger.error("[网联-支付宝]查询异常",e.getMessage());
					return "查询异常"+e.getMessage();
				}
			}else if (61 == fiid.intValue()) {
				try {
					return netCupsPayService.cupsAliQuery(null,map);
				} catch (BusinessException e) {
					logger.error("[网联-微信]查询异常",e.getMessage());
					return "查询异常"+e.getMessage();
				}
			}else if (62 == fiid.intValue()) {
				try {
					return newCupsPayService.queryOrder(map,"1002");
				} catch (BusinessException e) {
					logger.error("[银联快捷]查询异常",e.getMessage());
					return "查询异常"+e.getMessage();
				}
		    }
		} else if ("1".equals(txnType)){ //退款查询
//			if (11 == fiid.intValue() || 12 == fiid.intValue() || 13 == fiid.intValue()){
//				return cmbc.query(orderid);
//			}
		} else {
			return "交易类型错误";
		}
		return "不支持的fiid";
	}
	 /**
	  * 订单关闭
	  * 根据订单号进行查询，订单状态不为1 的订单可以进行关闭
	  * @param orderid
	  * @return
	  */
	 public  String  closeOrder(String orderid){
		//关单之前查询  确保订单状态
		 String orderStatus=queryOrder(orderid);
		 if ("DOING_TRADE_NOT_EXIST".equals(orderStatus)) {
			 return orderid+"订单不存在，无需关单";
		 }
		 String  closeOrdSql="select pwt.fiid, pwt.status,mer_orderid,bk_orderid,bankmid, mch_id,channel_id"
		 		+ " from pg_wechat_txn pwt ,bank_merregister bm  "
		 		+ " where bankmid=merchantcode and  mer_orderid=? and pwt.status<>'1'";
		 List<Map<String, Object>> closeOrdList=dao.queryForList(closeOrdSql, orderid);
		 if (closeOrdList.size()==0) {
			return orderid+"订单已经完成，无法关闭";
		 }
		 if (closeOrdList.size()>1) {
			 return orderid+"订单异常，无法关闭";
		 }
		 
		 Map<String, Object> closeOrder=closeOrdList.get(0);
		 String status=String.valueOf(closeOrder.get("status"));
		 if ("5".equals(status)) {
			 logger.info("[订单关闭] 订单{}已关闭", orderid);
			 return orderid+"订单已关闭";
		 }
		 if ("1".equals(status)) {
			 logger.info("[订单关闭] 订单{}已成功，不能做订单关闭操作", orderid);
			 return orderid+"订单已成功";
		 }
		 String fiid=String.valueOf(closeOrder.get("fiid"));
		 try {
			 if ("53".equals(fiid)) {
				return cupsATPayService.cupsWxClose(closeOrder);
			 }else if ("54".equals(fiid)) {
				return cupsATPayService.cupsAliClosed(closeOrder);
			 }else if ("60".equals(fiid)) {
				return netCupsPayService.cupsWxClose(closeOrder);
			 }else if ("61".equals(fiid)) {
				return netCupsPayService.cupsAliClosed(closeOrder);	
			 }else {
				 return "该通道不支持关单操作";
			 }
				
		} catch (Exception e) {
			logger.error("[订单关闭] 订单{}关闭异常，原因：{}", orderid,e.getMessage());
			return e.getMessage();
		}
	 }
	 
	 /**
	  * 订单撤销
	  * 根据订单号进行查询，订单状态不为1 的订单可以进行撤销
	  * @param orderid
	  * @return
	  */
	 public  String  cancelOrder(String orderid){
		//撤销之前查询  确保订单状态
		 queryOrder(orderid);
		 String  cancelOrdSql="select pwt.fiid, pwt.status,mer_orderid,bk_orderid,bankmid, mch_id,channel_id"
		 		+ " from pg_wechat_txn pwt ,bank_merregister bm  "
		 		+ " where bankmid=merchantcode and  mer_orderid=? and pwt.status<>'1'";
		 List<Map<String, Object>> cancelOrdList=dao.queryForList(cancelOrdSql, orderid);
		 if (cancelOrdList.size()==0) {
			return orderid+"订单已经完成，无法撤销，请执行退款操作。";
		 }
		 if (cancelOrdList.size()>1) {
			 return orderid+"订单异常，无法撤销";
		 }
		 
		 Map<String, Object> cancelOrder=cancelOrdList.get(0);
		 String status=String.valueOf(cancelOrder.get("status"));
		 if ("7".equals(status)) {
			 logger.info("[订单撤销] 订单{}已撤销", orderid);
			 return orderid+"订单已撤销";
		 }
		 if ("1".equals(status)) {
			 logger.info("[订单撤销] 订单{}已成功，不能做订单撤销操作", orderid);
			 return orderid+"订单已成功";
		 }
		 String fiid=String.valueOf(cancelOrder.get("fiid"));
		 try {
			 if ("53".equals(fiid)) {
				return cupsATPayService.cupsWxCancel(cancelOrder);
			 }else if ("54".equals(fiid)) {
				return cupsATPayService.cupsAliCancel(cancelOrder);
			 }else if ("60".equals(fiid)) {
				return netCupsPayService.cupsWxCancel(cancelOrder);
			 }else if ("61".equals(fiid)) {
				return netCupsPayService.cupsAliCancel(cancelOrder);	
			 }else {
				 return "该通道不支持撤销操作";
			 }
				
		} catch (Exception e) {
			logger.error("[订单撤销] 订单{}撤销异常，原因：{}", orderid,e.getMessage());
			return e.getMessage();
		}
	 }
	/**
	 * 退款
	 * @param orderid
	 * @param oriOrderid
	 * @param amount
	 * @return
	 * @throws BusinessException 
	 */
	public String refund(String orderid, String oriOrderid, BigDecimal amount) throws BusinessException{
		List<Map<String, Object>> list = dao.queryForList("select w.*,bm.mch_id,bm.channel_id from pg_wechat_txn w,bank_merregister bm where bm.merchantcode=w.bankmid and bm.fiid=w.fiid and  mer_orderid=?", oriOrderid);
		if (list.size()<1) {
			return "订单号对应订单不存在";
		} else if (list.size()>1) {
			return "订单号重复";
		}
		Map<String, Object> oriMap = list.get(0);
		String txnType = (String) oriMap.get("TXNTYPE");
		if (!"0".equals(txnType)) return "非消费交易不能退款";
		String oriStatus = (String) oriMap.get("STATUS");
		if (!"1".equals(oriStatus)) return "原交易未支付成功不能退款";
		
		
		List<Map<String, Object>> merlist = dao.queryForList("select  mf.cycle,nvl(mf.Settmethod,'0') settmethod  from hrt_merchacc mc ,hrt_merchfinacc mf where mc.maid=mf.maid and mc.hrt_mid =?",  list.get(0).get("mer_id"));
		if (merlist.size()<1) {
			return "商户号错误，请核对";
		}
		/*
		 *  part:1
		 *  结算周期为1 或计算周期为0 且settmethod不为null 和0的商户 可进行退款
		 */
		String cycle=String.valueOf(merlist.get(0).get("CYCLE"));
		String settmethod=String.valueOf(merlist.get(0).get("SETTMETHOD"));
		if (!"1".equals(cycle)&&"0".equals(settmethod)) {
			return "T0结算商户不能进行退款交易";
		}

		
		Object oriPwid = oriMap.get("PWID");
		
		BigDecimal availableAmt = (BigDecimal) oriMap.get("TXNAMT");
		List<Map<String, Object>> refundList = dao.queryForList("select * from pg_wechat_txn where oripwid=? and txntype='1'", oriPwid);
		for (Map<String, Object> map : refundList) {
			if ("1".equals(map.get("STATUS")) || "0".equals(map.get("STATUS"))||"2".equals(map.get("STATUS"))) {
				BigDecimal rfAmt = (BigDecimal) map.get("TXNAMT");
				availableAmt = availableAmt.subtract(rfAmt);
			}
		}
		/*
		 * part:2
		 * 判断当前退款金额 是否
		 * availableAmt>=amount 才能退款
		 * 
		 */
		if (availableAmt.compareTo(amount)<0) {
			return "退款金额超限";
		}
		BigDecimal fiid = (BigDecimal) oriMap.get("FIID");
		if (fiid == null) {
			return "fiid为空,无法判断支付通道";
		}
		//插入退款订单
		if (dao.queryForList("select * from Pg_Wechat_Txn t where t.mer_orderid=?", orderid).size()>0) {
			return "订单号重复";
		}
		BigDecimal refundpwid = getNewPwid();
		String sql = "insert into pg_wechat_txn (pwid,oripwid,fiid, txntype,cdate,status,"
				+ "mer_orderid, txnamt,mer_id,unno,bankmid,lmdate,trantype) values"
				+ "(?,?,?,'1',sysdate,'A',?,?,?,?,?,sysdate,?)";
		dao.update(sql, refundpwid,oriPwid,oriMap.get("FIID"), orderid, amount,oriMap.get("MER_ID")
				,oriMap.get("UNNO"),oriMap.get("BANKMID"),oriMap.get("TRANTYPE"));
		/*
		 * part:3
		 * 
		 * T0商户
		 * 判断钱包加金库余额是否>=当前退款金额  
		 * true ：先扣减 
		 * false：提示可用退款金额不足
		 * 
		 * T1 不做判断 正常做退款
		 */
		if (!"1".equals(cycle)&&!"0".equals(settmethod)) {
			String endDate =(oriMap.get("TIME_END")+"").substring(0, 8);
			
			String queryBalanceSql=" select   psp.balance,psp.bookbal, rf.rfamtoutfee,rf.maid " 
					+ " from  pg_sm_purse psp,(select round( ?*(1-MDA/MNAMT),2) rfamtoutfee,maid  from  pg_sm_wechattxn  where   pwid=? AND  TXNDAY=?) rf "
					+ " where  psp. maid= rf.maid ";
			List<Map<String, Object>> balanceList =dao.queryForList(queryBalanceSql, amount,oriPwid,endDate);
			if (balanceList.size()==0) {
				return "商户钱包余额为0，可退款金额不足。";
			}else if (balanceList.size()>1) {
				return "商户钱包余额错误，请联系客服进行咨询。";
			}
			BigDecimal  balance= (BigDecimal)balanceList.get(0).get("balance");
			BigDecimal  bookbal= (BigDecimal)balanceList.get(0).get("bookbal");
			BigDecimal  rfamtoutfee= (BigDecimal)balanceList.get(0).get("rfamtoutfee");
			String maid=balanceList.get(0).get("maid")+"";
			
			String updateOrderSql="update pg_wechat_txn set status='6',lmdate=sysdate,respcode=? ,respmsg=? where pwid=? ";

			//T+N余额+可提现余额<退款金额 提示 余额不足，扣款失败 更改 退款记录 为6
			if ((bookbal.add(balance)).compareTo(rfamtoutfee)<0) {
				dao.update(updateOrderSql, "FAIL","金额不足扣款失败",refundpwid);
				return "余额不足，扣款失败";
			}
			//当T+N余额 >退款金额 直接从 T+N余额内扣除金额 ，如果更新失败，更改 退款记录 为6，让用户重新发起退款
			if (bookbal.compareTo(rfamtoutfee)>=0) {
				String updateBookbalSql="update pg_sm_purse set bookbal=bookbal-? where maid=? ";
				int count=dao.update(updateBookbalSql, rfamtoutfee,maid);
				if (count==0) {
					dao.update(updateOrderSql, "FAIL","扣减T+N金额失败",refundpwid);
					return "扣款失败，请重新退款";
				}
			//当T+N余额 +可提现余额>退款金额 直接从 T+N余额内扣除金额 ，如果更新失败，更改 退款记录 为6，让用户重新发起退款
			}else if(bookbal.compareTo(rfamtoutfee)<0&&(bookbal.add(balance)).compareTo(rfamtoutfee)>0){
				String updateBookbalSql="update pg_sm_purse set balance=balance-?+bookbal,bookbal=0,curamt=balance where maid=? ";
				int count=dao.update(updateBookbalSql, rfamtoutfee,maid);
				if (count==0) {
					dao.update(updateOrderSql, "FAIL","扣减T+0金额失败",refundpwid);
					return "扣款失败，请重新退款";
				}
			}
		}

		if(18==fiid.intValue()){
			return cupsPayService.cupsRefund(orderid,amount,oriMap);
		} else if (25 == fiid.intValue()) {
			return baiduPayService.refund(orderid, amount, oriMap);
		}else if(34 == fiid.intValue()){
			return cibPayService.refund(orderid, amount, oriMap);
		}else if(43 == fiid.intValue()){
			return bcmPayService.refund(orderid, amount, oriMap);
		}else if(46 == fiid.intValue()){
			return bcmPayService.refund(orderid, amount, oriMap);
		}else if(40 == fiid.intValue()){
			return cupsQuickPayService.refund(orderid, amount, oriMap);
		}else if(54 == fiid.intValue() ){
			return cupsATPayService.refundAli(orderid, amount, oriMap);
		}else if(53 == fiid.intValue() ){
			return cupsATPayService.cupsWxRefund(orderid, amount, oriMap);
		}else if(60 == fiid.intValue() ){
			return netCupsPayService.cupsWxRefund(orderid, amount, oriMap);
		}else if(61 == fiid.intValue() ){
			return netCupsPayService.refundAli(orderid, amount, oriMap);
		}else if(62 == fiid.intValue() ){
			return newCupsPayService.refund(orderid, amount, oriMap);
		}else {
			return "支付通道暂不支持退款";
		}
	}
	
	public String refundQuery(String orderid) throws BusinessException{
		List<Map<String, Object>> list = dao.queryForList("select w.*,bm.mch_id,bm.channel_id from pg_wechat_txn w,bank_merregister bm where bm.merchantcode=w.bankmid and  bm.fiid=w.fiid and  mer_orderid=?", orderid);
		if (list.size()<1) {
			return "订单号对应订单不存在";
		} else if (list.size()>1) {
			return "订单号重复";
		}

		Map<String, Object> refundMap = list.get(0);
		String txnType = (String) refundMap.get("TXNTYPE");
		String status = (String) refundMap.get("STATUS");
		if (!"1".equals(txnType)) return "非退款交易不能做退款查询";
		
		if ("1".equals(status)) return "退款成功";
		if ("6".equals(status)){
			logger.info("[退款] 订单{} 原状态 为失败 ",orderid);
			return "FAIL";
		}		
		BigDecimal fiid = (BigDecimal) refundMap.get("FIID");
		if (25 == fiid.intValue()) {
			return baiduPayService.refundQuery(refundMap);
		} else if (34 == fiid.intValue()) {
			return cibPayService.refundQuery(refundMap);
		} else if (40 == fiid.intValue()) {
			return cupsQuickPayService.refundQueryOrder(refundMap);
		} else if (43 == fiid.intValue()) {
			return bcmPayService.refundQueryOrder(refundMap);// .queryOrder(refundMap);
		} else if (46 == fiid.intValue()) {
			return bcmPayService.refundQueryOrder(refundMap);//.queryOrder(refundMap);
		} else if (54 == fiid.intValue()) {
			return cupsATPayService.refundAliQuery(refundMap);
		} else if (53 == fiid.intValue()) {
			return cupsATPayService.cupsWxRefundQuery(refundMap);
		} else if (60 == fiid.intValue()) {
			return netCupsPayService.cupsWxRefundQuery(refundMap);
		} else if (61 == fiid.intValue()) {
			return netCupsPayService.refundAliQuery(refundMap);
		} else if (62 == fiid.intValue()) {
			return newCupsPayService.queryOrder(refundMap, "1101");
		} else {
			return "支付通道暂不支持退款查询";
		}
		
	}
	
	private BigDecimal getNewPwid() {
		List<Map<String, Object>> list = dao.queryForList("select S_PG_Wechat_Txn.nextval pwid from dual");
		if (list.size()>0) {
			Map<String, Object> map = list.get(0);
			if (map.containsKey("PWID")) {
				return (BigDecimal) map.get("PWID");
			}
		}
		logger.info("获取pwid(S_PG_Wechat_Txn.nextval)失败");
		throw new HrtBusinessException(8000);
	}
	
	public boolean addDayMerAmt(String merid,Double amt) throws BusinessException{
		boolean flag=false;
		String querySql=" select nvl(t1.minfo1,10000) singAmt, nvl(t1.minfo2,50000) dayLimitAmt from hrt_merchacc t, pg_merchlimit t1 "
						+ " where t.maid = t1.maid and t.hrt_mid =? ";
		List<Map<String, Object>> list = dao.queryForList(querySql, merid);
		if(list.size()>0){
			Map<String, Object> map = list.get(0);
			Double singAmt=Double.parseDouble(String.valueOf(map.get("SINGAMT")));
			Double dayLimitAmt=Double.parseDouble(String.valueOf(map.get("DAYLIMITAMT")));
			if(amt>singAmt){
				throw new BusinessException(9001, "单笔超限额，不允许交易！");
			}
			if(amt>dayLimitAmt){
				throw new BusinessException(9001, "单日超限额，不允许交易！");
			}
			RedisUtil.addRiskAmt("limitAmt",amt, merid, dayLimitAmt);
			flag=true;
		}else{
			throw new BusinessException(9001, "商户限额未维护！");
		}
		return flag;
	}
	
    /**
     *  限额判断方法
     *  仅做校验 不做累增
     * @param merid
     * @param amt
     * @return
     * @throws BusinessException
     */
	public boolean checkDayMerAmtForLMF(String merid,Double amt,String unno) throws BusinessException{
		boolean flag=false;
		String querySql="";
	    if ("".equals(unno)||specUnno.contains(unno)) {
		     querySql=" select nvl(t1.minfo1,10000) singAmt, nvl(t1.minfo2,100000) dayLimitAmt from hrt_merchacc t, pg_merchlimit t1 "
				+ " where t.maid = t1.maid and t.hrt_mid =? ";
	    }else if (!"".equals(unno)&&daeUnno.contains(unno)) {
			querySql=" select nvl(t1.minfo1,20000) singAmt, nvl(t1.minfo2,100000) dayLimitAmt from hrt_merchacc t, pg_merchlimit t1 "
					+ " where t.maid = t1.maid and t.hrt_mid =? ";
		}else{
			querySql=" select nvl(t1.minfo1,9900) singAmt, nvl(t1.minfo2,100000) dayLimitAmt from hrt_merchacc t, pg_merchlimit t1 "
					+ " where t.maid = t1.maid and t.hrt_mid =? ";
		}
		List<Map<String, Object>> list = dao.queryForList(querySql, merid);
		if(list.size()>0){
			Map<String, Object> map = list.get(0);
			Double singAmt=Double.parseDouble(String.valueOf(map.get("SINGAMT")));
			Double dayLimitAmt=Double.parseDouble(String.valueOf(map.get("DAYLIMITAMT")));
			if(amt>singAmt){
				throw new BusinessException(9001, "单笔超限额，不允许交易！");
			}
			if(amt>dayLimitAmt){
				throw new BusinessException(9001, "单日超限额，不允许交易！");
			}
			RedisUtil.checkRiskAmtForLMF("limitAmt",amt, merid, dayLimitAmt);
			flag=true;
		}else{
			throw new BusinessException(9001, "商户限额未维护！");
		}
		return flag;
	}
	
	
	 /**
     *  立码富专用限额累增方法
     *  仅做累增   不做校验
     * @param merid
     * @param amt
     * @return
     * @throws BusinessException
     */
	public boolean addDayMerAmtForLMF(String merid,Double amt) throws BusinessException{
		boolean flag=true;
		try {
			RedisUtil.addRiskAmtForLMF("limitAmt",amt, merid);
		} catch (Exception e) {
			logger.error(e);
		}
		return flag;
	}
	
	public void addDayMerAmtForCups(String merid,Double amt) throws BusinessException{
		Double dayLimitAmt=5000.0;
		if(amt>1000){
			throw new BusinessException(9001, "银联二维码单笔超限额，不允许交易！");
		}
		if(amt>dayLimitAmt){
			throw new BusinessException(9001, "银联二维码单日超限额，不允许交易！");
		}
		RedisUtil.addRiskAmt("cupsLimitAmt",amt, merid, dayLimitAmt);
	}
	
//	public void addDayUnnoAmt(String unno, Double amt) throws BusinessException{
//		
//		String querySql="select nvl(d.dayamtlimit,0) dayamtlimit from hrt_xpayorginfo d where d.unno=?";
//		List<Map<String, Object>> list =dao.queryForList(querySql, unno);
//		if(list.size()>0){
//			Map<String, Object> map = list.get(0);
//			Double dayUnnoAmt=Double.parseDouble(String.valueOf(map.get("DAYAMTLIMIT")));
//			if(amt>dayUnnoAmt){
//				throw new BusinessException(9001, "单日机构限额不允许交易！");
//			}
//			RedisUtil.addUnnoLimit(unno,amt,dayUnnoAmt);
//		}else{
//			throw new BusinessException(9001, "机构限额未维护！");
//		}
//	}
	
	
	/**
	 * 快捷支付限额专用
	 * @param merid
	 * @param amt
	 * @return
	 * @throws BusinessException
	 */
	public boolean addQuickPayDayMerAmt(String merid,Double amt) throws BusinessException{
		boolean flag=false;
		String querySql=" select nvl(t1.txnlimit,50000) singAmt, nvl(t1.dailylimit,200000) dayLimitAmt from hrt_merchacc t, pg_merchlimit t1 "
						+ " where t.maid = t1.maid and t.hrt_mid =? ";
		List<Map<String, Object>> list = dao.queryForList(querySql, merid);
		if(list.size()>0){
			Map<String, Object> map = list.get(0);
			Double singAmt=Double.parseDouble(String.valueOf(map.get("SINGAMT")));
			Double dayLimitAmt=Double.parseDouble(String.valueOf(map.get("DAYLIMITAMT")));
			if(amt>singAmt){
				throw new BusinessException(9001, "单笔超限额，不允许交易！");
			}
			if(amt>dayLimitAmt){
				throw new BusinessException(9001, "单日超限额，不允许交易！");
			}
			RedisUtil.addRiskAmt("qpLimitAmt",amt, merid, dayLimitAmt);
			flag=true;
		}else{
			throw new BusinessException(9001, "商户限额未维护！");
		}
		return flag;
	}
	
	
	 /**
     *  快捷支付专用限额判断方法
     *  仅做校验 不做累增
     * @param merid
     * @param amt
     * @return
     * @throws BusinessException
     */
	public boolean checkDayMerAmtForQuickPay(String merid,Double amt) throws BusinessException{
		boolean flag=false;
//		String querySql=" select nvl(t1.minfo1,50000) singAmt, nvl(t1.minfo2,200000) dayLimitAmt from hrt_merchacc t, pg_merchlimit t1 "
//						+ " where t.maid = t1.maid and t.hrt_mid =? ";
//		List<Map<String, Object>> list = dao.queryForList(querySql, merid);
//		if(list.size()>0){
//			Map<String, Object> map = list.get(0);
//			Double singAmt=Double.parseDouble(String.valueOf(map.get("SINGAMT")));
//			Double dayLimitAmt=Double.parseDouble(String.valueOf(map.get("DAYLIMITAMT")));
			Double singAmt=new Double(signLAmt);
			Double dayLimitAmt=new Double(dayLAmt);
			if(amt>singAmt){
				throw new BusinessException(9001, "单笔超限额，不允许交易！");
			}
			if(amt>dayLimitAmt){
				throw new BusinessException(9001, "单日超限额，不允许交易！");
			}
			RedisUtil.checkRiskAmtForLMF("qpLimitAmt",amt, merid, dayLimitAmt);
			flag=true;
//		}else{
//			throw new BusinessException(9001, "商户限额未维护！");
//		}
		return flag;
	}
	
	
	 /**
     *  
     *  快捷支付专用限额累增方法
     *  仅做累增   不做校验
     * @param merid
     * @param amt
     * @return
     * @throws BusinessException
     */
	public boolean addDayMerAmtForQuickPay(String merid,Double amt) throws BusinessException{
		boolean flag=true;
		try {
			RedisUtil.addRiskAmtForLMF("qpLimitAmt",amt, merid);
		} catch (Exception e) {
			logger.error(e);
		}
		return flag;
	}
	
	/**
	 * 累增快捷支付成功次数
	 * @param orderid
	 * @param accno
	 * @param mid
	 * @param type
	 */
	public void addQuickPayDayCount(String orderid,String accno,String type){
		try {
			String redisAccNo="";
			if("1".equals(type)){
				// 携带明文卡号
				redisAccNo=accno;
			}else{
				// 需关联查出明文卡号
				String querySql="select qkcard.accno_encrypt from pg_wechat_txn pgtxn ,"
						+ " hrt_qkpaycard qkcard where pgtxn.mer_id=qkcard.mid and"
						+ " pgtxn.isscode=qkcard.qpcid and qkcard.status='1' and"
						+ " pgtxn.lmdate between trunc(sysdate) and trunc(sysdate+1)"
						+ " and pgtxn.mer_orderid=?";
				List<Map<String, Object>> list = dao.queryForList(querySql, orderid);
				if(list.size()>0){
					String eaccNo =String.valueOf(list.get(0).get("accno_encrypt"));
					redisAccNo =quickPayService.decodeByAES(eaccNo);
				}
			}
			if(redisAccNo!=null&&!"".equals(redisAccNo)){
				RedisUtil.addRiskAmt("qpLimitCount",1.0, redisAccNo, 5.0);
			}
			
		} catch (Exception e) {
			logger.error("快捷支付成功次数累增异常:"+e);
		}
	}
	
	
	/**
	 * 判断快捷支付成功是否已经达五次
	 * @param accno
	 */
	public boolean  checkQuickPayDayCount(String toaccno){
		try {
			boolean flag =RedisUtil.checkQuickPayDayCount("qpLimitCount", toaccno);
			return flag;
		} catch (BusinessException e) {
			logger.error("校验快捷支付是否5次异常:"+e);
		}
		return false;
	}
	
	
	/**
	 * 2018-11-27 新增
	 * 
	 * 功能：限制大额交易
	 * 规则：根据openid和userid及和融通商户号进行判断
	 *       1、同一个openid、userid当日交易超过9000笔数超过三笔且均为贷记卡交易
	 *          禁止该商户当日进行交易
	 *  
	 * @param merid  商户号
	 */
	public void  checkPayForDae(String merid){
		String chOrdSql="select 1 "
				+ "       from ( select  userid,count(1) c from  pg_wechat_txn where  lmdate between trunc(sysdate,'dd') and sysdate  and mer_id=? and txnamt>9000 and paytype='2' group by  userid ) t "
				+ "      where t.c>=3 ";
		List<Map<String, Object>> list=dao.queryForList(chOrdSql, merid);
		if (list.size()>0) {
			throw new HrtBusinessException(8000,"今日交易次数超限，暂时无法进行交易");
		}
	}
}
