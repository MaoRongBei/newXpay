package com.hrtpayment.xpay.baidu.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author lvjianyu 百度钱包-扫码-按订单号查询bean
 */
public class BaiduQueryBean {

	private String sp_no; // 百度钱包商户号 10 位数字组成的字符串 是
	private String order_no;// 商户订单号 不超过 20 个字符 是
	private String input_charset;// 请求参数的字符编码 取值范围参见附录 是
	private String version; // 接口的版本号 必须为 2 是
	private String sign; // 签名结果 取决于签名方法 是
	private String sign_method;// 签名方法 取值范围参见附录 是

	public String getSp_no() {
		return sp_no;
	}

	public void setSp_no(String sp_no) {
		this.sp_no = sp_no;
	}

	public String getOrder_no() {
		return order_no;
	}

	public void setOrder_no(String order_no) {
		this.order_no = order_no;
	}

	public String getInput_charset() {
		return input_charset;
	}

	public void setInput_charset(String input_charset) {
		this.input_charset = input_charset;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getSign_method() {
		return sign_method;
	}

	public void setSign_method(String sign_method) {
		this.sign_method = sign_method;
	}
	
	public Map<String, String> getPostMap() {
		Map<String, String> retMap = new LinkedHashMap<String, String>();
		retMap.put("sp_no", sp_no);
		retMap.put("order_no", order_no);
		retMap.put("input_charset", input_charset);
		retMap.put("version", version);
		retMap.put("sign_method", sign_method);
		if(sign!=null)
			retMap.put("sign", sign);

		return retMap;
	}
	
}
