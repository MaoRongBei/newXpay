package com.hrtpayment.xpay.pos.server.codec;

import java.util.List;

import com.hrtpayment.xpay.pos.server.PosMsg;
import com.hrtpayment.xpay.utils.Hex;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

@Sharable
public class PosMsgEncoder extends MessageToMessageEncoder<PosMsg> {

	@Override
	protected void encode(ChannelHandlerContext ctx, PosMsg msg, List<Object> out) throws Exception {
		if (msg!=null) {
			out.add(Hex.decode(msg.assemble()));
		}
	}

}
