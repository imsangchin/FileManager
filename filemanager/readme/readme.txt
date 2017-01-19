
源碼編譯請保持目錄結構，配置Android.mk



Eclipse運行：

1.導入目錄下所有項目，其他項目作為 library add 進FileManager

2.添加 FileManager/JAR/android.jar 作為 UserLibrary(system library) 進FileManager

3.由於AsusAccount項目中已有aidl，故刪除FileManager/src/com/asus/service目錄下.aidl档案