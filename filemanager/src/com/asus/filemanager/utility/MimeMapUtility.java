package com.asus.filemanager.utility;

import com.asus.filemanager.R;

import android.webkit.MimeTypeMap;

import java.util.HashMap;

public class MimeMapUtility {
    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------

    /**
     * Members of this class are mainly defined for sorting file type.
     */
    public static final class FileType {
        public static final int TYPE_DOCUMENT = 1;
        public static final int TYPE_AUDIO = 2;
        public static final int TYPE_VIDEO = 3;
        public static final int TYPE_PLAYLIST = 4;
        public static final int TYPE_IMAGE = 5;
        public static final int TYPE_BOOK = 6;
        public static final int TYPE_ZIP = 7;
        public static final int TYPE_RAR = 8;
        public static final int TYPE_ABU = 9;
        public static final int TYPE_APK = 10;
        public static final int TYPE_FILE = 11;
        public static final int TYPE_DIR = 12;
    }

    /**
     * Class MappingValues is the value part defined for sMimeMap.
     * With a key string of MIME type, there are corresponding file type and
     * resource ID we could get from MappingValues.
     */
    public static final class MappingValues {
        public final int fileType;
        public final int resId;
        public final int stringId;

        public MappingValues(int type, int res, int string) {
            this.fileType = type;
            this.resId = res;
            this.stringId = string;
        }
    }

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static HashMap<String, MappingValues> sMimeMap
            = new HashMap<String, MappingValues>();

    // ------------------------------------------------------------------------
    // STATIC INITIALIZERS
    // ------------------------------------------------------------------------
    static {
        // --- MediaFile defined MIME types ---
        // Audio type
        addMimeType("audio/mpeg", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/mp4", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/wav", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/x-wav", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/amr", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/amr-wb", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/x-ms-wma", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("application/ogg", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/aac", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/aac-adts", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/mp4a-latm", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/x-matroska", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/midi", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/sp-midi", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/imelody", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
        addMimeType("audio/flac", FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);

        // Video type
        addMimeType("video/mpeg", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/mp4", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/3gpp", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/3gpp2", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/x-matroska", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/webm", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/mp2ts", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/avi", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/x-msvideo", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/quicktime", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/x-ms-wmv", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/x-ms-asf", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/mp2p", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("application/x-android-drm-fl", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
        addMimeType("video/vnd.rn-realvideo", FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);

        // Image type
        addMimeType("image/jpeg", FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);
        addMimeType("image/x-mpo", FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);
        addMimeType("image/x-jps", FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);
        addMimeType("image/gif", FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);
        addMimeType("image/png", FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);
        addMimeType("image/x-ms-bmp", FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);
        addMimeType("image/vnd.wap.wbmp", FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);
        addMimeType("image/webp", FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);

        // Play-list type
        addMimeType("audio/x-mpegurl", FileType.TYPE_PLAYLIST, R.drawable.asus_ic_playlist, R.string.type_audio);
        addMimeType("application/x-mpegurl", FileType.TYPE_PLAYLIST, R.drawable.asus_ic_playlist, R.string.type_audio);
        addMimeType("audio/x-scpls", FileType.TYPE_PLAYLIST, R.drawable.asus_ic_playlist, R.string.type_audio);
        addMimeType("application/vnd.ms-wpl", FileType.TYPE_PLAYLIST, R.drawable.asus_ic_playlist, R.string.type_audio);
        addMimeType("application/vnd.apple.mpegurl", FileType.TYPE_PLAYLIST, R.drawable.asus_ic_playlist, R.string.type_audio);
        addMimeType("audio/mpegurl", FileType.TYPE_PLAYLIST, R.drawable.asus_ic_playlist, R.string.type_audio);

        // Document type
        addMimeType("text/plain", FileType.TYPE_DOCUMENT, R.drawable.asus_ep_ic_txt, R.string.type_document);
        addMimeType("text/html", FileType.TYPE_DOCUMENT, R.drawable.asus_ep_ic_file, R.string.type_document);
        addMimeType("application/pdf", FileType.TYPE_DOCUMENT, R.drawable.asus_ep_ic_pdf, R.string.type_document);
        addMimeType("application/msword", FileType.TYPE_DOCUMENT, R.drawable.asus_ep_ic_word, R.string.type_document);
        addMimeType("application/vnd.ms-excel", FileType.TYPE_DOCUMENT, R.drawable.asus_ep_ic_excel, R.string.type_document);
        addMimeType("application/vnd.ms-powerpoint", FileType.TYPE_DOCUMENT, R.drawable.asus_ep_ic_ppt, R.string.type_document);
        addMimeType("application/mspowerpoint", FileType.TYPE_DOCUMENT, R.drawable.asus_ep_ic_ppt, R.string.type_document);
		addMimeType("application/vnd.openxmlformats-officedocument.presentationml.presentation", FileType.TYPE_DOCUMENT, R.drawable.asus_ep_ic_ppt, R.string.type_document);
		
        // Book type
        addMimeType("application/epub+zip", FileType.TYPE_BOOK, R.drawable.asus_ep_ic_book, R.string.type_book);
        addMimeType("application/vnd.adobe.adept+xml", FileType.TYPE_BOOK, R.drawable.asus_ep_ic_book, R.string.type_book);
        addMimeType("application/octet-stream", FileType.TYPE_BOOK, R.drawable.asus_ep_ic_book, R.string.type_book);
        addMimeType("application/vnd.palm", FileType.TYPE_BOOK, R.drawable.asus_ep_ic_book, R.string.type_book);

        // Zip type
        addMimeType("application/zip", FileType.TYPE_ZIP, R.drawable.asus_ep_ic_zip, R.string.type_zip);
        addMimeType("application/rar", FileType.TYPE_RAR, R.drawable.asus_ep_ic_rar, R.string.type_rar);

        // ABU type
        addMimeType("application/vnd.asus-appbackup", FileType.TYPE_ABU, R.drawable.asus_ep_ic_abu, R.string.type_abu);

        // File type (General type)
        addMimeType("application/x-mspublisher", FileType.TYPE_FILE, R.drawable.asus_ep_ic_file, R.string.type_file);
        addMimeType("message/rfc822", FileType.TYPE_FILE, R.drawable.asus_ep_ic_file, R.string.type_file);

        // --- Not MediaFile defined MIME types ---
        // APK type
        addMimeType("application/vnd.android.package-archive", FileType.TYPE_APK, R.drawable.asus_ep_ic_apk, R.string.type_apk);
    }

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------
    private static void addMimeType(String mimeType, int fileType, int resId, int stringId) {
        sMimeMap.put(mimeType, new MappingValues(fileType, resId, stringId));
    }

    public static MappingValues getMappingValues(String fileName, String extension, boolean isFolder) {
        if (isFolder)
            return new MappingValues(FileType.TYPE_DIR, R.drawable.asus_ep_ic_folder, R.string.type_directory);

        String mimeType = reflectionApis.mediaFile_getMimeTypeForFile(fileName);
        MappingValues mapValues = null;

        if (mimeType != null) {
            mapValues = sMimeMap.get(mimeType);
        } else {
            // Get the MIME type from MimeTypeMap if it dosen't defined in MediaFile
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType == null) {
                mapValues = new MappingValues(FileType.TYPE_FILE, R.drawable.asus_ep_ic_file, R.string.type_file);
            } else {
                // Check the map once again if MIME type is defined in MimeTypeMap
                // MIME types those are new added to maps but not defined in MediaFile could be checked
                // For example, APK type is parsed here
                mapValues = sMimeMap.get(mimeType);
                if (mapValues != null) {
                    return mapValues;
                }

                // For those MimeTypeMap defined MIME types, we simply
                // classify them by the prefix of MIME type
                if (mimeType.startsWith("image/")){
                mapValues = new MappingValues(FileType.TYPE_IMAGE, R.drawable.asus_ep_ic_photo, R.string.type_image);
                } else if (mimeType.startsWith("audio/")) {
                    mapValues = new MappingValues(FileType.TYPE_AUDIO, R.drawable.asus_ep_ic_music, R.string.type_audio);
                } else if (mimeType.startsWith("video/")) {
                    mapValues = new MappingValues(FileType.TYPE_VIDEO, R.drawable.asus_ep_ic_movie, R.string.type_video);
                } else {
                    mapValues = new MappingValues(FileType.TYPE_FILE, R.drawable.asus_ep_ic_file, R.string.type_file);
                }
            }
        }
        if (mapValues == null) {
            mapValues = new MappingValues(FileType.TYPE_FILE, R.drawable.asus_ep_ic_file, R.string.type_file);
        }
        return mapValues;
    }

    public static int getFileType(VFile file) {
        return getMappingValues(file.getName(), file.getExtensiontName(), file.isDirectory()).fileType;
    }

    public static int getIconRes(VFile file) {
        return getMappingValues(file.getName(), file.getExtensiontName(), file.isDirectory()).resId;
    }

    public static int getIconRes(UnZipPreviewData data) {
        return getMappingValues(data.getName(), data.getExtensionName(), data.isFolder()).resId;
    }

    public static int getTypeRes(VFile file) {
        return getMappingValues(file.getName(), file.getExtensiontName(), file.isDirectory()).stringId;
    }

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------

}
