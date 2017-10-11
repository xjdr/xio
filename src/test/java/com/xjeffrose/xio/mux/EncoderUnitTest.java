package com.xjeffrose.xio.mux;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.EncoderException;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class EncoderUnitTest extends Assert {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  EmbeddedChannel channel;

  @Before
  public void setUp() throws Exception {
    channel = new EmbeddedChannel();

    channel.pipeline()
      .addLast(new Encoder())
    ;
  }

  @Test
  public void testWriteSucceeds() throws Exception {
    Integer payload = new Integer(1);
    Request request = new Request(UUID.fromString("f6fb3dbf-43fa-4ebf-8f45-b38e7444beae"), SettableFuture.create());
    Message message = new Message(request, payload);
    ByteBuf buf = Unpooled.buffer();
    buf.writeBytes(Ints.toByteArray(payload));

    channel.writeOutbound(buf);
    channel.writeOutbound(message);
    channel.runPendingTasks();

    ByteBuf encoded = (ByteBuf)channel.outboundMessages().poll();
    String expectedEncoded = new StringBuilder()
      .append("         +-------------------------------------------------+\n")
      .append("         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |\n")
      .append("+--------+-------------------------------------------------+----------------+\n")
      .append("|00000000| 66 36 66 62 33 64 62 66 2d 34 33 66 61 2d 34 65 |f6fb3dbf-43fa-4e|\n")
      .append("|00000010| 62 66 2d 38 66 34 35 2d 62 33 38 65 37 34 34 34 |bf-8f45-b38e7444|\n")
      .append("|00000020| 62 65 61 65 00 00 00 00 00 00 00 04 00 00 00 01 |beae............|\n")
      .append("+--------+-------------------------------------------------+----------------+")
      .toString();

    assertEquals("Expected:\n" + expectedEncoded, expectedEncoded, ByteBufUtil.prettyHexDump(encoded));
  }

  @Test
  public void testWriteMultipleByteBufs() {
    Integer payload = new Integer(1);
    ByteBuf buf1 = Unpooled.buffer();
    buf1.writeBytes(Ints.toByteArray(payload));

    ByteBuf buf2 = Unpooled.buffer();
    buf2.writeBytes(Ints.toByteArray(payload));

    Request request = new Request(UUID.fromString("6566a1c5-b0ec-47a8-8e77-b5173b78873a"), SettableFuture.create());
    Message message = new Message(request, payload);

    channel.writeOutbound(buf1);
    channel.writeOutbound(buf2);
    channel.writeOutbound(message);
    channel.runPendingTasks();

    ByteBuf encoded = (ByteBuf)channel.outboundMessages().poll();
    String expectedEncoded = new StringBuilder()
      .append("         +-------------------------------------------------+\n")
      .append("         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |\n")
      .append("+--------+-------------------------------------------------+----------------+\n")
      .append("|00000000| 36 35 36 36 61 31 63 35 2d 62 30 65 63 2d 34 37 |6566a1c5-b0ec-47|\n")
      .append("|00000010| 61 38 2d 38 65 37 37 2d 62 35 31 37 33 62 37 38 |a8-8e77-b5173b78|\n")
      .append("|00000020| 38 37 33 61 00 00 00 00 00 00 00 08 00 00 00 01 |873a............|\n")
      .append("|00000030| 00 00 00 01                                     |....            |\n")
      .append("+--------+-------------------------------------------------+----------------+")
      .toString();

    assertEquals("Expected:\n" + expectedEncoded, expectedEncoded, ByteBufUtil.prettyHexDump(encoded));
  }

  @Test
  public void testErrorInvalidPayload() {
    thrown.expect(EncoderException.class);
    thrown.expectMessage("Can only encode Message or ByteBuf");

    Integer payload = new Integer(1);
    Request request = new Request(UUID.randomUUID(), SettableFuture.create());
    Message message = new Message(request, payload);
    channel.writeOutbound(payload);
    channel.writeOutbound(message);
    channel.runPendingTasks();
  }

  @Test
  public void testErrorNoPayload() {
    thrown.expect(EncoderException.class);
    thrown.expectMessage("Encoder received Message without anything to encode");

    Integer payload = new Integer(1);
    Request request = new Request(UUID.randomUUID(), SettableFuture.create());
    Message message = new Message(request, payload);
    channel.writeOutbound(message);
    channel.runPendingTasks();
  }

}
