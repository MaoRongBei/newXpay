package com.hrtpayment.xpay.cib.bean;


import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import com.hrtpayment.xpay.utils.crypto.Md5Util;

@XmlRootElement(name="xml")
public class CibMsg {
	private static JAXBContext jc;
	static {
		try {
			jc = JAXBContext.newInstance(CibMsg.class);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	public static CibMsg parseXmlFromStr(String str){
		CibMsg msg = null;
		try {
			Unmarshaller us = jc.createUnmarshaller();
			msg = (CibMsg) us.unmarshal(new StringReader(str));
		} catch (JAXBException e) {
		}
		return msg;
	}
	private String appid;
	private String attach;
	private String auth_code;
	private String bank_billno;
	private String bank_type;
	private String body;
	private String buyer_logon_id;
	private String buyer_pay_amount;
	private String buyer_user_id;
	private String callback_url;
	private String charset;
	private String code_img_url;
	private String code_url;
	private String coupon_fee;
	private String device_info;
	private String err_code;
	private String err_msg;
	private String fee_type;
	private String fund_bill_list;
	private String goods_tag;
	private String invoice_amount;
	private String is_raw;
	private String is_subscribe;
	private String mch_create_ip;
	private String mch_id;
	private String message;
	private String need_query;
	private String nonce_str;
	private String notify_url;
	private String op_user_id;
	private String openid;
	private String out_trade_no;
	private String out_transaction_id;
	private String pay_info;
	private String pay_result;
	private String point_amount;
	private String product_id;
	private String receipt_amount;
	private String result_code;
	private String scene;
	private String service;
	private String sign;
	private String sign_type;
	private String status;
	private String sub_appid;
	private String sub_is_subscribe;
	private String sub_openid;
	private String time_end;
	private String time_expire;
	private String time_start;
	private String token_id;
	private String total_fee;
	private String trade_state;
	private String trade_type;
	private String transaction_id;
	private String uuid;
	private String version;
	private String bill_type;
	private String bill_date;
	private String method;
	private String spbill_create_ip;
	private String return_code;
	private String return_msg;
	private String pass_trade_no;
	private String wx_appid;
	private String sign_agentno;
	


	public String calSign(String key) {
		StringBuilder s = new StringBuilder();
		if(null!=appid){s.append("appid").append("=").append(appid).append("&");}
		if(null!=attach){s.append("attach").append("=").append(attach).append("&");}
		if(null!=auth_code){s.append("auth_code").append("=").append(auth_code).append("&");}
		if(null!=bank_billno){s.append("bank_billno").append("=").append(bank_billno).append("&");}
		if(null!=bank_type){s.append("bank_type").append("=").append(bank_type).append("&");}
		if(null!=bill_date){s.append("bill_date").append("=").append(bill_date).append("&");}
		if(null!=bill_type){s.append("bill_type").append("=").append(bill_type).append("&");}
		if(null!=body){s.append("body").append("=").append(body).append("&");}
		if(null!=buyer_logon_id){s.append("buyer_logon_id").append("=").append(buyer_logon_id).append("&");}
		if(null!=buyer_pay_amount){s.append("buyer_pay_amount").append("=").append(buyer_pay_amount).append("&");}
		if(null!=buyer_user_id){s.append("buyer_user_id").append("=").append(buyer_user_id).append("&");}
		if(null!=callback_url){s.append("callback_url").append("=").append(callback_url).append("&");}
		if(null!=charset){s.append("charset").append("=").append(charset).append("&");}
		if(null!=code_img_url){s.append("code_img_url").append("=").append(code_img_url).append("&");}
		if(null!=code_url){s.append("code_url").append("=").append(code_url).append("&");}
		if(null!=coupon_fee){s.append("coupon_fee").append("=").append(coupon_fee).append("&");}
		if(null!=device_info){s.append("device_info").append("=").append(device_info).append("&");}
		if(null!=err_code){s.append("err_code").append("=").append(err_code).append("&");}
		if(null!=err_msg){s.append("err_msg").append("=").append(err_msg).append("&");}
		if(null!=fee_type){s.append("fee_type").append("=").append(fee_type).append("&");}
		if(null!=fund_bill_list){s.append("fund_bill_list").append("=").append(fund_bill_list).append("&");}
		if(null!=goods_tag){s.append("goods_tag").append("=").append(goods_tag).append("&");}
		if(null!=is_raw){s.append("is_raw").append("=").append(is_raw).append("&");}
		if(null!=is_subscribe){s.append("is_subscribe").append("=").append(is_subscribe).append("&");}
		if(null!=invoice_amount){s.append("invoice_amount").append("=").append(invoice_amount).append("&");}
		if(null!=mch_create_ip){s.append("mch_create_ip").append("=").append(mch_create_ip).append("&");}
		if(null!=mch_id){s.append("mch_id").append("=").append(mch_id).append("&");}
		if(null!=message){s.append("message").append("=").append(message).append("&");}
		if(null!=method){s.append("method").append("=").append(method).append("&");}
		if(null!=need_query){s.append("need_query").append("=").append(need_query).append("&");}
		if(null!=nonce_str){s.append("nonce_str").append("=").append(nonce_str).append("&");}
		if(null!=notify_url){s.append("notify_url").append("=").append(notify_url).append("&");}
		if(null!=op_user_id){s.append("op_user_id").append("=").append(op_user_id).append("&");}
		if(null!=openid){s.append("openid").append("=").append(openid).append("&");}
		if(null!=out_trade_no){s.append("out_trade_no").append("=").append(out_trade_no).append("&");}
		if(null!=out_transaction_id){s.append("out_transaction_id").append("=").append(out_transaction_id).append("&");}
		if(null!=pass_trade_no){s.append("pass_trade_no").append("=").append(pass_trade_no).append("&");}
		if(null!=pay_info){s.append("pay_info").append("=").append(pay_info).append("&");}
		if(null!=pay_result){s.append("pay_result").append("=").append(pay_result).append("&");}
		if(null!=point_amount){s.append("point_amount").append("=").append(point_amount).append("&");}
		if(null!=product_id){s.append("product_id").append("=").append(product_id).append("&");}
		if(null!=receipt_amount){s.append("receipt_amount").append("=").append(receipt_amount).append("&");}
		if(null!=result_code){s.append("result_code").append("=").append(result_code).append("&");}
		if(null!=return_code){s.append("return_code").append("=").append(return_code).append("&");}
		if(null!=return_msg){s.append("return_msg").append("=").append(return_msg).append("&");}
		if(null!=scene){s.append("scene").append("=").append(scene).append("&");}
		if(null!=service){s.append("service").append("=").append(service).append("&");}
		//if(null!=sign){s.append("sign").append("=").append(sign).append("&");}
		if(null!=sign_agentno){s.append("sign_agentno").append("=").append(sign_agentno).append("&");}
		if(null!=sign_type){s.append("sign_type").append("=").append(sign_type).append("&");}
		if(null!=spbill_create_ip){s.append("spbill_create_ip").append("=").append(spbill_create_ip).append("&");}
		if(null!=status){s.append("status").append("=").append(status).append("&");}
		if(null!=sub_appid){s.append("sub_appid").append("=").append(sub_appid).append("&");}
		if(null!=sub_is_subscribe){s.append("sub_is_subscribe").append("=").append(sub_is_subscribe).append("&");}
		if(null!=sub_openid){s.append("sub_openid").append("=").append(sub_openid).append("&");}
		if(null!=time_end){s.append("time_end").append("=").append(time_end).append("&");}
		if(null!=time_expire){s.append("time_expire").append("=").append(time_expire).append("&");}
		if(null!=time_start){s.append("time_start").append("=").append(time_start).append("&");}
		if(null!=token_id){s.append("token_id").append("=").append(token_id).append("&");}
		if(null!=total_fee){s.append("total_fee").append("=").append(total_fee).append("&");}
		if(null!=trade_state){s.append("trade_state").append("=").append(trade_state).append("&");}
		if(null!=trade_type){s.append("trade_type").append("=").append(trade_type).append("&");}
		if(null!=transaction_id){s.append("transaction_id").append("=").append(transaction_id).append("&");}
		if(null!=uuid){s.append("uuid").append("=").append(uuid).append("&");}
		if(null!=version){s.append("version").append("=").append(version).append("&");}
		if(null!=wx_appid){s.append("wx_appid").append("=").append(wx_appid).append("&");}
		s.append("key=").append(key);
		String sign = Md5Util.digestUpperHex(s.toString());
		return sign==null?sign:sign.toUpperCase();
	}
	
	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}

	public String getAttach() {
		return attach;
	}
	public String getBank_billno() {
		return bank_billno;
	}
	public String getBank_type() {
		return bank_type;
	}
	public String getBody() {
		return body;
	}
	public String getCallback_url() {
		return callback_url;
	}
	public String getCharset() {
		return charset;
	}
	public String getCode_img_url() {
		return code_img_url;
	}
	public String getCode_url() {
		return code_url;
	}
	public String getCoupon_fee() {
		return coupon_fee;
	}
	public String getDevice_info() {
		return device_info;
	}
	public String getErr_code() {
		return err_code;
	}

	public String getFee_type() {
		return fee_type;
	}
	public String getGoods_tag() {
		return goods_tag;
	}
	public String getIs_raw() {
		return is_raw;
	}
	public String getIs_subscribe() {
		return is_subscribe;
	}
	public String getMch_create_ip() {
		return mch_create_ip;
	}
	public String getMch_id() {
		return mch_id;
	}
	public String getMessage() {
		return message;
	}
	public String getNonce_str() {
		return nonce_str;
	}
	public String getNotify_url() {
		return notify_url;
	}
	public String getOp_user_id() {
		return op_user_id;
	}
	public String getOpenid() {
		return openid;
	}
	public String getOut_trade_no() {
		return out_trade_no;
	}
	public String getOut_transaction_id() {
		return out_transaction_id;
	}
	public String getPay_info() {
		return pay_info;
	}
	public String getPay_result() {
		return pay_result;
	}
	public String getProduct_id() {
		return product_id;
	}
	public String getResult_code() {
		return result_code;
	}
	public String getService() {
		return service;
	}
	public String getSign() {
		return sign;
	}
	public String getSign_type() {
		return sign_type;
	}
	public String getStatus() {
		return status;
	}
	public String getSub_appid() {
		return sub_appid;
	}
	public String getSub_is_subscribe() {
		return sub_is_subscribe;
	}
	public String getSub_openid() {
		return sub_openid;
	}
	public String getTime_end() {
		return time_end;
	}
	public String getTime_expire() {
		return time_expire;
	}
	public String getTime_start() {
		return time_start;
	}
	public String getToken_id() {
		return token_id;
	}
	public String getTotal_fee() {
		return total_fee;
	}
	public String getTrade_state() {
		return trade_state;
	}
	public String getTrade_type() {
		return trade_type;
	}
	public String getTransaction_id() {
		return transaction_id;
	}
	public String getVersion() {
		return version;
	}
	public void setAttach(String attach) {
		this.attach = attach;
	}
	public void setBank_billno(String bank_billno) {
		this.bank_billno = bank_billno;
	}
	public void setBank_type(String bank_type) {
		this.bank_type = bank_type;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public void setCallback_url(String callback_url) {
		this.callback_url = callback_url;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	public void setCode_img_url(String code_img_url) {
		this.code_img_url = code_img_url;
	}
	public void setCode_url(String code_url) {
		this.code_url = code_url;
	}
	public void setCoupon_fee(String coupon_fee) {
		this.coupon_fee = coupon_fee;
	}
	public void setDevice_info(String device_info) {
		this.device_info = device_info;
	}
	public void setErr_code(String err_code) {
		this.err_code = err_code;
	}
	public void setFee_type(String fee_type) {
		this.fee_type = fee_type;
	}
	public void setGoods_tag(String goods_tag) {
		this.goods_tag = goods_tag;
	}
	public void setIs_raw(String is_raw) {
		this.is_raw = is_raw;
	}
	public void setIs_subscribe(String is_subscribe) {
		this.is_subscribe = is_subscribe;
	}
	public void setMch_create_ip(String mch_create_ip) {
		this.mch_create_ip = mch_create_ip;
	}
	public void setMch_id(String mch_id) {
		this.mch_id = mch_id;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public void setNonce_str(String nonce_str) {
		this.nonce_str = nonce_str;
	}
	public void setNotify_url(String notify_url) {
		this.notify_url = notify_url;
	}
	public void setOp_user_id(String op_user_id) {
		this.op_user_id = op_user_id;
	}
	public void setOpenid(String openid) {
		this.openid = openid;
	}
	public void setOut_trade_no(String out_trade_no) {
		this.out_trade_no = out_trade_no;
	}
	public void setOut_transaction_id(String out_transaction_id) {
		this.out_transaction_id = out_transaction_id;
	}
	public void setPay_info(String pay_info) {
		this.pay_info = pay_info;
	}
	public void setPay_result(String pay_result) {
		this.pay_result = pay_result;
	}
	public void setProduct_id(String product_id) {
		this.product_id = product_id;
	}
	public void setResult_code(String result_code) {
		this.result_code = result_code;
	}
	public void setService(String service) {
		this.service = service;
	}
	public void setSign(String sign) {
		this.sign = sign;
	}
	public void setSign_type(String sign_type) {
		this.sign_type = sign_type;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setSub_appid(String sub_appid) {
		this.sub_appid = sub_appid;
	}
	public void setSub_is_subscribe(String sub_is_subscribe) {
		this.sub_is_subscribe = sub_is_subscribe;
	}
	public void setSub_openid(String sub_openid) {
		this.sub_openid = sub_openid;
	}
	public void setTime_end(String time_end) {
		this.time_end = time_end;
	}
	public void setTime_expire(String time_expire) {
		this.time_expire = time_expire;
	}
	public void setTime_start(String time_start) {
		this.time_start = time_start;
	}
	public void setToken_id(String token_id) {
		this.token_id = token_id;
	}
	public void setTotal_fee(String total_fee) {
		this.total_fee = total_fee;
	}
	public void setTrade_state(String trade_state) {
		this.trade_state = trade_state;
	}
	public void setTrade_type(String trade_type) {
		this.trade_type = trade_type;
	}

	
	public void setTransaction_id(String transaction_id) {
		this.transaction_id = transaction_id;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getAuth_code() {
		return auth_code;
	}

	public void setAuth_code(String auth_code) {
		this.auth_code = auth_code;
	}

	public String getBill_type() {
		return bill_type;
	}

	public void setBill_type(String bill_type) {
		this.bill_type = bill_type;
	}

	public String getBill_date() {
		return bill_date;
	}

	public void setBill_date(String bill_date) {
		this.bill_date = bill_date;
	}
	public String getNeed_query() {
		return need_query;
	}

	public void setNeed_query(String need_query) {
		this.need_query = need_query;
	}
	public String getBuyer_logon_id() {
		return buyer_logon_id;
	}

	public void setBuyer_logon_id(String buyer_logon_id) {
		this.buyer_logon_id = buyer_logon_id;
	}

	public String getBuyer_pay_amount() {
		return buyer_pay_amount;
	}

	public void setBuyer_pay_amount(String buyer_pay_amount) {
		this.buyer_pay_amount = buyer_pay_amount;
	}

	public String getBuyer_user_id() {
		return buyer_user_id;
	}

	public void setBuyer_user_id(String buyer_user_id) {
		this.buyer_user_id = buyer_user_id;
	}

	public String getFund_bill_list() {
		return fund_bill_list;
	}

	public void setFund_bill_list(String fund_bill_list) {
		this.fund_bill_list = fund_bill_list;
	}
	public String getInvoice_amount() {
		return invoice_amount;
	}
	public void setInvoice_amount(String invoice_amount) {
		this.invoice_amount = invoice_amount;
	}
	public String getPoint_amount() {
		return point_amount;
	}
	public void setPoint_amount(String point_amount) {
		this.point_amount = point_amount;
	}
	public String getReceipt_amount() {
		return receipt_amount;
	}
	public void setReceipt_amount(String receipt_amount) {
		this.receipt_amount = receipt_amount;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getSpbill_create_ip() {
		return spbill_create_ip;
	}
	public void setSpbill_create_ip(String spbill_create_ip) {
		this.spbill_create_ip = spbill_create_ip;
	}
	public String getReturn_code() {
		return return_code;
	}
	public void setReturn_code(String return_code) {
		this.return_code = return_code;
	}
	public String getReturn_msg() {
		return return_msg;
	}
	public void setReturn_msg(String return_msg) {
		this.return_msg = return_msg;
	}
	public String getErr_msg() {
		return err_msg;
	}

	public void setErr_msg(String err_msg) {
		this.err_msg = err_msg;
	}

	public String getScene() {
		return scene;
	}
	public void setScene(String scene) {
		this.scene = scene;
	}
	public String getWx_appid() {
		return wx_appid;
	}
	public void setWx_appid(String wx_appid) {
		this.wx_appid = wx_appid;
	}

	public String getSign_agentno() {
		return sign_agentno;
	}

	public void setSign_agentno(String sign_agentno) {
		this.sign_agentno = sign_agentno;
	}

	public String toXmlString(){
		String str = null;
		try {
			Marshaller ms = jc.createMarshaller();
			ms.setProperty(Marshaller.JAXB_FRAGMENT, true);
			StringWriter writer = new StringWriter();
			ms.marshal(this, writer);
			str = writer.toString();
		} catch (JAXBException e) {
		}
		return str;
		
	}

	
}
