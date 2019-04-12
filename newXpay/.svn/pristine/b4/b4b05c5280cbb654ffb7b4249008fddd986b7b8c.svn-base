package com.hrtpayment.xpay.quickpay.common.controller;

import java.math.BigDecimal;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.channel.service.ChannelService;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.quickpay.common.bean.QuickPayBean;
import com.hrtpayment.xpay.quickpay.common.service.QuickpayService;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException; 

/**
 * 和融通快捷支付入口 APP传入参数有：卡号、mid、手机号、金额 根据规则判断走哪个快捷支付通道
 * 
 * @author songbeibei 2017-10-26
 */
@Controller
@RequestMapping("xpay")
public class HrtQuickPayController {

	Logger logger = LogManager.getLogger();

	@Autowired
	MerchantService merService;
	@Autowired
	ManageService manageService;
	@Autowired
	QuickpayService service;
	@Autowired
	ChannelService ch;
	
	
	@RequestMapping("getquickpayforchannel")
	@ResponseBody
	public QuickPayBean getPayUrlForChannel(@RequestBody String xml) {
		Map<String, String> map = SimpleXmlUtil.xml2map(xml);
		String isPoint = map.get("ispoint");
		String mid = map.get("mid");
		String unno=map.get("unno");
		String orderid =map.get("orderid"); 
		String qpcid =map.get("qpcid"); 
		
		QuickPayBean bean=new QuickPayBean(); 
		bean.setMid(mid); 
		bean.setIsPoint(isPoint); 
		bean.setUnno(unno);
		bean.setOrderId(orderid);
		bean.setQpcid(qpcid);
		bean.setStatus("1");
		try{
			bean.setAmount(String.valueOf(map.get("amount")));
			BigDecimal amount = new BigDecimal(map.get("amount"));
			
			if (amount.scale()>2) throw new Exception("金额精度超过2");
			
			if(amount.doubleValue()<100){
				bean.setRtnHtml("最低消费金额100元");
				return bean;
			}
		} catch (Exception e) {
			logger.info("金额错误:",map.get("amount"));
			bean.setRtnHtml("金额格式错误");
			return bean;
		}
		bean.setSign(map.get("sign"));
		String key = null;
		try {
			key = ch.checkChannelInfo(map);
			service.getQuickPayChannel(bean);
			
		} catch (Exception e) {
			logger.info("{}快捷支付失败:{}", bean.getMid(), e.getMessage());
			bean.setRtnHtml(e.getMessage());
//			return bean;
		}
		if (key!=null) bean.setSign(bean.calSign(key));
		return  bean;
		
	}

	/**
	 * 和融通二维码支付下单 数据金额后,点击确定支付,获取支付链接或二维码
	 * 
	 * @param bean
	 * @param request
	 * @return
	 */
	@RequestMapping("getquickpay")
	@ResponseBody
	public QuickPayBean getPayUrl(@RequestBody QuickPayBean bean,HttpServletRequest request) {	
		/**
		 * 获取金额、mid、accid、ispoint
		 */
		BigDecimal amount =BigDecimal.valueOf( Double.valueOf(bean.getAmount()));
		String isPoint = bean.getIsPoint();
		String mid = bean.getMid();
		String accid = bean.getQpcid();
		bean.setStatus("1");
		/**
		 * 1、校验数据 a、金额：金额不能为空且保留两位小数 b、卡号：判断卡号是否为空 c、商户号：判断商户号是否为空
		 * d、手机号：判断手机号是否为空
		 */
		if (amount == null || "".equals(amount)) {
			bean.setRtnHtml("金额不能为空");
			return bean;
		}
		if(amount.doubleValue()<100){
			bean.setRtnHtml("最低消费金额100元");
			return bean;
		}
		if (amount.scale() > 2) {
			bean.setRtnHtml("金额最多只能保留两位小数");
			return bean;
		}
		if (accid == null || "".equals(accid)) {
			bean.setRtnHtml("卡id不能为空");
			return bean;
		}
		if (isPoint == null || "".equals(isPoint)) {
			bean.setRtnHtml("积分标识不能为空");
			return bean;
		}
		if (mid == null || "".equals(mid)) {
			bean.setRtnHtml("商户号不能为空");
			return bean;
		}
		/**
		 * 2、 判断限额   是否超过日限额和单笔限额
		 *    不做金额累增
		 */
		try {
			manageService.checkDayMerAmtForQuickPay(bean.getMid(), amount.doubleValue());
		} catch (BusinessException e) {
			bean.setRtnHtml(e.getMessage());
			return bean;
		}
		try {
			// 测试使用，生产时app需要将两个栏位信息上送，
			// 并且经纬度分隔符不能是\ 做下转换
//			bean.setDeviceId("864000353110268");
//			bean.setPosition("116.317408-39.959688");
			service.getQuickPayChannel(bean);
			return  bean;
		} catch (Exception e) {
			logger.info("{}快捷支付失败:{}", bean.getMid(), e.getMessage());
			bean.setRtnHtml(e.getMessage());
			return bean;
		}
	}
	
	/**
	 * 获取短信验证码
	 * @param request
	 */
	@RequestMapping("getMessage")
	@ResponseBody 
	public QuickPayBean getMessage(HttpServletRequest request) {
		QuickPayBean bean =new QuickPayBean();
		try{
			String fiid =request.getParameter("fiid");
			String mid = request.getParameter("mid");
			String qpcid=request.getParameter("qpcid");
			String amount=request.getParameter("amount");
			String orderid=request.getParameter("orderid");
			String cvn=request.getParameter("cvn");
			String effective=request.getParameter("effective");
			
			
			bean.setFiid(fiid);
			bean.setMid(mid);
			bean.setQpcid(qpcid);
			bean.setAmount(amount);
			bean.setOrderId(orderid);
			bean.setCvn(cvn);
			bean.setEffective(effective);
			service.getMessage(bean);
			return  bean;
		}catch(Exception e){
			logger.info("{}快捷支付获取验证码失败:{}", bean.getMid(), e.getMessage());
			bean.setRtnHtml(e.getMessage());
			return bean;
		}
	}
	
	/**
	 * 安全认证 支付接口 适用通道：兴业、银联
	 * QuickPayBean
	 * @param bean
	 * @param request
	 * @return
	 */
	@RequestMapping("setpayinfo")
	@ResponseBody
	public QuickPayBean setPayInfo(HttpServletRequest request) {
		QuickPayBean bean =new QuickPayBean();
		try {
		String fiid =request.getParameter("fiid");
		String s_amount=request.getParameter("amount");
		BigDecimal amount = BigDecimal.valueOf(Double.parseDouble(s_amount));
		String mid = request.getParameter("mid");
		String accNo = request.getParameter("accNo");
		String bankName=request.getParameter("bankName");
		String isPoint = request.getParameter("isPoint");
		String qpcid= request.getParameter("qpcid");
		String cvn=request.getParameter("cvn");
		String effective=request.getParameter("effective");
		bean.setCvn(cvn);
		bean.setEffective(effective);
		String captcha="";
		if ("0".equals(request.getParameter("iscaptcha"))){
			captcha=request.getParameter("captcha");
		} 
		String preSerial= request.getParameter("preSerial");
		String smsCode= request.getParameter("smsCode");
		String unno=request.getParameter("unno");
		String orderid =request.getParameter("orderid");
		String bankMid =request.getParameter("bankMid");
		String deviceId =request.getParameter("deviceId");
		String position =request.getParameter("position");
		bean.setSmsCode(smsCode);
		bean.setPreSerial(preSerial);
		bean.setFiid(fiid);
		bean.setAmount(s_amount);
		bean.setBankName(bankName);
		bean.setMid(mid);
		bean.setAccNo(accNo);
		bean.setIsPoint(isPoint);
		bean.setQpcid(qpcid);
		bean.setUnno(unno);
		bean.setOrderId(orderid);
		bean.setBankmid(bankMid);
		bean.setStatus("1");
		bean.setDeviceId(deviceId);
		bean.setPosition(position);
		/**
		 * 1、校验数据 a、金额：金额不能为空且保留两位小数 b、卡号：判断卡号是否为空 c、商户号：判断商户号是否为空
		 * d、手机号：判断手机号是否为空
		 */
		if (unno == null || "".equals(unno)) {
			bean.setRtnHtml("机构号不能为空");
			return bean;
		}
		if (orderid == null || "".equals(orderid)) {
			bean.setRtnHtml("订单号不能为空");
			return bean;
		}
		if (amount == null || "".equals(amount)) {
			bean.setRtnHtml("金额不能为空");
			return bean;
		}
		if (amount.scale() > 2) {
			bean.setRtnHtml("金额最多只能保留两位小数");
			return bean;
		}
		if (accNo == null || "".equals(accNo)) {
			bean.setRtnHtml("卡号不能为空");
			return bean;
		}
		if (mid == null || "".equals(mid)) {
			bean.setRtnHtml("商户号不能为空");
			return bean;
		}
		if (fiid == null || "".equals(fiid)) {
			bean.setRtnHtml("fiid不能为空");
			return bean;
		}
//		if (cvn == null || "".equals(cvn)) {
//			bean.setRtnHtml("cvn不能为空");
//			return bean;
//		}
//		if (effective == null || "".equals(effective)) {
//			bean.setRtnHtml("有效期不能为空");
//			return bean;
//		}
			service.setPayInfo(bean,fiid, amount, mid, null, accNo, null,captcha);
			return bean;
		} catch (Exception e) {
			logger.info("{}快捷支付下单失败:{}", bean.getMid(), e.getMessage());
			bean.setHtnlStatus("3");
			bean.setMsg(e.getMessage());
			return bean;
		}

	}
}
