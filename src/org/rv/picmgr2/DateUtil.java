package org.rv.picmgr2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

  private static String DATE_FORMAT = "yyyy:MM:dd HH:mm:ss";

  private static SimpleDateFormat dateFormater = new SimpleDateFormat(DATE_FORMAT);
 

  public static Date toDate(String stringDate) throws ParseException {
    return dateFormater.parse(stringDate);
  }
}
