package com.asus.filetransfer.send.http;

import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.filesystem.ObservableInputStream;
import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.server.HttpFileServer;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by Yenju_Lai on 2015/9/18.
 */
public class HttpPostFileRequestBuilder extends HttpPostRequestBuilder {

    protected IInputFile file;
    protected String url;
    private int fileId;

    public HttpPostFileRequestBuilder(IInputFile inputFile, String dstPath, String sessionId, int fileId) {
        super(sessionId);
        this.file = inputFile;
        this.url = HttpFileServer.URL_FILE_PREFIX + dstPath + inputFile.getEncodePath();
        this.fileId = fileId;
    }

    @Override
    public void setURL(String host, int port) throws MalformedURLException {
        httpRequest.setUrl(host, port, url);
    }

    @Override
    public void setHeaders() {
        super.setHeaders();
        httpRequest.addHeader(HttpConstants.HttpHeaderField.CUSTOM_FILE_ID, String.valueOf(fileId));
    }

    @Override
    public void setContent(ObservableInputStream.Listener listener) throws IOException {
        if (file.isDirectory())
            httpRequest.setContent(null, HttpConstants.HTTP_MIME_TYPE_DIRECTORY, 0);
        else
            httpRequest.setContent(file.getObservableInputStream(listener)
                    , HttpConstants.HTTP_MIME_TYPE_FILE, file.getSize());
    }

}
