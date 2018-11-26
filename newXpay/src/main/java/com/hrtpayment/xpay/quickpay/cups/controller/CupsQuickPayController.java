package com.hrtpayment.xpay.quickpay.cups.controller;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.channel.service.ChannelService;
import com.hrtpayment.xpay.quickpay.cups.service.CupsQuickPayService;

@Controller
@RequestMapping("xpay")
public class CupsQuickPayController {

	Logger logger = LogManager.getLogger();
	
	@Autowired
	CupsQuickPayService quickPayService;
	
	@Autowired
	ChannelService ch;
	
	
//	@RequestMapping("quickpay")
//	@ResponseBody
//	public HrtPayXmlBean cupsDkpay(@RequestBody HrtPayXmlBean bean) {
//		String key = null;
//		try {
////			key = ch.checkChannelInfo(bean);
//			String status=quickPayService.pay(bean.getOrderid(),bean.getAmount(),"13671375129","孙珊珊",
//					"6225768762710661","02","01","372924199010120046",bean.getMid(),bean.getTid(),bean.getUnno());
//			bean.setStatus(status);
//		} catch (HrtBusinessException e) {
//			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
//			bean.setStatus("E");
//			bean.setErrcode(e.getCode()+"");
//			bean.setErrdesc(e.getMessage());
//		}catch (BusinessException e) {
//			logger.info("{}:{}{}",bean.getOrderid(),e.getCode(),e.getMessage());
//			bean.setStatus("E");
//			bean.setErrcode(e.getCode()+"");
//			bean.setErrdesc(e.getMessage());
//		} 
//		catch (Exception e) {
//			logger.error("系统异常",e);
//			bean.setStatus("E");
//			bean.setErrcode("8000");
//			bean.setErrdesc("交易失败");
//		}
//		if (key!=null) bean.setSign(bean.calSign(key));
//		return bean;
//	}
	
	
	@RequestMapping("queryquickpay")
	@ResponseBody
	public String cupsQueryDkpay(@RequestParam String orderid,String orderTime) {
		String status = null;
		try {
			Map<String,Object > orderInfo = new HashMap<String,Object >();
			orderInfo.put("orderid", orderid);
			orderInfo.put("orderTime", orderTime);
			status=quickPayService.queryOrder(orderInfo);
		}catch (Exception e) {
			logger.error("系统异常",e);
			status="R";	
		}
		return status;
	}
	
	
	/**
	 * 快捷支付预签约
	 * 
	 * @param request
	 */
	@RequestMapping("/qkpay/presign")
	@ResponseBody
	public void cupPreSign(@RequestBody String formStr){
//		quickPayService.preSign();
	}
	
	
	/**
	 * 快捷支付签约
	 * 
	 * @param request
	 */
	@RequestMapping("/qkpay/sign")
	@ResponseBody
	public void cupSign(@RequestBody String formStr){
//		quickPayService.sign();
	}
	
	
	/**
	 * 快捷支付取消签约
	 * 
	 * @param request
	 */
	@RequestMapping("/qkpay/cancelsign")
	@ResponseBody
	public String cupCancelSign(@RequestParam String accname,String idNo,String cardNo){
		boolean flag =quickPayService.cancelSign(accname, idNo, null, cardNo);
		return String.valueOf(flag);
	}
	
	
	/**
	 * 接收北京银联代扣异步通知
	 * 
	 * @param request
	 */
	@RequestMapping("qkpaycallback")
	@ResponseBody
	public String cupCallBank(@RequestBody String formStr){
		 logger.info("接收到的消息:"+formStr);
		 quickPayService.updateStatusByNotify(formStr);
		 return "SUCCESS";
	}
	
}
