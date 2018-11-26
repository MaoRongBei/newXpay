package com.hrtpayment.xpay.utils;

public class BinaryEncoder {
    private static final char[] decodingTable =
    {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    private static final String[] encodingTable =
    {
        "0000", "0001","0010","0011","0100","0101","0110","0111","1000","1001","1010","1011","1100","1101","1110","1111"
    };
    
	public static String binToHex(String data){
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < data.length(); i+=4) {
			String bin = data.substring(i, i+4);
			int index = Integer.valueOf(bin, 2);
			result.append(decodingTable[index]);
		}
		return result.toString();
	}
	
	public static String hexToBin(String data) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < data.length(); i++) {
			int n = Integer.valueOf(data.substring(i,i+1),16);
			result.append(encodingTable[n]);
		}
		return result.toString();
	}
	
	public static String bytesToBin(byte[] msg, int off, int length){
		StringBuilder result = new StringBuilder();
		for (int i = off; i < off+length; i++) {
			final byte b = msg[i];
			int b1 = (b & 0B11110000)>>>4;
			result.append(encodingTable[b1]);
			int b2 = b & 0B00001111;
			result.append(encodingTable[b2]);
		}
		return result.toString();
	}
}
