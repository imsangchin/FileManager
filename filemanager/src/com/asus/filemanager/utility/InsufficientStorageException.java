package com.asus.filemanager.utility;

/**
 * Created by Yenju_Lai on 2016/4/28.
 */
public class InsufficientStorageException extends RuntimeException {
    public InsufficientStorageException(String s) {
        super(s);
    }
}
