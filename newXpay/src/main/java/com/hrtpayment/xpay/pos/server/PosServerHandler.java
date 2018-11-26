package com.hrtpayment.xpay.pos.server;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hrtpayment.xpay.channel.bean.HrtPayXmlBean;
import com.hrtpayment.xpay.pos.service.PosService;
import com.hrtpayment.xpay.utils.CommonUtils;
import com.hrtpayment.xpay.utils.Hex;
import com.hrtpayment.xpay.utils.exception.BusinessException;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class PosServerHandler extends ChannelInboundHandlerAdapter{
	protected static final Logger logger = LogManager.getLogger();
	private final PosService service;

	public PosServerHandler(PosService service) {
		this.service = service;
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
		final byte[] bmsg = (byte[]) msg;
		service.execute(new Runnable() {
			
			@Override
			public void run() {
				handleMsg(ctx,bmsg);
			}
		});
	}
	
	private void handleMsg(ChannelHandlerContext ctx, byte[] msg){
		logger.info("POSserver 接收:"+ByteBufUtil.hexDump(msg));
		PosMsg pmsg = new PosMsg();
		pmsg.parse(msg);
		pmsg.setMsgType("0210");
		String tpdu = pmsg.getTpdu();
		tpdu = tpdu.substring(0, 2) + tpdu.substring(6,10) + tpdu.substring(2,6);
		pmsg.setTpdu(tpdu);
		String mid = pmsg.getBit42();
		String tid = pmsg.getBit41();
		if (pmsg.getBit4()==null || pmsg.getBit4().length()<12 || mid==null || mid.length()<15 || tid==null || tid.length()<8){
			pmsg.setBit39("30");
			writeAndFlushWithLenPrefix(ctx, pmsg);
			return;
		}
		if (!pmsg.checkMac()) {
			pmsg.setBit39("80");
			writeAndFlushWithLenPrefix(ctx, pmsg);
			return;
		}

		String transType = pmsg.getBit3();
		if ("000000".equals(transType)){
			String merOrderId = CommonUtils.getWxTransId();
			BigDecimal amt = new BigDecimal(pmsg.getBit4()).divide(BigDecimal.TEN).divide(BigDecimal.TEN);
			String payway = pmsg.getDf26();
			/**
			 * 扫码 单笔限额 日限额
			 */
			try {
				if("LMF".equals(payway)){
					if(amt.doubleValue()>1000){
						throw new BusinessException(9001, "单笔超限额，不允许交易！");
					}
				}else{
					service.addPosDaySumAmt(mid, amt.doubleValue());
				}
			} catch (BusinessException e) {
				logger.info(mid+"单日/笔限额.......");
				pmsg.setBit39("61");
				writeAndFlushWithLenPrefix(ctx, pmsg);
				return ;
			}
			String merchantName;
			try {
				merchantName = service.queryMerchantName(mid);
			} catch (BusinessException e) {
				logger.info("未查询到"+mid+"对应的商户");
				merchantName = null;
			}
			pmsg.setBit39("25");
			if (merchantName != null) {
				 if(null !=pmsg.getDf35() && !"".equals(pmsg.getDf35())){
					try {
						HrtPayXmlBean bean = new HrtPayXmlBean();
						bean.setAmount(String.valueOf(amt));
						bean.setOrderid(merOrderId);
						bean.setAuthcode(pmsg.getDf35());
						if("WX".equals(payway)){
							payway="WXZF";
						}
						if("ZFB".equals(payway)){
							payway="ZFBZF";
						}
						bean.setPayway(payway);
						bean.setMid(mid);
						bean.setTid(tid);
						bean.setPayOrderTime(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
						service.posBarcodePay(bean,pmsg);
					} catch (RuntimeException e) {
						logger.info(e.getMessage());
						pmsg.setBit39("01");
						return;
					} catch (Exception e) {
						logger.error(e);
						pmsg.setBit39("01");
						return;
					}
					pmsg.setDf27(merOrderId);
				 }else{
						String authUrl = null;
						try {
							/**
							 * POS
							 */
							if("LMF".equals(payway)){
								logger.info("传统Pos******************************************"+payway);
								authUrl = service.getCupsPayUrl(mid,tid,merOrderId,  merchantName, amt);
							}else{
//								authUrl = service.getPayUrl(mid,tid,merOrderId,  merchantName, amt, payway);
								authUrl = service.getPayUrl2(mid,tid,merOrderId,  merchantName, amt);
							}

							
						} catch (RuntimeException e) {
							logger.info(e.getMessage());
							pmsg.setBit39("25");
							return;
						} catch (Exception e) {
							logger.error(e);
							pmsg.setBit39("25");
							return;
						}
						logger.info(authUrl);
						pmsg.setDf27(merOrderId);
						pmsg.setDf28(authUrl);
						pmsg.setBit39("00"); 
				 }
				writeAndFlushWithLenPrefix(ctx, pmsg);
				return;
			}
		} else if("310000".equals(transType)) {
			String orderId = pmsg.getDf27();
			String payway = pmsg.getDf26();
			logger.info(orderId);
			if (orderId == null || orderId.length()==0) {
				pmsg.setBit39("25");
			} else {
				Map<String, Object> map;
				try {
					map = service.queryOrder(orderId,payway);
					if (map == null || "0".equals((String) map.get("STATUS"))) {
						pmsg.setBit39("01");
					} else if("1".equals((String) map.get("STATUS"))) {
						pmsg.setBit39("00");
						// 立码付小票打印数据
						if(payway.equals("LMF")){
							if(map.get("ACCNO")!=null){
								pmsg.setDf31(map.get("ACCNO")==null?"":map.get("ACCNO")+"");
							}
							if(map.get("ISSCODE")!=null){
								pmsg.setDf32(map.get("ISSCODE")==null?"":map.get("ISSCODE")+"");
							}
							pmsg.setDf33("48641000");
							pmsg.setDf34(map.get("BK_ORDERID")==null?"":map.get("BK_ORDERID")+"");
						}
					} else {
						pmsg.setBit39("25");
					}
				} catch (BusinessException e) {
					logger.info("查询不到订单号对应订单");
					pmsg.setBit39("25");
				}
			}
		}/*else if("200000".equals(transType)){
			String orderId = pmsg.getDf27();
			BigDecimal amt = new BigDecimal(pmsg.getBit4()).divide(BigDecimal.TEN).divide(BigDecimal.TEN);
			String payway = pmsg.getDf26();
			logger.info("撤销交易原订单号："+orderId+"原交易方式："+payway);
			try {
				boolean flag =service.undoTxn(orderId,mid,tid,amt,payway);
				if(flag){
					pmsg.setBit39("00");
				}else{
					pmsg.setBit39("25");
				}
			} catch (Exception e) {
				logger.error("撤销交易失败",e.getMessage());
				pmsg.setBit39("25");
			}
		} */else {
			pmsg.setBit39("30");
			writeAndFlushWithLenPrefix(ctx, pmsg);
			return;
		}
		writeAndFlushWithLenPrefix(ctx, pmsg);
	}
	
	private void writeAndFlushWithLenPrefix(ChannelHandlerContext ctx,PosMsg posMsg){
		byte[] resp = Hex.decode(posMsg.assemble());
		logger.info("返回:"+ByteBufUtil.hexDump(resp));
		int len = resp.length;
		byte[] lenField = new byte[2];
		lenField[0] = (byte) ((len>>>8)&0B11111111);
		lenField[1] = (byte) (len&0B11111111);
		ctx.write(lenField);
		ctx.writeAndFlush(resp);
	}
}
