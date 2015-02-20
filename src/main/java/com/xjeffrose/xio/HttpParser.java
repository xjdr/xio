package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

@SuppressWarnings (value={"fallthrough"})

class HttpParser {
  private static final Logger log = Log.getLogger(HttpParser.class.getName());

  private int lastByteRead;
  private HttpRequest req;
  private ByteBuffer temp;

  private state state_ = state.method_start;
  public boolean done = false;

  HttpParser() {
    lastByteRead = -1;
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

  public boolean parse(HttpRequest req) {
    this.req = req;
    this.temp = req.buffer.duplicate();

    ParseState result = ParseState.good;
    temp.flip();
    temp.position(lastByteRead + 1);
    while (temp.hasRemaining()) {
      lastByteRead = temp.position();
      result = parseSegment(temp.get());
    }
    if(result == ParseState.good) {
      return true;
    }
    return false;
  }

  public HttpRequest request() {
    return req;
  }


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

  private boolean is_digit(char c) {
    return c >= '0' && c <= '9';
  }

  private enum ParseState {
    good,
    bad,
    indeterminate;

    private static final ParseState fromBoolean(boolean state) {
      if (state) {
        return ParseState.good;
      } else {
        return ParseState.bad;
      }
    }
  }

  private ParseState parseSegment(byte input) {
    switch (state_) {
      case method_start:
        if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          state_ = state.method;
          req.method.tick(lastByteRead);
          return ParseState.indeterminate;
        }
      case method:
        if (input == ' ') {
          state_ = state.uri;
          return ParseState.indeterminate;
        } else if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          req.method.tick(lastByteRead);
          return ParseState.indeterminate;
        }
      case uri:
        if (input == ' ') {
          state_ = state.http_version_h;
          return ParseState.indeterminate;
        } else if (is_ctl(input)) {
          return ParseState.bad;
        } else {
          req.setMethod();
          req.uri.tick(lastByteRead);
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
          state_ = state.http_version_major_start;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_major_start:
        if (is_digit((char)input)) {
          req.http_version_major = (char)input - '0';
          state_ = state.http_version_major;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_major:
        if (input == '.') {
          state_ = state.http_version_minor_start;
          return ParseState.indeterminate;
        } else if (is_digit((char)input)) {
          req.http_version_major = req.http_version_major * 10 + (char)input - '0';
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_minor_start:
        if (is_digit((char)input)) {
          req.http_version_minor = (char)input - '0';
          state_ = state.http_version_minor;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_minor:
        if (input == '\r') {
          state_ = state.expecting_newline_1;
          return ParseState.indeterminate;
        } else if (is_digit((char)input)) {
          req.http_version_minor = req.http_version_minor * 10 + (char)input - '0';
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
          req.headers.newHeader();
          req.headers.tick(lastByteRead);
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
          req.headers.newValue();
          req.headers.tick(lastByteRead);
          return ParseState.indeterminate;
        }
      case header_name:
        if (input == ':') {
          state_ = state.space_before_header_value;
          return ParseState.indeterminate;
        } else if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          req.headers.tick(lastByteRead);
          return ParseState.indeterminate;
        }
      case space_before_header_value:
        if (input == ' ') {
          state_ = state.header_value;
          req.headers.newValue();
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
          req.headers.tick(lastByteRead);
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
        finish();
        return ParseState.fromBoolean(input == '\n');
      default:
        return ParseState.bad;
      }
    }

  private void finish() {
    req.headers.done();
    done = true;
    switch(req.method_) {
      case get:
        return;
      case post:
        req.body.set(lastByteRead);
        return;
      case put:
        return;
      case delete:
        return;
      default:
        return;
    }
  }

}

