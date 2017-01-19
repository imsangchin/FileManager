package com.asus.filetransfer.send.http;

import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.filesystem.ObservableInputStream;
import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.client.request.HttpRequest;
import com.asus.filetransfer.http.client.request.IHttpRequestBuilder;
import com.asus.filetransfer.http.server.HttpFileServer;

import java.io.IOException;
import java.net.MalformedURLException;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/10/14.
 */
public class HttpHeadFileRequestBuilder implements IHttpRequestBuilder {

    protected HttpRequest httpRequest = new HttpRequest();
    private String sessionId;
    private IInputFile file;
    private String url;
    private int fileId;

    public HttpHeadFileRequestBuilder(IInputFile inputFile, String dstPath, String sessionId, int fileId) {
        this.sessionId = sessionId;
        this.file = inputFile;
        this.url = HttpFileServer.URL_FILE_PREFIX + dstPath + inputFile.getEncodePath();
        this.fileId = fileId;
    }

    @Override
    public void setURL(String host, int port) throws MalformedURLException {
        httpRequest.setUrl(host, port, url);
    }

    @Override
    public void setMethod() {
        httpRequest.setMethod(NanoHTTPD.Method.HEAD);
    }

    @Override
    public void setHeaders() {
        httpRequest.addHeader(HttpConstants.HttpHeaderField.CUSTOM_SESSION_ID, sessionId);
        httpRequest.addHeader(HttpConstants.HttpHeaderField.CUSTOM_FILE_ID, String.valueOf(fileId));
    }

    @Override
    public void setContent(ObservableInputStream.Listener listener) throws IOException {

    }

    @Override
    public HttpRequest buildHttpRequest() {
        return httpRequest;
    }
}
