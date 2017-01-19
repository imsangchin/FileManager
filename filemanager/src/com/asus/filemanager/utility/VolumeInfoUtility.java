package com.asus.filemanager.utility;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.AppCompatDrawableManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.lang.ref.WeakReference;

/**
 * Created by ChenHsin_Hsieh on 2016/5/18.
 */
public class VolumeInfoUtility {

    private WeakReference<Activity> activity;
    private String[] mStorageTitle = null;
    private TypedArray mStorageDrawable= null;
    private int iconArrayId = -1;

    private final int INTERNAL_STORAGE = 0;
    private final int MICROSD = 1;
    private final int USBDISK1 = 2;
    private final int USBDISK2 = 3;
    private final int SDREADER = 4;
    private final int USBDISK3 = 5;
    private final int USBDISK4 = 6;
    private final int USBDISK5 = 7;
    private final int USBDISK6 = 8;
    private final int USBDISK7 = 9;
    private final int USBDISK8 = 10;
    
    private static VolumeInfoUtility volumeInfoUtility;

    public static VolumeInfoUtility getInstance(Activity activity){
        if(volumeInfoUtility == null){
            volumeInfoUtility = new VolumeInfoUtility();
        }
        volumeInfoUtility.setActivity(activity);
        return volumeInfoUtility;
    }

    private VolumeInfoUtility(){

    }

    private void setActivity(Activity mActivity)
    {
        this.activity = new WeakReference<Activity>(mActivity);
    }

    public Activity getActivity()
    {
        if(activity!=null)
            return activity.get();
        return null;
    }

    public String findStorageTitleByStorageVolume(Object storageVolume) {
        String storagePath = reflectionApis.volume_getPath(storageVolume);
        return findStorageTitleByStorageVolumeAndPath(storageVolume,storagePath);
    }

    public String findStorageTitleByStorageVolumeAndPath(Object storageVolume,String storagePath) {
        if(getActivity()==null)
            return getTitleByStorageVolume(storageVolume);

        if(mStorageTitle==null)
            mStorageTitle = getActivity().getResources().getStringArray(R.array.storage_title);
        //non-asus device will happen
        if(storagePath==null)
            return getTitleByStorageVolume(storageVolume);

        if (storagePath.startsWith(WrapEnvironment.SDCARD_CANONICAL_PATH)) {
            return mStorageTitle[INTERNAL_STORAGE];
        } else if (storagePath.startsWith(WrapEnvironment.MICROSD_CANONICAL_PATH)) {
            return mStorageTitle[MICROSD];
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK1_CANONICAL_PATH)) {
            return fixUSBDiskTitle(storageVolume, mStorageTitle[USBDISK1]);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK2_CANONICAL_PATH)) {
            return fixUSBDiskTitle(storageVolume,mStorageTitle[USBDISK2]);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK3_CANONICAL_PATH)) {
            return fixUSBDiskTitle(storageVolume,mStorageTitle[USBDISK3]);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK4_CANONICAL_PATH)) {
            return fixUSBDiskTitle(storageVolume,mStorageTitle[USBDISK4]);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK5_CANONICAL_PATH)) {
            return fixUSBDiskTitle(storageVolume,mStorageTitle[USBDISK5]);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK6_CANONICAL_PATH)) {
            return fixUSBDiskTitle(storageVolume,mStorageTitle[USBDISK6]);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK7_CANONICAL_PATH)) {
            return fixUSBDiskTitle(storageVolume,mStorageTitle[USBDISK7]);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK8_CANONICAL_PATH)) {
            return fixUSBDiskTitle(storageVolume,mStorageTitle[USBDISK8]);
        } else if (storagePath.startsWith(WrapEnvironment.SDREADER_CANONICAL_PATH)) {
            return mStorageTitle[SDREADER];
        } else {
            return getTitleByStorageVolume(storageVolume);
        }
    }

    public Drawable getSVGSmallIconByStorageVolume(Object storageVolume) {
        int drawableId;
        String storagePath = reflectionApis.volume_getPath(storageVolume);
        if(getActivity()==null)
            return null;

        if(storagePath==null)
            return AppCompatDrawableManager.get().getDrawable(getActivity(), R.drawable.ic_icon_small_internal_storage);

        if (storagePath.startsWith(WrapEnvironment.SDCARD_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_small_internal_storage;
        } else if (storagePath.startsWith(WrapEnvironment.MICROSD_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_small_micro_sdcard;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK1_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK2_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK3_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK4_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK5_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK6_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK7_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK8_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.SDREADER_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_small_micro_sdcard;
        } else {
            drawableId = R.drawable.ic_icon_small_micro_sdcard;
        }
        return AppCompatDrawableManager.get().getDrawable(getActivity(), drawableId);
    }

    public Drawable getSVGIconByStorageVolume(Object storageVolume) {
        int drawableId;
        String storagePath = reflectionApis.volume_getPath(storageVolume);
        if(getActivity()==null)
            return null;

        if(storagePath==null)
            return AppCompatDrawableManager.get().getDrawable(getActivity(), R.drawable.ic_icon_internal_storage);

        if (storagePath.startsWith(WrapEnvironment.SDCARD_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_internal_storage;
        } else if (storagePath.startsWith(WrapEnvironment.MICROSD_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_micro_sdcard;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK1_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK2_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK3_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK4_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK5_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK6_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK7_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK8_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_usb_disk;
        } else if (storagePath.startsWith(WrapEnvironment.SDREADER_CANONICAL_PATH)) {
            drawableId = R.drawable.ic_icon_micro_sdcard;
        } else {
            drawableId = R.drawable.ic_icon_micro_sdcard;
        }
        return AppCompatDrawableManager.get().getDrawable(getActivity(), drawableId);
    }

    public Drawable findStorageIconByStorageVolume(Object storageVolume,int iconArrayId)
    {
        String storagePath = reflectionApis.volume_getPath(storageVolume);
        return findStorageIconByStorageVolumeAndPath(storagePath,iconArrayId);
    }

    public Drawable findStorageIconByStorageVolumeAndPath(String storagePath,int iconArrayId) {
        if(getActivity()==null)
            return null;

        if(mStorageDrawable==null || this.iconArrayId!=iconArrayId) {
            this.iconArrayId=iconArrayId;
            mStorageDrawable = getActivity().getResources().obtainTypedArray(iconArrayId);
        }
        if(storagePath==null)
            return mStorageDrawable.getDrawable(SDREADER);

        if (storagePath.startsWith(WrapEnvironment.SDCARD_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(INTERNAL_STORAGE);
        } else if (storagePath.startsWith(WrapEnvironment.MICROSD_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(MICROSD);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK1_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(USBDISK1);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK2_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(USBDISK2);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK3_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(USBDISK2);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK4_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(USBDISK2);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK5_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(USBDISK2);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK6_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(USBDISK2);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK7_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(USBDISK2);
        } else if (storagePath.startsWith(WrapEnvironment.USBDISK8_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(USBDISK2);
        } else if (storagePath.startsWith(WrapEnvironment.SDREADER_CANONICAL_PATH)) {
            return mStorageDrawable.getDrawable(SDREADER);
        } else {
            return mStorageDrawable.getDrawable(SDREADER);
        }
    }

    public String fixUSBDiskTitle(Object storageVolume,String defaultUSBDiskTitle)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            //Toast.makeText(getActivity(), "VERSION>M :" + getTitleByStorageVolume(storageVolume), Toast.LENGTH_SHORT).show();
            //M can get USB title
            return getTitleByStorageVolume(storageVolume);
        }
        return defaultUSBDiskTitle;
    }

    public String getTitleByStorageVolume(Object storageVolume)
    {

        String title = reflectionApis.volume_getMountPointTitle(storageVolume);
        String desc = reflectionApis.volume_getDescription(storageVolume,getActivity());
//        Log.i("QAQ","getTitleByStorageVolume :"+title+" , "+desc);
//        Toast.makeText(activity,"getTitleByStorageVolume :"+title+" , "+desc,Toast.LENGTH_SHORT).show();
        return (!TextUtils.isEmpty(desc) ? desc : title);
    }
    
}
