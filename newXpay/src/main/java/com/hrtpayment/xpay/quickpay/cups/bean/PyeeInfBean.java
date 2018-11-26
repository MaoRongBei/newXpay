package com.hrtpayment.xpay.quickpay.cups.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("PyeeInf") 
public class PyeeInfBean {
	 
	private  String PyeeIssrId; 
	private  String PyeeAcctIssrId;
	private  String PyeeAcctId;//退款用  收款方账户
	
	
	
	/**
	 * @return the pyeeAcctId
	 */
	public String getPyeeAcctId() {
		return PyeeAcctId;
	}
	/**
	 * @param pyeeAcctId the pyeeAcctId to set
	 */
	public void setPyeeAcctId(String pyeeAcctId) {
		PyeeAcctId = pyeeAcctId;
	}
	public String getPyeeIssrId() {
		return PyeeIssrId;
	}
	public void setPyeeIssrId(String pyeeIssrId) {
		PyeeIssrId = pyeeIssrId;
	}
	public String getPyeeAcctIssrId() {
		return PyeeAcctIssrId;
	}
	public void setPyeeAcctIssrId(String pyeeAcctIssrId) {
		PyeeAcctIssrId = pyeeAcctIssrId;
	}
	
	
	
}
