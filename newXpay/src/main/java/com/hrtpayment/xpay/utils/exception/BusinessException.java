package com.hrtpayment.xpay.utils.exception;

public class BusinessException extends Exception{
	private int code;

	/**
	 * 
	 */
	private static final long serialVersionUID = 2829571340086323488L;

	public BusinessException(int code, String str) {
		super(str);
		this.code = code;
	}
	public String getErrorCode(){
		return code+"";
	}
	public int getCode(){
		return code;
	}
}
