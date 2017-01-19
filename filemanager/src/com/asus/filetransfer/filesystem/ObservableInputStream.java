package com.asus.filetransfer.filesystem;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Yenju_Lai on 2015/9/16.
 */
public class ObservableInputStream extends InputStream {

    public interface Listener {
        public void onStreamRead(int byteRead);
    }

    private InputStream stream = null;
    private Listener listener = null;

    public ObservableInputStream(InputStream stream, Listener listener) {
        this.stream = stream;
        this.listener = listener;
    }

    private int updateReadLength(int read) {
        final boolean shouldUpdate = read > 0 && listener != null;
        if (shouldUpdate)
            listener.onStreamRead(read);
        return read;
    }

    @Override
    public int read() throws IOException {
        int readData = stream.read();
        updateReadLength(readData == -1? 0 : 1);
        return readData;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return updateReadLength(stream.read(buffer));
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return updateReadLength(stream.read(buffer, byteOffset, byteCount));
    }
}
