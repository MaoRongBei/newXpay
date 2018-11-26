package com.hrtpayment.xpay.quickpay.cups.bean;
 
 
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("root") 
public class CupsQuickPayRootBean {
    private  CupsQuickPayMsgHeaderBean MsgHeader;
    private  CupsQuickPayMsgBodyBean MsgBody;
	public CupsQuickPayMsgHeaderBean getMsgHeader() {
		return MsgHeader;
	}
	public void setMsgHeader(CupsQuickPayMsgHeaderBean msgHeader) {
		MsgHeader = msgHeader;
	}
	public CupsQuickPayMsgBodyBean getMsgBody() {
		return MsgBody;
	}
	public void setMsgBody(CupsQuickPayMsgBodyBean msgBody) {
		MsgBody = msgBody;
	}
	 
    
    
    
   
}
