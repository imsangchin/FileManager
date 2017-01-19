package com.asus.filemanager.functionaldirectory.hiddenzone;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
import android.util.Base64;

public final class Encryptor {

    private static IEncryptor encryptor = null;
    private static IEncryptor mockEncryptor = null;

    static void setMockEncryptor(IEncryptor encryptor) {
        mockEncryptor = encryptor;
    }

    public static IEncryptor getEncryptor() {
        if (mockEncryptor != null)
            return mockEncryptor;
        if (encryptor == null)
            encryptor = new AesEncryptor();
        return encryptor;
    }

    @SuppressLint("GetInstance")
    private static class AesEncryptor implements IEncryptor {
        private final String mKey = "HiddenZoneSecure";

        @SuppressLint("GetInstance")
        public String encode(String value) {
            SecretKeySpec spec = new SecretKeySpec(mKey.getBytes(), "AES");
            Cipher cipher;
            try {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, spec);
                String result = Base64.encodeToString(cipher.doFinal(value.getBytes()), Base64.NO_WRAP);
                return result.replace('/', '_');
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @SuppressLint("GetInstance")
        public String decode(String value) {
            SecretKeySpec spec = new SecretKeySpec(mKey.getBytes(), "AES");
            Cipher cipher;
            try {
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.DECRYPT_MODE, spec);
                return new String(cipher.doFinal(Base64.decode(value.replace('_', '/'), Base64.NO_WRAP)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public interface IEncryptor {
        String encode(String value);
        String decode(String value);
    }
}
