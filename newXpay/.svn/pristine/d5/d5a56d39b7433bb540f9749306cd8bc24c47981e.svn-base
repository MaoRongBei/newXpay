package com.hrtpayment.xpay.pos.server;

import com.hrtpayment.xpay.pos.server.codec.MyLengthFieldBasedFrameDecoder;
import com.hrtpayment.xpay.pos.service.PosService;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;

public class PosServerInitializer extends ChannelInitializer<SocketChannel> {
	private final PosService service;

	public PosServerInitializer(PosService service) {
		this.service = service;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new MyLengthFieldBasedFrameDecoder(9999, 0, 2, 0, 2));
        pipeline.addLast(new ByteArrayEncoder());
        pipeline.addLast(new ByteArrayDecoder());
        pipeline.addLast(new PosServerHandler(service));
	}

}
