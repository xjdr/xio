package com.xjeffrose.xio.server;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RouteTest {

  @Test
  public void testWildcard() throws Exception {
    Route matches1 = Route.build(".*");

    assertTrue(matches1.matches("/api/people/jeff"));
  }

  @Test
  public void testPathPattern() throws Exception {

    String pathPattern1 = Route
        .build("/api/people/:person")
        .pathPattern().toString();

    assertEquals(pathPattern1, "/api/people/(?<person>[^/]*)[/]?");

    String pathPattern2 = Route
        .build("/api/people/:person/hands/:hand/slap")
        .pathPattern().toString();

    assertEquals(pathPattern2, "/api/people/(?<person>[^/]*)/hands/(?<hand>[^/]*)/slap[/]?");
  }

  @Test
  public void testMatches() throws Exception {
    Route matches1 = Route.build("/api/people/:person");

    assertTrue(matches1.matches("/api/people/jeff"));
  }

  @Test
  public void testMatchesAny() throws Exception {
    Route matches1 = Route.build(".*");

    assertTrue(matches1.matches("/api/people/jeff"));
  }

  @Test
  public void testGroups() throws Exception {

    Map<String, String> group1 = Route
        .build("/api/people/:person")
        .groups("/api/people/jeff");

    assertEquals(group1.get("person"), "jeff");


    Map<String, String> group2 = Route
        .build("/api/people/:person/hands/:hand/slap")
        .groups("/api/people/jeff/hands/left/slap");

    assertEquals(group2.get("person"), "jeff");
    assertEquals(group2.get("hand"), "left");
  }

  @Test
  public void testCompile() throws Exception {

  }

  @Test
  public void testBuild() throws Exception {

  }
}