package com.asus.filetransfer.http;

/**
 * Created by Yenju_Lai on 2015/9/7.
 */
public class HttpConstants {
    public static String HTTP_MIME_TYPE_FILE = "application/octet-stream";
    public static String HTTP_MIME_TYPE_DIRECTORY = "text/directory";
    public static String HTTP_MIME_TYPE_PLAINTEXT = "text/plain";
    public static String HTTP_MIME_TYPE_ZIP = "application/zip";
    public static String HTTP_MIME_TYPE_HTML = "text/html";
    public static String HTTP_MIME_TYPE_CSS = "text/css";
    public static String HTTP_MIME_TYPE_SVG = "image/svg+xml";
    public static String HTTP_MIME_TYPE_JAVASCRIPT = "text/javascript";
    public static String HTTP_SUPPORT_RANGE = "bytes=";
    public static String HTTP_ACCEPT_RANGE_VALUE = "bytes";
    public static String HTTP_ACCESS_CONTROL_ALLOW_ORIGIN_ALL = "*";
    public static String HTTP_CONNECTION_CLOSE = "close";
    public static String HTTP_CONTENT_DISPOSITION_PREFIX = "attachment; filename=";
    public static String HTTP_CONTENT_RANGE_PREFIX = "bytes ";
    public static String HTTP_CONTENT_TRANSFER_ENCODING_VALUE = "binary";

    public enum HttpHeaderField {

        ACCESS_CONTROL_ALLOW_ORIGIN("access-control-allow-origin"),
        ACCEPT_RANGES("accept-ranges"),
        CONNECTION("connection"),
        CONTENT_DISPOSITION("content-disposition"),
        CONTENT_LENGTH("content-length"),
        CONTENT_RANGE("content-range"),
        CONTENT_TRANSFER_ENCODING("content-transfer-encoding"),
        CONTENT_TYPE("content-type"),
        CUSTOM_FILE_ID("custom-file-id"),
        CUSTOM_FILE_CANCELLED("custom-file-cancelled"),
        CUSTOM_SESSION_ID("custom-session-id"),
        RANGE("range");

        private String field;

        HttpHeaderField(String s) {
            field = s;
        }

        /*public static int count() {
            return values().length;
        }*/

        @Override
        public String toString() {
            return field;
        }
    }
}
