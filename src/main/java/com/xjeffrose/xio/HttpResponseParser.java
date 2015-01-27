package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.util.logging.*;

import com.xjeffrose.log.*;

@SuppressWarnings (value={"fallthrough"})

// http://tools.ietf.org/html/rfc2616
class HttpResponseParser {
  private static final Logger log = Log.getLogger(HttpResponseParser.class.getName());

  private int lastByteRead;
  private final HttpResponse response;
  private ByteBuffer temp;

  HttpResponseParser(HttpResponse response) {
    this.response = response;
    lastByteRead = -1;
  }

  private enum state {
    http_version_h,
    http_version_t_1,
    http_version_t_2,
    http_version_p,
    http_version_slash,
    http_version_major_start,
    http_version_major,
    http_version_minor_start,
    http_version_minor,
    status_code,
    reason_phrase,
    expecting_newline_1,
    header_line_start,
    header_lws,
    header_name,
    space_before_header_value,
    header_value,
    expecting_newline_2,
    expecting_newline_3
  };

  private state state_ = state.http_version_h;

  public boolean parse(ByteBuffer bb) {
    this.temp = bb.duplicate();

    ParseState result = ParseState.good;
    response.bb(temp);
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
          response.http_version_major = (char)input - '0';
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
          response.http_version_major = response.http_version_major * 10 + (char)input - '0';
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_minor_start:
        if (is_digit((char)input)) {
          response.http_version_minor = (char)input - '0';
          state_ = state.http_version_minor;
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case http_version_minor:
        if (input == ' ') {
          state_ = state.status_code;
          return ParseState.indeterminate;
        } else if (is_digit((char)input)) {
          response.http_version_minor = response.http_version_minor * 10 + (char)input - '0';
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case status_code:
        if (input == ' ') {
          state_ = state.reason_phrase;
          return ParseState.indeterminate;
        } else if (is_digit((char)input)) {
          return ParseState.indeterminate;
        } else {
          return ParseState.bad;
        }
      case reason_phrase:
        if (input == '\r') {
          state_ = state.expecting_newline_1;
          return ParseState.indeterminate;
        } else if (!is_ctl(input)) {
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
        } else if (!response.headers.empty() && (input == ' ' || input == '\t')) {
          state_ = state.header_lws;
          return ParseState.indeterminate;
        } else if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          //TODO
          response.headers.newHeader();
          response.headers.tick(lastByteRead);
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
          response.headers.newValue();
          response.headers.tick(lastByteRead);
          return ParseState.indeterminate;
        }
      case header_name:
        if (input == ':') {
          state_ = state.space_before_header_value;
          return ParseState.indeterminate;
        } else if (!is_char(input) || is_ctl(input) || is_tspecial(input)) {
          return ParseState.bad;
        } else {
          response.headers.tick(lastByteRead);
          return ParseState.indeterminate;
        }
      case space_before_header_value:
        if (input == ' ') {
          state_ = state.header_value;
          response.headers.newValue();
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
          response.headers.tick(lastByteRead);
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
        response.headers.done();
        return ParseState.fromBoolean(input == '\n');
      default:
        return ParseState.bad;
      }
    }

}

