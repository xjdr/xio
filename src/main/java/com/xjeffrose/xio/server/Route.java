package com.xjeffrose.xio.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class Route {
  private static final Logger log = Logger.getLogger(Route.class);

  private static final Pattern keywordPattern = Pattern.compile("(:\\w+)");
  private final Pattern pathPattern;
  private final List<String> keywords;

  private Route(Pattern path, List<String> keywords) {
    this.pathPattern = path;
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
            regexPattern
                .append("(?<")
                .append(keyword)
                .append(">[^/]*)");
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
    return new Route(compile(pattern, keywords), keywords);
  }

  public Pattern pathPattern() {
    return pathPattern;
  }

  public boolean matches(String path) {
    return pathPattern.matcher(path).matches();
  }

  public Map<String, String> groups(String path) {
    Matcher matcher = pathPattern.matcher(path);
    if (matcher.matches()) {
      Map<String, String> groups = new HashMap<>();
      for (String keyword : keywords) {
        groups.put(keyword, matcher.group(keyword));
      }
      return groups;
    } else {
      return null;
    }
  }
}
