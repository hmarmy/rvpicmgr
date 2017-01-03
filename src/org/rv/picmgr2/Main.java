package org.rv.picmgr2;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.json.JSONException;

public final class Main {

  public static void main(String[] args) throws IOException, ImageReadException, ParseException, ImageWriteException, JSONException {

    List<String> params = new ArrayList<>(Arrays.asList(args));

    if (params.size()<2) {
      System.out.println(" Usage : [-gps] src dest");

      return;
    }

    boolean photoMode = true;
    String _extension = "jpg";
    String toRem = null;
    for (String param : params) {
      if (param.startsWith("-ext:")) {
        _extension = param.substring("-ext:".length());
        toRem = param;
        if (!param.toLowerCase().endsWith("jpg")) {
          photoMode = false;
        }
      }
    }
    if (toRem!=null) {
      params.remove(toRem);
    }
    
    final String extension = _extension;

    RvLogger.info("Process extension : "+extension);
    
    final boolean processGps = params.remove("-gps");
    if (processGps) {
      RvLogger.info("Process GPS info");
    } else {
      RvLogger.info("Do not process GPS info");
    }
    
    final boolean processLink = params.remove("-link");
    if (processLink) {
      RvLogger.info("Process Location link");
    } else {
      RvLogger.info("Do not process Location link");
    }

    String srcFolder = params.remove(0);
    Path srcPath = Paths.get(srcFolder);

    if (Files.notExists(srcPath, LinkOption.NOFOLLOW_LINKS)) {
      RvLogger.exit("SRC DIR does not exist : " + srcFolder);
      return;
    }

    String dstFolder = params.remove(0);
    Path dstPath = Paths.get(dstFolder);
    if (Files.notExists(srcPath, LinkOption.NOFOLLOW_LINKS)) {
      RvLogger.exit("DST DIR does not exist : " + dstFolder);
      return;
    }

    final StringBuilder existingImages = FileUtil.existingFiles(dstPath,extension);
    RvLogger.info("existingImages size (chars) :" + existingImages.length());

    final List<PhotoInfo> allImages = new ArrayList<>();

    final AtomicInteger newFiles = new AtomicInteger(0); 
    Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        if (extension==null || "*".equals(extension) 
            || path.toString().toLowerCase().endsWith("."+extension.toLowerCase())) {
        boolean exist = existingImages.indexOf(path.getFileName().toString().toLowerCase() + "#") >= 0;
        allImages.add(new PhotoInfo(path, exist));
        if (!exist) {
          newFiles.incrementAndGet();
        }
        }
        return FileVisitResult.CONTINUE;
      }
  });

    if (newFiles.get() == 0) {
      RvLogger.exit("No new file to process");
      return;
    }

    RvLogger.info(newFiles + " to process");
    
    if(photoMode) {
     for (PhotoInfo info : allImages) {
      boolean ok = MetaTagUtil.extractMetaTags(info,processGps);
      if (!ok) {
        RvLogger.info(info.getSourcePath() + " can't extract metadata");
      }
     }
    }
 
    if (processGps) { 
        Collections.sort(allImages);
    
     for (int i = 0; i < allImages.size(); i++) {
      PhotoInfo currentPhoto = allImages.get(i);
      
        if (!currentPhoto.isExistInDst()) {
          if (!currentPhoto.hasGpsInfo()) {
            currentPhoto.setMaster(MetaTagUtil.findCloserDateWithGpsInfo(i, allImages));
            }
        }
      }
    }
    
    FileUtil.copy(allImages, dstFolder, processLink);
    RvLogger.info(newFiles + " processed succesfully");
  }
}
          
