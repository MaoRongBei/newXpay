package com.hrtpayment.xpay.common.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.FileItem;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.cupsAT.sdk.WXPayConstants.SignType;
import com.hrtpayment.xpay.cupsAT.sdk.WXPayUtil;
import com.hrtpayment.xpay.cupsAT.service.CupsATMerchantService;
import com.hrtpayment.xpay.cupsAT.service.CupsATService;
import com.hrtpayment.xpay.netCups.service.NetCupsMerchantService;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Service
public class BankMerchantService {
	
	@Autowired
	JdbcDao dao;
	@Autowired
	CupsATMerchantService cupsMerchantService;
	@Autowired
	NetCupsMerchantService netCupsMerchantService;
	@Autowired
	AliJspayService alipayService;
	@Autowired
	CupsATService cupsService;
	@Value("${cupsWx.mchId}")
	String mchid;
	@Value("${cupsWx.channelId}")
	String channelid;
	@Value("${cupsWx.md5Key}")
	String cupsWxMd5Key;
	@Autowired
	NettyClientService client;
	
	private final Logger logger = LogManager.getLogger();
	
	/**
	 * 商户入驻
	 * @param hrid
	 * @return
	 */
	public String registerBankMer(String hrid){
		
		String sql = "select * from bank_merregister where  hrid=?";
		List<Map<String, Object>> list = dao.queryForList(sql, hrid);
		if (list.size()<1) return "入驻商户hrid不存在";
		
		Map<String, Object> map = list.get(0);
		String approveStatus = (String) map.get("APPROVESTATUS");
		if ("Y".equals(approveStatus)) return "商户入驻状态为已成功";
		
		BigDecimal fiid = (BigDecimal) map.get("FIID");
		
		try {
			 if (fiid.intValue()==54) {
				return  cupsMerchantService.addAliMerchants(map);
			}else if (fiid.intValue()==53) {
				return  cupsMerchantService.addWxMerchants(map);
			}else if (fiid.intValue()==60) {
				return  netCupsMerchantService.addWxMerchants(map);
			}else if (fiid.intValue()==61) {
				return  netCupsMerchantService.addAliMerchants(map);
			}
		} catch (Exception e) {
			logger.error("入驻商户异常：",e);
		}
		return hrid;
	}
	
	/**
	 * 子商户配置
	 * @param hrid
	 * @param zdlx
	 * @return
	 */
	public String registerSubBankMer(String hrid,String zdlx) {
		try {
			String sql = "select * from bank_merregister where  hrid=? ";
			List<Map<String, Object>> list = dao.queryForList(sql, hrid);
			if (list.size()<1) {
				logger.info("------------------->商户hrid={}的商户不符合子商户配置要求",hrid);
				return "配置商户hrid不符合子商户配置要求";
			}
			Map<String, Object> map = list.get(0);
			String approveStatus =String.valueOf( map.get("APPROVESTATUS"));
			String fiid =String.valueOf( map.get("FIID"));
			String merchantCode=(String)map.get("MERCHANTCODE");
			
			if("Z".equals(approveStatus)){
				if ("1".equals(zdlx)) {
					String resultMsg="";
					if("53".equals(fiid)){
						cupsMerchantService.addSubMerchant(merchantCode, "1",map); 
					}if("60".equals(fiid)){
						netCupsMerchantService.addSubMerchant(merchantCode, "1",map); 
					}else{
						return merchantCode+":不符合更新微信配置要求";
					}
					logger.info("------------------->{}：配置授权目录 {}",merchantCode,resultMsg);
					return merchantCode+":配置授权目录 "+ resultMsg;
				}else{
					logger.info("------------------->{}：未进行授权目录操作。",merchantCode);
					return merchantCode+":未进行授权目录操作。";
				}
			}else if("M".equals(approveStatus)){
				if ("2".equals(zdlx)) {
					String resultMsg="";
					if("53".equals(fiid)){
						cupsMerchantService.addSubMerchant(merchantCode, "2",map); 
					}if("60".equals(fiid)){
						netCupsMerchantService.addSubMerchant(merchantCode, "2",map); 
					}else{
						return merchantCode+":不符合更新微信配置要求";
					}
					logger.info("------------------->{}：配置APPID {}",merchantCode,resultMsg);
					return merchantCode+":配置APPID "+resultMsg; 
				}else{
					logger.info("------------------->{}：未进行配置APPID操作。",merchantCode);
					return merchantCode+":未进行配置APPID操作。";
				}
			}else if("Y".equals(approveStatus)){
				if ("3".equals(zdlx)) {
					String resultMsg="";
					if("53".equals(fiid)){
						cupsMerchantService.addSubMerchant(merchantCode, "3",map); 
					}if("60".equals(fiid)){
						netCupsMerchantService.addSubMerchant(merchantCode, "3",map); 
					}else{
						return merchantCode+":不符合更新微信配置要求";
					}
					logger.info("------------------->{}：关注公众号操作 {}",merchantCode,resultMsg);
					return merchantCode+":关注公众号操作  "+resultMsg;  
				}else{
					logger.info("------------------->{}：关注公众号操作有误。",merchantCode);
					return merchantCode+":关注公众号操作有误。";
				}
				
			}else{
				return merchantCode+":不符合更新微信配置要求";
			}
		} catch (Exception e) {
			logger.error("微信更改商户配置异常：",e);
		}
	     return "";	
	}
	
	
	public String applyCallBackMsg(Map<String, String> requestMap) {
		 JSONObject rtnJson=new  JSONObject();
		 try {
			 String  msg_method=requestMap.get("msg_method");
			 Map<String, String>  bizJson=JSONObject.parseObject(requestMap.get("biz_content"),Map.class);
			 String order_id=bizJson.get("order_id");
			 String status="2";
			 String reason="";
			 if ("ant.merchant.expand.indirect.activity.rejected".equals(msg_method)) {
				 status="5"; //商户申请失败  
				 reason=bizJson.get("fail_reason");
				 logger.info("[特殊商户报备] 通知响应  order_id={}，商户申请失败。原因：{}。", order_id,reason);
			 }else if ("ant.merchant.expand.indirect.activity.passed".equals(msg_method)) {
				 status="3";//商户申请成功
				 reason="申请成功";
				 logger.info("[特殊商户报备] 通知响应  order_id={}，商户申请成功。", order_id);
			 }else{
				 logger.info("[特殊商户报备] 通知响应  msg_method：{}，未知方法。", msg_method);
			 }
			 System.out.println(status+"  "+reason+"  "+order_id); //bs_activity_respmsg||'1'||
			 String updateApplyStatusSql="update bank_specialmerimages set bs_status=?,bs_activity_respmsg=?,bs_lmdate=sysdate  where bs_activity_orderid=?  ";
			 dao.update(updateApplyStatusSql, status,reason,order_id);
		} catch (Exception e) {
			 logger.info("[特殊商户报备]处理通知响应  {}，处理异常：{}。",requestMap,e.getMessage());
		}
		 return rtnJson.toJSONString();
	}
	
	public String querySpercialStatus(Map<String, Object> merchantInfo){
		 JSONObject rtnJson=new  JSONObject();
		 String querySql="select * from bank_specialmerimages where bs_mid=?,bs_payway=? ";
		 List<Map<String, Object>> merchantInfoList=dao.queryForList(querySql, merchantInfo.get("mid"), merchantInfo.get("payway"));
		 Map<String, Object> merchantInfoMap=merchantInfoList.get(0);
		 String status=String.valueOf(merchantInfoMap.get("bs_status"));
		 String msg=""; 
		 String infoName="";
		 if ("0".equals(status)) {
			 msg="初始状态，照片未上传";
			 status="R";
			 infoName="add.merchant.uploadimage";
		 }else  if ("1".equals(status)){ 
			 msg="照片上传成功，请进行报备申请";
			 status="S";
			 infoName="add.merchant.uploadimage";
		 }else  if ("2".equals(status)){
			 msg="报备申请中，请稍后查询";
			 infoName="add.merchant.apply";
			 status="R";
		 }else  if ("3".equals(status)){
			 msg="报备申请成功，请进行报备确认";
			 status="S";
			 infoName="add.merchant.apply";
		 }else  if ("4".equals(status)){
			 msg="确认成功，蓝海商户报备完成";
			 status="S";
			 infoName="add.merchant.confirm";
		 }else  if ("5".equals(status)){
			 msg="报备申请失败，原因"+merchantInfoMap.get("bs_activity_respmsg");
			 infoName="add.merchant.apply";
			 status="E";
		 }else  if ("6".equals(status)){
			 msg="确认失败，原因"+merchantInfoMap.get("bs_activity_respmsg");
			 infoName="add.merchant.confirm";
			 status="E";
		 }
		 rtnJson.put("mid",  merchantInfo.get("mid"));
		 rtnJson.put("status", status);
		 rtnJson.put("msg", msg);
		 rtnJson.put("method", infoName);
		 return rtnJson.toJSONString();
	}
	
	
	public String confirmSpecialMer(Map<String, Object> confirmInfo) {
		 JSONObject rtnJson=new  JSONObject(); 
		 try {
			 String payway=confirmInfo.get("payway")+"";
			 String mid=confirmInfo.get("mid")+"";
			 if ("".equals(payway)) {
				 rtnJson.put("status", "E");
				 rtnJson.put("errcode","8000");
				 rtnJson.put("errmsg","商户号为空，请核实。");
				 rtnJson.put("mid", "");
			 }
			 if (!"".equals(payway)) {
					String queryImgID="select bs_activity_orderid from  bank_specialmerimages where bs_mid =? and bs_payway=? and bs_status in('3','6') ";
					List<Map<String, Object>> specialMerchantList=dao.queryForList(queryImgID,mid,payway);
		            if (specialMerchantList.size()==0) {
		            	 rtnJson.put("status", "E");
						 rtnJson.put("errcode","8000");
						 rtnJson.put("errmsg","当前状态无法执行确认操作");
						 rtnJson.put("mid", confirmInfo.get("mid"));
						 return  rtnJson.toJSONString();
					}
		            confirmInfo.put("bs_activity_orderid", specialMerchantList.get(0).get("bs_activity_orderid"));
			 }
			 Map<String, Object> confirmRtnStr= alipayService.activityConfirm(confirmInfo);
			 String status=""; 
			 String reason=confirmRtnStr.get("msg")+"";
			 if ("S".equals(confirmRtnStr.get("status"))) {
				 status="4";//确认成功
			 }else {
				 status="6";//确认失败
			 }
			 String updateApplyStatusSql="update bank_specialmerimages set bs_status=?,bs_activity_respmsg=?,bs_lmdate=sysdate  where bs_mid=?  and bs_payway=? and bs_status in ('3','6') ";
			 int count=dao.update(updateApplyStatusSql, status,reason,confirmInfo.get("mid"),confirmInfo.get("payway"));
			 if (count==0) {
            	 rtnJson.put("status", "E");
				 rtnJson.put("errcode","8000");
				 rtnJson.put("errmsg","记录更新失败");
				 rtnJson.put("mid", confirmInfo.get("mid"));
			 }else{ 
				 rtnJson.put("status",confirmRtnStr.get("status"));
				 rtnJson.put("msg",confirmRtnStr.get("msg"));
				 rtnJson.put("mid", confirmInfo.get("mid"));
			 } 
			 return rtnJson.toJSONString();
		} catch (BusinessException e) {
			rtnJson.put("status", "E");
			rtnJson.put("errcode","8000");
			rtnJson.put("errmsg",e.getMessage());
			rtnJson.put("mid", confirmInfo.get("mid"));
		}
		 return rtnJson.toJSONString();
	}
	
	public String  applySpecialMer(Map<String, Object> applyInfo) {
		 JSONObject rtnJson=new  JSONObject();
		 try{
			if ("".equals(applyInfo.get("payway")+"")) {
				 rtnJson.put("status", "E");
				 rtnJson.put("errcode","8000");
				 rtnJson.put("errmsg","商户号为空，请核实。");
				 rtnJson.put("mid", "");
				 return rtnJson.toJSONString();
			}
			if (!"".equals(applyInfo.get("payway")+"")) {
 
				String queryM3RelatSql=" select hrid ,merchantcode,merchantname,shortname  from bank_merregister T where hrid in ("
						+ " select hrid from hrt_merbanksub where hrt_mid=? and status='1' ) "
						+ " and  FIID IN ( ";
				String payway=applyInfo.get("payway")+"";
				String fiid="";
				if ("ZFB".equals(payway)) {
					logger.info("[定时处理]特殊商户报备 蓝海商户 {}",applyInfo.get("mid"));
					fiid="54,61";
				}else if ("WX".equals(payway)){
					logger.info("[定时处理]特殊商户报备 绿洲商户 {}",applyInfo.get("mid"));
					fiid="53,60";
				}else{
					logger.info("[定时处理]特殊商户报备 商户{}，payway错误{}", applyInfo.get("mid"), payway); 
				}
				queryM3RelatSql =queryM3RelatSql+fiid+" ) and approvestatus='Y' and indirect_optstatus='1' ";//'987990010000003'
				List<Map<String, Object>> m3RelatList=dao.queryForList(queryM3RelatSql, applyInfo.get("mid"));
				if (m3RelatList.size()==0) {
					rtnJson.put("status", "E");
					rtnJson.put("errcode","8000");
					rtnJson.put("errmsg","挂载商户号对应的银行商户号等级或商户入驻状态不正确，请联系运营进行核实");
					rtnJson.put("mid", applyInfo.get("mid"));
					logger.info("[定时处理]特殊商户报备 商户{} 挂载商户号对应的商户等级或商户入驻状态不正确。", applyInfo.get("mid"));
					return rtnJson.toJSONString();
				}else if (m3RelatList.size()>1) {
					rtnJson.put("status", "E");
					rtnJson.put("errcode","8000");
					rtnJson.put("errmsg","商户号挂载多个银行商户号，请联系运营进行核实");
					rtnJson.put("mid", applyInfo.get("mid"));
					logger.info("[定时处理]特殊商户报备 商户{} 挂载多条 {}商户，请核实。", applyInfo.get("mid"),payway);
					return rtnJson.toJSONString();
				} 
				
				String queryImgID="select bs_activity_type,bs_checkstand_pic,bs_shop_entrance_pic,bs_business_license_pic, "
						+ " bs_indoor_pic,bs_settled_pic from  bank_specialmerimages where bs_mid =? and bs_payway=? and bs_status in ('1','5')";
				List<Map<String, Object>> specialMerchantList=dao.queryForList(queryImgID, applyInfo.get("mid"),payway);
	            if (specialMerchantList.size()==0) {
	            	 rtnJson.put("status", "E");
					 rtnJson.put("errcode","8000");
					 rtnJson.put("errmsg","当前状态无法进行申请");
					 rtnJson.put("mid", applyInfo.get("mid"));
					 return rtnJson.toJSONString();
				}
				applyInfo.put("bs_activity_type", specialMerchantList.get(0).get("bs_activity_type")+"");//活动类型
				applyInfo.put("bs_checkstand_pic", specialMerchantList.get(0).get("bs_checkstand_pic")+"");//收银台照片
				applyInfo.put("bs_shop_entrance_pic", specialMerchantList.get(0).get("bs_shop_entrance_pic")+"");//门头照
				applyInfo.put("bs_business_license_pic", specialMerchantList.get(0).get("bs_business_license_pic")+"");//营业执照
				applyInfo.put("bs_indoor_pic", specialMerchantList.get(0).get("bs_indoor_pic")+"");//店内环境照
				applyInfo.put("bs_settled_pic", specialMerchantList.get(0).get("bs_settled_pic")+""); //主流餐饮平台入驻证明（任选一个即可）大众点评、美团、饿了么、口碑、百度外卖餐饮平台商户展示页面。
				applyInfo.put("merchantcode", m3RelatList.get(0).get("merchantcode")+"");
				applyInfo.put("merchantname", m3RelatList.get(0).get("merchantname")+"");
				applyInfo.put("shortname", m3RelatList.get(0).get("shortname")+"");
				applyInfo.put("hrid", m3RelatList.get(0).get("hrid")+"");
			 } 
			 Map<String, Object> applyRtnStr= alipayService.activityCreate(applyInfo);
			
			 String updateApplySql="update bank_specialmerimages  set bs_status=?,bs_activity_respmsg=? ,bs_lmdate=sysdate,bs_activity_orderid=?,bs_hrid=?  where bs_mid =? ";
			 String status=""; 
			 if ("S".equals(applyRtnStr.get("status"))) {
				 status="2";
			 }else {
				 status="1";
			 }
			 int count=dao.update(updateApplySql,status,applyRtnStr.get("msg")+"",applyRtnStr.get("order_id"),applyInfo.get("hrid"),applyInfo.get("mid"));
             if (count==0) {
            	 rtnJson.put("status", "E");
				 rtnJson.put("errcode","8000");
				 rtnJson.put("errmsg","记录更新失败");
				 rtnJson.put("mid", applyInfo.get("mid"));
			 }else{ 
				 rtnJson.put("status",applyRtnStr.get("status"));
				 rtnJson.put("msg",applyRtnStr.get("msg"));
				 rtnJson.put("mid", applyInfo.get("mid"));
			 } 
			 return rtnJson.toJSONString();
		 } catch (BusinessException e) {
				rtnJson.put("status", "E");
				rtnJson.put("errcode","8000");
				rtnJson.put("errmsg",e.getMessage());
				rtnJson.put("mid", applyInfo.get("mid"));
				return rtnJson.toJSONString();
		 }
		 
	}
	
	/**
	 * 
	 * 特殊商户 报备 之  图片上送 
	 * 1、每个商户（hrt_mid）只允许有一条图片记录 
	 * 2、支持上送1-5条图片信息
	 * 
	 * 注意：
	 * 1、图片五张全部上送完成后 bs_status 更新为1 用于特殊商户申请 的自动扫描
	 * 2、某张图片上送失败时，失败信息存储于bs_activity_respmsg  格式   |照片类别：失败原因 |照片类别：失败原因
	 * 
	 * 待完善 同时触发两遍 (redis 存储 判断是否已经存在处理的数据)
	 * 
	 * @param imageInfo
	 * @return
	 */
	public String  uploadImage(Map<String, Object> imageInfo) {
	    JSONObject rtnJson=new  JSONObject();
		try {
			//获取请求的mid
			String mid= imageInfo.get("mid")+"";
			if ("".equals(mid)) {
				rtnJson.put("status", "E");
				rtnJson.put("errcode","8000");
				rtnJson.put("errmsg","商户号为空，请确认。");
				rtnJson.put("mid", mid);
				return rtnJson.toJSONString();
			}
			//获取请求的图片信息
			if (null==imageInfo.get("images")||"".equals(imageInfo.get("images"))) {
				rtnJson.put("status", "E");
				rtnJson.put("errcode","8000");
				rtnJson.put("errmsg","images为空");
				rtnJson.put("mid", mid);
				return rtnJson.toJSONString();
			}
			JSONObject fileItems= JSONObject.parseObject(imageInfo.get("images")+"");
			Set<Entry<String, Object>> fileEntrys=fileItems.entrySet();
			
			//获取请求的iscover 0 是新增 ;1 是修改
			String iscover= imageInfo.get("iscover")+"";
			//获取请求的payway ZFB 是支付宝 ;WX 是微信
			String payway= imageInfo.get("payway")+"";
			if ("".equals(payway)) {
				rtnJson.put("status", "E");
				rtnJson.put("errcode","8000");
				rtnJson.put("errmsg","支付方式为空，请确认。");
				rtnJson.put("mid", mid);
				return rtnJson.toJSONString();
			}
			String activityType="";
			 logger.info("payway");
			//根据payway 设置 活动类型
			if ("ZFB".equals(payway)) {
				activityType="BLUE_SEA";
			}else if ("WX".equals(payway)) {
				activityType="GREEN_WOODS";
			}
			/*
			 * 根据mid 查询iscover 0 是新增 ;1 是修改
			 * 用途：
			 * 规避请求内的iscover 上送错误
			 */
			String queryCoverSql="select *  from bank_specialmerimages where bs_mid=?  and bs_payway=? ";
			List<Map<String, Object>> list=dao.queryForList(queryCoverSql, mid,payway);
			if (list.size()==0) {
				iscover="0";
			}else{
				iscover="1";
			}
			
			/*
			 * 当iscover 为 0 时，必须同事上送5张照片
			 * 本处功能待确认 是否保留 
			 */
			int imageCouns=0;
			if ("0".equals(iscover)) {
				if (5!=fileEntrys.size()) {
					rtnJson.put("status", "E");
					rtnJson.put("errcode","8000");
					rtnJson.put("errmsg","上传照片数量不足，请核实后重新传输");
					rtnJson.put("mid", mid);
					return rtnJson.toJSONString();
				}else {
					imageCouns=5;
				}
			}
			JSONArray imageArray=new JSONArray();
			int count=0;
			String errmsg="";
			/*
			 * 循环上送照片
			 * 1、当上送照片返回成功后   数据库内没有mid对应的记录时做新增，有做修改  并记录数值， 用于判断5张照片是否都上送成功
			 * 2、当上送照片返回失败后  记录失败原因
			 * 3、在循环到最后一条时  如果成功数据=照片数 更新bs_status='1'  否则 将失败原因更新至bs_activity_respmsg内 状态bs_status='0'
			 */
			for (Entry<String, Object> entry:fileEntrys){ 
				JSONObject imgJson=new JSONObject();
				JSONObject item=JSONObject.parseObject(entry.getValue()+""); 
				if (item.getString("fileName")==null) {
					rtnJson.put("status", "E");
					rtnJson.put("errcode","8000");
					rtnJson.put("errmsg","图片名称为空");
					rtnJson.put("mid", mid);
					return rtnJson.toJSONString();
				}
				if (item.getString("mimeType")==null) {
					rtnJson.put("status", "E");
					rtnJson.put("errcode","8000");
					rtnJson.put("errmsg","图片类别为空");
					rtnJson.put("mid", mid);
					return rtnJson.toJSONString();
				}
				byte[] contents=item.getBytes("content");
				if (contents==null) {
					rtnJson.put("status", "E");
					rtnJson.put("errcode","8000");
					rtnJson.put("errmsg","图片内容不正确");
					rtnJson.put("mid", mid);
					return rtnJson.toJSONString();
				}
				Map<String, Object> imgRtnStr=alipayService.uploadImage(new FileItem(item.getString("fileName"), item.getBytes("content"),item.getString("mimeType")));
				imgJson.put(entry.getKey()+"_status",imgRtnStr.get("status"));
				imgJson.put(entry.getKey()+"_msg",imgRtnStr.get("msg"));
				if ("S".equals(imgRtnStr.get("status"))) {
					StringBuffer sBuffer=new StringBuffer();
					sBuffer.append("merge into  bank_specialmerimages ht ");
					sBuffer.append("  using  (select ? mid  FROM dual) dd ");
					sBuffer.append("  on (ht.bs_mid = dd.mid and ht.bs_payway = ?) ");
					if ("0".equals(iscover)&&count==0) {
						sBuffer.append(" when not matched then ");
						sBuffer.append(" insert  (ht.bs_id,ht.bs_payway,bs_activity_type,ht.bs_mid,ht.bs_cdate,bs_lmdate,bs_status,ht.bs_");
						sBuffer.append(entry.getKey() ).append(") values(sys_guid(),?,?,dd.mid,sysdate,sysdate,0,?)");
						dao.update(sBuffer.toString(),mid,payway,payway,activityType,imgRtnStr.get("imgID"));
					}else{
						sBuffer.append(" when  matched then ");
						sBuffer.append(" update set  bs_");
						sBuffer.append(entry.getKey() ).append("= ?");
						dao.update(sBuffer.toString(),mid,payway,imgRtnStr.get("imgID"));
					}
					count=count+1;
					iscover="1";
				}else{ 
					errmsg=errmsg+"|"+entry.getKey()+":"+imgRtnStr.get("msg"); 
				}
				imageArray.add(imgJson);
				if (count==fileEntrys.size()&&imageCouns==5) {
					String updSpecialMerSql="update bank_specialmerimages set bs_status='1', bs_lmdate=sysdate where bs_mid=?";
					dao.update(updSpecialMerSql,mid);
				}else{
					String updateImageSql="merge into  bank_specialmerimages ht "
							+ "  using  (select ? mid  FROM dual) dd "
							+ "  on (ht.bs_mid = dd.mid and ht.bs_payway = ? and bs_checkstand_pic is not null "
							+ "      and  bs_shop_entrance_pic is not null  and  bs_business_license_pic is not null  "
							+ "      and  bs_indoor_pic is not null  and  bs_settled_pic is not null )  "
//							+ "  when not matched then " 
//							+ "    update set bs_activity_respmsg = ? " 
							+ " when  matched then "
							+ " update set bs_activity_respmsg = ? ,bs_status='1'";
					int uptImgCounts =dao.update(updateImageSql,mid,payway,errmsg);
					if (uptImgCounts==0) {
						String updaterespMsg="update bank_specialmerimages set  bs_activity_respmsg = ? where  bs_mid =  ?  and bs_payway = ?";
						dao.update(updaterespMsg, mid,payway ,errmsg);
					}
				}
			} 
			rtnJson.put("mid", imageInfo.get("mid"));
			rtnJson.put("status", "S");
			rtnJson.put("imageinfo", imageArray.toJSONString());
			logger.info("[响应报文]{}",rtnJson);
			return rtnJson.toJSONString();
		} catch (BusinessException e) {
			rtnJson.put("status", "E");
			rtnJson.put("errcode","8000");
			rtnJson.put("errmsg",e.getMessage());
			rtnJson.put("mid", imageInfo.get("mid"));
			return rtnJson.toJSONString();
		}
	}
	
	/**
	 * 商户入驻信息查询
	 * @param hrid
	 * @return
	 */
	public String queryBankMer(String hrid){	
		String sql = "select * from bank_merregister where hrid=?";
		List<Map<String, Object>> list = dao.queryForList(sql, hrid);
		if (list.size()<1) return "报备商户信息不存在";
			
		Map<String, Object> map = list.get(0);
		
		BigDecimal fiid = (BigDecimal) map.get("FIID");
		try {
			if (fiid.intValue()==54){
				return  cupsMerchantService.queryAliMerchants(map);
				//String.valueOf(map.get("MERCHANTID")),fiid,String.valueOf(map.get("MERCHANTNAME")),String.valueOf(map.get("CHANNEL_ID")));
			}else if (fiid.intValue()==53){
				return  cupsMerchantService.queryWxMerchants(map);
			}else if (fiid.intValue()==60){
				return  netCupsMerchantService.queryWxMerchants(map);
			}else if (fiid.intValue()==61){
				return  netCupsMerchantService.queryAliMerchants(String.valueOf(map.get("MERCHANTID")),fiid,String.valueOf(map.get("MERCHANTNAME")),String.valueOf(map.get("MCH_ID")));
			}
		} catch (Exception e) {
			logger.error("查询入驻商户状态异常：",e);
		}
		return hrid;
	}
	
	/**
	 * 子商户配置   查询
	 * @param hrid
	 * @return
	 */
	public String  querySubBankMer(String hrid){
		
		try {
			String sql = "select * from bank_merregister where hrid=?";
			List<Map<String, Object>> list = dao.queryForList(sql, hrid);
			if (list.size()<1) {
				logger.info("------------------->不存在 hrid={}的商户",hrid);
				return "hrid对应的商户不存在";
			}
			Map<String, Object> map = list.get(0);
			String reqmsgid =String.valueOf( map.get("REQMSGID"));
			String fiid =String.valueOf(map.get("FIID"));
//			if ("11".equals(fiid)) {
//				return cmbcService.querySubMerchant(reqmsgid);
//			}
			/**
			 * 根据fiid向各自上游通道发起查询
			 */
			
		} catch (Exception e) {
			logger.error("厦门民生微信查询商户配置异常：",e);
		}
		return hrid;
	}

	
	
	/**
	 * 查询微信商户是否验证通过
	 * @param hrid
	 * @return
	 */
	public void queryIsCheckMer(String hrid) {
		try {
			String sql = "select * from bank_merregister where approvestatus='Y' and hrid=?";
			List<Map<String, Object>> list = dao.queryForList(sql, hrid);
			if (list.size()<1) {
				logger.info("------------------->不存在 hrid={}的商户或未入驻成功",hrid);
				return ;
			}
			Map<String, Object> bankMer=list.get(0);
			Map<String, String> subMer = new HashMap<String, String>();
			String reqUrl="";
			subMer.put("mch_id", mchid);
			subMer.put("sub_mch_id", String.valueOf(bankMer.get("MERCHANTCODE")));
			subMer.put("channel_id", channelid);
			subMer.put("sign_type", "HMAC-SHA256");
			subMer.put("nonce_str",  cupsService.getRandomString(32));
			reqUrl="https://api.mch.weixin.qq.com/mchrisk/bankquerymchauditinfo"; 
			String reqStr =WXPayUtil.generateSignedXml(subMer, cupsWxMd5Key, SignType.HMACSHA256);
			String res = null;
			try {
				logger.info("[银联-微信]商户认证查询请求信息----->" + reqStr);
				res = client.sendFormData(reqUrl, reqStr);
				logger.info("[银联-微信]商户认证查询响应信息----->" + res);
			} catch (Exception e) {
				logger.info("[银联-微信]商户{}:{}认证查询异常，错误 原因{}----->", bankMer.get("HRID"), bankMer.get("MERCHANTID"), e.getMessage());
				return ;
			}
			Map<String, String> respJson=null;
			try {
				respJson = cupsService.xmlToMap(res);
			} catch (Exception e) {
				 logger.info("[银联-微信]商户入驻查询    响应报文转化异常：{}",e.getMessage());
				 return ;
			}
			if ("SUCCESS".equals(respJson.get("return_code"))) {
				String rtnCode = respJson.get("result_code"); 
				String resmsg=(String)bankMer.get("RTNMSG");
				if ("SUCCESS".equals(rtnCode)) {
					String checkStatus=respJson.get("audit_status");
					if("PASSED".equals(checkStatus)){
						// 认证通过
						resmsg="认证通过";
					}else if("AUDITING".equals(checkStatus)){
						// 审核中
						resmsg="认证中";
					}else{
						// 已驳回
						resmsg="已驳回";
					}
					String updateSql = "update Bank_MerRegister set rtnmsg=?,lmdate=sysdate  "
							+ " where hrid=? ";
					dao.update(updateSql, resmsg,hrid);
				}else{
					logger.error("[银联-微信]商户认证查询失败", respJson.get("rtnCode")+":"+respJson.get("return_msg"));
				}
			}
		} catch (Exception e) {
			logger.error("[银联-微信]商户认证查询失败", e);
		}
	}
}
