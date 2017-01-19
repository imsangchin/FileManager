package com.asus.filetransfer.receive;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yenju_Lai on 2015/9/25.
 */
public class ReceiveFileHandler implements IReceiveFileHandler {


    private class SessionHandler {
        String sessionId;
        ArrayList<Integer> fileIdTofileUIIndexMap;
        ArrayList<Boolean> fileRejectList;
        private boolean cancalled = false;

        public SessionHandler(String sessionId) {
            this.sessionId = sessionId;
            fileIdTofileUIIndexMap = new ArrayList<>();
            fileRejectList = new ArrayList<>();
        }

        public void cancelSession() {
            cancalled = true;
        }

        public boolean isSessionCancelled() {
            return cancalled;
        }

        public void addFileIndex(int fileId, int fileIndex) {
            while (fileIdTofileUIIndexMap.size() <= fileId) {
                fileIdTofileUIIndexMap.add(-1);
                fileRejectList.add(false);
            }
            fileIdTofileUIIndexMap.set(fileId, fileIndex);
        }

        public boolean isSameSession(String sessionId) {
            return  this.sessionId.compareTo(sessionId) == 0;
        }

        public int getFileUIIndex(int fileId) {
            return fileIdTofileUIIndexMap.size()<= fileId? -1 : fileIdTofileUIIndexMap.get(fileId);
        }

        public boolean checkFileRejected(int fileId) {
            return fileIdTofileUIIndexMap.size()<= fileId? false : fileRejectList.get(fileId);
        }

        public boolean rejectFileByUIIndex(int uiIndex) {
            for (int i = 0; i < fileIdTofileUIIndexMap.size(); i++)
                if (fileIdTofileUIIndexMap.get(i) == uiIndex) {
                    fileRejectList.set(i, true);
                    return true;
                }
            return false;
        }

        public List<Integer> getFileIndexList() {
            return fileIdTofileUIIndexMap;
        }
    }

    private ArrayList<SessionHandler> sessionList;
    private IReceiveFileStatusListener fileStatusListener;
    public ReceiveFileHandler(IReceiveFileStatusListener listener) {
        this.fileStatusListener = listener;
        sessionList = new ArrayList<>();
    }

    @Override
    public boolean checkFileCancelled(String sessionId, int fileId) {
        SessionHandler sessionHandler = findSessionHandler(sessionId);
        if (sessionHandler == null)
            return false;
        else
            return sessionHandler.checkFileRejected(fileId);
    }

    @Override
    public boolean onFileListReceived(String sessionId, String fileList) {
        if (sessionId == null || fileList == null || fileStatusListener == null)
            return false;
        else
            return parseFileList(fileList, sessionId);
    }

    private boolean parseFileList(String fileList, String sessionId) {
        try {
            JSONObject jsonObject = new JSONObject(fileList);
            JSONArray jsonArray = jsonObject.getJSONArray("list");
            if (jsonArray.length() == 0)
                return false;
            SessionHandler sessionHandler = new SessionHandler(sessionId);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObj = jsonArray.getJSONObject(i);
                sessionHandler.addFileIndex(
                        jsonObj.getInt("id"),
                        fileStatusListener.onFileReceived(
                                jsonObj.getString("name"), jsonObj.getBoolean("isDirectory"), jsonObj.getLong("size"))
                );
            }
            sessionList.add(sessionHandler);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onReceiveProgressUpdated(String sessionId, int fileId, int received) {
        if (sessionId == null || fileId < 0 || received < 0 || fileStatusListener == null)
            return false;
        int index;
        SessionHandler sessionHandler = findSessionHandler(sessionId);
        if (sessionHandler == null)
            Log.d(this.getClass().getName(), "sessionHandler is null");
        if (sessionHandler == null
                || sessionHandler.isSessionCancelled()
                || sessionHandler.checkFileRejected(fileId)
                || (index = sessionHandler.getFileUIIndex(fileId)) < 0)
            return false;
        fileStatusListener.onFileProgressUpdated(index, received);
        return true;
    }

    @Override
    public void onSessionDisconnect(String sessionId) {
        SessionHandler sessionHandler = findSessionHandler(sessionId);
        if (sessionHandler == null)
            return;
        sessionHandler.cancelSession();
        for (int fileIndex : sessionHandler.getFileIndexList())
            fileStatusListener.onFileCancalled(fileIndex);
    }

    private SessionHandler findSessionHandler(String sessionId) {
        for (SessionHandler sessionHandler : sessionList)
            if (sessionHandler.isSameSession(sessionId))
                return sessionHandler;
        return null;
    }

    @Override
    public void stopReceiveItemAt(int index) {
        for (SessionHandler sessionHandler : sessionList) {
            if (sessionHandler.rejectFileByUIIndex(index))
                break;
        }
    }
}
