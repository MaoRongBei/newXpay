package com.hrtpayment.xpay.baidu.bean;

/**
 * @author lvjianyu 生成二维码图片实体
 */
public class BaseBaiduQrPayBean {
	/* 基本参数，不参与签名 */
	private String code_type;// 码类型(不参与签名) 整数；目前必须为 0；默认值：0 否
	private String code_size;// 码大小(不参与签名) 取值范围：1-10；默认值：2 否
	private String output_type;// 输出格式(不参与签名) 0：image【默认值】；1：json； 否
	private String nologo; // 不带百度钱包 LOGO(不参与签名) 0：带 LOGO【默认值】； 1：不带 LOGO 否
	private String mno; // 实体商户门店号 (不参与签名) 取值范围参见附录 否
	private String mname;// 实体商户门店名称 (不参与签名) 取值范围参见附录 否
	private String tno; // 实体商户终端号 (不参与签名) 取值范围参见附录 否

	public String getCode_type() {
		return code_type;
	}

	public void setCode_type(String code_type) {
		this.code_type = code_type;
	}

	public String getCode_size() {
		return code_size;
	}

	public void setCode_size(String code_size) {
		this.code_size = code_size;
	}

	public String getOutput_type() {
		return output_type;
	}

	public void setOutput_type(String output_type) {
		this.output_type = output_type;
	}

	public String getNologo() {
		return nologo;
	}

	public void setNologo(String nologo) {
		this.nologo = nologo;
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

}
