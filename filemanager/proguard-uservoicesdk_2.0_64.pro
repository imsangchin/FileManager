# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#rn org.apache.commons.codec.binary.Base64

-dontwarn org.apache.commons.codec.binary.Base64
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

-keepattributes Signature
-keepattributes Exceptions
-keep class com.uservoice.** { *; }
-dontwarn com.uservoice.uservoicesdk.**
-keep class retrofit.** { *; }
-dontwarn retrofit.**
-dontwarn android.google.support.**
-dontwarn com.esotericsoftware.**
-keep class com.esotericsoftware.** { *; }
-dontwarn com.squareup.**
-keep class com.squareup.** { *; }
-dontwarn de.greenrobot.**
-keep class de.greenrobot.** { *; }
-dontwarn okio.**
-keep class okio.** { *; }
-dontwarn org.objenesis.**
-keep class org.objenesis.** { *; }

-keep class org.apache.** {*;}

-keep class com.asus.push.messagemgr.bean.Message { *; }
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-dontwarn com.nostra13.universalimageloader.**
-keep class com.nostra13.universalimageloader.** {*;}

-keep class com.asus.push.**{*;}

-keepclassmembers class ** {
    public void onEvent*(**);
}

-keep enum * { *; }