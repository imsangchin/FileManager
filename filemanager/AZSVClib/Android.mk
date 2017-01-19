LOCAL_PATH:= $(call my-dir)

##################################################
# build resource first
##################################################
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := AZSVClib-res
LOCAL_CERTIFICATE := AMAX

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res/

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_PATH := $(TARGET_OUT_JAVA_LIBRARIES)
LOCAL_EXPORT_PACKAGE_RESOURCES := true

include $(BUILD_PACKAGE)
##################################################
# build library
##################################################
include $(CLEAR_VARS)

library_res_source_path := APPS/AZSVClib-res_intermediates/src/

LOCAL_MODULE := AZSVClib
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := libAZSVC

LOCAL_INTERMEDIATE_SOURCES := \
    $(library_res_source_path)/com/asus/azs/version/checker/R.java

include $(BUILD_STATIC_JAVA_LIBRARY)
##################################################
# build jar into library
##################################################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libAZSVC:/libs/AZCSVersionChecker_150113.jar

include $(BUILD_MULTI_PREBUILT)
##################################################
