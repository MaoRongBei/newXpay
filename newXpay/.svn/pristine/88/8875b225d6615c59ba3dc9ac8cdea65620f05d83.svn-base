package com.hrtpayment.xpay.baidu.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 百度钱包-退款查询
 * 
 * @author lvjianyu
 *
 */
public class BaiduRefundQueryBean {
	private String service_code;// 服务编号 整数，目前必须为12 Y
	private String sp_no;// 百度钱包商户号 10位数字组成的字符串 Y
	private String order_no;// 订单号 不超过20个字符 Y
	private String sp_refund_no;// 退款流水号 外部商户退款流水号(不超过21个字符) N
	private String output_type;// 响应数据的格式，默认XML 具体取值请参考附录 Y
	private String output_charset;// 响应数据的编码，默认GBK具体取值请参考附录 Y
	private String version;// 接口的版本号 必须为2 Y
	private String sign;// 签名结果 取决于签名方法 Y
	private String sign_method;// 签名方法，默认MD5 取值范围参见附录 Y

	public String getService_code() {
		return service_code;
	}

	public void setService_code(String service_code) {
		this.service_code = service_code;
	}

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

	public String getSp_refund_no() {
		return sp_refund_no;
	}

	public void setSp_refund_no(String sp_refund_no) {
		this.sp_refund_no = sp_refund_no;
	}

	public String getOutput_type() {
		return output_type;
	}

	public void setOutput_type(String output_type) {
		this.output_type = output_type;
	}

	public String getOutput_charset() {
		return output_charset;
	}

	public void setOutput_charset(String output_charset) {
		this.output_charset = output_charset;
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

	public Map<String, String> getSignMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("service_code", service_code);
		map.put("sp_no", sp_no);
		map.put("order_no", order_no);
		if(sp_refund_no!=null)
			map.put("sp_refund_no", sp_refund_no);
		map.put("output_type", output_type);
		map.put("output_charset", output_charset);
		map.put("version", version);
		map.put("sign", sign);
		map.put("sign_method", sign_method);
		return map;
	}

}
