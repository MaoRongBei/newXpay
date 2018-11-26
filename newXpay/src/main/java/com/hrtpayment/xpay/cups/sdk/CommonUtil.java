package com.hrtpayment.xpay.cups.sdk;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

 

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;




/**
 * 工具类
 * 
 * @author xzhu
 */
public class CommonUtil {
	protected static Logger Logger  = LogManager.getLogger();
	
	/**
	 * 将对象转成JSon格式,返回给前台
	 * 
	 * @param response
	 * @param obj
	 * @throws IOException
	 */
	/*public static void writeResult(HttpServletResponse response, Object obj) {
		try {
			response.setContentType("text/html;charset=utf-8");
			PrintWriter write = response.getWriter();
			write.write(JSON.toJSONString(obj));
			write.flush();
			write.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * 判断当前request url是否和excludUrl中设置的url匹配
	 * 
	 * @return 匹配 -> true<br>
	 *         不匹配 -> false
	 */
	/*public static boolean isMatchUrl(HttpServletRequest request,
			String excludeUrl) throws ServletException {
		if (excludeUrl != null
				&& !excludeUrl.trim().equalsIgnoreCase(ApiConstants.STR_BLANK)) {
			String contentPath = request.getContextPath();
			String url = request.getRequestURI();
			String[] urlArr = excludeUrl.split(",");
			for (int i = 0; i < urlArr.length; i++) {
				String excUrl = contentPath + urlArr[i].toString();
				if (excUrl.indexOf("*") > 0) {
					int a = excUrl.indexOf("*");
					excUrl = excUrl.substring(0, a);
				}
				if (url.startsWith(excUrl)) {
					return true;
				}
			}
		}
		return false;
	}*/

	/**
	 * 判断是否为空字符串
	 * 
	 * @author jlni
	 * @param str
	 * @return
	 */
	public static boolean isNullOrEmpty(String str) {
		if (str == null || str.isEmpty())
			return true;
		return false;
	}

	/**
	 * 字符串list以某分隔符连接
	 * 
	 * @author jlni
	 * @param strList
	 * @param sep
	 * @return
	 */
	/*public static String join(List<String> strList, String sep) {
		boolean isFirst = true;
		String joinStr = ApiConstants.STR_BLANK;
		for (String str : strList) {
			if (isFirst) {
				joinStr += str;
				isFirst = false;
			} else {
				joinStr += sep + str;
			}

		}
		return joinStr;
	}*/

    /**
     * 获取配置文件内容
     * @since 
     * @param fileName 配置文件名称
     * @param key 
     * @return
     */
    /*public static String getPropertyByKey(String fileName, String key) {
        try {
            Properties pps = new Properties();
            ClassPathResource resource = new ClassPathResource("/appCfg/" + fileName);
            String ppsPath = resource.getURL().getPath().replace("20%", "");
            InputStream in = new BufferedInputStream(new FileInputStream(ppsPath));
            pps.load(in);
            String value = pps.getProperty(key);
            in.close();
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }*/
    
    
    /**
     * 检查指定的字符串是否为空。
     * <ul>
     * <li>SysUtils.isEmpty(null) = true</li>
     * <li>SysUtils.isEmpty("") = true</li>
     * <li>SysUtils.isEmpty("   ") = true</li>
     * <li>SysUtils.isEmpty("abc") = false</li>
     * </ul>
     * 
     * @param value 待检查的字符串
     * @return true/false
     */
	public static boolean isEmpty(String value) {
		int strLen;
		if (value == null || (strLen = value.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(value.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	}
	
	/**
     * 检查指定的字符串列表是否不为空。
     */
	public static boolean areNotEmpty(String... values) {
		boolean result = true;
		if (values == null || values.length == 0) {
			result = false;
		} else {
			for (String value : values) {
				result &= !isEmpty(value);
			}
		}
		return result;
	}
	
	/**
     * 递归删除目录下的所有文件及子目录下所有文件
     * 
     * @param dir
     *            将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful. If a deletion fails, the method stops attempting
     *         to delete and returns "false".
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            // 递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }
    
    /**
     * map按照key值升序排列
     * @param params
     * @return
     */
    public static Map<String,String> sortMap(Map<String, String> params) {
        if (params == null) {
            return null;
        }
        List<String> keys = new ArrayList<String>(params.keySet());
        Collections.sort(keys);
        TreeMap<String, String > resultMap= new TreeMap<String, String >();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            resultMap.put(key, value);
        }
        return resultMap;
    }
    
    
    public static String coverMap2JsonString(Map<String, String> data) {
		TreeMap<String, String> tree = new TreeMap<String, String>();
		Iterator<Entry<String, String>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			if (ApiConstants.FIELD_SIGNMETHOD.equals(en.getKey().trim())) {
				continue;
			}
			tree.put(en.getKey(), en.getValue());
		}
		it = tree.entrySet().iterator();
		StringBuffer sf = new StringBuffer("{");
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			sf.append("\"").append(en.getKey()).append("\":\"")
					.append(en.getValue()).append("\",");
		}
		String str = sf.substring(0, sf.length() - 1);
		return new StringBuffer(str).append("}").toString();
	}
    
    public static Map<String,String> jsonStr2Map(String jsonResult){
		Map<String,String>  tmpRspData = new HashMap<String,String>();
		JSONObject jsonObject =JSONObject.parseObject(jsonResult);
		Iterator<String> iterator = jsonObject.keySet().iterator();
        String key = null;
        String value = null;
        while (iterator.hasNext()) {
            key = (String) iterator.next();
			value = jsonObject.getString(key);
            tmpRspData.put(key, value);
        }
	    return tmpRspData;
	}
    /**
	 * 将Map中的数据转换成key1=value1&key2=value2的形式 不包含签名域signature
	 * 
	 * @param data
	 *            待拼接的Map数据
	 * @return 拼接好后的字符串
	 */
	public static String coverMap2String(Map<String, String> data) {
		TreeMap<String, String> tree = new TreeMap<String, String>();
		Iterator<Entry<String, String>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			if (ApiConstants.FIELD_SIGNMETHOD.equals(en.getKey().trim())) {
				continue;
			}
			tree.put(en.getKey(), en.getValue());
		}
		it = tree.entrySet().iterator();
		StringBuffer sf = new StringBuffer();
		while (it.hasNext()) {
			Entry<String, String> en = it.next();
			sf.append(en.getKey() + ApiConstants.EQUAL + en.getValue()
					+ ApiConstants.AMPERSAND);
		}
		return sf.substring(0, sf.length() - 1);
	}
    /**
     * 获取文件的真实后缀名。目前只支持JPG, GIF, PNG, BMP四种图片文件。
     * 
     * @param bytes 文件字节流
     * @return JPG, GIF, PNG or null
     */
    public static String getFileSuffix(byte[] bytes) {
        if (bytes == null || bytes.length < 10) {
            return null;
        }

        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "GIF";
        } else if (bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "PNG";
        } else if (bytes[6] == 'J' && bytes[7] == 'F' && bytes[8] == 'I' && bytes[9] == 'F') {
            return "JPG";
        } else if (bytes[0] == 'B' && bytes[1] == 'M') {
            return "BMP";
        } else {
            return null;
        }
    }

    /**
     * 获取文件的真实媒体类型。目前只支持JPG, GIF, PNG, BMP四种图片文件。
     * 
     * @param bytes 文件字节流
     * @return 媒体类型(MEME-TYPE)
     */
    public static String getMimeType(byte[] bytes) {
        String suffix = getFileSuffix(bytes);
        String mimeType;

        if ("JPG".equals(suffix)) {
            mimeType = "image/jpeg";
        } else if ("GIF".equals(suffix)) {
            mimeType = "image/gif";
        } else if ("PNG".equals(suffix)) {
            mimeType = "image/png";
        } else if ("BMP".equals(suffix)) {
            mimeType = "image/bmp";
        } else {
            mimeType = "application/octet-stream";
        }

        return mimeType;
    }
    
  /*  public static void main(String[] args) {
    	HashMap<String, String > mapTest= new HashMap<String, String >();
    	mapTest.put("dd", "55");
    	mapTest.put("acg", "22");
    	mapTest.put("ab", "23");
    	Map<String, String > mapTest2 =sortMap(mapTest);
    	System.out.println("------");
    	  Iterator<Map.Entry<String, String>> it = mapTest2.entrySet().iterator();
          while (it.hasNext()) {
                 Map.Entry<String, String> entry = it.next();
                  System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
          }  
	}
    */
     
}
