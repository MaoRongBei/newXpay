package com.hrtpayment.xpay.baidu.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author lvjianyu Baidu钱包 申请退款后-异步通知bean
 */
public class BaiduRefundCallBackBaen {
	private String bfb_order_no;// 百度钱包交易号 百度钱包交易号 Y
	private String cashback_amount;// 1 退款金额 Y
	private String order_no;// 外部交易单号 外部商户交易号 Y
	private String ret_code;// 退款结果 1 —— 退款成功（详细的失败描述码，见附录）Y
	private String ret_detail;// 退款结果详情 退款结果详情 N
	private String sp_no;// 商户id 商户id Y
	private String sp_refund_no;// 退款流水号 外部商户退款流水号 Y
	private String sign;// 5C7E1DBAC2C40764D9D00678D42B45C0 签名结果 Y
	private String sign_method;// 1 签名算法为MD5 Y

	public Map<String, String> getSignMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("bfb_order_no", bfb_order_no);
		map.put("cashback_amount", cashback_amount);
		map.put("order_no", order_no);
		map.put("ret_code", ret_code);
		map.put("ret_detail", ret_detail);
		map.put("sp_no", sp_no);
		map.put("sp_refund_no", sp_refund_no);
		map.put("sign_method", sign_method);
		map.put("sign", sign);
		return map;
	}

	public String getBfb_order_no() {
		return bfb_order_no;
	}

	public void setBfb_order_no(String bfb_order_no) {
		this.bfb_order_no = bfb_order_no;
	}

	public String getCashback_amount() {
		return cashback_amount;
	}

	public void setCashback_amount(String cashback_amount) {
		this.cashback_amount = cashback_amount;
	}

	public String getOrder_no() {
		return order_no;
	}

	public void setOrder_no(String order_no) {
		this.order_no = order_no;
	}

	public String getRet_code() {
		return ret_code;
	}

	public void setRet_code(String ret_code) {
		this.ret_code = ret_code;
	}

	public String getRet_detail() {
		return ret_detail;
	}

	public void setRet_detail(String ret_detail) {
		this.ret_detail = ret_detail;
	}

	public String getSp_no() {
		return sp_no;
	}

	public void setSp_no(String sp_no) {
		this.sp_no = sp_no;
	}

	public String getSp_refund_no() {
		return sp_refund_no;
	}

	public void setSp_refund_no(String sp_refund_no) {
		this.sp_refund_no = sp_refund_no;
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

	@Override
	public String toString() {
		return "BaiduRefundBackBaen [bfb_order_no=" + bfb_order_no + ", cashback_amount=" + cashback_amount
				+ ", order_no=" + order_no + ", ret_code=" + ret_code + ", ret_detail=" + ret_detail + ", sp_no="
				+ sp_no + ", sp_refund_no=" + sp_refund_no + ", sign=" + sign + ", sign_method=" + sign_method + "]";
	}

}
