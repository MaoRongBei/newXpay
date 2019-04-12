package com.hrtpayment.xpay.common.service.impl;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.channel.bean.HrtDeviceTypeInfoBean;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.utils.CommonUtils;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

@Service
public class MerchantService {
	@Autowired
	JdbcDao dao;
	private Logger logger = LogManager.getLogger();
	
	public String queryMerName(String mid) {
		List<Map<String, Object>> list = dao.queryForList("select hm.tname,hxh.status from Hrt_Merchacc hm ,hrt_xpay_hot hxh  where hm.hrt_mid=hxh.mid(+) and  hm.hrt_MID =?", mid);
		if (list.size()>0) {
			if (!"1".equals(String.valueOf(list.get(0).get("STATUS")))) {
				return (String) list.get(0).get("TNAME");
			}else{
				return "blackList";
			}
		}
		return null;
	}
	
	public String queryMerNameForHrt(String mid,String tradeType) {
		List<Map<String, Object>> list = dao.queryForList("select hm.tname,hxh.status,nvl(hxh.tradetype,'ALL') tradetype from Hrt_Merchacc hm ,hrt_xpay_hot hxh  where hm.hrt_mid=hxh.mid(+) and  hm.hrt_MID =? ", mid);
		if (list.size()>0) {
			String hotMid=String.valueOf(list.get(0).get("tradetype"));
			if((tradeType.equals(hotMid)||"ALL".equals(hotMid))&&"1".equals(String.valueOf(list.get(0).get("STATUS")))){
				return "blackList";
			}else {
			    return (String) list.get(0).get("TNAME");
			}
		}
		return null;
	}
	
	public String queryMerNameByTid(String tid) {
		List<Map<String, Object>> list = dao.queryForList("select  mid  from Hrt_Aggpaytermsub where qrtid=?", tid);
		if (list.size()>0) {
			return (String) list.get(0).get("MID");
		}
		return null;
	}
	/**
	 *  2019-01-05 新增 
	 *  
	 *  根据tid（SN） 获取对应的机具秘钥、商户编号、tid
	 *  机具没有秘钥时 先进行秘钥的生成
	 *  
	 * @param tid
	 * @return
	 */
	public HrtPayXmlBean queryMerInfoByTid(HrtPayXmlBean bean){
		String tid=bean.getTid();
		String macKey=CommonUtils.getRandomString(32);
		String addTidInfo = " merge into  hrt_syt_tidinfo st"
				+" using (select htid,hsid  from (select  ht.htaid htid, hst.htaid hsid from hrt_termacc ht ,hrt_syt_tidinfo hst where ht.htaid=hst.htaid(+) and ht.status='1' and ht.sn=? order by ht.cdate desc )  where rownum=1 ) t "
				+" on (st.htaid=t.htid)"
				+" when MATCHED then  "
				+"	   update set mackey=?,lmdate=sysdate"
				+" when not matched then "
				+"     insert  (st.stid,st.htaid,st.mackey,st.cdate,st.lmdate,st.unno) values(get_sequence('s_hrt_syt_tidinfo'),t.htid,?,sysdate,sysdate,?)";
		
		 int count=dao.update(addTidInfo, tid,macKey,macKey,bean.getUnno());
		 if (count==1) {
			logger.info("[获取机具秘钥]SN：{}获取机具秘钥成功",tid);
		 }else if (count==0) {
		 	logger.error("[获取机具秘钥]SN：{}获取机具秘钥 异常，count=0",tid);
		 	throw new HrtBusinessException(8000,"机具秘钥获取异常，请重新获取");
		 }else if (count>1) {
			logger.error("[获取机具秘钥]SN：{} 统一机具存在多条记录 更新机具秘钥异常，count={}",tid,count);
			 throw new HrtBusinessException(8000,"机具秘钥获取异常，请重新获取");
		 }
		 String getTidInfoSql="select  ht.sn tid,ht.mid mid, hm.tname tname from hrt_termacc ht ,hrt_merchacc hm where ht.mid=hm.hrt_mid and ht.status='1' and ht.sn=?";
		 List<Map<String , Object>> tidInfoList=dao.queryForList(getTidInfoSql, tid);
		 Map<String , Object> tidInfo =tidInfoList.get(0);
		 try {
			tidInfo.put("macKey",new String(Base64.encodeBase64(macKey.getBytes()),"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error("[获取机具秘钥]SN：{}  mackey进行base64编码异常",tid);
			throw new HrtBusinessException(8000,"机具秘钥获取异常，请重新获取");
		}
		 bean.setTid(String.valueOf(tidInfo.get("tid")));
		 bean.setMerName(String.valueOf(tidInfo.get("tname")));
		 bean.setMid(String.valueOf(tidInfo.get("mid")));
		 bean.setMackey(String.valueOf(tidInfo.get("macKey")));
		 return bean;
	}

     /**
      * 2019-01-09 新增
      * 根据设备类型查询当前版本号
      * @param bean
      * @return
     */
    public HrtDeviceTypeInfoBean queryDeviceTypeInfo(HrtDeviceTypeInfoBean bean){
       String queryDeviceInfo="select t.title,t.upload_path,t.download_path from sys_param t  where  title=? ";
       List<Map<String , Object>> list=dao.queryForList(queryDeviceInfo, bean.getDeviceType());
       bean.setIsForceUpdate(String.valueOf(list.get(0).get("download_path")));
       bean.setLastVersion(String.valueOf(list.get(0).get("upload_path")));
       logger.info("[获取设备最新版本号] {} ",list.get(0));
       return bean;
    	 
     }
   
    /**
     * 2019-01-16 新增
     * 
     * 设置交易为风控交易
     * 
     * @param oldOrderid
     * @param orderid
     */
    public void updateOrderRisk(String oldOrderid,String orderid)  {
    	try {
    		String updateOrderRiskSql="update pg_wechat_txn set riskflag=1 where mer_orderid in (?,?) ";
    		dao.update(updateOrderRiskSql,oldOrderid,orderid);
		} catch (Exception e) {
			 logger.error("[风控交易]更新风控交易，错误原因{}",e.getMessage());
		}
		
	}
    
    /**
     * 2019-04-9 新增
     * 
     * 获取对应状态
     * 
     * @param mid
     * @param tid
     * @return status
     */
    public String queryStatus(String mid,String sn) {
    	String sql = "select STATUS from hrt_termacc where sn=? and mid=?";
    	List<Map<String, Object>> list = dao.queryForList(sql,sn,mid);
    	if(list.size() > 0) {
        	return String.valueOf(list.get(0).get("STATUS"));
    	}
    	return null;
    }
}
