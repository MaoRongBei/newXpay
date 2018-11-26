package com.hrtpayment.xpay.baidu.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author lvjianyu 百度钱包-条码请求参数bean
 */
public class BaiduOrderBean {
	private String pay_code; // 付款码 付款码。不超过 18 位；前缀：31 是
	private Integer service_code; // 服务编号 整数，目前必须为 1 是
	private String sp_no; // 百度钱包商户号 10 位数字组成的字符串 是
	private String order_create_time;// 创建订单的时间 YYYYMMDDHHMMSS 是
	private String order_no;// 订单号，商户须保证订单号在商户系统内部唯一。 不超过 20 个字符 是
	private String goods_name;// 商品的名称 允许包含中文；不超过 128 个字符或 64 个汉字 是
	private String goods_desc;// 商品的描述信息 允许包含中文；不超过 255 个字符或 127 个汉字 否

	private String goods_url;// 商品在商户网站上的URL。 URL 否
	private Integer total_amount;// 总金额，以分为单位 非负整数 是
	private String currency; // 币种，默认人民币 取值范围参见附录 是
	private String return_url;// 百度钱包主动通知商户支付结果的 URL 仅支持 http(s)的 URL。 否
	private String expire_time;// 交易的超时时间YYYYMMDDHHMMSS，不得早于交 易创建的时间。 否
	private Integer input_charset; // 请求参数的字符编码 取值范围参见附录 是
	private String version;// 接口的版本号 必须为 2 是
	private String sign; // 签名结果 取决于签名方法 是
	private String sign_method; // 签名方法 取值范围参见附录 是
	private String extra; // 商户自定义数据 不超过 255 个字符 否
	private String mno; // 实体商户门店号 取值范围参见附录 否
	private String mname; // 实体商户门店名称 取值范围参见附录 否
	private String tno; // 实体商户终端号 取值范围参见附录 否
	private String profit_type; // 分润类型 1：实时分账【默认值】；2：异步分账；3：记账（只记账不分润） 否
	private String profit_solution; // 分润方案 //分账明细：格式为(id
									// 类型\^id\^金额类型\^金额\^备注\|)
									// {0,4}(id 类型\^id\^ 金额类型\^金额\^备注) 否

	//非必填，系统暂时取不到数据
//	private String sku_list;// 单品列表，百度源泉平台权益核销格式为：
//							// (单品编号\^单品名称\^单品 单价\^单品数量\^单品总价\|){0,9}
//							// (单品编号\^单品名称\^单品单价\^单品数量 \^单品总价) 否
//	private String sku_no;// 单品编号 不超过 20 个字符 是
//	private String sku_goods_name; // 单品名称 允许包含中文；不超过 128 个字符或 64 个汉字 否
//	private String sku_unit_amount; // 单品单价，以分为单位 非负整数 是
//	private String sku_count; // 单品数量 非负整数 是
//	private String sku_total_amount; // 单品总价，以分为单位 非负整数 是

	private String sp_pass_through; // 商户定制服务字段 商户定制服务字段，参与签名，具体 格式参见附录。 否

	public String getPay_code() {
		return pay_code;
	}

	public void setPay_code(String pay_code) {
		this.pay_code = pay_code;
	}

	public Integer getService_code() {
		return service_code;
	}

	public void setService_code(Integer service_code) {
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

	public String getReturn_url() {
		return return_url;
	}

	public void setReturn_url(String return_url) {
		this.return_url = return_url;
	}

	public String getExpire_time() {
		return expire_time;
	}

	public void setExpire_time(String expire_time) {
		this.expire_time = expire_time;
	}

	public Integer getInput_charset() {
		return input_charset;
	}

	public void setInput_charset(Integer input_charset) {
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

	public String getMno() {
		return mno;
	}

	public void setMno(String mno) {
		this.mno = mno;
	}

	public String getMname() {
		return mname;
	}

	public void setMname(String mname) {
		this.mname = mname;
	}

	public String getTno() {
		return tno;
	}

	public void setTno(String tno) {
		this.tno = tno;
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


	public Map<String, String> getPostMap() {
		Map<String, String> retMap = new LinkedHashMap<String, String>();

		retMap.put("pay_code", pay_code);
		retMap.put("service_code", service_code+"");
		retMap.put("sp_no", sp_no);
		retMap.put("order_create_time", order_create_time);
		retMap.put("order_no", order_no);
		retMap.put("goods_name", goods_name);
		if(goods_desc!=null)
			retMap.put("goods_desc", goods_desc);
		if(goods_url!=null)
			retMap.put("goods_url", goods_url);
		retMap.put("total_amount", total_amount+"");
		retMap.put("currency", currency);
		if(return_url!=null)
			retMap.put("return_url", return_url);
		if(expire_time!=null)
			retMap.put("expire_time", expire_time);
		retMap.put("input_charset", input_charset+"");
		retMap.put("version", version);
		retMap.put("sign", sign);
		retMap.put("sign_method", sign_method);
		if(extra!=null)
			retMap.put("extra", extra);
		if(mno!=null)
			retMap.put("mno", mno);
		if(mname!=null)
			retMap.put("mname", mname);
		if(tno!=null)
			retMap.put("tno", tno);
		if(profit_type!=null)
			retMap.put("profit_type", profit_type);
		if(profit_solution!=null)
			retMap.put("profit_solution", profit_solution);

		return retMap;
	}

}
