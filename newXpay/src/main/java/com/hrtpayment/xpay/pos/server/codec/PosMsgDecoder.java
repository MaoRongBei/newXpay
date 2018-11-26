package com.hrtpayment.xpay.pos.server.codec;

import java.util.List;

import com.hrtpayment.xpay.pos.server.PosMsg;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

@Sharable
public class PosMsgDecoder extends MessageToMessageDecoder<byte[]>{

	@Override
	protected void decode(ChannelHandlerContext ctx, byte[] msg, List<Object> out) throws Exception {
		PosMsg pmsg = new PosMsg();
		pmsg.parse(msg);
		out.add(pmsg);
	}

}
