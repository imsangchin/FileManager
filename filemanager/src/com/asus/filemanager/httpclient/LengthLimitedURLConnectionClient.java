package com.asus.filemanager.httpclient;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.client.HttpResponse;
import com.asus.filetransfer.http.client.IHttpClient;
import com.asus.filetransfer.http.client.request.HttpRequest;
import com.asus.filetransfer.utility.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Yenju_Lai on 2015/9/21.
 */
public class LengthLimitedURLConnectionClient extends URLConnectionClient {

    private final int DEFAULT_MAX_LENGTH = 1 << 30;
    private final String TAG = this.getClass().getName();
    int maxLength = 0;

    public LengthLimitedURLConnectionClient(int maxLength) {
        this.maxLength = maxLength > 0 && maxLength < DEFAULT_MAX_LENGTH? maxLength : DEFAULT_MAX_LENGTH;
        Log.d(TAG, "max length: " + this.maxLength);
    }

    @Override
    public HttpResponse handleHttpRequest(final HttpRequest request) throws IOException {
        int contentLength = maxLength < request.getContentLength()? maxLength : (int)request.getContentLength();
        Log.d(TAG, String.format("request content length: %d, send content length: %d"
                , request.getContentLength(), contentLength));
        HttpURLConnection connection = (HttpURLConnection) request.getUrl().openConnection();
        try {
            connection.setRequestMethod(request.getMethod().name());
            connection.setRequestProperty("Accept-Encoding", "identity");
            HttpRequest.Header header = null;
            String contentRange = String.format("bytes 0-%d/%d", contentLength - 1, request.getContentLength());
            while ((header = request.getNextHeader()) != null) {
                if (header.getField() == HttpConstants.HttpHeaderField.CONTENT_RANGE)
                    contentRange = getContentRange(header.getValue(), contentLength, contentRange);
                else
                    connection.setRequestProperty(header.getField().toString(),
                            header.getField() == HttpConstants.HttpHeaderField.CONTENT_LENGTH?
                                    String.valueOf(contentLength) : header.getValue());
            }
            if (contentLength > 0) {
                Log.d(TAG, "content-range: " + contentRange);
                connection.setRequestProperty(HttpConstants.HttpHeaderField.CONTENT_RANGE.toString(), contentRange);
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(contentLength);
                writeContent(request.getContent(), connection.getOutputStream(), contentLength);
            }
            HttpResponse response =  handleResponse(connection);
            if (contentLength < request.getContentLength())
                throw new IOException("Not finished yet due to length limitation");
            else
                return response;
        } finally {
            if (request.getContent() != null)
                request.getContent().close();
            connection.disconnect();
        }
    }

    private String getContentRange(String originContentRange, int contentLength, String defautContentRange) {
        Pattern pattern =
                Pattern.compile("bytes ([0-9]*)-([0-9]*)/(([0-9]*))");
        Matcher matcher = pattern.matcher(originContentRange);
        if (matcher.find()) {
            return String.format("bytes %s-%d/%s",
                    matcher.group(1), Long.valueOf(matcher.group(1)) + contentLength - 1, matcher.group(3));
        }
        return defautContentRange;
    }
}
