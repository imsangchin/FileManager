
package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.MimeMapUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.MsgObj;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class InfoDialogFragment extends DialogFragment implements OnClickListener {

    public static class FileInfo {

        long mNum;
        double mSize;

        public FileInfo(long num, double size) {
            mNum = num;
            mSize = size;
        }
    }

    // ++ Willie
    public static class FlashState {

        private static final int FLASH_FIRED_MASK = 1;
        private int mState;

        public FlashState(int state) {
            mState = state;
        }

        public boolean isFlashFired() {
            return (mState & FLASH_FIRED_MASK) != 0;
        }
    }

    private class CalculateRunnable implements Runnable {

        private VFile mFile;
        private boolean isStop = false;

        public CalculateRunnable(VFile file) {
            mFile = file;
        }

        public void terminate() {
            isStop = true;
        }

        @Override
        public void run() {
        	FileInfo fileInfo = new FileInfo(0, 0);
        	if (mFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE){
        		fileInfo = calculateSambaFileSize(mFile.getAbsolutePath(), new FileInfo(0, 0));
        	}else
        		fileInfo = calculateSize(mFile, new FileInfo(0, 0));

            mHandler.sendMessage(mHandler.obtainMessage(STOP_SIZE_PROGRESS));

            if (!isStop) {
                if (DEBUG) {
                    Log.d(TAG, "Finish Calculate size");
                }
                mNum = fileInfo.mNum;
                mSize = FileUtility.bytes2String(getActivity().getApplicationContext(), fileInfo.mSize, 2);
            }

            isFinish = true;
        }

        private FileInfo calculateSambaFileSize(String filePath, FileInfo fileInfo) {
        	try {
				SmbFile smbFile = new SmbFile(filePath);
				SmbFile[] files = smbFile.listFiles();
				if (files != null) {
	                for (int i = 0; i < files.length; i++) {
	                    if (!files[i].isDirectory()) {
	                        if (isStop) {
	                            break;
	                        }
	                        fileInfo.mSize = fileInfo.mSize + files[i].length();
	                        fileInfo.mNum++;
	                    } else {
	                        if (isStop) {
	                            break;
	                        }
	                        fileInfo.mSize = calculateSambaFileSize(files[i].getPath(), fileInfo).mSize;
	                    }
	                }
	            }
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SmbException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            mHandler.removeMessages(UPDATE_INFO);
            mHandler.sendMessage(mHandler.obtainMessage(UPDATE_INFO, fileInfo));
            return fileInfo;
        }

        private FileInfo calculateSize(VFile file, FileInfo fileInfo) {
            // Log.d(TAG, "calculateSize file : " + file.getAbsolutePath());
            VFile[] files = file.listVFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (!files[i].isDirectory()) {
                        if (isStop) {
                            break;
                        }
                        fileInfo.mSize = fileInfo.mSize + files[i].length();
                        fileInfo.mNum++;
                    } else {
                        if (isStop) {
                            break;
                        }
                        fileInfo.mSize = calculateSize(files[i], fileInfo).mSize;
                    }
                }
            }

            mHandler.removeMessages(UPDATE_INFO);
            mHandler.sendMessage(mHandler.obtainMessage(UPDATE_INFO, fileInfo));
            return fileInfo;
        }

    }

    // ++ Willie
    private class LocationRunnable implements Runnable {

        private double mLatitude;
        private double mLongitude;
        private boolean isStop = false;

        public LocationRunnable(double latitude, double longitude) {
            mLatitude = latitude;
            mLongitude = longitude;
        }

        public void terminate() {
            isStop = true;
        }

        @Override
        public void run() {
            Address addr = null;
            Geocoder gc = null;
            String addressText = "";

            try {
                gc = new Geocoder(getActivity(), Locale.getDefault());
                List<Address> listAddress = gc.getFromLocation(mLatitude, mLongitude, 1);
                if (isStop) {
                    throw new IOException("cancel thread");
                }
                if (listAddress.size() > 0) {
                    addr = listAddress.get(0);
                }

                if (addr != null) {
                    String parts[] = {
                            addr.getAdminArea(),
                            addr.getSubAdminArea(),
                            addr.getLocality(),
                            addr.getSubLocality(),
                            addr.getThoroughfare(),
                            addr.getSubThoroughfare(),
                            addr.getPremises(),
                            addr.getPostalCode(),
                            addr.getCountryName()
                    };

                    int len = parts.length;
                    for (int i = 0; i < len; i++) {
                        if (isStop) {
                            throw new IOException("cancel thread");
                        }
                        if (parts[i] == null || parts[i].isEmpty()) continue;
                        if (!addressText.isEmpty()) {
                            addressText += ", ";
                        }
                        addressText += parts[i];
                    }
                    mAddressText = addressText;
                }

                if (isStop) {
                    throw new IOException("cancel thread");
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            mHandler.sendMessage(mHandler.obtainMessage(UPDATE_LOCATION));

        }
    }

    public static final String TAG = "InfoDialogFragment";
    public static final boolean DEBUG = false;

    protected static final int UPDATE_INFO = 0;
    protected static final int STOP_SIZE_PROGRESS = 1;
    protected static final int UPDATE_LOCATION = 2;
    protected static final int UPDATE_REMOTE_INFO = 3;

    // ++ Willie
    // EXIF attributes
    public static final int INDEX_APERTURE = 100;
    public static final int INDEX_DATETIME = 101;
    public static final int INDEX_EXPOSURE_TIME = 102;
    public static final int INDEX_FLASH = 103;
    public static final int INDEX_FOCAL_LENGTH = 104;
    public static final int INDEX_IMAGE_HEIGHT = 105;
    public static final int INDEX_IMAGE_WIDTH = 106;
    public static final int INDEX_ISO = 107;
    public static final int INDEX_LOCATION = 108;
    public static final int INDEX_MAKER = 109;
    public static final int INDEX_MODEL = 110;
    public static final int INDEX_ORIENTATION = 111;
    public static final int INDEX_WHITE_BALANCE = 112;;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_INFO:
                    FileInfo fileInfo = (FileInfo) msg.obj;
                    if (getActivity() == null || fileInfo == null) break;
                    mSize = FileUtility.bytes2String(getActivity().getApplicationContext(), fileInfo.mSize, 2);
//        	    String h = mSize.substring(0,4);
//	            if (h.equals("0.00")||h.equals("0.0 ")){
//        		String e = mSize.substring(4,mSize.length());
//        		mSize = "0"+e;
//        	    }
                    mFileSizeView.setText(mSize);
                    mNum = fileInfo.mNum;
                    mFileNumView.setText(String.valueOf(mNum));
                    mInfoView.postInvalidate();
                    break;
                case STOP_SIZE_PROGRESS:
                    mProgressSizeView.post(new Runnable() {

                        public void run() {
                            mProgressSizeView.setVisibility(View.GONE);
                        }
                    });
                    break;
                // ++ Willie
                case UPDATE_LOCATION:
                    if (mAddressText != null) {
                        TextView addressView = (TextView) mInfoView.findViewById(R.id.info_location);
                        addressView.setText(mAddressText);
                    }
                    break;
                // update remote storage file info: file size and file number
                case UPDATE_REMOTE_INFO:
                    double size = (double) msg.arg1;
                    int count = (int) msg.arg2;
                    mSize = FileUtility.bytes2String(getActivity().getApplicationContext(), size, 2);
                    mFileSizeView.setText(mSize);
                    if (!(mFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE)) {
                    	mFileNumView.setText(String.valueOf(count));
                    }
                    mProgressSizeView.setVisibility(View.GONE);
                    mProgressFileNumView.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    };

    private View mInfoView;
    private TextView mFileSizeView;
    private TextView mFileNumView;
    private ProgressBar mProgressSizeView;
    private ProgressBar mProgressFileNumView;

    private VFile mFile;

    private boolean cancel = false;

    private long mNum = 0;
    private String mSize = "0.0 B";

    private CalculateRunnable mRunnable;
    private Thread mThread;
    private boolean isFirst = true;
    private boolean isFinish = false;

    // ++Willie
    private String mAddressText = null;
    private LocationRunnable mLocationRunnable;
    private boolean isLocationFirst = true;

    private ConnectivityManager mConnectivityManager;
    private boolean showOpenFileButton = false;

    public static InfoDialogFragment newInstance(VFile file) {
        return newInstance(file, null);
    }

    public static InfoDialogFragment newInstance(VFile file, String path) {
        InfoDialogFragment fragment = new InfoDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        if (path != null)
            args.putString("path", path);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG)
            Log.d(TAG, "onCreate");
        setRetainInstance(true);

    }

    @Override
    public void onStart() {
        super.onStart();
        if (DEBUG)
            Log.d(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG)
            Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG)
            Log.d(TAG, "onPause");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (DEBUG)
            Log.d(TAG, "onDismiss");
        if (getDialog() != null && getRetainInstance() && !cancel)
            return;
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (DEBUG)
            Log.d(TAG, "onCancel");

        cancel = true;
        if (mRunnable != null)
            mRunnable.terminate();

        if (mLocationRunnable != null)
            mLocationRunnable.terminate();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG) {
            Log.d(TAG, "onActivityCreate");
        }
        // only local storage files need thread
        if (isFirst && mFile.isDirectory() && (mFile.getVFieType() == VFileType.TYPE_LOCAL_STORAGE
        		//+++yiqiu_huang, samba file
        		|| mFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE)) {
            mRunnable = new CalculateRunnable(mFile);
            mThread = new Thread(mRunnable);
            mThread.start();
            isFirst = false;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mFile = (VFile) getArguments().getSerializable("file");

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInfoView = inflater.inflate(R.layout.dialog_info, null);

        int titleId;
        if (mFile.isDirectory()) {
            titleId = R.string.dir_info_dialog_title;
        } else {
            titleId = R.string.file_info_dialog_title;
        }

        TextView fileNameView = (TextView) mInfoView.findViewById(R.id.info_file_name);
        fileNameView.setText(mFile.getName());

        TextView filePathView = (TextView) mInfoView.findViewById(R.id.info_file_path);
        String path;
        if (getArguments().containsKey("path")) {
            path = getArguments().getString("path");
        }
        else {
            path = FileUtility.changeToSdcardPath(mFile.getAbsolutePath());
        if (path != null && path.toLowerCase().startsWith("smb://")){
            try {
                path = path.replaceAll("smb://.*@", "smb://");
            }catch (Throwable ignore){
                path = FileUtility.changeToSdcardPath(mFile.getAbsolutePath());
            }
        }
        }
        filePathView.setText(path);

        TextView filePermissionView = (TextView) mInfoView.findViewById(R.id.info_file_permission);
        filePermissionView.setText(mFile.getAttrSimple());

        TextView fileModifiedView = (TextView) mInfoView.findViewById(R.id.info_file_modified);
        java.text.DateFormat shortDateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
        java.text.DateFormat shortTimeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
        Date date = new Date(mFile.lastModified());
        String shortDate = shortDateFormat.format(date);
        fileModifiedView.setText(android.text.format.DateFormat.format("EEEE", date)+ " " + shortDate + " " + shortTimeFormat.format(date));

        mFileSizeView = (TextView) mInfoView.findViewById(R.id.info_file_size);

        mFileNumView = (TextView) mInfoView.findViewById(R.id.info_file_num);
        mProgressSizeView = (ProgressBar) mInfoView.findViewById(R.id.info_size_progress);
        mProgressFileNumView = (ProgressBar) mInfoView.findViewById(R.id.info_file_num_progress);

        if (!mFile.isDirectory()) {
            mInfoView.findViewById(R.id.info_file_num_container).setVisibility(View.GONE);
            mFileSizeView.setText(FileUtility.bytes2String(getActivity().getApplicationContext(), mFile.length(), 2));

            // ++ Willie
            String mime = reflectionApis.mediaFile_getMimeTypeForFile(mFile.getName());
            // remote storage don't support image info
            if (mime != null && mFile.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
                if (mime.startsWith("image/jpeg")) {
                    try {
                        ExifInterface exif = new ExifInterface(mFile.getAbsolutePath());
                        setExifAttributeView(exif, INDEX_APERTURE);
                        //setExifAttributeView(exif, INDEX_DATETIME);
                        setExifAttributeView(exif, INDEX_EXPOSURE_TIME);
                        setExifAttributeView(exif, INDEX_FLASH);
                        setExifAttributeView(exif, INDEX_FOCAL_LENGTH);
                        setExifAttributeView(exif, INDEX_IMAGE_HEIGHT);
                        setExifAttributeView(exif, INDEX_IMAGE_WIDTH);
                        setExifAttributeView(exif, INDEX_ISO);
                        setExifAttributeView(exif, INDEX_LOCATION);
                        setExifAttributeView(exif, INDEX_MAKER);
                        setExifAttributeView(exif, INDEX_MODEL);
                        setExifAttributeView(exif, INDEX_ORIENTATION);
                        setExifAttributeView(exif, INDEX_WHITE_BALANCE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (mime.startsWith("image/")){
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
                    if (options.outHeight > 0 && options.outWidth > 0) {
                        TextView heightView = (TextView) mInfoView.findViewById(R.id.info_image_height);
                        heightView.setText(String.valueOf(options.outHeight));
                        //mInfoView.findViewById(R.id.info_image_height_container).setVisibility(View.VISIBLE);
                        TextView widthView = (TextView) mInfoView.findViewById(R.id.info_image_width);
                        widthView.setText(String.valueOf(options.outWidth));
                        mInfoView.findViewById(R.id.info_image_width_container).setVisibility(View.VISIBLE);
                    }
                }
            }

            mProgressSizeView.setVisibility(View.GONE);
        } else {
            if (mFile.getVFieType() == VFileType.TYPE_REMOTE_STORAGE) {
                // set default value is 0, then query the remote storage detailed info
                mFileSizeView.setText(mSize);
                mFileNumView.setText(String.valueOf(mNum));
                mProgressSizeView.setVisibility(View.VISIBLE);
                mProgressFileNumView.setVisibility(View.VISIBLE);

                ShortCutFragment shortcutFragment = (ShortCutFragment) getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
                shortcutFragment.sendRemoteStorage(mFile, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_INFO);
            } else if (mFile.getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                // set default value is 0, then query the remote storage detailed info
//                mFileSizeView.setText(mSize);
//                mFileNumView.setText(String.valueOf(mNum));
//                mProgressSizeView.setVisibility(View.VISIBLE);
//                mProgressFileNumView.setVisibility(View.VISIBLE);
                mFileSizeView.setText("N/A");
                mFileNumView.setText("N/A");
               // mProgressSizeView.setVisibility(View.INVISIBLE);
                //mProgressFileNumView.setVisibility(View.INVISIBLE);
                //fixed felix_zhang only the skydrive has the folder size;
                if (((RemoteVFile)mFile).getMsgObjType()==MsgObj.TYPE_SKYDRIVE_STORAGE) {
                	   mProgressSizeView.setVisibility(View.VISIBLE);
                       ShortCutFragment shortcutFragment = (ShortCutFragment) getFragmentManager().findFragmentByTag(FileManagerActivity.sShortCutFragmentTag);
                       shortcutFragment.sendCloudStorage(mFile, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_FILE_INFO);
				}else {
					 mProgressSizeView.setVisibility(View.GONE);
				}
                mProgressFileNumView.setVisibility(View.GONE);

            }else {
//               String h = mSize.substring(0,4);
//               if (h.equals("0.00")||h.equals("0.0 ")){
//                   String e = mSize.substring(4,mSize.length());
//                   mSize = "0"+e;
//                }
                mFileSizeView.setText(mSize);
                mFileNumView.setText(String.valueOf(mNum));

                if (isFinish) {
                    mProgressSizeView.setVisibility(View.GONE);
                }
            }
        }

        LinearLayout dialogContainer = (LinearLayout)mInfoView.findViewById(R.id.info_dialog_container);
        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

        dialogContainer.setPadding(spacing_left, spacing_top, spacing_right, spacing_bottom);


        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                //.setIcon(MimeMapUtility.getIconRes(mFile))
                .setTitle(titleId)
                .setPositiveButton(android.R.string.ok, this).create();

        if(showOpenFileButton)
            dialog.setButton(DialogInterface.BUTTON_NEUTRAL,getString(R.string.dm_openfile), this);

        //dialog.setView(mInfoView, spacing_left, spacing_top, spacing_right, spacing_bottom);
        dialog.setView(mInfoView);

        GaAccessFile.getInstance().sendEvents(getActivity(),
                GaAccessFile.ACTION_INFORMATION, mFile.getVFieType(), -1, 1);
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if(which==DialogInterface.BUTTON_NEUTRAL)
        {
            //open file
            FileUtility.openFile(getActivity(),mFile,false,false);
        }

        onCancel(dialog);

    }

    // ++ Willie
    public void setExifAttributeView(ExifInterface exif, int tag) {
        String value;
        TextView exifAttributeView;
        switch (tag) {
            case INDEX_APERTURE:
                value = exif.getAttribute(ExifInterface.TAG_APERTURE);
                if (value != null) {
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_aperture);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_aperture_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_DATETIME:
                value = exif.getAttribute(ExifInterface.TAG_DATETIME);
                if (value != null) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_datetime);
                    try {
                        exifAttributeView.setText(DateFormat.getDateTimeInstance(
                                DateFormat.FULL, DateFormat.SHORT).format(
                                dateFormat.parse(value)));
                        mInfoView.findViewById(R.id.info_datetime_container).setVisibility(View.VISIBLE);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case INDEX_EXPOSURE_TIME:
                value = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
                if (value != null) {
                    double time = Double.valueOf(value);
                    if (time > 0.0f) {
                        if (time < 1.0f) {
                            value = String.format("1/%d", (int) (0.5f + 1 / time));
                        } else {
                            int integer = (int) time;
                            time -= integer;
                            value = String.valueOf(integer) + "''";
                            if (time > 0.0001) {
                                value += String.format(" 1/%d", (int) (0.5f + 1 / time));
                            }
                        }
                        exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_exposure_time);
                        exifAttributeView.setText(value);
                        mInfoView.findViewById(R.id.info_exposure_time_container).setVisibility(View.VISIBLE);
                    }
                }
                break;
            case INDEX_FLASH:
                value = exif.getAttribute(ExifInterface.TAG_FLASH);
                if (value != null) {
                    FlashState state = new FlashState(Integer.valueOf(value));
                    if (state.isFlashFired()) {
                        value = getString(R.string.flash_on);
                    } else {
                        value = getString(R.string.flash_off);
                    }
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_flash);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_flash_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_FOCAL_LENGTH:
                double fLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0);
                if (fLength != 0f) {
                    value = String.format("%s %s", fLength, getString(R.string.unit_mm));
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_focal_length);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_focal_length_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_IMAGE_HEIGHT:
                value = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
                if (value != null) {
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_image_height);
                    exifAttributeView.setText(value);
                    //mInfoView.findViewById(R.id.info_image_height_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_IMAGE_WIDTH:
                value = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                if (value != null) {
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_image_width);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_image_width_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_ISO:
                value = exif.getAttribute(ExifInterface.TAG_ISO);
                if (value != null) {
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_iso);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_iso_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_LOCATION:
                float[] latlng = new float[2];
                if (exif.getLatLong(latlng)) {
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_location);
                    if (mAddressText == null) {
                        value = String.format("(%f,%f)", latlng[0], latlng[1]);
                        exifAttributeView.setText(value);
                    } else {
                        exifAttributeView.setText(mAddressText);
                    }
                    mInfoView.findViewById(R.id.info_location_container).setVisibility(View.VISIBLE);

                    if (isLocationFirst) {
                        mConnectivityManager = (ConnectivityManager)
                                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
                        if (networkInfo != null && networkInfo.isConnected()) {
                            mLocationRunnable = new LocationRunnable((double)latlng[0], (double)latlng[1]);
                            mThread = new Thread(mLocationRunnable);
                            mThread.start();
                            isLocationFirst = false;
                        }
                    }
                }
                break;
            case INDEX_MAKER:
                value = exif.getAttribute(ExifInterface.TAG_MAKE);
                if (value != null) {
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_maker);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_maker_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_MODEL:
                value = exif.getAttribute(ExifInterface.TAG_MODEL);
                if (value != null) {
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_model);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_model_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_ORIENTATION:
                value = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                if (value != null) {
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_orientation);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_orientation_container).setVisibility(View.VISIBLE);
                }
                break;
            case INDEX_WHITE_BALANCE:
                value = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
                if (value != null) {
                    value = value.equals("1") ? getString(R.string.manual) : getString(R.string.auto);
                    exifAttributeView = (TextView) mInfoView.findViewById(R.id.info_white_balance);
                    exifAttributeView.setText(value);
                    mInfoView.findViewById(R.id.info_white_balance_container).setVisibility(View.VISIBLE);
                }
                break;
            default:
                break;
        }
    }

    // +++ Johnson, for remote storage
    public void updateRemoteFileInfo(int size, int count) {
        mHandler.sendMessage(mHandler.obtainMessage(UPDATE_REMOTE_INFO, size, count));
    }
    // ---
    public void setShowOpenFileButton(boolean isShow)
    {
        showOpenFileButton = isShow;
    }
}
