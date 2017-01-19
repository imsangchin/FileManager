package com.asus.filemanager.filesystem;

import android.content.Context;
import android.content.res.AssetManager;

import com.asus.filetransfer.filesystem.IInputFile;
import com.asus.filetransfer.http.HttpConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by Yenju_Lai on 2015/9/18.
 */
public class AssetsInputFile extends IInputFile {

    private InputStream stream = null;
    private String path;
    private long size;

    public AssetsInputFile(Context context, String path) throws FileNotFoundException {
        super(path);
        try {
            this.path = path;
            AssetManager assetManager = context.getAssets();
            stream = assetManager.open(path);
            size = stream.available();
        } catch (IOException e) {
            throw new FileNotFoundException("Asset not found: " + path);
        }
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public String getName() {
        return path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getType() {
        if (path.endsWith(".html"))
            return HttpConstants.HTTP_MIME_TYPE_HTML;
        else if (path.endsWith(".css"))
            return HttpConstants.HTTP_MIME_TYPE_CSS;
        else if (path.endsWith(".js"))
            return HttpConstants.HTTP_MIME_TYPE_JAVASCRIPT;
        else if (path.endsWith(".svg"))
            return HttpConstants.HTTP_MIME_TYPE_SVG;
        else
            return HttpConstants.HTTP_MIME_TYPE_FILE;
    }

    @Override
    public List<IInputFile> listChildren() {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return stream;
    }

    @Override
    public InputStream getPartialInputStream(long from, long to) throws IllegalArgumentException {
        throw new IllegalArgumentException("Asset file not support partial get");
    }

    @Override
    public long getSize() {
        return size;
    }
}
