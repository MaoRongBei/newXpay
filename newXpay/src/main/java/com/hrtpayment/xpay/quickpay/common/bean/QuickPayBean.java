package com.hrtpayment.xpay.quickpay.common.bean;

import javax.xml.bind.annotation.XmlRootElement;

import com.hrtpayment.xpay.utils.crypto.Md5Util;

@XmlRootElement(name="xml")
public class QuickPayBean {

	
	private String mid;
	private String accNo;
	private String amount;
	private String mobile;
	
	private String status;
	private String htnlStatus;
	private  String msg;
	private String rtnHtml;
	
	private String bankName;
	private String bankNo;
	private String fiid;
	private String accName;
	private String idCard;
	
	private String tid ;
	private String unno;
	
	private String qpcid;
	private String isPoint;
	private String orderTime;
	
	private String orderId;
	
	
	private String preSerial;
	private String smsCode;
	private String cvn;
	private String effective;
	
	private String sign;
	
	private String bankmid;
	
	
	private String position;
	
	private String deviceId;
	
	
	
	
	public String getBankmid() {
		return bankmid;
	}
	public void setBankmid(String bankmid) {
		this.bankmid = bankmid;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public String getSign() {
		return sign;
	}
	public void setSign(String sign) {
		this.sign = sign;
	}
	public String getHtnlStatus() {
		return htnlStatus;
	}
	public void setHtnlStatus(String htnlStatus) {
		this.htnlStatus = htnlStatus;
	}
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public String getOrderTime() {
		return orderTime;
	}
	public void setOrderTime(String orderTime) {
		this.orderTime = orderTime;
	}
	public String getIsPoint() {
		return isPoint;
	}
	public void setIsPoint(String isPoint) {
		this.isPoint = isPoint;
	}
	public String getQpcid() {
		return qpcid;
	}
	public void setQpcid(String qpcid) {
		this.qpcid = qpcid;
	}
	public String getIdCard() {
		return idCard;
	}
	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}
	public String getTid() {
		return tid;
	}
	public void setTid(String tid) {
		this.tid = tid;
	}
	public String getUnno() {
		return unno;
	}
	public void setUnno(String unno) {
		this.unno = unno;
	}
	public String getBankName() {
		return bankName;
	}
	public void setBankName(String bankName) {
		this.bankName = bankName;
	}
	public String getBankNo() {
		return bankNo;
	}
	public void setBankNo(String bankNo) {
		this.bankNo = bankNo;
	}
	public String getFiid() {
		return fiid;
	}
	public void setFiid(String fiid) {
		this.fiid = fiid;
	}
	public String getAccName() {
		return accName;
	}
	public void setAccName(String accName) {
		this.accName = accName;
	}
	public String getMid() {
		return mid;
	}
	public void setMid(String mid) {
		this.mid = mid;
	}
	public String getAccNo() {
		return accNo;
	}
	public void setAccNo(String accNo) {
		this.accNo = accNo;
	}
	public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getRtnHtml() {
		return rtnHtml;
	}
	public void setRtnHtml(String rtnHtml) {
		this.rtnHtml = rtnHtml;
	}
	public String getPreSerial() {
		return preSerial;
	}
	public void setPreSerial(String preSerial) {
		this.preSerial = preSerial;
	}
	public String getSmsCode() {
		return smsCode;
	}
	public void setSmsCode(String smsCode) {
		this.smsCode = smsCode;
	}
	public String getCvn() {
		return cvn;
	}
	public void setCvn(String cvn) {
		this.cvn = cvn;
	}
	public String getEffective() {
		return effective;
	}
	public void setEffective(String effective) {
		this.effective = effective;
	}
	
	public String getPosition() {
		return position;
	}
	public void setPosition(String position) {
		this.position = position;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	/**
	 * 
	 * @param key
	 * @return
	 */
	public String calSign(String key) {
		StringBuilder sb = new StringBuilder();

		if (accName!=null) {
			sb.append("accName=").append(accName).append("&");
		}
		if (accNo!=null) {
			sb.append("accNo=").append(accNo).append("&");
		}
		if (amount!=null ) {
			sb.append("amount=").append(amount).append("&");
		}
		if (bankName!=null && bankName.length()>0) {
			sb.append("bankName=").append(bankName).append("&");
		}
		if (bankNo!=null && bankNo.length()>0) {
			sb.append("bankNo=").append(bankNo).append("&");
		}
		if (cvn!=null && cvn.length()>0) {
			sb.append("cvn=").append(cvn).append("&");
		}
		if (effective!=null && effective.length()>0) {
			sb.append("effective=").append(effective).append("&");
		}
		if (fiid!=null && fiid.length()>0) {
			sb.append("fiid=").append(fiid).append("&");
		}
		if (htnlStatus!=null && htnlStatus.length()>0) {
			sb.append("htnlStatus=").append(htnlStatus).append("&");
		}
		if (idCard!=null && idCard.length()>0) {
			sb.append("idCard=").append(idCard).append("&");
		}
		if (isPoint!=null && isPoint.length()>0) {
			sb.append("isPoint=").append(isPoint).append("&");
		}
		if (mid!=null && mid.length()>0) {
			sb.append("mid=").append(mid).append("&");
		}
		if(mobile!=null && mobile.length()>0) {
			sb.append("mobile=").append(mobile).append("&");
		}
		if (msg!=null && msg.length()>0){
			sb.append("msg=").append(msg).append("&");
		}
		if (orderId!=null && orderId.length()>0) {
			sb.append("orderId=").append(orderId).append("&");
		}
		if (orderTime!=null && orderTime.length()>0) {
			sb.append("orderTime=").append(orderTime).append("&");
		}
		if (preSerial!=null && preSerial.length()>0) {
			sb.append("preSerial=").append(preSerial).append("&");
		}
		if(qpcid!=null && qpcid.length()>0) {
			sb.append("qpcid=").append(qpcid).append("&");
		}
		if (rtnHtml!=null && rtnHtml.length()>0){
			sb.append("rtnHtml=").append(rtnHtml).append("&");
		}
		if (smsCode!=null && smsCode.length()>0) {
			sb.append("smsCode=").append(smsCode).append("&");
		}
		if (status!=null && status.length()>0) {
			sb.append("status=").append(status).append("&");
		}
		if (tid!=null && tid.length()>0) {
			sb.append("tid=").append(tid).append("&");
		}
		if (unno!=null && unno.length()>0) {
			sb.append("unno=").append(unno).append("&");
		}
		sb.append("key=").append(key);
		System.out.println(sb.toString());
		String sign = Md5Util.digestUpperHex(sb.toString());
		System.out.println(sign);
		return sign;
	}

}
