package com.xjeffrose.xio.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.*;

/**
 * This class is used to generate a List<DynamicRouteConfig> based in a JSON string input matching
 * the format described in /test/resources/route_parameters.json
 */
public class DynamicRouteConfigsFactory {

  /**
   * This is a factory method used to invoke the build operation given a JSON input string The
   * output of this file is the raw material used to build ProxyRoutes dynamically from JSON
   */
  public static ImmutableList<DynamicRouteConfig> build(String string) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    List<DynamicRouteEntry> dynamicRouteEntries =
        objectMapper.readValue(string, new TypeReference<List<DynamicRouteEntry>>() {});
    return ImmutableList.copyOf(createDynamicRouteConfigs(dynamicRouteEntries));
  }

  private static List<DynamicRouteConfig> createDynamicRouteConfigs(
      List<DynamicRouteEntry> dynamicRouteEntries) {
    ArrayList<DynamicRouteConfig> dynamicRouteConfigs = new ArrayList<>();
    // This is used to group together route configs that share the same path
    Map<String, ArrayList<DynamicRouteEntry>> groupedRouteEntries =
        groupRouteEntriesByPath(dynamicRouteEntries);
    // Now we will squash the various versions of each route together
    for (ArrayList<DynamicRouteEntry> routeEntries : groupedRouteEntries.values()) {
      if (!routeEntries.isEmpty()) {
        // We will use the first entries name/path as the base template, this is not really an issue for paths since these are
        // grouped by path anyway
        DynamicRouteEntry firstElement = routeEntries.get(0);
        String path = firstElement.getPath();

        // combine the generated clientConfigs from different instances of this route path
        ArrayList<DynamicClientConfig> dynamicClientConfigs = new ArrayList<>();
        for (DynamicRouteEntry routeEntry : routeEntries) {
          List<DynamicClientConfig> generatedClientConfigList =
              createDynamicClientConfigs(routeEntry);
          dynamicClientConfigs.addAll(generatedClientConfigList);
        }
        dynamicRouteConfigs.add(new DynamicRouteConfig(path, dynamicClientConfigs));
      }
    }
    // lets sort these from shortest to longest and in alphabetical order
    Collections.sort(dynamicRouteConfigs);
    return dynamicRouteConfigs;
  }

  private static Map<String, ArrayList<DynamicRouteEntry>> groupRouteEntriesByPath(
      List<DynamicRouteEntry> dynamicRouteEntries) {
    Map<String, ArrayList<DynamicRouteEntry>> consolidationMap = new HashMap<>();
    for (DynamicRouteEntry entry : dynamicRouteEntries) {
      if (consolidationMap.containsKey(entry.getPath())) {
        ArrayList<DynamicRouteEntry> existingEntry = consolidationMap.get(entry.getPath());
        existingEntry.add(entry);
        consolidationMap.put(entry.getPath(), existingEntry);
      } else {
        ArrayList<DynamicRouteEntry> listOfEntries = new ArrayList<>();
        listOfEntries.add(entry);
        consolidationMap.put(entry.getPath(), listOfEntries);
      }
    }
    return consolidationMap;
  }

  private static List<DynamicClientConfig> createDynamicClientConfigs(
      DynamicRouteEntry dynamicRouteEntry) {
    ArrayList<DynamicClientConfig> dynamicClientConfigs = new ArrayList<>();
    for (String clientIp : dynamicRouteEntry.getClientsIps()) {
      dynamicClientConfigs.add(
          new DynamicClientConfig(
              clientIp, dynamicRouteEntry.getPort(), dynamicRouteEntry.isTlsEnabled()));
    }
    return dynamicClientConfigs;
  }
}
