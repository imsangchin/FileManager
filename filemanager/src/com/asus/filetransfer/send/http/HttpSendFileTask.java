package com.asus.filetransfer.send.http;

import android.util.Log;

import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.http.client.IHttpClient;
import com.asus.filetransfer.http.client.HttpResponse;
import com.asus.filetransfer.http.client.request.IHttpRequestDirector;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.send.ISendTaskStateListener;
import com.asus.filetransfer.send.SendFileTask;

import java.io.IOException;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/16.
 */
public class HttpSendFileTask extends SendFileTask {

    private IHttpRequestDirector httpRequestDirector;
    private IHttpClient httpClient;

    public HttpSendFileTask(IHttpClient httpClient, String host, int port, List<IInputFile> list, ISendTaskStateListener listener) {
        super(list, listener);
        this.httpRequestDirector = new HttpRequestDirector(host, port, sendedLengthListener);
        this.httpClient = httpClient;
    }

    void setHttpRequestDirector(IHttpRequestDirector httpRequestDirector) {
        this.httpRequestDirector = httpRequestDirector;
    }

    @Override
    protected String sendFileList(String fileListJson) throws IOException {
        return httpClient.handleHttpRequest(
                httpRequestDirector.buildHttpRequest(
                        new HttpPostStringRequestBuilder(HttpFileServer.URL_COMMAND_FILELIST, fileListJson, getSessionId())))
                .getResponseMessage();
    }

    @Override
    protected String sendFile(int id, IInputFile file) throws IOException {
        return httpClient.handleHttpRequest(
                httpRequestDirector.buildHttpRequest(
                        new HttpPostFileRequestBuilder(file, getDstPath(), getSessionId(), id))).getResponseMessage();
    }

    @Override
    protected long checkFileStatus(int id, IInputFile file) throws IOException {
        HttpResponse response =
                httpClient.handleHttpRequest(
                        httpRequestDirector.buildHttpRequest(
                                new HttpHeadFileRequestBuilder(file, getDstPath(), getSessionId(), id)));
        if (response.getResponseCode() == NanoHTTPD.Response.Status.FORBIDDEN.getRequestStatus())
            return -1;
        else if (response.getResponseCode() == NanoHTTPD.Response.Status.OK.getRequestStatus())
            return response.getContentLength();
        else if (response.getResponseCode() == NanoHTTPD.Response.Status.NOT_FOUND.getRequestStatus())
            return 0;
        throw new IOException("invalid response");
    }

    @Override
    protected void stopSendFile() {
        try {
            httpClient.handleHttpRequest(
                    httpRequestDirector.buildHttpRequest(
                            new HttpPostStringRequestBuilder(HttpFileServer.URL_COMMAND_DISCONNECT, null, getSessionId())));
        } catch (IOException e) {
            Log.d(getClass().getName(), "Send disconnect message failed");
        }
    }

    @Override
    protected void resumeSendFile(int id, IInputFile file, long breakPoint) throws IOException {
        Log.d(this.getClass().getName(), "session id: " + getSessionId() + ", file id:" + id);
        httpClient.handleHttpRequest(
                httpRequestDirector.buildHttpRequest(
                        new HttpPostResumeFileRequestBuilder(file, getDstPath(), getSessionId(), id, breakPoint)));
    }

}
