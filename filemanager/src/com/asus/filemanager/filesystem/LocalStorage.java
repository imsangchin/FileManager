package com.asus.filemanager.filesystem;

/**
 * Created by Yenju_Lai on 2015/11/3.
 */
public class LocalStorage extends LocalFile {

    boolean isLocalStorage;
    String storageName;
    public LocalStorage(String path, String name, FileManager fileManager) {
        this(path, name, fileManager, false);
    }

    public LocalStorage(String path, String name, FileManager fileManager, boolean isLocalStorage) {
        super(path, fileManager);
        this.storageName = name;
        this.isLocalStorage = isLocalStorage;
    }

    @Override
    public String getName() {
        return storageName == null? super.getName() : storageName;
    }

    public boolean isLocalStorage() {
        return isLocalStorage;
    }
}
