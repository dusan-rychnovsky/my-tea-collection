package cz.dusanrychnovsky.myteacollection.util;

import java.io.File;

public class ClassLoaderUtils {

  public static File toFile(String dirPath) {
    var classLoader = ClassLoaderUtils.class.getClassLoader();
    var resourceUrl = classLoader.getResource(dirPath);
    return new File(resourceUrl.getFile());
  }
}
