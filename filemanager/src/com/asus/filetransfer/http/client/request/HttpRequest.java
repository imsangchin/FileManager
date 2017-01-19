package com.asus.filetransfer.http.client.request;

import com.asus.filetransfer.http.HttpConstants.HttpHeaderField;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/18.
 */
public class HttpRequest {

    public class Header {
        private HttpHeaderField field;

        private String value;

        public Header(HttpHeaderField field, String value) {
            this.field = field;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public HttpHeaderField getField() {
            return field;
        }
    }

    private NanoHTTPD.Method method;
    private URL url;

    private InputStream content;
    private String contentType;
    private long contentLength;

    private Queue<Header> headers = null;

    public void setUrl(String host, int port, String url) throws MalformedURLException {
        /*try {
            this.url = new URI("http", null, host, port, url, null, null).toURL();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }*/
        url = url == null? "" : url;
        this.url = new URL("http", host, port, url);
    }

    public void setMethod(NanoHTTPD.Method method) {
        this.method = method;
    }


    public void addHeader(HttpHeaderField field, String value) {
        if (value == null)
            return;
        if (headers == null)
            headers = new ConcurrentLinkedQueue<>();
        headers.add(new Header(field, value));
    }

    public Queue<Header> getHeaders() {
        return headers;
    }

    public Header getNextHeader() {
        if (headers == null || headers.size() == 0) {
            headers = null;
            return null;
        }
        else
            return headers.poll();
    }

    public void setContent(InputStream inputStream, String type, long length) {
        content = inputStream;
        contentType = type;
        contentLength = length;
        addHeader(HttpHeaderField.CONTENT_TYPE, type);
        addHeader(HttpHeaderField.CONTENT_LENGTH, String.valueOf(length));
    }

    public NanoHTTPD.Method getMethod() {
        return method;
    }

    public URL getUrl() {
        return url;
    }

    public InputStream getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }
}
