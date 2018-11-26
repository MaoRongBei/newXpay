package com.hrtpayment.xpay.common.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.impl.WechatService;
import com.hrtpayment.xpay.utils.UrlCodec;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;
/**
 * 公众号支付统一入口(授权与支付页面)
 * @author 
 * 2016年11月22日
 */
@Controller
@RequestMapping("xpay")
public class WechatController {
	Logger logger = LogManager.getLogger();
	
	@Autowired
	WechatService wechat;
	
	@Autowired
	private JdbcDao dao;
	
	/**
	 * 公众号支付入口
	 * 重定向到微信,授权后重定向到pay(wxpay_xxx)接口进行支付
	 * @param fiid
	 * @param orderid
	 * @return
	 */
	@RequestMapping("wxauth_{fiid:[0-9]+}_{orderid}")
	public String auth(@PathVariable int fiid,@PathVariable String orderid) {
		try{
			return String.format("redirect:%s", wechat.getWxAuthUrl(fiid, orderid));
		}catch(HrtBusinessException e) {
			return String.format("redirect:wxerr?msg=%s_%s",e.getCode(), UrlCodec.encodeWithUtf8(e.getMessage()));
		}catch(Exception e) {
			return String.format("redirect:wxerr?msg=%s", UrlCodec.encodeWithUtf8(e.getMessage()));
		}
	}
	/**
	 * 公众号支付页面(空白,只用来调起支付或提示错误)
	 * @param fiid
	 * @param orderid
	 * @param code
	 * @return
	 */
	@RequestMapping("wxpay_{fiid:[0-9]+}_{orderid}")
	@ResponseBody
	public String pay(@PathVariable int fiid,@PathVariable String orderid,@RequestParam String code) {
		logger.info("获取微信code:"+code+" 订单号："+orderid);
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
		sb.append("<title>微信支付</title><script type=\"text/javascript\">");
		
		try{
			String openid = wechat.getOpenid(fiid, code);
			logger.info("获取微信opeind完成-------->"+fiid+"订单号："+orderid+" openid:"+openid);
			String payinfo = wechat.getJsPayInfo(fiid,orderid,openid);
			sb.append("var onBridgeReady = function() {	WeixinJSBridge.invoke('getBrandWCPayRequest',JSON.parse('");
			sb.append(payinfo);
			sb.append("'),function(res) {   if (res.err_msg == \"get_brand_wcpay_request：ok\"	|| res.err_msg == \"get_brand_wcpay_request:ok\")");
			/**
			 * 880000 立码富交易跳转到他们的指定页面上 	
			 */
			if (!"88000020".equals(orderid.substring(0,8))&&!"96207320".equals(orderid.substring(0,8))) {//880000
				sb.append("{WeixinJSBridge.call('closeWindow');}  else {	alert('支付失败，请重新支付！'); WeixinJSBridge.call('closeWindow');	}	});	};");
			}else{
				if("88000020".equals(orderid.substring(0,8))){
					sb.append("{window.location.href='https://xpay.hybunion.cn/HYBComboPayment/successPageRedirect.do?orderNo="+orderid+"'}  else {	alert('支付失败，请重新支付！'); WeixinJSBridge.call('closeWindow');	}	});	};");
				}else if("96207320".equals(orderid.substring(0,8))){
					sb.append("{window.location.href='https://xpay.yrmpay.com/YRMComboPayment/successPageRedirect.do?orderNo="+orderid+"'}  else {	alert('支付失败，请重新支付！'); WeixinJSBridge.call('closeWindow');	}	});	};");
				}
			}
			sb.append("if (typeof WeixinJSBridge == \"undefined\") {if (document.addEventListener)");
			sb.append("{document.addEventListener('WeixinJSBridgeReady', onBridgeReady,	false);	}");
			sb.append("else if (document.attachEvent) {	document.attachEvent('WeixinJSBridgeReady',onBridgeReady);");
			sb.append("	document.attachEvent('onWeixinJSBridgeReady', onBridgeReady);}} else {onBridgeReady();}");
		} catch(Exception e) {
			sb.append(String.format("alert('%s')",e.getMessage()));
		}
		
		sb.append("</script></head></body></html>");
		return sb.toString();
	}
	/**
	 * 公众号支付错误页面
	 * @param msg
	 * @return
	 */
	@RequestMapping("wxerr")
	@ResponseBody
	public String err(@RequestParam String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
		sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
		sb.append("<title>微信支付</title><script type=\"text/javascript\">");

		sb.append(String.format("alert('%s')",UrlCodec.decodeWithUtf8(msg)));
		
		sb.append("</script></head></body></html>");
		return sb.toString();
	}
}
