package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.utility.HttpFileServerAnalyzer;
import com.asus.filetransfer.utility.HttpServerEvents;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/15.
 */
public class DeleteHandler extends IHttpMethodHandler {
    FileManager fileManager;

    public DeleteHandler(NanoHTTPD.IHTTPSession session, FileManager fileManager) {
        super(session);
        this.fileManager = fileManager;
    }

    @Override
    public NanoHTTPD.Response executeMethod() {
        HttpFileServerAnalyzer.commandExecuted(new HttpServerEvents(HttpServerEvents.Action.Delete));
        return fileManager.delete(session.getUri().substring(HttpFileServer.URL_FILE_PREFIX.length()))?
                NanoHTTPD.Response.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, null, null) :
                NanoHTTPD.Response.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, null, null);
    }
}
