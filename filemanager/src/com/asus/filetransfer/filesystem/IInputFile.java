package com.asus.filetransfer.filesystem;

import com.asus.filetransfer.http.HttpConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Yenju_Lai on 2015/9/14.
 */
public abstract class IInputFile {

    public enum Writable {
        No(0),
        Yes(1),
        SAF(2);

        private final int value;
        Writable(int writable) {
            this.value = writable;
        }

        public int getValue() {
            return value;
        }
    }
    private String encodePath;
    protected IInputFile(String path) {
        if (path == null)
            throw new NullPointerException("Path can't be null");
    }

    public abstract boolean exists();

    public abstract boolean isDirectory();

    public abstract String getName();

    public abstract String getPath();

    public String getType() {
        if (isDirectory())
            return HttpConstants.HTTP_MIME_TYPE_PLAINTEXT;
        else
            return HttpConstants.HTTP_MIME_TYPE_FILE;
    }

    public abstract List<IInputFile> listChildren();

    public abstract InputStream getInputStream() throws IOException;

    public abstract InputStream getPartialInputStream(long from, long to) throws IllegalArgumentException;

    public abstract long getSize();

    public InputStream getObservableInputStream(ObservableInputStream.Listener listener) throws IOException {
        return listener == null? getInputStream() : new ObservableInputStream(getInputStream(), listener);
    }

    public InputStream getObservablePartialInputStream(long from, long to, ObservableInputStream.Listener listener) throws IllegalArgumentException {
        return listener == null? getPartialInputStream(from, to)
                : new ObservableInputStream(getPartialInputStream(from, to), listener);
    }

    private JSONArray getChildrenJsonList() {
        if (!isDirectory())
            return null;
        JSONArray jsonArr = new JSONArray();
        for (IInputFile file : listChildren()) {
            jsonArr.put(file.getJson());
        }
        return jsonArr;
    }

    public String getEncodePath() {
        return encodePath;
    }

    public void setEncodePath(String encodePath) {
        this.encodePath = encodePath.replace("+", "%20");
    }

    private JSONObject getJson() {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("name", getName());
            jsonObj.put("path", getPath());
            jsonObj.put("type", isDirectory()? "dir" : "file");
            jsonObj.put("size", getSize());
            jsonObj.put("date", getModifiedTime());
            return jsonObj;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;

    }

    protected Writable canWrite() {
        return Writable.No;
    }

    protected String getModifiedTime() {
        return new SimpleDateFormat("yyyy:MM:dd hh:mm:ss").format(new Date(0));
    }

    public JSONObject toJson() {
        JSONObject jsonObj = getJson();
        try {
            jsonObj.put("canWrite", canWrite().value);
            jsonObj.put("result", getChildrenJsonList());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObj;
    }

}
