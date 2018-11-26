package com.hrtpayment.xpay.common.bean;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="merchant")
public class Merchant {
	private String mid;
	private Merchant2 data;

	public String getMid() {
		return mid;
	}

	public void setMid(String mid) {
		this.mid = mid;
	}

	public Merchant2 getData() {
		return data;
	}

	public void setData(Merchant2 data) {
		this.data = data;
	}
	
}
