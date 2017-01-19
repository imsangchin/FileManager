package com.asus.filetransfer.send;

import com.asus.filetransfer.filesystem.IInputFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Yenju_Lai on 2015/9/17.
 */
public class SendFileList {

    private class RenameInfo {
        private final String originEncodedPath;
        private final String newEncodedPath;

        public RenameInfo(String originEncodedPath, String newEncodedPath) {
            this.originEncodedPath = originEncodedPath;
            this.newEncodedPath = newEncodedPath;
        }
    }
    private Queue<IInputFile> queue = new ConcurrentLinkedQueue<IInputFile>();
    private Stack<RenameInfo> renameStack = new Stack<>();
    private int id;
    private String name;
    private long totalFileSize = 0;
    private boolean isDirectory = false;

    public SendFileList(IInputFile file, int id) {
        parseFileList(file, "");
        this.id = id;
        this.name = file.getName();
        this.isDirectory = file.isDirectory();
    }

    private void parseFileList(IInputFile file, String parentEncodedPath) {
        try {
            file.setEncodePath(parentEncodedPath + "/" + URLEncoder.encode(file.getName(), "UTF-8"));
        } catch (Exception e) {
            file.setEncodePath(parentEncodedPath + "/" + file.getName().toString());
        }
        queue.add(file);
        if (file.isDirectory())
            for (IInputFile child : file.listChildren())
                parseFileList(child, file.getEncodePath());
        else
            totalFileSize += file.getSize();

    }

    public int getFileCount() {
        return queue.size();
    }

    public IInputFile getNext() {
        final IInputFile top = queue.peek();
        checkFilePath(top);
        if (!top.isDirectory())
            totalFileSize -= top.getSize();
        return queue.poll();
    }

    private void checkFilePath(IInputFile firstFile) {
        if (renameStack.empty())
            return;
        else {
            RenameInfo renameInfo = renameStack.peek();
            if (firstFile.getEncodePath().startsWith(renameInfo.originEncodedPath)) {
                firstFile.setEncodePath(firstFile.getEncodePath().replace(renameInfo.originEncodedPath, renameInfo.newEncodedPath));
            }
            else {
                renameStack.pop();
                checkFilePath(firstFile);
            }
        }
    }

    public int getId() {
        return id;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public void clear() {
        queue.clear();
    }

    public JSONObject toJson() {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("id", id);
            jsonObj.put("name", name);
            jsonObj.put("size", getTotalFileSize());
            jsonObj.put("isDirectory", isDirectory);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObj;
    }

    public void updateFilePath(String encodePath, String newPath) {

        String newFileName = newPath.substring(newPath.lastIndexOf("/") + 1);
        String parentPath = encodePath.substring(0, encodePath.lastIndexOf("/") + 1);
        try {
            newFileName = URLEncoder.encode(newFileName, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
        }
        renameStack.push(new RenameInfo(findOriginEncodedPath(encodePath), parentPath + newFileName));
    }

    private String findOriginEncodedPath(String encodePath) {
        if (renameStack.empty())
            return encodePath;
        else {
            RenameInfo renameInfo = renameStack.peek();
            if (encodePath.startsWith(renameInfo.newEncodedPath)) {
                return encodePath.replace(renameInfo.newEncodedPath, renameInfo.originEncodedPath);
            }
            else
                return encodePath;
        }
    }
}
