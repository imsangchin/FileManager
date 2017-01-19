package com.asus.filemanager.adapter.grouper.categoryparser;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Created by roger_huang on 2015/9/16.
 */
public class NetUtils {
    public static final String TAG = "[NetUtils]";
    public static final boolean DEBUG = true;

    public static final int DEFAULT_CONNECTION_TIMEOUT_IN_MILLI_SECOND = 3000;
    public static final int DEFAULT_READ_TIMEOUT_IN_MILLI_SECOND = 3000;

    /**
     * Download a file from the given URL
     *
     * Refer to:
     * http://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
     */
    public static boolean downloadFromUrl(URL url, File file) throws IOException {
        if (DEBUG) Log.v(TAG, String.format("[downloadFromUrl] url: %s, file: %s", url, file.toString()));

        ReadableByteChannel rbc = null;
        FileOutputStream fos = null;
        try {
            rbc = Channels.newChannel(openStreamWithCustomTimeout(url));
            fos = new FileOutputStream(file);
            long pos = 0;
            long count = 0;

            while ((count = fos.getChannel().transferFrom(rbc, pos, 1024)) != 0) {
                // if (DEBUG) Log.v(TAG, String.format("[downloadFromUrl] read %d bytes from position %d", count, pos));
                pos += count;
            }

            return true;

        } catch (IOException e) {
            Log.w(TAG, String.format("Exception when download from url(%s) to file(%s): %s", url, file, e.toString()));

            return false;

        } finally {
            if (rbc != null) {
                try {
                    rbc.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Read the byte array from the given URL
     */
    public static byte[] readByteArrayFromUrl(URL url) {
        InputStream is = null;

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] result = null;

        try {
            is = openStreamWithCustomTimeout(url);

            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            result = byteBuffer.toByteArray();

        } catch (Exception e) {
            Log.w(TAG, "[readByteArrayFromUrl] exception: " + e.toString());

        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (byteBuffer != null) {
                try {
                    byteBuffer.close();
                } catch (IOException e) {
                }
            }
        }

        return result;
    }

    /**
     * Open input stream for given URL w/ customized timeout
     *
     * Refer to:
     * http://stackoverflow.com/questions/5351689/alternative-to-java-net-url-for-custom-timeout-setting
     */
    public static InputStream openStreamWithCustomTimeout(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT_IN_MILLI_SECOND);
        conn.setReadTimeout(DEFAULT_READ_TIMEOUT_IN_MILLI_SECOND);
        return conn.getInputStream();
    }
}
