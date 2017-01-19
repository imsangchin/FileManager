package com.asus.filetransfer.send.http;

import com.asus.filetransfer.filesystem.ObservableInputStream;
import com.asus.filetransfer.http.HttpConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by Yenju_Lai on 2015/9/18.
 */
public class HttpPostStringRequestBuilder extends HttpPostRequestBuilder {

    private String string;
    private String url;

    public HttpPostStringRequestBuilder(String url, String string, String sessionId) {
        super(sessionId);
        this.string = string;
        this.url = url;
    }

    @Override
    public void setURL(String host, int port) throws MalformedURLException {
        httpRequest.setUrl(host, port, url);
    }

    @Override
    public void setContent(ObservableInputStream.Listener listener) throws IOException {
        if (string == null)
            super.setContent(null);
        else {
            byte[] content = string.getBytes("UTF-8");
            httpRequest.setContent(new ByteArrayInputStream(content),
                    HttpConstants.HTTP_MIME_TYPE_PLAINTEXT, content.length);
        }
    }

}
