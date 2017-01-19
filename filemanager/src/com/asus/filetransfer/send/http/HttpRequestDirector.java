package com.asus.filetransfer.send.http;

import com.asus.filetransfer.filesystem.ObservableInputStream;
import com.asus.filetransfer.http.client.request.HttpRequest;
import com.asus.filetransfer.http.client.request.IHttpRequestBuilder;
import com.asus.filetransfer.http.client.request.IHttpRequestDirector;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by Yenju_Lai on 2015/9/18.
 */
public class HttpRequestDirector implements IHttpRequestDirector {

    private String host;
    private int port;
    private ObservableInputStream.Listener listener;
    public HttpRequestDirector(String host, int port, ObservableInputStream.Listener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    @Override
    public HttpRequest buildHttpRequest(IHttpRequestBuilder builder) throws IOException {
        try {
            builder.setURL(host, port);
        } catch (MalformedURLException e) {
            throw new IOException("set URL fail");
        }
        builder.setMethod();
        builder.setContent(listener);
        builder.setHeaders();
        return builder.buildHttpRequest();
    }
}
