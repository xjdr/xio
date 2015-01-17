package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

@SuppressWarnings (value={"unchecked", "fallthrough"})

class HttpParser {
  private static final Logger log = Log.getLogger(HttpParser.class.getName());
  /* private final ByteBuffer bb; */
  private final HttpRequest req = new HttpRequest();
  private int lastByteRead;
  private StringBuilder debug = new StringBuilder();

  HttpParser() {
    lastByteRead = -1;
  }

  public boolean parse(ByteBuffer bb) {
    ByteBuffer temp = bb.duplicate();
    ParseState result = ParseState.good;
    temp.flip();
    temp.position(lastByteRead + 1);
    while (temp.hasRemaining()) {
      int index = temp.position();
      lastByteRead = index;
      byte bite = temp.get();
      /* log.info("byte " + bite + " index " + index); */
      /* debug.append((char)bite); */
      result = parseSegment(bite);
    }
    if(result == ParseState.good) {
      return true;
    }
    return false;
  }

  private enum state {
    method_start,
    method,
    uri,
    http_version_h,
    http_version_t_1,
    http_version_t_2,
    http_version_p,
    http_version_slash,
    http_version_major_start,
    http_version_major,
    http_version_minor_start,
    http_version_minor,
    expecting_newline_1,
    header_line_start,
    header_lws,
    header_name,
    space_before_header_value,
    header_value,
    expecting_newline_2,
    expecting_newline_3
  };


  private state state_ = state.method_start;

  private boolean is_char(int c) {
    return c >= 0 && c <= 127;
  }

  private boolean is_ctl(int c) {
    return (c >= 0 && c <= 31) || (c == 127);
  }

  private boolean is_tspecial(int c) {
    switch (c) {
    case '(': case ')': case '<': case '>': case '@':
    case ',': case ';': case ':': case '\\': case '"':
    case '/': case '[': case ']': case '?': case '=':
    case '{': case '}': case ' ': case '\t':
      return true;
    default:
      return false;
    }
  }

  private boolean is_digit(int c) {
    return c >= '0' && c <= '9';
  }

  enum ParseState {
    good,
    bad,
    indeterminate;

    static public ParseState fromBoolean(boolean state) {
      if (state) {
        return ParseState.good;
      } else {
        return ParseState.bad;
      }
    }
  }

  private ParseState parseSegment(byte input) {
    byte[] inputs = {input};
    switch (state_) {
      case method_start:
        if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          state_ = state.method;
          req.method.push_back(new String(inputs, Charset.forName("UTF-8")));
          return ParseState.indeterminate;
        }
      case method:
        if (input == ' ') {
          state_ = state.uri;
          return ParseState.indeterminate;
        } else if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          req.method.push_back(new String(inputs, Charset.forName("UTF-8")));
          return ParseState.indeterminate;
        }
      case uri:
        if (input == ' ') {
          state_ = state.http_version_h;
          return ParseState.indeterminate;
        } else if (is_ctl(input)) {
          return ParseState.bad;
        } else {
          req.uri.push_back(new String(inputs, Charset.forName("UTF-8")));
          return ParseState.indeterminate;
        }
      case http_version_h:
        if (input == 'H') {
          state_ = state.http_version_t_1;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_t_1:
        if (input == 'T') {
          state_ = state.http_version_t_2;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_t_2:
        if (input == 'T') {
          state_ = state.http_version_p;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_p:
        if (input == 'P') {
          state_ = state.http_version_slash;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_slash:
        if (input == '/') {
          req.http_version_major = 0;
          req.http_version_minor = 0;
          state_ = state.http_version_major_start;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_major_start:
        if (is_digit(input)) {
          req.http_version_major = req.http_version_major * 10 + input - '0';
          state_ = state.http_version_major;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_major:
        if (input == '.') {
          state_ = state.http_version_minor_start;
          return ParseState.indeterminate;
        } else if (is_digit(input)) {
          req.http_version_major = req.http_version_major * 10 + input - '0';
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_minor_start:
        if (is_digit(input)) {
          req.http_version_minor = req.http_version_minor * 10 + input - '0';
          state_ = state.http_version_minor;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_minor:
        if (input == '\r') {
          state_ = state.expecting_newline_1;
          return ParseState.indeterminate;
        } else if (is_digit(input)) {
          req.http_version_minor = req.http_version_minor * 10 + input - '0';
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case expecting_newline_1:
        if (input == '\n') {
          state_ = state.header_line_start;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case header_line_start:
        if (input == '\r') {
          state_ = state.expecting_newline_3;
          return ParseState.indeterminate;
        } else if (!req.headers.empty() && (input == ' ' || input == '\t')) {
          state_ = state.header_lws;
          return ParseState.indeterminate;
        } else if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          req.headers.push_back();
          req.headers.push_back_name(new String(inputs, Charset.forName("UTF-8")));
          state_ = state.header_name;
          return ParseState.indeterminate;
        }
      case header_lws:
        if (input == '\r') {
          state_ = state.expecting_newline_2;
          return ParseState.indeterminate;
        } else if (input == ' ' || input == '\t') {
          return ParseState.indeterminate;
        } else if (is_ctl(input)) {
          return ParseState.bad;
        } else {
          state_ = state.header_value;
          req.headers.push_back_value(new String(inputs, Charset.forName("UTF-8")));
          return ParseState.indeterminate;
        }
      case header_name:
        if (input == ':') {
          state_ = state.space_before_header_value;
          return ParseState.indeterminate;
        } else if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          req.headers.push_back_name(new String(inputs, Charset.forName("UTF-8")));
          return ParseState.indeterminate;
        }
      case space_before_header_value:
        if (input == ' ') {
          state_ = state.header_value;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case header_value:
        if (input == '\r') {
          state_ = state.expecting_newline_2;
          return ParseState.indeterminate;
        } else if (is_ctl(input)) {
          return ParseState.bad;
        } else {
          req.headers.push_back_value(new String(inputs, Charset.forName("UTF-8")));
          return ParseState.indeterminate;
        }
      case expecting_newline_2:
        if (input == '\n') {
          state_ = state.header_line_start;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case expecting_newline_3:
        /* log.info("andrei needs a diaper change: " + debug); */
        return ParseState.fromBoolean(input == '\n');
      default:
        return ParseState.bad;
      }
    }

}

