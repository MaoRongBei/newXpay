package com.hrtpayment.xpay.channel.bean;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;

import com.hrtpayment.xpay.utils.crypto.Md5Util;

@XmlRootElement(name="xml")
public class HrtDeviceTypeInfoBean {
	
	private static JAXBContext jcde;
	static {
		try {
			jcde = JAXBContext.newInstance(HrtDeviceTypeInfoBean.class);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	private String status;
	private String unno;
	private String deviceVersion;
	private String deviceType;
	private String lastVersion;
	private String isForceUpdate;
	private String sign;
	
	
	
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getUnno() {
		return unno;
	}

	public void setUnno(String unno) {
		this.unno = unno;
	}

	public String getDeviceVersion() {
		return deviceVersion;
	}

	public void setDeviceVersion(String deviceVersion) {
		this.deviceVersion = deviceVersion;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getLastVersion() {
		return lastVersion;
	}

	public void setLastVersion(String lastVersion) {
		this.lastVersion = lastVersion;
	}

	public String getIsForceUpdate() {
		return isForceUpdate;
	}

	public void setIsForceUpdate(String isForceUpdate) {
		this.isForceUpdate = isForceUpdate;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public static HrtDeviceTypeInfoBean parseXmlFromStr(String str) throws JAXBException {
		HrtDeviceTypeInfoBean bean = null;
		Unmarshaller us = jcde.createUnmarshaller();
		bean = (HrtDeviceTypeInfoBean) us.unmarshal(new StringReader(str));
		return bean;
	}

	public String toXmlString() throws JAXBException {
		String str = null;
		Marshaller ms = jcde.createMarshaller();
		ms.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		StringWriter writer = new StringWriter();
		ms.marshal(this, writer);
		str = writer.toString();
		return str;
	}
}
