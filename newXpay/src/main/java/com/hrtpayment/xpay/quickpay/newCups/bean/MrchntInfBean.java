package com.hrtpayment.xpay.quickpay.newCups.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("MrchntInf") 
public class MrchntInfBean {
	
	private String MrchntNo;
	private String MrchntTpId;
	private String MrchntPltfrmNm;
	public String getMrchntNo() {
		return MrchntNo;
	}
	public void setMrchntNo(String mrchntNo) {
		MrchntNo = mrchntNo;
	}
	public String getMrchntTpId() {
		return MrchntTpId;
	}
	public void setMrchntTpId(String mrchntTpId) {
		MrchntTpId = mrchntTpId;
	}
	public String getMrchntPltfrmNm() {
		return MrchntPltfrmNm;
	}
	public void setMrchntPltfrmNm(String mrchntPltfrmNm) {
		MrchntPltfrmNm = mrchntPltfrmNm;
	}
	
	

}
