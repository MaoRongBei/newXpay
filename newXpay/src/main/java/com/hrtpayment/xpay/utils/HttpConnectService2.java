package com.hrtpayment.xpay.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

public class HttpConnectService2 {

	public static final Logger log = Logger.getLogger("HttpConnectService:");

	public static final String APPLICATION = "text/xml;charset=UTF-8";

	public static final String CONTENT_TYPE = "application/xmlstream";
	/**
	 * 地址串
	 */

	private static String connTimeOut = "30000";

	private static String hostTimeOut = "120000";

	private static PoolingHttpClientConnectionManager cm = null;
	private static PoolingHttpClientConnectionManager cupCm1507834131 = null;
	private static PoolingHttpClientConnectionManager cupCm1516556061=null;

	private static CloseableHttpClient closeableHttpClient;

	static {
		cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(20);// 连接池最大并发连接数
		cm.setDefaultMaxPerRoute(20);// 单路由最大并发数

		closeableHttpClient = HttpClients.custom().setConnectionManager(cm).build();
		
		FileInputStream instream=null;
	    try {
			KeyStore keyStore1507834131  = KeyStore.getInstance("PKCS12");
	        instream = new FileInputStream(new File(System.getProperty("user.dir")+"/conf/1507834131.p12"));
	        keyStore1507834131.load(instream, "1507834131".toCharArray()); 
		        // Trust own CA and all self-signed certs
		        SSLContext sslcontext1507834131 = SSLContexts.custom()
		                .loadKeyMaterial(keyStore1507834131, "1507834131".toCharArray())
		                .build();
		        // Allow TLSv1 protocol only
		        SSLConnectionSocketFactory sslsf1507834131 = new SSLConnectionSocketFactory(
		                sslcontext1507834131,
		                new String[] { "TLSv1" },
		                null,
		                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
			Registry<ConnectionSocketFactory> socketFactoryRegistry1507834131 = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", sslsf1507834131).build();
			cupCm1507834131 = new PoolingHttpClientConnectionManager(socketFactoryRegistry1507834131);
			cupCm1507834131.setMaxTotal(10);// 连接池最大并发连接数
			cupCm1507834131.setDefaultMaxPerRoute(10);// 单路由最大并发数
			
			KeyStore keyStore1516556061  = KeyStore.getInstance("PKCS12");
	        instream = new FileInputStream(new File(System.getProperty("user.dir")+"/conf/1516556061.p12"));
	        keyStore1516556061.load(instream, "1516556061".toCharArray()); 
		        // Trust own CA and all self-signed certs
		        SSLContext sslcontext1516556061 = SSLContexts.custom()
		                .loadKeyMaterial(keyStore1516556061, "1516556061".toCharArray())
		                .build();
		        // Allow TLSv1 protocol only
		        SSLConnectionSocketFactory sslsf1516556061 = new SSLConnectionSocketFactory(
		                sslcontext1516556061,
		                new String[] { "TLSv1" },
		                null,
		                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
			Registry<ConnectionSocketFactory> socketFactoryRegistry1516556061 = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", sslsf1516556061).build();
			cupCm1516556061 = new PoolingHttpClientConnectionManager(socketFactoryRegistry1516556061);
			cupCm1516556061.setMaxTotal(10);// 连接池最大并发连接数
			cupCm1516556061.setDefaultMaxPerRoute(10);// 单路由最大并发数
			
        } catch (Exception e){
        	e.printStackTrace();
        }finally {
        	if(instream!=null){
                try {
					instream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}

        }
	}

	/**
	 * 发送报文
	 * 
	 * @throws TranException
	 */
	public static String sendMessage(String message, String url,String mchId) throws Exception {

		HttpPost httpPost = new HttpPost(url);

		httpPost.addHeader(HTTP.CONTENT_TYPE, APPLICATION);

		httpPost.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

		StringEntity se = new StringEntity(message, "UTF-8");

		se.setContentType(CONTENT_TYPE);

		se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, APPLICATION));

		RequestConfig reuqestConfig = RequestConfig.custom().setSocketTimeout(Integer.parseInt(hostTimeOut))
				.setConnectTimeout(Integer.parseInt(connTimeOut)).build();

		httpPost.setConfig(reuqestConfig);

		httpPost.setEntity(se);
		log.info("发送数据到:" + url);

		// CloseableHttpClient httpclient = HttpClients.createDefault();
		// post请求
		// CloseableHttpResponse response = httpclient.execute(httpPost);
		CloseableHttpResponse response = createSSLClientDefault(mchId).execute(httpPost);

		// 获取状态行
		if (response != null) {
			log.info(response.getStatusLine().toString());
		}
		//////////////////////////////////////////////////////////////////////
		HttpEntity entity = response.getEntity();
//
//		// 返回内容
		String returnBody = EntityUtils.toString(entity);
		String resXml = new String(returnBody.getBytes(), "UTF-8");
//////////////////////////////////////////////////////////////////////////////////////
		
//		InputStream inputStream;
//		StringBuffer sb = new StringBuffer();
//		inputStream = response..getInputStream();
//		String s;
//		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
//		while ((s = in.readLine()) != null) {
//			sb.append(s);
//		}
//		in.close();
//		inputStream.close();
//		String reqStr = sb.toString();
		// log.info(resXml);

		response.close();
		// 释放资源
		// closeableHttpClient.close();
		// 发送报
		return resXml;
	}

	/**
	 * 
	 * 对账文件下载
	 * 
	 */
	public static String sendDownloadMessage(String message, String url) throws Exception {
		HttpPost httpPost = new HttpPost(url);

		httpPost.addHeader(HTTP.CONTENT_TYPE, APPLICATION);

		httpPost.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

		StringEntity se = new StringEntity(message, "UTF-8");

		se.setContentType(CONTENT_TYPE);

		se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, APPLICATION));

		RequestConfig reuqestConfig = RequestConfig.custom().setSocketTimeout(Integer.parseInt(hostTimeOut))
				.setConnectTimeout(Integer.parseInt(connTimeOut)).build();

		httpPost.setConfig(reuqestConfig);

		httpPost.setEntity(se);
		log.info("发送数据到:" + url);

		CloseableHttpResponse response = closeableHttpClient.execute(httpPost);

		// 获取状态行
		if (response != null) {
			log.info(response.getStatusLine().toString());
		}
		HttpEntity entity = response.getEntity();
		StringBuffer strBuf = new StringBuffer();
		InputStream is = entity.getContent();
		BufferedReader bReader = new BufferedReader(new InputStreamReader(is));
		String r = "";
		try{
		while ((r = bReader.readLine()) != null) {
			strBuf.append(r).append("\n");
		}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			bReader.close();
			is.close();
			response.close();
		}
		return strBuf.toString();
	}

	/**
	 * 连接不被信任的ssl站点
	 * 
	 * @return
	 */
	public static CloseableHttpClient createSSLClientDefault(String mchid) {
      

		HttpClientConnectionManager cupCm = null;
		try {
			Field[] fields=HttpConnectService2.class.newInstance().getClass().getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				fields[i].setAccessible(true);
				if (("cupCm"+mchid).equals(fields[i].getName())) {
					cupCm=(HttpClientConnectionManager) fields[i].get("cupCm"+mchid);
					break;
				} 
			}  
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return HttpClients.custom().setConnectionManager(cupCm).build();
	}

	public String getConnTimeOut() {
		return connTimeOut;
	}

	public static void setConnTimeOut(String connTimeOut) {
		HttpConnectService2.connTimeOut = connTimeOut;
	}

	public static String getHostTimeOut() {
		return hostTimeOut;
	}

	public static void setHostTimeOut(String hostTimeOut) {
		HttpConnectService2.hostTimeOut = hostTimeOut;
	}

}
