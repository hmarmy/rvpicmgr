package org.rv.picmgr2;

import java.nio.file.Path;
import java.util.Date;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo;
import org.json.JSONException;

public class PhotoInfo implements Comparable<PhotoInfo> {

  final Path sourcePath;
  final boolean existInDst;

  Date takenOn;
  GPSInfo gpsInfo;
  PhotoInfo master;
  JsonInfo jsonInfo;


  public PhotoInfo(Path path, boolean existInDst) {
    sourcePath = path;
    this.existInDst = existInDst;
  }

  String newName = null;
  public String buildNewNameWithGps() throws ImageReadException, JSONException {
    if (newName==null) {
        JsonInfo jsonInfoTmp = getJsonInfo();
        if (jsonInfoTmp!=null && !StringUtil.isBlank(jsonInfoTmp.getCountry())) {
          if (StringUtil.isBlank(jsonInfoTmp.getCity())) {
            newName = jsonInfoTmp.getCountry()+"-"+getSourcePath().getFileName();
          } else {
            newName = jsonInfoTmp.getCountry()+"-"+jsonInfoTmp.getCity()+"-"+getSourcePath().getFileName();
          }
        }
        newName = StringUtil.safeChar(StringUtil.stripAccents(newName));
      } else { 
        newName = getSourcePath().getFileName().toString();
    }
    System.out.println(newName);
    return newName;
  }
  
  @Override
  public int compareTo(PhotoInfo o) {
    Date t1 = retrieveTakenOn();
    Date t2 = o.retrieveTakenOn();
    return t1.compareTo(t2);
  }

  public Date retrieveTakenOn() {
    if (takenOn!=null) return takenOn;
    return new Date(this.sourcePath.toFile().lastModified());
  }

  public void setTakenOn(Date takenOn) {
    this.takenOn = takenOn;
  }

  public Path getSourcePath() {
    return sourcePath;
  }

  public boolean isExistInDst() {
    return existInDst;
  }

  public GPSInfo getGpsInfo() {
    return gpsInfo;
  }

  public void setGpsInfo(GPSInfo gpsInfo) {
    this.gpsInfo = gpsInfo;
  }

  public boolean hasGpsInfo() {
    return gpsInfo != null;
  }

  public void setMaster(PhotoInfo master) {
    this.master = master;
    if (master!=null)
    this.gpsInfo = master.getGpsInfo();
  }

  public PhotoInfo getMaster() {
    return master;
  }
  
  public boolean hasMaster() {
    return master != null;
  }
  
  public JsonInfo getJsonInfo() throws ImageReadException, JSONException {
    if (!hasGpsInfo()) {
      return null;
    }
    if (jsonInfo==null) {
      jsonInfo = JsonUtil.retrieve(getGpsInfo());
    }
    return jsonInfo;
  }

  @Override
  public String toString() {
    return sourcePath.getFileName().toString();
  }
  
}
