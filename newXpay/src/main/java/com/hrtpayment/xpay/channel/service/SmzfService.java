package com.hrtpayment.xpay.channel.service;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.baidu.service.BaiduPayService;
import com.hrtpayment.xpay.bcm.service.BcmPayService;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.cib.service.CibPayService;
import com.hrtpayment.xpay.cmbc.service.CmbcPayService;
import com.hrtpayment.xpay.common.service.impl.AliJspayService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.common.service.impl.XpayService;
import com.hrtpayment.xpay.cupsAT.service.CupsATPayService;
import com.hrtpayment.xpay.netCups.service.NetCupsPayService;
import com.hrtpayment.xpay.utils.exception.BusinessException;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

/**
 * 扫码支付接口对应service
 * 
 * @author aibing
 *
 */
@Service
public class SmzfService {
	private Logger logger = LogManager.getLogger();
	@Autowired
	CmbcPayService cmbcPay;
	@Autowired
	MerchantService merService;
	@Autowired
	XpayService xpayService;
	@Autowired
	AliJspayService alipayService;
	@Autowired
	BaiduPayService baiduPayService;
	@Autowired
	BcmPayService bcmPayService;
	@Autowired
	CupsATPayService cupsATPayService;
	@Autowired
	NetCupsPayService netCupsPayService;
	@Autowired
	CibPayService cibService;
	
	/**
	 * 支付通道商选择和判断
	 * @param bean
	 */
	public String pay(HrtPayXmlBean bean,String unno,String mid,String orderid,String subject,String payway,
			BigDecimal amount,String tid,String hybRate,String hybType) {
		if (orderid == null || "".equals(orderid)
				|| !orderid.startsWith(unno) || orderid.length()>32) {
			throw new HrtBusinessException(9005);
		}
		String qrcode;
		try {
			qrcode = getQrCode(bean,unno, mid, orderid, subject, payway, amount, tid,hybRate,hybType);
			return qrcode;
		} catch (BusinessException e) {
			throw new HrtBusinessException(Integer.valueOf(e.getErrorCode()),e.getMessage());
		}
	}

	
	/**
	 * 获取支付二维码
	 * @param unno
	 * @param mid
	 * @param orderid
	 * @param subject
	 * @param payway
	 * @param amount
	 * @return
	 * @throws BusinessException
	 */
	private String getQrCode(HrtPayXmlBean bean,String unno,String mid,String orderid,String subject,
			String payway,BigDecimal amount,String tid,String hybRate,String hybType) throws BusinessException {
		int fiid = 0;
		if ("WXZF".equals(payway) || "ZFBZF".equals(payway)||"QQZF".equals(payway)||"JDZF".equals(payway)|| "BDQB".equals(payway)) {
		}else{
			throw new HrtBusinessException(9010);
		}
		/*
		 * 2018-11-27 修改
		 * 
		 * 机构 j62077 根据定位获取交易地址area 
		 * 
		 */
		String area=bean.getArea(); //交易地点
		Map<String, Object> map = cmbcPay.getMerchantCode3(unno, mid, payway,amount,area,"ZS");
		if (subject == null || "".equals(subject)) {
			subject = String.valueOf(map.get("SHORTNAME"));
		}
		fiid =Integer.parseInt(String.valueOf(map.get("FIID")));
		xpayService.checkBankTxnLimit(fiid,amount,payway);
		String QrCode="";
		String isCredit= String.valueOf(map.get("isCredit")==null?1:map.get("isCredit"));
		if(fiid==25){
			QrCode=baiduPayService.getQrCode(unno, mid, map.get("MERCHANTCODE")==null?"":String.valueOf(map.get("MERCHANTCODE")), 
					subject, amount, fiid, orderid, tid,hybRate,hybType);
		}else if(fiid==34){
			String key=String.valueOf(map.get("MINFO2"));
			if(key==null || "null".equals(key) || "".equals(key)){
				throw new BusinessException(9006,"未找到与商户对应的秘钥！");
			}
			QrCode=cibService.getcibPayUrl(unno,mid, amount, String.valueOf(map.get("MERCHANTCODE")), 
					subject, fiid, orderid,tid,key,payway,hybRate,hybType);
		}else if(fiid==43){
			QrCode=bcmPayService.getQrCode(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
			 payway,orderid, tid,hybRate,hybType);
		}else if(fiid==46){
			QrCode=bcmPayService.getQrCode(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
					 payway,orderid, tid,hybRate,hybType);
		}else if(fiid==54){
			QrCode= cupsATPayService.cupsAliPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
					 payway,orderid, tid,hybRate,hybType,String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if(fiid==53){
			QrCode= cupsATPayService.cupsWxPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
					 payway,orderid, tid,hybRate,hybType,String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if(fiid==60){
			QrCode= netCupsPayService.cupsWxPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
					 payway,orderid, tid,hybRate,hybType,String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if(fiid==61){
			QrCode= netCupsPayService.cupsAliPay(unno, mid, String.valueOf(map.get("MERCHANTCODE")), subject, amount, fiid, 
					payway, orderid, tid, hybRate, hybType,String.valueOf(map.get("CHANNEL_ID")),isCredit);
		} else{
			throw new BusinessException(8005, "未知错误");
		}
		
		return QrCode;
	}
	

	public String barcodePay(HrtPayXmlBean bean) throws BusinessException {
		
		checkAuthCodePayType(bean);
		String subject = bean.getSubject();
        /*
         * 2018-11-27 修改
         * 
         * 机构  j62077 根据定位获取交易地点 area
         * 
         */
		String area=bean.getArea();
		BigDecimal amount = new BigDecimal(bean.getAmount());
		Map<String, Object> map = cmbcPay.getMerchantCode3(bean.getUnno(), bean.getMid(), bean.getPayway(),amount,area,"BS");
		int fiid=Integer.parseInt(String.valueOf(map.get("FIID")));
		String isCredit= String.valueOf(map.get("isCredit")==null?1:map.get("isCredit"));
		xpayService.checkBankTxnLimit(fiid,amount,bean.getPayway());
		String merchantCode = String.valueOf(map.get("MERCHANTCODE"));
		if (subject == null || "".equals(subject)) {
			subject = String.valueOf(map.get("SHORTNAME"));
		}
		String resp;
		if (fiid == 25) {
			resp = baiduPayService.barCodePay(bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getScene(), bean.getAuthcode(), amount, subject, bean.getTid());
		}else if(fiid==34){
			String key=String.valueOf(map.get("MINFO2"));
			if(key==null || "null".equals(key) || "".equals(key)){
				throw new BusinessException(9006,"未找到与商户对应的秘钥！");
			}
			resp=cibService.barcodePay(bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getScene(), bean.getAuthcode(), amount, subject,bean.getTid(),key,bean.getPaymode());
		}else if (fiid == 43) { 
			resp = bcmPayService.barCodePay(bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),bean.getPaymode());
		}else if (fiid == 46) { 
			resp = bcmPayService.barCodePay(bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),bean.getPaymode());
		}else if (fiid == 53 ) { 
			resp = cupsATPayService.cupsWxBsPay (bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if (fiid == 54 ) { 
			resp = cupsATPayService.cupsAliBsPay(bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if (fiid == 60 ) { 
			resp = netCupsPayService.cupsWxBsPay (bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),String.valueOf(map.get("MCH_ID")), String.valueOf(map.get("CHANNEL_ID")),isCredit);
		}else if (fiid == 61 ) { 
			resp = netCupsPayService.cupsAliBsPay (bean,bean.getUnno(), bean.getMid(), fiid, bean.getPayway(), bean.getOrderid(),
					merchantCode, bean.getAuthcode(), amount, subject, bean.getTid(),String.valueOf(map.get("CHANNEL_ID")),isCredit);
		} else{
			 throw new BusinessException(8005,"未知错误");
		}
		return resp;
	}
	
	/**
	 * 判断条码支付类型
	 * @param bean
	 * @throws BusinessException 
	 */
	public void checkAuthCodePayType(HrtPayXmlBean bean) throws BusinessException{
		if ("WXZF".equals(bean.getPayway()) || "ZFBZF".equals(bean.getPayway())
				|| "QQZF".equals(bean.getPayway()) || "BDQB".equals(bean.getPayway())|| "JDZF".equals(bean.getPayway())|| "JDPAY".equals(bean.getPayway())) {
		}else{
			throw new HrtBusinessException(9010);
		}
		try {
			Integer start=Integer.parseInt(bean.getAuthcode().substring(0, 2));
			if("WXZF".equals(bean.getPayway())&&start>=10 &&start<=15){
				bean.setPaymode("1");
			}else if("ZFBZF".equals(bean.getPayway())&&start>=25&&start<=30){
				bean.setPaymode("2");
			}else if(("JDZF".equals(bean.getPayway())|| "JDPAY".equals(bean.getPayway())) 
					&&(start==18||start==62)){
				bean.setPaymode("6");
			}else if("BDQB".equals(bean.getPayway())&&start==31){
				bean.setPaymode("5");
			}else if("QQZF".equals(bean.getPayway())&&start==91){
				bean.setPaymode("4");
			}else{
				throw new BusinessException(8005,"请扫描正确的付款码!");
			}
		} catch (Exception e) {
			throw new BusinessException(8005,"请扫描正确的付款码!");
		}
	}
}
