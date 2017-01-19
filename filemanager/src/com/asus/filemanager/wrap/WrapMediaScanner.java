package com.asus.filemanager.wrap;

import android.os.Environment;

public class WrapMediaScanner {

    public static String[] getMountPoints(String volume) {
        if ("internal".equals(volume)) {
            return new String[] {
                    Environment.getRootDirectory().getAbsolutePath(),
                    Environment.getDataDirectory().getAbsolutePath()
            };
        } else {
            return new String[] {
                    Environment.getExternalStorageDirectory().getPath(),
                    WrapEnvironment.MOUNT_POINT_MICROSD,
                    // WrapEnvironment.MOUNT_POINT_USBDISK1,
                    // WrapEnvironment.MOUNT_POINT_USBDISK2,
                    WrapEnvironment.MOUNT_POINT_SDREADER
            };
        }
    }

    public static boolean isPathInScanDirectoriesWithUID(String path) {
        if (path == null) {
            return false;
        }

        String directories[] = getMountPoints("external");

        for (int i = 0; i < directories.length; i++) {
            String directory = directories[i];
            if (path.startsWith(directory)) {
                return true;
            }
        }
        return false;
    }
}
