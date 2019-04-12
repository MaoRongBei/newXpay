package com.hrtpayment.xpay.quickpay.newCups.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

public class HttpXmlClient {
	 private static Logger log = Logger.getLogger(HttpXmlClient.class);  
     
	    public static String post(String url, String params ,String msgTp) {  
	        DefaultHttpClient httpclient = new DefaultHttpClient(); 
	        httpclient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1500);
//	        httpclient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 1000*5);
	        String body = null;  
	          
	        log.info("create httppost:" + url);
	        HttpPost post = postForm(url, params,msgTp);  
	          
	        body = invoke(httpclient, post);  
//	        post.setHeader("Connection","close");  
	        httpclient.getConnectionManager().shutdown();  
	          
	        return body;  
	    }
	    
	   
	    public static String post1(String url, Map<String, String> params) {  
	        DefaultHttpClient httpclient = new DefaultHttpClient(); 
	        httpclient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1500);
//	        httpclient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 1000*5);
	        String body = null;  
	          
	        log.info("create httppost:" + url);  
	        HttpPost post = postForm1(url, params);  
	          
	        body = invoke(httpclient, post);  
	        post.setHeader("Connection","close");
  	        httpclient.getConnectionManager().shutdown();  
	          
	        return body;  
	    } 
	      
	    public static String get(String url) {  
	        DefaultHttpClient httpclient = new DefaultHttpClient();  
	        String body = null;  
	          
	        log.info("create httppost:" + url);  
	        HttpGet get = new HttpGet(url);  
	        body = invoke(httpclient, get);  
	          
	        httpclient.getConnectionManager().shutdown();  
	          
	        return body;  
	    }  
	          
	      
	    private static String invoke(DefaultHttpClient httpclient,  
	            HttpUriRequest httpost) {
	        HttpResponse response = sendRequest(httpclient, httpost);  
	        String body = paseResponse(response);  
	          
	        return body;  
	    }  
	  
	    private static String paseResponse(HttpResponse response) {  
	        log.info("get response from http server..");  
	        HttpEntity entity = response.getEntity();  
	          
	        log.info("response status: " + response.getStatusLine());
	        log.info("response status: " + response.getStatusLine().getReasonPhrase());  
	        System.out.println("response status: " + response.getStatusLine());
	        System.out.println("response status: " + response.getStatusLine().getReasonPhrase());
	        String charset = EntityUtils.getContentCharSet(entity);  
	        log.info(charset);  
	          
	        String body = null;  
	        try {  
	            body = EntityUtils.toString(entity);  
	            log.info(body);  
	        } catch (ParseException e) {  
	            e.printStackTrace();  
	        } catch (IOException e) {  
	            e.printStackTrace();  
	        }  
	          
	        return body;  
	    }  
	  
	    private static HttpResponse sendRequest(DefaultHttpClient httpclient,  
	            HttpUriRequest httpost) {  
	        log.info("execute post...");  
	        HttpResponse response = null;  
	          
	        try {
	            response = httpclient.execute(httpost);  
	            System.out.println(response.getStatusLine());
	        } catch (ClientProtocolException e) {  
	            e.printStackTrace();  
	        } catch (IOException e) {  
	            e.printStackTrace();  
	        }  
	        return response;  
	    }  
	  
	    private static HttpPost postForm(String url, String params,String msgTp){  
	          
	        HttpPost httpost = new HttpPost(url); 
//	        HttpPost httpost=null;
//			try {
//				httpost = new HttpPost(new URI(url));
//			} catch (URISyntaxException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
	         
	        List<NameValuePair> nvps = new ArrayList <NameValuePair>();  
	          
//	        Set<String> keySet = params.keySet();  
//	        for(String key : keySet) {  
//	            nvps.add(new BasicNameValuePair(key, params.get(key)));  
//	        }  
	          
	        try {  
	            log.info("set utf-8 form entity to httppost"); 
	            
	            log.info("send messsage-----------------"+params);
	            StringEntity stringEntity=new StringEntity(params,"UTF-8");
	              
	            stringEntity.setContentType("application/xml");
	            httpost.setEntity(stringEntity );  
	            httpost.setHeader("Connection","close");
//	            httpost.setHeader("Connection","keep-alive");
	            httpost.addHeader("Content-Type", "application/xml;charset=utf-8");
	            httpost.addHeader("MsgTp", msgTp);
	            httpost.addHeader("OriIssrId", "48641000"); 
	            httpost.addHeader("SderReserved", ""); 
	            httpost.addHeader("RcverReserved", ""); 
	            httpost.addHeader("CupsReserved", "");
//	            httpost.addHeader("Content-Encoding", "gzip"); 
//	            httpost.addHeader("Host", "10.51.130.148:8081");
//	            httpost.setHeader("Content-Length",  "991"); 
//	            httpost.headerIterator(HTTP.CONTENT_LEN);
//	            httpost.addHeader( new BasicHeader(HTTP.CONTENT_LEN, "199") );
	        } catch (Exception e) {  
	            e.printStackTrace();  
	        }  
	          
	        return httpost;  
	    } 
	    private static HttpPost postForm1(String url, Map<String, String> params){  
	          
	        HttpPost httpost = new HttpPost(url);  
	        List<NameValuePair> nvps = new ArrayList <NameValuePair>();  
	          
	        Set<String> keySet = params.keySet();  
	        for(String key : keySet) {  
	            nvps.add(new BasicNameValuePair(key, params.get(key)));  
	        }  
	          
	        try {  
	            log.info("set utf-8 form entity to httppost");  
	            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));  
	            httpost.setHeader("Connection","close");
	        } catch (UnsupportedEncodingException e) {  
	            e.printStackTrace();  
	        }  
	          
	        return httpost;  
	    }  
	    public static String postString(String url,String params){
	    	DefaultHttpClient httpclient = new DefaultHttpClient(); 
	        httpclient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1500);
	        HttpPost httpost = new HttpPost(url);
	        HttpResponse response;
	        HttpEntity entity;
	        String body = null;
	        try {
				httpost.setEntity(new StringEntity(params,"UTF-8"));
				response=httpclient.execute(httpost);
				entity=response.getEntity();
				body=EntityUtils.toString(entity);
				httpclient.getConnectionManager().shutdown();
				httpost.setHeader("Connection","close");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return body;
	    	
	    }
	    public static String post(String url, Map<String, String> params) {  
	        DefaultHttpClient httpclient = new DefaultHttpClient(); 
	        httpclient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 1500);
	        httpclient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 1000*10);
	        String body = null;  
	        try{
		        log.info("create httppost:" + url);  
		        HttpPost post = postForm(url, params);  
		          
		        body = invoke(httpclient, post);  
		          
		        httpclient.getConnectionManager().shutdown();  
	        }catch (Exception e) {
	        	log.info("http请求(map)异常; url"+url+";e:"+e);
			} 
	        return body;  
	    } 
	    private static HttpPost postForm(String url, Map<String, String> params){  
	          
	        HttpPost httpost = new HttpPost(url);  
	        List<NameValuePair> nvps = new ArrayList <NameValuePair>();  
	          
	        Set<String> keySet = params.keySet();  
	        for(String key : keySet) {  
	            nvps.add(new BasicNameValuePair(key, params.get(key)));  
	        }  
	          
	        try {  
	            log.info("set utf-8 form entity to httppost");  
	            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));  
	        } catch (UnsupportedEncodingException e) {  
	            e.printStackTrace();  
	        }  
	          
	        return httpost;  
	    }  
}
