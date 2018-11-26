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
package com.hrtpayment.xpay.quickpay.cups.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Enumeration;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

/**
 * RSA 证书工具类： 从X509证书中获取公钥 ;从 PKXS12、JKS、PKCS8 证书中获取私钥
 */
public class RsaCertUtils {

	private static final Logger logger = LogManager.getLogger();

	/**
	 * 将证书文件读取为证书存储对象：证书文件类型可为：JKS（.keystore等），PKCS12（.pfx）
	 * 
	 * @param keyfile
	 *            证书文件名
	 * @param keypwd
	 *            证书密码
	 * @param type
	 *            证书类型
	 * @return 证书对象
	 */
	private static KeyStore getKeyStore(String keyfile, String keypwd, String type) {
		try {
			KeyStore keyStore = null;
			if ("JKS".equals(type)) {
				keyStore = KeyStore.getInstance(type);
			} else if ("PKCS12".equals(type)) {
				Security.insertProviderAt(new BouncyCastleProvider(), 1);
				Security.addProvider(new BouncyCastleProvider());
				keyStore = KeyStore.getInstance(type);
			}
			FileInputStream fis = new FileInputStream(keyfile);
			char[] nPassword = null;
			nPassword = null == keypwd || "".equals(keypwd.trim()) ? null : keypwd.toCharArray();
			keyStore.load(fis, nPassword);
			fis.close();
			return keyStore;
		} catch (Exception e) {
			if (Security.getProvider("BC") == null) {
				logger.info("BC Provider not installed.");
			}
			logger.error("Fail: load privateKey certificate", e);
		}
		return null;
	}

	/**
	 * 从私钥证书中获取私钥
	 * 
	 * @param keyfile
	 *            证书文件(.pfx, .keystore)
	 * @param keypwd
	 *            证书密码
	 * @param type
	 *            证书类型
	 * @return：私钥
	 */
	public static PrivateKey getPriKeyPkcs12(String keyfile, String keypwd, String type) {
		KeyStore keyStore = getKeyStore(keyfile, keypwd, type);
		PrivateKey privateKey = null;
		try {
			Enumeration<String> aliasenum = keyStore.aliases();
			String keyAlias = null;
			if (aliasenum.hasMoreElements()) {
				// 第一个条目
				keyAlias = (String) aliasenum.nextElement();
			}
			privateKey = (PrivateKey) keyStore.getKey(keyAlias, keypwd.toCharArray());
		} catch (Exception e) {
			logger.error("Fail: get private key from private certificate", e);
		}
		logger.debug("private key: " + Hex.toHexString(privateKey.getEncoded()));
		return privateKey;
	}

	public static PrivateKey getPriKeyJks(String keyfile, String keypwd, String type) {
		return getPriKeyPkcs12(keyfile, keypwd, type);
	}

	/**
	 * 从文件中加载私钥
	 * 
	 * @param pemkeyfile
	 *            私钥文件名 (.pem)
	 * @return 私钥
	 */
	public static PrivateKey getPriKeyPkcs8(String pemkeyfile) {
		PrivateKey privateKey = null;
		try {
			byte[] buffer = readFile(pemkeyfile);
			// byte[] buffer = readFileBytes(pemkeyfile);
			buffer = Base64.decodeBase64(buffer);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			privateKey = keyFactory.generatePrivate(keySpec);
		} catch (Exception e) {
			logger.error("Fail: get private key from private certificate", e);
		}
		logger.debug("private key: " + Hex.toHexString(privateKey.getEncoded()));
		return privateKey;
	}

	/**
	 * 从公钥证书中获取公钥
	 * 
	 * @param filePath
	 * @return
	 */
	public static PublicKey getPubKey(String filePath) {
		X509Certificate publicCert = getPubCert(filePath);
		PublicKey publicKey = publicCert.getPublicKey();
		logger.debug("public key: " + Hex.toHexString(publicKey.getEncoded()));
		return publicKey;
	}

	private static byte[] readFile(String filePath) throws Exception {
		BufferedReader br = null;
		br = new BufferedReader(new FileReader(filePath));
		String readLine = null;
		StringBuilder sb = new StringBuilder();
		while ((readLine = br.readLine()) != null) {
			if (readLine.startsWith("--")) {
				continue;
			}
			sb.append(readLine);
		}
		br.close();
		return sb.toString().getBytes();
	}

	/**
	 * 读取公钥证书
	 * 
	 * @param filePath
	 *            文件路径
	 * @return 证书对象
	 */
	private static X509Certificate getPubCert(String filePath) {
		CertificateFactory cf = null;
		FileInputStream in = null;
		X509Certificate x509Certificate = null;
		try {
			cf = CertificateFactory.getInstance("X.509");
			in = new FileInputStream(filePath);
			x509Certificate = (X509Certificate) cf.generateCertificate(in);
		} catch (Exception e) {
			logger.error("Fail: load public certificate", e);
		} finally {
			if (null != in) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error("Fail: close FileInputStream", e);
				}
			}
		}
		return x509Certificate;
	}

}
