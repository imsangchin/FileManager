package com.asus.filemanager.samba;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.text.TextUtils;

import com.asus.filemanager.samba.util.SambaUtils;

public class SambaServer {

    private static final String SAMBAHEADER = "smb://";
    String mDomain;
    String mIp;
    static String sUsername;
    String mPassword;
    static String sUserAccountName;

    public SambaServer(String domain, String ip, String userName, String password) {
        mDomain = domain;
        mIp = ip;
        sUsername = userName;
        mPassword = password;
    }

    public String getUsername() {

        String userName = sUserAccountName;
        return SambaUtils.getServerNameWithoutLastSlash(userName);
    }

    public String getIpAddress() {
        return this.mIp;
    }

    public String getSMBURL() {
        return getUrl(mDomain, mIp, sUsername, mPassword);
    }

    private String getUrl(String domain,String ip,String userName,String password) {
        String url;
        String endSeparator = File.separator;
        // Log.d(TAG," username== " + username);

        userName = SambaUtils.getServerNameWithoutLastSlash(userName);

        sUserAccountName = userName;
        if(userName.contains(endSeparator)) {
            String[] infos = userName.split(endSeparator);
            domain = infos[0];
            userName = infos[1];
            sUserAccountName = userName;
        } else if(userName.contains("\\")) {
            String[] infos = userName.split("\\\\");
            domain = infos[0];
            userName = infos[1];
            sUserAccountName = userName;
        }

        String encodeDomain = getURLEncoder(domain);
        String encodeUserName = getURLEncoder(userName);
        String encodePassword = getURLEncoder(password);

        // New case when using jcifs to access smb server directly.
        // The url may contain domain name instead ip address.
        if(ip.contains("smb"))
        {
            if (encodePassword.length() < 1)
            {
                url = ip;
            }
            else
            {
                int beginIndex = ip.indexOf('/');
                // Remove "smb".
                ip = ip.substring(beginIndex);
                // Remove "/"
                ip = ip.replace("/", "");
                url = SAMBAHEADER + encodeUserName + ":" + encodePassword + "@" + ip + endSeparator;
            }
        }
        else
        {
            if (encodeDomain.length() > 0 && encodePassword.length() > 0)
            {
                url = SAMBAHEADER + encodeDomain + ";" + encodeUserName + ":" + encodePassword + "@" + ip + endSeparator;
            }
            else if (encodePassword.length() < 1)
            {
                url = SAMBAHEADER + ip + endSeparator;
            }
            else
            {
                url = SAMBAHEADER + encodeUserName + ":" + encodePassword + "@" + ip + endSeparator;
            }
        }

        return url;
    }

    private static String getURLEncoder(String url) {
        StringBuilder builder = new StringBuilder();
        try {
            if(!TextUtils.isEmpty(url)) {
                char[] info = url.toCharArray();
                for(int i = 0; i < info.length; i++) {
                    int asc2 = info[i];
                    if(asc2 != 32 && !isChineseChar(info[i])) {
                        String newChar = URLEncoder.encode(String.valueOf(info[i]), "utf-8");
                        builder.append(newChar);
                    } else {
                        builder.append(info[i]);
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private static boolean isChineseChar(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }
}
