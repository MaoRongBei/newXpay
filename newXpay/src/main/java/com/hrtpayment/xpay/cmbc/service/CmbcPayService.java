package com.hrtpayment.xpay.cmbc.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.WxpayService;
import com.hrtpayment.xpay.quickpay.cups.util.CTime;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Service
public class CmbcPayService implements WxpayService{
	private final Logger logger = LogManager.getLogger();
	
	@Value("dae.defaultGroup")
	private String daeDefaultGroup;
	
	@Value("dae.optUnno")
	private String daeOptUnno;
	

	@Autowired
	JdbcDao dao;

	public String getMerchantCode (String unno, String mid, String payway) throws BusinessException{
		int fiid = 0;
		if ("WXZF".equals(payway)) {
			fiid = 11;
		} else if ("ZFBZF".equals(payway)) {
			fiid = 12;
		} else throw new BusinessException(8002,"不支持的支付通道");
		return getMerchantCode(unno,mid,fiid);
	}
	public String getMerchantCode (String unno, String mid, int fiid) throws BusinessException{
		if (fiid!=11 && fiid!=12) throw new BusinessException(8002,"不支持的支付通道");
		
		List<Map<String, Object>> list;
		if (unno==null || "110000".equals(unno)) {
			String sql = "select a.merchantcode from Bank_MerRegister a,HRT_MerBanksub b,"
					+ "Hrt_Merchacc ma where ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and a.approvestatus='Y' and a.status=1 "
					+ "and ma.hrt_MID =? and a.fiid=?";
			list = dao.queryForList(sql, mid,fiid);
		} else {
			String sql = "select a.merchantcode from Bank_MerRegister a,HRT_MerBanksub b,"
					+ "Hrt_Merchacc ma where ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and a.approvestatus='Y' and a.status=1"
					+ "and ma.hrt_MID =? and a.fiid=? and ma.unno=?";
			list = dao.queryForList(sql, mid,fiid,unno);
		}
		if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
		Map<String,Object> map = list.get(0);
		String merchantCode = (String) map.get("MERCHANTCODE");
		return merchantCode;
	}
	
	
	/**
	 * 最新从轮寻组中获取银行商户号 公众号，支付宝扫码，微信扫码 全支持
	 * @param unno
	 * @param mid
	 * @param payway
	 * @return
	 * @throws BusinessException
	 */
	public Map<String, Object> getMerchantCode3 (String unno, String mid, String payway,BigDecimal amount) throws BusinessException{
		List<Map<String, Object>> list;
		//111000用于测试  生产 不提交||"111000".equals(unno)
		if (unno==null || "110000".equals(unno) ||"880000".equals(unno)) {
			String sql = "select a.merchantcode,a.storeid,a.fiid,a.merchantid ,a.orgcode,a.minfo2,a.cdate,a.category,a.merchantaddress,"
						+ "a.appid,a.shortname,a.channel_id,a.mch_id from Bank_MerRegister a,"
						+ "(select hrid,status,hrt_MID  from Hrt_Merbanksub where hrt_MID =?) b,"
						+ "Hrt_Merchacc ma,hrt_fi f where "
						+ "ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and "
						+ "a.fiid=f.fiid and a.approvestatus='Y' and a.status=1 "
						+ "and b.status=1 and ma.status=1 and f.fiinfo2 like ? and ma.hrt_MID =? order by a.fiid";
			
			list = dao.queryForList(sql, mid,"%"+payway+"%", mid);
		} else {
			String sql = "select a.merchantcode,a.storeid,a.fiid,a.merchantid ,a.orgcode,a.minfo2,a.cdate,a.category,a.merchantaddress,"
						+ "a.appid,a.shortname ,a.channel_id,a.mch_id from Bank_MerRegister a,"
						+ "(select hrid,status,hrt_MID  from Hrt_Merbanksub where hrt_MID =?) b,"
						+ "Hrt_Merchacc ma,hrt_fi f where "
						+ "ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and "
						+ "a.fiid=f.fiid and a.approvestatus='Y' and a.status=1 "
						+ "and b.status=1 and ma.status=1 and f.fiinfo2 like ? and ma.unno=? "
						+ "and ma.hrt_MID =?  order by a.fiid";
			list = dao.queryForList(sql, mid,"%"+payway+"%",unno, mid);
		}
		if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
		Map<String,Object> map = list.get(0);
		String fiid =String.valueOf(map.get("FIID"));
		if("99".equals(fiid)){
//			logger.info("哈哈哈哈哈。。。。。。走轮序组了！");
			String gorupName= String.valueOf(map.get("MERCHANTID"));
			String orderTime=CTime.formatDate(new Date(), "HHMI");
			String poolSql=" select * from (select t1.hpid,t2.merchantcode,t2.storeid,t2.fiid ,T2.orgcode,T2.minfo2,"
							+ "T2.cdate,T2.category,T2.merchantaddress,T2.appid,T2.mch_id,T1.txnmaxcount  from hrt_termaccpool T1,"
							+ " bank_merregister T2,hrt_fi f WHERE t1.btaid=t2.hrid"
							+ " and T2.fiid=f.fiid and f.fiinfo2 like ? and T1.status=1 "
							+ " and t1.txnmaxamt>=nvl(t1.txnamt,0)+? and t1.groupname=? "
							+ " and t1.txnmaxcount>=nvl(t1.txncount,0)+?  "  //增加金额判断
							+ " and ? between nvl(t1.starttime,'0000') and nvl(t1.endtime,'2359') "//增加时间判断
							+ " and T2.status=1 order by T1.txnamt asc,txncount,txnmaxcount desc) where rownum=1 ";
			list = dao.queryForList(poolSql, "%"+payway+"%",amount,gorupName,orderTime);
			
			/*
			 * 2018-11-26  修改
			 * 
			 * 当list.size <1 时判断  gorupName 是否含有DAE 
			 * true： 则为大额地域分组，跳转到默认组 daeDefaultGroup 内取商户号 进行交易
			 *       如果 daeDefaultGroup 组内也没有可用商户号 返回 8001
			 * false：返回 8001
			 * 
			 */
			if (list.size()<1){
				if (gorupName.toUpperCase().contains("DAE")) {
					gorupName=daeDefaultGroup;
					list = dao.queryForList(poolSql, "%"+payway+"%",amount,gorupName,orderTime);
					if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
				}else {
					throw new BusinessException(8001,"指定通道未开通");
				}
			}
			
			/*
			 * 
			 * 2018-11-26 修改
			 * 
			 * daeOptUnno 包含  unno 进行判断 
			 * 判断点： 
			 * 1、上送
			 * 
			 */
 
			for(Map<String, Object> mm :list){
				Integer hpid = Integer.parseInt(String.valueOf(mm.get("HPID")));
				Integer txnmaxcount = Integer.parseInt(String.valueOf(mm.get("TXNMAXCOUNT")));
				/*
				 * 2018-11-26 修改 
				 * 
				 * 如果gorupName内不含有DAE时 当最大值既减少至小于等于1时
				 * 修改最大值为99999999
				 * 
				 */
				if (txnmaxcount<=1 && !gorupName.toUpperCase().contains("DAE")) {
					String updateTxnmaxcountSql=" update HRT_TERMACCPOOL t  set  txnmaxcount = '99999999' "  
							+ " where t.status=1  and t.groupname=? ";
					dao.update(updateTxnmaxcountSql, gorupName);
				}
				String updateSql=" update HRT_TERMACCPOOL t set t.txnamt=nvl(t.txnamt,0)+?,"
								+ " t.txncount=t.txncount+1 , txnmaxcount = to_char(txnmaxcount-1) " //(case txnmaxcount when 1 then  '9999999' else to_char(txnmaxcount-1) end) "
								+ " where t.status=1 and "
								+ " t.txnmaxamt>=nvl(t.txnamt,0)+? and t.hpid=?";
				Integer count=dao.update(updateSql, amount,amount,hpid);
				if(count>0){
					return mm;
				}
			}
			throw new BusinessException(8001,"指定通道未开通");
//		}else if("19".equals(fiid)){
//			String querySql="select a.bankmid MERCHANTCODE,a.fiid fiid from HRT_SCANMIDRELATIONSUB a where a.hrtMID = ? and a.fiid=?";
//			list =dao.queryForList(querySql, mid,fiid);
//			if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
//			return list.get(0);
//		}else if("20".equals(fiid)){
//			String querySql="select a.bankmid MERCHANTCODE,a.fiid fiid from HRT_SCANMIDRELATIONSUB a where a.hrtMID = ? and a.fiid=?";
//			list =dao.queryForList(querySql, mid,fiid);
//			if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
//			return list.get(0);
		}
		return map;
	}
	
	
	/**
	 * 检测订单号是否重复
	 * @param orderid
	 * @throws BusinessException
	 */
	private void checkRepeatOrderid(String orderid) throws BusinessException{
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn where mer_orderid=? and rownum=1", orderid);
		if (list.size()>0) throw new BusinessException(9007, "订单号重复");
	}
	
	/**
	 * 公众号支付 获取  appid、secret
	 * @param merchantCode
	 * @return
	 * @throws BusinessException
	 */
	public  Map<String, Object>  getAppidInfo(String merchantCode) throws BusinessException{
		String querySql="select appid,secret from bank_merregister where merchantCode= ? ";
		List<Map<String, Object>> list=null;
		try{
			if (list.size()!=0) {
				list=dao.queryForList(querySql, merchantCode);
				return list.get(0);
			}else{
				return null;
			}
		}catch(Exception e){
			throw new BusinessException(4000, "操作数据库失败");
		}	
	} 
	
	
	public String getWxAppid(String merchantCode){
		
		String sql="select APPID from bank_merregister t where t.merchantcode=?";
		List<Map<String, Object>> list =dao.queryForList(sql, merchantCode);
		if(list.size()>0){
			String newAppid=String.valueOf(list.get(0).get("APPID"));
			if(newAppid!=null && !"".equals(newAppid) && !"null".equals(newAppid)){
				return newAppid;
			}else{
				return getWxpayAppid();
			}
		}else{
			return getWxpayAppid();
		}
	}
	@Override
	public String getWxpayAppid() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getWxpayPayInfo(String orderid, String openid) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getWxpaySecret() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int getWxpayFiid() {
		// TODO Auto-generated method stub
		return 0;
	}


}
