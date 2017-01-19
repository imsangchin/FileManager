package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.receive.IReceiveFileHandler;

import java.io.FileNotFoundException;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/4.
 */
public class HttpMethodContext {

    private FileManager fileManager = null;
    private IReceiveFileHandler receiveFileHandler = null;
    private IHttpMethodHandler httpMethodHandler;
    private NanoHTTPD.IHTTPSession session;

    public HttpMethodContext(NanoHTTPD.IHTTPSession session, FileManager fileManager, IReceiveFileHandler receiveFileHandler) {
        this.fileManager = fileManager;
        this.receiveFileHandler = receiveFileHandler;
        this.session = session;
        create();
    }

    private IInputFile getInputFileByUri(String uri) {
        try {
            if (uri.startsWith(HttpFileServer.URL_FILE_PREFIX))
                return fileManager.getInputFile(uri.substring(HttpFileServer.URL_FILE_PREFIX.length()));
            else
                return fileManager.getAssetFile(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void create() {
        switch (session.getMethod()) {
            case GET:
                httpMethodHandler = new GetHandler(session, getInputFileByUri(session.getUri()));
                break;
            case HEAD:
                httpMethodHandler = new HeadHandler(session, fileManager, getInputFileByUri(session.getUri()), receiveFileHandler);
                break;
            case POST:
                httpMethodHandler = PostHandlerFactory.createCommand(session, fileManager, receiveFileHandler);
                break;
            case DELETE:
                httpMethodHandler = new DeleteHandler(session, fileManager);
                break;
        }
        if (httpMethodHandler == null)
            httpMethodHandler = new NotImplementedMethodHandler(session);
    }

    public NanoHTTPD.Response getHttpResponse() {
        try {
        return httpMethodHandler.executeMethod();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, null, null);
    }
}
