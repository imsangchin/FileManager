package com.asus.filetransfer.http.server.method;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filetransfer.filesystem.FileCompressor;
import com.asus.filetransfer.http.HttpConstants;
import com.asus.filetransfer.http.HttpConstants.HttpHeaderField;
import com.asus.filetransfer.http.server.HttpFileServer;
import com.asus.filetransfer.utility.HttpFileServerAnalyzer;
import com.asus.filetransfer.utility.HttpServerEvents;
import com.asus.filetransfer.utility.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * Created by Yenju_Lai on 2015/10/16.
 */
public class PostCompressHandler extends IHttpMethodHandler {
    private FileManager fileManager;

    public PostCompressHandler(NanoHTTPD.IHTTPSession session, FileManager fileManager) {
        super(session);
        this.fileManager = fileManager;
    }

    @Override
    public NanoHTTPD.Response executeMethod() {
        try {
            HttpFileServerAnalyzer.commandExecuted(new HttpServerEvents(HttpServerEvents.Action.Compress));
            JSONObject compressListJson = new JSONObject(
                    StringUtils.getFixLengthStringFromInputStream(session.getInputStream()
                    , Long.valueOf(session.getHeader(HttpHeaderField.CONTENT_LENGTH.toString()))));

            String compressedFilePath = compressFile(session.getUri().substring(HttpFileServer.URL_COMPRESS_PREFIX.length())
                    , compressListJson.getJSONArray("items"));
            if (compressedFilePath == null)
                return Response.newFixedLengthCloseConnectionResponse(Status.NOT_FOUND, null, null);
            Response response = Response.newFixedLengthResponse(
                    Response.Status.OK, HttpConstants.HTTP_MIME_TYPE_PLAINTEXT, compressedFilePath);
            response.addHeader(HttpHeaderField.CONTENT_DISPOSITION.toString(), HttpConstants.HTTP_CONTENT_DISPOSITION_PREFIX + "files.zip");
            //response.addHeader(HttpHeaderField.CONTENT_TRANSFER_ENCODING.toString(), HttpConstants.HTTP_CONTENT_TRANSFER_ENCODING_VALUE);
            return response;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return Response.newFixedLengthCloseConnectionResponse(Status.BAD_REQUEST, null, null);
    }

    private String compressFile(String filePath, JSONArray jsonArray) throws JSONException, IOException {
        FileCompressor fileCompressor = null;
        try {
            fileCompressor = fileManager.createFileCompressor(filePath);
            for (int i = 0; i < jsonArray.length(); i++)
                    fileCompressor.addFile(fileManager.getInputFile(jsonArray.getJSONObject(i).getString("path")));
            return fileCompressor.getCompressedFilePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if (fileCompressor != null)
                fileCompressor.delete();
            return null;
        }
    }
}
