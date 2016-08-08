package com.xjeffrose.xio.handler.util;

import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChannelMessageQueueTest {

  private List<String> firedMessages;
  private ChannelMessageQueue<List<String>, String> messageQueue;

  @Before
  public void setUp() throws Exception {
    firedMessages = new LinkedList<>();
    messageQueue = new ChannelMessageQueue<>(List::add);
  }

  @Test
  public void testAddMessage() throws Exception {
    messageQueue.addMessage(firedMessages, "message1");
    assertEquals(messageQueue.size(), 1);
    messageQueue.addMessage(firedMessages, "message2");
    assertEquals(messageQueue.size(), 2);
    assertEquals(firedMessages.size(), 0);
  }

  @Test
  public void testReset() throws Exception {
    messageQueue.addMessage(firedMessages, "message1");
    assertEquals(messageQueue.size(), 1);
    messageQueue.addMessage(firedMessages, "message2");
    assertEquals(messageQueue.size(), 2);
    messageQueue.startStreaming();
    assertEquals(firedMessages.size(), 2);

    messageQueue.reset();
    assertEquals(messageQueue.size(), 0);
    messageQueue.addMessage(firedMessages, "message3");
    assertEquals(firedMessages.size(), 2);
    assertEquals(messageQueue.size(), 1);
    messageQueue.startStreaming();
    assertEquals(firedMessages.size(), 3);
  }

  @Test
  public void testForEach() throws Exception {
    List<String> iteratedMessages = new LinkedList<>();

    messageQueue.addMessage(firedMessages, "message1");
    assertEquals(messageQueue.size(), 1);
    messageQueue.addMessage(firedMessages, "message2");
    assertEquals(messageQueue.size(), 2);
    assertEquals(firedMessages.size(), 0);

    messageQueue.forEach((ctx, msg) -> {
      assertEquals(ctx, firedMessages);
      iteratedMessages.add(msg);
    });
    assertEquals(messageQueue.size(), 2);
    assertEquals(firedMessages.size(), 0);
    assertEquals(iteratedMessages.size(), 2);
  }

  @Test
  public void testStartStreaming() throws Exception {
    messageQueue.addMessage(firedMessages, "message1");
    assertEquals(messageQueue.size(), 1);
    assertEquals(firedMessages.size(), 0);
    messageQueue.addMessage(firedMessages, "message2");
    assertEquals(messageQueue.size(), 2);
    assertEquals(firedMessages.size(), 0);

    messageQueue.startStreaming();
    assertEquals(messageQueue.size(), 0);
    assertEquals(firedMessages.size(), 2);
    messageQueue.addMessage(firedMessages, "message3");
    assertEquals(messageQueue.size(), 0);
    assertEquals(firedMessages.size(), 3);
  }

  @Test
  public void testSize() throws Exception {
    messageQueue.addMessage(firedMessages, "message1");
    assertEquals(messageQueue.size(), 1);
    messageQueue.addMessage(firedMessages, "message2");
    assertEquals(messageQueue.size(), 2);

    messageQueue.reset();
    assertEquals(messageQueue.size(), 0);
    messageQueue.addMessage(firedMessages, "message3");
    assertEquals(messageQueue.size(), 1);
    assertEquals(firedMessages.size(), 0);
  }

  @Test
  public void testIsStreaming() throws Exception {
    assertFalse(messageQueue.isStreaming());
    messageQueue.startStreaming();
    assertTrue(messageQueue.isStreaming());
    messageQueue.reset();
    assertFalse(messageQueue.isStreaming());
  }
}
