package com.hrtpayment.xpay.common.service.impl;

import static io.netty.buffer.Unpooled.copiedBuffer;

import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hrtpayment.xpay.common.service.Callback;
import com.hrtpayment.xpay.redis.RedisUtil;
import com.hrtpayment.xpay.utils.exception.ClientNetworkException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

@Service
public class NettyClientService {
	private NioEventLoopGroup clientGroup = new NioEventLoopGroup();
	private final Logger logger = LogManager.getLogger();
	
	public String sendFormData(String url, String formData) throws InterruptedException, URISyntaxException, SSLException {
        return post(url,formData,HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
	}
	public String post(String url, String formData,Callback<String> callback) throws InterruptedException, URISyntaxException, SSLException {
        return post(url,formData,HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED,callback);
	}
	public String sendJson(String url, String json) throws SSLException, InterruptedException, URISyntaxException{
		return post(url,json,HttpHeaderValues.APPLICATION_JSON);
	}
	public void sendAsyncXml(String url, String xml,Callback<String> callback) throws SSLException, InterruptedException, URISyntaxException{
        ByteBuf buf = copiedBuffer(xml.getBytes(CharsetUtil.UTF_8));
        HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST,url, buf);
    	request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/xml");
    	request.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
    	request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		sendAsyncRequest(url, request,callback);
	}
	public String sendXml(String url, String xml) {
        ByteBuf buf = copiedBuffer(xml.getBytes(CharsetUtil.UTF_8));
        HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST,url, buf);
    	request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/xml");
    	request.headers().set(HttpHeaderNames.ACCEPT, "text/xml");
    	request.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
    	request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    	return sendRequest(url, request);
	}
	public String sendGetRequest(String url) throws InterruptedException, URISyntaxException, SSLException {
		HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET,url);
    	request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
    	request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		return sendRequest(url, request);
	}
	private String post(String url,String content, AsciiString contentType) throws InterruptedException, URISyntaxException, SSLException {
        ByteBuf buf = copiedBuffer(content.getBytes(CharsetUtil.UTF_8));
        HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST,url, buf);
    	request.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    	request.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
    	request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		return sendRequest(url, request);
	}
	private String post(String url,String content, AsciiString contentType,Callback<String> callback) throws InterruptedException, URISyntaxException, SSLException {
        ByteBuf buf = copiedBuffer(content.getBytes(CharsetUtil.UTF_8));
        HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST,url, buf);
    	request.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    	request.headers().set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
    	request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
		return sendRequest(url, request);
	}
	public String sendRequest(String url, HttpRequest request) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			logger.error("url错误",e);
			throw new ClientNetworkException(String.format("错误的url:%s", url));
		}
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
		int port = uri.getPort();
		if (port == -1) {
			if ("http".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(scheme)) {
				port = 443;
			}
		}
		// Configure SSL context if necessary.
		final boolean ssl = "https".equalsIgnoreCase(scheme);
		final SslContext sslCtx;
		if (ssl) {
			try {
				sslCtx = SslContextBuilder.forClient()
						.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
			} catch (SSLException e) {
				logger.error("SSL错误",e);
				throw new ClientNetworkException(String.format("SSL错误:%s", url));
			}
		} else {
			sslCtx = null;
		}
		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			try {
				throw new URISyntaxException(url,"Only HTTP(S) is supported.");
			} catch (URISyntaxException e) {
				logger.error("URI语法错误",e);
				throw new ClientNetworkException(String.format("URI语法错误:%s", url));
			}
		}
		final ResponseHolder responseHolder = new ResponseHolder();
//		DefaultPromise<String> dp = new DefaultPromise<>(clientGroup.next());
		Bootstrap bootstrap = new Bootstrap().group(clientGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.TCP_NODELAY,true)
				.handler(new ChannelInitializer<Channel>() {

					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline p = ch.pipeline();
						p.addLast(new IdleStateHandler(30, 0, 0));
						// Enable HTTPS if necessary.
						if (sslCtx != null) {
							p.addLast(sslCtx.newHandler(ch.alloc()));
						}

						p.addLast(new HttpClientCodec());
						p.addLast(new HttpContentDecompressor());
						p.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
						p.addLast(
								new SimpleChannelInboundHandler<FullHttpResponse>() {
							@Override
							protected void channelRead0(
									ChannelHandlerContext ctx,
									FullHttpResponse response)
											throws Exception {
								String content = response.content().toString(CharsetUtil.UTF_8);
								responseHolder.setResponse(content);
								ctx.close();
							}
							
							@Override
							public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
									throws Exception {
								logger.error("网络访问出错",cause);
								RedisUtil.addFailCountByRedis(1);
								responseHolder.setCause(cause);
							}
							
							@Override
							public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
									throws Exception {
								super.userEventTriggered(ctx, evt);
								if(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT.equals(evt) || IdleStateEvent.READER_IDLE_STATE_EVENT.equals(evt)){
									responseHolder.setCause( new Throwable("读取超时！"));
									RedisUtil.addFailCountByRedis(1);
									ctx.close();
								}
								
							}
						});
					}
				});
		Channel ch = null;
		try {
			ch = bootstrap.connect(host, port).sync().channel();
	        request.headers().set(HttpHeaderNames.HOST, host);
			ch.writeAndFlush(request);
			ch.closeFuture().sync();
//			dp.await();
//			if(dp.isSuccess()){
//				return dp.get();
//			}else{
//				dp.cause();
//			}
		} catch (Exception e) {
			logger.error("通讯异常:",e);
			RedisUtil.addFailCountByRedis(1);
			throw new ClientNetworkException(String.format("通讯中断,访问地址:%s", url));
		}
		if (null!=responseHolder.getCause()) {
			RedisUtil.addFailCountByRedis(1);
			throw new ClientNetworkException(String.format("访问%s出错", url));
		}
		return responseHolder.getResponse();
	}
	public void sendAsyncRequest(String url, HttpRequest request,final Callback<String> callback) throws InterruptedException, URISyntaxException, SSLException {
		URI uri = new URI(url);
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
		int port = uri.getPort();
		if (port == -1) {
			if ("http".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(scheme)) {
				port = 443;
			}
		}
		// Configure SSL context if necessary.
		final boolean ssl = "https".equalsIgnoreCase(scheme);
		final SslContext sslCtx;
		if (ssl) {
			sslCtx = SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} else {
			sslCtx = null;
		}
		if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
			throw new URISyntaxException(url,"Only HTTP(S) is supported.");
		}
		Bootstrap bootstrap = new Bootstrap().group(clientGroup)
				.channel(NioSocketChannel.class)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.TCP_NODELAY,true)
				.handler(new ChannelInitializer<Channel>() {

					@Override
					protected void initChannel(Channel ch) throws Exception {
						ChannelPipeline p = ch.pipeline();
						p.addLast(new IdleStateHandler(30, 0, 0));
						// Enable HTTPS if necessary.
						if (sslCtx != null) {
							p.addLast(sslCtx.newHandler(ch.alloc()));
						}

						p.addLast(new HttpClientCodec());
						p.addLast(new HttpContentDecompressor());
						p.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
						p.addLast(
								new SimpleChannelInboundHandler<FullHttpResponse>() {
							@Override
							protected void channelRead0(
									ChannelHandlerContext ctx,
									FullHttpResponse response)
											throws Exception {
								String content = response.content().toString(CharsetUtil.UTF_8);
								callback.resp(content);
								ctx.close();
							}
							
							@Override
									public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
											throws Exception {
										// TODO Auto-generated method stub
										super.userEventTriggered(ctx, evt);
										if(IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT.equals(evt) || IdleStateEvent.READER_IDLE_STATE_EVENT.equals(evt)){
											callback.catchTimeOut("读取超时！");
											ctx.close();
										}
									}
						});
					}
				});
		Channel ch = bootstrap.connect(host, port).sync().channel();
        request.headers().set(HttpHeaderNames.HOST, host);
		ch.writeAndFlush(request);
	}
}
class ResponseHolder {
	private String response;
	private Throwable cause;

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public Throwable getCause() {
		return cause;
	}

	public void setCause(Throwable cause) {
		this.cause = cause;
	}
}
