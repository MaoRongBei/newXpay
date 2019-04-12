package com.hrtpayment.xpay.common.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.common.service.WxpayService;
import com.hrtpayment.xpay.utils.UrlCodec;
import com.hrtpayment.xpay.utils.exception.HrtBusinessException;

/**
 * 微信公众号支付相关服务
 * @author aibing
 * 2016年11月22日
 */
@Service
public class WechatService {
	@Autowired
	private JdbcDao dao;
	Logger logger = LogManager.getLogger();
	@Autowired
	NettyClientService client;
	@Resource(name="bcmPayService")
	WxpayService bcm;
	@Resource(name="cupsATPayService")
	WxpayService cupsWx;
	@Resource(name="netCupsPayService")
	WxpayService netCupsWx;
	@Resource(name="cibPayService")
	WxpayService cib;

	
	
	@Value("${xpay.host}")
	private String host;
	
	@Value("${xpay.host2}")
	private String host2;
	/**
	 * 插入公众号订单信息
	 * @param unno
	 * @param mid
	 * @param orderid
	 * @param subject
	 * @param amount
	 * @param merchantCode
	 * @param limit_pay
	 * @return
	 */
	public int insertPubaccOrder(int fiid,String unno,String mid,String orderid,String subject,BigDecimal amount,String merchantCode,String limit_pay) {
		WxpayService wx = getWxpayService(fiid);
		//插入订单信息
		List<Map<String, Object>> list = dao.queryForList("select * from pg_wechat_txn t where t.mer_orderid=?", orderid);
		if (list.size()>0) {
			throw new HrtBusinessException(9007);
		}
		String sql = "insert into pg_wechat_txn (pwid,fiid, txntype,cdate,status,"
				+ "mer_orderid, detail, txnamt, mer_id,unno,bankmid,bank_type) values"
				+ "(S_PG_Wechat_Txn.nextval,?,'0',sysdate,'A',?,?,?,?,?,?,?)";
		int n = dao.update(sql, wx.getWxpayFiid(), orderid, subject, amount, mid, unno,merchantCode,"no_credit".equals(limit_pay)?"no_credit":null);
		return n;
	}
	/**
	 * 获取公众号订单支付地址-调用
	 * @param fiid
	 * @param orderid
	 * @return
	 */
	public String getPubAccPayUrl(int fiid, String orderid,String isCredit){
		//1.0
//		return String.format("%s/xpay/wxauth_%s_%s", host, fiid, orderid);
		
		//2.0
//		WxpayService wx = getWxpayService(fiid);
//		String codeUrl = String.format("%s/xpay/wxpay_%s_%s", host, wx.getWxpayFiid(), orderid);
//		String authUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s"
//				+ "&response_type=code&scope=snsapi_base&state=STATE#wechat_redirect";
//		String url = String.format(authUrl, wx.getWxpayAppid(), UrlCodec.encodeWithUtf8(codeUrl));
//		return url;
		
		//3.0
		WxpayService wx = getWxpayService(fiid);
		String codeUrl = String.format("%s/xpay/wxpay_%s_%s_%s%s", host, wx.getWxpayFiid(), orderid,isCredit,host2);
		String authUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=STATE#wechat_redirect";
		String url = String.format(authUrl, wx.getWxpayAppid(), UrlCodec.encodeWithUtf8(codeUrl));
		return url;	
	}
	
	/**
	 * 获取公众号订单支付地址
	 * @param fiid
	 * @param orderid
	 * @return
	 */
	public String getPubaccPayUrl(int fiid, String orderid,String isCredit){
		return getPubAccPayUrl(fiid, orderid, isCredit);
	}
	
	/**
	 * 拼接微信授权重定向地址
	 * @param payUrl
	 * @return
	 */
	public String getWxAuthUrl(int fiid, String orderid){
		WxpayService wx = getWxpayService(fiid);
		String codeUrl = String.format("%s/xpay/wxpay_%s_%s%s", host, wx.getWxpayFiid(), orderid,host2);
		String authUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=STATE#wechat_redirect";
		String url = String.format(authUrl, wx.getWxpayAppid(), UrlCodec.encodeWithUtf8(codeUrl));
		return url;
	}

	public String getOpenid(int fiid,String code){
		WxpayService wx = getWxpayService(fiid);
		String url = String.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code", 
				wx.getWxpayAppid(),wx.getWxpaySecret(),code);
		
//		String url = String.format("https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code", 
//				"wx66201eed90c89d31","069f1db0f9833597771b1ed0b353f040",code);
		String resp = null;
		JSONObject json;
		try {
			resp = client.sendGetRequest(url);
			json = JSONObject.parseObject(resp);
		} catch (Exception e) {
			logger.error(e);
			throw new HrtBusinessException(8000, "获取openid出错");
		}
		if (json == null || !json.containsKey("openid")) {
			logger.info(resp);
			throw new HrtBusinessException(8000, "获取openid失败");
		}
		return json.getString("openid");
	}
	
	public String getJsPayInfo(int fiid, String orderid, String openid,String isCredit) {
		return getWxpayService(fiid).getWxpayPayInfo(orderid, openid,isCredit);
	}
	
	private WxpayService getWxpayService (int fiid) {
		if (fiid == 34){
			return cib; 
		} else if (fiid == 46){
			return bcm;
		} else if (fiid == 53) {
			return cupsWx ;
		} else if (fiid == 60) {
			return netCupsWx ;
		} else {
			throw new HrtBusinessException(9009,"指定通道未开通");
		}
	}
}
