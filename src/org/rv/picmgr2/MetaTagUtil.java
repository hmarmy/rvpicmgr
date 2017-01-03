package org.rv.picmgr2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

public class MetaTagUtil {

  public static boolean extractMetaTags(PhotoInfo photo, boolean processGps) throws IOException,
      ParseException, ImageReadException {

    IImageMetadata metadata;
    try {
      metadata = Imaging.getMetadata(photo.getSourcePath().toFile());
    } catch (ImageReadException e) {
      RvLogger.info("SKIP [photo:" + photo.getSourcePath() + "]ImageReadException:"+e.getMessage());
      return false;
    }
    
    
    if (metadata == null) {
      RvLogger.info("SKIP [photo:" + photo.getSourcePath() + "]Metadata is null");
      return false;
    }

    if (!(metadata instanceof JpegImageMetadata)) {
      RvLogger.info("[photo:" + photo.getSourcePath() + "]Metadata not instance of JpegImageMetadata " + metadata);
      return false;
    }

    JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
    TiffImageMetadata exifMetadata = jpegMetadata.getExif();

    if (processGps && null != exifMetadata) {
      TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
      photo.setGpsInfo(gpsInfo);
    }

    String date = extractTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
    if (date == null) {
      RvLogger.info("No date takenOn found for " + photo.getSourcePath() + " -> SKIP");
      return false;
    } else {
      Date dateTaken = DateUtil.toDate(date);
      photo.setTakenOn(dateTaken);
      return true;
    }
  }

  private static String extractTagValue(JpegImageMetadata jpegMetadata, TagInfo tagInfo) throws ImageReadException {
    TiffField field = jpegMetadata.findEXIFValueWithExactMatch(tagInfo);
    if (field == null)
      return null;
    return "" + field.getValue();
  }

  static long aWeek = 7 * 24 * 60 * 60 * 1000;
  public static PhotoInfo findCloserDateWithGpsInfo(int startIndex, List<PhotoInfo> allImages) {
    PhotoInfo before = null, after = null;
    if (startIndex == 0) {
      before = null;
    } else {
      for (int k = startIndex - 1; k >= 0; k--) {
        if (allImages.get(k).hasGpsInfo()) {
          before = allImages.get(k);
          break;
        }
      }
    }
    if (startIndex == allImages.size() - 1) {
      after = null;
    } else {
      for (int k = startIndex + 1; k < allImages.size(); k++) {
        if (allImages.get(k).hasGpsInfo()) {
          after = allImages.get(k);
          break;
        }
      }
    }
    
    Date c = allImages.get(startIndex).takenOn;
    if (c==null) {return null;}
    long current = c.getTime();
    long delta1 = (before==null) ? Long.MAX_VALUE : current - before.takenOn.getTime();
    long delta2 = (after==null) ? Long.MAX_VALUE : after.takenOn.getTime() - current;
      
    if (Math.min(delta1, delta2) > aWeek) {
      return null;
    }
    return delta1 < delta2 ? before : after;
  }

  public static void copyAndSetExifTag(PhotoInfo photoSrc, File dst) throws ImageReadException, ImageWriteException, IOException {
     TiffOutputSet outputSet = new TiffOutputSet();

    IImageMetadata metadata = Imaging.getMetadata(photoSrc.getSourcePath().toFile());
    JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
    if (null != jpegMetadata) {
      // note that exif might be null if no Exif metadata is found.
      TiffImageMetadata exif = jpegMetadata.getExif();
      if (null != exif) {
        outputSet = exif.getOutputSet();
      }
    }
    double longitude = photoSrc.getGpsInfo().getLongitudeAsDegreesEast();
    double latitude = photoSrc.getGpsInfo().getLatitudeAsDegreesNorth();
    outputSet.setGPSInDegrees(longitude, latitude);
    OutputStream os = new FileOutputStream(dst);
    os = new BufferedOutputStream(os);
    new ExifRewriter().updateExifMetadataLossless(photoSrc.getSourcePath().toFile(), os, outputSet);
    os.close();
    os = null;
  }

}
