package com.asus.filemanager.editor;

import java.util.concurrent.Semaphore;

import android.util.Log;

public class Mutex {
    private static final Semaphore LOCK = new Semaphore(0, true);

    public static void Lock()
    {
        Thread t = Thread.currentThread();
        Log.d("[Msg]", "Lock: " + t.toString());
        try {
            LOCK.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void Unlock()
    {
        Thread t = Thread.currentThread();
        Log.d("[Msg]", "Unlock: " + t.toString());
        LOCK.release();
    }
}
