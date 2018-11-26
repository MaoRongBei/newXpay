package com.hrtpayment.xpay.quickpay.cups.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("TrxInf") 
public class TrxInfBean {
	
	private  String TrxId;
	private  String TrxDtTm;
	private  String  SettlmtDt;
	private  String TrxAmt;//CNY16+1+2
	private  String TrxTrmTp;
	
	public String getTrxTrmTp() {
		return TrxTrmTp;
	}
	public void setTrxTrmTp(String trxTrmTp) {
		TrxTrmTp = trxTrmTp;
	}
	public String getTrxId() {
		return TrxId;
	}
	public void setTrxId(String trxId) {
		TrxId = trxId;
	}
	public String getTrxDtTm() {
		return TrxDtTm;
	}
	public void setTrxDtTm(String trxDtTm) {
		TrxDtTm = trxDtTm;
	}
 
	public String getSettlmtDt() {
		return SettlmtDt;
	}
	public void setSettlmtDt(String settlmtDt) {
		SettlmtDt = settlmtDt;
	}
	public String getTrxAmt() {
		return TrxAmt;
	}
	public void setTrxAmt(String trxAmt) {
		TrxAmt = trxAmt;
	}
	
	
 
}
