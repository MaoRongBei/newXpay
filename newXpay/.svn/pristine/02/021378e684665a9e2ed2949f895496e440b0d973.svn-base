package com.hrtpayment.xpay.cups.sdk;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.hrtpayment.xpay.cups.sdk.FileItem;
import com.hrtpayment.xpay.cups.sdk.HttpClient;

public class ApiHttpClient {
	
	/**
	 * 功能：后台交易提交请求报文并接收同步应答报文<br>
	 * @param reqData 请求报文<br>
	 * @param rspData 应答报文<br>
	 * @param reqUrl  请求地址<br>
	 * @param encoding<br>
	 * @return 应答http 200返回true ,其他false<br>
	 */
	public static Map<String,String> postXW(
			Map<String, String> reqData,String reqUrl,String encoding) {
		Map<String, String> rspData = new HashMap<String,String>();
		LogUtil.writeLog("请求银联地址:" + reqUrl);
		//发送后台请求数据
		HttpClient hc = new HttpClient(reqUrl, 30000, 30000);//连接超时时间，读超时时间（可自行判断，修改）
		try {
			int status = hc.send(reqData, encoding);
			if (200 == status) {
				String resultString = hc.getResult();
				if (null != resultString && !"".equals(resultString)) {
					// 将返回结果转换为map
					rspData.putAll(CommonUtil.jsonStr2Map(resultString));
				}
			}else{
				LogUtil.writeLog("返回http状态码["+status+"]，请检查请求报文或者请求地址是否正确");
			}
		} catch (Exception e) {
			LogUtil.writeErrorLog(e.getMessage(), e);
		}
		return rspData;
	}
	
	public static String postJSON(
			String jsonStr,String reqUrl,String encoding) {
		LogUtil.writeLog("请求银联地址:" + reqUrl);
		String resultString = null;
		//发送后台请求数据
		HttpClient hc = new HttpClient(reqUrl, 30000, 30000);//连接超时时间，读超时时间（可自行判断，修改）
		try {
			int status = hc.sendJSON(jsonStr, encoding);
			if (200 == status) {
				resultString = hc.getResult();
			}else{
				LogUtil.writeLog("返回http状态码["+status+"]，请检查请求报文或者请求地址是否正确");
			}
		} catch (Exception e) {
			LogUtil.writeErrorLog(e.getMessage(), e);
		}
		return resultString;
	}
	
	/**
     * 执行带文件上传的HTTP POST请求。
     * 
     * @param url 请求地址
     * @param textParams 文本请求参数
     * @param fileParams 文件请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串
     * @throws IOException
     */
    public static Map<String,String> postWithFile(Map<String, String> reqData,String reqUrl,
                                Map<String, FileItem> fileParams, String encoding) {
    	
    	Map<String, String> rspData = new HashMap<String,String>();
		LogUtil.writeLog("请求银联地址:" + reqUrl);
		//发送后台请求数据
		HttpClient hc = new HttpClient(reqUrl, 30000, 30000);//连接超时时间，读超时时间（可自行判断，修改）
		try {
			int status = hc.sendMultipart(reqData,fileParams, encoding);
			if (200 == status) {
				String resultString = hc.getResult();
				if (null != resultString && !"".equals(resultString)) {
					// 将返回结果转换为map
					rspData.putAll(CommonUtil.jsonStr2Map(resultString));
				}
			}else{
				LogUtil.writeLog("返回http状态码["+status+"]，请检查请求报文或者请求地址是否正确");
			}
		} catch (Exception e) {
			LogUtil.writeErrorLog(e.getMessage(), e);
		}
		return rspData;
    }
}
