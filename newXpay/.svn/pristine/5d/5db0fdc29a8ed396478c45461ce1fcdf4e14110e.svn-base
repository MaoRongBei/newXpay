package com.hrtpayment.xpay.pos.server;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hrtpayment.xpay.utils.BinaryEncoder;
import com.hrtpayment.xpay.utils.Hex;
import com.hrtpayment.xpay.utils.crypto.DesUtil;

public class PosMsg {
	private static final Logger logger = LogManager.getLogger();
	private String tpdu;
	private String head;
	private String msgType;
	private String bitmap;
	private String bit2;
	private String bit3;
	private String bit4;
	private String bit11;
	private String bit12;
	private String bit13;
	private String bit25;
	private String bit37;
	private String bit39;
	private String bit41;
	private String bit42;
	private String bit56;
	private String bit61;
	private String bit64;
	private String df26;
	private String df27;
	private String df28;
	private String df29;
	private String df30;
	private String df31;
	private String df32;
	private String df33;
	private String df34;
	private String df35;
	private byte[] msg;
	private int index;

	public String assemble() {
		assembleField61();
		StringBuilder bitMap = new StringBuilder();
		bitMap.append("0");
		bitMap.append(getBit2()==null?"0":"1");
		bitMap.append(getBit3()==null?"0":"1");
		bitMap.append(getBit4()==null?"0":"1");
		bitMap.append("000000");
		bitMap.append(getBit11()==null?"0":"1");
		bitMap.append(getBit12()==null?"0":"1");
		bitMap.append("000000000000");
		bitMap.append(getBit25()==null?"0":"1");
		bitMap.append("00000000000");
		bitMap.append(getBit37()==null?"0":"1");
		bitMap.append("0");
		bitMap.append(getBit39()==null?"0":"1");
		bitMap.append("0");
		bitMap.append(getBit41()==null?"0":"1");
		bitMap.append(getBit42()==null?"0":"1");
		bitMap.append("0000000000000");
		bitMap.append(getBit56()==null?"0":"1");
		bitMap.append("0000");
		bitMap.append(getBit61()==null?"0":"1");
		bitMap.append("001");
		StringBuilder message = new StringBuilder();
		message.append(tpdu);
		message.append(head);
		message.append(getMsgType());
		message.append(BinaryEncoder.binToHex(bitMap.toString()));
		if (getBit2()!=null) {
			int len = getBit2().length();
			message.append(getBcdLen(len, 2));
			message.append(getBit2());
			if (len%2>0) {
				message.append("0");
			}
		}
		if (getBit3()!=null) {
			message.append(getBit3());
		}
		if (getBit4()!=null) {
			message.append(getBit4());
		}
		if (getBit11()!=null) {
			message.append(getBit11());
		}
		if (getBit12()!=null) {
			message.append(getBit12());
		}
		if (getBit13()!=null) {
			message.append(getBit13());
		}
		if (getBit25()!=null) {
			message.append(getBit25());
		}
		if (getBit37()!=null) {
			message.append(new String(Hex.encode(getBit37().getBytes())));
		}
		if (getBit39()!=null) {
			message.append(new String(Hex.encode(getBit39().getBytes())));
		}
		if (getBit41()!=null) {
			message.append((new String(Hex.encode(getBit41().getBytes()))));
		}
		if (getBit42()!=null) {
			message.append((new String(Hex.encode(getBit42().getBytes()))));
		}
		if (getBit56()!=null) {
			int len = getBit56().length();
			message.append(getBcdLen(len, 4));
			message.append(new String(Hex.encode(getBit56().getBytes())));
		}
		if (getBit61()!=null) {
			assembleField61();
			int len = getBit61().length()>>1;
			message.append(getBcdLen(len, 4));
			message.append(getBit61());
		}
		setBit64(assembleMac());
		message.append(getBit64());
		return message.toString();
	}
	public void parse(byte[] msg) {
		this.msg = msg;
		setTpdu(getHex(10));

		setHead(getHex(4));

		setMsgType(getHex(4));

		setBitmap(getBinaryString(64));
		if (hasField(2)) {
			int len = getBcdInt(1);
			setBit2(getHex(len));
		}
		if (hasField(3)) {
			setBit3(getHex(6));
		}
		if (hasField(4)) {
			setBit4(getHex(12));
		}
		if (hasField(11)) {
			setBit11(getHex(6));
		}
		if (hasField(12)) {
			setBit12(getHex(6));
		}
		if (hasField(13)) {
			setBit13(getHex(4));
		}
		if (hasField(25)) {
			setBit25(getHex(2));
		}
		if (hasField(37)) {
			setBit37(getString(12));
		}
		if (hasField(39)) {
			setBit39(getString(2));
		}
		if (hasField(41)) {
			setBit41(getString(8));
		}
		if (hasField(42)) {
			setBit42(getString(15));
		}
		if (hasField(56)) {
			int len = getBcdInt(2);
			setBit56(getString(len));
		}
		if (hasField(61)) {
			int len = getBcdInt(4)<<1;
			setBit61(getHex(len));
			parseField61();
		}
		if (hasField(64)) {
			setBit64(getHex(16));
		}
	}
	public String getBcdLen(int n, int len){
		StringBuilder result = new StringBuilder();
		String str = Integer.toString(n);
		int zeroNum = len - str.length();
		for (int i = 0; i < zeroNum; i++) {
			result.append("0");
		}
		result.append(str);
		if (result.length()>len) {
			result.delete(0, result.length()-len);
		}
		return result.toString();
	}
	public String getHexLen(int n, int len){
		StringBuilder result = new StringBuilder();
		String str = Integer.toHexString(n);
		int zeroNum = len - str.length();
		for (int i = 0; i < zeroNum; i++) {
			result.append("0");
		}
		result.append(str);
		if (result.length()>len) {
			result.delete(0, result.length()-len);
		}
		return result.toString();
	}
	private boolean hasField(int n) {
		return bitmap.charAt(n - 1) == '1';
	}
	private String getHex(int length) {
		int len = (length + length % 2) >> 1;
		String result = new String(
				Hex.encode(Arrays.copyOfRange(msg, index, index + len)));
		this.index += len;
		return result;
	}
	private String getBinaryString(int length) {
		int len = length >> 3;
		String result = BinaryEncoder.bytesToBin(msg, index, len);
		index += len;
		return result;
	}
	private String getString(int length) {
		String result = new String(
				Arrays.copyOfRange(msg, index, index + length));
		index += length;
		return result;
	}
	private int getBcdInt(int length) {
		int len = (length + length % 2) >> 1;
		byte[] result = Arrays.copyOfRange(msg, index, index + len);

		index += len;
		return Integer.valueOf(new String(Hex.encode(result)));
	}
	public String getTpdu() {
		return tpdu;
	}
	public String getHead() {
		return head;
	}
	public String getMsgType() {
		return msgType;
	}
	public String getBitmap() {
		return bitmap;
	}
	public String getBit2() {
		return bit2;
	}
	public String getBit3() {
		return bit3;
	}
	public String getBit4() {
		return bit4;
	}
	public String getBit11() {
		return bit11;
	}
	public String getBit12() {
		return bit12;
	}
	public String getBit13() {
		return bit13;
	}
	public String getBit25() {
		return bit25;
	}
	public String getBit37() {
		return bit37;
	}
	public String getBit39() {
		return bit39;
	}
	public String getBit41() {
		return bit41;
	}
	public String getBit42() {
		return bit42;
	}
	public String getBit61() {
		return bit61;
	}
	public String getBit56() {
		return bit56;
	}
	public String getBit64() {
		return bit64;
	}
	public String getDf26() {
		return df26;
	}
	public String getDf27() {
		return df27;
	}
	public String getDf28() {
		return df28;
	}
	public String getDf29() {
		return df29;
	}
	public String getDf30() {
		return df30;
	}
	public void setTpdu(String tpdu) {
		this.tpdu = tpdu;
	}
	public void setHead(String head) {
		this.head = head;
	}
	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}
	public void setBitmap(String bitmap) {
		this.bitmap = bitmap;
	}
	public void setBit2(String bit2) {
		this.bit2 = bit2;
	}
	public void setBit3(String bit3) {
		this.bit3 = bit3;
	}
	public void setBit4(String bit4) {
		this.bit4 = bit4;
	}
	public void setBit11(String bit11) {
		this.bit11 = bit11;
	}
	public void setBit12(String bit12) {
		this.bit12 = bit12;
	}
	public void setBit13(String bit13) {
		this.bit13 = bit13;
	}
	public void setBit25(String bit25) {
		this.bit25 = bit25;
	}
	public void setBit37(String bit37) {
		this.bit37 = bit37;
	}
	public void setBit39(String bit39) {
		this.bit39 = bit39;
	}
	public void setBit41(String bit41) {
		this.bit41 = bit41;
	}
	public void setBit42(String bit42) {
		this.bit42 = bit42;
	}
	public void setBit61(String bit61) {
		this.bit61 = bit61;
	}
	public void setBit56(String bit56) {
		this.bit56 = bit56;
	}
	public void setBit64(String bit64) {
		this.bit64 = bit64;
	}
	public void setDf26(String df26) {
		this.df26 = df26;
	}
	public void setDf27(String df27) {
		this.df27 = df27;
	}
	public void setDf28(String df28) {
		this.df28 = df28;
	}
	public void setDf29(String df29) {
		this.df29 = df29;
	}
	public void setDf30(String df30) {
		this.df30 = df30;
	}
	
	public String getDf31() {
		return df31;
	}
	public void setDf31(String df31) {
		this.df31 = df31;
	}
	public String getDf32() {
		return df32;
	}
	public void setDf32(String df32) {
		this.df32 = df32;
	}
	public String getDf33() {
		return df33;
	}
	public void setDf33(String df33) {
		this.df33 = df33;
	}
	public String getDf34() {
		return df34;
	}
	public void setDf34(String df34) {
		this.df34 = df34;
	}
	public String getDf35() {
		return df35;
	}
	public void setDf35(String df35) {
		this.df35 = df35;
	}
	private void assembleField61(){
		StringBuilder f61 = new StringBuilder();
		if(getDf26()!=null){
			f61.append("df26");
			f61.append(getHexLen(getDf26().length(), 2));
			f61.append(new String(Hex.encode(getDf26().getBytes())));
		}
		if(getDf27()!=null){
			f61.append("df27");
			f61.append(getHexLen(getDf27().length(), 2));
			f61.append(new String(Hex.encode(getDf27().getBytes())));
		}
		if(getDf28()!=null){
			f61.append("df28");
			f61.append(getHexLen(getDf28().length(),2));
			f61.append(new String(Hex.encode(getDf28().getBytes())));
		}
		if(getDf29()!=null){
			f61.append("df29");
			f61.append(getHexLen(getDf29().length(),2));
			f61.append(new String(Hex.encode(getDf29().getBytes())));
		}
		if(getDf30()!=null){
			f61.append("df30");
			f61.append(getHexLen(getDf30().length(),2));
			f61.append(new String(Hex.encode(getDf30().getBytes())));
		}
		if(getDf31()!=null){
			f61.append("df31");
			f61.append(getHexLen(getDf31().length(),2));
			f61.append(new String(Hex.encode(getDf31().getBytes())));
		}
		if(getDf32()!=null){
			f61.append("df32");
			f61.append(getHexLen(getDf32().length(),2));
			f61.append(new String(Hex.encode(getDf32().getBytes())));
		}
		if(getDf33()!=null){
			f61.append("df33");
			f61.append(getHexLen(getDf33().length(),2));
			f61.append(new String(Hex.encode(getDf33().getBytes())));
		}
		if(getDf34()!=null){
			f61.append("df34");
			f61.append(getHexLen(getDf34().length(),2));
			f61.append(new String(Hex.encode(getDf34().getBytes())));
		}
		if(getDf35()!=null){
			f61.append("df35");
			f61.append(getHexLen(getDf35().length(),2));
			f61.append(new String(Hex.encode(getDf35().getBytes())));
		}
		
		if (f61.length()>0) {
			setBit61(f61.toString());
		}
	}
	private void parseField61(){
		String f61 = getBit61();
		int f61len = f61.length();
		int n = 0;
		if (f61len>n+4 && "df26".equals(f61.substring(n, n+4))){
			n+=4;
			int len = Integer.valueOf(f61.substring(n,n+2),16)<<1;
			n+=2;
			setDf26(new String(Hex.decode(f61.substring(n,n+len))));
			n+=len;
		}
		if (f61len>n+4 && "df27".equals(f61.substring(n, n+4))){
			n+=4;
			int len = Integer.valueOf(f61.substring(n,n+2),16)<<1;
			n+=2;
			setDf27(new String(Hex.decode(f61.substring(n,n+len))));
			n+=len;
		}
		if (f61len>n+4 && "df28".equals(f61.substring(n, n+4))){
			n+=4;
			int len = Integer.valueOf(f61.substring(n,n+2),16)<<1;
			n+=2;
			setDf28(new String(Hex.decode(f61.substring(n,n+len))));
			n+=len;
		}
		if (f61len>n+4 && "df29".equals(f61.substring(n, n+4))){
			n+=4;
			int len = Integer.valueOf(f61.substring(n,n+2),16)<<1;
			n+=2;
			setDf29(new String(Hex.decode(f61.substring(n,n+len))));
			n+=len;
		}
		if (f61len>n+4 && "df30".equals(f61.substring(n, n+4))){
			n+=4;
			int len = Integer.valueOf(f61.substring(n,n+2),16)<<1;
			n+=2;
			setDf30(new String(Hex.decode(f61.substring(n,n+len))));
			n+=len;
		}
		if (f61len>n+4 && "df35".equals(f61.substring(n, n+4))){
			n+=4;
			int len = Integer.valueOf(f61.substring(n,n+2),16)<<1;
			n+=2;
			setDf35(new String(Hex.decode(f61.substring(n,n+len))));
			n+=len;
		}
		
	}
	public String assembleMac(){
		
		try {
			String str = getBit41();
			byte[] data = str.getBytes();
			
			String keyStr = getBit4().substring(1)+getBit11()+getBit42();
			keyStr = keyStr + keyStr.substring(0, 16);
			byte[] mac = DesUtil.des3EncodeECB(Hex.decode(keyStr), data);
			return new String(Hex.encode(mac)).toUpperCase().substring(0,16);
		} catch (Exception e) {
			logger.error("计算mac错误", e);
			return "0000000000000000";
		}
	}
	public boolean checkMac(){
		String mac = assembleMac();
		return mac.startsWith(getBit64().toUpperCase());
	}
}
