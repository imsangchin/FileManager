package com.asus.filetransfer.send.http;

import com.asus.filetransfer.filesystem.ObservableInputStream;
import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.client.request.HttpRequest;
import com.asus.filetransfer.http.client.request.IHttpRequestBuilder;

import java.io.IOException;
import java.net.MalformedURLException;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/21.
 */
public abstract class HttpPostRequestBuilder implements IHttpRequestBuilder {
    protected HttpRequest httpRequest = new HttpRequest();
    private String sessionId;
    public HttpPostRequestBuilder(String sessionId) {
        this.sessionId = sessionId;
    }

    public abstract void setURL(String host, int port) throws MalformedURLException;

    @Override
    public void setMethod() {
        httpRequest.setMethod(NanoHTTPD.Method.POST);
    }


    @Override
    public void setHeaders() {
        httpRequest.addHeader(HttpConstants.HttpHeaderField.CUSTOM_SESSION_ID, sessionId);
    }

    @Override
    public void setContent(ObservableInputStream.Listener listener) throws IOException {
        httpRequest.setContent(null, null, 0);
    }

    @Override
    public HttpRequest buildHttpRequest() {
        return httpRequest;
    }
}
