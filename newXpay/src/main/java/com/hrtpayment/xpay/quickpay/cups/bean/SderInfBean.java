package com.hrtpayment.xpay.quickpay.cups.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("SderInf") 
public class SderInfBean {
   
	private  String SderIssrId;
	private  String SderAcctIssrId;
	private  String SderAcctIssrNm;
	private  String SderAcctInf;
	
	
	public String getSderAcctInf() {
		return SderAcctInf;
	}
	public void setSderAcctInf(String sderAcctInf) {
		SderAcctInf = sderAcctInf;
	}
	public String getSderIssrId() {
		return SderIssrId;
	}
	public void setSderIssrId(String sderIssrId) {
		SderIssrId = sderIssrId;
	}
	public String getSderAcctIssrId() {
		return SderAcctIssrId;
	}
	public void setSderAcctIssrId(String sderAcctIssrId) {
		SderAcctIssrId = sderAcctIssrId;
	}
	public String getSderAcctIssrNm() {
		return SderAcctIssrNm;
	}
	public void setSderAcctIssrNm(String sderAcctIssrNm) {
		SderAcctIssrNm = sderAcctIssrNm;
	}
	 
	
	
}
