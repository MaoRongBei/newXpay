package com.hrtpayment.xpay.common.service.impl;

import java.math.BigDecimal;
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
	
	@Value("dae.unno") 
	private String daeUnno;

	/**
	 * 查询订单
	 * @param orderid
	 * @return
	 */
	public String queryOrder(String orderid) {
		List<Map<String, Object>> list = dao.queryForList("select w.*,bm.mch_id,bm.channel_id from pg_wechat_txn w,bank_merregister bm where bm.merchantcode=w.bankmid and bm.fiid=w.fiid and    mer_orderid=?", orderid);
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
		
		
		List<Map<String, Object>> merlist = dao.queryForList("select  mf.cycle  from hrt_merchacc mc ,hrt_merchfinacc mf where mc.maid=mf.maid and mc.hrt_mid =?",  list.get(0).get("mer_id"));
		if (merlist.size()<1) {
			return "商户号错误，请核对";
		}
		String cycle=String.valueOf(merlist.get(0).get("CYCLE"));
		if (!"1".equals(cycle)) {
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
		String querySql=" select nvl(t1.minfo1,9900) singAmt, nvl(t1.minfo2,100000) dayLimitAmt from hrt_merchacc t, pg_merchlimit t1 "
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
		if (unno.contains(daeUnno)) {
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
				+ "       from ( select  userid,count(1) c from plusr.pg_wechat_txn where  lmdate between trunc(sysdate,'dd') and sysdate  and mer_id=? and txnamt>9000 and paytype='2' group by  userid ) t "
				+ "      where t.c>=3 ";
		List<Map<String, Object>> list=dao.queryForList(chOrdSql, merid);
		if (list.size()>0) {
			throw new HrtBusinessException(8000,"今日交易次数超限，暂时无法进行交易");
		}
		 
	}
	
}
