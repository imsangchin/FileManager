package com.asus.filetransfer.http.server.method;

import android.util.Log;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.filesystem.IOutputFile;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.receive.IReceiveFileHandler;
import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.HttpConstants.HttpHeaderField;
import com.asus.filetransfer.utility.HttpFileServerAnalyzer;
import com.asus.filetransfer.utility.HttpServerEvents;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/4.
 */
public class PostHandler extends IHttpMethodHandler {

    private String TAG = getClass().getSimpleName();
    FileManager fileManager;
    IReceiveFileHandler receiveFileHandler;
    private boolean containEOF = true;
    private String originFilePath;

    public PostHandler(NanoHTTPD.IHTTPSession session, FileManager fileManager, IReceiveFileHandler handler) {
        super(session);
        this.fileManager = fileManager;
        receiveFileHandler = handler;
    }

    @Override
    public NanoHTTPD.Response executeMethod() {
        if (session.getUri().startsWith(HttpFileServer.URL_FILE_PREFIX)) {
            try {
                return receiveFile(session, getOutputFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.FORBIDDEN, null, null);
    }

    protected IOutputFile getOutputFile() throws IOException {
        final String sessionId = session.getHeader(HttpHeaderField.CUSTOM_SESSION_ID.toString());
        final Integer fileId = session.getHeader(HttpHeaderField.CUSTOM_FILE_ID.toString()) == null?
                -1 : Integer.valueOf(session.getHeader(HttpHeaderField.CUSTOM_FILE_ID.toString()));
        IOutputFile outputFile = fileManager.getOutputFile(session.getUri().substring(HttpFileServer.URL_FILE_PREFIX.length()));
        originFilePath = outputFile.getPath();
        if (sessionId != null)
            outputFile = fileManager.getOutputFile(getHashFilePath(
                        session.getUri().substring(HttpFileServer.URL_FILE_PREFIX.length()), sessionId, fileId));
        Log.d(TAG, String.format("destination path: %s, temp file path: %s", originFilePath, outputFile.getPath()));
        return outputFile;
    }

    protected String parseSessionId(Map<String, String> headers) {
        String sessionId = headers.get(HttpHeaderField.CUSTOM_SESSION_ID.toString());
        final boolean shouldParseSessionId = receiveFileHandler != null
                && sessionId != null;
        return shouldParseSessionId? sessionId : null;
    }

    protected NanoHTTPD.Response receiveFile(NanoHTTPD.IHTTPSession session, IOutputFile outputFile) {
        if (!invokeHandler(session.getHeaders(), 0))
            return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.FORBIDDEN, null, null);
            if (isPostDirectory(session.getHeader(HttpHeaderField.CONTENT_TYPE.toString())))
                return handlePostDirectory(session, outputFile);
            else if (getResumeUpdateIndex(session.getHeaders()) == outputFile.getSize()) {
                if (outputFile.getSize() == 0)
                    HttpFileServerAnalyzer.commandExecuted(new HttpServerEvents(HttpServerEvents.Action.Upload));
                try {
                    boolean receiveSuccess = saveInputStreamToFile(session, outputFile.getOutputStream(),
                            Long.parseLong(session.getHeader(HttpHeaderField.CONTENT_LENGTH.toString())));
                    if (!receiveSuccess)
                        return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.FORBIDDEN, null, null);
                    else {
                    if (containEOF)
                            fileManager.triggerMediaScanner(
                                fileManager.renameFile(outputFile.getPath(), originFilePath));
                        return NanoHTTPD.Response.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, null, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, null, null);
                }
            }
        return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.CONFLICT, null, null);
    }

    protected boolean invokeHandler(Map<String, String> headers, int received) {
        try {
            final String sessionId = headers.get(HttpHeaderField.CUSTOM_SESSION_ID.toString());
            final Integer fileId = Integer.valueOf(headers.get(HttpHeaderField.CUSTOM_FILE_ID.toString()));
            boolean shouldInvoke = receiveFileHandler != null && sessionId != null && fileId != null;
            //Log.d(this.getClass().getName(), "session id: " + sessionId + ", file id:" + fileId);
            if (shouldInvoke)
                return receiveFileHandler.onReceiveProgressUpdated(sessionId, fileId, received);
        } catch (Exception e) {
        }
        return true;
    }

    protected long getResumeUpdateIndex(Map<String, String> headers) {
        String contentRange = headers == null? null : headers.get(HttpHeaderField.CONTENT_RANGE.toString());
        if (contentRange == null)
            return 0;
        try {
            Log.d(this.getClass().getName(), "content-range: " + contentRange);
            contentRange = contentRange.replace(HttpConstants.HTTP_CONTENT_RANGE_PREFIX, "");
            long totalLength = Long.parseLong(contentRange.substring(contentRange.indexOf("/") + 1));
            String[] range = contentRange.substring(0, contentRange.indexOf("/")).split("-");
            if (range.length == 2) {
                containEOF = Long.parseLong(range[1]) == totalLength -1;
            }
            return Long.parseLong(range[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private NanoHTTPD.Response handlePostDirectory(NanoHTTPD.IHTTPSession session, IOutputFile outputFile) {
        HttpFileServerAnalyzer.commandExecuted(new HttpServerEvents(HttpServerEvents.Action.CreateFolder));
        final boolean createDirectorySuccess = outputFile.isDirectory() || outputFile.mkdirs();
        if (session.getHeader(HttpHeaderField.CONTENT_LENGTH.toString()) != null) {
            try {
            saveInputStreamToFile(session, null,
                        Long.parseLong(session.getHeader(HttpHeaderField.CONTENT_LENGTH.toString())));
            } catch (IOException e) {
                e.printStackTrace();
                return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, null, null);
        }
        }
        if (!createDirectorySuccess)
            return NanoHTTPD.Response.newFixedLengthResponse(NanoHTTPD.Response.Status.CONFLICT, null, null);
        try {
            String actualPath = fileManager.renameFile(outputFile.getPath(), originFilePath);
            fileManager.triggerMediaScanner(actualPath);
            return  NanoHTTPD.Response.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK, HttpConstants.HTTP_MIME_TYPE_PLAINTEXT, actualPath.substring(actualPath.lastIndexOf(File.separator) + 1));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return NanoHTTPD.Response.newFixedLengthCloseConnectionResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, null, null);
    }

    protected boolean isPostDirectory(String contentType) {
        return contentType == null? false :
                contentType.compareTo(HttpConstants.HTTP_MIME_TYPE_DIRECTORY) == 0;
    }

    protected boolean saveInputStreamToFile(NanoHTTPD.IHTTPSession session, OutputStream outputStream, long contentLength) throws IOException {
        final int BUFFER_SIZE = 64 * 1024;
        InputStream inputStream = session.getInputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read = 0, shouldRead = (int)(contentLength < BUFFER_SIZE? contentLength : BUFFER_SIZE);
            while (shouldRead > 0 && (read = inputStream.read(buffer, 0, shouldRead)) != -1) {
                contentLength -= read;
                shouldRead = (int)(contentLength < BUFFER_SIZE? contentLength : BUFFER_SIZE);
                if (outputStream == null)
                    continue;
                outputStream.write(buffer, 0, read);
                if (!invokeHandler(session.getHeaders(), read))
                    return false;
            }
        return true;
    }

}
