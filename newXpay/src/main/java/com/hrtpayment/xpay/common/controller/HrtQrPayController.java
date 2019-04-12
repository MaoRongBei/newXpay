package com.hrtpayment.xpay.common.controller;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hrtpayment.xpay.channel.bean.HrtDeviceTypeInfoBean;
import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.channel.service.ChannelService;
import com.hrtpayment.xpay.cmbc.bean.json.HrtCmbcBean;
import com.hrtpayment.xpay.common.service.impl.ManageService;
import com.hrtpayment.xpay.common.service.impl.MerchantService;
import com.hrtpayment.xpay.common.service.impl.XpayService;
import com.hrtpayment.xpay.quickpay.cups.util.CTime;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.DateUtil;
import com.hrtpayment.xpay.utils.SimpleXmlUtil;
import com.hrtpayment.xpay.utils.exception.BusinessException;

/**
 * 和融通二维码支付
 * 扫描和融通二维码,输入金额,根据浏览器判断走微信还是支付宝
 * @author aibing
 * 2016年11月18日
 */
@Controller
@RequestMapping("xpay")
public class HrtQrPayController {
	Logger logger = LogManager.getLogger();
	private final BigDecimal MIN_AMOUNT = new BigDecimal(5);
	@Autowired
	MerchantService merService;	
	@Autowired
	XpayService service;
	@Autowired
	ManageService manageService;
	
	@Autowired
	ChannelService ch;

	@Value("${xpay.timeStamp.step}")
	int step;
	@Value("${xpay.checkPayOrd.count}")
	Integer count;
	@Value("${xpay.daepay.starttime}")
	Integer startTime;
	@Value("${xpay.daepay.endtime}")
	Integer endTime;
	@Value("${xpay.maxAmtForTerms.wx}")
	long wxMaxAmt;
	@Value("${xpay.maxAmtForTerms.zfb}")
	long zfbMaxAmt;
	@Value("${xpay.qrBarcodePay.depositcaps}")
	String depositCaps;
	@Value("${xpay.qrBarcodePay.depositcollars}")
	String depositCollars;
	@Value("${xpay.qrBarcodePay.barlimit}")
	String barLimit;
	@Value("${xpay.qrBarcodePay.wxcode}")
	String wxCode;
	@Value("${xpay.qrBarcodePay.zfbcode}")
	String zfbCode;
	
	/**
	 * 查询商户名称,显示在页面上
	 * @param mid
	 * @return
	 */
	@RequestMapping("querymername")
	@ResponseBody
	public String queryMerName(@RequestParam String mid,@RequestParam String paytype) {
		String merName = merService.queryMerNameForHrt(mid,paytype==null?"ZS":paytype);
		logger.info(merName);
		return merName == null ? "" : merName;
	}
	/**
	 * 和融通二维码入口(金融的UA是jdjr，商城的ＵＡ是jdapp，钱包的ＵＡ是WalletClient)
	 * @param mid 
	 * @param request
	 * @return
	 */
	@RequestMapping("qrpayment")
	public String auth(@RequestParam String mid,@RequestParam long timeStamp, HttpServletRequest request) {
		try {
//			timeStamp=(new Date()).getTime();
//			System.out.println((new Date()).getTime());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Calendar beforeTime = Calendar.getInstance();
//			beforeTime.add(Calendar.MINUTE, -3);// 3分钟之前的时间  
			try{
				step=(new Double(RedisUtil.getProperties("step")).intValue());
				if (step==0) {
					timeStamp=(new Date()).getTime();
				}
			}catch(Exception e){
				logger.error("redis连接异常{}",e.getMessage());
				timeStamp=(new Date()).getTime();
			}
			beforeTime.add(Calendar.SECOND, -step);
			Date beforeD = beforeTime.getTime();
			String time = sdf.format(beforeD);
			long ts;
			try {
				 ts =(sdf.parse(time)).getTime();
//				System.out.println(ts);
			} catch (ParseException e) {
				logger.error("获取当前时间戳失败，{}",e.getMessage());
				return "redirect:orderPayCus.html?status=8";
			}
			//时间戳超过3分钟后的提示该二维码无效
			if (ts>timeStamp) {
				logger.info("付款码已失效，请刷新二维码");
				return "redirect:orderPayCus.html?status=9";
			}
			
			/*
			 * 2018-12-20 修改
			 * 
			 * 需求：手刷聚合扫码可交易时间设置为 8:00（包含）至23:00，23:00（包含）至次日8:00不可进行交易
			 * 
			 * 暂定时间点 为8:00--23:00   时间存在redis内随时修改，最终时间点控制以redis内时间为准
			 * 配置文件内存储一对，redis连接异常时取 配置文件内设置值
			 * 
			 */ 
			try{
				startTime=Integer.valueOf(RedisUtil.getProperties("startTime"));
				endTime=Integer.valueOf(RedisUtil.getProperties("endTime"));
			}catch(Exception e){
				logger.error("redis连接异常{}可用时间取值取配置文件数据",e.getMessage()); 
			}
			if (startTime==endTime) {
				logger.info("[手刷--大额] 不设置交易时间限制");
			}else if (startTime<endTime) {
				Integer orderTime=Integer.valueOf(CTime.formatDate(new Date(), "HHMI"));  //请求下单时间
				if (orderTime<startTime||orderTime>endTime) {
					logger.info(mid+"现在非交易时段，扫码收款的交易时段"+startTime+"—"+endTime+"，请在交易时段进行交易，感谢您的理解和支持！");
					String start=DateUtil.formartDate(startTime.toString(), "HHmm", "HH:mm");
					String end=DateUtil.formartDate(endTime.toString(), "HHmm", "HH:mm");
					return "redirect:orderPayCus.html?status=10&startTime="+start+"&endTime="+end;
				}
			}
			String userAgent = request.getHeader("user-agent");
			if (userAgent.indexOf("MicroMessenger")>-1||userAgent.indexOf("micromessenger")>-1
					|| userAgent.indexOf("Alipay")>-1 || userAgent.indexOf("alipay")>-1
					|| userAgent.indexOf("QQ")>-1 || userAgent.indexOf("qq")>-1
					|| userAgent.toUpperCase().indexOf("JDJR")>-1 || userAgent.toUpperCase().indexOf("JDAPP")>-1|| userAgent.toUpperCase().indexOf("WALLETCLIENT")>-1
					|| userAgent.indexOf("BaiduWallet")>-1 || userAgent.indexOf("baiduwallet")>-1){
				return "redirect:hrtcmbcwxpay.html?mid="+mid;
			} else {
				logger.info(userAgent);
				return "/error";
			}
		} catch (Exception e) {
			logger.error("二维码支付下单入口操作异常{}，{}",mid,e.getMessage());
			return "/error";
		}
	}
	/**
	 * 和融通二维码支付下单
	 * 数据金额后,点击确定支付,获取支付链接或二维码
	 * @param bean
	 * @param request
	 * @return
	 */
	@RequestMapping("getqrurl")
	@ResponseBody
	public HrtCmbcBean getPayUrl(@RequestBody HrtCmbcBean bean, HttpServletRequest request) {
		bean.setStatus("1");
		if (bean.getAmount()==null) {
			bean.setMsg("金额不能为空");
			return bean;
		}
		if (bean.getAmount().scale()>2) {
			bean.setMsg("金额最多只能保留两位小数");
			return bean;
		}
		/*
		 * 收银台交易金额   首笔不小于300.00元
		 * 秒到版交易首笔   不少于5.00元
		 */
		if (bean.getMid().contains("HRTSYT")) {
			try {
				barLimit=RedisUtil.getProperties("qrpayLimit");
			} catch (Exception e) {
				 logger.error("[收银台主扫交易]redis 获取 交易限额异常{} ",e.getMessage());
			}
			if (bean.getAmount().compareTo(new BigDecimal(barLimit)) < 0){
				bean.setMsg("交易金额不可小于"+barLimit+"元");
				return bean;
			}
		}else{
			if (bean.getAmount().compareTo(MIN_AMOUNT) < 0){
				bean.setMsg("金额最小为5元");
				return bean;
			}
		}
		
		
		if (bean.getSubject()==null) {
			String merName = merService.queryMerNameForHrt(bean.getMid(),"ZS");
			bean.setSubject(merName);
		}
		/*
		 *2019-01-16  新增
		 *
		 *校验两笔交易间时间是否超过3分钟
		 *如果超过  1、交易间隔>3min可交易 
		 *        2、出提示信息不允许交易
		 * 
		 */
		try {
			Map<String,Object> midOrdInfo=RedisUtil.checkMid(bean.getMid(),"");
			boolean checkMidFlag=Boolean.valueOf(String.valueOf(midOrdInfo.get("rtnFlag")));
			if (!checkMidFlag) {
				bean.setMsg("交易间隔不足"+midOrdInfo.get("midLimitTime")+"分钟，请稍后再试。");
				return bean;
			}
		} catch (Exception e) {
			logger.error("[大额交易]校验时长出错，本条交易跳过时长校验，{}",e.getMessage());
		}
		if (bean.getPayway()==null) {
			String userAgent = request.getHeader("user-agent");
			if (userAgent.indexOf("MicroMessenger") > -1) {
//				bean.setPayway("WXZF");
				bean.setPayway("WXPAY");
			} else if (userAgent.indexOf("Alipay") > -1) {
				bean.setPayway("ZFBZF");
			} else if (userAgent.indexOf("QQ") > -1) {
				bean.setPayway("QQZF");
			} else if( userAgent.toUpperCase().indexOf("JDJR")>-1|| userAgent.toUpperCase().indexOf("JDAPP")>-1||userAgent.toUpperCase().indexOf("WALLETCLIENT")>-1) {
				bean.setPayway("JDZF");
			} else if (userAgent.indexOf("BaiduWallet") > -1) {
				bean.setPayway("BDQB");
			} else {
				return bean;
			}
		}
		
		try {
			/*
			 *2018-12-11 修改 
			 *
			 *手刷交易校验 金额判断 
			 *判断当前交易金额+redis内的值是否已经超过当日限额
			 */
			long dayMaxAmout=0;
			String key="";
			String checkPayKey=bean.getPayway();
			Double payAmt=Double.valueOf(String.valueOf(bean.getAmount()));
			if (checkPayKey.contains("WX")) {
				key="WX";
				dayMaxAmout=wxMaxAmt;
			}else if(checkPayKey.contains("ZFB")) {
				 key="ZFB";
				 dayMaxAmout=zfbMaxAmt;
			}
			if (dayMaxAmout!=0) {
				RedisUtil.checkAmtByGroupName(key, payAmt, dayMaxAmout);
			}
			/*
			 * 2018-12-06 修改
			 * 
			 * 手刷交易校验
			 * 订单交易成功后  redis内未支付笔数加一 
			 *
			 * 2018-12-14 修改
			 * 
			 * 商户做微信、支付宝交易时累增
			 * 
			 * 2019-01-16 修改
			 * 
			 * 商户做微信、支付宝交易时交易笔数分别累增
			 * 
			 */
			String payway="";
			try{
				payway=RedisUtil.getProperties("payway");
			}catch(Exception e){
				logger.error("redis连接异常payway取bean内值");
				payway=bean.getPayway();
			}
			/*
			 *  2019-01-24  修改
			 *  
			 *  商户限额判断
			 *  
			 */
			manageService.checkDayMerAmtForLMF(bean.getMid(), bean.getAmount().doubleValue(),"");
			if (payway.contains(bean.getPayway())) {
				try {
					count=Integer.valueOf(RedisUtil.getProperties(key+"maxCon"));
				} catch (Exception e) {
					logger.error("[大额手刷] redis 获取{}异常，原因：{}",key+"maxCon",e.getMessage());
				}
				boolean counts=RedisUtil.addCountPayByMid(bean.getMid()+key,count);
				if (!counts) {
					if ("WX".equals(key)) {
						bean.setMsg("今日微信未支付笔数已达"+count+" 笔，请更换支付宝或明日再试。");
					}else if("ZFB".equals(key)){
						bean.setMsg("今日支付宝未支付笔数已达"+count+" 笔，请完成支付或者更换微信。");
					}
					return bean;
				}
			}
			/*
			 *  2019-01-24  修改
			 *  
			 *  商户限额累增
			 *  
			 */
			try {
				manageService.addDayMerAmtForLMF(bean.getMid(),bean.getAmount().doubleValue());
			} catch (Exception e) {
				logger.error("[大额手刷] redis 累增交易金额异常{}：{}",bean.getMid(),e.getMessage());
			}
			
		} catch (BusinessException e) {
			bean.setMsg(e.getMessage());
			return bean;
		} 
		try {
			ch.checkChannelInfo2(bean);
			/*
			 * 2018-12-19 修改
			 * 
			 * 更新为新版入口 只手刷用
			 * 
			 */
			String QrCode = service.getPayUrl(bean);
			bean.setQrCodeUrl(QrCode);
			bean.setStatus("0");
			return bean;
		} catch (Exception e) {
			logger.info("{}扫码下单失败:{}",bean.getMid(),e.getMessage());
			bean.setMsg(e.getMessage());
			return bean;
		}
	}

	/**
	 * 
	 * 2019-01-07 新增
	 * 
	 * QR65 QR800 机具 被扫交易入口
	 * 
	 * @param bean
	 * @return
	 */
	@RequestMapping("qrbarcodepay")
	@ResponseBody
	public HrtPayXmlBean qrbarcodepay(@RequestBody HrtPayXmlBean bean) {
		
		//验证mid和tid的对应关系是否正常
		if("130000".equals(bean.getUnno())) {
			String status = merService.queryStatus(bean.getMid(), bean.getTid());
			if(!"1".equals(status)) {
				logger.info("[机具条码支付] 订单号:{} ，Mid:{}==SN:{},该设备已解绑", bean.getOrderid(),bean.getMid(),bean.getTid());
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("请绑定设备并更新商户信息");
				return bean;
			}
		}	
		
		// 验证是否是黑名单商户  黑名单商户禁止进行交易
		String mName=queryMerName(bean.getMid(),"BS");
		if ("".equals(mName)) {
			logger.info("[机具条码支付] 订单号:{} ，{}:{},无效商户号", bean.getOrderid(),bean.getMid(),mName);
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("无效商户号");
			return bean;
		}else if("blackList".equals(mName)){
			logger.info("[机具条码支付] 订单号:{} ，{}:{},黑名单商户", bean.getOrderid(),bean.getMid(),mName);
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc("暂不能使用该功能。");
			return bean;
		}
		// 验签 并获取机具秘钥 用于响应报文加密
		String key = null;
		
			/*
			 * 交易限制 同秒到版本（包括取号规则）
			 */
			logger.info("[机具条码支付] 订单号:{}", bean.getOrderid());
			String tidStatus="";
			try {
				if ("130000".equals(bean.getUnno())) {
					Map< String, Object> tidInfo = ch.checkChannelInfoForSyt(bean);
					key=String.valueOf(tidInfo.get("MACKEY"));
					tidStatus=String.valueOf(tidInfo.get("MINFO1"));
				}else if ("130001".equals(bean.getUnno())||"110001".equals(bean.getUnno())) {
					key=ch.checkChannelInfo(bean);
				}else{
					key=ch.checkChannelInfo(bean);
				}
			} catch (BusinessException e) {
				logger.info(e.getMessage());
				bean.setStatus("E");
				bean.setErrcode(e.getErrorCode());
				bean.setErrdesc(e.getMessage());
				return bean;
			}
			BigDecimal amount = null;
			try {
				amount = new BigDecimal(bean.getAmount());
				if (bean.getTid()!=null&&bean.getTid().length() > 10)
					throw new Exception("SN长度非法");
				if (amount.scale() > 2)
					throw new Exception("金额精度超过2");
				if (amount.compareTo(BigDecimal.ZERO) <= 0)
					throw new Exception("交易金额非法");
				/*
				 * 2019-01-09 修改 
				 * 
				 * 在redis内增加笔限额 和首笔交易上下限额
				 * 
				 * 1、终端首笔交易金额区间 暂定 300~5000后期可通过配置文件和redis修改
				 * 2、终端非首笔交易  暂定单笔金额不许低于 5.00 后期可通过配置文件和redis修改
				 * 
				 */
				try {
					barLimit=RedisUtil.getProperties("barLimit");
				} catch (Exception e) {
					 logger.error("[机具被扫交易]redis 获取 交易限额异常{} ",e.getMessage());
				}
				if ("130000".equals(bean.getUnno())) {	
					if ("5".equals(tidStatus)) {
						try {
							depositCaps=RedisUtil.getProperties("depositCaps");
						} catch (Exception e) {
							 logger.error("[机具被扫交易]redis 获取 首笔交易下限异常{},取配置文件默认值 ",e.getMessage());
						}
						try {
							depositCollars=RedisUtil.getProperties("depositCollars");
						} catch (Exception e) {
							 logger.error("[机具被扫交易]redis 获取 首笔交易上限异常{},取配置文件默认值 ",e.getMessage());
						}
						if (amount.compareTo(new BigDecimal(depositCaps))<0||amount.compareTo(new BigDecimal(depositCollars))>0) {
							throw new BusinessException(8000,"首笔交易请支付"+depositCaps+"~"+depositCollars+"元。");
						}
					}else{
						if (amount.compareTo(new BigDecimal(barLimit))<0) {
							throw new BusinessException(8000,"交易金额不可小于"+barLimit+"元。");
						}
					}
				}else if ("130001".equals(bean.getUnno())) {
					if (amount.compareTo(new BigDecimal(barLimit))<0) {
						throw new BusinessException(8000,"交易金额不可小于"+barLimit+"元。");
					}
				}else if ("110001".equals(bean.getUnno())) {
					if (new BigDecimal(bean.getAmount()).compareTo(MIN_AMOUNT) < 0){
						throw new BusinessException(8000,"交易金额不可小于5.00元。");
					}
				} 
			} catch (Exception e) {
				logger.info("[机具条码支付]金额错误:{},{}", bean.getAmount(),e.getMessage());
				bean.setStatus("E");
				bean.setErrcode("9001");
				bean.setErrdesc(e.getMessage());
				return bean;
			}
			if (null==bean.getOrderid()||"".equals(bean.getOrderid())||bean.getOrderid().length()>32) {
				logger.info("[机具条码支付]订单号:{},{}", bean.getOrderid(),"订单号长度不合法。");
				bean.setStatus("E");
				bean.setErrcode("9001");
				bean.setErrdesc("订单号长度不合法。"); 
				return bean;
			}
			try {
			/*
			 * 本处复用大额秒到扫码逻辑
			 * 
			 * 1、金额判断 判断当前交易金额+redis内的值是否已经超过当日限额
			 * 2、未支付笔数redis内加一，成功后减一
			 * 3、商户做微信、支付宝交易时累增
			 * 
			 */
			//金额判断 判断当前交易金额+redis内的值是否已经超过当日限额
			long dayMaxAmout = 0;
			getPayWayForAuthCode(bean);
			String checkPayKey = bean.getPayway();
			Double payAmt = Double.valueOf(String.valueOf(bean.getAmount()));
			String payKey="";
			if (checkPayKey.contains("WX")) {
				payKey = "WX";
				dayMaxAmout = wxMaxAmt;
			} else if (checkPayKey.contains("ZFB")) {
				payKey = "ZFB";
				dayMaxAmout = zfbMaxAmt;
			}
			if (dayMaxAmout != 0) {
				RedisUtil.checkAmtByGroupName(payKey, payAmt, dayMaxAmout);
			}
			/*
			 *  2019-01-24  修改
			 *  
			 *  商户限额判断
			 *  
			 */
			manageService.checkDayMerAmtForLMF(bean.getMid(), new Double(bean.getAmount()),bean.getUnno());
			//商户做微信、支付宝交易时累增
			String payway = "";
			try {
				payway = RedisUtil.getProperties("payway");
			} catch (Exception e) {
				logger.error("redis连接异常payway取bean内值");
				payway = bean.getPayway();
			}
			//未支付笔数redis内加一
			if (payway.contains(bean.getPayway())) {
				try {
					count=Integer.valueOf(RedisUtil.getProperties(payKey+"maxCon"));
				} catch (Exception e) {
					logger.error("[大额手刷] redis 获取{}异常，原因：{}",payKey+"maxCon",e.getMessage());
				}
				boolean counts = RedisUtil.addCountPayByMid(bean.getMid()+payKey, count);
				if (!counts) {
//					logger.info("当天未支付笔数超过" + count + "笔，请完成支付或隔天重试。");
					bean.setStatus("E");
					bean.setErrcode("9001");
					if ("WX".equals(payKey)) {
						logger.info("今日微信未支付笔数已达" + count + "笔，请完成支付或隔天重试。");
						bean.setErrdesc("今日微信未支付笔数已达" + count + "笔，请更换支付宝或明日再试。");
					}else if ("ZFB".equals(payKey)) {
						logger.info("今日支付宝未支付笔数已达" + count + "笔，请完成支付或隔天重试。");
						bean.setErrdesc("今日支付宝未支付笔数已达" + count + "笔，请完成支付或者更换微信。");
					}
					return bean;
				}
			}
//			manageService.addDayMerAmt(bean.getMid(), Double.valueOf(bean.getAmount()));
			/*
			 *  2019-01-24  修改
			 *  
			 *  商户限额累增
			 *  
			 */
			try {
				manageService.addDayMerAmtForLMF(bean.getMid(),new Double(bean.getAmount()));
			} catch (Exception e) {
				logger.error("[大额手刷] redis 累增交易金额异常{}：{}",bean.getMid(),e.getMessage());
			}
			payway=bean.getPayway();

			//机具被扫交易入口
			String status = service.barcodePay(bean);
			bean.setErrcode(bean.getRtnCode());
			bean.setErrdesc(bean.getRtnMsg());
			bean.setRtnCode("");
			bean.setRtnMsg("");
			
			/*
			 * 当同步响应结果为S时 操作redis数据 
			 * 1、订单交易成功后 redis内未支付笔数减一 
			 * 2、手刷 商户交易成功后做金额累增
			 * 按照payway进行累增
			 * 
			 */
			if ("S".equals(status)) {
				try {
					RedisUtil.cutCountPayByMid(bean.getMid()+payKey);
				} catch (Exception e) {
					logger.error("未支付笔数扣减失败");
				}
				Double amt = amount.doubleValue();
				try {
					if ("WXZF".equals(payway)) {
						// wxMaxAmt=
						// Long.parseLong(RedisUtil.getProperties("wxLimit"));
						RedisUtil.addAmtByGroupName("WX", amt, wxMaxAmt);
						logger.info("[机具条码支付] 微信交易 订单{}，交易金额{}", bean.getOrderid(), amt);
					} else if ("ZFBZF".equals(payway)) {
						// zfbMaxAmt=
						// Long.parseLong(RedisUtil.getProperties("zfbLimit"));
						RedisUtil.addAmtByGroupName("ZFB", amt, zfbMaxAmt);
						logger.info("[机具条码支付] 支付宝交易订单{}，交易金额{}", bean.getOrderid(), amt);
					}
				} catch (Exception e) {
					logger.error("[机具条码支付] 成功交易累增金额失败，{}", e.getMessage());
				}

			}
			bean.setStatus(status);
		} catch (Exception e) {
			logger.error("[机具条码支付] 交易失败，原因{}:{}  {}",bean.getMid(),amount, e.getMessage());
			bean.setStatus("E");
			bean.setErrcode("8000");
			bean.setErrdesc(e.getMessage());
			return bean;
		}
		if (key != null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (Exception e) {
				logger.error("响应签名异常", e);
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("响应签名异常");
			}
		}
		return bean;
	}
    
	/**
	 * 根据上送的SN进行获取机具秘钥
	 * @param bean
	 * @return
	 */
	@RequestMapping("getMacKey")
	@ResponseBody
	public HrtPayXmlBean getMacKey(@RequestBody HrtPayXmlBean bean) {
		logger.info("[获取机具秘钥]机具SN编号：{}", bean.getTid());
		String key = null;
		try {
			/*
			 * 复用智能pos获取机构秘钥的方法 进行验签、获取机构秘钥
			 */
			key = ch.checkChannelInfoForPos(ch.beanToMap(bean));
			/*
			 * 根据tid（SN） 获取机具秘钥 并返回 mid、商户名称
			 */
			merService.queryMerInfoByTid(bean);
			bean.setStatus("S");
			bean.setErrdesc("获取成功");
		} catch (Exception e ) {
			logger.info(e.getMessage());
			bean.setStatus("E");
			bean.setErrdesc(e.getMessage());
			return bean;
		}
		if (key != null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (Exception e) {
				logger.error("响应签名异常", e);
				bean.setStatus("E");
				bean.setErrcode("8000");
				bean.setErrdesc("响应签名异常");
			}
		}
		logger.info("[获取机具秘钥]机具tid编号：{},返回：{}", bean.getTid(), bean.getStatus());
		return bean;
	}
	
	
	/**
	 * 
	 * 2019-01-09 新增
	 * 
	 * 设备获取最新版本信息
	 * 
	 * @param bean
	 * @return
	 */
	@RequestMapping("getDeviceTypeInfo")
	@ResponseBody
	public HrtDeviceTypeInfoBean  getDeviceInfo(@RequestBody HrtDeviceTypeInfoBean  bean){
		logger.info("[获取机具秘钥]设备版本查询：{}", bean.getDeviceType());
		String key = null;
		try {
			/*
			 * 复用智能pos获取机构秘钥的方法 进行验签、获取机构秘钥
			 */
			key = ch.checkChannelInfoForPos(ch.beanToMap(bean));
			/*
			 * 根据tid（SN） 获取机具秘钥 并返回 mid、商户名称
			 */
			merService.queryDeviceTypeInfo(bean);			
			bean.setStatus("S");
		} catch (Exception e) {
			logger.info(e.getMessage());
			bean.setStatus("E");
		}
		if (key != null) {
			try {
				bean.setSign(SimpleXmlUtil.getMd5Sign(ch.beanToMap(bean), key));
			} catch (Exception e) {
				logger.error("响应签名异常", e);
				bean.setStatus("E");
			}
		}
		logger.info("[获取机具秘钥]机设备版本查询：{}:{},返回：{}", bean.getDeviceType(),bean.getDeviceVersion(), bean.getStatus());
		return bean;
	}
	
	
	
	public  void getPayWayForAuthCode(HrtPayXmlBean bean) throws BusinessException{
		String authCode=bean.getAuthcode();
		Pattern p = Pattern.compile("[0-9]*");
		if(!p.matcher(authCode).matches()){
			throw new BusinessException(8000, "请扫描正确的付款码！");
		}
		String twoIndex=authCode.substring(0, 2);
		// 支付宝 25~30 
		// 微信10~15
		try {
			wxCode=RedisUtil.getProperties("wx_code");
			zfbCode=RedisUtil.getProperties("zfb_code");
			if(wxCode==null ||"".equals(wxCode)||zfbCode==null ||"".equals(zfbCode)){
				throw new BusinessException(8000, "redis获取付款码异常！");
			}
		}catch (Exception e) {
			logger.error("redis连接异常  wxCode, zfbCode 取配置文件内值{}，{}",wxCode,zfbCode); 
		}
		String wx_start=wxCode.split("-")[0];
		String wx_end=wxCode.split("-")[1];
		String zfb_start=zfbCode.split("-")[0];
		String zfb_end=zfbCode.split("-")[1];
		if(Integer.parseInt(twoIndex)>=Integer.parseInt(wx_start)&&
				Integer.parseInt(twoIndex)<=Integer.parseInt(wx_end)){
			bean.setPayway("WXZF");
		}else if(Integer.parseInt(twoIndex)>=Integer.parseInt(zfb_start)&&
				Integer.parseInt(twoIndex)<=Integer.parseInt(zfb_end)){
			bean.setPayway("ZFBZF");
		}else{
			throw new BusinessException(8000, "请扫描正确的付款码！");
		}
	}
	
}
