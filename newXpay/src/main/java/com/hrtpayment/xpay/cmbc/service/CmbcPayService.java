package com.hrtpayment.xpay.cmbc.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.cmbc.bean.json.HrtCmbcBean;
import com.hrtpayment.xpay.common.dao.HdbJdbcDao;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.WxpayService;
import com.hrtpayment.xpay.quickpay.cups.util.CTime;
import com.hrtpayment.xpay.redis.RedisUtil; 
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

@Service
public class CmbcPayService implements WxpayService{
	private final Logger logger = LogManager.getLogger();
	
	@Value("${dae.defaultGroup}")
	private String daeDefaultGroup;
	
	@Value("${dae.mpos.defaultGroup}")
	private String daeMposDefaultGroup;
	@Value("${dae.mpos.byGroup}")
	private String daeMposByGroup;
	@Value("${dae.syt.defaultGroup}")
	private String daeSYTDefaultGroup;
	@Value("${dae.mpos.step}")
	private String daeMposStep;
	@Value("${dae.mpos.differStep}")
	private String daeMposDifferStep;
	@Value("${dae.mpos.areaStep}")
	private String daeMposAreaStep;
	@Value("${dae.mpos.wxtxnavg}")
	private String wxTxnAvg;
	@Value("${dae.mpos.wxcreditradio}")
	private String wxCreditRadio;
	@Value("${dae.mpos.zfbtxnavg}")
	private String zfbTxnAvg;
	@Value("${dae.mpos.zfbcreditradio}")
	private String zfbCreditRadio;
	@Value("${dae.mpos.by.wxTimes}")
	private String byTimesWx;
	@Value("${dae.mpos.by.zfbTimes}")
	private String byTimesZfb;
	@Value("${dae.mpos.qt.smallAmt}")
	private String qtSmallAmt;
	@Value("${dae.mpos.bj01.name}")
	private String bj01Name;
	@Value("${dae.mpos.bj01.times}")
	private String bj01Times;
	@Value("${dae.mpos.bj01.topAmt}")
	private String bj01TopAmt;
	@Value("${dae.mpos.bj01.botmAmt}")
	private String bj01BotmAmt;
	
	@Value("${dae.unno}") 
	private String daeUnno;
	
	@Value("${hyb.special.group.name}")
	private String hybSpecialGroupName;
	@Value("${hyb.group.times}")
	private String hybGroupTimes;
	@Value("${hyb.group.topAmt}")
	private String hybGroupTopAmt;
	@Value("${hyb.group.botmAmt}")
	private String hybGroupBotmAmt;
	@Value("${hyb.group.specialArea}")
	private String hybGroupSpecialArea;
	

	@Autowired
	JdbcDao dao;
	@Autowired
	HdbJdbcDao hdbDao;
   
	/*
	 * 2018-12-11 修改 
	 * 停止使用下述配置参数
	 * 原因：手刷交易只累增成功金额
	 */
//	@Value("${xpay.addAmtByGp.name}")
//	String addGroupName;
//	@Value("${xpay.maxAmtForTerms.wx}")
//	long wxMaxAmt;
//	@Value("${xpay.maxAmtForTerms.zfb}")
//	long zfbMaxAmt;
	@Value("${xpay.special.unno}")
	String specUnno; 
	
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
	public Map<String, Object> getMerchantCode3 (String unno, String mid, String payway,BigDecimal amount,String area,String paytype) throws BusinessException{
		List<Map<String, Object>> list=null;
		String orderTime=CTime.formatDate(new Date(), "HHMI");  //请求下单时间
		/*
		 * 2019-03-06 修改  
		 * 判断商户上是否挂了可用的轮询组 
		 * 如果有判断轮询组名是否是 LZLXBJ  
		 * 是去里面捞可用商户号 ，不是  执行原有规则 
		 * 捞到商户号后
		 * 组名设置进REDIS
		 * 轮询组 组名时长有效时长设置进redis  按照  3分钟内不允许交易处修改  
		 * LZLXB+txncount为主键 
		 * 判断 LZLXB+（txncount-1）存在时  查询超时时长 
		 * 如果不限时长  先设置再返回
		 * 如果限制了直接返回 走原有规则
		 * 判断 LZLXB+（txncount-1）不存在时
		 * 新增key  然后走LZLXBJ内捞到的值
		 */
		
		//查询商户挂号  是否挂了 轮询组  99  LZLXBJ
		
		/*
		 * 1、获取组名 、交易起始时间  交易结束时间、交易金额区间
		 */
		try {
			hybSpecialGroupName="".equals(RedisUtil.getProperties("hybSpecialGroupName"))?hybSpecialGroupName:RedisUtil.getProperties("hybSpecialGroupName");
		} catch (Exception e) {
		    logger.error("[会员宝--特殊商户组] 会员宝特殊商户组 取号异常，hybSpecialGroupName 取配置文件内默认值：{}",hybSpecialGroupName);
		}
		try {
			hybGroupTimes="".equals(RedisUtil.getProperties("hybGroupTimes"))?hybGroupTimes:RedisUtil.getProperties("hybGroupTimes");
		} catch (Exception e) {
		    logger.error("[会员宝--特殊商户组] 会员宝特殊商户组{} 交易时间异常，hybGroupTimes 取配置文件内默认值：{}",hybSpecialGroupName,hybGroupTimes);
		}
		try {
			hybGroupTopAmt="".equals(RedisUtil.getProperties("hybGroupTopAmt"))?hybGroupTopAmt:RedisUtil.getProperties("hybGroupTopAmt");
		} catch (Exception e) {
		    logger.error("[会员宝--特殊商户组] 会员宝特殊商户组{} 交易金额上限异常，hybGroupTopAmt 取配置文件内默认值：{}",hybSpecialGroupName,hybGroupTopAmt);
		}
		try {
			hybGroupBotmAmt="".equals(RedisUtil.getProperties("hybGroupBotmAmt"))?hybGroupBotmAmt:RedisUtil.getProperties("hybGroupBotmAmt");
		} catch (Exception e) {
		    logger.error("[会员宝--特殊商户组] 会员宝特殊商户组{} 交易金额下限异常，hybGroupTopAmt 取配置文件内默认值：{}",hybSpecialGroupName,hybGroupBotmAmt);
		}
		try {
			hybGroupSpecialArea="".equals(RedisUtil.getProperties("hybGroupSpecialArea"))?hybGroupSpecialArea:RedisUtil.getProperties("hybGroupSpecialArea");
		} catch (Exception e) {
		    logger.error("[会员宝--特殊商户组] 会员宝特殊商户组{} 交易金额下限异常，hybGroupSpeciialArea 取配置文件内默认值：{}",hybGroupSpecialArea);
		}
		String[] hybGroupTime=hybGroupTimes.split(";");
		String start="";
		String end="";
		boolean flag=false;
		for (int i = 0; i < hybGroupTime.length; i++) {
			start=hybGroupTime[i].split("-")[0];
			end=hybGroupTime[i].split("-")[1];
			if (orderTime.compareTo(start)>=0 && orderTime.compareTo(end)<=0 ) {
				flag=true;
				break;
			} 
		}
		int sleepTime=(int)(Math.random()*10+1);
		
		SimpleDateFormat sFormat= new SimpleDateFormat("yyyyMMddHHmmss");
		String nowTime=sFormat.format(new Date());
		Calendar calendar=Calendar.getInstance();   
		calendar.add(Calendar.MINUTE, sleepTime);
		Date date=calendar.getTime();
		String afterTime=sFormat.format(date);
		String txncount="";
		boolean checkFlag=false;
		//2、北京-被扫交易-交易时间在1100-1500;1700-2200；-交易金额在100-1000间  走本轮询组
		if ("BS".equals(paytype)&&null!=area&&!"".equals(area)&&!"null".equals(area)&&hybGroupSpecialArea.contains(area)&&flag==true&&amount.compareTo(new BigDecimal(hybGroupBotmAmt))>=0&&amount.compareTo(new BigDecimal(hybGroupTopAmt))<=0) {
			String LXBJSql="select *  "
					+ "       from (select a.merchantid,a.fiid    "
					+ "       		  from  bank_merregister a ,"
					+ "                    (select hrid,status,hrt_MID  from  Hrt_Merbanksub where hrt_MID =?) b,"
					+ "				       Hrt_Merchacc ma "
					+ "              where  a.hrid=b.hrid and ma.hrt_mid=b.hrt_mid and ma.status=1 "
					+ "                and a.fiid=99 and a.merchantid=? and a.approvestatus='Y' "
					+ "                and a.status=1  and b.status=1  ) t1,"
					+ "           (select nvl(TXNCOUNT,0) txncount ,COUNT(1) counts ,groupname from  hrt_termaccpool  where  groupname =?  AND STATUS='1' and txncount<txnmaxcount GROUP BY TXNCOUNT,groupname ) t2 "
					+ "      where t1.merchantid= t2.groupname order by txncount asc ";
			logger.info("[会员宝--特殊商户组] 商户属于北京地区  订单{}交易金额{} 在 {}~{}之间 ，时间{}在{}之间，走{}零费率组",mid,amount,hybGroupBotmAmt,hybGroupTopAmt,orderTime,hybGroupTimes,hybSpecialGroupName);
			if ("WXZF".equals(payway)) {
				list = dao.queryForList(LXBJSql, mid, hybSpecialGroupName,hybSpecialGroupName);
//			}else if ("ZFBZF".equals(payway)) {
//				list = dao.queryForList(LXBJSql, mid, hybSpecialGroupName,hybSpecialGroupName);
			}
			/*
			 * 当list.size >1时 表示 当前轮询组可走商户号，走轮询组
			 * 当list.size =0时 表示 当前轮询组没有符合要求的商户号
			 * 当list.size =1且txncount！=0时 表示当前轮询组已经有过交易 进入redis 内限制时间 
			 * 当list.size =1且txncoun=0时 表示当前轮询组没有走交易，可直接走交易 
			 */
			if (list!=null&&list.size()==1) {
				txncount=list.get(0).get("txncount")+"";
				if (!"0".equals(txncount)) {
					logger.info("[会员宝--特殊商户组] txncount={}，间隔{}s后 走本轮询组的商户号码。",txncount,sleepTime*60);
					checkFlag=RedisUtil.checkSpecialGroupTime(hybSpecialGroupName, nowTime,afterTime);
				}else{
					logger.info("[会员宝--特殊商户组] txncount={}，轮询组{}首次取号。",txncount,hybSpecialGroupName);
				}
			}
		}
		if (unno==null || "110000".equals(unno) ||specUnno.contains(unno)) {
			String sql = "select a.merchantcode,a.storeid,a.fiid,a.merchantid ,a.orgcode,a.minfo2,a.cdate,a.category,a.merchantaddress,"
						+ "a.appid,a.shortname,a.channel_id,a.mch_id,a.iscredit from Bank_MerRegister a,"
						+ "(select hrid,status,hrt_MID  from Hrt_Merbanksub where hrt_MID =?) b,"
						+ "Hrt_Merchacc ma,hrt_fi f where "
						+ "ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and "
						+ "a.fiid=f.fiid and a.approvestatus='Y' and a.status=1 "
						+ "and b.status=1 and ma.status=1 and f.fiinfo2 like ? and ma.hrt_MID =? order by a.fiid";
			if (list==null||list.size()==0||checkFlag) { //(list.size()==1&&"0".equals(txncount))||
			   list = dao.queryForList(sql, mid,"%"+payway+"%", mid);
			}
		} else {
			String sql = "select a.merchantcode,a.storeid,a.fiid,a.merchantid ,a.orgcode,a.minfo2,a.cdate,a.category,a.merchantaddress,"
						+ "a.appid,a.shortname ,a.channel_id,a.mch_id,a.iscredit from Bank_MerRegister a,"
						+ "(select hrid,status,hrt_MID  from Hrt_Merbanksub where hrt_MID =?) b,"
						+ "Hrt_Merchacc ma,hrt_fi f where "
						+ "ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid and "
						+ "a.fiid=f.fiid and a.approvestatus='Y' and a.status=1 "
						+ "and b.status=1 and ma.status=1 and f.fiinfo2 like ? and ma.unno=? "
						+ "and ma.hrt_MID =?  order by a.fiid";
			if (list==null||list.size()==0||checkFlag) { //||(list.size()==1&&"0".equals(txncount))
				list = dao.queryForList(sql, mid,"%"+payway+"%",unno, mid);
			}
		}
		if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
		Map<String,Object> map = list.get(0);
		String fiid =String.valueOf(map.get("FIID"));
		boolean deflag=true;
		if("99".equals(fiid)){
//			logger.info("哈哈哈哈哈。。。。。。走轮序组了！");
			String gorupName= String.valueOf(map.get("MERCHANTID"));
			/*
			 * 2018-12-07 修改
			 * 
			 * 按照轮询组+payway 累增金额
			 * wx、zfb金额限制最大值  存放在 redis内
			 * 利用redis进行操作
			 * 
			 * 
			 * 2018-12-11 禁用下述代码
			 * 
			 * 原因：只累增成功金额 失败金额不计金额内
			 * 
			 */
//			if (addGroupName.contains(gorupName)) {
//				Double amt=amount.doubleValue();
//				if (payway.contains("WX")) {
////					wxMaxAmt= Long.parseLong(RedisUtil.getProperties("wxLimit"));
//					RedisUtil.addAmtByGroupName("WX",amt, wxMaxAmt);
//				}else if (payway.contains("ZFB")) {
////					zfbMaxAmt= Long.parseLong(RedisUtil.getProperties("zfbLimit"));
//					RedisUtil.addAmtByGroupName("ZFB",amt,zfbMaxAmt);
//				} 
//			 }
//			String orderTime=CTime.formatDate(new Date(), "HHMI");
            /*
             *2018-11-27 修改
             * 
             * 1、j62077 选择与area上送的省份一直的轮询组进行交易，如果上送过来的交易没有匹配到组 就默认走 JUHEPAY03
             * 2、判断上送交易所挂商户号 是否挂在轮询组上，
             *    如果是就根据地区 area 查询组与地区组间交集的商户号 随机选择一个
             *    如果没有交集就默认从JUHEPAY01（商户本身挂的组）下按照原有规则取号
             * 3、新增判断条件  时间、最大金额
             * 
             * 步骤2 小额交易，剔出机构号为962073的商户
             *
             */
			String poolDefSql=" select * from (select t1.hpid,t2.merchantcode,t2.storeid,t2.fiid ,T2.orgcode,T2.minfo2,"
					+ "T2.cdate,T2.category,T2.merchantaddress,T2.appid,T2.shortname,T2.mch_id,T2.channel_id,T2.iscredit,T1.txnmaxcount  from hrt_termaccpool T1,"
					+ " bank_merregister T2,hrt_fi f WHERE t1.btaid=t2.hrid"
					+ " and T2.fiid=f.fiid and f.fiinfo2 like ? and T1.status=1 "
					+ " and t1.txnmaxamt>=nvl(t1.txnamt,0)+? and t1.txnmaxcount>=nvl(t1.txncount,0)+1 and t1.groupname=? "
					+ " and ? between nvl(CASE t1.starttime WHEN 'null' then '0000' else t1.starttime  end ,'0000') and nvl(CASE t1.endtime WHEN 'null' then '2359' else t1.endtime  end ,'2359')  "
					+ " and T2.status=1 ";
			if ("LZLXBJ".equals(gorupName)) {
				poolDefSql=poolDefSql+ " order by T1.txncount asc) where rownum=1 ";
			}else{
				poolDefSql=poolDefSql+ " order by T1.txnamt asc,txnmaxcount desc) where rownum=1 ";
			}
			if (null!=area&&!"".equals(area.trim())&&!"null".equals(area.trim())&&!daeUnno.contains(unno)) {
				String poolSql="select * from (select t.* ,ht.hpid,ht.txnmaxcount from  hrt_termaccpool ht,"
						+ " (select hrid,merchantcode,storeid,bm.fiid, merchantid , orgcode, minfo2, bm.cdate, category, merchantaddress, appid,shortname ,channel_id,mch_id   "
						+ "    from hrt_termaccpool ht ,hrt_fi hi,bank_merregister bm "
						+ "   where 1=1 and hi.status='1' and hi.fiinfo2 like ? "
						+ "     and hi.fiid= bm.fiid and bm.status='1' and bm.approvestatus='Y' and ht.btaid=bm.hrid"
						+ "     and ht.status='1'  and ht.txnmaxamt>=nvl(ht.txnamt,0)+? and ht.txnmaxcount>=nvl(ht.txncount,0)+1"
						+ "     and ? between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
						+ "     and ht.GROUPNAME=(select MERCHANTID from bank_merregister where fiid=99 and merchantid like 'DAE%' and  merchantname like ?  and status='1')"
						+ "  intersect "
						+ "  select  hrid,merchantcode,storeid,bm.fiid, merchantid , orgcode, minfo2, bm.cdate, category, merchantaddress, appid,shortname ,channel_id,mch_id    "
						+ "    from hrt_termaccpool ht ,hrt_fi hi,bank_merregister bm "
						+ "   where 1=1 and hi.status='1' and hi.fiinfo2 like ? "
						+ "     and hi.fiid= bm.fiid and bm.status='1' and bm.approvestatus='Y' and ht.btaid=bm.hrid "
						+ "     and ht.status='1'  and ht.txnmaxamt>=nvl(ht.txnamt,0)+? and ht.txnmaxcount>=nvl(ht.txncount,0)+1"
						+ "     and ? between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')  and ht.GROUPNAME=?) t "
						+ " where 1=1 and ht.btaid=t.hrid and  GROUPNAME=? and  status='1' "
						+ " order by txncount) where rownum=1 ";
				list = dao.queryForList(poolSql,"%"+payway+"%",amount,orderTime,"%"+area+"%","%"+payway+"%",amount,orderTime,gorupName,gorupName);
				if (list.size()==0) {
					list = dao.queryForList(poolDefSql, "%"+payway+"%",amount,gorupName,orderTime);
				}
				
				if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
			}else{
				if("SQ".equals(gorupName)){
					String poolDefSql2=" select * from (select t1.hpid,t2.merchantcode,t2.storeid,t2.fiid ,T2.orgcode,T2.minfo2,"
							+ "T2.cdate,T2.category,T2.merchantaddress,T2.appid,T2.shortname,T2.mch_id,T2.channel_id,T2.iscredit,T1.txnmaxcount  from hrt_termaccpool T1,"
							+ " bank_merregister T2,hrt_fi f WHERE t1.btaid=t2.hrid"
							+ " and T2.fiid=f.fiid and f.fiinfo2 like ? and T1.status=1 AND T1.FLAG=2 "
							+ " and t1.txnmaxamt>=nvl(t1.txnamt,0)+? and t1.txnmaxcount>=nvl(t1.txncount,0)+1 and t1.groupname=? "
							+ " and ? between nvl(CASE t1.starttime WHEN 'null' then '0000' else t1.starttime  end ,'0000') and nvl(CASE t1.endtime WHEN 'null' then '2359' else t1.endtime  end ,'2359')  "
							+ " and T2.status=1 order by T1.txnamt asc,txnmaxcount desc) where rownum=1 ";
					// 优先查询出SQ轮询组 需要配送大额手刷交易的号
					list = dao.queryForList(poolDefSql2, "%"+payway+"%",amount,gorupName,orderTime);
					if(list.size()<1){
						deflag=false;
						list = dao.queryForList(poolDefSql, "%"+payway+"%",amount,gorupName,orderTime);
						if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
					}
				}else{
					deflag=false;
					list = dao.queryForList(poolDefSql, "%"+payway+"%",amount,gorupName,orderTime);
					if (list.size()<1) throw new BusinessException(8001,"指定通道未开通");
				}
			}
			
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
				if (txnmaxcount<=1 && !gorupName.toUpperCase().contains("DAE")&&!deflag) {
					String updateTxnmaxcountSql=" update HRT_TERMACCPOOL t  set  txnmaxcount = '99999999' "  
							+ " where t.status=1  and t.groupname=?";
					dao.update(updateTxnmaxcountSql, gorupName);
				}
				String updateSql=" update HRT_TERMACCPOOL t set t.txnamt=nvl(t.txnamt,0)+? "
								+ " ,t.txncount=t.txncount+1  ";
				if (!gorupName.toUpperCase().contains("DAE")&&!deflag) {
					updateSql=updateSql+ " ,txnmaxcount = to_char(txnmaxcount-1) " ;
				}
				updateSql=updateSql	+ " where t.status=1 and "
								+ " t.txnmaxamt>=nvl(t.txnamt,0)+? and t.hpid=?";
				Integer count=dao.update(updateSql, amount,amount,hpid);
				if(count>0){
					return mm;
				}
			}
			throw new BusinessException(8001,"指定通道未开通");
		}
		return map;
	}
	
	
 
    /**
     * 
     * 2018-12-19 新增
     * 
	 * 手刷大额专用轮询组操作方法
	 * 
	 * 1、根据商户号选择出商户上一次登录的地址，根据地址获取对应的轮询组 
	 *    暂定从手刷数据库内查询商户上一次的登录地址
	 * 
	 * 2、查询商户号条件添加： 按照设定好的实体商户笔数增值进行匹配大额交易 
	 * 
	 * 3、查询商户号条件添加：按照银行商户号笔均交易额度
	 *    需求不定 暂未实现
	 * 
	 * 4、查询商户号条件添加：按照银行商户号借贷比  
	 *    需求不定 暂未实现
	 * 
	 * 手刷交易整体走轮询组，不支持单独挂号操作
	 * 
	 * 按照地域查找对应的轮询组名进行银行商户号的选取，当不符合笔增等要求时，白天的交易走默认组内的商户号，夜间交易均走默认组内的夜间商户号
	 * 商户号选择：地区组  （银行商户号当前累增的最大交易笔数+1）*笔增<银行商户号实体交易笔数  + 时间点控制 +flag=2（原因：与会员宝大额商户组公用一个组名，用来区分交易来源）；取号： 按照大额交易笔数升序、实体交易降序排列，选择第一条数据
	 *            默认组  时间符合要求+最大金额>=当前累增金额+当笔交易金额+最大笔数>=当前笔数+1；取号： 按照交易笔数升序、总交易笔数降序取一条
	 *                  日间交易时间 starttime<endtime  且 交易时间 between starttime and endtime 
	 *                  夜间交易时间 starttime>endtime  且((交易时间>starttime and 交易时间>endtime) or (交易时间<starttime and  交易时间<endtime))
	 * 
	 * 2019-02-15 修改
	 * 1、为北京地区商户新增轮询组 SSJHBJ01组（零费率商户组） 
	 *    上送交易金额为300—5000
	 * 	     交易时间为11:00—14:00、18:00—23:00
	 * 2、创建 SSJHBY 优先级在SSJHQT前 
	 *    交易配比为手刷：立码富=1:35
	 *    交易时间限制支付宝（14：00—16:00、20:00-21:00）微信（18:00-19:00）
	 * 3、单笔交易小于100元的默认走QT轮询组
	 * 
     * @param bean
     * @return
     */
	public Map<String, Object> getMerchantForMpos(HrtCmbcBean bean){
		/*
		 * 基础数据准备
		 */
		String hrt_mid=bean.getMid();  //和融通商户号
		String payway=bean.getPayway(); //支付方式  WXPAY 微信支付  ZFBZF支付宝支付  
		String orderTime=CTime.formatDate(new Date(), "HHMI");  //请求下单时间
		String amount=String.valueOf(bean.getAmount());//大额交易金额
		String txnavg="";
		String creditRadio="";
		try {
			daeMposDefaultGroup=RedisUtil.getProperties("mposDefaultGroup");//默认组组名 （组内含有日间默认商户及夜间商户）
		} catch (Exception e) {
			logger.error("[大额--手刷]获取默认组名称异常：{},改使用配置文件内的手刷默认组名：{}",e.getMessage(),daeMposDefaultGroup);
		}
		try {
			daeMposByGroup=RedisUtil.getProperties("daeMposByGroup");//备用组租名  (组内商户号为立码富商户号)
		} catch (Exception e) {
			logger.error("[大额--手刷]获取备用组名称异常：{},改使用配置文件内的手刷备用组组名：{}",e.getMessage(),daeMposByGroup);
		}
		try {
			daeSYTDefaultGroup="".equals(RedisUtil.getProperties("sytDefaultGroup"))?daeSYTDefaultGroup:RedisUtil.getProperties("sytDefaultGroup");//默认组组名 （组内含有日间默认商户及夜间商户）
		} catch (Exception e) {
			logger.error("[大额--手刷]获取收银台专用组名称异常：{},改使用配置文件内的收银台专用组名：{}",e.getMessage(),daeSYTDefaultGroup);
		}
		try {
			bj01Name=RedisUtil.getProperties("bj01Name");//零费率组组名 
		} catch (Exception e) {
			logger.error("[大额--手刷]获取零费率组名称异常：{},改使用配置文件内的手刷零费率组组名：{}",e.getMessage(),bj01Name);
		}
		try {
			daeMposStep=RedisUtil.getProperties("daeMposStep"); //设置的配置大额交易的笔增值
		} catch (Exception e) {
			logger.error("[大额--手刷]获取实体商户配比笔数异常：{},改使用配置文件内的实体商户配比笔数：{}",e.getMessage(),daeMposDefaultGroup);
		}
		
		/*
		 * 2018-12-29 修改
		 * 
		 * 新增字段 
		 * 微信、支付宝贷记比例、成功交易笔均
		 * 按日累计交易笔数基数
		 * 
		 */
		if ("WXPAY".equals(payway)||"WXZF".equals(payway)) {
			try {
			    creditRadio=RedisUtil.getProperties("wxCreditRadio");//获取微信贷记交易比例 
			} catch (Exception e) {
				logger.error("[大额--手刷]获取贷记交易比例异常：{},改使用配置文件内的贷记交易比例：{}",e.getMessage(),wxCreditRadio);
				creditRadio=wxCreditRadio;
			}
			try {
				txnavg=RedisUtil.getProperties("wxTxnAvg");//获取微信成功交易笔均
			} catch (Exception e) {
				logger.error("[大额--手刷]获取成功交易笔均异常：{},改使用配置文件内的成功交易笔均：{}",e.getMessage(),wxTxnAvg);
				txnavg=wxTxnAvg;
			}
			try {
				byTimesWx=RedisUtil.getProperties("byTimesWx");//获取微信成功交易笔均
			} catch (Exception e) {
				logger.error("[大额--手刷]获取SSJHBY轮询组微信交易时间段异常：{},改使用配置文件内的SSJHYB轮询组微信交易时间段：{}",e.getMessage(),byTimesWx);
			}
		}else if ("ZFBZF".equals(payway)) {
			try {
			    creditRadio=RedisUtil.getProperties("zfbCreditRadio");//获取支付宝贷记交易比例 
			} catch (Exception e) {
				logger.error("[大额--手刷]获取贷记交易比例异常：{},改使用配置文件内的贷记交易比例：{}",e.getMessage(),zfbCreditRadio);
				creditRadio=zfbCreditRadio;
			}
			try {
				txnavg=RedisUtil.getProperties("zfbTxnAvg");//获取支付宝成功交易笔均
			} catch (Exception e) {
				logger.error("[大额--手刷]获取成功交易笔均异常：{},改使用配置文件内的成功交易笔均：{}",e.getMessage(),zfbTxnAvg);
				txnavg=zfbTxnAvg;
			}
			try {
				byTimesZfb=RedisUtil.getProperties("byTimesZfb");//获取微信成功交易笔均
			} catch (Exception e) {
				logger.error("[大额--手刷]获取SSJHBY轮询组交易支付宝时间段异常：{},改使用配置文件内的SSJHYB轮询组支付宝交易时间段：{}",e.getMessage(),byTimesZfb);
			}
		}else{
			logger.error("[手刷--大额] payway异常");
			throw new HrtBusinessException(8000,"指定通道未开通");
		}
		/*
		 * 
		 * 2019-02-15 新增
		 * 
		 * 获取SSJHBJ01组 交易时间段
		 * 
		 */
		try {
			bj01Times=RedisUtil.getProperties("bj01Times");//SSJHBJ01组交易时间段
		} catch (Exception e) {
			logger.error("[大额--手刷]获取SSJHBJ01组交易时间段异常：{},改使用配置文件内SSJHBJ01组交易时间段：{}",e.getMessage(),bj01Times);
		}
		try {
			qtSmallAmt=RedisUtil.getProperties("qtSmallAmt");//小金额交易，默认直接走QT组
		} catch (Exception e) {
			logger.error("[大额--手刷]获取小金额交易金额上限异常：{},改使用配置文件内小金额交易金额上限值：{}",e.getMessage(),qtSmallAmt);
		}
		try {
			bj01TopAmt=RedisUtil.getProperties("bj01TopAmt");//SSJHBJ01组交易上限 
		} catch (Exception e) {
			logger.error("[大额--手刷]获取SSJHBJ01组交易金额上限异常：{},改使用配置文件内SSJHBJ01组交易金额上限值：{}",e.getMessage(),bj01TopAmt);
		}
		try {
			bj01BotmAmt=RedisUtil.getProperties("bj01BotmAmt");//SSJHBJ01组交易下限 
		} catch (Exception e) {
			logger.error("[大额--手刷]获取SSJHBJ01组交易金额下限异常：{},改使用配置文件内SSJHBJ01组交易金额下限值：{}",e.getMessage(),bj01TopAmt);
		}
		
		try {
			daeMposAreaStep=RedisUtil.getProperties("areaStep");//按日累计交易笔数限制步长
		} catch (Exception e) {
			logger.error("[大额--手刷]获取按日累计交易笔数异常：{},改使用配置文件内的按日累计交易笔数：{}",e.getMessage(),daeMposAreaStep);
		}
		/*
		 * 2019-03-18 修改
		 * 
		 * 修改步长差异 daeMposDifferAreaStep 
		 * 交易配比例  X：(XN-S(N-1))
		 * 
		 */
		try {
			daeMposDifferStep="".equals(RedisUtil.getProperties("differStep"))?daeMposDifferStep:RedisUtil.getProperties("differStep");//按日累计交易笔数限制步长
		} catch (Exception e) {
			logger.error("[大额--手刷]获取步长累计差异值：{},改使用配置文件内的累计差异值：{}",e.getMessage(),daeMposDifferStep);
		}
		List<Map<String, Object>> merInfoList= new ArrayList<Map<String, Object>>();
		//走默认组 日间交易
		/*
		 * 2018-12-26  修改
		 * 
		 * 更新默认组查询sql 
		 * 1、存在易融码商户时  按原有逻辑取数
		 * 2、易融码商户不满足时，走非易融码商户号  
		 *    非易融码商户号 按照失败笔数进行排序，优先走失败笔数小的 且失败笔数小于6的商户失败笔数大于6  按原有规则
		 *       
		 * 原有sql弃用 
		 * 2018-12-29 修改
		 * 易融码商户 规则跟 地区组一致
		 * 
		 * 2019-02-18 修改
		 * 取号逻辑   地区组 -->立码富组-->默认QT组
		 * 
		 * 2019-03-29 修改
		 * by组里取号规则更新更按照交易配比进行 取号   
		 * 
		 */
		//走会员宝轮询组  取号sql
		
		String queryMerHybSql= "select * from (select 4 flag, ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				+"					     bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit, ht.txnmaxcount "
				+"					from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				+"				   where ht.groupname = ? "
//				+"		             and  txnmaxcount>=? *(nvl(txncount,0)+1)"
				+"		             and  txnmaxcount>=(?-? )*(nvl(txncount,0)+1)+?"
				+"					 and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  /*and ht.flag='2'*/ and  bm.approvestatus='Y'  "
				+"					 and  hf.status='1' and hf.fiinfo2 like  ? "
				+"		             and  ? between nvl(CASE ? WHEN '' then '0000' else ?  end ,'0000') and nvl(CASE ? WHEN '' then '2359' else ?  end ,'2359')"
				+"		           order by txncount,txnmaxcount desc)   where rownum=1";
		//走默认组  日间取号sql
		String queryMerDefSql="select  * from ( "
				               + " SELECT * FROM  ( "
				               +"   select * from (select 0 rowflag,  ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				               +"                          bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txncount,ht.txnmaxcount,nvl(failcount,0) failcount,ht.flag "
				               +"                    from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				               +"                   where  ht.groupname = ?"
				               +"                     and  txncount<0  and ?>1000 and ht.flag='2'"
				               +"	                  and  txnmaxcount>= nvl(txncount,0)+1  and nvl(txnmaxamt,0)>=txnamt+?  "
				               +"                     and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				               +"                     and  hf.status='1' and hf.fiinfo2 like ?"
				               +"                     and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
				               +"                   ORDER BY txncount ,txnmaxcount desc) where rownum=1"
				               +"   union "
				               +"   select * from (select 1 rowflag,  ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				               +"                          bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txncount,ht.txnmaxcount,nvl(failcount,0) failcount,ht.flag "
				               +"                    from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				               +"                   where ht.groupname = ?"
				               +"                     and  nvl(succount,0)>=?  and ht.flag='2'"
				               +"	                  and  txnmaxcount>= nvl(txncount,0)+1  and nvl(txnmaxamt,0)>=txnamt+?  "
				               +"                     and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				               +"                     and  hf.status='1' and hf.fiinfo2 like ?"
				               +"                     and  nvl(ht.creditradio,0) <(select hml.creditradio from hrt_mcc_limit hml where bm.category= hml.mcc  and status='1' )"
				               +"   				  and  nvl(ht.txnavg,0) < (select hml.txnavg from hrt_mcc_limit hml where bm.category= hml.mcc  and status='1' ) "
				               +"                     and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
				               +"                   ORDER BY txncount ,txnmaxcount desc) where rownum=1"
				               +"   union "
				               +"   select * from (select  2 rowflag,   ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				               +"                          bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txncount,ht.txnmaxcount,nvl(failcount,0) failcount,ht.flag "
				               +"                    from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				               +"                   where ht.groupname = ? "
				               +"                     and   nvl(succount,0)>=?  and ht.flag='2' and  txncount>=0 "
				               +"	                  and  txnmaxcount>= nvl(txncount,0)+1  and nvl(txnmaxamt,0)>=txnamt+?   "
				               +"                     and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				               +"                     and  hf.status='1' and hf.fiinfo2 like ?"
				               +"                     and  nvl(ht.creditradio,0) <(select hml.creditradio from hrt_mcc_limit hml where bm.category= hml.mcc  and status='1' )"
				               +"                     and  nvl(ht.txnavg,0) < ?"
				               +"                     and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
				               +"                    ORDER BY txncount ,txnmaxcount desc )where rownum=1"
				               +"   union"
				               +"   select * from (select  3 rowflag, ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				               +"                          bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txncount,ht.txnmaxcount,nvl(failcount,0) failcount,ht.flag "
				               +"                    from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				               +"                   where ht.groupname = ?"
				               +"                     and  nvl(succount,0)>=? and ht.flag='2'   and  txncount>=0 "
				               +"                     and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				               +"	                  and  txnmaxcount>= nvl(txncount,0)+1  and nvl(txnmaxamt,0)>=txnamt+?   "
				               +"                     and  hf.status='1' and hf.fiinfo2 like ? "
				               +"                     and  nvl(ht.txnavg,0) <(select hml.txnavg from hrt_mcc_limit hml where bm.category= hml.mcc  and status='1' )"
				               +"                     and  nvl(ht.creditradio,0)<? "
				               +"                     and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')"
				               +"                    ORDER BY txncount ,txnmaxcount desc ) where rownum=1"
				               +"  union"
				               +"  select *  from (select 4 rowflag, ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,  "
				               +"                         bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txnmaxcount ,txncount,nvl(failcount,0) failcount,ht.flag "
				               +"                    from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht  "
				               +"                   where ht.groupname = ? and  txncount>=0  "
				               +"                     and  txnmaxcount>= nvl(txncount,0)+1  and nvl(txnmaxamt,0)>=txnamt+?  "
				               +"                     and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'   and ht.flag='2' and  bm.approvestatus='Y'    "
				               +"                     and  hf.status='1' and hf.fiinfo2 like  ? "
				               +"                     and  nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000')< nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')   "
				               +"                     and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')  "
				               +"                     order by ht.txncount,ht.txnmaxcount desc)   where rownum=1"
//				               + "     select  ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,  "
//				               + "             bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txnmaxcount ,txncount,nvl(failcount,0) failcount,ht.flag "
//				               + "       from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht  "
//				               + "      where ht.groupname =?  "
//				               + "        and  txnmaxcount>= nvl(txncount,0)+1  and nvl(txnmaxamt,0)>=txnamt+?   "
//				               + "        and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'   and ht.flag='2' and  bm.approvestatus='Y'    "
//				               + "        and  hf.status='1' and hf.fiinfo2 like ?  "
//				               + "        and  nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000')< nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')   "
//				               + "        and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')   "
				               + "     UNION "
				               + "     select  5 rowflag,ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress, "
				               + "             bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txnmaxcount  ,txncount,nvl(failcount,0) failcount,ht.flag"
				               + "       from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht  "
				               + "      where ht.groupname =? and  txncount>=0  "
				               + "        and  txnmaxcount>= nvl(txncount,0)+1  and nvl(txnmaxamt,0)>=txnamt+?   and txncount <=6"
				               + "        and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'   and ht.flag='1' and  bm.approvestatus='Y'   "
				               + "        and  hf.status='1' and hf.fiinfo2 like ?"
				               + "        and  nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000')< nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')  "
				               + "        and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')  "
				               + "     UNION"
				               + "     select 6 rowflag,ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress, "
				               + "             bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txnmaxcount  ,txncount,nvl(failcount,0) failcount  ,ht.flag"
				               + "       from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht  "
				               + "      where ht.groupname =? and  txncount>=0  "
				               + "        and  txnmaxcount>= nvl(txncount,0)+1  and nvl(txnmaxamt,0)>=txnamt+?  "
				               + "        and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'   and ht.flag='1' and  bm.approvestatus='Y'   "
				               + "        and  hf.status='1' and hf.fiinfo2 like ? "
				               + "        and  nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000')< nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')  "
				               + "        and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')  "
				               + "  )"
				               + " ORDER BY FLAG DESC,FAILCOUNT,TXNCOUNT,TXNMAXCOUNT DESC   )"
				               + " where   rownum=1  ORDER BY ROWFLAG ";
//		String queryMerDefSql="select  *  "
//					         + " from (select  ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
//					         + "               bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txnmaxcount  "
//					         + "         from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
//					         + "        where ht.groupname =? "
//					         + "          and  txnmaxcount> nvl(txncount,0)+1  and nvl(txnmaxamt,0)>txnamt+? "
//					         + "          and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  /*and ht.flag='2'*/  and  bm.approvestatus='Y'  "
//					         + "          and  hf.status='1' and hf.fiinfo2 like ? "
//					         + "          and  nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000')< nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
//					         + "          and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
//					         + " order by txncount,txnmaxcount desc) "
//					         + " where rownum=1";
		//走默认组  夜间交易
		String queryMerDefNightSql="select  *  "
		         + " from (select  ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
		         + "               bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit,ht.txnmaxcount  "
		         + "         from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
		         + "        where ht.groupname =? "
		         + "          and  txnmaxcount>= nvl(txncount,0)+1  and  txnmaxamt  >= nvl(txnamt, 0)+?  "
		         + "          and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  /*and ht.flag='2'*/  and  bm.approvestatus='Y'  "
		         + "          and  hf.status='1' and hf.fiinfo2 like ? "
		         + "          and  nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000')> nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
		         + "          and  ( (?>nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000')and  ?>nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') )"
		         + "               or(?<nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000')and  ?<nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') ))"
		         + " order by txncount,txnmaxcount desc) "
		         + " where rownum=1";
		/*
		 * part 1: 
		 * 
		 * 按照商户号获取商户上一次登录地址 
		 * 查询手刷数据库
		 */
		String province="";
		try{
			String queryLocateSql="select province from ( "
					+ "  select province from  hy_tab_mpos_user_log  "
					+ "   where user_name in (select login_name  from hy_tab_mpos_merchant  where mid =?) "
					+ "   order by oper_date desc ) "
					+ "  where rownum=1";
			Map<String, Object> locateMap=hdbDao.queryForMap(queryLocateSql, hrt_mid);
			//最近一次登录 地址
			province=String.valueOf(locateMap.get("province"));
			logger.info("[大额--手刷]商户号{}：上次登录省份为{}",hrt_mid,province);
		}catch(Exception e){
			logger.error("[大额--手刷]商户号{}：手刷数据库连接异常，设置省份默认为空{}",hrt_mid,e.getMessage());
		}
		/*
		 * part 2: 
		 *
		 * 根据最近一次登录地址 匹配对应的交易轮询组
		 * 
		 * 如果没有找到 就走默认对应的轮询组（配置文件、Redis）
		 * 
		 * 按照配置的配比笔数进行商户号上送  如 配比50笔 则 当txnmaxcount>50*(txncount+1) 则可以选择该商户号进行交易上送
		 * 多个商户号符合要求是，按照txncount升序、 txnmaxcount降序进行 取第一条记录
		 * 逻辑：优先走符合要求的当日大额交易进行的少、实体商户交易进行的多的商户号上送大额交易
		 *
		 * 复用了会员宝大额商户交易组 flag=2 用于区分 手刷大额商户使用的商户号
		 * 
		 * 二期：2018-12-29 修改
		 * 
		 * 条件一  :日累计笔均金额小于值
		 * 条件二  :日累计贷记卡比例小于值
		 * 首先：有限选择满足条件一和条件二的商户号
		 * 其次：选择满足条件二、笔均大于设置在mcc内的笔均金额  和  笔均设置的限定值的商户号
		 * 再次：选择满足条件一、贷记卡比例大于设置在mcc内的贷记卡比例  和  贷记卡比例设置的限定值的商户号 
		 * 最后：不满足条件一和条件二的商户号  按照一期方式取号
		 * 增加 按照笔均进行判断 该商户是否符合要求 
		 * 增加 按照借贷比进行判断 该商户是否符合要求 
		 * 
		 * 2019-02-15  修改
		 * 
		 * 北京地区的商户交易默认先走  WSSJHBJ01（零费率餐饮商户）
		 * 上送交易金额为300—5000
		 * 交易时间为11:00—14:00、18:00—23:00   
		 * 时间限制存储在redis内
		 * 
		 */
		
		if (province.contains("北京")&&bean.getAmount().compareTo(new BigDecimal(bj01BotmAmt))>=0&&bean.getAmount().compareTo(new BigDecimal(bj01TopAmt))<=0) {
			String[] bj01Time=bj01Times.split(";");
			String start="";
			String end="";
			boolean flag=false;
			for (int i = 0; i < bj01Time.length; i++) {
				start=bj01Time[i].split("-")[0];
				end=bj01Time[i].split("-")[1];
				if (orderTime.compareTo(start)>=0 && orderTime.compareTo(end)<=0 ) {
					flag=true;
					break;
				} 
			}
		    String queryMerSq="  select * from (select ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				+"					     bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit, ht.txnmaxcount "
				+"					from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				+"				   where ht.groupname = ? "
				+"		             and  txnmaxcount>= nvl(txncount,0)+1  and  txnmaxamt  >= nvl(txnamt, 0)+? "
				+"					 and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				+"					 and  hf.status='1' and hf.fiinfo2 like  ?    "
				+"                   and  ? between nvl(CASE  ? WHEN '' then '0000' else ?  end ,'0000') and nvl(CASE ? WHEN '' then '2359' else ?  end ,'2359')"
			    +" 	           order by txncount,txnmaxcount desc)   where rownum=1" ;	
			if (flag) {
				//"WSSJHBJ01"   修改成redis 取值
				logger.info("[大额--手刷] 商户属于北京地区  订单{}交易金额{} 在 {}~{}之间 ，时间{}在{}之间，走{}零费率组",bean.getOrderid(),amount,bj01BotmAmt,bj01TopAmt,orderTime,bj01Times,bj01Name);
				merInfoList=dao.queryForList(queryMerSq,bj01Name,amount,"%"+payway+"%",orderTime,start,start,end,end);
			}
		}
		/*
		 * 2019-03-18 修改
		 * 
		 * 收银台1000元以下交易走 SSJHSYT
		 *   
		 */
		if (hrt_mid.contains("HRTSYT")&&new BigDecimal(amount).compareTo(new BigDecimal(1000.00))<0) {
			
			String queryPoolMerSql=" select * from (select t1.hpid,t2.merchantcode,t2.storeid,t2.fiid ,T2.orgcode,T2.minfo2,"
					+ "T2.cdate,T2.category,T2.merchantaddress,T2.appid,T2.shortname,T2.mch_id,T2.channel_id,T2.iscredit,T1.txnmaxcount  from hrt_termaccpool T1,"
					+ " bank_merregister T2,hrt_fi f WHERE t1.btaid=t2.hrid"
					+ " and T2.fiid=f.fiid and f.fiinfo2 like ? and T1.status=1 "
					+ " and t1.txnmaxamt>=nvl(t1.txnamt,0)+? and t1.txnmaxcount>=nvl(t1.txncount,0)+1 and t1.groupname=? "
					+ " and ? between nvl(CASE t1.starttime WHEN 'null' then '0000' else t1.starttime  end ,'0000') and nvl(CASE t1.endtime WHEN 'null' then '2359' else t1.endtime  end ,'2359')  "
					+ " and T2.status=1 order by T1.txnamt asc,txnmaxcount asc) where rownum=1 ";
			merInfoList = dao.queryForList(queryPoolMerSql,"%"+payway+"%",amount,daeSYTDefaultGroup, orderTime); 
			if (merInfoList.size()>0) {
				logger.info("[大额--手刷] 商户号{}，金额{}，走组{}",hrt_mid,amount,daeSYTDefaultGroup);
			}
		}
		
		String queryMerSql= "select * from ("
				+"    select * from (select 0 flag,  ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				+"                        bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit, ht.txnmaxcount "
				+"                     from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				+"                    where ht.groupname =(select merchantid from bank_merregister  where instr(merchantname,?)>0 and fiid=99 ) "
				+"                      and  txncount<0   and ?>1000 "
				+"                      and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				+"                      and  hf.status='1' and hf.fiinfo2 like ? "
				+"                      and  ? between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
				+"                    ORDER BY txncount ,txnmaxcount desc) where rownum=1"
				+"    union "
				+"    select * from (select 1 flag,  ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				+"                        bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit, ht.txnmaxcount "
				+"                     from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				+"                    where ht.groupname =(select merchantid from bank_merregister  where instr(merchantname,?)>0 and fiid=99 ) "
				+"                      and  txnmaxcount>? and  txncount>=0 "
				+"                      and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				+"                      and  hf.status='1' and hf.fiinfo2 like ? "
				+"                      and  nvl(ht.creditradio,0) <(select hml.creditradio from hrt_mcc_limit hml where bm.category= hml.mcc  and status='1' )"
				+"                      and  nvl(ht.txnavg,0) < (select hml.txnavg from hrt_mcc_limit hml where bm.category= hml.mcc  and status='1' ) "
				+"                      and  ? between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
				+"                    ORDER BY txncount ,txnmaxcount desc) where rownum=1"
				+"    union "
				+"    select * from (select  2 flag, ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				+"						   bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit, ht.txnmaxcount "
				+"					   from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				+"					  where ht.groupname =(select merchantid from bank_merregister  where instr(merchantname,?)>0 and fiid=99 ) "
				+"					    and  txnmaxcount>?  and  txncount>=0 "
				+"	                    and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				+"                      and  hf.status='1' and hf.fiinfo2 like ? "
				+"                      and  nvl(ht.creditradio,0) <(select hml.creditradio from hrt_mcc_limit hml where bm.category= hml.mcc  and status='1' )"
				+"                      and  nvl(ht.txnavg,0) < ? "
				+"                      and  ?  between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359') "
				+"                    ORDER BY txncount ,txnmaxcount desc )where rownum=1"
				+"    union"
				+"    select * from (select  3 flag, ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				+"						   bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit, ht.txnmaxcount "
				+"					 from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				+"	                where ht.groupname =(select merchantid from bank_merregister  where instr(merchantname,?)>0 and fiid=99 ) "
				+"	                  and  txnmaxcount>? and  txncount>=0 "
				+"	                  and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  and  bm.approvestatus='Y'  "
				+"	                  and  hf.status='1' and hf.fiinfo2 like ? "
				+"	                  and  nvl(ht.txnavg,0) <(select hml.txnavg from hrt_mcc_limit hml where bm.category= hml.mcc  and status='1' )"
				+"	                  and  nvl(ht.creditradio,0)< ? "
				+"	                  and  ?   between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')"
				+"	                ORDER BY txncount ,txnmaxcount desc ) where rownum=1"
				+"   union"
				+"   select * from (select 4 flag, ht.hpid,bm.merchantcode,bm.storeid,bm.fiid ,bm.orgcode,bm.minfo2,bm.cdate,bm.category,bm.merchantaddress,"
				+"					     bm.appid,bm.shortname,bm.mch_id,bm.channel_id,bm.iscredit, ht.txnmaxcount "
				+"					from bank_merregister bm ,hrt_fi hf , hrt_termaccpool ht "
				+"				   where ht.groupname =(select merchantid from bank_merregister  where instr(merchantname,? )>0 and fiid=99 )  "
//				+"		             and  txnmaxcount>=? *(nvl(txncount,0)+1)"    更改成的新的配比   XN-S(X-1)
				+"                   and  txncount>=0 "
				+"		             and  txnmaxcount>=(?-? )*(nvl(txncount,0)+1)+?"
				+"					 and  ht.btaid=bm.hrid  and  bm.fiid= hf.fiid  and  ht.status='1'  /*and ht.flag='2'*/ and  bm.approvestatus='Y'  "
				+"					 and  hf.status='1' and hf.fiinfo2 like  ? "
				+"		             and  ? between nvl(CASE ht.starttime WHEN 'null' then '0000' else ht.starttime  end ,'0000') and nvl(CASE ht.endtime WHEN 'null' then '2359' else ht.endtime  end ,'2359')"
				+"		           order by txncount,txnmaxcount desc)   where rownum=1"
				+"    )where rownum=1 order by flag ";
		//单笔金额小于等于100元的交易 直接走QT轮询组
		if (merInfoList.size()==0&&bean.getAmount().compareTo(new BigDecimal(qtSmallAmt))<0) {
			logger.info("[大额--手刷] 订单{}交易金额{}小于金额{}，默认走QT组-日间商户号",bean.getOrderid(),amount,qtSmallAmt);
			merInfoList=dao.queryForList(queryMerDefSql,daeMposDefaultGroup,amount,amount,"%"+payway+"%",orderTime
					,daeMposDefaultGroup,daeMposAreaStep,amount,"%"+payway+"%",orderTime
					,daeMposDefaultGroup,daeMposAreaStep,amount,"%"+payway+"%",txnavg,orderTime
					,daeMposDefaultGroup,daeMposAreaStep,amount,"%"+payway+"%",creditRadio,orderTime
					,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime
					,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime
					,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime);
		}
		if (merInfoList.size()==0) {
			merInfoList=dao.queryForList(queryMerSql, province,amount,"%"+payway+"%",orderTime,
					                                  province,daeMposAreaStep,"%"+payway+"%",orderTime,
					                                  province,daeMposAreaStep,"%"+payway+"%",txnavg,orderTime,
					                                  province,daeMposAreaStep,"%"+payway+"%",creditRadio,orderTime,
					                                  province,daeMposStep,daeMposDifferStep,daeMposDifferStep,"%"+payway+"%",orderTime);
		}
		/*
		 * 启用笔均判断 禁用下面描述
		 */
//		merInfoList=dao.queryForList(queryMerSql, province,daeMposStep,"%"+payway+"%",orderTime);
		if (merInfoList.size()==0) {
			/*
			 * 2019-02-18 修改
			 * 地区组没有符合条件的商户号时 
			 * 新增层级 SSJHBY组 
			 * 交易时间限制 
			 * 支付宝：14：00—16:00、20:00-21:00 
			 * 微信：18:00-19:00
			 * 交易配比 手刷：立码富=1:35
			 */
			String start="";
			String end="";
			String[] hybTime=null;
			if (payway.contains("WX")) {
				hybTime=byTimesWx.split(";");
			}else if (payway.contains("ZFB")) {
				hybTime=byTimesZfb.split(";");
			}
		
			boolean flag=false;
			for (int i = 0; i < hybTime.length; i++) {
				start=hybTime[i].split("-")[0];
				end=hybTime[i].split("-")[1];
				if (orderTime.compareTo(start)>=0 && orderTime.compareTo(end)<=0 ) {
					flag=true;
					break;
				} 
			}
			if (flag) {
				logger.info("[大额--手刷]查询备用组轮询组。");
				merInfoList=dao.queryForList(queryMerHybSql,daeMposByGroup,daeMposStep,daeMposDifferStep,daeMposDifferStep,"%"+payway+"%",orderTime,start,start,end,end);
			}
		}
		if (merInfoList.size()==0) {
			logger.info("[大额--手刷] 查询默认轮询组--日间商户号，订单{}",bean.getOrderid());
//			merInfoList=dao.queryForList(queryMerDefSql,daeMposDefaultGroup,amount,"%"+payway+"%",creditRadio,creditRadio,txnavg,txnavg,amount,orderTime,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime);
			merInfoList=dao.queryForList(queryMerDefSql,daeMposDefaultGroup,amount,amount,"%"+payway+"%",orderTime
					,daeMposDefaultGroup,daeMposAreaStep,amount,"%"+payway+"%",orderTime
					,daeMposDefaultGroup,daeMposAreaStep,amount,"%"+payway+"%",txnavg,orderTime
					,daeMposDefaultGroup,daeMposAreaStep,amount,"%"+payway+"%",creditRadio,orderTime
					,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime
					,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime
					,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime);
		}
		if (merInfoList.size()==0) {
			logger.info("[大额--手刷]查询默认轮询组--夜间商户号");
			merInfoList=dao.queryForList(queryMerDefNightSql,daeMposDefaultGroup,amount,"%"+payway+"%",orderTime,orderTime,orderTime,orderTime);
		}
		if (merInfoList.size()<1)  throw new HrtBusinessException(8000,"指定通道未开通");
		
		/*
		 * 
		 * part 3：
		 * 
		 * 更新 hrt_termaccpool 表内 交易笔数 交易金额 （txnamt,txncount）
		 * 
		 */
		Integer hpid=Integer.parseInt(String.valueOf(merInfoList.get(0).get("hpid")));
//		Integer hrid =Integer.parseInt(String.valueOf(merInfoList.get(0).get("hrid")));
		 String merchantcode=String.valueOf(merInfoList.get(0).get("merchantcode"));
		 String updateTermSql="update hrt_termaccpool set txnamt=nvl(txnamt,0)+( case when txncount>=0 then to_number(?) else 0 end) ,txncount=txncount+1  where 1=1  and hpid=? ";
		 int count=dao.update(updateTermSql, amount,hpid);//,hrid
		 if (count==0) {
			logger.info("[大额--手刷]更新商户号{}:{}状态,更新数据条数{}。",hpid,merchantcode,count);
		 }else if (count>1) {
			logger.info("[大额--手刷]更新商户号{}:{}状态,更新数据条数{},大于一条 ，需要核实原因。",hpid,merchantcode,count);
			RedisUtil.addFailCountByRedis(1);
		 }else {
			logger.info("[大额--手刷]更新商户号{}:{}状态,更新数据条数{}。",hpid,merchantcode,count);
		 }
		return merInfoList.get(0);
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
	public String getWxpayPayInfo(String orderid, String openid,String isCredit) {
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
