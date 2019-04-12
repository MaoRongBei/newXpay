package com.hrtpayment.xpay.quickpay.newCups.bean;
 

import com.hrtpayment.xpay.utils.crypto.Md5Util;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("MsgBody") 
public class CupsQuickPayMsgBodyBean {
	private TrxInfBean TrxInf;
	private RcverInfBean RcverInf;
	private SderInfBean SderInf;
	private OriTrxInfBean OriTrxInf;
	private String BizTp;
	private PyerInfBean PyerInf;
	private PyeeInfBean PyeeInf;
	private OrdrInfBean OrdrInf;
	private MrchntInfBean MrchntInf;
	private ChannelIssrInfBean ChannelIssrInf;
	private BizInfBean BizInf;
	private SysRtnInfBean SysRtnInf;
  
	
	
	public BizInfBean getBizInf() {
		return BizInf;
	}
	public void setBizInf(BizInfBean bizInf) {
		BizInf = bizInf;
	}

	public SysRtnInfBean getSysRtnInf() {
		return SysRtnInf;
	}



	public void setSysRtnInf(SysRtnInfBean sysRtnInf) {
		SysRtnInf = sysRtnInf;
	}



	public MrchntInfBean getMrchntInf() {
		return MrchntInf;
	}



	public void setMrchntInf(MrchntInfBean mrchntInf) {
		MrchntInf = mrchntInf;
	}



	public ChannelIssrInfBean getChannelIssrInf() {
		return ChannelIssrInf;
	}



	public void setChannelIssrInf(ChannelIssrInfBean channelIssrInf) {
		ChannelIssrInf = channelIssrInf;
	}



	public OrdrInfBean getOrdrInf() {
		return OrdrInf;
	}



	public void setOrdrInf(OrdrInfBean ordrInf) {
		OrdrInf = ordrInf;
	}

	public PyerInfBean getPyerInf() {
		return PyerInf;
	}



	public void setPyerInf(PyerInfBean pyerInf) {
		PyerInf = pyerInf;
	}



	public PyeeInfBean getPyeeInf() {
		return PyeeInf;
	}



	public void setPyeeInf(PyeeInfBean pyeeInf) {
		PyeeInf = pyeeInf;
	}



	public OriTrxInfBean getOriTrxInf() {
		return OriTrxInf;
	}



	public void setOriTrxInf(OriTrxInfBean oriTrxInfBean) {
		this.OriTrxInf = oriTrxInfBean;
	}



	public SderInfBean getSderInf() {
		return SderInf;
	}



	public void setSderInf(SderInfBean sderInf) {
		SderInf = sderInf;
	}



	public TrxInfBean getTrxInf() {
		return TrxInf;
	}



	public void setTrxInf(TrxInfBean trxInf) {
		TrxInf = trxInf;
	}



	public RcverInfBean getRcverInf() {
		return RcverInf;
	}



	public void setRcverInf(RcverInfBean rcverInf) {
		RcverInf = rcverInf;
	}



	public String getBizTp() {
		return BizTp;
	}



	public void setBizTp(String bizTp) {
		BizTp = bizTp;
	}



	/**
	 * 
	 * @param key
	 * @return
	 */
	public String calSign(String key) {
		StringBuilder sb = new StringBuilder();

//		if (accName!=null) {
//			sb.append("accName=").append(accName).append("&");
//		}
//		if (accNo!=null) {
//			sb.append("accNo=").append(accNo).append("&");
//		}
	 
		sb.append("key=").append(key);
		System.out.println(sb.toString());
		String sign = Md5Util.digestUpperHex(sb.toString());
		System.out.println(sign);
		return sign;
	}

}
