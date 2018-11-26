package com.hrtpayment.xpay.quickpay.cups.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("OrdrInf") 
public class OrdrInfBean {
	private  String OrdrId; 
	private  String OrdrDesc;
	public String getOrdrId() {
		return OrdrId;
	}
	public void setOrdrId(String ordrId) {
		OrdrId = ordrId;
	}
	public String getOrdrDesc() {
		return OrdrDesc;
	}
	public void setOrdrDesc(String ordrDesc) {
		OrdrDesc = ordrDesc;
	} 
	 
	
}
