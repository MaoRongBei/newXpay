package com.hrtpayment.xpay.channel.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.baidu.service.BaiduPayService;
import com.hrtpayment.xpay.bcm.service.BcmPayService;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.cib.service.CibPayService;
import com.hrtpayment.xpay.cmbc.bean.json.HrtCmbcBean;
import com.hrtpayment.xpay.cmbc.service.CmbcPayService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.AliJspayService;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.common.service.impl.WechatService;
import com.hrtpayment.xpay.common.service.impl.XpayService;
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.cupsAT.service.CupsATPayService;
import com.hrtpayment.xpay.netCups.service.NetCupsPayService;
import com.hrtpayment.xpay.quickpay.cups.service.CupsQuickPayService;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.redis.RedisUtilForTest;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;
/**
 * 通道商service
 * @author aibing
 * 2016年11月15日
 */
@Service
public class ChannelService {
	private Logger logger = LogManager.getLogger();
	@Autowired
	private JdbcDao dao;
	@Autowired
	CmbcPayService cmbcPay;
	@Autowired
	MerchantService merService;
	@Autowired
	WechatService wechatService;
	@Autowired
	AliJspayService alipayService;
	@Autowired
	ManageService manageService;
	
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
	XpayService xpayService;
	@Autowired
	CupsQuickPayService cupsQuickPayService;
	@Autowired
	CibPayService cibPayService;
   
	@Value("${xpay.special.unno}")
	String specUnno;
	
	/**
	 * 校验unno,key
	 * 
	 * @param bean
	 * @return
	 * @throws BusinessException
	 */
	public String checkChannelInfo(HrtPayXmlBean bean) throws BusinessException {
		String unno = bean.getUnno();
		if (unno == null || "".equals(unno)) {
			throw new BusinessException(9002, "unno错误");
		}
		String sql = "select * from HRT_XPAYORGINFO where unno=?";
		List<Map<String, Object>> list = dao.queryForList(sql, unno);
		if (list.size() < 1) {
			throw new BusinessException(9006, "未找到unno对应的签名密钥");
		}
		String status = (String) list.get(0).get("STATUS");
		if (!"1".equals(status)) {
			logger.info("unno:{},状态不可用:{}", unno, status);
			throw new BusinessException(9002, "unno错误");
		}
		String key = (String) list.get(0).get("MACKEY");
		if (key == null || "".equals(key)) {
			logger.info("unno:{},密钥错误:{}", unno, key);
			throw new BusinessException(9006, "未找到unno对应的签名密钥");
		}
//		 String sign =bean.calSign(key);
		 String sign =SimpleXmlUtil.getMd5Sign(beanToMap(bean), key);
		if (!sign.equals(bean.getSign())) {
			logger.info("校验签名失败:计算签名={},上传签名={}", sign, bean.getSign());
			throw new BusinessException(9004, "签名校验失败");
		}
		if (bean.getOrderid() == null || !bean.getOrderid().startsWith(unno)) {
			throw new BusinessException(9005, "订单号格式错误");
		}
		return key;
	}
	public String checkChannelInfo(Map<String,String> map) {
		String unno = map.get("unno");
		if (unno == null || "".equals(unno)) {
			throw new HrtBusinessException(9002);
		}
		String sql = "select * from HRT_XPAYORGINFO where unno=?";
		List<Map<String, Object>> list = dao.queryForList(sql, unno);
		if (list.size() < 1) {
			throw new HrtBusinessException(9006);
		}
		String status = (String) list.get(0).get("STATUS");
		if (!"1".equals(status)) {
			logger.info("unno:{},状态不可用:{}", unno, status);
			throw new HrtBusinessException(9002);
		}
		String key = (String) list.get(0).get("MACKEY");
		if (key == null || "".equals(key)) {
			logger.info("unno:{},密钥错误:{}", unno, key);
			throw new HrtBusinessException(9006);
		}
		String sign = SimpleXmlUtil.getMd5Sign(map, key);
		if (!sign.equals(map.get("sign"))) {
			logger.info("校验签名失败:计算签名={},上传签名={}", sign, map.get("sign"));
			throw new HrtBusinessException(9004);
		}
		if (map.get("orderid") == null || !map.get("orderid").startsWith(unno)|| map.get("orderid").length()>32) {
			throw new HrtBusinessException(9005);
		}
		return key;
	}

	/**
	 * 智能pos 退款专用   验签
	 * @param map
	 * @return
	 */
	public String checkChannelInfoForPos(Map<String,String> map) {
		String unno = map.get("unno");
		if (unno == null || "".equals(unno)) {
			throw new HrtBusinessException(9002);
		}
		String sql = "select * from HRT_XPAYORGINFO where unno=?";
		List<Map<String, Object>> list = dao.queryForList(sql, unno);
		if (list.size() < 1) {
			throw new HrtBusinessException(9006);
		}
		String status = (String) list.get(0).get("STATUS");
		if (!"1".equals(status)) {
			logger.info("unno:{},状态不可用:{}", unno, status);
			throw new HrtBusinessException(9002);
		}
		String key = (String) list.get(0).get("MACKEY");
		if (key == null || "".equals(key)) {
			logger.info("unno:{},密钥错误:{}", unno, key);
			throw new HrtBusinessException(9006);
		}
		String sign = SimpleXmlUtil.getMd5Sign(map, key);
		if (!sign.equals(map.get("sign"))) {
			logger.info("校验签名失败:计算签名={},上传签名={}", sign, map.get("sign"));
			throw new HrtBusinessException(9004);
		}
		return key;
	}
	
	public Map<String, Object> checkChannelInfoForSyt(HrtPayXmlBean bean) throws BusinessException {
		String tid = bean.getTid();
		String mid = bean.getMid();
		if (tid == null || "".equals(tid)) {
			throw new BusinessException(9002,"未找到对应的tid");
		}
		if (mid == null || "".equals(mid)) {
			throw new BusinessException(9002,"未找到对应的mid");
		}
		String sql = " select hst.mackey,ht.minfo1 from  hrt_termacc ht ,hrt_syt_tidinfo hst  where ht.htaid=hst.htaid and mid =? and sn=? and ht.status='1' ";
		List<Map<String, Object>> list = dao.queryForList(sql, mid,tid);
		if (list.size() < 1) {
			throw new BusinessException(9006,"未找到对应的秘钥参数");
		}
		String key = (String) list.get(0).get("MACKEY");
		if (key == null || "".equals(key)) {
			logger.info("mid:{},sn:{},密钥错误:{}", mid,tid, key);
			throw new BusinessException(9006,"未找到对应的秘钥参数");
		}
		String sign="";
		try {
			sign = SimpleXmlUtil.getMd5Sign(beanToMap(bean), key);
		} catch (BusinessException e) {
			logger.error("订单{}验签异常,{}",bean.getOrderid(),e.getMessage() );
			throw new BusinessException(9004, "签名校验失败");
		}
		if (!sign.equals(bean.getSign())) {
			logger.info("订单{},校验签名失败:计算签名={},上传签名={}",bean.getOrderid(), sign,bean.getSign());
			throw new BusinessException(9004, "签名校验失败");
		}
		if (bean.getOrderid() == null || !bean.getOrderid().startsWith(bean.getUnno())) {
			throw new BusinessException(9005, "订单号格式错误");
		}
		return list.get(0);
	}
	public void queryOrder(HrtPayXmlBean bean) {
		bean.setStatus("R");
		/*
		 * 2019-04-08 修改
		 * 
		 * 查询次数 及查询 时间累增是redis数据库内
		 * 
		 */
		long beginTime=System.currentTimeMillis();
		List<Map<String, Object>> list = dao.queryForList("select w.*,bm.mch_id,bm.channel_id from pg_wechat_txn w,bank_merregister bm where  bm.merchantcode=w.bankmid and bm.fiid=w.fiid and mer_orderid=?",
				bean.getOrderid());
		long endTime=System.currentTimeMillis();
		try {
			RedisUtilForTest.addCount("addCountsForCounts", endTime-beginTime);
		} catch (BusinessException e1) {
			logger.error("[数据收集]  channelService.queryOrder() 查询 所需时长获取异常 ，原因{}",e1.getMessage());
		}
		if (list.size() < 1) {
			logger.info("要查询订单不存在:{}", bean.getOrderid());
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("订单号不存在！");
			return;
		}
		Map<String, Object> map = list.get(0);
		String dbstatus = (String) map.get("STATUS");
		BigDecimal fiid = (BigDecimal) map.get("FIID");
		bean.setPaymode(String.valueOf(map.get("TRANTYPE")));
		bean.setPayOrderTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		if ("1".equals(dbstatus)) {
			logger.info("订单状态已经为成功");
			bean.setErrdesc("SUCCESS");
			bean.setPayOrderTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(list.get(0).get("LMDATE")));
			if("110000".equals(bean.getUnno())){
				bean.setPayOrderTime(new SimpleDateFormat("yyyyMMddHHmmss").format(list.get(0).get("LMDATE")));
				bean.setAmount(String.valueOf(list.get(0).get("TXNAMT")));
//			}else if("880000".equals(bean.getUnno())||"962073".equals(bean.getUnno())){
			}else if(specUnno.contains(bean.getUnno())){
				bean.setRtnMsg(list.get(0).get("respcode")==null?"":String.valueOf(list.get(0).get("respcode")));
				bean.setRtnMsg(list.get(0).get("respmsg")==null?"":String.valueOf(list.get(0).get("respmsg")));
				bean.setBankType(list.get(0).get("bank_type")==null?"":String.valueOf(list.get(0).get("bank_type")));//后期易融码需要具体银行名称将本处放开 下一行注释掉
//				bean.setBankType(list.get(0).get("paytype")==null?"":String.valueOf(list.get(0).get("paytype")));
				bean.setUserId(list.get(0).get("userid")==null?"":String.valueOf(list.get(0).get("userid")));
			}
			bean.setStatus("S");
			return;
		}else if("0".equals(dbstatus) && !"110000".equals(bean.getUnno())
//				&& !"880000".equals(bean.getUnno())
//				&& !"962073".equals(bean.getUnno())
				&& !specUnno.contains(bean.getUnno())
				//立码富测试使用  不提交生产
				//=================================
//				&& !"111000".equals(bean.getUnno())
				//=================================
				&& !"112052".equals(bean.getUnno())){
			logger.info("交易状态未知");
			bean.setErrdesc("交易状态未知");
			bean.setStatus("R");
			return;
		}else if("5".equals(dbstatus)){
			logger.info("订单{}已经关闭",bean.getOrderid());
			bean.setErrdesc("交易已关闭");
			bean.setErrcode("8000");
			bean.setStatus("E");
			return;
		}else if("6".equals(dbstatus)){
			logger.info("订单{}交易失败",bean.getOrderid());
			bean.setErrdesc("交易失败 ："+list.get(0).get("respmsg"));
			bean.setErrcode("8000");
			bean.setStatus("E");
			return;
		}else if("7".equals(dbstatus)){
			logger.info("订单{}已经撤销",bean.getOrderid());
			bean.setErrdesc("交易已撤销");
			bean.setErrcode("8000");
			bean.setStatus("E");
			return;
		}
		if (fiid == null) {
			logger.info("订单通道id为空");
			return;
		}
		int ifiid = fiid.intValue();
		if (ifiid == 25) {
			String tradeState =baiduPayService.queryOrder(map);
			if ("SUCCESS".equals(tradeState)){
				bean.setStatus("S");
				queryOrderTimeAndAmount(bean);
			} else if ("DOING".equals(tradeState) ) {
				bean.setStatus("R");
			} else if ("CLOSED".equals(tradeState)) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("订单已关闭");
			} else if ("ERROR".equals(tradeState)) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("交易状态未知");
			} else {
				bean.setStatus("R");
				bean.setErrdesc("交易状态未知");
			}
		} else if (ifiid == 34) {
			String tradeState = cibPayService.queryOrder(bean,map);
			if ("SUCCESS".equals(tradeState) || "REFUND".equals(tradeState)){
				bean.setStatus("S");
				queryOrderTimeAndAmount(bean);
			} else if ("NOTPAY".equals(tradeState) || "NOPAY".equals(tradeState)) {
				bean.setStatus("R");
			} else if ("CLOSED".equals(tradeState)) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("订单已关闭");
			} else if ("REVOKED".equals(tradeState)) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("订单已撤销");
			} else if ("PAYERROR".equals(tradeState)) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("订单支付失败");
			} else if("USERPAYING".equals(tradeState)){
				bean.setStatus("R");
				bean.setErrdesc("交易状态未知");
			} else {
				bean.setStatus("R");
				bean.setErrdesc("交易状态未知");
			}
		}else if (ifiid == 40) {
			String tradeState;
			try {
				tradeState = cupsQuickPayService.queryOrder(map);
				if ("S".equals(tradeState)){
					bean.setStatus("S");
					queryOrderTimeAndAmount(bean);
				} else if ("R".equals(tradeState) ) {
					bean.setStatus("R");
				} else if ("E".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已关闭");
				} else {
					bean.setStatus("R");
					bean.setErrdesc("交易状态未知");
				}
			} catch (BusinessException e) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("交易状态未知");
			}
		}  else if (ifiid == 43) {
			String tradeState =bcmPayService.queryOrder(bean,map);
			if ("SUCCESS".equals(tradeState)){
				bean.setStatus("S");
				queryOrderTimeAndAmount(bean);
			} else if ("DOING".equals(tradeState) ) {
				bean.setStatus("R");
			} else if ("FAIL".equals(tradeState)) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("订单支付失败");
			} else if ("ERROR".equals(tradeState)) {
				bean.setStatus("R");
				bean.setErrcode("8000");
				bean.setErrdesc("交易状态未知");
			} else {
				bean.setStatus("R");
				bean.setErrdesc("交易状态未知");
			}
		}else if (ifiid == 46) {
			String tradeState =bcmPayService.queryOrder(bean,map);
			if ("SUCCESS".equals(tradeState)){
				bean.setStatus("S");
				queryOrderTimeAndAmount(bean);
			} else if ("DOING".equals(tradeState) ) {
				bean.setStatus("R");
			} else if ("FAIL".equals(tradeState)) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("订单支付失败");
			} else if ("ERROR".equals(tradeState)) {
				bean.setStatus("R");
				bean.setErrcode("8000");
				bean.setErrdesc("交易状态未知");
			} else {
				bean.setStatus("R");
				bean.setErrdesc("交易状态未知");
			}
		} else if (ifiid == 53) { 
			try {
				String tradeState = cupsATPayService.cupsWxQuery(bean,map);
				if ("SUCCESS".equals(tradeState)){
					bean.setStatus("S");
				} else if ("DOING".equals(tradeState) ) {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("订单处理中");
				} else if ("FAIL".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单支付失败");
				} else if ("CLOSED".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已关闭");
				} else if ("REVOKED".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已撤销");
				} else if ("ERROR".equals(tradeState)) {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("交易状态未知");
				} else {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("交易状态未知");
				}
			} catch (BusinessException e) {
				bean.setStatus("R");
				bean.setErrcode("8000");
				bean.setErrdesc("交易状态未知");
			} 
		} else if (ifiid == 54) {
			try {
				String tradeState = cupsATPayService.cupsAliQuery(bean,map);
				if ("SUCCESS".equals(tradeState)){
					bean.setStatus("S");
				} else if ("DOING".equals(tradeState) ) {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("订单处理中");
				} else if ("FAIL".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单支付失败");
				} else if ("CLOSED".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已关闭");
				} else if ("REVOKED".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已撤销");
				} else if ("ERROR".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8001");
					bean.setErrdesc("交易状态未知");
				} else if ("DOING_TRADE_NOT_EXIST".equals(tradeState)) {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("交易处理中");
				} else {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("交易状态未知");
				}
			} catch (BusinessException e) {
				bean.setStatus("R");
				bean.setErrcode("8001");
				bean.setErrdesc("交易状态未知");
			} 
		} else if (ifiid == 60) { 
			try {
				String tradeState =netCupsPayService.cupsWxQuery(bean,map);
				if ("SUCCESS".equals(tradeState)){
					bean.setStatus("S");
				} else if ("DOING".equals(tradeState) ) {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("订单处理中");
				} else if ("FAIL".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单支付失败");
				} else if ("CLOSED".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已关闭");
				} else if ("REVOKED".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已撤销");
				} else if ("ERROR".equals(tradeState)) {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("交易状态未知");
				} else {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("交易状态未知");
				}
			} catch (BusinessException e) {
				bean.setStatus("R");
				bean.setErrcode("8001");
				bean.setErrdesc("交易状态未知");
			}
		} else if (ifiid == 61) { 
			try {
				String tradeState =netCupsPayService.cupsAliQuery(bean,map);
				if ("SUCCESS".equals(tradeState)){
					bean.setStatus("S");
				} else if ("DOING".equals(tradeState) ) {
					bean.setStatus("R");
					bean.setErrcode("8001");
				} else if ("FAIL".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单支付失败");
				} else if ("CLOSED".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已关闭");
				} else if ("REVOKED".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8000");
					bean.setErrdesc("订单已撤销");
				} else if ("ERROR".equals(tradeState)) {
					bean.setStatus("E");
					bean.setErrcode("8001");
					bean.setErrdesc("交易状态未知");
				} else if ("DOING_TRADE_NOT_EXIST".equals(tradeState)) {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("交易处理中");
				} else {
					bean.setStatus("R");
					bean.setErrcode("8001");
					bean.setErrdesc("交易状态未知");
				}
			} catch (BusinessException e) {
				bean.setStatus("R");
				bean.setErrcode("8001");
				bean.setErrdesc("交易状态未知");
			}
		}else {
			bean.setStatus("R");
			bean.setErrcode("8001");
			bean.setErrdesc("交易状态未知");
		}
		if("110000".equals(bean.getUnno())){
			bean.setPayOrderTime(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
		}
	}
	
	 /**
	  * 订单关闭
	  * 根据订单号进行查询，订单状态不为1 的订单可以进行关闭
	  * @param orderid
	  * @return
	  */
	 public  void  closeOrder(HrtPayXmlBean bean){
		 bean.setStatus("R");
		 String  orderid=bean.getOrderid();
		 String  closeOrdSql="select pt.fiid, pt.status,pt.mer_orderid,pt.bk_orderid,pt.bankmid, bm.mch_id,bm.channel_id,pt.respcode,pt.respmsg "
		 		+ " from pg_wechat_txn pt ,bank_merregister bm "
		 		+ " where bankmid=merchantcode and  mer_orderid=?";
		 List<Map<String, Object>> closeOrdList=dao.queryForList(closeOrdSql, orderid);
		 if (closeOrdList.size()==0) {
			 logger.info("[订单关闭] 订单{}不存在，无法关闭",orderid);
			 bean.setStatus("E");
			 bean.setErrcode("8000");
			 bean.setErrdesc("订单号不存在！");
		 }
		 if (closeOrdList.size()>1) {
			  logger.info("[订单关闭] 订单{}异常，无法关闭，请核实订单",orderid);
			  bean.setStatus("E");
			  bean.setErrcode("8000");
			  bean.setErrdesc("订单异常，无法关闭，请核实订单！");
		 }
		 Map<String, Object> closeOrder=closeOrdList.get(0);
		 String status=String.valueOf(closeOrder.get("status"));
		 if ("5".equals(status)) {
//			 return orderid+"订单已关闭";
			 logger.info("[订单关闭] 订单{}已关闭",orderid);
			 bean.setStatus("S");
		 }else if("1".equals(status)){
			 logger.info("[订单关闭] 订单{}已经完成，无法关闭",orderid);
			 bean.setStatus("E");
			 bean.setErrcode("8000");
			 bean.setErrdesc("订单已经完成，无法关闭！");
		 }
		 String fiid=String.valueOf(closeOrder.get("fiid"));
		 String respStatus="";
		 try {
			 if ("53".equals(fiid)) {
				 respStatus=cupsATPayService.cupsWxClose(closeOrder);
			 }else if ("54".equals(fiid)) {
				 respStatus=cupsATPayService.cupsAliClosed(closeOrder);
			 }else if ("60".equals(fiid)) {
				 respStatus=netCupsPayService.cupsWxClose(closeOrder);
			 }else if ("61".equals(fiid)) {
				 respStatus=netCupsPayService.cupsAliClosed(closeOrder);	
			 }else {
				 logger.info("[订单关闭] 订单{}:{}该通道不支持关单操作",orderid,fiid);
				 bean.setStatus("E");
				 bean.setErrcode("8000");
				 bean.setErrdesc("该笔订单不支持关单操作！");
			 }
		} catch (Exception e) {
			logger.error("[订单关闭] 订单{}关闭异常，原因：{}", orderid,e.getMessage());
			 bean.setStatus("E");
			 bean.setErrcode("8000");
			 bean.setErrdesc("订单关闭异常");
		}
		if("SUCCESS".equals(respStatus)){
			bean.setStatus("S");
			bean.setErrdesc("订单关闭成功");
		}else if("DOING".equals(respStatus)){
			bean.setStatus("R");
			bean.setErrdesc("订单关闭中");
		}else if ("E-S".equals(respStatus)) {
			bean.setStatus("E");
			bean.setErrdesc("订单关闭失败，订单已支付请重新查询。");
		}else if("FAIL".equals(respStatus)){
			bean.setStatus("E");
			 bean.setErrcode("8000");
			 bean.setErrdesc("订单关闭失败！");
		}else{
			bean.setStatus("R");
			bean.setErrdesc("交易状态未知");
		}
		 
	 }
	
	 /**
	  * 订单撤销
	  * 根据订单号进行查询，订单状态不为1 的订单可以进行撤销
	  * @param orderid
	  * @return
	  */
	 public  void  cancelOrder(HrtPayXmlBean bean){
		 bean.setStatus("R");
		 String  orderid=bean.getOrderid();
		 String  closeOrdSql="select pt.fiid, pt.status,pt.mer_orderid,pt.bk_orderid,pt.bankmid, bm.mch_id,bm.channel_id,pt.respcode,pt.respmsg "
		 		+ " from pg_wechat_txn pt ,bank_merregister bm "
		 		+ " where bankmid=merchantcode and  mer_orderid=?";
		 List<Map<String, Object>> cancelOrdList=dao.queryForList(closeOrdSql, orderid);
		 if (cancelOrdList.size()==0) {
			 logger.info("[订单撤销] 订单{}不存在，无法撤销",orderid);
			 bean.setStatus("E");
			 bean.setErrcode("8000");
			 bean.setErrdesc("订单号不存在！");
			 return ;
		 }
		 if (cancelOrdList.size()>1) {
			  logger.info("[订单撤销] 订单{}异常，无法撤销，请核实订单",orderid);
			  bean.setStatus("E");
			  bean.setErrcode("8000");
			  bean.setErrdesc("订单异常，无法撤销，请核实订单！");
			  return ;
		 }
		 Map<String, Object> cancelOrd=cancelOrdList.get(0);
		 String status=String.valueOf(cancelOrd.get("status"));
		 if ("7".equals(status)) {
			 logger.info("[订单撤销] 订单{}已撤销",orderid);
			 bean.setStatus("S");
			 bean.setErrdesc("订单撤销成功");
			 return ;
		 }else if("1".equals(status)){
			 logger.info("[订单撤销] 订单{}已经成功，无法撤销，请执行退款操作。",orderid);
			 bean.setStatus("E");
			 bean.setErrcode("8000");
			 bean.setErrdesc("订单支付成功，无法撤销！");
			 return ;
		 }
		 String fiid=String.valueOf(cancelOrd.get("fiid"));
		 String respStatus="";
		 try {
			 if ("53".equals(fiid)) {
				 respStatus=cupsATPayService.cupsWxCancel(cancelOrd);
			 }else if ("54".equals(fiid)) {
				 respStatus=cupsATPayService.cupsAliCancel(cancelOrd);
			 }else if ("60".equals(fiid)) {
				 respStatus=netCupsPayService.cupsWxCancel(cancelOrd);
			 }else if ("61".equals(fiid)) {
				 respStatus=netCupsPayService.cupsAliCancel(cancelOrd);	
			 }else {
				 logger.info("[订单撤销] 订单{}:{}该通道不支持关单操作",orderid,fiid);
				 bean.setStatus("E");
				 bean.setErrcode("8000");
				 bean.setErrdesc("该笔订单不支持撤销操作！");
			 }
		} catch (Exception e) {
			logger.error("[订单撤销] 订单{}撤销异常，原因：{}", orderid,e.getMessage());
			 bean.setStatus("E");
			 bean.setErrcode("8000");
			 bean.setErrdesc("订单撤销异常");
			 return ;
		}
		if("SUCCESS".equals(respStatus)){
			bean.setStatus("S");
			bean.setErrdesc("订单撤销成功");
		}else if("DOING".equals(respStatus)){
			bean.setStatus("R");
			bean.setErrdesc("订单撤销中");
//		}else if ("E-S".equals(respStatus)) {
//			bean.setStatus("E");
//			bean.setErrdesc("订单撤销失败，订单已支付请重新查询。");
		}else if("FAIL".equals(respStatus)){
			bean.setStatus("E");
			 bean.setErrcode("8000");
			 bean.setErrdesc("订单撤销失败！");
		}else{
			bean.setStatus("R");
			bean.setErrdesc("交易状态未知");
		}
		 
	 }
	
	
	/**
	 * 公众号支付下单
	 * @param unno
	 * @param mid
	 * @param orderid
	 * @param subject
	 * @param amount
	 * @return url:支付地址
	 * @throws BusinessException 
	 * @throws HrtBusinessException
	 */
	public String insertGzhOrder(HrtPayXmlBean bean,String limit_pay,BigDecimal amount,String hybRate,String hybType) throws BusinessException {
		
		String unno =bean.getUnno();
		String mid =bean.getMid();
		String orderid=bean.getOrderid();
		String qrtid = bean.getTid();
		String subject=bean.getSubject();
		/*
		 * 2018-11-27 修改
		 * 
		 * 机构 j62077 根据定位获取交易地址area 
		 * 
		 */
		String area=bean.getArea(); //交易地点
		//商品名为空则取商户名
		if (subject == null || "".equals(subject)) {
			subject = merService.queryMerName(mid);
		}
//		Map<String,Object> paywayMap = queryPayway(unno,mid);
		Map<String,Object> paywayMap = cmbcPay.getMerchantCode3(unno, mid, "WXPAY",amount,area,"ZS");
		BigDecimal fiid = (BigDecimal) paywayMap.get("FIID");
		xpayService.checkBankTxnLimit(fiid.intValue(),amount,"WXPAY");
		Object merchantCode = paywayMap.get("MERCHANTCODE");
		String orgcode = String.valueOf(paywayMap.get("ORGCODE"));
		//插入订单信息
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?", orderid);
		if (list.size()>0) {
			throw new HrtBusinessException(9007);
		}
		String sql = "insert into pg_wechat_txn (pwid,fiid, txntype,cdate,status,"
				+ "mer_orderid, detail, txnamt, mer_id,unno,bankmid,bank_type,mer_tid,trantype,hybtype,hybrate,trade_type) values"
				+ "(S_PG_Wechat_Txn.nextval,?,'0',sysdate,'A',?,?,?,?,?,?,?,?,'1',?,?,'ZS')";
		dao.update(sql, fiid, orderid, subject, amount, mid, unno,merchantCode,"no_credit".equals(limit_pay)?"no_credit":null,
				qrtid,hybType,hybRate);
		
		String bankAppid =String.valueOf(paywayMap.get("APPID"));
		String isCredit = String.valueOf(paywayMap.get("ISCREDIT")==null?1:paywayMap.get("ISCREDIT"));
		int ifiid = fiid.intValue();
		if((bean.getAppid()==null || "".equals(bean.getAppid())) 
				|| (bean.getOpenid()==null || "".equals(bean.getOpenid()))){
			String url =wechatService.getPubaccPayUrl(ifiid, orderid,isCredit);
			bean.setQrcode(url);
			bean.setStatus("S");
			return "";
		}else{
			if(bankAppid.equals(bean.getAppid())){
				String payinfo =wechatService.getJsPayInfo(ifiid, orderid, bean.getOpenid(),isCredit);
				bean.setPayinfo(payinfo);
				bean.setStatus("S");
				return "";
			}else{
				String url =wechatService.getPubaccPayUrl(ifiid, orderid,isCredit);
				bean.setQrcode(url);
				bean.setStatus("S");
				return "";
			}
		}
	}

	
	public String insertJsapiOrder(HrtPayXmlBean bean,String limit_pay,BigDecimal amount,String hybRate,String hybType) throws BusinessException{
		String unno =bean.getUnno();
		String mid =bean.getMid();
		String orderid=bean.getOrderid();
		String qrtid = bean.getTid();
		String subject=bean.getSubject();
		//商品名为空则取商户名
		if (subject == null || "".equals(subject)) {
			subject = merService.queryMerName(mid);
		}
//		Map<String,Object> paywayMap = queryPayway(unno,mid);
		/*
		 * 2018-11-27 修改
		 * 
		 * 机构 j62077 根据定位获取交易地址area 
		 * 
		 */
		String area=bean.getArea(); //交易地点
		Map<String,Object> paywayMap = cmbcPay.getMerchantCode3(unno, mid, "ZFBZF",amount,area,"ZS");
		BigDecimal fiid = (BigDecimal) paywayMap.get("FIID");
		xpayService.checkBankTxnLimit(fiid.intValue(),amount,"ZFBZF");
		Object merchantCode = paywayMap.get("MERCHANTCODE");
		String orgcode = String.valueOf(paywayMap.get("CHANNEL_ID"));
		String isCredit= String.valueOf(paywayMap.get("isCredit")==null?1:paywayMap.get("isCredit"));
		//插入订单信息
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?", orderid);
		if (list.size()>0) {
			throw new HrtBusinessException(9007);
		}
		String insertPaySql=" insert into pg_wechat_txn (pwid,txntype,trantype,cdate,lmdate,status,txnamt,detail"
				+ ",fiid,mer_orderid,mer_id,bankmid,hybtype,hybrate,unno,mer_tid,trade_type)"
				+ " values (S_PG_Wechat_Txn.nextval,0,2,sysdate,sysdate,'A',?,?,?,?,?,?,?,?,?,?,'ZS') ";
		dao.update(insertPaySql,amount,subject, fiid,orderid,mid,merchantCode,hybType,hybRate,unno,qrtid);
		int ifiid = fiid.intValue();
//		String url =alipayService.getPubaccPayUrl(ifiid, orderid,orgcode);
		
		if (bean.getAppid()!=null&&!"".equals(bean.getAppid())) {
			if(bean.getAppid().equals(paywayMap.get("APPID"))){
				String payinfo=alipayService.getJsPayInfo(fiid.intValue(), orderid, bean.getOpenid(),bean.getUserId(),isCredit);
				JSONObject jObject=new JSONObject();
				jObject.put("package", payinfo);
				bean.setPayinfo(jObject.toJSONString());
				bean.setStatus("S");
				return "";
			}else{
				String url =alipayService.getPubaccPayUrl(ifiid, orderid,isCredit);
				logger.info("支付宝公众号支付URL："+url);
				bean.setQrcode(url);
				bean.setStatus("S");
				return "";
			}
		}else{
			String url =alipayService.getPubaccPayUrl(ifiid, orderid,isCredit);
			logger.info("支付宝公众号支付URL："+url);
			bean.setQrcode(url);
			bean.setStatus("S");
			return "";
		}
	}
	
	/**
	 * 根据unno和mid获取支付通道信息
	 * @param unno
	 * @param mid
	 * @return
	 * @throws HrtBusinessException
	 */
	public Map<String,Object> queryPayway(String unno,String mid) {
		List<Map<String,Object>> paywayList = null;
		if (null == unno || "110000".equals(unno)) {
			String sql = "select a.merchantcode,a.fiid,fi.fiinfo2 from Bank_MerRegister a,HRT_MerBanksub b,"+
					"Hrt_Merchacc ma,Hrt_Fi fi where ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid " + 
					"and fi.fiid = a.fiid and a.approvestatus='Y' and a.status=1 and ma.hrt_MID =? and fi.fiinfo2 like '%WXPAY%'";
			paywayList = dao.queryForList(sql, mid);
		} else {
			String sql = "select a.merchantcode,a.fiid,fi.fiinfo2 from Bank_MerRegister a,HRT_MerBanksub b,"+
					"Hrt_Merchacc ma,Hrt_Fi fi where ma.hrt_mid = b.hrt_mid and a.hrid = b.hrid " + 
					"and fi.fiid = a.fiid and a.approvestatus='Y' and a.status=1 and ma.hrt_MID =? and ma.unno=? and fi.fiinfo2 like '%WXPAY%'";
			paywayList = dao.queryForList(sql, mid, unno);
		}
		if (paywayList.size()<1) throw new HrtBusinessException(9009); //通道未开通
		return paywayList.get(0);
	}
	
	
	
	/**
	 * 公众号支付下单
	 * @param unno
	 * @param mid
	 * @param orderid
	 * @param subject
	 * @param amount
	 * @param openid
	 * @return payeinfo
	 * @throws BusinessException 
	 * @throws HrtBusinessException
	 */
	public String insertOrderByOpenid(HrtPayXmlBean bean,String unno,String mid,String orderid,String subject,BigDecimal amount
			,String limit_pay,String qrtid,String openid) throws BusinessException {
		//商品名为空则取商户名
		if (subject == null || "".equals(subject)) {
			subject = merService.queryMerName(mid);
		}
		/*
		 * 2018-11-27 修改
		 * 
		 * 机构 j62077 根据定位获取交易地址area 
		 * 
		 */
		String area=bean.getArea(); //交易地点
		Map<String,Object> paywayMap = cmbcPay.getMerchantCode3(unno, mid, "WXPAY",amount,area,"ZS");
		BigDecimal fiid = (BigDecimal) paywayMap.get("FIID");
		Object merchantCode = paywayMap.get("MERCHANTCODE");
		String isCredit= String.valueOf(paywayMap.get("isCredit")==null?1:paywayMap.get("isCredit"));
		//插入订单信息
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?", orderid);
		if (list.size()>0) {
			throw new HrtBusinessException(9007);
		}
		String sql = "insert into pg_wechat_txn (pwid,fiid, txntype,cdate,status,"
				+ "mer_orderid, detail, txnamt, mer_id,unno,bankmid,bank_type,mer_tid,trantype) values"
				+ "(S_PG_Wechat_Txn.nextval,?,'0',sysdate,'A',?,?,?,?,?,?,?,?,'1')";
		dao.update(sql, fiid, orderid, subject, amount, mid, unno,merchantCode,"no_credit".equals(limit_pay)?"no_credit":null,qrtid);

		int ifiid = fiid.intValue();
		return wechatService.getJsPayInfo(ifiid, orderid, openid,isCredit);

	}
	public void insertQBpay(String unno, String mid, String orderid, String subject, String payway, BigDecimal amount,
			String tid, String hybRate, String hybType) {
		if (orderid == null || "".equals(orderid)
				|| !orderid.startsWith(unno) || orderid.length()>32) {
			throw new HrtBusinessException(9005);
		}
		try {
			manageService.addDayMerAmt(mid,amount.doubleValue() );
			String sql = "insert into pg_wechat_txn (pwid,fiid, txntype, "+
					"mer_orderid, detail, txnamt, mer_id, bankmid, respcode, respmsg, "
					+ "status, cdate, lmdate,qrcode,unno,bk_orderid,mer_tid,trantype,hybtype,hybrate) values"
					+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,?,'1',sysdate,sysdate,?,?,?,?,?,?,?)";
			dao.update(sql,36,orderid,subject,amount,mid,"","00","SUCCESS","",unno,"",tid,"7",hybType,hybRate);
		}catch (BusinessException e1) {
			throw new HrtBusinessException(8000,"单日超限额，不允许交易！");
		}catch (Exception e) {
			logger.error("立码富钱包支付插入订单异常：",e);
			throw new HrtBusinessException(8000,"订单插入失败！");
		}
	}
	
	public void queryOrderTimeAndAmount(HrtPayXmlBean bean){
		List<Map<String, Object>> list = dao.queryForList("select lmdate,txnamt from pg_wechat_txn where status='1' and mer_orderid=?",
				bean.getOrderid());
		if(list.size()>0){
			bean.setAmount(String.valueOf(list.get(0).get("TXNAMT")));
			bean.setPayOrderTime(new SimpleDateFormat("yyyyMMddHHmmss").format(list.get(0).get("LMDATE")));
		}
	}
	
	public Map<String,String> beanToMap(Object bean) throws BusinessException{
		Map<String, String> map = null;
		try {
			map = BeanUtils.describe(bean);
			if(map.containsKey("class")){
				map.remove("class");
			}
		} catch (Exception e) {
			throw new BusinessException(9000, "报文格式转换异常");
		}
		return map;
	}
	public void checkChannelInfo2(HrtCmbcBean bean) throws BusinessException {
		
		String sb=bean.getMid()+"&"+bean.getAmount()+"&"+bean.getStrUuid();
		String sign=SimpleXmlUtil.getMd5SignByStr(sb);
		if(!sign.equals(bean.getSign().toUpperCase())){
			logger.info("校验签名失败:计算签名={},上传签名={}", sign, bean.getSign());
			throw new BusinessException(9004, "签名校验失败");
		}
	}
}
