package com.hrtpayment.xpay.quickpay.newCups.util;

import org.apache.http.protocol.HttpRequestExecutor;

import io.netty.handler.codec.http.HttpRequest;

public class http {
  
	
	
	public void send(String url,String msg){
		HttpRequest request =(HttpRequest) new HttpRequestExecutor();
		request.setUri(url);
	 
		
		
	}
}
