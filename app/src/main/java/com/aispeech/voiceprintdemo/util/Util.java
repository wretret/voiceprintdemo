package com.aispeech.voiceprintdemo.util;
/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;


/**
 * Util for AISpeech android sdk.
 */
public class Util {

    public static final String UTF8 = "UTF-8";

    static final int BUFF_SIZE = 10240;

    /**
     * 获取应用程序可以用的存储目录空间，以requireSizeInByte字节大小为最低存储大小要求限制
     * 优先选择内部存储空间，若内部存储空间不足则使用外部存储空间，若都不满足，则返回null
     *
     * @param ctx
     * @param relativePath
     * @param requireSizeInByte
     * @return File
     *         <ul>
     *         <li>内部： /data/data/{包名}/files/releativePath</li>
     *         <li>外部： {外部存储}/Android/data/files/releativePath</li>
     *         </ul>
     */
    public static File getAvaiableAppDataDirPerInternal(Context ctx, String relativePath,
                                                        long requireSizeInByte) {
        if (ctx == null) {
            return null;
        }
        File path = null;
        if (getAvailableInternalMemorySize() >= requireSizeInByte) {
            path = new File(ctx.getFilesDir(), relativePath);
        } else if (getAvailableExternalMemorySize() >= requireSizeInByte) {
            path = new File(getAvaiableExternalDataDir(ctx), relativePath);
        }
        return path;
    }

    /**
     * 获取应用程序可以用的存储目录空间，以10MB大小为最低存储大小要求限制
     * 优先选择内部存储空间，若内部存储空间不足则使用外部存储空间，若都不满足，则返回null
     *
     * @param ctx
     * @param relativePath
     * @return File
     *         <ul>
     *         <li>内部： /data/data/{包名}/files/releativePath</li>
     *         <li>外部： {外部存储}/Android/data/files/releativePath</li>
     *         </ul>
     */
    public static File getAvaiableAppDataDirPerInternal(Context ctx, String relativePath) {
        return getAvaiableAppDataDirPerInternal(ctx, relativePath, 1024 * 1024 * 10);
    }

    /**
     * 获取可用的外部存储数据目录
     *
     * @param ctx
     * @return
     */
    @SuppressLint("NewApi")
    public static File getAvaiableExternalDataDir(Context ctx) {
        if (ctx == null) {
            return null;
        }
        File path = null;
        if (externalMemoryAvailable()) {
            path = ctx.getExternalFilesDir(null);
        }
        return path;
    }

    /**
     * 获取可用的内部存储空间大小(单位:byte)
     *
     * @return
     */
    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    /**
     * 获取内部存储空间大小(单位:byte)
     *
     * @return
     */
    public static long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    /**
     * 获取外部存储空间是否可用
     *
     * @return true 可用
     */
    public static boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取外部可用的存储空间大小(单位:byte)
     *
     * @return 大小，-1表示外部存储空间不可用
     */
    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize;
        } else {
            return -1;
        }
    }

    /**
     * 获取外部存储空间的大小(单位:byte)
     *
     * @return 大小，-1表示外部存储不可用
     */
    public static long getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return totalBlocks * blockSize;
        } else {
            return -1;
        }
    }

    /**
     * Get the external app cache directory.
     *
     * @param context
     *            The context to use
     * @return The external cache dir
     */
    @TargetApi(8)
    public static File getExternalCacheDir(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return context.getExternalCacheDir();
            }
        }

        return null;
    }

    /**
     * Get the external app cache directory.
     *
     * @param context
     *            The context to use
     * @param name
     *            the dir name
     * @return The external cache dir
     */
    @TargetApi(8)
    public static File getExternalCacheDir(Context context, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File cacheDir = context.getExternalCacheDir();
                if (cacheDir != null) {
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs();
                    }
                    if (TextUtils.isEmpty(name)) {
                        return context.getExternalCacheDir();
                    } else {
                        File dir = new File(cacheDir, name);
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        return dir;
                    }
                }
            }
        }

        return null;
    }

    /**
     * 判断是否是Android 2.2以下系统
     *
     * @return true表示Android2.2及以上系统，false表示Android2.2以下系统;
     */
    public static boolean isAboveAndroid22() {
        int sdkInt = Build.VERSION.SDK_INT;

        if (sdkInt <= 7) {
            return false;
        }

        return true;
    }

    /**
     * 获取资源目录
     *
     * @param context
     * @return
     */
    public static String getResourceDir(Context context) {
        if (context == null) {
            return null;
        }
        String dirPath = null;
        File file = context.getFilesDir();
        if(file == null) {
            file = new File("/data/data/" + context.getPackageName() + "/files");
        }
        dirPath = file.getAbsolutePath();
        return dirPath;
    }

    /**
     * 从assets目录中拷贝资源到资源目录，如果是zip文件则解压
     *
     * @param context
     *            Android环境句柄
     * @param resName
     *            资源名
     * @return true 执行成功
     */
    public static int copyResource(final Context context, String resName, String resMd5sumName) {
        return copyResource(context, resName, true, resMd5sumName);
    }

    /**
     * 从assets目录中拷贝资源到资源目录，如果是zip文件则解压
     *
     * @param context
     *            Android环境句柄
     * @param resName
     *            资源名
     * @return true 执行成功
     */
    public static int copyResource(final Context context, String resName) {
        return copyResource(context, resName, true, null);
    }

    /**
     * 从assets目录中拷贝资源文件到/data/data/$pkgname/files目录下，如果是zip文件则解压
     *
     * @param context
     *            Android环境句柄
     * @param resName
     *            资源名
     * @param isMD5
     *            是否进行MD5校验,如果校验和相同则忽略拷贝和解压
     * @return -1 拷贝失败; 0 MD5相同,略过拷贝; 1 拷贝成功
     */
    public static synchronized int copyResource(Context context, String resName, boolean isMD5, String resMd5sumName) {
        if (context == null) {
            return -1;
        }
        InputStream is;
        try {
            is = context.getAssets().open(resName);
        } catch (IOException e) {
            Log.e("speech", "file " + resName
                    + " not found in assest floder, Did you forget add it?");
            return -1;
        }
        try {
            is.read(new byte[1]);
            is.reset();
        } catch (IOException e) {
            Log.e("speech",
                    "file"
                            + resName
                            + "should be one of the suffix below to avoid be compressed in assets floder."
                            + "“.jpg”, “.jpeg”, “.png”, “.gif”, “.wav”, “.mp2″, “.mp3″, “.ogg”, “.aac”, “.mpg”, “.mpeg”, “.mid”, “.midi”, “.smf”, “.jet”, “.rtttl”, “.imy”, “.xmf”, “.mp4″, “.m4a”, “.m4v”, “.3gp”, “.3gpp”, “.3g2″, “.3gpp2″, “.amr”, “.awb”, “.wma”, “.wmv”");
            return -1;
        }

//        File destFile = new File(Util.getResourceDir(context), resName);
        File destFile = new File("/sdcard/voiceprint/", resName);
        if(destFile.exists()) {
            return 0;
        }
        if(resMd5sumName == null) {//如果没有对应资源的md5文件
            Log.i("speech", "there is no md5 file of : " + resName);
            // if file exists, verify MD5 code
            if (isMD5 && checkMD5(is, destFile)) {
                Log.i("speech", "md5 is same : " + resName);
                try {
                    is.close();
                    return 0; // MD5 same do nothing
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
            } else {
                saveDestFile(context, is, resName);
                if (isZipFile(context.getFileStreamPath(resName))) {
                    // unzip
                    unZip(context, destFile);
                }
                return 1;
            }
        } else { //如果有对应资源的md5文件
            Log.i("speech", "there is md5 file of : " + resName);
            InputStream isMd5sumIs;
            try {
                isMd5sumIs = context.getAssets().open(resMd5sumName);
            } catch (IOException e) {
                Log.e("speech", "file " + resMd5sumName
                        + " not found in assest floder, Did you forget add it?");
                e.printStackTrace();
                return -1;
            }
            File dstMd5sumFile = new File(Util.getResourceDir(context), resMd5sumName);
            if(isMD5 && checkMD5(isMd5sumIs, dstMd5sumFile)) {
                Log.i("speech", " md5 file in assets and data drectory is same : " + resName);
                try {
                    isMd5sumIs.close();
                    is.close();
                    return 0; // MD5 same do nothing
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
            } else {
                Log.i("speech", " md5 file in assets and data drectory is not same : " + resName);
                saveDestFile(context, is, resName);
                saveDestFile(context, isMd5sumIs, resMd5sumName);
                if (isZipFile(context.getFileStreamPath(resName))) {
                    // unzip
                    unZip(context, destFile);
                }
                return 1;
            }
        }
    }

    public static void saveDestFile(Context context, InputStream is, String resName) {
        Log.i("speech", "save to data : " + resName);
        try {
            is.reset();
//            FileOutputStream fos = context.openFileOutput(resName, Context.MODE_PRIVATE);
            FileOutputStream fos = new FileOutputStream("/sdcard/voiceprint/" + resName);
            byte[] data = new byte[BUFF_SIZE];
            int len = 0;
            while ((len = is.read(data)) != -1) {
                fos.write(data, 0, len);
            }
            fos.close();
            is.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] MAGIC = { 'P', 'K', 0x3, 0x4 };

    /**
     * 检测是否是zip文件
     *
     * @param f
     * @return
     */
    public static boolean isZipFile(File f) {
        boolean isZip = true;
        byte[] buffer = new byte[MAGIC.length];
        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            raf.readFully(buffer);
            for (int i = 0; i < MAGIC.length; i++) {
                if (buffer[i] != MAGIC[i]) {
                    isZip = false;
                    break;
                }
            }
            raf.close();
        } catch (Throwable e) {
            isZip = false;
        }
        return isZip;
    }

    /**
     * 比较输入流和文件的MD5码是否相同，内部未关闭输入流
     *
     * @param is
     * @param file
     * @return true MD5校验通过， false 文件不存在或MD5校验未通过
     */
    private static boolean checkMD5(final InputStream is, File file) {
        if (file.exists()) {
            try {
                FileInputStream destFis = new FileInputStream(file);
                byte[] md5_1 = getFileMD5String(destFis);
                byte[] md5_2 = getFileMD5String(is);
                boolean same = true;
                int minLength = (md5_1.length > md5_2.length) ? md5_2.length : md5_1.length;
                for (int k = 0; k < minLength; k++) {
                    if (md5_1[k] != md5_2[k]) {
                        same = false;
                        break;
                    }
                }

                destFis.close();

                return same;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void unZip(final Context context, File zipfileName) {
        byte data[] = new byte[BUFF_SIZE];
        ZipFile zipFile;
        try {
            zipFile = new ZipFile((zipfileName));
            Enumeration<? extends ZipEntry> emu = zipFile.entries();
            while (emu.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) emu.nextElement();
                if (entry.isDirectory()) {
                    new File(Util.getResourceDir(context), entry.getName()).mkdirs();
                    continue;
                }
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                Log.d("unzip", entry.getName());
                OutputStream outputStream = new FileOutputStream(new File(getResourceDir(context),
                        entry.getName()));
                BufferedOutputStream bos = new BufferedOutputStream(outputStream, BUFF_SIZE);
                int readSize;
                while ((readSize = bis.read(data, 0, BUFF_SIZE)) != -1) {
                    bos.write(data, 0, readSize);
                }
                bos.flush();
                bos.close();
                bis.close();
            }
            zipFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * get File MD5 codes string
     *
     * @param in
     *            inputstream
     * @return
     */
    private static byte[] getFileMD5String(InputStream in) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[BUFF_SIZE];
            int length = -1;
            while ((length = in.read(buffer)) != -1) {
                messagedigest.update(buffer, 0, length);
            }
            return messagedigest.digest();

        } catch (NoSuchAlgorithmException nsaex) {
            nsaex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * 生成32位的uuid字符串
     *
     * @return uuid字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 字节数组转换为utf8字符串
     *
     * @param bytes
     *            字节数组
     * @return utf8字符串
     */
    public static String newUTF8String(byte[] bytes) {
        try {
            return new String(bytes, UTF8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new String(bytes);
        }
    }

    /**
     * 获取UTF-8编码的字符数组
     *
     * @param str
     *            UTF8字符串
     * @return 出错时返回null
     */
    public static byte[] getUTF8Bytes(String str) {
        try {
            return str.getBytes(UTF8);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 获取IMEI
     * @param context
     * @return
     */
    public static String getIMEI(final Context context){
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return (tm.getDeviceId() != null) ? tm.getDeviceId() : "";
    }

    public static String uniqueID = null;



    /**
     * 生成16位deviceId
     *
     * @param context
     * @return
     */
    public static String generateDeviceId16(final Context context) {
        String did;
        String androidId, serial = null, imei = null;
        androidId = android.provider.Settings.Secure.getString(context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        if (Build.VERSION.SDK_INT > 9) {
            serial = Build.SERIAL;
        }
        try {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            imei = tm.getSimSerialNumber();
        } catch (Exception e) {
            Log.e("",
                    "Did you forget add android.permission.READ_PHONE_STATE permission in your application? Add it now to fix this bug!");
        }

        if (!TextUtils.isEmpty(androidId) && !TextUtils.equals(androidId, "9774d56d682e549c")) {
            did = androidId;
        } else if (!TextUtils.isEmpty(imei)) {
            did = imei;
        } else if (!TextUtils.isEmpty(serial)) {
            did = serial;
        } else {
            did = "";
        }

        if (did.length() < 8) {
            did = "";
        }

        return did.toLowerCase();
    }


    /**
     * 获取显示屏分辨率信息
     *
     * @return
     */
    public static String getDisplayInfo(final Context context) {
        if (context == null) {
            return null;
        }
        int height = context.getResources().getDisplayMetrics().heightPixels;
        int width = context.getResources().getDisplayMetrics().widthPixels;
        return width + "x" + height;
    }

    /**
     * 显示当前执行的线程信息
     */
    public static final void logThread(String tag) {
        Thread t = Thread.currentThread();
        Log.d(tag, "<" + t.getName() + ">id: " + t.getId() + ", Priority: " + t.getPriority()
                + ", Group: " + t.getThreadGroup().getName());
    }




    private static void removeFile(String filenPath) {
        File file = new File(filenPath);
        if (file.exists()) {
            file.delete();
        }
    }


    /**
     * 运行时刻检测是否被授予了指定的权限
     *
     * @param ctx
     *            android上下文环境
     * @param permission
     *            对应的权限名
     * @return true 具有指定的权限， false 未被授予或ctx为null
     */
    public static boolean checkPermissionAtRunTime(Context ctx, String permission) {
        if (ctx == null) {
            return false;
        }
        int res = ctx.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * 判断当前线程是不是单元测试线程
     *
     * @return
     */
    public static boolean isUnitTesting() {
        return Thread.currentThread().getName().contains("android.test.InstrumentationTestRunner");
    }

    /**
     * 将一个Runnable的任务在主线程或测试线程中执行
     *
     * @param context
     *            android上下文环境
     * @param r
     *            Runnable任务
     */
    public static void executeRunnableInMainOrTestThread(Context context, Runnable r) {
        if (context != null) {
            HandlerThread thread = null;
            boolean isUT = isUnitTesting();
            if (isUT) {
                thread = new HandlerThread("TestHandlerThread");
                thread.start();
            }
            Handler handler = new Handler(isUT ? thread.getLooper() : context.getMainLooper());
            handler.post(r);
        }
    }

    /**
     * 获取时间串
     *
     * @return
     */
    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    /**
     * 返回当前Wifi是否连接上
     *
     * @param context
     * @return true 已连接
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    /**
     * 生成随机数字串
     * @param length 数字串长度
     * @return
     */
    public static long generateRandom(int length) {
        Random random = new Random();
        char[] digits = new char[length];
        digits[0] = (char) (random.nextInt(9) + '1');
        for (int i = 1; i < length; i++) {
            digits[i] = (char) (random.nextInt(10) + '0');
        }
        return Long.parseLong(new String(digits));
    }

}
