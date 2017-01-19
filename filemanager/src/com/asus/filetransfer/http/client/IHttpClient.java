package com.asus.filetransfer.http.client;

import com.asus.filetransfer.http.client.request.HttpRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Yenju_Lai on 2015/9/18.
 */
public abstract class IHttpClient {
    static final int DEFAULT_BUFFER_SIZE = 64*1024;
    public abstract HttpResponse handleHttpRequest(HttpRequest iHttpRequest) throws IOException;

    public void writeContent(InputStream input, OutputStream output, long contentLength) throws IOException {
        if (contentLength > 0) {
            long contentLeft = contentLength;
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            while (contentLeft > 0) {
                long shouldRead = buffer.length < contentLeft? buffer.length : contentLeft;
                int read = input.read(buffer, 0, (int)shouldRead);
                output.write(buffer, 0, read);
                contentLeft -= read;
            }
            input.close();
            output.flush();
            output.close();
        }
    }
}
