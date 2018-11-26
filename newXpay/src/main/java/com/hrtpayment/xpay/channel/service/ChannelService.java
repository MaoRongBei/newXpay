package com.hrtpayment.xpay.channel.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
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
import com.hrtpayment.xpay.common.service.impl.AliJspayService;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.common.service.impl.WechatService;
import com.hrtpayment.xpay.common.service.impl.XpayService;
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.cupsAT.service.CupsATPayService;
import com.hrtpayment.xpay.netCups.service.NetCupsPayService;
import com.hrtpayment.xpay.quickpay.cups.service.CupsQuickPayService;
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
//	public String checkChannelInfo(QuickPayBean bean) throws BusinessException {
//		String unno = bean.getUnno();
//		if (unno == null || "".equals(unno)) {
//			throw new BusinessException(9002, "unno错误");
//		}
//		String sql = "select * from HRT_XPAYORGINFO where unno=?";
//		List<Map<String, Object>> list = dao.queryForList(sql, unno);
//		if (list.size() < 1) {
//			throw new BusinessException(9006, "未找到unno对应的签名密钥");
//		}
//		String status = (String) list.get(0).get("STATUS");
//		if (!"1".equals(status)) {
//			logger.info("unno:{},状态不可用:{}", unno, status);
//			throw new BusinessException(9002, "unno错误");
//		}
//		String key = (String) list.get(0).get("MACKEY");
//		if (key == null || "".equals(key)) {
//			logger.info("unno:{},密钥错误:{}", unno, key);
//			throw new BusinessException(9006, "未找到unno对应的签名密钥");
//		}
//		String sign = bean.calSign(key);
//		if (!sign.equals(bean.getSign())) {
//			logger.info("校验签名失败:计算签名={},上传签名={}", sign, bean.getSign());
//			throw new BusinessException(9004, "签名校验失败");
//		}
//		if (bean.getOrderId() == null || !bean.getOrderId().startsWith(unno)) {
//			throw new BusinessException(9005, "订单号格式错误");
//		}
//		return key;
//	}
	
	public void queryOrder(HrtPayXmlBean bean) {
		bean.setStatus("R");
		List<Map<String, Object>> list = dao.queryForList("select w.*,bm.mch_id,bm.channel_id from pg_wechat_txn w,bank_merregister bm where  bm.merchantcode=w.bankmid and bm.fiid=w.fiid and mer_orderid=?",
				bean.getOrderid());
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
		if ("1".equals(dbstatus)) {
			logger.info("订单状态已经为成功");
			bean.setErrdesc("SUCCESS");
			if("110000".equals(bean.getUnno())){
				bean.setPayOrderTime(new SimpleDateFormat("yyyyMMddHHmmss").format(list.get(0).get("LMDATE")));
				bean.setAmount(String.valueOf(list.get(0).get("TXNAMT")));
			}else if("880000".equals(bean.getUnno())||"962073".equals(bean.getUnno())){
				bean.setRtnMsg(list.get(0).get("respcode")==null?"":String.valueOf(list.get(0).get("respcode")));
				bean.setRtnMsg(list.get(0).get("respmsg")==null?"":String.valueOf(list.get(0).get("respmsg")));
				bean.setBankType(list.get(0).get("paytype")==null?"":String.valueOf(list.get(0).get("paytype")));
				bean.setUserId(list.get(0).get("userid")==null?"":String.valueOf(list.get(0).get("userid")));
			}
			bean.setStatus("S");
			return;
		}else if("0".equals(dbstatus) && !"110000".equals(bean.getUnno())
				&& !"880000".equals(bean.getUnno())
				&& !"962073".equals(bean.getUnno())
				//立码富测试使用  不提交生产
				//=================================
//				&& !"111000".equals(bean.getUnno())
				//=================================
				&& !"112052".equals(bean.getUnno())){
			logger.info("交易状态未知");
			bean.setErrdesc("交易状态未知");
			bean.setStatus("R");
			return;
		}else if("6".equals(dbstatus)){
			logger.info("交易失败");
			bean.setErrdesc("交易失败");
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
			} catch (BusinessException e) {
				bean.setStatus("E");
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
			} catch (BusinessException e) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("交易状态未知");
			} 
		} else if (ifiid == 60) { 
			try {
				String tradeState =netCupsPayService.cupsWxQuery(bean,map);
				if ("SUCCESS".equals(tradeState)){
					bean.setStatus("S");
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
			} catch (BusinessException e) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("交易状态未知");
			}
		} else if (ifiid == 61) { 
			try {
				String tradeState =netCupsPayService.cupsAliQuery(bean,map);
				if ("SUCCESS".equals(tradeState)){
					bean.setStatus("S");
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
			} catch (BusinessException e) {
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("交易状态未知");
			}
		}else {
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
		//商品名为空则取商户名
		if (subject == null || "".equals(subject)) {
			subject = merService.queryMerName(mid);
		}
//		Map<String,Object> paywayMap = queryPayway(unno,mid);
		Map<String,Object> paywayMap = cmbcPay.getMerchantCode3(unno, mid, "WXPAY",amount);
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
				+ "mer_orderid, detail, txnamt, mer_id,unno,bankmid,bank_type,mer_tid,trantype,hybtype,hybrate) values"
				+ "(S_PG_Wechat_Txn.nextval,?,'0',sysdate,'A',?,?,?,?,?,?,?,?,'1',?,?)";
		dao.update(sql, fiid, orderid, subject, amount, mid, unno,merchantCode,"no_credit".equals(limit_pay)?"no_credit":null,
				qrtid,hybType,hybRate);
		
		String bankAppid =String.valueOf(paywayMap.get("APPID"));
		int ifiid = fiid.intValue();
		if((bean.getAppid()==null || "".equals(bean.getAppid())) 
				|| (bean.getOpenid()==null || "".equals(bean.getOpenid()))){
			String url =wechatService.getPubaccPayUrl(ifiid, orderid,orgcode);
			bean.setQrcode(url);
			bean.setStatus("S");
			return "";
		}else{
			if(bankAppid.equals(bean.getAppid())){
				String payinfo =wechatService.getJsPayInfo(ifiid, orderid, bean.getOpenid());
				bean.setPayinfo(payinfo);
				bean.setStatus("S");
				return "";
			}else{
				String url =wechatService.getPubaccPayUrl(ifiid, orderid,orgcode);
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
		Map<String,Object> paywayMap = cmbcPay.getMerchantCode3(unno, mid, "ZFBZF",amount);
		BigDecimal fiid = (BigDecimal) paywayMap.get("FIID");
		xpayService.checkBankTxnLimit(fiid.intValue(),amount,"ZFBZF");
		Object merchantCode = paywayMap.get("MERCHANTCODE");
		String orgcode = String.valueOf(paywayMap.get("CHANNEL_ID"));
		//插入订单信息
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?", orderid);
		if (list.size()>0) {
			throw new HrtBusinessException(9007);
		}
		String insertPaySql=" insert into pg_wechat_txn (pwid,txntype,trantype,cdate,lmdate,status,txnamt,detail"
				+ ",fiid,mer_orderid,mer_id,bankmid,hybtype,hybrate,unno,mer_tid)"
				+ " values (S_PG_Wechat_Txn.nextval,0,2,sysdate,sysdate,'A',?,?,?,?,?,?,?,?,?,?) ";
		dao.update(insertPaySql,amount,subject, fiid,orderid,mid,merchantCode,hybType,hybRate,unno,qrtid);
		int ifiid = fiid.intValue();
//		String url =alipayService.getPubaccPayUrl(ifiid, orderid,orgcode);
		String url =alipayService.getPubaccPayUrl(ifiid, orderid,orgcode);
		logger.info("支付宝公众号支付URL："+url);
		bean.setQrcode(url);
		bean.setStatus("S");
		return "";
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
	public String insertOrderByOpenid(String unno,String mid,String orderid,String subject,BigDecimal amount
			,String limit_pay,String qrtid,String openid) throws BusinessException {
		//商品名为空则取商户名
		if (subject == null || "".equals(subject)) {
			subject = merService.queryMerName(mid);
		}
		Map<String,Object> paywayMap = cmbcPay.getMerchantCode3(unno, mid, "WXPAY",amount);
		BigDecimal fiid = (BigDecimal) paywayMap.get("FIID");
		Object merchantCode = paywayMap.get("MERCHANTCODE");
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
		return wechatService.getJsPayInfo(ifiid, orderid, openid);

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
	
	public Map<String,String> beanToMap(HrtPayXmlBean bean) throws BusinessException{
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
