package com.hrtpayment.xpay.common.controller;

import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.cmbc.bean.json.HrtCmbcBean;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.common.service.impl.XpayService;
import com.hrtpayment.xpay.utils.exception.BusinessException;
/**
 * 和融通二维码支付(兼容早期www.hybunion.com的二维码,优先走公众号支付,没有最低金额限制)
 * 扫描和融通二维码,输入金额,根据浏览器判断走微信还是支付宝
 * @author 
 * 2016年11月18日
 */
@Controller
@RequestMapping("xpay")
public class JspayController {
	Logger logger = LogManager.getLogger();
	@Autowired
	XpayService service;
	@Autowired
	MerchantService merService;	
	@Autowired
	ManageService manageService;

	/**
	 * 和融通二维码入口
	 * 兼容早期www.hybunion.com的二维码,优先走公众号支付(页面下单时payway字段传入WXPAY)
	 * @param mid 
	 * @param request
	 * @return
	 */
	@RequestMapping("hrtjspay")
	public String pubacc(@RequestParam String mid, HttpServletRequest request) {
		String userAgent = request.getHeader("user-agent");
		if (userAgent.indexOf("MicroMessenger")>-1||userAgent.indexOf("micromessenger")>-1
				|| userAgent.indexOf("Alipay")>-1 || userAgent.indexOf("alipay")>-1
				|| userAgent.indexOf("QQ")>-1 || userAgent.indexOf("qq")>-1
				|| userAgent.toUpperCase().indexOf("JDJR")>-1 || userAgent.toUpperCase().indexOf("JDAPP")>-1|| userAgent.toUpperCase().indexOf("WALLETCLIENT")>-1 
				|| userAgent.indexOf("BaiduWallet")>-1 || userAgent.indexOf("baiduwallet")>-1){
			return "jspay.html";
		} else { 
			logger.info(userAgent);
			return "/error";
		}
	}

	/**
	 * 和融通二维码支付下单
	 * 数据金额后,点击确定支付,获取支付链接或二维码
	 * hrtjspay页面下单接口,无最小金额限制
	 * @param bean
	 * @param request
	 * @return
	 */
	@RequestMapping("getjspayurl")
	@ResponseBody
	public HrtCmbcBean getJsPayUrl(@RequestBody HrtCmbcBean bean, HttpServletRequest request) {
		bean.setStatus("1");
		if (bean.getAmount()==null) {
			bean.setMsg("金额不能为空");
			return bean;
		}
		if (bean.getAmount().scale()>2) {
			bean.setMsg("金额最多只能保留两位小数");
			return bean;
		}
		if (bean.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			bean.setMsg("金额必须大于0");
			return bean;
		}
		try {
			manageService.addDayMerAmt(bean.getMid(),bean.getAmount().doubleValue() );
		} catch (BusinessException e) {
			bean.setMsg(e.getMessage());
			return bean;
		}
		if (bean.getSubject()==null) {
			String merName = merService.queryMerName(bean.getMid());
			bean.setSubject(merName);
		}
		String userAgent = request.getHeader("user-agent");
		if (userAgent.indexOf("MicroMessenger")>-1) {
			bean.setPayway("WXPAY");
		} else if(userAgent.indexOf("Alipay")>-1) {
			bean.setPayway("ZFBZF");
//			bean.setPayway("ALIPAY");
		} else if(userAgent.indexOf("QQ")>-1) {
			bean.setPayway("QQZF");
		} else if( userAgent.toUpperCase().indexOf("JDJR")>-1||userAgent.toUpperCase().indexOf("JDAPP")>-1||userAgent.toUpperCase().indexOf("WALLETCLIENT")>-1) {
			bean.setPayway("JDZF");
		} else if(userAgent.indexOf("BaiduWallet")>-1) {

			bean.setPayway("BDQB");
		} else {
			return bean;
		}
		
		try {
			String QrCode = service.getPayUrl(null, bean.getMid(), bean.getAmount(), bean.getSubject(),  bean.getPayway(),bean.getQrtid());
			bean.setQrCodeUrl(QrCode);
			bean.setStatus("0");
			return bean;
		} catch (Exception e) {
			logger.info("{}公众号下单失败:{}",bean.getMid(),e.getMessage());
			bean.setMsg(e.getMessage());
			return bean;
		}
	}
}
