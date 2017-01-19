package com.asus.filemanager.provider;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import com.asus.filemanager.functionaldirectory.hiddenzone.Encryptor;
import com.asus.filemanager.functionaldirectory.hiddenzone.HiddenZoneUtility;
import com.asus.filemanager.utility.FileUtility;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ChenHsin_Hsieh on 2016/1/22.
 */
public class OpenFileProvider extends ContentProvider {

    private static final String authority = "com.asus.filemanager.OpenFileProvider";
    private static final String path = "/file";
    private final String[] BASE_COLS = {MediaStore.MediaColumns.DATA,MediaStore.MediaColumns.DISPLAY_NAME,MediaStore.MediaColumns.SIZE};

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException
    {
        if (!uri.getEncodedPath().startsWith(path))
            throw new FileNotFoundException("invalid path");
        File file = new File(getFilePath(uri));
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    public static Uri getUriForFile(File file, boolean fromHiddenZone) {
        if (fromHiddenZone) {
//            return Uri.parse("content://" + authority + path + "/" + HiddenZoneUtility.HIDDEN_ZONE_NAME + "/"
//                    + Uri.encode(Encryptor.getEncryptor().encode(file.getPath()), "/"));
            return Uri.parse("content://"+authority + path + getReadableHiddenZoneUri(file));
        }
        return Uri.parse("content://"+authority + path + Uri.encode(file.getPath(),"/"));
    }

    @NonNull
    private static String getReadableHiddenZoneUri(File file) {
        long lastModified = file.lastModified();
        String newFilePath = "";
        String[] path = file.getPath().split("/");
        for (int i = 0, found = 0; i < path.length; i++) {
            boolean isHiddenZoneDirectory = path[i].compareTo(HiddenZoneUtility.HIDDEN_ZONE_DIRECTORY_NAME) == 0;
            if (path[i].length() == 0)
                continue;
            else if (found == 0 && !isHiddenZoneDirectory) {
                newFilePath = newFilePath + "/" + path[i];
                continue;
            }
            else if (isHiddenZoneDirectory) {
                newFilePath = newFilePath + "/" + HiddenZoneUtility.HIDDEN_ZONE_NAME;
                found = 1;
            }
            else {
                Pattern pattern =
                        Pattern.compile("\\.([0-9]+)-(.+)");
                Matcher matcher = pattern.matcher(path[i]);
                if (matcher.find()) {
                    lastModified = Long.valueOf(matcher.group(1));
                    newFilePath = newFilePath + "/" +  Encryptor.getEncryptor().decode(matcher.group(2));
                } else {
                    newFilePath = newFilePath + "/" +  Encryptor.getEncryptor().decode(path[i].substring(1));
                }
            }
        }
        return Uri.encode(newFilePath,"/") + "?timestamp=" + lastModified;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private String getFilePath(Uri uri) {
        if (uri.getEncodedPath().contains(HiddenZoneUtility.HIDDEN_ZONE_NAME) && uri.getQuery() != null)
            return getPhysicalHiddenZonePath(uri);
        else
            return Uri.decode(uri.getEncodedPath()).substring(path.length());
    }

    private String getPhysicalHiddenZonePath(Uri uri) {
        String newFilePath = "";
        String[] paths = Uri.decode(uri.getEncodedPath()).substring(path.length()).split("/");
        for (int i = 0, hiddenZoneIndex = 0; i < paths.length; i++) {
            boolean isHiddenZoneDirectory = paths[i].compareTo(HiddenZoneUtility.HIDDEN_ZONE_NAME) == 0;
            if (paths[i].length() == 0)
                continue;
            else if (hiddenZoneIndex == 0 && !isHiddenZoneDirectory) {
                newFilePath = newFilePath + "/" + paths[i];
                continue;
            }
            else if (isHiddenZoneDirectory) {
                newFilePath = newFilePath + "/" + HiddenZoneUtility.HIDDEN_ZONE_DIRECTORY_NAME;
                hiddenZoneIndex = i;
            }
            else if (i == hiddenZoneIndex + 1) {
                newFilePath = newFilePath + "/." + uri.getQueryParameter("timestamp")
                        + "-" + Encryptor.getEncryptor().encode(paths[i]);
            }
            else {
                newFilePath = newFilePath + "/." + Encryptor.getEncryptor().encode(paths[i]);
            }
        }
        return newFilePath;
    }

    @Override
    public String getType(Uri uri) {
        File file = new File(getFilePath(uri));
        final int lastDot = file.getName().lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = file.getName().substring(lastDot + 1);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return uri;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        if(projection==null)
            projection = BASE_COLS;
        MatrixCursor matrixCursor= new MatrixCursor(projection);
        matrixCursor.addRow(getProjectionValues(uri, projection));
        return matrixCursor;
    }

    public Object[] getProjectionValues(Uri uri,String[] projection)
    {
        Object[] result = new Object[projection.length];
        File file = new File(getFilePath(uri));
        if(file.exists()) {
            for (int i = 0; i < projection.length; i++) {
                switch (projection[i]) {
                    case MediaStore.MediaColumns.DATA:
                        result[i] = FileUtility.getCanonicalPathNoException(file);
                        break;
                    case MediaStore.MediaColumns.DISPLAY_NAME:
                        result[i] = file.getName();
                        break;
                    case MediaStore.MediaColumns.SIZE:
                        result[i] = file.length();
                        break;
                    default:
                        result[i] = null;
                        break;
                }
            }
        }
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
}
