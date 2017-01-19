package com.asus.filemanager.provider;

import java.util.concurrent.atomic.AtomicBoolean;

import com.asus.filemanager.utility.VFile;

public class WorkerArgs {
    VFile oldFile;
    VFile newFile;
    boolean subTree;
    Object locker;
    AtomicBoolean actionComplete;
}
