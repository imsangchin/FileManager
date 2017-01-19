package com.asus.filemanager.utility;


public class DebugLog {
    public static final boolean DEBUG = (reflectionApis.getSystemPropertyInt("ro.debuggable", 0) == 1);
    public static final boolean PRIVATE_DEBUG = false;
}
