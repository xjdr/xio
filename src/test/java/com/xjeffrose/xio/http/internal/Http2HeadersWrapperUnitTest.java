package com.xjeffrose.xio.http.internal;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Http2HeadersWrapperUnitTest extends Assert {

  Http2HeadersWrapper headers;

  @Before
  public void setUp() {
    headers = new Http2HeadersWrapper(new DefaultHttp2Headers());
  }

  @Test
  public void testAdd() {
    headers.add("header", "value");
    assertEquals("value", headers.delegate().get("header"));
  }

  @Test
  public void testAddCharsequenceArray() {
    headers.add("header", "value1", "value2");
    assertEquals(Arrays.asList("value1", "value2"), headers.delegate().getAll("header"));
  }

  @Test
  public void testAddCharsequenceIterable() {
    headers.add("header", Arrays.asList("value1", "value2"));
    assertEquals(Arrays.asList("value1", "value2"), headers.delegate().getAll("header"));
  }

  @Test
  public void testAddHeaders() {
    Http2Headers other = new DefaultHttp2Headers();
    other.add("header", "value");
    headers.add(other);
    assertEquals(other, headers.delegate());
    assertEquals("value", headers.delegate().get("header"));
  }

  @Test
  public void testAddBoolean() {
    headers.addBoolean("header", true);
    assertEquals(true, headers.delegate().getBoolean("header"));
  }

  @Test
  public void testAddByte() {
    headers.addByte("header", (byte) 1);
    assertEquals((Byte) ((byte) 1), headers.delegate().getByte("header"));
  }

  @Test
  public void testAddChar() {
    headers.addChar("header", 'a');
    assertEquals((Character) 'a', headers.delegate().getChar("header"));
  }

  @Test
  public void testAddDouble() {
    headers.addDouble("header", 1.0);
    assertEquals((Double) 1.0, headers.delegate().getDouble("header"));
  }

  @Test
  public void testAddFloat() {
    headers.addFloat("header", 1.0f);
    assertEquals((Float) 1.0f, headers.delegate().getFloat("header"));
  }

  @Test
  public void testAddInt() {
    headers.addInt("header", 1);
    assertEquals((Integer) 1, headers.delegate().getInt("header"));
  }

  @Test
  public void testAddLong() {
    headers.addLong("header", 1L);
    assertEquals((Long) 1L, headers.delegate().getLong("header"));
  }

  @Test
  public void testAddObjectIterable() {
    headers.addObject("header", Arrays.asList((Object) ((Integer) 1), (Object) ((Integer) 2)));
    assertEquals(Arrays.asList("1", "2"), headers.delegate().getAll("header"));
  }

  @Test
  public void testAddObject() {
    headers.addObject("header", (Object) ((Integer) 1));
    assertEquals("1", headers.delegate().get("header"));
  }

  @Test
  public void testAddObjectArray() {
    headers.addObject("header", (Object) ((Integer) 1), (Object) ((Integer) 2));
    assertEquals(Arrays.asList("1", "2"), headers.delegate().getAll("header"));
  }

  @Test
  public void testAddShort() {
    headers.addShort("header", (short) 1);
    assertEquals((Short) ((short) 1), headers.delegate().getShort("header"));
  }

  @Test
  public void testAddTimeMillis() {
    headers.addTimeMillis("header", 1514400989L);
    // TODO(CK): netty bug? we can't call getTimeMillis() here
    assertEquals((Long) 1514400989L, headers.delegate().getLong("header"));
  }

  @Test
  public void testClear() {
    headers.add("header", "value");
    assertEquals(1, headers.delegate().size());
    headers.clear();
    assertEquals(0, headers.delegate().size());
  }

  @Test
  public void testContains() {
    headers.add("header", "value");
    assertTrue(headers.contains("header"));
  }

  @Test
  public void testContainsValue() {
    headers.add("header", "value");
    assertTrue(headers.contains("header", "value"));
    assertFalse(headers.contains("header", "badvalue"));
    assertFalse(headers.contains("other", "value"));
  }

  @Test
  public void testContainsBoolean() {
    headers.addBoolean("header", true);
    assertTrue(headers.containsBoolean("header", true));
    assertFalse(headers.containsBoolean("header", false));
    assertFalse(headers.containsBoolean("other", true));
  }

  @Test
  public void testContainsByte() {
    headers.addByte("header", (byte) 1);
    assertTrue(headers.containsByte("header", (byte) 1));
    assertFalse(headers.containsByte("header", (byte) 2));
    assertFalse(headers.containsByte("other", (byte) 0));
  }

  @Test
  public void testContainsChar() {
    headers.addChar("header", 'a');
    assertTrue(headers.containsChar("header", 'a'));
    assertFalse(headers.containsChar("header", 'b'));
    assertFalse(headers.containsChar("other", 'b'));
  }

  @Test
  public void testContainsDouble() {
    headers.addDouble("header", 1.0);
    assertTrue(headers.containsDouble("header", 1.0));
    assertFalse(headers.containsDouble("header", 2.0));
    assertFalse(headers.containsDouble("other", 3.0));
  }

  @Test
  public void testContainsFloat() {
    headers.addFloat("header", 1.0f);
    assertTrue(headers.containsFloat("header", 1.0f));
    assertFalse(headers.containsFloat("header", 2.0f));
    assertFalse(headers.containsFloat("other", 3.0f));
  }

  @Test
  public void testContainsInt() {
    headers.addInt("header", 1);
    assertTrue(headers.containsInt("header", 1));
    assertFalse(headers.containsInt("header", 2));
    assertFalse(headers.containsInt("other", 3));
  }

  @Test
  public void testContainsLong() {
    headers.addLong("header", 1L);
    assertTrue(headers.containsLong("header", 1L));
    assertFalse(headers.containsLong("header", 2L));
    assertFalse(headers.containsLong("other", 3L));
  }

  @Test
  public void testContainsObject() {
    headers.addObject("header", (Object) ((Integer) 1));
    assertTrue(headers.containsObject("header", (Object) ((Integer) 1)));
    assertFalse(headers.containsObject("header", (Object) ((Integer) 2)));
    assertFalse(headers.containsObject("other", (Object) ((Integer) 3)));
  }

  @Test
  public void testContainsShort() {
    headers.addShort("header", (short) 1);
    assertTrue(headers.containsShort("header", (short) 1));
    assertFalse(headers.containsShort("header", (short) 2));
    assertFalse(headers.containsShort("other", (short) 3));
  }

  @Test
  public void testContainsTimeMillis() {
    headers.addTimeMillis("header", 1L);
    assertTrue(headers.containsTimeMillis("header", 1L));
    assertFalse(headers.containsTimeMillis("header", 2L));
    assertFalse(headers.containsTimeMillis("other", 3L));
  }

  @Test
  public void testEquals() {
    assertTrue(headers.equals(headers));
    assertFalse(headers.equals((Integer) 1));
    Http2HeadersWrapper otherHeaders = new Http2HeadersWrapper(new DefaultHttp2Headers());
    assertTrue(headers.equals(otherHeaders));
    otherHeaders.add("header", "value");
    assertFalse(headers.equals(otherHeaders));
  }

  @Test
  public void testGet() {
    headers.delegate().add("header", "value");
    assertEquals("value", headers.get("header"));
  }

  @Test
  public void testGetDefault() {
    headers.delegate().add("header", "value");
    assertEquals("value", headers.get("header", "default"));
    assertEquals("default", headers.get("other", "default"));
  }

  @Test
  public void testGetAll() {
    headers.delegate().add("header", "value1", "value2");
    assertEquals(Arrays.asList("value1", "value2"), headers.getAll("header"));
  }

  @Test
  public void testGetAllAndRemove() {
    headers.delegate().add("header", "value1", "value2");
    assertEquals(Arrays.asList("value1", "value2"), headers.getAllAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetAndRemove() {
    headers.delegate().add("header", "value");
    assertEquals("value", headers.getAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetAndRemoveDefault() {
    headers.delegate().add("header", "value");
    assertEquals("value", headers.getAndRemove("header", "default"));
    assertEquals(0, headers.size());
    assertEquals("default", headers.getAndRemove("header", "default"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetBoolean() {
    headers.delegate().addBoolean("header", true);
    assertEquals(true, headers.getBoolean("header"));
  }

  @Test
  public void testGetBooleanDefault() {
    headers.delegate().addBoolean("header", true);
    assertEquals(true, headers.getBoolean("header", false));
    assertEquals(false, headers.getBoolean("other", false));
  }

  @Test
  public void testGetBooleanAndRemove() {
    headers.delegate().addBoolean("header", true);
    assertEquals(true, headers.getBooleanAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetBooleanAndRemoveDefault() {
    headers.delegate().addBoolean("header", true);
    assertEquals(true, headers.getBooleanAndRemove("header", false));
    assertEquals(0, headers.size());
    assertEquals(false, headers.getBooleanAndRemove("header", false));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetByte() {
    headers.delegate().addByte("header", (byte) 1);
    assertEquals((Byte) ((byte) 1), headers.getByte("header"));
  }

  @Test
  public void testGetByteDefault() {
    headers.delegate().addByte("header", (byte) 1);
    assertEquals((byte) 1, headers.getByte("header", (byte) 0));
    assertEquals((byte) 0, headers.getByte("other", (byte) 0));
  }

  @Test
  public void testGetByteAndRemove() {
    headers.delegate().addByte("header", (byte) 1);
    assertEquals((Byte) ((byte) 1), headers.getByteAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetByteAndRemoveDefault() {
    headers.delegate().addByte("header", (byte) 1);
    assertEquals((byte) 1, headers.getByteAndRemove("header", (byte) 0));
    assertEquals(0, headers.size());
    assertEquals((byte) 0, headers.getByteAndRemove("header", (byte) 0));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetChar() {
    headers.delegate().addChar("header", 'a');
    assertEquals((Character) 'a', headers.getChar("header"));
  }

  @Test
  public void testGetCharDefault() {
    headers.delegate().addChar("header", 'a');
    assertEquals('a', headers.getChar("header", 'b'));
    assertEquals('b', headers.getChar("other", 'b'));
  }

  @Test
  public void testGetCharAndRemove() {
    headers.delegate().addChar("header", 'a');
    assertEquals((Character) 'a', headers.getCharAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetCharAndRemoveDefault() {
    headers.delegate().addChar("header", 'a');
    assertEquals('a', headers.getCharAndRemove("header", 'b'));
    assertEquals(0, headers.size());
    assertEquals('b', headers.getCharAndRemove("header", 'b'));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetDouble() {
    headers.delegate().addDouble("header", 1.0);
    assertEquals((Double) 1.0, headers.getDouble("header"));
  }

  @Test
  public void testGetDoubleDefault() {
    headers.delegate().addDouble("header", 1.0);
    assertEquals(1.0, headers.getDouble("header", 2.0), 0.1);
    assertEquals(2.0, headers.getDouble("other", 2.0), 0.1);
  }

  @Test
  public void testGetDoubleAndRemove() {
    headers.delegate().addDouble("header", 1.0);
    assertEquals((Double) 1.0, headers.getDoubleAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetDoubleAndRemoveDefault() {
    headers.delegate().addDouble("header", 1.0);
    assertEquals(1.0, headers.getDoubleAndRemove("header", 2.0), 0.1);
    assertEquals(0, headers.size());
    assertEquals(2.0, headers.getDoubleAndRemove("header", 2.0), 0.1);
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetFloat() {
    headers.delegate().addFloat("header", 1.0f);
    assertEquals((Float) 1.0f, headers.getFloat("header"));
  }

  @Test
  public void testGetFloatDefault() {
    headers.delegate().addFloat("header", 1.0f);
    assertEquals(1.0f, headers.getFloat("header", 2.0f), 0.1);
    assertEquals(2.0f, headers.getFloat("other", 2.0f), 0.1);
  }

  @Test
  public void testGetFloatAndRemove() {
    headers.delegate().addFloat("header", 1.0f);
    assertEquals((Float) 1.0f, headers.getFloatAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetFloatAndRemoveDefault() {
    headers.delegate().addFloat("header", 1.0f);
    assertEquals(1.0f, headers.getFloatAndRemove("header", 2.0f), 0.1);
    assertEquals(0, headers.size());
    assertEquals(2.0f, headers.getFloatAndRemove("header", 2.0f), 0.1);
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetInt() {
    headers.delegate().addInt("header", 1);
    assertEquals((Integer) 1, headers.getInt("header"));
  }

  @Test
  public void testGetIntDefault() {
    headers.delegate().addInt("header", 1);
    assertEquals(1, headers.getInt("header", 2));
    assertEquals(2, headers.getInt("other", 2));
  }

  @Test
  public void testGetIntAndRemove() {
    headers.delegate().addInt("header", 1);
    assertEquals((Integer) 1, headers.getIntAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetIntAndRemoveDefault() {
    headers.delegate().addInt("header", 1);
    assertEquals(1, headers.getIntAndRemove("header", 2));
    assertEquals(0, headers.size());
    assertEquals(2, headers.getIntAndRemove("header", 2));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetLong() {
    headers.delegate().addLong("header", 1L);
    assertEquals((Long) 1L, headers.getLong("header"));
  }

  @Test
  public void testGetLongDefault() {
    headers.delegate().addLong("header", 1L);
    assertEquals(1L, headers.getLong("header", 2L));
    assertEquals(2L, headers.getLong("other", 2L));
  }

  @Test
  public void testGetLongAndRemove() {
    headers.delegate().addLong("header", 1L);
    assertEquals((Long) 1L, headers.getLongAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetLongAndRemoveDefault() {
    headers.delegate().addLong("header", 1L);
    assertEquals(1L, headers.getLongAndRemove("header", 2L));
    assertEquals(0, headers.size());
    assertEquals(2L, headers.getLongAndRemove("header", 2L));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetShort() {
    headers.delegate().addShort("header", (short) 1);
    assertEquals((Short) ((short) 1), headers.getShort("header"));
  }

  @Test
  public void testGetShortDefault() {
    headers.delegate().addShort("header", (short) 1);
    assertEquals((short) 1, headers.getShort("header", (short) 2));
    assertEquals((short) 2, headers.getShort("other", (short) 2));
  }

  @Test
  public void testGetShortAndRemove() {
    headers.delegate().addShort("header", (short) 1);
    assertEquals((Short) ((short) 1), headers.getShortAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetShortAndRemoveDefault() {
    headers.delegate().addShort("header", (short) 1);
    assertEquals((short) 1, headers.getShortAndRemove("header", (short) 2));
    assertEquals(0, headers.size());
    assertEquals((short) 2, headers.getShortAndRemove("header", (short) 2));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetTimeMillis() {
    // TODO(CK): netty bug? we can't call addTimeMillis() here
    headers.delegate().add("header", new Date(1514400000L).toGMTString());
    assertEquals((Long) 1514400000L, headers.getTimeMillis("header"));
  }

  @Test
  public void testGetTimeMillisDefault() {
    // TODO(CK): netty bug? we can't call addTimeMillis() here
    headers.delegate().add("header", new Date(1514400000L).toGMTString());
    assertEquals(1514400000L, headers.getTimeMillis("header", 2L));
    assertEquals(2L, headers.getTimeMillis("other", 2L));
  }

  @Test
  public void testGetTimeMillisAndRemove() {
    // TODO(CK): netty bug? we can't call addTimeMillis() here
    headers.delegate().add("header", new Date(1514400000L).toGMTString());
    assertEquals((Long) 1514400000L, headers.getTimeMillisAndRemove("header"));
    assertEquals(0, headers.size());
  }

  @Test
  public void testGetTimeMillisAndRemoveDefault() {
    // TODO(CK): netty bug? we can't call addTimeMillis() here
    headers.delegate().add("header", new Date(1514400000L).toGMTString());
    assertEquals(1514400000L, headers.getTimeMillisAndRemove("header", 2L));
    assertEquals(0, headers.size());
    assertEquals(2L, headers.getTimeMillisAndRemove("header", 2L));
    assertEquals(0, headers.size());
  }

  @Test
  public void testHashCode() {
    assertNotEquals(0, headers.hashCode());
  }

  @Test
  public void testIsEmpty() {
    assertTrue(headers.isEmpty());
  }

  @Test
  public void testIterator() {
    headers.delegate().add("header1", "value");
    headers.delegate().add("header2", "value");

    Iterator<Map.Entry<CharSequence, CharSequence>> it = headers.iterator();
    assertTrue(it.hasNext());
    assertEquals(new AbstractMap.SimpleEntry("header1", "value"), it.next());
    assertTrue(it.hasNext());
    assertEquals(new AbstractMap.SimpleEntry("header2", "value"), it.next());
    assertFalse(it.hasNext());
  }

  @Test
  public void testNames() {
    headers.delegate().add("header1", "value");
    headers.delegate().add("header2", "value");
    assertEquals(new HashSet<>(Arrays.asList("header1", "header2")), headers.names());
  }

  @Test
  public void testRemove() {
    headers.delegate().add("header", "value");
    assertTrue(headers.remove("header"));
    assertEquals(0, headers.delegate().size());
    assertFalse(headers.remove("header"));
    assertEquals(0, headers.delegate().size());
  }

  @Test
  public void testSet() {
    headers.set("header", "value");
    assertEquals("value", headers.delegate().get("header"));
    headers.set("header", "value2");
    assertEquals("value2", headers.delegate().get("header"));
  }

  @Test
  public void testSetCharsequenceArray() {
    headers.set("header", "value1", "value2");
    assertEquals(Arrays.asList("value1", "value2"), headers.delegate().getAll("header"));
    headers.set("header", "value3", "value4");
    assertEquals(Arrays.asList("value3", "value4"), headers.delegate().getAll("header"));
  }

  @Test
  public void testSetCharsequenceIterable() {
    headers.set("header", Arrays.asList("value1", "value2"));
    assertEquals(Arrays.asList("value1", "value2"), headers.delegate().getAll("header"));
    headers.set("header", Arrays.asList("value3", "value4"));
    assertEquals(Arrays.asList("value3", "value4"), headers.delegate().getAll("header"));
  }

  @Test
  public void testSetHeaders() {
    Http2Headers other = new DefaultHttp2Headers();
    other.set("header", "value");
    headers.set(other);
    assertEquals(other, headers.delegate());
    assertEquals("value", headers.delegate().get("header"));
    other.set("header", "value2");
    headers.set(other);
    assertEquals(other, headers.delegate());
    assertEquals("value2", headers.delegate().get("header"));
  }

  @Test
  public void testSetAll() {
    Http2Headers other = new DefaultHttp2Headers();
    other.set("header", "value");
    headers.setAll(other);
    assertEquals(other, headers.delegate());
    assertEquals("value", headers.delegate().get("header"));
    other.set("header", "value2");
    headers.setAll(other);
    assertEquals(other, headers.delegate());
    assertEquals("value2", headers.delegate().get("header"));
  }

  @Test
  public void testSetBoolean() {
    headers.setBoolean("header", true);
    assertEquals(true, headers.delegate().getBoolean("header"));
    headers.setBoolean("header", false);
    assertEquals(false, headers.delegate().getBoolean("header"));
  }

  @Test
  public void testSetByte() {
    headers.setByte("header", (byte) 1);
    assertEquals((Byte) ((byte) 1), headers.delegate().getByte("header"));
    headers.setByte("header", (byte) 2);
    assertEquals((Byte) ((byte) 2), headers.delegate().getByte("header"));
  }

  @Test
  public void testSetChar() {
    headers.setChar("header", 'a');
    assertEquals((Character) 'a', headers.delegate().getChar("header"));
    headers.setChar("header", 'b');
    assertEquals((Character) 'b', headers.delegate().getChar("header"));
  }

  @Test
  public void testSetDouble() {
    headers.setDouble("header", 1.0);
    assertEquals((Double) 1.0, headers.delegate().getDouble("header"));
    headers.setDouble("header", 2.0);
    assertEquals((Double) 2.0, headers.delegate().getDouble("header"));
  }

  @Test
  public void testSetFloat() {
    headers.setFloat("header", 1.0f);
    assertEquals((Float) 1.0f, headers.delegate().getFloat("header"));
    headers.setFloat("header", 2.0f);
    assertEquals((Float) 2.0f, headers.delegate().getFloat("header"));
  }

  @Test
  public void testSetInt() {
    headers.setInt("header", 1);
    assertEquals((Integer) 1, headers.delegate().getInt("header"));
    headers.setInt("header", 2);
    assertEquals((Integer) 2, headers.delegate().getInt("header"));
  }

  @Test
  public void testSetLong() {
    headers.setLong("header", 1L);
    assertEquals((Long) 1L, headers.delegate().getLong("header"));
    headers.setLong("header", 2L);
    assertEquals((Long) 2L, headers.delegate().getLong("header"));
  }

  @Test
  public void testSetObjectIterable() {
    headers.setObject("header", Arrays.asList((Object) ((Integer) 1), (Object) ((Integer) 2)));
    assertEquals(Arrays.asList("1", "2"), headers.delegate().getAll("header"));
    headers.setObject("header", Arrays.asList((Object) ((Integer) 3), (Object) ((Integer) 4)));
    assertEquals(Arrays.asList("3", "4"), headers.delegate().getAll("header"));
  }

  @Test
  public void testSetObject() {
    headers.setObject("header", (Object) ((Integer) 1));
    assertEquals("1", headers.delegate().get("header"));
    headers.setObject("header", (Object) ((Integer) 2));
    assertEquals("2", headers.delegate().get("header"));
  }

  @Test
  public void testSetObjectArray() {
    headers.setObject("header", (Object) ((Integer) 1), (Object) ((Integer) 2));
    assertEquals(Arrays.asList("1", "2"), headers.delegate().getAll("header"));
    headers.setObject("header", (Object) ((Integer) 3), (Object) ((Integer) 4));
    assertEquals(Arrays.asList("3", "4"), headers.delegate().getAll("header"));
  }

  @Test
  public void testSetShort() {
    headers.setShort("header", (short) 1);
    assertEquals((Short) ((short) 1), headers.delegate().getShort("header"));
    headers.setShort("header", (short) 2);
    assertEquals((Short) ((short) 2), headers.delegate().getShort("header"));
  }

  @Test
  public void testSetTimeMillis() {
    headers.setTimeMillis("header", 1514400989L);
    // TODO(CK): netty bug? we can't call getTimeMillis() here
    assertEquals((Long) 1514400989L, headers.delegate().getLong("header"));
  }

  @Test
  public void testSize() {
    assertEquals(0, headers.size());
  }
}
