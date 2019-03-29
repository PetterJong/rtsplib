package com.hibox.rtsplib.utils;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class FileUtils {


    //  <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
    // <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    //  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    private static String tag = "FileUtils";
    public static String ROOT_PATH  = FileUtils.getSDCardFile().getAbsolutePath() + File.separator + "hibox" + File.separator; // 根路径

    /**
     * 获取SDCard状态
     *
     * @return boolean
     */
    public static boolean getSDCardState() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }

    /**
     * 获取SDCard的抽象路径
     *
     * @return File
     */
    public static File getSDCardFile() {
        if (getSDCardState()) {
            return Environment.getExternalStorageDirectory();
        }
        return null;
    }

    /**
     * 过得SDCard的总空间大小
     *
     * @return long 单位 M
     */
    @SuppressWarnings("deprecation")
    public static long getSDCardSize() {
        if (getSDCardFile() != null) {
            StatFs fs = new StatFs(getSDCardFile().getAbsolutePath());
            long blockSize = fs.getBlockSize();
            long blockCount = fs.getBlockCount();
            return blockSize * blockCount / 1024 / 1024;
        }
        return 0;
    }

    /**
     * 得到SDCard剩余空间大小
     *
     * @return long 单位M
     */
    @SuppressWarnings("deprecation")
    public static long getSDCardFreeSize() {
        if (getSDCardFile() != null) {
            StatFs fs = new StatFs(getSDCardFile().getAbsolutePath());
            long blockSize = fs.getBlockSize();
            long blockCount = fs.getAvailableBlocks(); // 返回文件系统上剩余的所有块
//            long blockSize = fs.getBlockSize();
//            long blockCount = fs.getFreeBlocks(); //  返回文件系统上剩余的所有块 包括预留的一般程序无法访问的
            return blockSize * blockCount / 1024 / 1024;
        }
        return 0;
    }


    /**
     * 将二进制文件写入到制定的文件夹中
     *
     * @param parentPath
     * @param fileName
     * @param content
     * @return boolean
     */
    public static boolean writeResoursToSDCard(String parentPath, String fileName,
                                               byte[] content) {
        File parentFile = new File(parentPath);

        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }

        String path = parentPath + "/" + fileName;
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(path, true);

            fo.write(content, 0, content.length);
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d(tag, "文件写入失败");
        } finally {
            try {
                if (fo != null) {
                    fo.close();
                    fo = null;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }

    public static byte[] getResoursFromSDCard(String dirpath) {
        if (getSDCardFile() == null)
            return null;
        File file = new File(dirpath);
        FileInputStream fi = null;
        ByteArrayOutputStream baos = null;
        try {
            fi = new FileInputStream(file);
            baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buf = new byte[1024];
            while ((len = fi.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toByteArray();
        }  catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Log.e(tag,dirpath + " 文件未找到！" );
                }
                if (fi != null) {
                    try {
                        fi.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }



    public static boolean reName(String path, String newPath) {
        File file = new File(path);
        boolean result = file.renameTo(new File(newPath));
        return result;
    }

}
