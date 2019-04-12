package com.hrtpayment.xpay.baidu.bean;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
public class BaiduRefundQueryRetBean {
	private static JAXBContext jc;
	static {
		try {
			jc = JAXBContext.newInstance(BaiduRefundQueryRetBean.class);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public static BaiduRefundQueryRetBean parseXmlFromStr(String str) {
		BaiduRefundQueryRetBean msg = null;
		try {
			Unmarshaller us = jc.createUnmarshaller();
			msg = (BaiduRefundQueryRetBean) us.unmarshal(new StringReader(str));
		} catch (JAXBException e) {
		}
		return msg;
	}

	public String toXmlString() {
		String str = null;
		try {
			Marshaller ms = jc.createMarshaller();
			ms.setProperty(Marshaller.JAXB_FRAGMENT, true);
			StringWriter writer = new StringWriter();
			ms.marshal(this, writer);
			str = writer.toString();
		} catch (JAXBException e) {
		}
		return str;
	}

	// <?xml version="1.0" encoding="gbk" ?>
	// <response>
	// <ret_code>1</ret_code>
	// <ret_details>已退款至百度钱包余额</ret_details>
	// <cashback_amount>1</cashback_amount>
	// </response>

	private String ret_code;
	private String ret_details;
	private String cashback_amount;

	public String getCashback_amount() {
		return cashback_amount;
	}

	public void setCashback_amount(String cashback_amount) {
		this.cashback_amount = cashback_amount;
	}
	
	public String getRet_details() {
		return ret_details;
	}

	public void setRet_details(String ret_details) {
		this.ret_details = ret_details;
	}

	public String getRet_code() {
		return ret_code;
	}

	public void setRet_code(String ret_code) {
		this.ret_code = ret_code;
	}

}
