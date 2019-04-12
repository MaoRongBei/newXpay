package com.hrtpayment.xpay.quickpay.cups.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import com.hrtpayment.xpay.redis.RedisUtil;

import io.netty.handler.codec.Headers;

public class HttpConnectService {

	public static final Logger log = Logger.getLogger("HttpConnectService:");

	public static final String APPLICATION = "application/xml;charset=utf-8";// "text/xml;charset=UTF-8";

	public static final String CONTENT_TYPE = "application/xml";
	/**
	 * 地址串
	 */

	private static String connTimeOut = "30000";

	private static String hostTimeOut = "1200000";

	private static PoolingHttpClientConnectionManager cm = null;
	private static PoolingHttpClientConnectionManager cupCm = null;
	private static CloseableHttpClient closeableHttpClient;

	static {
		cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(20);// 连接池最大并发连接数
		cm.setDefaultMaxPerRoute(20);// 单路由最大并发数

		closeableHttpClient = HttpClients.custom().setConnectionManager(cm).build();

		System.setProperty("jsse.enableSNIExtension", "false");
		SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				// 信任所有
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}
			}).build();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https", new SSLConnectionSocketFactory(sslContext)).build();
		cupCm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		cupCm.setMaxTotal(20);// 连接池最大并发连接数
		cupCm.setDefaultMaxPerRoute(20);// 单路由最大并发数
	}

	/**
	 * 发送报文
	 * 
	 * @throws TranException
	 */
	public static String sendMessage(String message, String url) throws Exception {

		HttpPost httpPost = new HttpPost(url);

		// for (Header header : Headers) {
		// log.info("+{}:{}",header,header.getValue());
		//// htmlMap.put(name, value);
		// }

		httpPost.addHeader(HTTP.CONTENT_TYPE, APPLICATION);//"application/x-www-form-urlencoded");//

		httpPost.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
		// httpPost.removeHeaders(HTTP.CONTENT_LEN);
		// httpPost.setHeader(HTTP.CONTENT_LEN,"542");
		httpPost.addHeader("MsgTp", "0001");
		httpPost.addHeader("OriIssrId", "48641000");
//		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");//"application/xml"); 
//		httpPost.addHeader("Content-Encoding", "gzip");
		// httpPost.setHeader(HTTP.CONTENT_LEN, "1");
		// httpPost.addHeader(HTTP.CONTENT_LEN,"542");
		System.out.println(httpPost.getAllHeaders());
		Header[] Headers = httpPost.getAllHeaders();

		// for (Header header : Headers) {
		// log.info("+{}:{}",header,header.getValue());
		//// htmlMap.put(name, value);
		// }
		for (Header header : Headers) {
			String name = header.getName().toString();
			String value = header.getValue().toString();
			System.out.println(name + "       " + value);
		}

		StringEntity se = new StringEntity(message, "UTF-8");

		se.setContentType(CONTENT_TYPE);

		// se.setContentEncoding("");
		// se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
		// APPLICATION));

		RequestConfig reuqestConfig = RequestConfig.custom().setSocketTimeout(Integer.parseInt(hostTimeOut))
				.setConnectTimeout(Integer.parseInt(connTimeOut)).build();

		httpPost.setConfig(reuqestConfig);
		// httpPost.setHeader("Content-Length", "996");
		// httpPost.

		httpPost.setEntity(se);
		log.info("发送数据到:" + url);

		// CloseableHttpClient httpclient = HttpClients.createDefault();

		// post请求
		// CloseableHttpResponse response = httpclient.execute(httpPost);
		CloseableHttpResponse response = closeableHttpClient.execute(httpPost);

		// 获取状态行
		if (response != null) {
			log.info(response.getStatusLine().toString());
		}
		//////////////////////////////////////////////////////////////////////
		HttpEntity entity = response.getEntity();
		//
		// // 返回内容
		String returnBody = EntityUtils.toString(entity);
		String resXml = new String(returnBody.getBytes(), "UTF-8");
		//////////////////////////////////////////////////////////////////////////////////////

		// InputStream inputStream;
		// StringBuffer sb = new StringBuffer();
		// inputStream = response..getInputStream();
		// String s;
		// BufferedReader in = new BufferedReader(new
		// InputStreamReader(inputStream, "UTF-8"));
		// while ((s = in.readLine()) != null) {
		// sb.append(s);
		// }
		// in.close();
		// inputStream.close();
		// String reqStr = sb.toString();
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
		try {
			while ((r = bReader.readLine()) != null) {
				strBuf.append(r).append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bReader.close();
			is.close();
			response.close();
		}
		return strBuf.toString();
	}

	/**
	 * form 发送
	 * 
	 * @param formMap
	 * @param urlStr
	 * @return
	 * @throws Exception
	 */
	public static String postForm(Map<String, String> formMap, String urlStr) throws Exception {
		// HashMap<String, String>
		log.info("发送的消息：" + formMap);

		CloseableHttpClient httpclient = createSSLClientDefault();// 创建不被信任的ssl站点连接

		HttpPost httpPost = new HttpPost();
		httpPost.setURI(new URI(urlStr));
		RequestConfig reuqestConfig = RequestConfig.custom().setSocketTimeout(Integer.parseInt(hostTimeOut))
				.setConnectTimeout(Integer.parseInt(connTimeOut)).build();

		httpPost.setConfig(reuqestConfig);
		httpPost.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
		// 实体转Map
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		Iterator<String> it = formMap.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			String value = formMap.get(key);
			if (value == null || "".equals(value)) {
				continue;
			}
			formparams.add(new BasicNameValuePair(key, value));
		}

		UrlEncodedFormEntity urlentity;

		urlentity = new UrlEncodedFormEntity(formparams, "UTF-8");
		httpPost.setEntity(urlentity);

		log.info("后台发post消息URL:" + urlStr);
		// post请求
		CloseableHttpResponse response = null;
		String returnBody = null;
		try {
			response = httpclient.execute(httpPost);
			log.info(response.getStatusLine().toString());
			HttpEntity entity = response.getEntity();
			// 返回内容
			returnBody = EntityUtils.toString(entity);
			log.info("解析返回内容返回Map");
			log.info("returnBody:" + returnBody);
			if (returnBody == null || returnBody.equals("")) {
				return returnBody;
			}
		} catch (Exception e) {
			RedisUtil.addFailCountByRedis(1);
			log.error("", e);
			return returnBody;
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				log.error("", e);
			} finally {
				try {
					// 释放资源
					if (httpPost != null) {
						httpPost.abort();
					}
				} catch (Exception e) {
					log.error("", e);
				}
			}

		}

		return returnBody;
	}

	/**
	 * 以表单的形式发送数据
	 * 
	 * @param URL
	 * @param msg
	 * @return
	 * @throws Exception
	 */
	public static TreeMap<String, String> postForm1(Map<String, String> formMap, String url) throws Exception {
		log.info("发送的消息：" + formMap);

		CloseableHttpClient httpclient = createSSLClientDefault();// 创建不被信任的ssl站点连接

		HttpPost httpPost = new HttpPost();
		httpPost.setURI(new URI(url));
		RequestConfig reuqestConfig = RequestConfig.custom().setSocketTimeout(Integer.parseInt(hostTimeOut))
				.setConnectTimeout(Integer.parseInt(connTimeOut)).build();

		httpPost.setConfig(reuqestConfig);
		httpPost.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
		// 实体转Map
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		Iterator<String> it = formMap.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			String value = formMap.get(key);
			if (value == null || "".equals(value)) {
				continue;
			}
			formparams.add(new BasicNameValuePair(key, value));
		}

		UrlEncodedFormEntity urlentity;

		TreeMap<String, String> htmlMap = new TreeMap<String, String>();
		urlentity = new UrlEncodedFormEntity(formparams, "UTF-8");
		httpPost.setEntity(urlentity);

		log.info("后台发post消息URL:" + url);
		// post请求
		CloseableHttpResponse response = null;
		try {
			response = httpclient.execute(httpPost);

			response = httpclient.execute(httpPost);
			Header[] Headers = response.getAllHeaders();
			Headers.toString();
			// for (Header header : Headers) {
			// log.info("+{}:{}",header,header.getValue());
			//// htmlMap.put(name, value);
			// }
			for (Header header : Headers) {
				String name = header.getName().toString();
				String value = header.getValue().toString();
				htmlMap.put(name, value);
			}
			// logger.info(response.getStatusLine().toString());
			HttpEntity entity = response.getEntity();
			// 返回内容
			String returnBody = EntityUtils.toString(entity);

			// log.info(response.getStatusLine().toString());
			// HttpEntity entity = response.getEntity();
			// // 返回内容
			// String returnBody = EntityUtils.toString(entity);
			// log.info("解析返回内容返回Map");
			// log.info("returnBody:"+returnBody);
			// if(returnBody==null||returnBody.equals("")){
			// return (TreeMap<String, String>) htmlMap;
			// }
			// log.info("解析返回信息");
			// String[] resultStrings = returnBody.split("&");
			// for(String str : resultStrings ){
			// String[] keyvalue = str.split("=",2);
			//
			// htmlMap.put(keyvalue[0], keyvalue[1]==null?"":keyvalue[1]);
			// }
		} catch (Exception e) {
			log.error("", e);
			htmlMap.put("message:", e.getMessage());
			return htmlMap;
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception e) {
				log.error("", e);
			} finally {
				try {
					// 释放资源
					if (httpPost != null) {
						httpPost.abort();
					}
				} catch (Exception e) {
					log.error("", e);
				}
			}

		}
		return htmlMap;
	}

	public static HttpResponse sendPostToService(String url, Object pushData) throws IOException, KeyStoreException,
			UnrecoverableKeyException, NoSuchAlgorithmException, KeyManagementException {

		// if (!hasInit) {
		// init();
		// }

		String result = null;

		HttpPost httpPost = new HttpPost(url);
		StringEntity postEntity = new StringEntity(pushData.toString());
		// 设置一些Http头信息
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");//"application/xml");
		httpPost.addHeader("connection", "Keep-Alive");
		httpPost.addHeader("Content-Encoding", "gzip");
		httpPost.addHeader("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
		httpPost.addHeader("MsgTp", "0001");
		httpPost.addHeader("OriIssrId", "48641000");
//		httpPost.addHeader("Content-Length", "1992");
		// 将发送内容填装
		httpPost.setEntity(postEntity);

		RequestConfig reuqestConfig = RequestConfig.custom().setSocketTimeout(Integer.parseInt(hostTimeOut))
				.setConnectTimeout(Integer.parseInt(connTimeOut)).build();
		// 设置请求器的配置
		httpPost.setConfig(reuqestConfig);

		// 打印待发送的数据
		log.info("=====API，POST过去的数据是：");
		log.info("executing request" + httpPost.getRequestLine());
		log.info("请求头信息===" + httpPost.getAllHeaders().toString());
		log.info("请求状态行===" + httpPost.getRequestLine());
		log.info("请求配置===" + httpPost.getConfig());
		log.info("请求实体===" + httpPost.getEntity().getContentEncoding() + httpPost.getEntity().getContentType()
				+ httpPost.getEntity().getContent());

		CloseableHttpResponse response = null;
		try {
			// 忽略所有的SSL请求的证书
			// CloseableHttpClient httpclient = HttpClients.createDefault();
			// httpClient = new SSLClient();
			// response = httpClient.execute(httpPost);
			response = closeableHttpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();

			result = EntityUtils.toString(entity, "UTF-8");
			// 打印得到的响应信息
			log.info("API，POST回来的数据是：");
			log.info("=====Entity:" + result);
			log.info("=====Headers:" + response.getAllHeaders());
			log.info("=====StatusLine:" + response.getStatusLine());
			log.info("=====Locale:" + response.getLocale());

		} catch (ConnectionPoolTimeoutException e) {
			log.error("http get throw ConnectionPoolTimeoutException(wait time out)");

		} catch (ConnectTimeoutException e) {
			log.error("http get throw ConnectTimeoutException");

		} catch (SocketTimeoutException e) {
			log.error("http get throw SocketTimeoutException");

		} catch (ClientProtocolException e) {
			log.error("http get throw ClientProtocolException");

		} catch (IOException e) {
			e.printStackTrace();

		} catch (Exception e) {
			log.error("http get throw Exception");

		} finally {
			httpPost.abort();
		}

		return response;
	}

	/**
	 * 连接不被信任的ssl站点
	 * 
	 * @return
	 */
	public static CloseableHttpClient createSSLClientDefault() {
		return HttpClients.custom().setConnectionManager(cupCm).build();
	}

	public String getConnTimeOut() {
		return connTimeOut;
	}

	public static void setConnTimeOut(String connTimeOut) {
		HttpConnectService.connTimeOut = connTimeOut;
	}

	public static String getHostTimeOut() {
		return hostTimeOut;
	}

	public static void setHostTimeOut(String hostTimeOut) {
		HttpConnectService.hostTimeOut = hostTimeOut;
	}

}
