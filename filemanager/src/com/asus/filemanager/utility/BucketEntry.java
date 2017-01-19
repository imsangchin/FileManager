package com.asus.filemanager.utility;


public class BucketEntry {
    public static int LOCALFILE = 0;
    public static int PICASAFILE = 1;

    public long lastModifiedTime;
    public int fileType;
    public int number;
    public String bucketId;
    public String bucketName;
    public String data;

    public BucketEntry(String path, String id, String name, int type) {
        data = path;
        bucketId = id;
        bucketName = (name == null ? "" : name);
        number = 0;
        fileType = type;
    }

    public BucketEntry(String id, int count, String title, String thumbnailUrl, long edited_data) {
        bucketId = id;
        number = count;
        bucketName = title;
        data = thumbnailUrl;
        lastModifiedTime = edited_data;
        fileType = PICASAFILE;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof BucketEntry)) return false;
        BucketEntry entry = (BucketEntry) object;
        return bucketId.equals(entry.bucketId);
    }
}