package com.hrtpayment.xpay.common.service;

public interface Callback<T> {
	void resp(T t);
	
	void catchTimeOut(T t);
}
