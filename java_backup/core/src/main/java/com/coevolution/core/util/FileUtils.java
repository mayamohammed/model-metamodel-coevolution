package com.coevolution.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for file operations.
 */
public class FileUtils {

    public static List<File> listFiles(String dirPath,
                                        String extension) {
        List<File> result = new ArrayList<>();
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory())
            return result;
        File[] files = dir.listFiles(
            (d, name) -> name.endsWith("." + extension)
        );
        if (files != null)
            for (File f : files) result.add(f);
        return result;
    }

    public static boolean exists(String filePath,
                                   String extension) {
        File file = new File(filePath);
        return file.exists()
            && file.isFile()
            && filePath.endsWith("." + extension);
    }

    public static void ensureDir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println(
                "[FileUtils] Created : " + dirPath
            );
        }
    }

    public static String getBaseName(String filePath) {
        File file = new File(filePath);
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}