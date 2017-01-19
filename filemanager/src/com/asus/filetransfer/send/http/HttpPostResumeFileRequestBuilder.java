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
public class HttpPostResumeFileRequestBuilder extends HttpPostFileRequestBuilder {

    long breakPoint;
    long fileEnd;

    public HttpPostResumeFileRequestBuilder(IInputFile inputFile, String dstPath, String sessionId, int fileId, long breakPoint) {
        super(inputFile, dstPath, sessionId, fileId);
        this.breakPoint = breakPoint;
        fileEnd = file.getSize() - 1;
    }

    @Override
    public void setHeaders() {
        super.setHeaders();
        httpRequest.addHeader(HttpConstants.HttpHeaderField.CONTENT_RANGE,
                String.format("bytes %d-%d/%d", breakPoint, fileEnd, file.getSize()));
    }

    @Override
    public void setContent(ObservableInputStream.Listener listener) throws IOException {
        if (breakPoint == 0)
            super.setContent(listener);
        else
            httpRequest.setContent(file.getObservablePartialInputStream(breakPoint, fileEnd, listener)
                    , HttpConstants.HTTP_MIME_TYPE_FILE, file.getSize() - breakPoint);
    }

}
