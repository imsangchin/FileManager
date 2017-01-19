package com.asus.filetransfer.http.server;

import android.util.Log;

import com.asus.filemanager.filesystem.FileManager;
import com.asus.filemanager.utility.WifiStateMonitor;
import com.asus.filetransfer.receive.IReceiveFileHandler;
import com.asus.filetransfer.http.server.method.HttpMethodContext;

import java.io.IOException;
import java.net.BindException;
import java.util.Observable;
import java.util.Observer;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Yenju_Lai on 2015/9/1.
 */
public class HttpFileServer extends NanoHTTPD implements Observer {
    private static final String TAG = "HttpFileServer";
    private static final int DEFAULT_TIMEOUT = 10000;

    public static final String URL_FILE_PREFIX = "/file";
    public static final String URL_COMPRESS_PREFIX = "/compress";
    public static final String URL_COMMAND_DISCONNECT = "/disconnect";
    public static final String URL_COMMAND_FILELIST = "/filelist";
    public static final String PATH_TEMP_FOLDER_FOR_COMPRESSION = "/TempCompress";
    public static final String PATH_COMPRESSION_SUFFIX = ".zip";

    private FileManager fileManager;
    private IReceiveFileHandler receiveFileHandler;
    private IHttpFileServerStateListener serverStateListener;

    private boolean serverStarted = false;
    private final int defalutPort;
    private int retryCount;

    public HttpFileServer(FileManager fileManager, int port) {
        this(fileManager, null, port);
    }

    public HttpFileServer(FileManager fileManager, IReceiveFileHandler receiveFileHandler, int port) {
        super(port);
        this.defalutPort = port;
        this.fileManager = fileManager;
        this.receiveFileHandler = receiveFileHandler;
    }

    public boolean isServerStarted() {
        return serverStarted;
    }

    @Override
    public synchronized void start() throws IOException {
        startServer(defalutPort);
    }

    public synchronized void start(int maxRetryCount, IHttpFileServerStateListener listener) throws IOException {
        serverStateListener = listener;
        startAutoRetry(maxRetryCount);
    }

    private void startServer(int port) throws IOException {
        if (serverStarted)
            throw new IOException("server already start");
        setPort(port);
        super.start(DEFAULT_TIMEOUT);
        serverStarted = true;
        if (null != serverStateListener)
            serverStateListener.onServerStarted();
        return;
    }

    private void startAutoRetry(int maxRetryCount) throws IOException {
        try {
            startServer(defalutPort + retryCount);
        } catch (BindException e) {
            if (maxRetryCount == 0)
                throw e;
            retryCount++;
            startAutoRetry(--maxRetryCount);
        }
    }

    @Override
    public synchronized void stop() {
        if (!serverStarted)
            return;
        fileManager.clearCache();
        super.stop();
        serverStarted = false;
        if (null != serverStateListener)
            serverStateListener.onServerStopped();
        retryCount = 0;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "Receive " + session.getMethod().name() + " request from " + session.getUri());
        return new HttpMethodContext(session, fileManager, receiveFileHandler).getHttpResponse();
    }

    @Override
    public void update(Observable observable, Object data) {
        String ip = null;
        if (data instanceof String) {
            ip = String.format("http://%s:%d", data, getListeningPort() > 0? getListeningPort() : defalutPort + retryCount);
        }
        if (null != serverStateListener)
            serverStateListener.onAddressChanged(ip);
    }
}
