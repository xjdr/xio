package com.xjeffrose.xio;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import com.xjeffrose.log.*;

class Route {
  private static final Logger log = Log.getLogger(Route.class.getName());

  private static final Pattern keywordPattern = Pattern.compile("(:\\w+)");
  private final Pattern pathPattern;
  private final List<String> keywords;

  private Route(Pattern path, List<String> keywords) {
    this.pathPattern = path;
    this.keywords = keywords;
  }

  public Pattern pathPattern() {
    return pathPattern;
  }

  public boolean matches(String path) {
    return pathPattern.matcher(path).matches();
  }

  public Map<String,String> groups(String path) {
    Matcher matcher = pathPattern.matcher(path);
    if (matcher.matches()) {
      Map<String,String> groups = new HashMap<String,String>();
      for (String keyword : keywords) {
        groups.put(keyword, matcher.group(keyword));
      }
      return groups;
    } else {
      return null;
    }
  }

  public static Pattern compile(String pattern, List<String> keywords) {
    StringBuilder regexPattern = new StringBuilder();

    if (pattern.equals("/")) {
      regexPattern.append("/");
    } else {
      final String[] segments = pattern.split("/");

      for (String segment : segments) {
        if (!segment.equals("")) {
          regexPattern.append("/");
          if (keywordPattern.matcher(segment).matches()) {
            String keyword = segment.substring(1);
            regexPattern.append("(?<" + keyword + ">[^/]*)");
            keywords.add(keyword);
          } else {
            regexPattern.append(segment);
          }
        }
      }
    }
    regexPattern.append("[/]?");

    return Pattern.compile(regexPattern.toString());
  }

  public static Route build(String pattern) {
    List<String> keywords = new ArrayList<String>();
    return new Route(compile(pattern, keywords), keywords);
  }
}
