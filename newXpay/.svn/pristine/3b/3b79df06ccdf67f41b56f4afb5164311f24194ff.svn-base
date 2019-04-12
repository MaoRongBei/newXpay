package com.hrtpayment.xpay.baidu.bean;

import java.util.LinkedHashMap;
import java.util.Map;

public class BaiduRefundBean {
	private String service_code;// 服务编号 整数，取值为2 String(1) 是
	private String input_charset;// 参数字符编码集 使用GBK方式，具体取值请参考附录字符编码列表String(1)是
	private String sign_method;// 签名方式 使用MD5方式，具体取值请参考附录摘要算法列表String(1)是
	private String sign;// 签名 具体长度取决于签名方法 String 是
	private String output_type;// 响应数据的格式，默认XML具体取值请参考附录响应数据格式列表 String(1)是
	private String output_charset;// 响应数据的编码，默认GBK具体取值请参考附录字符编码列表String(1)是
	private String return_url;// 服务器异步通知地址
								// 退款完成后，百度钱包会按照此地址将结果以后台的方式发送到商户网站String(255)是
	private String return_method;// 后台通知请求方式 1为GET，2为POST，默认为POST方式 String(1)否
	private String version;// 版本号 本接口版本号，填写2 String(1)是
	private String sp_no;// 商户ID 商户id String(10)是
	private String order_no;// 外部交易单号 商户外部交易单号 String(20)是
	private Integer cashback_amount;// 退款金额 退款金额，以分为单位。 Number 是
	private String cashback_time;// 退款请求时间 格式YYYYMMDDHHMMSS String(14)是
	private String currency;// 币种，默认为CNY 具体取值请参考附录币种列表String(3)是
	private String sp_refund_no;// 商户退款流水号 商户生成退款流水号，要求同一商户退款流水号不可重复，需要在数据库
	// 中根据退款id及商户退款流水号建立索引。不超过21个字符)String(21)是
	private String refund_type;// 退款类型 1为退至钱包余额，2为原路退回。默认为2原路退回。
	// 注：若指定退至钱包余额，但交易为纯网关交易，则自动更改为原路退回。实际退款类型在同步返回结果及退款通知中体现。String(1)否
	private String refund_profit_solution;// 分润退款参数 具体取值请参考附录分润退款参数String 否

	public String getService_code() {
		return service_code;
	}

	public void setService_code(String service_code) {
		this.service_code = service_code;
	}

	public String getInput_charset() {
		return input_charset;
	}

	public void setInput_charset(String input_charset) {
		this.input_charset = input_charset;
	}

	public String getSign_method() {
		return sign_method;
	}

	public void setSign_method(String sign_method) {
		this.sign_method = sign_method;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
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

	public String getReturn_url() {
		return return_url;
	}

	public void setReturn_url(String return_url) {
		this.return_url = return_url;
	}

	public String getReturn_method() {
		return return_method;
	}

	public void setReturn_method(String return_method) {
		this.return_method = return_method;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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

	public Integer getCashback_amount() {
		return cashback_amount;
	}

	public void setCashback_amount(Integer cashback_amount) {
		this.cashback_amount = cashback_amount;
	}

	public String getCashback_time() {
		return cashback_time;
	}

	public void setCashback_time(String cashback_time) {
		this.cashback_time = cashback_time;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getSp_refund_no() {
		return sp_refund_no;
	}

	public void setSp_refund_no(String sp_refund_no) {
		this.sp_refund_no = sp_refund_no;
	}

	public String getRefund_type() {
		return refund_type;
	}

	public void setRefund_type(String refund_type) {
		this.refund_type = refund_type;
	}

	public String getRefund_profit_solution() {
		return refund_profit_solution;
	}

	public void setRefund_profit_solution(String refund_profit_solution) {
		this.refund_profit_solution = refund_profit_solution;
	}

	public Map<String, String> getSignMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("service_code", service_code);
		map.put("input_charset", input_charset);
		map.put("sign_method", sign_method);
		if (sign != null)
			map.put("sign", sign);
		map.put("output_type", output_type);
		map.put("output_charset", output_charset);
		map.put("return_url", return_url);
		map.put("return_method", return_method);
		map.put("version", version);
		map.put("sp_no", sp_no);
		map.put("order_no", order_no);
		map.put("cashback_amount", cashback_amount.toString());
		map.put("cashback_time", cashback_time);
		map.put("currency", currency);
		map.put("sp_refund_no", sp_refund_no);
		if(refund_type!=null)
			map.put("refund_type", refund_type);
		if(refund_profit_solution!=null)
			map.put("refund_profit_solution", refund_profit_solution);
		return map;
	}

}
