package com.xjeffrose.xio.mux;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DecoderUnitTest extends Assert {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  EmbeddedChannel channel;

  @Before
  public void setUp() throws Exception {
    channel = new EmbeddedChannel();

    channel.pipeline()
      .addLast(new Decoder())
    ;
  }

  @Test
  public void testReadSucceeds() throws Exception {
    UUID id = UUID.fromString("3f127172-0245-4018-b52d-a8967bd94e7d");
    Message.Op op = Message.Op.Response;
    int payloadSize = 4;
    Integer payload = new Integer(1);
    ByteBuf buf = Unpooled.buffer();
    buf.writeBytes(id.toString().getBytes());
    buf.writeBytes(Ints.toByteArray(op.ordinal()));
    buf.writeBytes(Ints.toByteArray(payloadSize));
    buf.writeBytes(Ints.toByteArray(payload));

    channel.writeInbound(buf);
    channel.runPendingTasks();

    ByteBuf decoded = (ByteBuf)channel.inboundMessages().poll();
    Message message = (Message)channel.inboundMessages().poll();


    String expectedDecoded = new StringBuilder()
      .append("         +-------------------------------------------------+\n")
      .append("         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |\n")
      .append("+--------+-------------------------------------------------+----------------+\n")
      .append("|00000000| 00 00 00 01                                     |....            |\n")
      .append("+--------+-------------------------------------------------+----------------+")
      .toString();

    assertEquals("Expected:\n" + expectedDecoded, expectedDecoded, ByteBufUtil.prettyHexDump(decoded));
    assertEquals(id, message.id);
    assertEquals(op, message.op);
  }

  @Test
  public void testErrorInvalidPayload() {
    thrown.expect(DecoderException.class);
    thrown.expectMessage("Not enough bytes available to decode payload");

    UUID id = UUID.fromString("3f127172-0245-4018-b52d-a8967bd94e7d");
    Message.Op op = Message.Op.Response;
    int payloadSize = 4;
    Integer payload = new Integer(1);
    ByteBuf buf = Unpooled.buffer();
    buf.writeBytes(id.toString().getBytes());
    buf.writeBytes(Ints.toByteArray(op.ordinal()));
    buf.writeBytes(Ints.toByteArray(payloadSize));

    channel.writeInbound(buf);
  }

}
