package com.asus.filemanager.utility.permission;

import java.util.ArrayList;

public interface PermissionChecker {
    PermissionManager getManager();
    void permissionDeniedForever(ArrayList<String> permissions);
}