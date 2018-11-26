/*
 * 版权说明：
 *1.中国银联股份有限公司（以下简称“中国银联”）对该代码保留全部知识产权权利， 包括但不限于版权、专利、商标、商业秘密等。
 *  任何人对该代码的任何使用都要 受限于在中国银联成员机构服务平台（http://member.unionpay.com/）与中国银
 *  联签 署的协议之规定。中国银联不对该代码的错误或疏漏以及由此导致的任何损失负 任何责任。中国银联针  对该代码放弃所有明
 *  示或暗示的保证,包括但不限于不侵 犯第三方知识产权。
 *  
 *2.未经中国银联书面同意，您不得将该代码用于与中国银联合作事项之外的用途和目的。未经中国银联书面同意，不得下载、
 *  转发、公开或以其它任何形式向第三方提供该代码。如果您通过非法渠道获得该代码，请立即删除，并通过合法渠道 向中国银
 *  联申请。
 *  
 *3.中国银联对该代码或与其相关的文档是否涉及第三方的知识产权（如加密算法可 能在某些国家受专利保护）不做任何声明和担
 *  保，中国银联对于该代码的使用是否侵犯第三方权利不承担任何责任，包括但不限于对该代码的部分或全部使用。
 *
 */
package com.hrtpayment.xpay.quickpay.newCups.util;

import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hrtpayment.xpay.utils.Hex;

/**
 * RSA 非对称、公钥加密,签名验签：基于数学函数
 * 	公钥(n,e)加密: m^e=c(mod n)，m明文，c密文
 *	私钥(n,d)解密: c^d=m(mod n)，c密文，m明文
 */
public class RsaUtils {

	private static Logger logger = LogManager.getLogger();

	public static final String KEY_ALGORITHM = "RSA";

	/**
	 * 算法常量
	 */
	public static final String SIGN_ALGORITHM_SHA256RSA = "SHA256withRSA";

	/**
	 * RSA Ecb模式 公钥加密
	 * @param publicKey 公钥
	 * @param data 明文
	 * @param padMode 填充模式
	 * @return 密文
	 */
	public static byte[] rsaEcbEncrypt(RSAPublicKey publicKey, byte[] data, String padMode) {
		//
		String algorithm = "RSA/ECB/" + padMode;
		byte[] res = null;
		if (publicKey == null) {
			logger.error("publicKey is null");
		}
		try {
			Cipher cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			res = cipher.doFinal(data);
		} catch (Exception e) {
			logger.error("Fail: RSA Ecb Encrypt",e);
		} 
		return res;
	}

	/**
	 * RSA Ecb 私钥解密
	 * @param privateKey 私钥
	 * @param data 密文
	 * @param padMode 填充模式
	 * @return 明文
	 */
	public static byte[] rsaEcbDecrypt(RSAPrivateKey privateKey, byte[] data, String padMode) {
		if (privateKey == null) {
			logger.error("privateKey is null");
		}
		String algorithm = "RSA/ECB/" + padMode;
		byte[] res = null;
		try {
			Cipher cipher = Cipher.getInstance(algorithm);
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			res = cipher.doFinal(data);
		} catch (Exception e) {
			logger.error("Fail: RSA Ecb Decrypt",e);
		} 
		return res;
	}

	/**
	 * RSA Sha256摘要  私钥签名
	 * @param privateKey 私钥
	 * @param data 消息
	 * @return 签名
	 */
	public static byte[] signWithSha256(RSAPrivateKey privateKey, byte[] data) {
	 
		byte[] result = null;
		Signature st;
		try {
			st = Signature.getInstance(SIGN_ALGORITHM_SHA256RSA);
			st.initSign(privateKey);
			st.update(data);
			result = st.sign();
		} catch (Exception e) {
			logger.error("Fail: RSA with sha256 sign",e);
		} 
		return result;
	}

	/**
	 * RSA Sha256摘要  公钥验签
	 * @param pubKey 公钥
	 * @param data 消息
	 * @param sign 签名
	 * @return 验签结果
	 */
	public static boolean verifyWithSha256(RSAPublicKey publicKey, byte[] data, byte[] sign) {
		boolean correct = false;
		try {
			Signature st = Signature.getInstance(SIGN_ALGORITHM_SHA256RSA);
			st.initVerify(publicKey);
			st.update(data);
			correct = st.verify(sign);
		} catch (Exception e) {
			logger.error("Fail: RSA with sha256 verify",e);
		} 
		return correct;
	}
}
