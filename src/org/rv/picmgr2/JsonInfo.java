package org.rv.picmgr2;

public class JsonInfo {

  String country;
  String city;
  
  public String getCountry() {
    return country;
  }
  public void setCountry(String country) {
    this.country = country;
  }
  public String getCity() {
    return city;
  }
  public void setCity(String city) {
    this.city = city;
  }
  
  public boolean isComplete() {
    return city!=null && country!=null;
  }
  
  public static String safeToString(JsonInfo i) {
   if (i==null) return null;
   return "Country:" + i.getCountry()+ "  City:" + i.getCity();
  }

}
