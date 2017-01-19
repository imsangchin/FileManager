#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This makefile shows how to build a shared library and an activity that
# bundles the shared library and calls it using JNI.
TOP_LOCAL_PATH:= $(call my-dir)

# Build activity
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += /src/com/asus/service/AccountAuthenticator/helper/IAsusAccountCallback.aidl
LOCAL_SRC_FILES += /src/com/asus/service/AccountAuthenticator/helper/IAsusAccountHelper.aidl

#LOCAL_STATIC_JAVA_LIBRARIES := cfs-api-beta21
LOCAL_STATIC_JAVA_LIBRARIES := dnsjava-2.1.6
LOCAL_STATIC_JAVA_LIBRARIES += servlet-api
#LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
#LOCAL_STATIC_JAVA_LIBRARIES += com.asus.pen
LOCAL_STATIC_JAVA_LIBRARIES += apache-ant-zip
LOCAL_STATIC_JAVA_LIBRARIES += libGoogleAnalyticsServices
LOCAL_STATIC_JAVA_LIBRARIES += java-unrar-0.3

LOCAL_STATIC_JAVA_LIBRARIES += asus-common-ui
LOCAL_RESOURCE_DIR := \
        vendor/amax-prebuilt/AsusUi/res \
        $(LOCAL_PATH)/res \
		$(LOCAL_PATH)/UserVoiceSDK/res \
		$(LOCAL_PATH)/ASUSAccount/res \
		$(LOCAL_PATH)/CloudStorage/res \
		$(LOCAL_PATH)/AZSVClib/res

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.asus.commonui
LOCAL_AAPT_FLAGS += --extra-packages com.uservoice.uservoicesdk
LOCAL_AAPT_FLAGS += --extra-packages com.asus.service.AccountAuthenticator
LOCAL_AAPT_FLAGS += --extra-packages com.asus.service.cloudstorage

LOCAL_STATIC_JAVA_LIBRARIES += AZSVClib
LOCAL_STATIC_JAVA_LIBRARIES += UserVoiceSDK
LOCAL_STATIC_JAVA_LIBRARIES += ASUSAccount
LOCAL_STATIC_JAVA_LIBRARIES += CloudStorage

LOCAL_AAPT_FLAGS += --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.asus.azs.version.checker

LOCAL_PACKAGE_NAME := FileManager2
LOCAL_CERTIFICATE := AMAX

LOCAL_OVERRIDES_PACKAGES := FileManager

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_PROGUARD_FLAGS := -include vendor/amax-prebuilt/AsusUi/proguard.flags

include $(BUILD_PACKAGE)

##################################################
#LOCAL_PRELINK_MODULE:= false
include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += /libs/cfs-api-beta21.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += /libs/dnsjava-2.1.6.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += /libs/servlet-api.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += /libs/apache-ant-zip.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += /libs/libGoogleAnalyticsServices.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += /libs/java-unrar-0.3.jar
include $(BUILD_MULTI_PREBUILT)
##################################################


# Use the following include to make our test apk.
#include $(call all-makefiles-under,$(LOCAL_PATH))

