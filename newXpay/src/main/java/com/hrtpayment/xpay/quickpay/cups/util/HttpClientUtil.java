package com.hrtpayment.xpay.quickpay.cups.util;
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import com.hrtpayment.xpay.redis.RedisUtil;

import oracle.net.aso.s;

import org.jboss.logging.Logger;

/**
 * HttpClient请求通用类
 * @author herman
 * @email hepengbj@bypay.cn Dec 22, 2015
 */
public class HttpClientUtil {
	public static final Logger log = Logger.getLogger("HttpConnectService:");
	private static CloseableHttpClient closeableHttpClient;
	private static PoolingHttpClientConnectionManager cm = null;
	private static PoolingHttpClientConnectionManager cupCm = null;
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
     * httpPost
     * @param url  路径
     * @param jsonParam 参数
     * @return
     */
    public static String httpPost(String url, String param){
        return httpPost(url, param, false);
    }
    
    
    /**
     * post请求
     * @param url         url地址
     * @param jsonParam     参数
     * @param noNeedResponse    不需要返回结果
     * @return
     */
    public static String httpPost(String url, String param, boolean noNeedResponse){
    	String result = null;
    	CloseableHttpClient httpClient = null;
        try {
        	httpClient = HttpClients.createDefault();
            HttpPost method = new HttpPost(url);
            if (null != param) {
                //解决中文乱码问题
                StringEntity entity = new StringEntity(param, "utf-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                method.setEntity(entity);
            }
            HttpResponse response = httpClient.execute(method);
            url = URLDecoder.decode(url, "UTF-8");
            /**请求发送成功，并得到响应**/
            if (response.getStatusLine().getStatusCode() == 200) {
                try {
                	result = EntityUtils.toString(response.getEntity());
                    if (noNeedResponse) {
                        return null;
                    }
                } catch (Exception e) {
                	System.out.println("post请求提交失败:"+e);
                }
            }
        } catch (Exception e) {
        	RedisUtil.addFailCountByRedis(1);
            System.out.println("post请求提交失败:"+e);
        } finally {
        	try {
        		if(httpClient != null)
				httpClient.close();
			} catch (IOException e) {
				System.out.println("HttpClientUtil类关闭httpClient出错:" + e);
			}
        }
        return result;
    }
 
 
    
 
    /**
     * post请求
     * @param url         url地址
     * @param jsonParam     参数
     * @param noNeedResponse    不需要返回结果
     * @return
     */
    public static String httpPost(String url, String param, String   msgTp){
    	String result = null;
    	CloseableHttpClient httpClient = null;
        try {
        	httpClient = HttpClients.createDefault();
            HttpPost method = new HttpPost(url);
            if (null != param) {
                //解决中文乱码问题
                StringEntity entity = new StringEntity(param, "utf-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                
                method.setEntity(entity);
            }
            method.setHeader("Connection","close");
//            httpost.setHeader("Connection","keep-alive");
            method.addHeader("Content-Type", "application/xml;charset=utf-8");
            method.addHeader("MsgTp", msgTp);
            method.addHeader("OriIssrId", "48641000"); 
            method.addHeader("SderReserved", ""); 
            method.addHeader("RcverReserved", ""); 
            method.addHeader("CupsReserved", "");
            HttpResponse response = httpClient.execute(method);
            url = URLDecoder.decode(url, "UTF-8");
            /**请求发送成功，并得到响应**/
            
            System.out.println(response.getStatusLine());
            if (response.getStatusLine().getStatusCode() == 200) {
                try {
                	result = EntityUtils.toString(response.getEntity());
                } catch (Exception e) {
                	System.out.println("post请求提交失败:"+e);
                }
            }
        } catch (Exception e) {
        	RedisUtil.addFailCountByRedis(1);
            System.out.println("post请求提交失败:"+e);
        } finally {
        	try {
        		if(httpClient != null)
				httpClient.close();
			} catch (IOException e) {
				System.out.println("HttpClientUtil类关闭httpClient出错:" + e);
			}
        }
        return result;
    }
 
 
    
    
    
    /**
     * 发送get请求
     * @param url 路径
     * @return
     */
    public static String httpGet(String url){
        //get请求返回结果
        String result = null;
        CloseableHttpClient httpClient = null;
        try {
        	httpClient = HttpClients.createDefault();
            //发送get请求
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            /**请求发送成功，并得到响应**/
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                result = EntityUtils.toString(response.getEntity());
                url = URLDecoder.decode(url, "UTF-8");
            } else {
                System.out.println("get请求提交失败:" + url);
            }
        } catch (IOException e) {
            System.out.println("get请求提交失败:"+ e);
        } finally {
        	try {
				httpClient.close();
			} catch (IOException e) {
				System.out.println("HttpClientUtil类关闭httpClient出错:" + e);
			}
        }
        return result;
    }
    
    
    public static String sendPosts(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
   
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader( new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }    

    /**
	 * @Title: getSignature
	 * @Description: 签名方法
	 * @param map
	 * @param key
	 * @return
	 * @return: String
	 */
	public static String getSignature(Map<String, String> map, String key) {
		String vaData = MapUtil.map2String(map) + "&signkey=" + key.trim();
		String signKey = Sha1Util.getSha1SignMsg(vaData, "UTF-8");
		return signKey;
	}
    
}