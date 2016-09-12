package com.xjeffrose.xio.client.mux;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ClientCodec.class, LoggerFactory.class})
public class ClientCodecUnitTest extends Assert {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  EmbeddedChannel channel;

  ClientCodec codec;

  @Before
  public void setUp() throws Exception {
    mockStatic(LoggerFactory.class);
    Logger logger = mock(Logger.class);
    when(LoggerFactory.getLogger(any(Class.class))).thenReturn(logger);

    channel = new EmbeddedChannel();
    codec = new ClientCodec();
    channel.pipeline()
      .addLast(codec)
    ;
  }

  @Test
  public void testRequestNoResponse() {
    Integer payload = new Integer(1);
    Request request = new Request(UUID.randomUUID(), SettableFuture.create());
    Message message = new Message(request, payload);
    channel.writeOutbound(message);
    channel.runPendingTasks();

    // assert request not in map
    assertNull(codec.getRequest(codec.getMapping(channel), message));

    // assert two objects written in the correct order
    assertEquals(payload, channel.outboundMessages().poll());
    assertEquals(message, channel.outboundMessages().poll());
  }

  @Test
  public void testRequestExpectedResponse() {
    Integer payload = new Integer(1);
    Request request = new Request(UUID.randomUUID(), SettableFuture.create(), SettableFuture.create());
    Message message = new Message(request, payload);
    channel.writeOutbound(message);
    channel.runPendingTasks();

    // assert request in map
    assertEquals(request, codec.getRequest(codec.getMapping(channel), message));

    // assert two objects written in the correct order
    assertEquals(payload, channel.outboundMessages().poll());
    assertEquals(message, channel.outboundMessages().poll());
  }

  @Test
  public void testIncorrectMessageType() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage("Only Message objects can be written to ClientCodec");

    channel.writeOutbound(new Integer(1));
  }

  @Test
  public void testExpectedResponse() throws Exception {
    Request request = new Request(UUID.randomUUID(), SettableFuture.create(), SettableFuture.create());
    codec.setRequest(codec.getMapping(channel), request);

    Integer payload = new Integer(1);
    Message message = Message.buildResponse(request.getId());

    channel.writeInbound(payload);
    channel.writeInbound(message);
    channel.runPendingTasks();

    Response response = request.getResponseFuture().get(1, TimeUnit.SECONDS);
    assertEquals(request.getId(), response.getInResponseTo());
    assertEquals(payload, response.getPayload());
  }

  @Test
  public void testRequestUnexpectedResponse() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage("Unexpected response received for request id 'db7598b0-a153-4ab1-85c2-2120a729c2db'");

    Request request = new Request(UUID.fromString("db7598b0-a153-4ab1-85c2-2120a729c2db"), SettableFuture.create(), SettableFuture.create());
    Integer payload = new Integer(1);
    Message message = Message.buildResponse(request.getId());

    channel.writeInbound(payload);
    channel.writeInbound(message);
  }

  @Test
  public void testNoResponsePayload() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage("No response payload received for request id 'a3f3a6db-aefc-40d3-bd49-3d52ee474630'");

    Request request = new Request(UUID.fromString("a3f3a6db-aefc-40d3-bd49-3d52ee474630"), SettableFuture.create(), SettableFuture.create());
    Message message = Message.buildResponse(request.getId());

    channel.writeInbound(message);
  }

  @Test
  public void testMultipleResponsePayloads() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage("Multiple response payloads received for request id '8f98f582-b726-408e-b974-040276b408dd'");

    Request request = new Request(UUID.fromString("8f98f582-b726-408e-b974-040276b408dd"), SettableFuture.create(), SettableFuture.create());
    Message message = Message.buildResponse(request.getId());
    Integer payload1 = new Integer(1);
    Integer payload2 = new Integer(2);

    channel.writeInbound(payload1);
    channel.writeInbound(payload2);
    channel.writeInbound(message);
  }

}
