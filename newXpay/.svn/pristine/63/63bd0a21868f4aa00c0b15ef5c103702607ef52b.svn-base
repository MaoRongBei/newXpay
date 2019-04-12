package com.hrtpayment.xpay.baidu.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author lvjianyu 支付成功后百度钱包向商户提供的 return_url 进行通知
 */
public class BaiduCalbackBean {

	private String sp_no; // 百度钱包商户号 10 位数字组成的字符串 是
	private String order_no;// 商户订单号 不超过 20 个字符 是
	private String bfb_order_no;// 百度钱包交易号 不超过 32 个字符 是
	private String bfb_order_create_time;// 百度钱包交易创建时间 YYYYMMDDHHMMSS 是
	private String pay_time;// 支付时间 YYYYMMDDHHMMSS 是
	private String pay_type; // 支付类型 取值范围参见附录 是
	private String bank_no; // 用于支付的银行编号 取值范围参见附录 否；用户使用网银支付和银行网关支付时才有
	private String unit_amount;// 商品单价，以分为单位 非负整数 否
	private String unit_count;// 商品数量 非负整数 否
	private String transport_amount;// 运费，以分为单位 非负整数 否
	private String total_amount;// 总金额，以分为单位 非负整数 是
	private String fee_amount;// 百度钱包收取商户的手续费，以分为单位 非负整数 是
	private String currency; // 币种，目前仅支持人民币 取值范围参见附录 是
	private String buyer_sp_username;// 买家在商户网站的用户名 允许包含中文；不超过 64 字符或 32 个汉字 否
	private String pay_result; // 支付结果代码 取值范围参见附录 是
	private String input_charset; // 请求参数的字符编码 取值范围参见附录 是
	private String version; // 版本号 与支付请求中的 version 保持一致 是
	private String sign;// 签名结果 取决于签名方法 是
	private String sign_method;// 签名方法 取值范围参见附录 是
	private String extra;// 商户自定义数据 不超过 255 个字符 否

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

	public String getBfb_order_no() {
		return bfb_order_no;
	}

	public void setBfb_order_no(String bfb_order_no) {
		this.bfb_order_no = bfb_order_no;
	}

	public String getBfb_order_create_time() {
		return bfb_order_create_time;
	}

	public void setBfb_order_create_time(String bfb_order_create_time) {
		this.bfb_order_create_time = bfb_order_create_time;
	}

	public String getPay_time() {
		return pay_time;
	}

	public void setPay_time(String pay_time) {
		this.pay_time = pay_time;
	}

	public String getPay_type() {
		return pay_type;
	}

	public void setPay_type(String pay_type) {
		this.pay_type = pay_type;
	}

	public String getBank_no() {
		return bank_no;
	}

	public void setBank_no(String bank_no) {
		this.bank_no = bank_no;
	}

	public String getUnit_amount() {
		return unit_amount;
	}

	public void setUnit_amount(String unit_amount) {
		this.unit_amount = unit_amount;
	}

	public String getUnit_count() {
		return unit_count;
	}

	public void setUnit_count(String unit_count) {
		this.unit_count = unit_count;
	}

	public String getTransport_amount() {
		return transport_amount;
	}

	public void setTransport_amount(String transport_amount) {
		this.transport_amount = transport_amount;
	}

	public String getTotal_amount() {
		return total_amount;
	}

	public void setTotal_amount(String total_amount) {
		this.total_amount = total_amount;
	}

	public String getFee_amount() {
		return fee_amount;
	}

	public void setFee_amount(String fee_amount) {
		this.fee_amount = fee_amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getBuyer_sp_username() {
		return buyer_sp_username;
	}

	public void setBuyer_sp_username(String buyer_sp_username) {
		this.buyer_sp_username = buyer_sp_username;
	}

	public String getPay_result() {
		return pay_result;
	}

	public void setPay_result(String pay_result) {
		this.pay_result = pay_result;
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

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public Map<String, String> getSignMap(){
		Map<String, String> map=new  LinkedHashMap<String, String>();
		map.put("sp_no", sp_no);
		map.put("order_no", order_no);
		map.put("bfb_order_no", bfb_order_no);
		map.put("bfb_order_create_time", bfb_order_create_time);
		map.put("pay_time", pay_time);
		map.put("pay_type", pay_type);
		map.put("bank_no", bank_no);
		if(unit_amount!=null)
			map.put("unit_amount", unit_amount);
		else
			map.put("unit_amount", "");
		if(unit_count!=null)
			map.put("unit_count", unit_count);
		else
			map.put("unit_count", "");
		if(transport_amount!=null)
			map.put("transport_amount", transport_amount);
		else
			map.put("transport_amount", "");
		map.put("total_amount", total_amount);
		map.put("fee_amount", fee_amount);
		map.put("currency", currency);
		if(buyer_sp_username!=null)
			map.put("buyer_sp_username", buyer_sp_username);
		else
			map.put("buyer_sp_username", "");
		map.put("pay_result", pay_result);
		map.put("input_charset", input_charset);
		map.put("version", version);
		map.put("sign", sign);
		map.put("sign_method", sign_method);
		if(extra!=null)
			map.put("extra", extra);
		else
			map.put("extra", "");
		return map;
	}
	
	
	@Override
	public String toString() {
		return "BaiduCalbackBean [sp_no=" + sp_no + ", order_no=" + order_no + ", bfb_order_no=" + bfb_order_no
				+ ", bfb_order_create_time=" + bfb_order_create_time + ", pay_time=" + pay_time + ", pay_type="
				+ pay_type + ", bank_no=" + bank_no + ", unit_amount=" + unit_amount + ", unit_count=" + unit_count
				+ ", transport_amount=" + transport_amount + ", total_amount=" + total_amount + ", fee_amount="
				+ fee_amount + ", currency=" + currency + ", buyer_sp_username=" + buyer_sp_username + ", pay_result="
				+ pay_result + ", input_charset=" + input_charset + ", version=" + version + ", sign=" + sign
				+ ", sign_method=" + sign_method + ", extra=" + extra + "]";
	}

	
}
