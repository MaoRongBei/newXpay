package com.hrtpayment.xpay.common.controller;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.channel.service.ChannelService;
import com.hrtpayment.xpay.channel.service.SmzfService;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.cups.service.CupsPayService;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;
/**
 * 通道商接入接口
 * @author aibing
 * 2016年11月18日
 */
@Controller
@RequestMapping("xpay")
public class ChannelController {
	Logger logger = LogManager.getLogger();
	@Autowired
	ChannelService ch;
	@Autowired
	SmzfService smzf;
	@Autowired
	ManageService manageService;
	@Autowired
	CupsPayService cupsPayService;

	/**
	 * 二维码
	 * @param xml
	 * @return
	 */
	@RequestMapping("hrtpay")
	@ResponseBody
	public HrtPayXmlBean hrtpay(@RequestBody HrtPayXmlBean bean) {
		
		String payway = bean.getPayway();
		String unno = bean.getUnno();
		BigDecimal amount = null;
		try{
			amount = new BigDecimal(bean.getAmount());
			if (amount.scale()>2) throw new Exception("金额精度超过2");
			if (amount.compareTo(BigDecimal.ZERO)<=0) throw new Exception("交易金额非法");
		} catch (Exception e) {
			logger.info("金额错误:",bean.getAmount());
			bean.setStatus("E");
			bean.setErrcode("9001");
			bean.setErrdesc("金额格式错误");
			return bean;
		}

		
		String key = null;
		try {
			key = ch.checkChannelInfo(bean);
			/**
			 * 机构日限额
			 */
//			manageService.addDayUnnoAmt(unno,amount.doubleValue() );
			/**
			 * 单笔，单户单日限额
			 */
 
			if(!"QBPAY".equals(payway)&&!"880000".equals(unno)&&!"962073".equals(unno)){
				manageService.addDayMerAmt(bean.getMid(),amount.doubleValue() );
			}
			if ("WXPAY".equals(payway)) {
					ch.insertGzhOrder(bean,"", amount,bean.getHybRate(),bean.getHybType());
					logger.info("{}:通道商微信公众号下单成功",bean.getOrderid());
			}else if("ZFBZF".equals(payway)&& "1".equals(bean.getIsAliSucPage())  ){
				ch.insertJsapiOrder(bean,"", amount,bean.getHybRate(),bean.getHybType());
				logger.info("{}:通道商支付宝公众号下单成功",bean.getOrderid());
			} else if ("WXZF".equals(payway) || "ZFBZF".equals(payway)|| "QQZF".equals(payway)|| "BDQB".equals(payway)||"JDZF".equals(payway)) {
				String qrcode = smzf.pay(unno, bean.getMid(), bean.getOrderid(), bean.getSubject(), payway, amount,bean.getTid(),bean.getHybRate(),bean.getHybType());
				bean.setQrcode(qrcode);
				bean.setStatus("S");
				logger.info("{}:通道商扫码支付下单成功",bean.getOrderid());
			}else {
				bean.setStatus("E");
				bean.setErrcode("9000");
				bean.setErrdesc("payway错误");
				logger.info("{}:hrtpay payway错误",bean.getOrderid());
			}
		} catch (HrtBusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (BusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (Exception e) {
			logger.error("系统异常",e);
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("交易失败");
		}
		if(key!=null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (BusinessException e) {
				logger.error("响应签名异常",e);
			}
		}
		return bean;
	}

	@RequestMapping("barcodepay")
	@ResponseBody
	public HrtPayXmlBean barcodepay(@RequestBody HrtPayXmlBean bean) {
		logger.info("通道商条码支付:{}",bean.getOrderid());
		String key = null;
		try {
			key = ch.checkChannelInfo(bean);
		} catch (BusinessException e) {
			logger.info(e.getMessage());
			bean.setStatus("E");
			bean.setErrdesc(e.getMessage());
			return bean;
		}
		BigDecimal amount = null;
		try{
			amount = new BigDecimal(bean.getAmount());
			if (amount.scale()>2) throw new Exception("金额精度超过2");
			if (amount.compareTo(BigDecimal.ZERO)<=0) throw new Exception("交易金额非法");
		} catch (Exception e) {
			logger.info("金额错误:",bean.getAmount());
			bean.setStatus("E");
			bean.setErrcode("9001");
			bean.setErrdesc("金额格式错误");
			return bean;
		}

		try {
			/**
			 * 单笔，单户单日限额
			 **
             * 立码富钱包限额 方式 
             * 先判断 成功后累增   
             * 可能存在的问题    边缘值 限制的不是很清楚
             * 
             * 其它方式交易  
             * 判断并累增 成功失败的交易额度 都会算在 日限额内
             * 
             */
			if(!"880000".equals(bean.getUnno())&&!"962073".equals(bean.getUnno())){
				manageService.addDayMerAmt(bean.getMid(),amount.doubleValue() );
			}
			bean.setPayOrderTime(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
			String status = smzf.barcodePay(bean);
			bean.setStatus(status);
			logger.info("[条码支付]机构号{}，订单号：{}，返回状态status={}",bean.getUnno(),bean.getOrderid(),bean.getStatus());
			if(("S".equals(status)&& "880000".equals(bean.getUnno()))||("S".equals(status)&& "962073".equals(bean.getUnno()))){
				manageService.addDayMerAmtForLMF(bean.getMid(),amount.doubleValue());
			}
			if ("E".equals(status)) {
				bean.setErrcode("8000");
				bean.setErrdesc("交易失败");
			}
		} catch (BusinessException e) {
			bean.setStatus("E");
			bean.setErrcode(e.getErrorCode());
			bean.setErrdesc(e.getMessage());
		}
		if(key!=null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (BusinessException e) {
				logger.error("响应签名异常",e);
			}
		}
		return bean;
	}
	@RequestMapping("hrtqueryorder")
	@ResponseBody
	public HrtPayXmlBean queryorder(@RequestBody HrtPayXmlBean bean) {
		logger.info("[通道查询订单]订单号：{}",bean.getOrderid());
		String key = null;
		try {
			key = ch.checkChannelInfo(bean);
			ch.queryOrder(bean);
		} catch (BusinessException e) {
			logger.info(e.getMessage());
			bean.setStatus("E");
			bean.setErrdesc(e.getMessage());
			return bean;
		}
		if(key!=null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (BusinessException e) {
				logger.error("响应签名异常",e);
			}
		}
		logger.info("[通道查询订单]订单号：{}返回：{}", bean.getOrderid(), bean.getStatus());
		return bean;
	}
	
	/**
	 * 银联二维码(银行卡前置模式)主扫模式
	 * @param xml
	 * @return
	 */
	@RequestMapping("cupszspay")
	@ResponseBody
	public HrtPayXmlBean cupsQrZspay(@RequestBody String xml) {
		Map<String, String> map = SimpleXmlUtil.xml2map(xml);
		String unno = map.get("unno");
		String mid = map.get("mid");
		String tid=map.get("tid");
		String orderid = map.get("orderid");
		
		
		HrtPayXmlBean bean = new HrtPayXmlBean();
		bean.setUnno(unno);
		bean.setMid(mid);
		bean.setTid(tid);
		bean.setOrderid(orderid);
		BigDecimal amount = null;
		try{
			amount = new BigDecimal(map.get("amount"));
			bean.setAmount(map.get("amount"));
			if (amount.scale()>2) throw new Exception("金额精度超过2");
			if (amount.compareTo(BigDecimal.ZERO)<=0) throw new Exception("交易金额非法");
		} catch (Exception e) {
			logger.info("金额错误:",map.get("amount"));
			bean.setStatus("E");
			bean.setErrcode("9001");
			bean.setErrdesc("金额格式错误");
			return bean;
		}
		bean.setSign(map.get("sign"));
		bean.setDesc(map.get("desc"));
		
		String key = null;
		try {
			key = ch.checkChannelInfo(map);
			/**
			 * 银联二维码十分钟内相同商户相同金额不允许上送。
			 */
			boolean flag=cupsPayService.querCupsMidAmtLimit(unno,mid,amount);
			if(!flag){
				throw new BusinessException(9001, "相同金额收款太频繁，请间隔10分钟后再试！");
			}
			/**
			 * 单笔，单户单日限额
			 */
			manageService.addDayMerAmtForCups(bean.getMid(),amount.doubleValue() );
			String qrcode=cupsPayService.getCupsPayQrCode(bean);
			bean.setQrcode(qrcode);
			bean.setStatus("S");
			logger.info("{}:银联二维码支付下单成功",bean.getOrderid());
		} catch (HrtBusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (BusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (Exception e) {
			logger.error("系统异常",e);
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("交易失败");
		}
		if(key!=null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (BusinessException e) {
				logger.error("响应签名异常",e);
			}
		}
		return bean;
	}
	
	/**
	 * 银联二维码(银行卡前置模式)被扫模式
	 * @param xml
	 * @return
	 */
	@RequestMapping("cupsbspay")
	@ResponseBody
	public HrtPayXmlBean cupsQrBspay(@RequestBody String xml) {
		Map<String, String> map = SimpleXmlUtil.xml2map(xml);
		String unno = map.get("unno");
		String mid = map.get("mid");
		String tid=map.get("tid");
		String orderid = map.get("orderid");
		String qrcode=map.get("qrcode");
		
		
		HrtPayXmlBean bean = new HrtPayXmlBean();
		bean.setUnno(unno);
		bean.setMid(mid);
		bean.setTid(tid);
		bean.setOrderid(orderid);
		bean.setQrcode(qrcode);
		
		if("".equals(bean.getQrcode())|| null==bean.getQrcode()){
			logger.info("qrcode参数错误:",bean.getQrcode());
			bean.setStatus("E");
			bean.setErrcode("9000");
			bean.setErrdesc("qrcode格式错误");
			return bean;
		}
		BigDecimal amount = null;
		try{
			amount = new BigDecimal(map.get("amount"));
			bean.setAmount(map.get("amount"));
			if (amount.scale()>2) throw new Exception("金额精度超过2");
		} catch (Exception e) {
			logger.info("金额错误:",map.get("amount"));
			bean.setStatus("E");
			bean.setErrcode("9001");
			bean.setErrdesc("金额格式错误");
			return bean;
		}
		bean.setSign(map.get("sign"));
		bean.setDesc(map.get("desc"));
		
		String key = null;
		try {
			key = ch.checkChannelInfo(map);
			/**
			 * 银联二维码十分钟内相同商户相同金额不允许上送。
			 */
			boolean flag=cupsPayService.querCupsMidAmtLimit(unno,mid,amount);
			if(!flag){
				throw new BusinessException(9001, "相同金额收款太频繁，请间隔10分钟后再试！");
			}
			/**
			 * 单笔，单户单日限额
			 */
			//银联扫码不限额商户 ，归属和融通，有效期到2017-12-30
			if(!bean.getMid().equals("864001159480647")){
				manageService.addDayMerAmtForCups(bean.getMid(),amount.doubleValue() );
			}
			cupsPayService.cupsPay(bean);
		} catch (HrtBusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (BusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (Exception e) {
			logger.error("系统异常",e);
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("交易失败");
		}
		if(key!=null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (BusinessException e) {
				logger.error("响应签名异常",e);
			}
		}
		return bean;
	}
	
	/**
	 * 银联二维码(银行卡前置模式)被扫模式
	 * 针对绑卡消费，轮询组
	 * @param xml
	 * @return
	 */
	@RequestMapping("cupsQrBspayForCups")
	@ResponseBody
	public HrtPayXmlBean cupsQrBspayForCups(@RequestBody String xml) {
		Map<String, String> map = SimpleXmlUtil.xml2map(xml);
		String unno = map.get("unno");
		String mid = map.get("mid");
		String tid=map.get("tid");
		String orderid = map.get("orderid");
		String qrcode=map.get("qrcode");
		String groupName=map.get("groupName");
		String bankMid=map.get("bankMid");
		
		
		HrtPayXmlBean bean = new HrtPayXmlBean();
		bean.setUnno(unno);
		bean.setMid(mid);
		bean.setTid(tid);
		bean.setOrderid(orderid);
		bean.setQrcode(qrcode);
		bean.setGroupName(groupName);
		bean.setBankMid(bankMid);
		
		if("".equals(bean.getQrcode())|| null==bean.getQrcode()){
			logger.info("qrcode参数错误:",bean.getQrcode());
			bean.setStatus("E");
			bean.setErrcode("9000");
			bean.setErrdesc("qrcode格式错误");
			return bean;
		}
		BigDecimal amount = null;
		try {
			amount = new BigDecimal(map.get("amount"));
			bean.setAmount(map.get("amount"));
			if (amount.scale() > 2)
				throw new Exception("金额精度超过2");
			if (amount.compareTo(BigDecimal.ZERO)<=0) throw new Exception("交易金额非法");
		} catch (Exception e) {
			logger.info("金额错误:", map.get("amount"));
			bean.setStatus("E");
			bean.setErrcode("9001");
			bean.setErrdesc("金额格式错误");
			return bean;
		}
		bean.setSign(map.get("sign"));
		bean.setDesc(map.get("desc"));
		
		String key = null;
		try {
			key = ch.checkChannelInfo(map);
			cupsPayService.cupsPayNew(bean);
		} catch (HrtBusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (BusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (Exception e) {
			logger.error("系统异常",e);
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("交易失败");
		}
		if(key!=null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (BusinessException e) {
				logger.error("响应签名异常",e);
			}
		}
		return bean;
	}
	
	@RequestMapping("hrtwxpay")
	@ResponseBody
	public HrtPayXmlBean hrtwxpay(@RequestBody String xml) {
		Map<String, String> map = SimpleXmlUtil.xml2map(xml);
		String unno = map.get("unno");
		String mid = map.get("mid");
		String tid=map.get("tid");
		String orderid = map.get("orderid");
		String subject = map.get("subject");
		String limit_pay = map.get("limit_pay");
		String payway = map.get("payway");
		String openid = map.get("openid");
		
		if (openid == null || "".equals(openid)) {
			throw new HrtBusinessException(9000);
		}
		
		HrtPayXmlBean bean = new HrtPayXmlBean();
		bean.setUnno(unno);
		bean.setMid(mid);
		bean.setTid(tid);
		bean.setOrderid(orderid);
		bean.setSubject(subject);
		bean.setOpenid(openid);
		BigDecimal amount = null;
		try{
			amount = new BigDecimal(map.get("amount"));
			bean.setAmount(map.get("amount"));
			if (amount.scale()>2) throw new Exception("金额精度超过2");
			if (amount.compareTo(BigDecimal.ZERO)<=0) throw new Exception("交易金额非法");
		} catch (Exception e) {
			logger.info("金额错误:",map.get("amount"));
			bean.setStatus("E");
			bean.setErrcode("9001");
			bean.setErrdesc("金额格式错误");
			return bean;
		}
		bean.setSign(map.get("sign"));
		bean.setPayway(payway);
		bean.setDesc(map.get("desc"));
		
		String key = null;
		try {
			key = ch.checkChannelInfo(map);
//			/**
//			 * 机构日限额
//			 */
//			manageService.addDayUnnoAmt(unno,amount.doubleValue() );
			/**
			 * 单笔，单户单日限额
			 */
			manageService.addDayMerAmt(bean.getMid(),amount.doubleValue() );
			if ("WXPAY".equals(payway)) {
					String payinfo = ch.insertOrderByOpenid(unno,mid,orderid,subject,amount,limit_pay,tid,openid);
					bean.setPayinfo(payinfo);
					bean.setStatus("S");
					logger.info("{}:通道商公众号下单成功",bean.getOrderid());
			}else {
				bean.setStatus("E");
				bean.setErrcode("9000");
				bean.setErrdesc("payway错误");
				logger.info("{}:hrtpay payway错误",bean.getOrderid());
			}
		} catch (HrtBusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (BusinessException e) {
			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
			bean.setStatus("E");
			bean.setErrcode(e.getCode()+"");
			bean.setErrdesc(e.getMessage());
		} catch (Exception e) {
			logger.error("系统异常",e);
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("交易失败");
		}
		if(key!=null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (BusinessException e) {
				logger.error("响应签名异常",e);
			}
		}
		return bean;
	}
}
