package com.xjeffrose.xio.http.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.xjeffrose.xio.http.Headers;
import io.netty.handler.codec.CharSequenceValueConverter;
import io.netty.handler.codec.ValueConverter;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Http2HeadersWrapper implements Headers {

  private final Http2Headers delegate;
  private final ValueConverter<CharSequence> valueConverter = CharSequenceValueConverter.INSTANCE;

  @VisibleForTesting
  io.netty.handler.codec.http2.Http2Headers delegate() {
    return delegate;
  }

  public Http2HeadersWrapper(Http2Headers delegate) {
    this.delegate = delegate;
  }

  @Override
  public Headers add(CharSequence name, CharSequence value) {
    delegate.add(name, value);
    return this;
  }

  @Override
  public Headers add(CharSequence name, CharSequence... values) {
    delegate.add(name, values);
    return this;
  }

  @Override
  public Headers add(CharSequence name, Iterable<? extends CharSequence> values) {
    delegate.add(name, values);
    return this;
  }

  @Override
  public Headers add(
      io.netty.handler.codec.Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
    for (Map.Entry<? extends CharSequence, ? extends CharSequence> entry : headers) {
      delegate.add(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public Headers addBoolean(CharSequence name, boolean value) {
    return add(name, valueConverter.convertBoolean(value));
  }

  @Override
  public Headers addByte(CharSequence name, byte value) {
    return add(name, valueConverter.convertByte(value));
  }

  @Override
  public Headers addChar(CharSequence name, char value) {
    return add(name, valueConverter.convertChar(value));
  }

  @Override
  public Headers addDouble(CharSequence name, double value) {
    return add(name, valueConverter.convertDouble(value));
  }

  @Override
  public Headers addFloat(CharSequence name, float value) {
    return add(name, valueConverter.convertFloat(value));
  }

  @Override
  public Headers addInt(CharSequence name, int value) {
    return add(name, valueConverter.convertInt(value));
  }

  @Override
  public Headers addLong(CharSequence name, long value) {
    return add(name, valueConverter.convertLong(value));
  }

  @Override
  public Headers addObject(CharSequence name, Iterable<?> values) {
    delegate.addObject(name, values);
    return this;
  }

  @Override
  public Headers addObject(CharSequence name, Object value) {
    delegate.addObject(name, value);
    return this;
  }

  @Override
  public Headers addObject(CharSequence name, Object... values) {
    delegate.addObject(name, values);
    return this;
  }

  @Override
  public Headers addShort(CharSequence name, short value) {
    return add(name, valueConverter.convertShort(value));
  }

  @Override
  public Headers addTimeMillis(CharSequence name, long value) {
    delegate.addTimeMillis(name, value);
    return this;
  }

  @Override
  public Headers clear() {
    delegate.clear();
    return this;
  }

  @Override
  public boolean contains(CharSequence name) {
    return delegate.contains(name);
  }

  @Override
  public boolean contains(CharSequence name, CharSequence value) {
    return delegate.contains(name, value);
  }

  @Override
  public boolean containsBoolean(CharSequence name, boolean value) {
    return contains(name, valueConverter.convertBoolean(value));
  }

  @Override
  public boolean containsByte(CharSequence name, byte value) {
    return contains(name, valueConverter.convertByte(value));
  }

  @Override
  public boolean containsChar(CharSequence name, char value) {
    return contains(name, valueConverter.convertChar(value));
  }

  @Override
  public boolean containsDouble(CharSequence name, double value) {
    return contains(name, valueConverter.convertDouble(value));
  }

  @Override
  public boolean containsFloat(CharSequence name, float value) {
    return contains(name, valueConverter.convertFloat(value));
  }

  @Override
  public boolean containsInt(CharSequence name, int value) {
    return contains(name, valueConverter.convertInt(value));
  }

  @Override
  public boolean containsLong(CharSequence name, long value) {
    return contains(name, valueConverter.convertLong(value));
  }

  @Override
  public boolean containsObject(CharSequence name, Object value) {
    return contains(name, valueConverter.convertObject(checkNotNull(value, "value")));
  }

  @Override
  public boolean containsShort(CharSequence name, short value) {
    return contains(name, valueConverter.convertShort(value));
  }

  @Override
  public boolean containsTimeMillis(CharSequence name, long value) {
    return contains(name, valueConverter.convertTimeMillis(value));
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Http2HeadersWrapper && delegate.equals(((Http2HeadersWrapper) o).delegate);
  }

  @Override
  public CharSequence get(CharSequence name) {
    return delegate.get(name);
  }

  @Override
  public CharSequence get(CharSequence name, CharSequence defaultValue) {
    return delegate.get(name, defaultValue);
  }

  @Override
  public List<CharSequence> getAll(CharSequence name) {
    return new ArrayList<CharSequence>(delegate.getAll(name));
  }

  @Override
  public List<CharSequence> getAllAndRemove(CharSequence name) {
    List<CharSequence> result = getAll(name);
    delegate.remove(name);
    return result;
  }

  @Override
  public CharSequence getAndRemove(CharSequence name) {
    CharSequence result = delegate.get(name);
    delegate.remove(name);
    return result;
  }

  @Override
  public CharSequence getAndRemove(CharSequence name, CharSequence defaultValue) {
    if (delegate.contains(name)) {
      CharSequence result = delegate.get(name);
      delegate.remove(name);
      return result;
    }

    return defaultValue;
  }

  @Override
  public Boolean getBoolean(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToBoolean(value) : null;
  }

  @Override
  public boolean getBoolean(CharSequence name, boolean defaultValue) {
    Boolean value = getBoolean(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Boolean getBooleanAndRemove(CharSequence name) {
    CharSequence value = getAndRemove(name);
    return value != null ? valueConverter.convertToBoolean(value) : null;
  }

  @Override
  public boolean getBooleanAndRemove(CharSequence name, boolean defaultValue) {
    Boolean value = getBooleanAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Byte getByte(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToByte(value) : null;
  }

  @Override
  public byte getByte(CharSequence name, byte defaultValue) {
    Byte value = getByte(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Byte getByteAndRemove(CharSequence name) {
    CharSequence value = getAndRemove(name);
    return value != null ? valueConverter.convertToByte(value) : null;
  }

  @Override
  public byte getByteAndRemove(CharSequence name, byte defaultValue) {
    Byte value = getByteAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Character getChar(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToChar(value) : null;
  }

  @Override
  public char getChar(CharSequence name, char defaultValue) {
    Character value = getChar(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Character getCharAndRemove(CharSequence name) {
    return delegate.getCharAndRemove(name);
  }

  @Override
  public char getCharAndRemove(CharSequence name, char defaultValue) {
    Character value = getCharAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Double getDouble(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToDouble(value) : null;
  }

  @Override
  public double getDouble(CharSequence name, double defaultValue) {
    Double value = getDouble(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Double getDoubleAndRemove(CharSequence name) {
    CharSequence value = getAndRemove(name);
    return value != null ? valueConverter.convertToDouble(value) : null;
  }

  @Override
  public double getDoubleAndRemove(CharSequence name, double defaultValue) {
    Double value = getDoubleAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Float getFloat(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToFloat(value) : null;
  }

  @Override
  public float getFloat(CharSequence name, float defaultValue) {
    Float value = getFloat(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Float getFloatAndRemove(CharSequence name) {
    CharSequence value = getAndRemove(name);
    return value != null ? valueConverter.convertToFloat(value) : null;
  }

  @Override
  public float getFloatAndRemove(CharSequence name, float defaultValue) {
    Float value = getFloatAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Integer getInt(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToInt(value) : null;
  }

  @Override
  public int getInt(CharSequence name, int defaultValue) {
    Integer value = getInt(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Integer getIntAndRemove(CharSequence name) {
    CharSequence value = getAndRemove(name);
    return value != null ? valueConverter.convertToInt(value) : null;
  }

  @Override
  public int getIntAndRemove(CharSequence name, int defaultValue) {
    Integer value = getIntAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Long getLong(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToLong(value) : null;
  }

  @Override
  public long getLong(CharSequence name, long defaultValue) {
    Long value = getLong(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Long getLongAndRemove(CharSequence name) {
    CharSequence value = getAndRemove(name);
    return value != null ? valueConverter.convertToLong(value) : null;
  }

  @Override
  public long getLongAndRemove(CharSequence name, long defaultValue) {
    Long value = getLongAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Short getShort(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToShort(value) : null;
  }

  @Override
  public short getShort(CharSequence name, short defaultValue) {
    Short value = getShort(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Short getShortAndRemove(CharSequence name) {
    CharSequence value = getAndRemove(name);
    return value != null ? valueConverter.convertToShort(value) : null;
  }

  @Override
  public short getShortAndRemove(CharSequence name, short defaultValue) {
    Short value = getShortAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Long getTimeMillis(CharSequence name) {
    CharSequence value = get(name);
    return value != null ? valueConverter.convertToTimeMillis(value) : null;
  }

  @Override
  public long getTimeMillis(CharSequence name, long defaultValue) {
    Long value = getTimeMillis(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public Long getTimeMillisAndRemove(CharSequence name) {
    CharSequence value = getAndRemove(name);
    return value != null ? valueConverter.convertToTimeMillis(value) : null;
  }

  @Override
  public long getTimeMillisAndRemove(CharSequence name, long defaultValue) {
    Long value = getTimeMillisAndRemove(name);
    return value != null ? value : defaultValue;
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public Iterator<Map.Entry<CharSequence, CharSequence>> iterator() {
    return delegate.iterator();
  }

  @Override
  public Set<CharSequence> names() {
    return new HashSet<CharSequence>(delegate.names());
  }

  @Override
  public boolean remove(CharSequence name) {
    int before = delegate.size();
    delegate.remove(name);
    return before != delegate.size();
  }

  @Override
  public Headers set(CharSequence name, CharSequence value) {
    delegate.set(name, value);
    return this;
  }

  @Override
  public Headers set(CharSequence name, CharSequence... values) {
    return set(name, Arrays.asList(values));
  }

  @Override
  public Headers set(
      io.netty.handler.codec.Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
    clear();
    for (Map.Entry<? extends CharSequence, ? extends CharSequence> entry : headers) {
      delegate.add(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public Headers set(CharSequence name, Iterable<? extends CharSequence> values) {
    delegate.set(name, values);
    return this;
  }

  @Override
  public Headers setAll(
      io.netty.handler.codec.Headers<? extends CharSequence, ? extends CharSequence, ?> headers) {
    for (Map.Entry<? extends CharSequence, ? extends CharSequence> entry : headers) {
      delegate.set(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public Headers setBoolean(CharSequence name, boolean value) {
    return set(name, valueConverter.convertBoolean(value));
  }

  @Override
  public Headers setByte(CharSequence name, byte value) {
    return set(name, valueConverter.convertByte(value));
  }

  @Override
  public Headers setChar(CharSequence name, char value) {
    return set(name, valueConverter.convertChar(value));
  }

  @Override
  public Headers setDouble(CharSequence name, double value) {
    return set(name, valueConverter.convertDouble(value));
  }

  @Override
  public Headers setFloat(CharSequence name, float value) {
    return set(name, valueConverter.convertFloat(value));
  }

  @Override
  public Headers setInt(CharSequence name, int value) {
    return set(name, valueConverter.convertInt(value));
  }

  @Override
  public Headers setLong(CharSequence name, long value) {
    return set(name, valueConverter.convertLong(value));
  }

  @Override
  public Headers setObject(CharSequence name, Iterable<?> values) {
    delegate.setObject(name, values);
    return this;
  }

  @Override
  public Headers setObject(CharSequence name, Object value) {
    delegate.setObject(name, value);
    return this;
  }

  @Override
  public Headers setObject(CharSequence name, Object... values) {
    delegate.setObject(name, values);
    return this;
  }

  @Override
  public Headers setShort(CharSequence name, short value) {
    return set(name, valueConverter.convertShort(value));
  }

  @Override
  public Headers setTimeMillis(CharSequence name, long value) {
    return set(name, valueConverter.convertTimeMillis(value));
  }

  @Override
  public int size() {
    return delegate.size();
  }

  /** Return an Http1 Headers object based on the values in the underlying Http2Headers object. */
  @Override
  public HttpHeaders http1Headers(boolean isTrailer, boolean isRequest) {
    try {
      HttpHeaders headers = new DefaultHttpHeaders();
      HttpConversionUtil.addHttp2ToHttpHeaders(
          -1, delegate, headers, HttpVersion.HTTP_1_1, isTrailer, isRequest);
      return headers;
    } catch (Http2Exception e) {
      throw new RuntimeException(e);
    }
  }
}
