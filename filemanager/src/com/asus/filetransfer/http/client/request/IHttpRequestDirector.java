package com.asus.filetransfer.http.client.request;

import java.io.IOException;

/**
 * Created by Yenju_Lai on 2015/9/21.
 */
public interface IHttpRequestDirector {
    public HttpRequest buildHttpRequest(IHttpRequestBuilder builder) throws IOException;
}
