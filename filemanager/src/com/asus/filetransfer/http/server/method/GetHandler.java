package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.AssetsInputFile;
import com.asus.filemanager.filesystem.LocalFile;
import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.HttpConstants.HttpHeaderField;
import com.asus.filetransfer.utility.HttpFileServerAnalyzer;
import com.asus.filetransfer.utility.HttpServerEvents;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

/**
 * Created by Yenju_Lai on 2015/9/1.
 */
public class GetHandler extends IHttpMethodHandler {

    private boolean shouldSendBody = true;
    private boolean supportRange = true;

    private IInputFile file;
    public GetHandler(IHTTPSession session, IInputFile file) {
        super(session);
        this.file = file;
    }

    protected void doNotSendBody() {
        shouldSendBody = false;
    }

    protected void doNotSupportRange() {
        supportRange = false;
    }

    private Response handleGetDirectory(IInputFile directoryInfo) {
        Response response =  Response.newFixedLengthResponse(
                Response.Status.OK,
                directoryInfo.getType(),
                shouldSendBody? directoryInfo.toJson().toString() : null);
        response.addHeader(HttpHeaderField.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), HttpConstants.HTTP_ACCESS_CONTROL_ALLOW_ORIGIN_ALL);
        return response;
    }

    protected Response handleGetFullFile(IInputFile fileInfo) throws IOException {
        sendDownloadEvent(fileInfo);
        Response response = Response.newFixedLengthResponse(
                Response.Status.OK,
                fileInfo.getType(),
                shouldSendBody? fileInfo.getInputStream() : null,
                shouldSendBody? fileInfo.getSize() : 0);
        response.addHeader(HttpHeaderField.CONTENT_LENGTH.toString(), String.valueOf(fileInfo.getSize()));
        response.addHeader(HttpHeaderField.ACCEPT_RANGES.toString(), HttpConstants.HTTP_ACCEPT_RANGE_VALUE);
        return response;
    }

    private Response handleGetPartialFile(String range, IInputFile fileInfo) throws IOException {
        long from, to = fileInfo.getSize() - 1;
        String[] splitRange = range.split("-");
        try {
            if (splitRange.length > 1)
                to = Long.valueOf(splitRange[1]);
            from = Long.valueOf(splitRange[0]);
            to = to >= fileInfo.getSize()? fileInfo.getSize() - 1: to;
            final long actualRange = to - from + 1;
            if (from == 0)
                sendDownloadEvent(fileInfo);
            Response response = Response.newFixedLengthResponse(
                    Response.Status.PARTIAL_CONTENT,
                    fileInfo.getType(),
                    fileInfo.getPartialInputStream(from, to),
                    actualRange);
            response.addHeader(HttpHeaderField.CONTENT_LENGTH.toString(), String.valueOf(actualRange));
            response.addHeader(HttpHeaderField.CONTENT_RANGE.toString(), "bytes " + from + "-" + to + "/" + fileInfo.getSize());
            response.addHeader(HttpHeaderField.ACCEPT_RANGES.toString(), HttpConstants.HTTP_ACCEPT_RANGE_VALUE);
            return response;
        } catch (Exception e) {
            return handleGetFullFile(fileInfo);
        }
    }

    private void sendDownloadEvent(IInputFile inputFile) {
        if (shouldSendBody && !(inputFile instanceof AssetsInputFile))
            HttpFileServerAnalyzer.commandExecuted(new HttpServerEvents(HttpServerEvents.Action.Download));
    }

    @Override
    public Response executeMethod() {
        try {
            if (!file.exists())
                return Response.newFixedLengthResponse(Response.Status.NOT_FOUND, null, null);
            if (file.isDirectory()) {
                return handleGetDirectory(file);
            }
            else if (!supportRange || supportedRange(session.getHeaders()) == null) {
                return handleGetFullFile(file);
            }
            else {
                return handleGetPartialFile(supportedRange(session.getHeaders()), file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.newFixedLengthResponse(Response.Status.NOT_FOUND, null, null);
    }

    private String supportedRange(Map<String, String> header) {
        final boolean containRange = header != null && header.get(HttpHeaderField.RANGE.toString()) != null;
        final boolean supportedRange =
                containRange? header.get(HttpHeaderField.RANGE.toString()).startsWith(HttpConstants.HTTP_SUPPORT_RANGE) : false;
        return (containRange && supportedRange)?
                header.get(HttpHeaderField.RANGE.toString()).substring(HttpConstants.HTTP_SUPPORT_RANGE.length()) : null;
    }
}
