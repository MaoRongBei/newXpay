package com.hrtpayment.xpay.utils.exception;
/**
 * 访问网络出错
 * @author aibing
 *
 */
public class ClientNetworkException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7589005188437271810L;

	public ClientNetworkException(String str) {
		super(str);
	}
}
