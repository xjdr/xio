package com.xjeffrose.xio.mux;

import com.google.common.util.concurrent.SettableFuture;
import com.xjeffrose.xio.core.FrameLengthCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpVersion;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClientCodecFunctionalTest extends Assert {

  ClientCodec encoder;

  EmbeddedChannel channel;

  @Before
  public void setUp() {
    encoder = new ClientCodec();

    channel = new EmbeddedChannel();

    channel.pipeline()
      .addLast(new FrameLengthCodec())
      .addLast(new Encoder())
      // http encoder
      .addLast(new HttpRequestEncoder())

      .addLast(encoder)
    ;
  }

  byte[] hexToBytes(String hex) {
    byte[] result = new byte[hex.length()/2];
    for(int i = 0; i < hex.length(); ) {
      int idx = i/2;
      result[idx] = Byte.parseByte(hex.substring(i, i+2), 16);
      i += 2;
    }

    return result;
  }

  @Test
  public void testEncodeGet() {
    Request request = new Request(UUID.fromString("934bf16b-7d6f-4f8a-92ce-6d46affb933f"), SettableFuture.create(), SettableFuture.create());
    HttpRequest payload = new DefaultHttpRequest(
      HttpVersion.HTTP_1_1,
      HttpMethod.GET,
      "/path"
    );

    Message message = new Message(request, payload);
    channel.writeOutbound(message);
    channel.runPendingTasks();

    ByteBuf expectedLength = Unpooled.copiedBuffer(hexToBytes("0042"));
    ByteBuf expectedEncoded = Unpooled.copiedBuffer(hexToBytes("39333462663136622d376436662d346638612d393263652d3664343661666662393333660000000100000016474554202f7061746820485454502f312e310d0a0d0a"));

    ByteBuf length = (ByteBuf)channel.outboundMessages().poll();
    ByteBuf encoded = (ByteBuf)channel.outboundMessages().poll();

    assertTrue("Expected: " + ByteBufUtil.hexDump(expectedLength), ByteBufUtil.equals(expectedLength, length));
    assertTrue("Expected: " + ByteBufUtil.hexDump(expectedEncoded), ByteBufUtil.equals(expectedEncoded, encoded));
  }

  @Test
  public void testEncodePost() {
    ByteBuf content = Unpooled.wrappedBuffer("this is the content".getBytes());
    HttpHeaders headers = new DefaultHttpHeaders();
    headers.add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);

    Request request = new Request(UUID.fromString("70b3e594-2387-412c-9050-f6f7f46b64d5"), SettableFuture.create(), SettableFuture.create());
    HttpRequest payload = new DefaultFullHttpRequest(
      HttpVersion.HTTP_1_1,
      HttpMethod.POST,
      "/path",
      content,
      headers,
      new DefaultHttpHeaders()
    );

    Message message = new Message(request, payload);
    channel.writeOutbound(message);
    channel.runPendingTasks();

    ByteBuf expectedLength = Unpooled.copiedBuffer(hexToBytes("007d"));
    ByteBuf expectedEncoded = Unpooled.copiedBuffer(hexToBytes("37306233653539342d323338372d343132632d393035302d6636663766343662363464350000000100000051504f5354202f7061746820485454502f312e310d0a7472616e736665722d656e636f64696e673a206368756e6b65640d0a0d0a31330d0a746869732069732074686520636f6e74656e740d0a300d0a0d0a"));

    ByteBuf length = (ByteBuf)channel.outboundMessages().poll();
    ByteBuf encoded = (ByteBuf)channel.outboundMessages().poll();

    assertTrue("Expected: " + ByteBufUtil.hexDump(expectedLength), ByteBufUtil.equals(expectedLength, length));
    assertTrue("Expected: " + ByteBufUtil.hexDump(expectedEncoded), ByteBufUtil.equals(expectedEncoded, encoded));
  }
}
