package com.hrtpayment.xpay.baidu.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.StringUtils;
import com.hrtpayment.xpay.baidu.bean.BaiduOrderBean;
import com.hrtpayment.xpay.baidu.bean.BaiduQrPayBean;
import com.hrtpayment.xpay.baidu.bean.BaiduQueryBean;
import com.hrtpayment.xpay.baidu.bean.BaiduRefundBean;
import com.hrtpayment.xpay.baidu.bean.BaiduRefundCallBackBaen;
import com.hrtpayment.xpay.baidu.bean.BaiduRefundQueryBean;
import com.hrtpayment.xpay.baidu.util.KeyUtil;
import com.hrtpayment.xpay.utils.UrlCodec;
import com.hrtpayment.xpay.utils.exception.BusinessException;

@Service
public class BaiduService {
	private final Logger logger = LogManager.getLogger();

	@Value("${baidu.key}")
	private String key;

	@Value("${baidu.notifyUrl}")
	private String notifyUrl;

	@Value("${baidu.refundBackUrl}")
	private String refundBackUrl;

	/**
	 * 获取百度条码付-发送消息体
	 * 
	 * @param orderid
	 * @param authCode
	 * @param totalAmount
	 * @param subject
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedEncodingException
	 */
	public Map<String, String> getBarCPReqMessage(String orderid, String authCode, BigDecimal totalAmount,
			String subject, String merchantCode)
			throws IllegalArgumentException, IllegalAccessException, UnsupportedEncodingException {
		BaiduOrderBean retBean = new BaiduOrderBean();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar now = Calendar.getInstance();
		
		retBean.setPay_code(authCode);
		retBean.setService_code(1);
		retBean.setSp_no(merchantCode);
		retBean.setOrder_create_time(sdf.format(now.getTime()));
		retBean.setOrder_no(orderid);
		retBean.setGoods_name(new String(subject.getBytes("GBK"), "GBK"));// 转码gbk
		retBean.setTotal_amount(totalAmount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
		retBean.setCurrency("1"); // 人民币
		retBean.setReturn_url(notifyUrl);
//		now.add(Calendar.DAY_OF_MONTH, 1);
//		retBean.setExpire_time(sdf.format(now.getTime()));
		retBean.setInput_charset(1);
		retBean.setVersion("2");
		retBean.setSign_method("1");
		String sign = KeyUtil.makeSgin(retBean.getPostMap(), key);
		retBean.setSign(sign);
		//对中文编码转码url
		retBean.setGoods_name(UrlCodec.encode(new String(subject.getBytes("GBK"), "GBK"), "GBK"));
		retBean.setReturn_url(UrlCodec.encode(new String(notifyUrl.getBytes("GBK"), "GBK"),"GBK"));
		
		return retBean.getPostMap();
	}

	public Map<String, String> getQrReqMessage(String subject, BigDecimal amount, String orderid, String merchantCode)
			throws UnsupportedEncodingException, IllegalArgumentException, IllegalAccessException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar now = Calendar.getInstance();

		BaiduQrPayBean retBean = new BaiduQrPayBean();
		retBean.setService_code("1");
		retBean.setSp_no(merchantCode);
		retBean.setOrder_create_time(sdf.format(now.getTime()));
		retBean.setOrder_no(orderid);
		retBean.setGoods_name(new String(subject.getBytes("GBK"), "GBK"));
		//以下三项可选
//		retBean.setUnit_amount(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
//		retBean.setUnit_count(1);
//		retBean.setTransport_amount(0);

		retBean.setTotal_amount(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
		retBean.setCurrency("1"); // 人民币
		retBean.setReturn_url(notifyUrl);
		retBean.setPay_type("1"); //默认支付方式1 余额支付
//		retBean.setBank_no("201");
//		now.add(Calendar.DAY_OF_MONTH, 1);
//		retBean.setExpire_time(sdf.format(now.getTime()));
		retBean.setInput_charset("1");
		retBean.setVersion("2");
		retBean.setSign_method("1");
		retBean.setOutput_type("1"); // 0：image【默认值】；1：json；

		String sign = KeyUtil.makeSgin(retBean.getPostMap(false), key);
		retBean.setSign(sign);
		//url转码
		retBean.setGoods_name(UrlCodec.encode(new String(subject.getBytes("GBK"), "GBK"), "GBK"));
		retBean.setReturn_url(UrlCodec.encode(new String(notifyUrl.getBytes("GBK"), "GBK"), "GBK"));
		
		return retBean.getPostMap(true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean verifyCallback(Map map) {
		try {
			String oriSign=map.get("sign").toString();
			// 1.生成sign
			String sign = KeyUtil.verifySgin(map, key);
			// 2.判断sign是否相同
			if (StringUtils.isEmpty(sign) || !sign.equalsIgnoreCase(oriSign)) {
				logger.info("收到验签："+oriSign+"本地验签："+sign);
				return false;
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("验签失败！");
		}
		return true;
	}

	public Map<String, String> getQueryReqMessage(String orderId, String mercode)
			throws IllegalArgumentException, IllegalAccessException {
		BaiduQueryBean bean = new BaiduQueryBean();
		bean.setSp_no(mercode);
		bean.setOrder_no(orderId);
		bean.setInput_charset("1");
		bean.setVersion("2");
		bean.setSign_method("1");
		String sign = KeyUtil.makeSgin(bean.getPostMap(), key);
		bean.setSign(sign);
		return bean.getPostMap();
	}

	/**
	 * 获取退款请求参数
	 * 
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws UnsupportedEncodingException 
	 */
	public Map<String, String> getRefundReqMsg(String merCode, String orderId, String oriOrderId, BigDecimal amount) throws IllegalArgumentException, IllegalAccessException, UnsupportedEncodingException
			  {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar now = Calendar.getInstance();

		BaiduRefundBean bean = new BaiduRefundBean();
		bean.setService_code("2");
		bean.setInput_charset("1");// gbk
		bean.setCurrency("1"); // rmb
		bean.setSign_method("1"); //1 md5
		bean.setReturn_method("1"); // 1get 2 post
		bean.setReturn_url(refundBackUrl);
		bean.setVersion("2");//4
		bean.setSp_no(merCode);
		bean.setOrder_no(oriOrderId);
		bean.setOutput_type("1"); // 1 xml
		bean.setOutput_charset("1");// gbk
		bean.setCashback_amount(amount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
		bean.setCashback_time(sdf.format(now.getTime()));
		bean.setSp_refund_no(orderId);
		// 做验签
		String sign = KeyUtil.makeSgin(bean.getSignMap(), key);
		bean.setSign(sign);
		
		bean.setReturn_url(UrlCodec.encode(new String(refundBackUrl.getBytes("GBK"), "GBK"), "GBK"));
		return bean.getSignMap();
	}

	public Map<String, String> getRefundQueryMsg(Map<String, Object> map) throws IllegalArgumentException, IllegalAccessException {
		BaiduRefundQueryBean bean =new BaiduRefundQueryBean();
		bean.setService_code("12");
		bean.setSp_no(map.get("BANKMID").toString());
		bean.setOrder_no(map.get("ORI_MER_ORDERID").toString());//订单号 即  原交易的 订单号
		bean.setSp_refund_no(map.get("MER_ORDERID").toString());//流水号 即为 退款订单的 订单号
		bean.setOutput_type("1");
		bean.setOutput_charset("1");
		bean.setVersion("2");
		bean.setSign_method("1");
		
		String sign=KeyUtil.makeSgin(bean.getSignMap(), key);
		bean.setSign(sign);
		return bean.getSignMap();
	}

	public boolean verifyRefundSign(BaiduRefundCallBackBaen refundback, String queryStr) throws BusinessException {
		try {
			// 1.1解决乱码问题，把经过decode的赋值到ret_detail
			String[] resultStrTemp = queryStr.split("&");// 把返回的参数用split取出来
			Map<String, String> map = new LinkedHashMap<>();
			for (String string : resultStrTemp) {
				String[] arr = string.split("=");
				if (arr.length > 1)
					map.put(arr[0], arr[1]);
				else
					map.put(arr[0], "");
			}
			refundback.setRet_detail(map.get("ret_detail"));
			// 1.2比对验签
			String sign = KeyUtil.makeSgin(refundback.getSignMap(), key);
			if (!sign.equalsIgnoreCase(refundback.getSign())) {
				logger.info("本地签名串     ：" + sign + "百度钱包返回签名串：" + refundback.getSign());
				return false;
			}
		} catch (Exception e) {
			throw new BusinessException(9004, "签名校验失败");
		}
		return true;
	}

}
