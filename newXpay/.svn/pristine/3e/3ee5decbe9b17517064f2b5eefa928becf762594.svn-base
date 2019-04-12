package com.hrtpayment.xpay.utils.exception;

import java.util.HashMap;
import java.util.Map;

import com.hrtpayment.xpay.common.bean.HrtErrorMsg;

public class HrtBusinessException extends RuntimeException{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5849497201962573899L;
	private static final Map<Integer,String> map = new HashMap<Integer,String>();

	static {
		add(9000, "参数错误");
		add(9001, "金额错误");
		add(9002, "unno错误");
		add(9003, "mid错误");
		add(9004, "签名校验失败");
		add(9005, "订单号格式错误");
		add(9006, "未找到unno对应的签名密钥");
		add(9007, "订单号重复");
		add(9008, "订单不存在");
		add(9009, "指定通道未开通");
		add(9010, "不支持的支付通道");
		
		add(8000, "交易失败");
		
		//退款
		add(7001,"原订单不存在");
		add(7002,"原交易金额错误");
		add(7003,"unno与原交易不符");
		add(7004,"mid与原交易不符");
		add(7005,"尚有在处理中的退款");
		add(7006,"退款金额超过剩余可退金额");
		
		//公众号
		add(6001,"交易状态错误");
		
		add(1001,"其他错误");
		add(1002,"接口后台错误");
		
		add(2001,"后端返回验签失败");
	}
	private static void add(int code,String msg){
		map.put(code, msg);
	}

	private int code;
	private String message;

	public HrtBusinessException(int code){
		this.code = code;
		this.message = map.get(code);
	}
	public HrtBusinessException (int code,String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
