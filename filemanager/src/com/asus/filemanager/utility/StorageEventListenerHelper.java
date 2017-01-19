package com.asus.filemanager.utility;

import android.os.storage.StorageManager;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class StorageEventListenerHelper {
    private static final String TAG = StorageEventListenerHelper.class.getSimpleName();
    public static Class clsStorageEventListener = null;

    static {
        try {
            clsStorageEventListener = StorageEventListenerHelper.class.getClassLoader().loadClass("android.os.storage.StorageEventListener");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } 
    }

    public static void setOnStorageEventListener(StorageManager storageManager, Object listener) {
        Class<?>[] classArray = new Class<?>[1];
        classArray[0] = clsStorageEventListener;

        try {
            Method[] methods = storageManager.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equalsIgnoreCase("registerListener")) {
                    method.invoke(storageManager, listener);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeOnStorageEventListener(StorageManager storageManager, Object listener) {
        Class<?>[] classArray = new Class<?>[1];
        classArray[0] = clsStorageEventListener;

        try {
            Method[] methods = storageManager.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equalsIgnoreCase("unregisterListener")) {
                    method.invoke(storageManager, listener);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ProxyStorageEventListener implements InvocationHandler {
        public void onVolumeStateChanged(Object vol, int oldState, int newState) {
            Log.d("VolumeChanged", "VolumeChanged from " + oldState  + " to " +newState);
        }

        // implements the method invoke from the InvocationHandler interface
        // it intercepts the calls to the listener methods
        // in this case it redirects the onAudioFocusChange listener method to the OnAudioFocusChange proxy method
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = null;
            try {
                if (args != null && args.length == 3) {
                    if (method.getName().equals("onVolumeStateChanged") &&
                        args[0] instanceof Object && args[1] instanceof Integer && args[2] instanceof  Integer) {
                        onVolumeStateChanged(args[0], (int)args[1], (int)args[2]);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("unexpected invocation exception: " + e.getMessage());
            }
            return result;
        }
    }
}
