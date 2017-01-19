package com.asus.filetransfer.http.client.request;

import com.asus.filetransfer.filesystem.ObservableInputStream;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by Yenju_Lai on 2015/9/18.
 */
public interface IHttpRequestBuilder {

    void setURL(String host, int port) throws MalformedURLException;
    void setMethod();
    void setHeaders();
    void setContent(ObservableInputStream.Listener listener) throws IOException;
    HttpRequest buildHttpRequest();
}
