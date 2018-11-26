package com.hrtpayment.xpay.quickpay.newCups.util;
import java.util.Calendar;
import java.util.Date;
/**
 * <p>Title: Time  </p>
 * <p>Description: </p>
 *      此类主要用来取得本地系统的系统时间并用下面5种格式显示
 *              1.　YYMMDDHH         8位
 *              2.　YYMMDDHHmm       10位
 *              3.　YYMMDDHHmmss     12位
 *              4.　YYYYMMDDHHmmss   14位
 *              5.　YYMMDDHHmmssxxx  15位 (最后的xxx 是毫秒)
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: hoten </p>
 * @author lqf
 * @version 1.0
 */
public class CTime {
    public static final int YYMMDDhhmmssxxx=15;
    public static final int YYYYMMDDhhmmss=14;
    public static final int YYMMDDhhmmss=12;
    public static final int MMDDHHmmss=13;
    public static final int YYMMDDhhmm=10;
    public static final int YYMMDDhh=8;
/**
 * 取得本地系统的时间，时间格式由参数决定
 * @param format 时间格式由常量决定
 * @return　String 具有format格式的字符串
 */
    public synchronized static String  getTime(int format){
        StringBuffer cTime=new StringBuffer(10);
        Calendar time=Calendar.getInstance();
        int miltime=time.get(Calendar.MILLISECOND);
        int second=time.get(Calendar.SECOND);
        int minute=time.get(Calendar.MINUTE);
        int hour=time.get(Calendar.HOUR_OF_DAY);
        int day =time.get(Calendar.DAY_OF_MONTH);
        int month=time.get(Calendar.MONTH)+1;
        int year =time.get(Calendar.YEAR);
        
        
        if(format!=14){
            if(year>=2000) year=year-2000;
            else year=year-1900;
        }
        if(format>=2){
            if(format==14 && format!=13) cTime.append(year);
            else if(format!=13)   cTime.append(getFormatTime(year,2));
        }
        if(format>=4)
            cTime.append(getFormatTime(month,2));
        if(format>=6)
            cTime.append(getFormatTime(day,2));
        if(format>=8)
            cTime.append(getFormatTime(hour,2));
        if(format>=10)
            cTime.append(getFormatTime(minute,2));
        if(format>=12)
            cTime.append(getFormatTime(second,2));
        if(format>=15)
            cTime.append(getFormatTime(miltime,3));
        return cTime.toString();
    }
    
    /**
     * 取得本地系统的时间，时间格式由参数决定
     * @param format 时间格式 支持由YYYY/YY MM DD HH MI SS以任意格式组成的日期格式
     * @return　String 具有format格式的字符串
     */
        public synchronized static String  getTime(String format){
        	format=format.toUpperCase();
            Calendar time=Calendar.getInstance();
            int second=time.get(Calendar.SECOND);
            int minute=time.get(Calendar.MINUTE);
            int hour=time.get(Calendar.HOUR_OF_DAY);
            int day =time.get(Calendar.DAY_OF_MONTH);
            int month=time.get(Calendar.MONTH)+1;
            int year =time.get(Calendar.YEAR);
            format=format.replaceAll("MM", getFormatTime(month,2));
            format=format.replaceAll("YYYY",String.valueOf(year));
            if(year>=2000) year=year-2000;
            else year=year-1900;
        	format = format.replaceAll("YY",getFormatTime(year,2));
        	format=format.replaceAll("DD", getFormatTime(day,2));
        	format=format.replaceAll("MI", getFormatTime(minute,2));
        	format=format.replaceAll("SS", getFormatTime(second,2));
        	format=format.replaceAll("HH", getFormatTime(hour,2));
        	return format;
        }
    
/**
 *　产生任意位的字符串
 * @param time 要转换格式的时间
 * @param format　转换的格式
 * @return　String 转换的时间
 */
    private synchronized static String getFormatTime(int time,int format){
        StringBuffer numm=new StringBuffer();
        int length=String.valueOf(time).length();

        if(format<length) return null;

        for(int i=0 ;i<format-length ;i++){
            numm.append("0");
        }
        numm.append(time);
        return numm.toString().trim();
    }
    
    public synchronized static String  formatDate(Date date,String format)
    {
    	format=format.toUpperCase();
        Calendar time=Calendar.getInstance();
        time.setTime(date);
        int second=time.get(Calendar.SECOND);
        int minute=time.get(Calendar.MINUTE);
        int hour=time.get(Calendar.HOUR_OF_DAY);
        int day =time.get(Calendar.DAY_OF_MONTH);
        int month=time.get(Calendar.MONTH)+1;
        int year =time.get(Calendar.YEAR);
        format=format.replaceAll("MM", getFormatTime(month,2));
        format=format.replaceAll("YYYY",String.valueOf(year));
        if(year>=2000) year=year-2000;
        else year=year-1900;
    	format = format.replaceAll("YY",String.valueOf(year-2000));
    	format=format.replaceAll("DD", getFormatTime(day,2));
    	format=format.replaceAll("MI", getFormatTime(minute,2));
    	format=format.replaceAll("SS", getFormatTime(second,2));
    	format=format.replaceAll("HH", getFormatTime(hour,2));
    	return format;
    }
    
    /**
     *　获取当前日期的前一天
     * @return　String 转换的时间
     */
    public synchronized static String getBeforeDate(String format){
    	Date date = new Date();
    	Calendar time = Calendar.getInstance();
    	time.setTime(date);
    	time.set(Calendar.DATE,time.get(Calendar.DATE)-1);
    	String end=formatDate(time.getTime(),format);
		return end;
    }
    
 
}