package com.asus.filetransfer.send;

import android.util.Log;

import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.filesystem.ObservableInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Yenju_Lai on 2015/9/17.
 */
public abstract class SendFileTask {

    private String sessionId = UUID.randomUUID().toString();
    private String dstPath = null;

    private List<SendFileList> fileLists = new ArrayList<>();

    private int totalFileCount = 0;
    private long totalFileLength = 0;
    private int currentFileListIndex = 0;
    private IInputFile currentFile = null;
    private long currentFileSentLength = 0;
    private int sentFileCount = 0;
    private long sentFileLength = 0;
    private int cancelledFileCount = 0;

    protected State currentState;
    private State initialState;
    private State sendFileListState;
    private State sendFileState;
    private State checkServerStatusState;
    private State completedState;
    private State cancelState;
    private State resumeState;
    private State pausedState;

    public SendFileTask(List<IInputFile> list, ISendTaskStateListener listener) {
        for (int i = 0; i < list.size(); i++) {
            SendFileList sendFileList = new SendFileList(list.get(i), i);
            totalFileCount += sendFileList.getFileCount();
            totalFileLength += sendFileList.getTotalFileSize();
            fileLists.add(sendFileList);
        }
        initialState = new InitialState(this, listener);
        sendFileListState = new SendFileListState(this, listener);
        sendFileState = new SendFileState(this, listener);
        checkServerStatusState = new CheckServerFileStatusState(this, listener);
        completedState = new CompletedState(this, listener);
        cancelState = new CancelState(this, listener);
        resumeState = new ResumeState(this, listener);
        pausedState = new PausedState(this, listener);
        currentState = initialState;
    }


    public int getTotalFileCount() {
        return totalFileCount;
    }

    public long getTotalFileLength() {
        return totalFileLength;
    }

    protected String getSessionId() {
        return sessionId;
    }

    protected String getDstPath() {
        return dstPath;
    }

    private boolean hasNextFile() {
        do {
            if (fileLists.get(currentFileListIndex).getFileCount() > 0)
                return true;
            currentFileListIndex++;
        } while (currentFileListIndex < fileLists.size());
        return false;
    }

    SendFileList getCurrentFileFileList() {
        return fileLists.get(currentFileListIndex);
    }

    public void start() {
        if (currentState instanceof UserStartState)
            currentState.run();
    }

    public void cancel() {
        changeStateTo(currentState, cancelState);
    }

    private String getJson() {
        JSONObject jsonObj = new JSONObject();
        try {
            JSONArray jsonArray = new JSONArray();
            for (SendFileList list : fileLists)
                jsonArray.put(list.toJson());
            jsonObj.put("list", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObj.toString();
    }

    protected abstract String sendFileList(String fileListJson) throws IOException;

    protected abstract String sendFile(int id, IInputFile file) throws IOException;

    protected abstract long checkFileStatus(int id, IInputFile file) throws IOException;

    protected abstract void stopSendFile();

    protected abstract void resumeSendFile(int id, IInputFile file, long breakPoint) throws IOException;

    protected ObservableInputStream.Listener sendedLengthListener = new ObservableInputStream.Listener() {
        @Override
        public void onStreamRead(int byteRead) {
            currentFileSentLength += byteRead;
            sentFileLength += byteRead;
            currentState.onSendProgressUpdated(sentFileLength);
        }
    };


    private synchronized boolean changeStateTo(State originState,State dstState) {
        if (currentState != originState || currentState instanceof TerminalState)
            return false;
        currentState = dstState;
        currentState.enter();
        return true;
    }

    private void changeToStateThenRun(State originState,State dstState) {
        if (changeStateTo(originState, dstState)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    currentState.run();
                }
            }).start();
            //currentState.run();
        }
    }

    static abstract class State {

        protected ISendTaskStateListener sendTaskStateListener;
        protected final WeakReference<SendFileTask> taskRef;
        public State(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            taskRef = new WeakReference<SendFileTask>(sendFileTask);
            sendTaskStateListener = listener;
        }
        public void enter() {}
        public void run() {}

        public void onSendProgressUpdated(long sentFileLength) {
        }
    }

    static abstract class UserStartState extends State {
        public UserStartState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }
    }

    static abstract class SendingState extends State {
        public SendingState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void onSendProgressUpdated(long sentFileLength) {
            sendTaskStateListener.onSendProgressUpdated(sentFileLength);
        }
    }

    static abstract class TerminalState extends State {
        public TerminalState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }
    }

    static class InitialState extends UserStartState {

        public InitialState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void enter() {
            sendTaskStateListener.onSendTaskStopped();
        }

        @Override
        public void run() {
            SendFileTask task = taskRef.get();
            task.changeToStateThenRun(this, task.sendFileListState);
        }

    }

    static class SendFileListState extends State {

        public SendFileListState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void run() {
            SendFileTask task = taskRef.get();
            try {
                task.dstPath = task.sendFileList(task.getJson());
            } catch (IOException e) {
                Log.d(this.getClass().getName(), "send file list fail");
                e.printStackTrace();
                task.dstPath = null;
            }
            if (task.dstPath != null && task.hasNextFile())
                task.changeToStateThenRun(this, task.sendFileState);
            else
                task.changeStateTo(this, task.initialState);
        }

    }

    static class SendFileState extends SendingState {

        public SendFileState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void enter() {
            sendTaskStateListener.onSendTaskStarted();
        }

        @Override
        public void run() {
            SendFileTask task = taskRef.get();
            try {
                task.currentFile = task.getCurrentFileFileList().getNext();
                task.currentFileSentLength = 0;
                String newPath = task.sendFile(task.getCurrentFileFileList().getId(), task.currentFile);
                if (newPath != null)
                    task.getCurrentFileFileList().updateFilePath(task.currentFile.getEncodePath(), newPath);
                sendTaskStateListener.onFileSendedCountUpdated(++task.sentFileCount);
                if (task.hasNextFile())
                    task.changeToStateThenRun(this, task.sendFileState);
                else
                    task.changeStateTo(this, task.completedState);
            } catch (IOException e) {
                Log.d(this.getClass().getName(), "send file fail");
                e.printStackTrace();
                task.changeToStateThenRun(this, task.checkServerStatusState);
            }
        }
    }

    static class CheckServerFileStatusState extends State {

        public CheckServerFileStatusState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void run() {
            SendFileTask task = taskRef.get();
            try {
                long sentSize = task.checkFileStatus(task.getCurrentFileFileList().getId(), task.currentFile);
                if (sentSize < 0)
                    serverRejectFile();
                else
                    updateActualProgress(sentSize);
            } catch (IOException e) {
                Log.d(this.getClass().getName(), "check file status failed");
                e.printStackTrace();
                task.changeStateTo(this, task.pausedState);
            }
        }

        private void updateActualProgress(long sentLength) {
            SendFileTask task = taskRef.get();
            task.sentFileLength += sentLength - task.currentFileSentLength;
            task.currentFileSentLength = sentLength;
            Log.d(this.getClass().getName(), "sentFileLength: " + task.sentFileLength);
            sendTaskStateListener.onSendProgressUpdated(task.sentFileLength);
            task.changeToStateThenRun(this, sentLength == 0? task.sendFileState : task.resumeState);
        }

        private void serverRejectFile() {
            SendFileTask task = taskRef.get();
            task.sentFileLength += task.currentFile.isDirectory()? 0 : (task.currentFile.getSize() - task.currentFileSentLength);
            task.sentFileLength += task.fileLists.get(task.currentFileListIndex).getTotalFileSize();
            sendTaskStateListener.onSendProgressUpdated(task.sentFileLength);
            sendTaskStateListener.onFileCancelled((task.cancelledFileCount += task.fileLists.get(task.currentFileListIndex).getFileCount() + 1));
            task.fileLists.get(task.currentFileListIndex).clear();
            task.currentFile = null;

            if (task.hasNextFile())
                task.changeToStateThenRun(this, task.sendFileState);
            else
                task.changeStateTo(this, task.completedState);
        }
    }

    static class CompletedState extends TerminalState {

        public CompletedState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void enter() {
            sendTaskStateListener.onSendTaskCompleted();
        }
    }

    static class CancelState extends TerminalState {

        public CancelState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void enter() {
            SendFileTask task = taskRef.get();
            task.stopSendFile();
            sendTaskStateListener.onSendTaskCancelled();
        }
    }

    static class ResumeState extends SendingState {

        public ResumeState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void run() {
            SendFileTask task = taskRef.get();
            try {
                task.resumeSendFile(task.getCurrentFileFileList().getId(), task.currentFile, task.currentFileSentLength);
                sendTaskStateListener.onFileSendedCountUpdated(++task.sentFileCount);
                if (task.hasNextFile())
                    task.changeToStateThenRun(this, task.sendFileState);
                else
                    task.changeStateTo(this, task.completedState);
            } catch (IOException e) {
                e.printStackTrace();
            }
            task.changeToStateThenRun(this, task.checkServerStatusState);
        }
    }

    private class PausedState extends UserStartState {
        public PausedState(SendFileTask sendFileTask, ISendTaskStateListener listener) {
            super(sendFileTask, listener);
        }

        @Override
        public void enter() {
            sendTaskStateListener.onSendTaskPaused();
        }

        @Override
        public void run() {
            SendFileTask task = taskRef.get();
            task.changeToStateThenRun(this, task.checkServerStatusState);
        }
    }
}
