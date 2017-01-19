/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os.storage;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information about a storage volume that may be mounted. A volume may be a
 * partition on a physical {@link DiskInfo}, an emulated volume above some other
 * storage medium, or a standalone container like an ASEC or OBB.
 * <p>
 * Volumes may be mounted with various flags:
 * <ul>
 * <li>{@link #MOUNT_FLAG_PRIMARY} means the volume provides primary external
 * storage, historically found at {@code /sdcard}.
 * <li>{@link #MOUNT_FLAG_VISIBLE} means the volume is visible to third-party
 * apps for direct filesystem access. The system should send out relevant
 * storage broadcasts and index any media on visible volumes. Visible volumes
 * are considered a more stable part of the device, which is why we take the
 * time to index them. In particular, transient volumes like USB OTG devices
 * <em>should not</em> be marked as visible; their contents should be surfaced
 * to apps through the Storage Access Framework.
 * </ul>
 *
 * @hide
 */
public class VolumeInfo implements Parcelable {

    public static final Creator<VolumeInfo> CREATOR = new Creator<VolumeInfo>() {
        @Override
        public VolumeInfo createFromParcel(Parcel in) {
            return new VolumeInfo();
        }

        @Override
        public VolumeInfo[] newArray(int size) {
            return new VolumeInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
    }
}
