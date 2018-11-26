package com.hrtpayment.xpay.pos.server.codec;

import java.nio.ByteOrder;

import com.hrtpayment.xpay.utils.Hex;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MyLengthFieldBasedFrameDecoder extends LengthFieldBasedFrameDecoder{

  public MyLengthFieldBasedFrameDecoder(int maxFrameLength,
      int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
      int initialBytesToStrip) {
    super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment,
        initialBytesToStrip);
  }
  
  @Override
  protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length,
      ByteOrder order) {
    buf = buf.order(order);
    byte[] bytes = new byte[length];
    buf.getBytes(offset, bytes);
    String lenStr = new String(Hex.encode(bytes));
    try {
      return Long.valueOf(lenStr,16);
    } catch (Exception e) {
      throw new DecoderException(
                "unsupported lengthFieldLength: " + length + " (expected: 1, 2, 3, 4, or 8)");
    }
  }
}
