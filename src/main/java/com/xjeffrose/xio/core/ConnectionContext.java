package com.xjeffrose.xio.core;


import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;

public interface ConnectionContext {
  /**
   * Gets the remote address of the client that made the request
   *
   * @return The client's remote address as a {@link SocketAddress}
   */
  public SocketAddress getRemoteAddress();

  /**
   * Gets the value of an additional attribute specific to the connection
   *
   * @param attributeName Name of attribute
   * @return Attribute value, or {@code null} if not present
   */
  public Object getAttribute(String attributeName);

  /**
   * Sets the value of an additional attribute specific to the connection
   *
   * @param attributeName Name of attribute
   * @param value New value of attribute. Must not be {@code null}
   * @return Old attribute value, or {@code null} if not present
   */
  public Object setAttribute(String attributeName, Object value);

  /**
   * Removes an additional attribute specific to the connection
   *
   * @param attributeName Name of attribute
   * @return Old attribute value, or {@code null} if attribute was not present
   */
  public Object removeAttribute(String attributeName);

  /**
   * Returns a read-only iterator over the additional attributes
   *
   * @return Iterator
   */
  public Iterator<Map.Entry<String, Object>> attributeIterator();
}