package com.hrtpayment.xpay.baidu.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 百度钱包-获取二维码连接 实体
 * @author lvjianyu 
 */
public class BaiduQrPayBean extends BaseBaiduQrPayBean {
	/* 收银台参数（以下参数需要签名） */
	private String service_code;// 服务编号 整数，目前必须为 1 是
	private String sp_no;// 百付宝商户号 10 位数字组成的字符串 是
	private String order_create_time;// 创建订单的时间 YYYYMMDDHHMMSS 是
	private String order_no;// 订单号，商户须保证订单号在商户系统内部唯一。 不超过 20 个字符 是
	private String goods_name;// 商品的名称允许包含中文；不超过 128 个字符或 64 个汉字 是
	private String goods_desc;// 商品的描述信息 允许包含中文；不超过 255 个字符或 127 个汉字 否
	private String goods_url;// 商品在商户网站上的 URL 否
	private Integer unit_amount;// 商品单价，以分为单位 非负整数 否
	private Integer unit_count;// 商品数量 非负整数 否
	private Integer transport_amount;// 运费 非负整数 否
	private Integer total_amount;// 总金额，以分为单位 非负整数 是
	private String currency;// 币种，默认人民币 取值范围参见附录 是
	private String buyer_sp_username;// 买家在商户网站的用户名 允许包含中文；不超过 64 字符或 32 个汉字 否
	private String return_url;// 百付宝主动通知商户支付结果的 URL 仅支持 http(s)的 URL。 是
	private String page_url;// 用户点击该 URL 可以返回到商户网站；该 URL 也可以起到通知支付结果的作用 仅支持
							// http(s)的 URL。 否
	private String pay_type;// 默认支付方式 取值范围参见附录 是
	private String bank_no;// 网银支付或银行网关支付时，默认银行的编码取值范围参见附录 否；如果pay_type 是银行网
							// 关支付， 则必须有 值
	private String expire_time;// 交易的超时时间YYYYMMDDHHMMSS，不得早于交易创建的时间。 否//
	private String sp_uno;// 用户在商户端的用户 id// 或者用户名(必须在商户 端唯一，用来形成快捷 支付合约) 不超过 64
							// 个字符 否
	private String input_charset;// 请求参数的字符编码 取值范围参见附录// 是
	private String version;// 接口的版本号 必须为// 2 是
	private String sign;// 签名结果 取决于签名方法// 是
	private String sign_method;// 签名方法 取值范围参见附录// 是
	private String extra;// 商户自定义数据 不超过// 255 个字符 否
	private String profit_type;// 分润类型 1：实时分账；2：异步分账；// 3：记账（只记账不分润） 否，默认 为// 1
	private String profit_solution;// 分润方案 //分账明细：格式为//(id 类型\^id//\^金额
									// 类型\^金额\^备注\|){0,4}(id 类型\^id\^
									// 金额类型\^金额\^备注)// 否
	private String sp_pass_through;// 商户定制服务字段 商户定制服务字段，参与签名，具体// 格式参见附录。 否//

	
	
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

	public String getOrder_create_time() {
		return order_create_time;
	}

	public void setOrder_create_time(String order_create_time) {
		this.order_create_time = order_create_time;
	}

	public String getOrder_no() {
		return order_no;
	}

	public void setOrder_no(String order_no) {
		this.order_no = order_no;
	}

	public String getGoods_name() {
		return goods_name;
	}

	public void setGoods_name(String goods_name) {
		this.goods_name = goods_name;
	}

	public String getGoods_desc() {
		return goods_desc;
	}

	public void setGoods_desc(String goods_desc) {
		this.goods_desc = goods_desc;
	}

	public String getGoods_url() {
		return goods_url;
	}

	public void setGoods_url(String goods_url) {
		this.goods_url = goods_url;
	}

	public Integer getUnit_amount() {
		return unit_amount;
	}

	public void setUnit_amount(Integer unit_amount) {
		this.unit_amount = unit_amount;
	}

	public Integer getUnit_count() {
		return unit_count;
	}

	public void setUnit_count(Integer unit_count) {
		this.unit_count = unit_count;
	}

	public Integer getTransport_amount() {
		return transport_amount;
	}

	public void setTransport_amount(Integer transport_amount) {
		this.transport_amount = transport_amount;
	}

	public Integer getTotal_amount() {
		return total_amount;
	}

	public void setTotal_amount(Integer total_amount) {
		this.total_amount = total_amount;
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

	public String getReturn_url() {
		return return_url;
	}

	public void setReturn_url(String return_url) {
		this.return_url = return_url;
	}

	public String getPage_url() {
		return page_url;
	}

	public void setPage_url(String page_url) {
		this.page_url = page_url;
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

	public String getExpire_time() {
		return expire_time;
	}

	public void setExpire_time(String expire_time) {
		this.expire_time = expire_time;
	}

	public String getSp_uno() {
		return sp_uno;
	}

	public void setSp_uno(String sp_uno) {
		this.sp_uno = sp_uno;
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

	public String getProfit_type() {
		return profit_type;
	}

	public void setProfit_type(String profit_type) {
		this.profit_type = profit_type;
	}

	public String getProfit_solution() {
		return profit_solution;
	}

	public void setProfit_solution(String profit_solution) {
		this.profit_solution = profit_solution;
	}

	public String getSp_pass_through() {
		return sp_pass_through;
	}

	public void setSp_pass_through(String sp_pass_through) {
		this.sp_pass_through = sp_pass_through;
	}
	
	
	public Map<String, String> getPostMap(boolean isPost){
		Map<String, String> retMap =new LinkedHashMap<String,String>();
		
		retMap.put("service_code", service_code);
		retMap.put("sp_no", sp_no);
		retMap.put("order_create_time",order_create_time);
		retMap.put("order_no", order_no);
		retMap.put("goods_name", goods_name);
		if(unit_amount!=null)
			retMap.put("unit_amount", unit_amount.toString());
		if(unit_count!=null)
			retMap.put("unit_count", unit_count.toString());
		if(transport_amount!=null)
			retMap.put("transport_amount", transport_amount.toString());
		retMap.put("total_amount", total_amount.toString());
		retMap.put("currency",currency);
		retMap.put("return_url", return_url);
		retMap.put("pay_type", pay_type);
		if(bank_no!=null)
			retMap.put("bank_no", bank_no);
		if(expire_time!=null)
			retMap.put("expire_time", expire_time);
		retMap.put("input_charset", input_charset);
		retMap.put("version", version);
		retMap.put("sign_method", sign_method);
		if(isPost){
			retMap.put("output_type", getOutput_type());
			retMap.put("sign", sign);
		}
		
		return retMap;
	}

}
