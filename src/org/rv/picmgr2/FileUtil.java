package org.rv.picmgr2;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.json.JSONException;

public class FileUtil {

  public static void ensureFolderExist(File f) {
    boolean created = f.mkdirs();
    if (created) {
      RvLogger.info(f.getPath() + " was created");
    }
  }

  public static void copy(Collection<PhotoInfo> photos, String baseDst, boolean processLink) throws IOException, ImageReadException,
      ImageWriteException, JSONException {
    for (PhotoInfo photo : photos) {
      if (!photo.isExistInDst()) {

        String newFileName = photo.hasGpsInfo() ? photo.buildNewNameWithGps() : photo.getSourcePath().getFileName().toString();
        String folderDest = buildDestFolder(photo.retrieveTakenOn());

        Path dstFile = Paths.get(baseDst, folderDest, newFileName);
        ensureFolderExist(dstFile.getParent().toFile());

        int i = 1;
        while  (Files.exists(dstFile)) {
          dstFile = Paths.get(baseDst, folderDest, newFileName+"__"+i);
          RvLogger.info("File already imported, set new name " + dstFile);
          i++;
        }


        if (photo.hasMaster()) {
          MetaTagUtil.copyAndSetExifTag(photo, dstFile.toFile());
        } else {
          Files.copy(photo.getSourcePath(), dstFile);
        }
        if (processLink && photo.hasGpsInfo()) {
          createSymLink(photo, dstFile, baseDst);
        }
      }
    }
  }
  
  
  

  private static void createSymLink(PhotoInfo photo, Path dstFile, String baseDst) throws ImageReadException, JSONException {
    Path newLink = Paths.get(baseDst, photo.getJsonInfo().getCountry(),photo.getJsonInfo().getCity());//, dstFile.getFileName().toString());
    ensureFolderExist(newLink.getParent().toFile());
    
    System.out.println("newlink "+newLink.toString());
    System.out.println("dst "+ dstFile.toString());
    try {
        Files.createSymbolicLink(newLink, dstFile);
    } catch (IOException x) {x.printStackTrace();
        System.err.println(x);
    } catch (UnsupportedOperationException x) {
        // Some file systems do not support symbolic links.
      x.printStackTrace();
        System.err.println(x);
    }
    
  }

  public static StringBuilder existingFiles(Path dir, final String extension) throws IOException {
    final StringBuilder sb = new StringBuilder();
     final AtomicInteger count = new AtomicInteger(0);
    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (extension==null || "*".equals(extension) 
            || file.toString().toLowerCase().endsWith("."+extension.toLowerCase())) {
        sb.append(file.getFileName().toString().toLowerCase());
        sb.append('#');
        count.incrementAndGet(); }
        return FileVisitResult.CONTINUE;
      }
    });
    RvLogger.info(count.get() + " existing files found");
    return sb;
  }

  private static Calendar calendar = new GregorianCalendar();

  public static String buildDestFolder(Date takenOn) {
    calendar.setTime(takenOn);
    int month = calendar.get(Calendar.MONTH) + 1;
    String stringMonth = month > 9 ? "" + month : "0" + month;
    String stringYear = calendar.get(Calendar.YEAR) + "";
    return stringYear + File.separator + stringYear + "-" + stringMonth;
  }

}
