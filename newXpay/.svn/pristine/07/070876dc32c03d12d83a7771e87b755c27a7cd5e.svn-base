package com.hrtpayment.xpay.common.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.cmbc.bean.json.HrtCmbcBean;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.common.service.impl.XpayService;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

@Controller
@RequestMapping("xpay")
public class XPayController {
	Logger logger = LogManager.getLogger();
	
	@Autowired
	XpayService service;
	@Autowired
	MerchantService mer;
	@Autowired
	MerchantService merService;
	@Autowired
	ManageService manageService;


	

	@RequestMapping("queryorder")
	@ResponseBody
	public String queryOrder(@RequestBody HrtCmbcBean bean) {
		String orderid = bean.getOrderid();
		try {
			String status =	manageService.queryOrder(orderid);
			logger.info("[订单查询]订单号{}，状态{}",orderid,status);
			return status;
		} catch (HrtBusinessException e) {
			return e.getMessage();
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	/**
	 * 由于微信在安卓手机会发生重定向两次 ，增加一个中转页面
	 * @param orderid
	 * @param request
	 * @param resp
	 * @return
	 */
	@RequestMapping(value="posgetpayurlaaa_{orderid}")
	public String redirect(@PathVariable String orderid, HttpServletRequest request, HttpServletResponse resp) {
		logger.info("pos跳转页面orderid：{}", orderid);
		return "redirect:posredirect.html?orderid=" + orderid;
	}
	
	/**
	 * 和融通pos入口
	 * @param orderid 
	 * @param request
	 * @return
	 */
	@RequestMapping(value="posgetpayurlxxx_{orderid}")
	@ResponseBody
	public String auth(@PathVariable String orderid, HttpServletRequest request,HttpServletResponse resp) {
		String userAgent = request.getHeader("user-agent");
		logger.info(userAgent);
		try {
			if (userAgent.indexOf("MicroMessenger") > -1 || userAgent.indexOf("micromessenger") > -1) {
				logger.info("微信");
				String qrCode = service.getPosPayUrl("WXPAY", orderid);
				logger.info(qrCode);
				return qrCode;
			} else if (userAgent.indexOf("Alipay") > -1 || userAgent.indexOf("alipay") > -1) {
				logger.info("支付宝");
				String qrCode = service.getPosPayUrl("ZFBZF", orderid);
				logger.info(qrCode);
				return qrCode;
			} else if (userAgent.toUpperCase().indexOf("QQ") > -1 ) {
				logger.info("QQ");
				String qrCode = service.getPosPayUrl("QQZF", orderid);
				logger.info(qrCode);
				return qrCode;
//			} else if( userAgent.toUpperCase().indexOf("JDJR")>-1 ||userAgent.toUpperCase().indexOf("JDAPP")>-1||userAgent.toUpperCase().indexOf("WALLETCLIENT")>-1) {
//				logger.info("京东");
//				String qrCode = service.getPosPayUrl("JDZF", orderid);
//				String url = String.format("redirect:%s",qrCode);
//				logger.info(url);
			} else if (userAgent.toUpperCase().indexOf("BAIDUWALLET") > -1 ) {
				logger.info("BaiduWallet");
				String qrCode = service.getPosPayUrl("BDQB", orderid);
				logger.info(qrCode);
				return qrCode;
			} else {
				logger.info("不支持的扫码设备,orderid:{}",orderid);
				return "不支持的扫码设备,请用微信或支付宝扫码";
			}
		} catch (Exception e) {
//			logger.info("---------------------->{}",e.getMessage());
//			resp.setContentType("application/json;charset=UTF-8");
//            try {
//				PrintWriter writer = resp.getWriter();
//				writer.write(e.getMessage());
//                writer.flush();
//                writer.close();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
			logger.info("{},{}", orderid, e.getMessage());
			return "支付错误："+e.getMessage();
		}
	}
}
