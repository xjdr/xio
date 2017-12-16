/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * <p>DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *
 * @generated
 */
package com.xjeffrose.xio.marshall.thrift;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Generated;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.server.AbstractNonblockingServer.*;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)", date = "2016-08-19")
public class IpRuleset
    implements org.apache.thrift.TBase<IpRuleset, IpRuleset._Fields>,
        java.io.Serializable,
        Cloneable,
        Comparable<IpRuleset> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC =
      new org.apache.thrift.protocol.TStruct("IpRuleset");

  private static final org.apache.thrift.protocol.TField BLACKLIST_IPS_FIELD_DESC =
      new org.apache.thrift.protocol.TField(
          "blacklistIps", org.apache.thrift.protocol.TType.SET, (short) 1);
  private static final org.apache.thrift.protocol.TField WHITELIST_IPS_FIELD_DESC =
      new org.apache.thrift.protocol.TField(
          "whitelistIps", org.apache.thrift.protocol.TType.SET, (short) 2);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes =
      new HashMap<Class<? extends IScheme>, SchemeFactory>();

  static {
    schemes.put(StandardScheme.class, new IpRulesetStandardSchemeFactory());
    schemes.put(TupleScheme.class, new IpRulesetTupleSchemeFactory());
  }

  public Set<ByteBuffer> blacklistIps; // required
  public Set<ByteBuffer> whitelistIps; // required

  /**
   * The set of fields this struct contains, along with convenience methods for finding and
   * manipulating them.
   */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    BLACKLIST_IPS((short) 1, "blacklistIps"),
    WHITELIST_IPS((short) 2, "whitelistIps");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /** Find the _Fields constant that matches fieldId, or null if its not found. */
    public static _Fields findByThriftId(int fieldId) {
      switch (fieldId) {
        case 1: // BLACKLIST_IPS
          return BLACKLIST_IPS;
        case 2: // WHITELIST_IPS
          return WHITELIST_IPS;
        default:
          return null;
      }
    }

    /** Find the _Fields constant that matches fieldId, throwing an exception if it is not found. */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null)
        throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /** Find the _Fields constant that matches name, or null if its not found. */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;

  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap =
        new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(
        _Fields.BLACKLIST_IPS,
        new org.apache.thrift.meta_data.FieldMetaData(
            "blacklistIps",
            org.apache.thrift.TFieldRequirementType.REQUIRED,
            new org.apache.thrift.meta_data.SetMetaData(
                org.apache.thrift.protocol.TType.SET,
                new org.apache.thrift.meta_data.FieldValueMetaData(
                    org.apache.thrift.protocol.TType.STRING, true))));
    tmpMap.put(
        _Fields.WHITELIST_IPS,
        new org.apache.thrift.meta_data.FieldMetaData(
            "whitelistIps",
            org.apache.thrift.TFieldRequirementType.REQUIRED,
            new org.apache.thrift.meta_data.SetMetaData(
                org.apache.thrift.protocol.TType.SET,
                new org.apache.thrift.meta_data.FieldValueMetaData(
                    org.apache.thrift.protocol.TType.STRING, true))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(IpRuleset.class, metaDataMap);
  }

  public IpRuleset() {}

  public IpRuleset(Set<ByteBuffer> blacklistIps, Set<ByteBuffer> whitelistIps) {
    this();
    this.blacklistIps = blacklistIps;
    this.whitelistIps = whitelistIps;
  }

  /** Performs a deep copy on <i>other</i>. */
  public IpRuleset(IpRuleset other) {
    if (other.isSetBlacklistIps()) {
      Set<ByteBuffer> __this__blacklistIps = new HashSet<ByteBuffer>(other.blacklistIps);
      this.blacklistIps = __this__blacklistIps;
    }
    if (other.isSetWhitelistIps()) {
      Set<ByteBuffer> __this__whitelistIps = new HashSet<ByteBuffer>(other.whitelistIps);
      this.whitelistIps = __this__whitelistIps;
    }
  }

  public IpRuleset deepCopy() {
    return new IpRuleset(this);
  }

  @Override
  public void clear() {
    this.blacklistIps = null;
    this.whitelistIps = null;
  }

  public int getBlacklistIpsSize() {
    return (this.blacklistIps == null) ? 0 : this.blacklistIps.size();
  }

  public java.util.Iterator<ByteBuffer> getBlacklistIpsIterator() {
    return (this.blacklistIps == null) ? null : this.blacklistIps.iterator();
  }

  public void addToBlacklistIps(ByteBuffer elem) {
    if (this.blacklistIps == null) {
      this.blacklistIps = new HashSet<ByteBuffer>();
    }
    this.blacklistIps.add(elem);
  }

  public Set<ByteBuffer> getBlacklistIps() {
    return this.blacklistIps;
  }

  public IpRuleset setBlacklistIps(Set<ByteBuffer> blacklistIps) {
    this.blacklistIps = blacklistIps;
    return this;
  }

  public void unsetBlacklistIps() {
    this.blacklistIps = null;
  }

  /** Returns true if field blacklistIps is set (has been assigned a value) and false otherwise */
  public boolean isSetBlacklistIps() {
    return this.blacklistIps != null;
  }

  public void setBlacklistIpsIsSet(boolean value) {
    if (!value) {
      this.blacklistIps = null;
    }
  }

  public int getWhitelistIpsSize() {
    return (this.whitelistIps == null) ? 0 : this.whitelistIps.size();
  }

  public java.util.Iterator<ByteBuffer> getWhitelistIpsIterator() {
    return (this.whitelistIps == null) ? null : this.whitelistIps.iterator();
  }

  public void addToWhitelistIps(ByteBuffer elem) {
    if (this.whitelistIps == null) {
      this.whitelistIps = new HashSet<ByteBuffer>();
    }
    this.whitelistIps.add(elem);
  }

  public Set<ByteBuffer> getWhitelistIps() {
    return this.whitelistIps;
  }

  public IpRuleset setWhitelistIps(Set<ByteBuffer> whitelistIps) {
    this.whitelistIps = whitelistIps;
    return this;
  }

  public void unsetWhitelistIps() {
    this.whitelistIps = null;
  }

  /** Returns true if field whitelistIps is set (has been assigned a value) and false otherwise */
  public boolean isSetWhitelistIps() {
    return this.whitelistIps != null;
  }

  public void setWhitelistIpsIsSet(boolean value) {
    if (!value) {
      this.whitelistIps = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
      case BLACKLIST_IPS:
        if (value == null) {
          unsetBlacklistIps();
        } else {
          setBlacklistIps((Set<ByteBuffer>) value);
        }
        break;

      case WHITELIST_IPS:
        if (value == null) {
          unsetWhitelistIps();
        } else {
          setWhitelistIps((Set<ByteBuffer>) value);
        }
        break;
    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
      case BLACKLIST_IPS:
        return getBlacklistIps();

      case WHITELIST_IPS:
        return getWhitelistIps();
    }
    throw new IllegalStateException();
  }

  /**
   * Returns true if field corresponding to fieldID is set (has been assigned a value) and false
   * otherwise
   */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
      case BLACKLIST_IPS:
        return isSetBlacklistIps();
      case WHITELIST_IPS:
        return isSetWhitelistIps();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null) return false;
    if (that instanceof IpRuleset) return this.equals((IpRuleset) that);
    return false;
  }

  public boolean equals(IpRuleset that) {
    if (that == null) return false;

    boolean this_present_blacklistIps = true && this.isSetBlacklistIps();
    boolean that_present_blacklistIps = true && that.isSetBlacklistIps();
    if (this_present_blacklistIps || that_present_blacklistIps) {
      if (!(this_present_blacklistIps && that_present_blacklistIps)) return false;
      if (!this.blacklistIps.equals(that.blacklistIps)) return false;
    }

    boolean this_present_whitelistIps = true && this.isSetWhitelistIps();
    boolean that_present_whitelistIps = true && that.isSetWhitelistIps();
    if (this_present_whitelistIps || that_present_whitelistIps) {
      if (!(this_present_whitelistIps && that_present_whitelistIps)) return false;
      if (!this.whitelistIps.equals(that.whitelistIps)) return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_blacklistIps = true && (isSetBlacklistIps());
    list.add(present_blacklistIps);
    if (present_blacklistIps) list.add(blacklistIps);

    boolean present_whitelistIps = true && (isSetWhitelistIps());
    list.add(present_whitelistIps);
    if (present_whitelistIps) list.add(whitelistIps);

    return list.hashCode();
  }

  @Override
  public int compareTo(IpRuleset other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetBlacklistIps()).compareTo(other.isSetBlacklistIps());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetBlacklistIps()) {
      lastComparison =
          org.apache.thrift.TBaseHelper.compareTo(this.blacklistIps, other.blacklistIps);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetWhitelistIps()).compareTo(other.isSetWhitelistIps());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetWhitelistIps()) {
      lastComparison =
          org.apache.thrift.TBaseHelper.compareTo(this.whitelistIps, other.whitelistIps);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot)
      throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("IpRuleset(");
    boolean first = true;

    sb.append("blacklistIps:");
    if (this.blacklistIps == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.blacklistIps, sb);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("whitelistIps:");
    if (this.whitelistIps == null) {
      sb.append("null");
    } else {
      org.apache.thrift.TBaseHelper.toString(this.whitelistIps, sb);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (blacklistIps == null) {
      throw new org.apache.thrift.protocol.TProtocolException(
          "Required field 'blacklistIps' was not present! Struct: " + toString());
    }
    if (whitelistIps == null) {
      throw new org.apache.thrift.protocol.TProtocolException(
          "Required field 'whitelistIps' was not present! Struct: " + toString());
    }
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(
          new org.apache.thrift.protocol.TCompactProtocol(
              new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in)
      throws java.io.IOException, ClassNotFoundException {
    try {
      read(
          new org.apache.thrift.protocol.TCompactProtocol(
              new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class IpRulesetStandardSchemeFactory implements SchemeFactory {
    public IpRulesetStandardScheme getScheme() {
      return new IpRulesetStandardScheme();
    }
  }

  private static class IpRulesetStandardScheme extends StandardScheme<IpRuleset> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, IpRuleset struct)
        throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true) {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) {
          break;
        }
        switch (schemeField.id) {
          case 1: // BLACKLIST_IPS
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set0 = iprot.readSetBegin();
                struct.blacklistIps = new HashSet<ByteBuffer>(2 * _set0.size);
                ByteBuffer _elem1;
                for (int _i2 = 0; _i2 < _set0.size; ++_i2) {
                  _elem1 = iprot.readBinary();
                  struct.blacklistIps.add(_elem1);
                }
                iprot.readSetEnd();
              }
              struct.setBlacklistIpsIsSet(true);
            } else {
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // WHITELIST_IPS
            if (schemeField.type == org.apache.thrift.protocol.TType.SET) {
              {
                org.apache.thrift.protocol.TSet _set3 = iprot.readSetBegin();
                struct.whitelistIps = new HashSet<ByteBuffer>(2 * _set3.size);
                ByteBuffer _elem4;
                for (int _i5 = 0; _i5 < _set3.size; ++_i5) {
                  _elem4 = iprot.readBinary();
                  struct.whitelistIps.add(_elem4);
                }
                iprot.readSetEnd();
              }
              struct.setWhitelistIpsIsSet(true);
            } else {
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, IpRuleset struct)
        throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.blacklistIps != null) {
        oprot.writeFieldBegin(BLACKLIST_IPS_FIELD_DESC);
        {
          oprot.writeSetBegin(
              new org.apache.thrift.protocol.TSet(
                  org.apache.thrift.protocol.TType.STRING, struct.blacklistIps.size()));
          for (ByteBuffer _iter6 : struct.blacklistIps) {
            oprot.writeBinary(_iter6);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.whitelistIps != null) {
        oprot.writeFieldBegin(WHITELIST_IPS_FIELD_DESC);
        {
          oprot.writeSetBegin(
              new org.apache.thrift.protocol.TSet(
                  org.apache.thrift.protocol.TType.STRING, struct.whitelistIps.size()));
          for (ByteBuffer _iter7 : struct.whitelistIps) {
            oprot.writeBinary(_iter7);
          }
          oprot.writeSetEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }
  }

  private static class IpRulesetTupleSchemeFactory implements SchemeFactory {
    public IpRulesetTupleScheme getScheme() {
      return new IpRulesetTupleScheme();
    }
  }

  private static class IpRulesetTupleScheme extends TupleScheme<IpRuleset> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, IpRuleset struct)
        throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      {
        oprot.writeI32(struct.blacklistIps.size());
        for (ByteBuffer _iter8 : struct.blacklistIps) {
          oprot.writeBinary(_iter8);
        }
      }
      {
        oprot.writeI32(struct.whitelistIps.size());
        for (ByteBuffer _iter9 : struct.whitelistIps) {
          oprot.writeBinary(_iter9);
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, IpRuleset struct)
        throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      {
        org.apache.thrift.protocol.TSet _set10 =
            new org.apache.thrift.protocol.TSet(
                org.apache.thrift.protocol.TType.STRING, iprot.readI32());
        struct.blacklistIps = new HashSet<ByteBuffer>(2 * _set10.size);
        ByteBuffer _elem11;
        for (int _i12 = 0; _i12 < _set10.size; ++_i12) {
          _elem11 = iprot.readBinary();
          struct.blacklistIps.add(_elem11);
        }
      }
      struct.setBlacklistIpsIsSet(true);
      {
        org.apache.thrift.protocol.TSet _set13 =
            new org.apache.thrift.protocol.TSet(
                org.apache.thrift.protocol.TType.STRING, iprot.readI32());
        struct.whitelistIps = new HashSet<ByteBuffer>(2 * _set13.size);
        ByteBuffer _elem14;
        for (int _i15 = 0; _i15 < _set13.size; ++_i15) {
          _elem14 = iprot.readBinary();
          struct.whitelistIps.add(_elem14);
        }
      }
      struct.setWhitelistIpsIsSet(true);
    }
  }
}
