package com.asus.filetransfer.http.client;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/18.
 */
public class HttpResponse {
    protected int responseCode = NanoHTTPD.Response.Status.BAD_REQUEST.getRequestStatus();
    protected String responseMessage;
    protected long contentLength;

    public HttpResponse(int responseCode, String responseMessage, long contentLength) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.contentLength = contentLength;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public long getContentLength() {
        return contentLength;
    }
}
