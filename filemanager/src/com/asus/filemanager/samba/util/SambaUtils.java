package com.asus.filemanager.samba.util;

import jcifs.smb.NtStatus;

/**
 * Basic utility method for access samba server.
 */
public class SambaUtils {

    /**
     * Convert {@link NtStatus} integer value to human readable string.
     *
     * @param status One of the {@link NtStatus} value.
     * @return The human readable string message.
     */
    public static String convertSmbNtStatus(int status)
    {
        for(int i = 0; i < NtStatus.NT_STATUS_CODES.length; i++)
        {
            if(status == NtStatus.NT_STATUS_CODES[i])
            {
                return NtStatus.NT_STATUS_MESSAGES[i];
            }
        }

        if(status == 71)
        {
            return "No more connections can be made to this remote computer at this time";
            /*
            return "No more connections can be made to this remote computer at this time because" +
                    " there are already as many connections as the computer can accept.";
            */
        }

        return "UNKNOWN_FAILURE";
    }

    /**
     * When using jcifs to connect smb servers directly,
     * the device name may has "/" if it's not a file.<br/>
     *
     * Use this method when you only need server name.
     *
     * @param name The connected workgroup or server name.
     * @return A name that the last character is not slash.
     */
    public static String getServerNameWithoutLastSlash(String name)
    {
        if(name.lastIndexOf("/") == name.length() - 1)
        {
            name = name.replace("/", "");
        }

        return name;
    }
}
