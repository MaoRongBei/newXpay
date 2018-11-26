package com.hrtpayment.xpay.quickpay.newCups.bean;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("PyerInf") 
public class PyerInfBean {
	private  String PyerAcctId; 
	private  String AuthMsg;//动态码  直接付款使用
	private  String Smskey;
	
	private  String PyerAcctIssrId;
	private  String PyeeIssrId; 
	
	
	
 
	/**
	 * @return the pyeeIssrId
	 */
	public String getPyeeIssrId() {
		return PyeeIssrId;
	}
	/**
	 * @param pyeeIssrId the pyeeIssrId to set
	 */
	public void setPyeeIssrId(String pyeeIssrId) {
		PyeeIssrId = pyeeIssrId;
	}
	/**
	 * @return the pyeeAcctIssrId
	 */
	public String getPyerAcctIssrId() {
		return PyerAcctIssrId;
	}
	/**
	 * @param pyeeAcctIssrId the pyeeAcctIssrId to set
	 */
	public void setPyerAcctIssrId(String pyerAcctIssrId) {
		PyerAcctIssrId = pyerAcctIssrId;
	}
	/**
	 * @return the smskey
	 */
	public String getSmskey() {
		return Smskey;
	}
	/**
	 * @param smskey the smskey to set
	 */
	public void setSmskey(String smskey) {
		Smskey = smskey;
	}
	public String getAuthMsg() {
		return AuthMsg;
	}
	public void setAuthMsg(String authMsg) {
		AuthMsg = authMsg;
	}
	public String getPyerAcctId() {
		return PyerAcctId;
	}
	public void setPyerAcctId(String pyerAcctId) {
		PyerAcctId = pyerAcctId;
	}
	
}
