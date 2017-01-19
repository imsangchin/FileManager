package com.asus.filemanager.httpclient;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.client.IHttpClient;
import com.asus.filetransfer.http.client.HttpResponse;
import com.asus.filetransfer.http.client.request.HttpRequest;
import com.asus.filetransfer.utility.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by Yenju_Lai on 2015/9/21.
 */
public class URLConnectionClient extends IHttpClient {

    int timeout = 5000;
    public URLConnectionClient() {
    }

    public URLConnectionClient(int timeout) {
        this.timeout = timeout < 0? 0 : timeout;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public HttpResponse handleHttpRequest(final HttpRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) request.getUrl().openConnection();
        try {
            connection.setRequestMethod(request.getMethod().name());
            connection.setRequestProperty("Accept-Encoding", "identity");
            HttpRequest.Header header = null;
            while ((header = request.getNextHeader()) != null) {
                connection.setRequestProperty(header.getField().toString(), header.getValue());
            }
            if (request.getContentLength() > 0) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(request.getContentLength());
                writeContent(request.getContent(), connection.getOutputStream(), request.getContentLength());
            }
            return handleResponse(connection);
        } finally {
            if (request.getContent() != null)
                request.getContent().close();
            connection.disconnect();
        }
    }

    protected HttpResponse handleResponse(HttpURLConnection connection) throws IOException {
        connection.setReadTimeout(timeout);
        final int responseCode = connection.getResponseCode();
        final boolean errorOccurred = connection.getResponseCode() >= 400;
        Log.d(this.getClass().getName(), responseCode + ": " + connection.getResponseMessage());
        Log.d(this.getClass().getName(), connection.getContentType() + ", " + connection.getContentLength());
        String responseData = connection.getContentLength() > 0? StringUtils.getFixLengthStringFromInputStream(
                errorOccurred ? connection.getErrorStream() : connection.getInputStream(),
                connection.getContentLength()) : null;
        if (connection.getContentType() != null &&
                connection.getContentType().compareTo(HttpConstants.HTTP_MIME_TYPE_PLAINTEXT) == 0) {
            return new HttpResponse(responseCode, responseData, connection.getContentLength());
        }
        return new HttpResponse(responseCode, null, connection.getContentLength());
    }
}
