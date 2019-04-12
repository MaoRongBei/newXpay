package com.hrtpayment.xpay.cups.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.channel.service.NotifyService;
import com.hrtpayment.xpay.common.dao.JdbcDao;
import com.hrtpayment.xpay.cups.sdk.ApiEncryptUtil;
import com.hrtpayment.xpay.cups.sdk.ApiSignatureUtil;
import com.hrtpayment.xpay.cups.sdk.CommonUtil;

@Service
public class CupsXwPayService {
	private final Logger logger = LogManager.getLogger();

	@Autowired
	JdbcDao dao;
	@Autowired
	NotifyService notify;

//	@Value("${cups.xw.privateKey}")
//	String privateKey;
//	@Value("${cups.wx.publicKey}")
//	String publicKey;
//	@Value("${cups.wx.aesKey}")
//	String aesKey; 
	@Value("${cups.wx.callback.aesKey}")
	String callBackAesKey;
//	@Value("${cups.wx.expandCode}")
//	String expandCode;
//	@Value("${cups.xw.getQrcodeUrl}")
//	String getQrcodeUrl;
//	@Value("${cups.xw.queryQrcodeUrl}")
//	String queryQrcodeUrl;
//	@Value("${cups.xw.sendMessageUrl}")
//	String sendMessageUrl;
//	@Value("${cups.xw.bindCodeUrl}")
//	String bindCodeUrl;
//	@Value("${cups.xw.imgUploadUrl}")
//	String imgUploadUrl;
    
	private String createOrderId() {
		String base = "0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 8; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String orderid = "hrt" + sdf.format(d) + sb.toString()+"unionpay";
		return orderid;
	}
	
	public void callBack(String jsonNotifyStr){
		
		   Map<String, String> notifyMap = CommonUtil.jsonStr2Map(jsonNotifyStr);
		   String notifySignatrueValue = notifyMap.get("signature");//ApiConstants.FIELD_SIGNATRUE);
		   
		   String bk_orderid=notifyMap.get("transSeqId");
		   String orderNo="hrt"+bk_orderid+"unionpay";
		   String checkBkOrderid="select * from pg_wechat_txn where fiid =52 and mer_orderid =? ";
		   List<Map<String, Object>> bkOrderIdList=dao.queryForList(checkBkOrderid,orderNo);
		   
		   if (bkOrderIdList.size()>0) {
			   logger.info("[银联小微商户]交易{}已经存在，不再添加",bk_orderid);
		   }else{
			   logger.info("[银联小微商户]通知报文中signatrue值：{}",notifySignatrueValue);
			   //secKey的值为通知aes密钥
			   String validteSignResult = ApiEncryptUtil.getNotifyValidteResult(ApiSignatureUtil.getSignCheckContentV2(notifyMap)+"&secKey="+callBackAesKey,"UTF-8");
			   logger.info("[银联小微商户]重新签名后的结果：{}",validteSignResult);
			   if(notifySignatrueValue.equals(validteSignResult)){
				   logger.info("[银联小微商户]验证签名成功");
				   try {
					   String getMidSql=" SELECT hrt_mid FROM HRT_MERBANKSUB where hrid in ( select HRID from bank_merregister where merchantcode=? and status='1' and approvestatus='Y') and status='1'";
					   List<Map<String, Object>> midList=dao.queryForList(getMidSql, notifyMap.get("mchntCd"));
					   
					   String hrt_mid=(String) midList.get(0).get("hrt_mid");
					   String insertSql = "insert into pg_wechat_txn (pwid,fiid, txntype,mer_orderid,bk_orderid,detail, txnamt, mer_id,"
								+ " bankmid,time_end,respcode,respmsg,status, cdate, lmdate,qrcode,settlekey,accno,unno,mer_tid,bank_type,trantype) values"
								+ "(S_PG_Wechat_Txn.nextval,?,'0',?,?,?,?,?,?,?,?,?,'1',sysdate,sysdate,?,?,?,?,?,'XW','9')";
						BigDecimal amt=BigDecimal.valueOf(Double.valueOf(notifyMap.get("txnAmt"))/100);
						int count=dao.update(insertSql,52,orderNo ,notifyMap.get("transSeqId") ,notifyMap.get("mchntNm"), amt, hrt_mid,
								notifyMap.get("mchntCd"),notifyMap.get("transTm"),notifyMap.get("respCode"),notifyMap.get("respMsg"), 
								notifyMap.get("qrVoucherNum"), notifyMap.get("settleKey"),
								notifyMap.get("accNo"),"880000",null);
						
						if (count==1) {
							String getOrderSql="select * from pg_wechat_txn where mer_orderid=?";
							List<Map<String, Object>> orderList=dao.queryForList(getOrderSql, orderNo);
							Map<String, Object > orderMap=orderList.get(0);
							orderMap.put("TXNLEVEL", BigDecimal.ONE);
							notify.sendNotify(orderMap);	
							logger.info("[银联小微商户]异步通知   订单{}添加成功",orderNo);
						}else {
							logger.info("[银联小微商户]异步通知   订单{}添加异常",orderNo);
						}
				   } catch (Exception e) {
					   logger.info("[银联小微商户]异步通知  {}订单处理异常:{}",notifyMap,e.getMessage());
				   }
				  
			   }else{
				   logger.info("验证签名失败");
			   }
		   }
	}
	
	
	/**
	 * 
	 * 批量生成二维码 
	 * 根据传入的数量 生成一个批次的二维码 
	 * 同步返回 批次号batchNo、应答码respCode、应答结果respsg、
	 * 
	 * @param count
	 * @return
	 * @throws BusinessException
	 */
//	public String getBatchQrcode(Integer count) throws BusinessException {
//		Map<String, String> reqData = new HashMap<String, String>();
//		reqData.put("encoding", "UTF-8");
//		reqData.put("version", "1.0");
//		reqData.put("signMethod", "RSA2");
//		reqData.put("expandcode", expandCode);
//		reqData.put("requestId", new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()));
//		reqData.put("bizContent", "{\"qrCodeNum\":\"" + count + "\"}");
//		String content = SDKUtil.coverMap2String(reqData);
//
//		String signature = "";
//		try {
//			signature = ApiSignatureUtil.rsaSign(content, privateKey, "UTF-8", "RSA2");
//		} catch (Exception e) {
//			logger.info("[银联小微商户]批量获取二维码 加密失败：{}", e.getMessage());
//			throw new BusinessException(8000, "交易失败");
//		}
//		System.out.println(signature);
//		reqData.put("signature", signature);
//		Map<String, String> rspMap = new TreeMap<String, String>();
//		// String resp="";
//		try {
//			// resp=AcpService.sendPost(reqData, getQrcodeUrl, "UTF-8");
//			// rspMap=JSONObject.parseObject(resp,Map.class);
//			logger.info("[银联小微商户]批量获取二维码  上送报文信息{}", reqData);
//			rspMap = ApiHttpClient.postXW(reqData, getQrcodeUrl, "UTF-8");
//			logger.info("[银联小微商户]批量获取二维码  返回报文信息{}", rspMap);
//			boolean validteFlag = false;
//			try {
//				validteFlag = ApiSignatureUtil.rsaCheckV2(rspMap, publicKey, "UTF-8", "RSA2");
//			} catch (ApiException e) {
//				logger.info("[银联小微商户]批量获取二维码  验签失败{}", e.getMessage());
//			}
//			if (validteFlag == true) {
//				logger.info("[银联小微商户]批量获取二维码  返回报文验签成功");
//			} else {
//				logger.info("[银联小微商户]批量获取二维码  返回报文验签失败");
//			}
//
//		} catch (Exception e) {
//			logger.info("[银联小微商户]批量获取二维码  发送请求报文异常：{}", e.getMessage());
//			// throw new BusinessException(8000, "交易失败");
//		}
//		return rspMap.toString();
//	}

	/**
	 * 查询 批量生成二维码
	 * 
	 * 根据批次号进行查询
	 * 同步返回 二维码url（qrcode）、二维码编号（qrnum）、二维码状态（是否可用（qrstatus））
	 * 
	 * @param batchNo
	 * @return
	 * @throws BusinessException
	 */
//	public String queryBatchQrcode(String batchNo) throws BusinessException {
//		Map<String, String> reqData = new HashMap<String, String>();
//		reqData.put("encoding", "UTF-8");
//		reqData.put("version", "1.0");
//		reqData.put("signMethod", "RSA2");
//		reqData.put("expandcode", expandCode);
//		reqData.put("requestId", new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()));
//		reqData.put("bizContent", "{\"batchNo\":\"" + batchNo + "\"}");
//		String content = SDKUtil.coverMap2String(reqData);
//		String signature = "";
//		try {
//			signature = ApiSignatureUtil.rsaSign(content, privateKey, "UTF-8", "RSA2");
//		} catch (ApiException e) {
//			logger.info("[银联小微商户]批量查询二维码 加密失败：{}", e.getMessage());
//			throw new BusinessException(8000, "交易失败");
//		}
//		System.out.println(signature);
//		reqData.put("signature", signature);
//		Map<String, String> rspMap = new TreeMap<String, String>();
//		try {
//			rspMap = ApiHttpClient.postXW(reqData, queryQrcodeUrl, "UTF-8");// JSONObject.parseObject(resp,Map.class);
//			boolean validteFlag = false;
//			try {
//				validteFlag = ApiSignatureUtil.rsaCheckV2(rspMap, publicKey, "UTF-8", "RSA2");// publicKey
//				System.out.println("---------------->验签返回信息" + validteFlag);
//			} catch (ApiException e) {
//				logger.info("[银联小微商户]批量查询二维码  验签失败{}", e.getMessage());
//			}
//			if (validteFlag == true) {
//				logger.info("[银联小微商户]批量查询二维码  返回报文验签成功");
//			} else {
//				logger.info("[银联小微商户]批量查询二维码  返回报文验签失败");
//			}
//
//		} catch (Exception e) {
//			logger.info("[银联小微商户]批量查询二维码  发送请求报文异常：{}", e.getMessage());
//			// throw new BusinessException(8000, "交易失败");
//		}
//		return rspMap.toString();
//	}
//	
//	
//   /**
//    * 发送短信
//    * 
//    * 上送电话号 （敏感信息加密    加密后转base64）
//    * 同步返回短信流水号（smsId）  被用于绑定二维码时使用
//    * @param phoneNo
//    * @return
//    * @throws BusinessException
//    */
//	public String sendMessageCode(String phoneNo) throws BusinessException {
//		System.out.println(aesKey);
//		Map<String, String> reqData = new HashMap<String, String>();
//		reqData.put("encoding", "UTF-8");
//		reqData.put("version", "1.0");
//		reqData.put("signMethod", "RSA2");
//		reqData.put("expandcode", expandCode);
//		reqData.put("requestId", new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()));
//
//		JSONObject jo = new JSONObject();
//		JSONObject encryptedInfoJo = new JSONObject();
//		encryptedInfoJo.put("phoneNo", phoneNo);
//		String encryptResult = "";
//		try {
//			encryptResult = ApiEncryptUtil.encryptContent(encryptedInfoJo.toString(), "AES", aesKey, "UTF-8");
//		} catch (ApiException e1) {
//			logger.info("[银联小微商户]发送短信  敏感信息 加密失败：{}", e1.getMessage());
//			throw new BusinessException(8000, "交易失败");
//		}
//		jo.put("encryptedInfo", encryptResult.toString());
//
//		reqData.put("bizContent", jo.toString());
//		String content = SDKUtil.coverMap2String(reqData);
//		String signature = "";
//		try {
//			signature = ApiSignatureUtil.rsaSign(content, privateKey, "UTF-8", "RSA2");
//		} catch (ApiException e) {
//			logger.info("[银联小微商户]发送短信  加密失败：{}", e.getMessage());
//			throw new BusinessException(8000, "交易失败");
//		}
//		reqData.put("signature", signature);
//		Map<String, String> rspMap = new HashMap<String, String>();
//		try {
//			logger.info("[银联小微商户]发送短信  请求报文{}",reqData);
//			rspMap = ApiHttpClient.postXW(reqData, sendMessageUrl, "UTF-8");
//			logger.info("[银联小微商户]发送短信  返回报文{}",reqData);
//			boolean validteFlag = false;
//			try {
//				validteFlag = ApiSignatureUtil.rsaCheckV2(rspMap, publicKey, "UTF-8", "RSA2");
//			} catch (ApiException e) {
//				logger.info("[银联小微商户]发送短信  验签失败{}", e.getMessage());
//			}
//			if (validteFlag == true) {
//				logger.info("[银联小微商户]发送短信  返回报文验签成功");
//			} else {
//				logger.info("[银联小微商户]发送短信  返回报文验签失败");
//			}
//
//		} catch (Exception e) {
//			logger.info("[银联小微商户]发送短信  发送请求报文异常：{}", e.getMessage());
//			// throw new BusinessException(8000, "交易失败");
//		}
//
//		return rspMap.toString();
//	}
//
//	/**
//	 * 给手机号 绑定二维码
//	 * 上送 二维码url、手机号、短信流水号、短信、收款账号、 证件号、收款人姓名 、联行号
//	 * 进行二维码的绑定
//	 * 
//	 * @param qrcode
//	 * @param phoneNo
//	 * @param smsId
//	 * @param accNo
//	 * @param certifId
//	 * @param customerNm
//	 * @param bankNo
//	 * @return
//	 * @throws BusinessException
//	 */
//	public String bindCode(String qrcode, String phoneNo, String smsId, String accNo, String certifId,
//			String customerNm, String bankNo,String smsCode) throws BusinessException {
//		Map<String, String> reqData = new HashMap<String, String>();
//		reqData.put("encoding", "UTF-8");
//		reqData.put("version", "1.0");
//		reqData.put("signMethod", "RSA2");
//		reqData.put("expandcode", expandCode);
//		reqData.put("requestId", new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()));
//		JSONObject encryptedInfoJo = new JSONObject();
//		encryptedInfoJo.put("accNo", accNo);
//		encryptedInfoJo.put("phoneNo", phoneNo);
//		Map<String, String> bizContentMap = new HashMap<String, String>();
//		try {
//			bizContentMap.put("encryptedInfo",
//					ApiEncryptUtil.encryptContent(encryptedInfoJo.toString(), "AES", aesKey, "UTF-8"));
//		} catch (ApiException e1) {
//			logger.info("[银联小微商户]绑定二维码  敏感信息加密失败：{}", e1.getMessage());
//			throw new BusinessException(8000, "交易失败");
//		}
//		bizContentMap.put("expandName", "48640000");//"hkrt3");
//		bizContentMap.put("merName", "测试");
//		bizContentMap.put("certifTp", "01");
//		bizContentMap.put("certifId", certifId);
//		bizContentMap.put("customerNm", customerNm);
//		bizContentMap.put("smsCode", smsCode);//"959720");//"123456");
//		bizContentMap.put("smsId", smsId);//"00021953");//201712201335190521
//		bizContentMap.put("qrCode", qrcode);
//		bizContentMap.put("merLat", "111.01");
//		bizContentMap.put("merLng", "90.02");
//		bizContentMap.put("merType", "01");
//		bizContentMap.put("bankNo", bankNo);
//		String seqBizCotentMapStr =CommonUtil.coverMap2JsonString(bizContentMap);
//		reqData.put("bizContent", seqBizCotentMapStr);
//
//		String content =SDKUtil.coverMap2String(reqData);
//
//		String signature = "";
//		try {
//			signature =ApiSignatureUtil.rsaSign(content, privateKey, "UTF-8", "RSA2");
//		} catch (ApiException e) {
//			logger.info("[银联小微商户]绑定二维码  加密异常：{}", e.getMessage());
//			throw new BusinessException(8000, "交易失败");
//		}
//		System.out.println(signature);
//		reqData.put("signature", signature);
//		Map<String, String> rspMap = new TreeMap<String, String>();
//		try {
//			logger.info("[银联小微商户]绑定二维码  上送请求报文：{}",reqData);
//			rspMap = ApiHttpClient.postXW(reqData, bindCodeUrl, "UTF-8");
//			logger.info("[银联小微商户]绑定二维码  接收返回报文：{}",rspMap);
//			boolean validteFlag = false;
//			try {
//				validteFlag = ApiSignatureUtil.rsaCheckV2(rspMap, publicKey, "UTF-8", "RSA2");
//			} catch (ApiException e) {
//				e.printStackTrace();
//			}
//			if (validteFlag == true) {
//				logger.info("[银联小微商户]绑定二维码  返回报文验签成功");
//			} else {
//				logger.info("[银联小微商户]绑定二维码  返回报文验签失败");
//			}
//
//		} catch (Exception e) {
//			logger.info("[银联小微商户]绑定二维码  发送失败：{}", e.getMessage());
//			// throw new BusinessException(8000, "交易失败");
//		}
//		return rspMap.toString();
//	}
//	
//	
//    /**
//     * 
//     * 上传照片
//     * 根据路径取照片 进行上送
//     * 
//     * @param merId
//     * @param imgType
//     * @param imgPath
//     * @return
//     * @throws BusinessException
//     */
//	public String imgUpLoad(String merId, String imgType, String imgPath) throws BusinessException {
//		Map<String, String> reqData = new HashMap<String, String>();
//		reqData.put("version", "1.0");
//		reqData.put("encoding", "UTF-8");
//		reqData.put("signMethod", "RSA2");
//		reqData.put("expandcode", expandCode);
//		reqData.put("requestId", new SimpleDateFormat("yyyyMMddHHmmssSSSS").format(new Date()));
//
//		Map<String, String> bizContentMap = new HashMap<String, String>();
//		bizContentMap.put("merId", merId);
//		bizContentMap.put("imgType", imgType);
//		String seqBizCotentMapStr = CommonUtil.coverMap2JsonString(bizContentMap);
//		reqData.put("bizContent", seqBizCotentMapStr);
//		String content = SDKUtil.coverMap2String(reqData);
//		String signature = null;
//		logger.info("[银联小微商户]上送图片  签名串{}", content);
//		try {
//			signature = ApiSignatureUtil.rsaSign(content, privateKey, "UTF-8", "RSA2");
//		} catch (ApiException e) {
//			logger.info("[银联小微商户]上送图片  签名失败{}", e.getMessage());
//		}
//		reqData.put("signature", signature);
//		FileItem fi = new FileItem(imgPath);//
//		Map<String, FileItem> fileItems = new HashMap<String, FileItem>();//
//		fileItems.put("imgContent", fi);
//		Map<String, String> rspMap = new TreeMap<String, String>();
//		try{
//			logger.info("[银联小微商户]上送图片  上送报文信息{}", reqData);
//			rspMap= ApiHttpClient.postWithFile(reqData, imgUploadUrl, fileItems, "UTF-8");
//			logger.info("[银联小微商户]上送图片  返回报文信息{}", rspMap);
//			boolean validteFlag = false;
//			try {
//				validteFlag = ApiSignatureUtil.rsaCheckV2(rspMap, publicKey, "UTF-8", "RSA2");
//			} catch (ApiException e) {
//				logger.info("[银联小微商户]上送图片    验签失败{}", e.getMessage());
//			}
//			if (validteFlag == true) {
//				logger.info("[银联小微商户]上送图片    返回报文验签成功");
//			} else {
//				logger.info("[银联小微商户]上送图片    返回报文验签失败");
//			}
//		} catch (Exception e) {
//			logger.info("[银联小微商户]上送图片  发送失败：{}", e.getMessage());
//			// throw new BusinessException(8000, "交易失败");
//		}
//		return rspMap.toString();
//	}
// 
}
