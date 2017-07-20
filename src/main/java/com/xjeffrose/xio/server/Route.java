package com.xjeffrose.xio.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Route {

  private static final Pattern keywordPattern = Pattern.compile("(:\\w+|:\\*\\w+)");
  private final String path;
  private final Pattern pathPattern;
  private final List<String> keywords;

  private Route(String path, Pattern pathPattern, List<String> keywords) {
    this.path = path;
    this.pathPattern = pathPattern;
    this.keywords = keywords;
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

            if (keyword.indexOf("*") == 0) {
              keyword = keyword.substring(1);
              regexPattern
                .append("(?<")
                .append(keyword)
                .append(">.*)");
            } else {
              regexPattern
                .append("(?<")
                .append(keyword)
                .append(">[^/]*)");
            }
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
    List<String> keywords = new ArrayList<>();
    return new Route(pattern, compile(pattern, keywords), keywords);
  }

  public Pattern pathPattern() {
    return pathPattern;
  }

  public boolean matches(String path) {
    return pathPattern.matcher(path).matches();
  }

  public Map<String, String> groups(String path) {
    Matcher matcher = pathPattern.matcher(path);
    Map<String, String> groups = new HashMap<>();
    if (matcher.matches()) {
      for (String keyword : keywords) {
        groups.put(keyword, matcher.group(keyword));
      }
    }
    return groups;
  }

  // Let's just assume that if two Route objects have been built
  // from the same path that they will have the same pattern and
  // keywords.

  @Override
  public boolean equals(final Object o) {
    if (o == this) return true;
    if (!(o instanceof Route)) return false;
    final Route other = (Route) o;
    return other.path.equals(path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }
}
