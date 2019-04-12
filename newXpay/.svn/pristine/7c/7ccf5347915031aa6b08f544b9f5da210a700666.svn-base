package com.hrtpayment.xpay.quickpay.cups.util;

import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;

/**
 * <p>The Super Java Framework .</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * @author Steven
 * @version 1.0
 */

public class BASE64Util {

  /**
   * 利用JDK自带的BASE64加密工具类进行编码，参数要求为byte[]
    @param byte[] b
    @return String
   */

  public static String encodeBySun(byte[] b) {
    return new BASE64Encoder().encode(b);
  }

  /**
   * BASE64加密工具类
    @param String s
    @return String
   */
  public static String encode(String s) {
    if (s == null)return null;
    return new BASE64Encoder().encode(s.getBytes());
  }

  /**
   * 利用JDK自带的BASE64解密工具类
    @param String s
    @return byte[]
   */
  public static byte[] decodeBySun(String s) {
    if (s == null)return null;
    try {
      BASE64Decoder decoder = new BASE64Decoder();
      byte[] b = decoder.decodeBuffer(s);
      return b;
    }
    catch (Exception e) {
      return null;
    }

  }

  /**
   * BASE64解密工具类
    @param String s
    @return String
   */

  public static String decodeWithString(String s) {
    if (s == null)return null;
    BASE64Decoder decoder = new BASE64Decoder();
    try {
      byte[] b = decoder.decodeBuffer(s);
      return new String(b);
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * BASE64解密工具类
    @param String s
    @return String
   */

  public static String decode(String s) {
    StringBuffer stringbuffer = new StringBuffer();
    byte abyte0[] = new byte[4];
    int i;
    if (s.length() % 3 == 0) {
      i = s.length();
    }
    else {
      i = s.length() + (3 - s.length() % 3);
    }
    int j = 0;
    int k = 0;
    for (; j < i; j++) {
      boolean flag = false;
      byte byte0;
      if (j < s.length()) {
        byte0 = (byte) s.charAt(j);
      }
      else {
        byte0 = 0;
      }
      if (byte0 >= 65 && byte0 < 91) {
        abyte0[k] = (byte) (byte0 - 65);
      }
      else
      if (byte0 >= 97 && byte0 < 123) {
        abyte0[k] = (byte) (byte0 - 71);
      }
      else
      if (byte0 >= 48 && byte0 < 58) {
        abyte0[k] = (byte) (byte0 + 4);
      }
      else
      if (byte0 == 43) {
        abyte0[k] = 62;
      }
      else
      if (byte0 == 47) {
        abyte0[k] = 63;
      }
      else
      if (byte0 == 61) {
        abyte0[k] = 0;
      }
      if (!flag && ++k == 4) {
        int l = (abyte0[0] << 18) + (abyte0[1] << 12) + (abyte0[2] << 6) +
            abyte0[3];
        for (int i1 = 16; i1 >= 0; i1 -= 8) {
          byte byte1 = (byte) (l >> i1);
          if (byte1 > 0) {
            stringbuffer.append( (char) byte1);
          }
        }

        k = 0;
        abyte0[0] = 0;
        abyte0[1] = 0;
        abyte0[2] = 0;
        abyte0[3] = 0;
      }
    }

    return stringbuffer.toString();
  }

}
