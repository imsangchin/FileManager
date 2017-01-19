package com.asus.filemanager.functionaldirectory;

import com.asus.filemanager.utility.VFile;

/**
 * Created by Yenju_Lai on 2016/4/7.
 */
public interface Movable {
    boolean destinationExist();
    boolean move();
    VFile getSourceFile();
    String getDestination();
}
