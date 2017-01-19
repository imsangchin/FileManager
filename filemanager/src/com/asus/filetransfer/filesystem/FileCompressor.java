package com.asus.filetransfer.filesystem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Yenju_Lai on 2015/10/20.
 */
public class FileCompressor {

    private static final int BUFFER_SIZE = 8096;

    File actualFile;
    ZipOutputStream zipOutputStream;
    public FileCompressor(File file) throws FileNotFoundException {
        actualFile = file;
        zipOutputStream = new ZipOutputStream(
                actualFile != null? new FileOutputStream(actualFile) : new ByteArrayOutputStream());
    }

    public void addFile(IInputFile file) throws IOException {
        if (file.isDirectory())
            writeFolder(file.getName() + File.separator, file);
        else
            writeFile(file.getName(), file);
    }

    void writeFile(String fullName, IInputFile file) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(fullName));
        InputStream input = file.getInputStream();
        long fileLength = file.getSize();
        byte[] buffer = new byte[BUFFER_SIZE];
        int shouldRead = fileLength < BUFFER_SIZE? (int)fileLength : BUFFER_SIZE;
        while (fileLength > 0) {
            int read = input.read(buffer, 0, shouldRead);
            zipOutputStream.write(buffer, 0, read);
            fileLength -= read;
            shouldRead = fileLength < BUFFER_SIZE? (int)fileLength : BUFFER_SIZE;
        }
        input.close();
    }

    void writeFolder(String fullName, IInputFile file) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(fullName));
        for (IInputFile child : file.listChildren()) {
            if (child.isDirectory())
                writeFolder(fullName + child.getName() + File.separator, child);
            else
                writeFile(fullName + child.getName(), child);
        }
    }

    public String getCompressedFilePath() throws IOException {
        zipOutputStream.close();
        return actualFile.getPath();
    }

    public void delete() throws IOException {
        zipOutputStream.close();
        actualFile.delete();
    }
}
