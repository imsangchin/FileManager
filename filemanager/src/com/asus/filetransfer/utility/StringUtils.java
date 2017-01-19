package com.asus.filetransfer.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Yenju_Lai on 2015/9/24.
 */
public class StringUtils {

    public static String getFixLengthStringFromInputStream(InputStream stream, long length) throws IOException
    {
        int n = 0;
        byte[] buffer = new byte[1024 * 4];
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        int shouldRead = (int)(length < buffer.length? length : buffer.length);
        while (shouldRead > 0 && -1 != (n = stream.read(buffer, 0, shouldRead))) {
            writer.write(buffer, 0, n);
            length -= n;
            shouldRead = (int)(length < buffer.length? length : buffer.length);
        }
        return writer.toString("UTF-8");
    }

    public static String getStringFromInputStream(InputStream stream) throws IOException {
        int n = 0;
        byte[] buffer = new byte[1024 * 4];
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        while (-1 != (n = stream.read(buffer))) {
            writer.write(buffer, 0, n);
        }
        return writer.toString("UTF-8");
    }
}
