package br.org.funcate.terramobile.util;



import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by bogo on 17/06/15.
 */
public class Util {
    public static HashMap<String,Integer> getRandomColor()
    {
        Random rand = new Random();
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);
        HashMap<String, Integer> colorMap = new HashMap<String, Integer>();
        colorMap.put("r", r);
        colorMap.put("g", g);
        colorMap.put("b", b);
        return colorMap;

    }

    /**
     * Ckeck the internet connection
     * @param context
     * @return
     */
    public static boolean isConnected(Context context){
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static void copyFile(String fileOrigin, String fileDestination) {
        InputStream inStream;
        OutputStream outStream;
        try {
            File origin = new File(fileOrigin);
            File destination = new File(fileDestination);

            inStream = new FileInputStream(origin);
            outStream = new FileOutputStream(destination);

            byte[] buffer = new byte[1024];

            int length;
            while ((length = inStream.read(buffer)) > 0)
                outStream.write(buffer, 0, length);
            inStream.close();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void moveFile(String inputFile, String outputPath) {

        try {
            File dir = new File(outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(inputFile);
            String fileName = file.getName();
            outputPath += "/" + fileName;

            Util.copyFile(inputFile, outputPath);

            file.delete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the files list from app working directory.
     * @param directory, The reference to working directory.
     * @param extension, the default file extension to identify a GeoPackage database file.
     * @return The list of files filtered using the extension.
     */
    public static ArrayList<File> getGeoPackageFiles(File directory, final String extension) {
        if (directory==null || !directory.isDirectory()) return null;
        ArrayList<File> files=new ArrayList<File>();
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if(pathname.isDirectory() || pathname.isHidden()) return false;
                return pathname.getName().endsWith(extension);
            }
        };

        File[] theFiles = directory.listFiles(filter);
        for(File file : theFiles) files.add(file);
        return files;
    }

    public static File getGeoPackageByName(File directory, final String extension, String fileName){
        ArrayList<File> files = getGeoPackageFiles(directory, extension);
        for (File file : files)
            if(file.getName().equals(fileName)) return file;
        return null;
    }
    /** Get a directory on external storage (SD card etc), ensuring it exists
     *
     * @return a new File representing the chosen directory
     */
    public static File getDirectory(String directory) {
        if (directory==null) return null;
        String path = "";
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            path = Environment.getExternalStorageDirectory().toString();
        }else{
            path = Environment.getDataDirectory().getAbsolutePath();
        }
        File file = new File(path);
        if(file.canWrite()) {
            path += directory.startsWith("/") ? "" : "/";
            path += directory.endsWith("/") ? directory : directory + "/";
            file = new File(path);
            if (file.mkdirs() && file.isDirectory())
                return file;
        }
        return null;
    }

    /**
     * Read the DisplayMetrics from an activity.
     *
     * To use this:
     * <code>
     *   int height = displaymetrics.heightPixels;
     *   int width = displaymetrics.widthPixels;
     * </code>
     *
     * @param activity, an activity
     * @return the metrics from a display associated to activity.
     */
    public static DisplayMetrics getDisplayDimension(Activity activity) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics;
    }

    public static File getRemovableStorage() {
        String value = System.getenv("SECONDARY_STORAGE");
        if (!TextUtils.isEmpty(value)) {
            String[] paths = value.split(":");
            for (String path : paths) {
                File file = new File(path);
                if (file.isDirectory()) {
                    return file;
                }
            }
        }
        return null; // Most likely, a removable micro sdcard doesn't exist
    }

    /**
     * Get a directory on external storage (SD card).
     * If this directory not exist, will be created.
     * @return a new File representing the chosen directory
     */
/*    public static File getPublicDirectory(String directory) {
        if (directory==null) return null;
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
        File outputDirectory = new File(path + File.separator + directory + File.separator);
        outputDirectory.mkdirs();
        return outputDirectory;
    }

    public static void applyAllPermission(File file) {

        if(file.exists()){
            file.setExecutable(true);
            file.setWritable(true);
            file.setReadable(true);
        }
    }

    public static void startSync(File file, Context context) {
        MediaScannerConnection.scanFile(context, new String[] {file.getAbsolutePath()}, null, null );
    }*/
}
