package org.rv.picmgr2;

public class RvLogger {

	public static void info(String msg) {
		    System.out.println("[INFO] " + msg);
		  }

	public static void warn(String msg, Exception e) {
		    System.out.println("[WARN] " + msg + "[" + e.getMessage() + "]");
		  }

  public static void exit(String msg) {
    System.out.println("[**EXIT**] " + msg);
    
  }
	
	
}
