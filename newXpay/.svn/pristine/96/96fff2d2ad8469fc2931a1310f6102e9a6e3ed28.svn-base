package com.hrtpayment.xpay.pos.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hrtpayment.xpay.pos.service.PosService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

@Component
public class PosServer  implements InitializingBean{
	protected static final Logger logger = LogManager.getLogger();
	@Value("${posserver.port}")
	private int port;
	@Value("${wx.pay_host}/WeChatServlet/hrtorderauth?mid=%s")
	private String payurl;
	@Autowired
	private PosService service;

	@Override
	public void afterPropertiesSet() throws Exception {
		new Thread(){
			public void run() {
				EventLoopGroup bossGroup = new NioEventLoopGroup(1);
				EventLoopGroup workerGroup = new NioEventLoopGroup();
				try {
					ServerBootstrap bootstrap = new ServerBootstrap();
					bootstrap.group(bossGroup, workerGroup)
							.channel(NioServerSocketChannel.class)
							.option(ChannelOption.TCP_NODELAY,true)
							.option(ChannelOption.SO_KEEPALIVE,true)
							.handler(new LoggingHandler(LogLevel.INFO))
							.childHandler(new PosServerInitializer(service));

					Channel ch = bootstrap.bind(port).sync().channel();

					ch.closeFuture().sync();
				} catch (Exception e) {
					 logger.error("启动服务器出错", e);
				} finally {
					bossGroup.shutdownGracefully();
					workerGroup.shutdownGracefully();
				}
			}
		}.start();
	}
}
