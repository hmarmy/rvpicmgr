package org.rv.picmgr2;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtil {

  private static JsonInfo extractJsonInfo(String json) throws JSONException {

    if (json == null)
      return null;

    JsonInfo info = new JsonInfo();

    JSONObject root = new JSONObject(json);
    System.out.println(root.getString("status"));
    JSONArray results = root.getJSONArray("results");

    for (int i = 0; i < results.length(); i++) {
      JSONObject result = results.getJSONObject(i);
      JSONArray address_components = result.getJSONArray("address_components");

      for (int j = 0; j < address_components.length(); j++) {
        JSONObject address_component = address_components.getJSONObject(j);
        String types = address_component.getJSONArray("types").toString();

        if (info.getCity() == null && types.contains("\"locality\"")) {
          info.setCity(address_component.getString("short_name"));
          if (info.isComplete()) {
            return info;
          }
        } else if (info.getCountry() == null && types.contains("\"country\"")) {
          info.setCountry(address_component.getString("short_name"));
          if (info.isComplete()) {
            return info;
          }
        }
      }
    }
    return info;
  }

  
  private static HashMap<String,JsonInfo> cache = new HashMap<>();
  private static int counter = 0;
  
  public static JsonInfo retrieve(GPSInfo info) throws ImageReadException,JSONException {
    // System.setProperty("http.proxyHost", "web-gw1.csintra.net");
    // System.setProperty("http.proxyPort", "8080");

    double lat = info.getLatitudeAsDegreesNorth();
    double lng = info.getLongitudeAsDegreesEast();

    String key = lat+ "_" +lng;
    
    if (cache.containsKey(key))  {
      JsonInfo fromCache = cache.get(key);
      RvLogger.info("Cache Hit for "+key+ " cache size:" + cache.size());
      return fromCache;
    }
    counter ++;
    String theURL = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lng + "&sensor=true";
    URL url;
    try {
      url = new URL(theURL);
    } catch (MalformedURLException e) {
      RvLogger.warn(theURL, e);
      return null;
    }
    InputStream is;
    try {
      is = url.openStream();

      int ptr = 0;
      StringBuilder buffer = new StringBuilder();

      while ((ptr = is.read()) != -1) {
        buffer.append((char) ptr);
      }

      String rep = buffer.toString();

      JsonInfo jsoninfo = extractJsonInfo(rep);

      cache.put(key, jsoninfo);
      RvLogger.info(counter + ") Fetched geocode : "+JsonInfo.safeToString(jsoninfo));
      return jsoninfo;

    } catch (IOException e) {
      RvLogger.warn("Error fetching " + theURL, e);
      return null;
    }
  }

}
