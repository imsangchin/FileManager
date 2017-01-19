package com.asus.filemanager.httpclient;

import android.util.Log;

import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.client.IHttpClient;
import com.asus.filetransfer.http.client.HttpResponse;
import com.asus.filetransfer.http.client.request.HttpRequest;
import com.asus.filetransfer.utility.StringUtils;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;

/**
 * Created by Yenju_Lai on 2015/10/8.
 */
public class ApacheHttpClient extends IHttpClient {

    @Override
    public HttpResponse handleHttpRequest(HttpRequest request) throws IOException {
        Log.d(this.getClass().getName(), "start handleHttpRequest");
        DefaultHttpClient httpClient = new DefaultHttpClient();

        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setSocketBufferSize(httpParameters, 8 * 1024 * 1024);
        HttpConnectionParams.setTcpNoDelay(httpParameters, false);
        httpClient.setParams(httpParameters);
        HttpPost httpPost = null;
        httpPost = new HttpPost(request.getUrl().toString());
        HttpRequest.Header header;

        while ((header = request.getNextHeader()) != null) {
            if (header.getField() != HttpConstants.HttpHeaderField.CONTENT_LENGTH)
                httpPost.setHeader(header.getField().toString(), header.getValue());
        }
        Log.d(this.getClass().getName(), "set entity");
        httpPost.setEntity(new InputStreamEntity(request.getContent(), request.getContentLength()));
        Log.d(this.getClass().getName(), "start execute post");
        org.apache.http.HttpResponse response = httpClient.execute(httpPost);
        Log.d(this.getClass().getName(), "start execute post");

        return handleResponse(response);
    }

    private HttpResponse handleResponse(org.apache.http.HttpResponse httpResponse) throws IOException {
        StatusLine statusLine = httpResponse.getStatusLine();
        final int responseCode = statusLine.getStatusCode();
        final HttpEntity content = httpResponse.getEntity();
        final String contentType = content.getContentType() == null? null : content.getContentType().getValue();
        final long contentLength = content.getContentLength();

        Log.d(this.getClass().getName(), responseCode + ", " + contentLength);
        if (contentType != null &&
                contentType.compareTo(HttpConstants.HTTP_MIME_TYPE_PLAINTEXT) == 0) {
            return new HttpResponse(
                    responseCode,
                    StringUtils.getFixLengthStringFromInputStream(
                            content.getContent(),
                            contentLength
                    ), contentLength);
        }
        return new HttpResponse(responseCode, null, contentLength);
    }
}
