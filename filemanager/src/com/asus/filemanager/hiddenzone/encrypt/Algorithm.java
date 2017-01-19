package com.asus.filemanager.hiddenzone.encrypt;

/**
 * Created by olivier.goutay on 4/15/16.
 */
public enum Algorithm {

    SHA1("1"), SHA256("2");

    private String mValue;

    Algorithm(String value) {
        this.mValue = value;
    }

    public String getValue() {
        return mValue;
    }

    public static Algorithm getAlgorithm() {
        return SHA256; // By default we choose SHA256 to encrypt
    }
}
