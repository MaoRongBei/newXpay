package com.hrtpayment.xpay.baidu.bean;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "response")
public class BaiduRefundRetMsgBean {
	private static JAXBContext jc;
	static {
		try {
			jc = JAXBContext.newInstance(BaiduRefundRetMsgBean.class);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public static BaiduRefundRetMsgBean parseXmlFromStr(String str) {
		BaiduRefundRetMsgBean msg = null;
		try {
			Unmarshaller us = jc.createUnmarshaller();
			msg = (BaiduRefundRetMsgBean) us.unmarshal(new StringReader(str));
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

	private String cashback_amount;
	private String sp_no;
	private String order_no;
	private String bfb_order_no;
	private String sp_refund_no;
	private String ret_code;
	private String ret_detail;

	public String getCashback_amount() {
		return cashback_amount;
	}

	public void setCashback_amount(String cashback_amount) {
		this.cashback_amount = cashback_amount;
	}

	public String getSp_no() {
		return sp_no;
	}

	public void setSp_no(String sp_no) {
		this.sp_no = sp_no;
	}

	public String getOrder_no() {
		return order_no;
	}

	public void setOrder_no(String order_no) {
		this.order_no = order_no;
	}

	public String getBfb_order_no() {
		return bfb_order_no;
	}

	public void setBfb_order_no(String bfb_order_no) {
		this.bfb_order_no = bfb_order_no;
	}

	public String getSp_refund_no() {
		return sp_refund_no;
	}

	public void setSp_refund_no(String sp_refund_no) {
		this.sp_refund_no = sp_refund_no;
	}

	public String getRet_code() {
		return ret_code;
	}

	public void setRet_code(String ret_code) {
		this.ret_code = ret_code;
	}

	public String getRet_detail() {
		return ret_detail;
	}

	public void setRet_detail(String ret_detail) {
		this.ret_detail = ret_detail;
	}

}
