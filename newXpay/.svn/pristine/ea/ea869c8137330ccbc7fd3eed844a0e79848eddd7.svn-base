package com.hrtpayment.xpay.common.controller;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.common.service.impl.AliJspayService;
import com.hrtpayment.xpay.utils.exception.BusinessException;

/**
 * 公众号支付统一入口(授权与支付页面)
 * 
 * @author aibing 2016年11月22日
 */
@Controller
@RequestMapping("xpay")
public class AliJsPayController {
	Logger logger = LogManager.getLogger();

	@Autowired
	AliJspayService alipay;

 
	/**
	 * 支付宝JSAPI支付页面(空白,只用来调起支付或提示错误)
	 * 
	 * @param fiid
	 * @param orderid
	 * @param code
	 * @return
	 */
	@RequestMapping("alipay_{fiid:[0-9]+}_{orderid}")
	@ResponseBody
	public String pay(@PathVariable int fiid, @PathVariable String orderid, @RequestParam String auth_code) {
		logger.info("获取支付宝code:" + auth_code + " 订单号：" + orderid);
		// 获取支付宝code:685c79f68dab4b388152b85bf290XD29 订单号：hrt201805281659544835799699
	
		StringBuilder sb = new StringBuilder(); 
		 
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"> ");
		sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"> ");
		sb.append("<title>支付</title> ");
		sb.append("<script src=\"http://libs.baidu.com/jquery/1.9.0/jquery.js\"></script> ");
		sb.append("<script type=\"application/javascript\">");
		try {
			Map<String, String> usrInfo= alipay.getOpenid(fiid, auth_code,orderid);
			String opendId=usrInfo.get("authcode");
			String userid=usrInfo.get("userid");
			String tradeNO=alipay.getJsPayInfo(fiid, orderid, opendId,userid);
			sb.append("$(function(){tradePay('"+tradeNO+"');");
			sb.append("$(\"#payButton\").click(function() { tradePay('"+tradeNO+"');});"); 
			sb.append(" $(\"#closeButton\").click(function() {AlipayJSBridge.call('closeWebview');});");
			sb.append("});");
			sb.append("function ready(callback) {");
			sb.append("if (window.AlipayJSBridge) {callback && callback();} ");
			sb.append("else {document.addEventListener('AlipayJSBridgeReady', callback, false);}}");
			sb.append("function tradePay(tradeNO) {ready(function(){");
			sb.append("AlipayJSBridge.call(\"tradePay\", {tradeNO: tradeNO}, ");
			sb.append("function (data) { if (\"9000\" == data.resultCode) {");
			if("88000020".equals(orderid.substring(0,8))){
				sb.append("window.location.href=\"https://xpay.hybunion.cn/HYBComboPayment/successPageRedirect.do?orderNo="+orderid+"\"}");
			}else if("96207320".equals(orderid.substring(0,8))){
				sb.append("window.location.href=\"https://xpay.yrmpay.com/YRMComboPayment/successPageRedirect.do?orderNo="+orderid+"\"}");
			}else{
				sb.append("AlipayJSBridge.call('closeWebview');}");
			}
			sb.append( "else if(\"8000\" == data.resultCode){ alert(\"订单支付中，请在支付订单内查询订单。\");AlipayJSBridge.call('closeWebview');}"
					+ "else {alert(\"支付失败，请重新下单。\");AlipayJSBridge.call('closeWebview');}"
					+ " });});}");
			sb.append("</script>");
		} catch (BusinessException e) {
			sb.append(String.format("alert('%s')",e.getMessage()));
		}
		sb.append("</head></body></html> ");
		
		return sb.toString();
	}

//	/**
//	 * 支付错误页面
//	 * 
//	 * @param msg
//	 * @return
//	 */
//	@RequestMapping("alierr")
//	@ResponseBody
//	public String err(@RequestParam String msg) {
//		StringBuilder sb = new StringBuilder();
//		sb.append(
//				"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
//		sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
//		sb.append("<title>支付宝支付</title><script type=\"text/javascript\">");
//
//		sb.append(String.format("alert('%s')", UrlCodec.decodeWithUtf8(msg)));
//
//		sb.append("</script></head></body></html>");
//		return sb.toString();
//	}
}
