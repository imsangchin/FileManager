package com.asus.filemanager.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.AddCloudAccountActivity;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.FileManagerActivity.FontType;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.activity.ShortCutFragment;
import com.asus.filemanager.dialog.FilePickerDialogFragment;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.ga.GaCloudStorage;
import com.asus.filemanager.ga.GaMoveToDialog;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.utility.VolumeInfoUtility;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.RemoteAccountUtility;
import com.asus.remote.utility.RemoteAccountUtility.AccountInfo;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;
import com.asus.service.cloudstorage.common.MsgObj;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
//import com.asus.filemanager.adapter.FolderTreeAdapter.StorageElement;

public class MoveToNaviAdapter extends BaseAdapter implements
        OnItemClickListener {
    private static final String TAG = "MoveToNaviAdapter";

    private static final int STORAGETYPE_TITLE = 0;
    private static final int STORAGETYPE_LOCAL = 1;
    private static final int STORAGETYPE_CLOUD = 2;
    private static final int STORAGETYPE_ADD_GOOGLE_ACCOUNT = 3;
    private static final int STORAGETYPE_NETWORK = 4;
    private static final int STORAGETYPE_ADD_CLOUD_ACCOUNT = 5;
    private static final int STORAGETYPE_SHORTCUT_PREVIOUS_FOLDER = 6;
    private static final int STORAGETYPE_SHORTCUT_CURRENT_FOLDER = 7;

//    private static final int INTERNAL_STORAGE = 0;
//    private static final int MICROSD = 1;
//    private static final int USBDISK1 = 2;
//    private static final int USBDISK2 = 3;
//    private static final int SDREADER = 4;

    private int mMode = 0;
    private boolean mIsSupportPreviousFolder = false;
    private boolean mIsSupportCurrentFolder = false;
    private boolean mIsSupportLocalStorage = false;
    private boolean mIsSupportSambaStorage = false;
    private boolean mIsSupportCloudStorage = false;

    public static class StorageItemElement {
        /**** Local storage ****/
        public Object storageVolume;
        public VFile vFile;
        /**** Local storage ****/

        public String storageTitle;

        public AccountInfo acountInfo;
        public int storageType;
        public int cloudStorageIndex;
    }

    private LinkedList<StorageItemElement> mStorageItemElementList = new LinkedList<StorageItemElement>();
    private ArrayList<Object> mLocalStorageElementList = new ArrayList<Object>();
    private ArrayList<VFile> mLocalStorageFile = new ArrayList<VFile>();
    private static ArrayList<AccountInfo> mConnectedCloudStorageAccountList;

//    private String[] mStorageTitle;
    private String[] mNetwork_StorageTitle;
    private String[] mCloud_StorageTitle;

//    private TypedArray mStorageDrawable;
    // private TypedArray mCloudStorageDrawable;
    private Drawable[] mNetworkStorageDrawable;
    private Drawable[] mCloudStorageDrawable;
    private FileManagerActivity mActivity;
    private String mSelectedStorage;
    private VFile mSelectedStorageFile;
    // public boolean mIsAttachOp = false;
    // public boolean mIsMultipleSelectionOp = false;
    private VFile mCurrentFolder;

    private class ViewHolder {
        View container;
        ImageView icon;
        TextView name;
        TextView account_name;
        View divider;
        View addGoogleAccountPadding;
    }

    public class CloudStorageIndex {
        public static final int NETWORK_PLACE = 0;
        public static final int ASUS_HOMEBOX = 1;
        public static final int ASUS_WEBSTORAGE = 2;
        public static final int DROPBOX = 3;
        public static final int BAIDUPCS = 4;
        public static final int SKYDRIVE = 5;
        public static final int GOOGLDRIVE = 6;
        public static final int YANDEX = 7;

    }

    public class ClickIconItem {
        public static final int LOCAL_STORAGE = 0;
        public static final int CLOUD_STORAGE = 1;
        public static final int ADD_GOOGLE_ACCOUNT = 2;
        public static final int NETWORK_STORAGE = 3;
        public static final int ADD_CLOUD_ACCOUNT = 4;

        int itemtype;
        int index;
        int cloudStorageIndex;
        VFile file;
        String storageName;
        boolean mounted;
        AccountInfo acountInfo;

        public int getItemType() {
            return itemtype;
        }

        private int getIndex() {
            return index;
        }

        private int getCloudStorageIndex() {
            return cloudStorageIndex;
        }

        public boolean getMounted() {
            return mounted;
        }

        public String getStorageName() {
            return storageName;
        }

        public AccountInfo getAccountInfo() {
            return acountInfo;
        }

        public VFile getFile() {
            return file;
        }
    }

    public Drawable[] getDrawablesFromTypeArray(TypedArray array) {
        int total = array.length();
        Drawable[] drawables = new Drawable[total];
        for (int i = 0; i < total; i++) {
            drawables[i] = array.getDrawable(i);
        }
        return drawables;
    }

    public void updateAvailableCloudsTitle(String[] titles, Drawable[] drawables) {
        this.mCloud_StorageTitle = titles;
        this.mCloudStorageDrawable = drawables;
        updateStorageItemElementList();
    }

    public MoveToNaviAdapter(FileManagerActivity activity) {
        mActivity = activity;
//        mStorageTitle = activity.getResources().getStringArray(R.array.storage_title);
//        mStorageDrawable = activity.getResources().obtainTypedArray(R.array.dialog_storage_icon);
        mNetwork_StorageTitle = activity.getResources().getStringArray(R.array.network_storage_title);
        mNetworkStorageDrawable = getDrawablesFromTypeArray(activity.getResources().obtainTypedArray(R.array.network_storage_icon));
        mNetworkStorageDrawable[0] = activity.getResources().getDrawable(R.drawable.dialog_asus_ic_network);
        mCloud_StorageTitle = activity.getResources().getStringArray(R.array.cloud_storage_title);
        mCloudStorageDrawable = getDrawablesFromTypeArray(activity.getResources().obtainTypedArray(R.array.dialog_cloud_storage_icon));

        mConnectedCloudStorageAccountList = new ArrayList<AccountInfo>();

        final StorageManager mStorageManager = (StorageManager) activity
                .getSystemService(Context.STORAGE_SERVICE);
        ArrayList<Object> storageVolume;

        storageVolume = ((FileManagerApplication) activity.getApplication())
                .getStorageVolume();
        VFile[] tmpVFiles = ((FileManagerApplication) mActivity
                .getApplication()).getStorageFile();
        for (int i = 0; i < storageVolume.size(); i++) {
            if (mStorageManager != null
                    && reflectionApis
                            .getVolumeState(mStorageManager, storageVolume.get(i)).equals(Environment.MEDIA_MOUNTED)) {
                mLocalStorageElementList.add(storageVolume.get(i));
                mLocalStorageFile.add(tmpVFiles[i]);
            }

        }

    }

    @Override
    public int getCount() {
        return mStorageItemElementList.size();
    }

    @Override
    public Object getItem(int position) {
        // return (mStorageElementList == null) ? null :
        // mStorageElementList.get(position);
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        ClickIconItem iconItem = new ClickIconItem();
        StorageItemElement storageItemElement = new StorageItemElement();
        if (convertView == null) {
            convertView = mActivity.getLayoutInflater().inflate(
                    R.layout.storage_list_item, null);
            holder = new ViewHolder();
            holder.container = (View) convertView
                    .findViewById(R.id.storage_list_item_container);
            holder.icon = (ImageView) convertView
                    .findViewById(R.id.storage_list_item_icon);
            holder.name = (TextView) convertView
                    .findViewById(R.id.storage_list_item_name);
            holder.account_name = (TextView) convertView
                    .findViewById(R.id.storage_list_item_account_name);
            holder.divider = (View) convertView
                    .findViewById(R.id.storage_list_item_divider);
            holder.addGoogleAccountPadding = (View) convertView
                    .findViewById(R.id.add_google_account_padding);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.container.setVisibility(View.VISIBLE);
            holder.addGoogleAccountPadding.setVisibility(View.GONE);
            // holder.name.setTypeface(null);
        }

        storageItemElement = mStorageItemElementList.get(position);
        if (storageItemElement.storageType == STORAGETYPE_TITLE) {
            String storage_type_cloud = ConstantsUtil.IS_AT_AND_T ? mActivity
                    .getResources().getString(R.string.storage_type_cloud_att)
                    : mActivity.getResources().getString(
                            R.string.storage_type_cloud);
            holder.icon.setVisibility(View.GONE);
            setTextFontAndSize(holder.name,
                    R.dimen.storage_list_storage_title_name_size,
                    FontType.ROBOTO_REGULAR);
            holder.name.setText(storageItemElement.storageTitle);
            holder.divider.setVisibility(View.VISIBLE);
            holder.account_name.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = holder.container.getLayoutParams();
            if (storageItemElement.storageTitle.endsWith(mActivity
                    .getResources().getString(R.string.storage_type_shortcut))) {
                params.height = (int) mActivity.getResources().getDimension(
                        R.dimen.storage_list_storage_local_title_height);
            } else {
                params.height = (int) mActivity.getResources().getDimension(
                        R.dimen.storage_list_height);
            }
            holder.container.setLayoutParams(params);
            ((LinearLayout) holder.container).setGravity(Gravity.BOTTOM);
            // }
        } else if (storageItemElement.storageType == STORAGETYPE_SHORTCUT_PREVIOUS_FOLDER) {
            holder.icon.setVisibility(View.VISIBLE);
            holder.divider.setVisibility(View.GONE);
            holder.account_name.setVisibility(View.VISIBLE);
            iconItem.index = position;
            ItemOperationUtility itemOperationUtility = ItemOperationUtility.getInstance();
            int lastFileType = itemOperationUtility.getMoveToLastFileType(mActivity);
            if (lastFileType != -1) {
                if (lastFileType == VFileType.TYPE_LOCAL_STORAGE) {
                    iconItem.itemtype = ClickIconItem.LOCAL_STORAGE;

                    String lastPath = itemOperationUtility.getMoveToLastPath(mActivity);

                    if (lastPath != null & new VFile(lastPath).exists()) {
                        iconItem.file = new VFile(lastPath);
                    } else {
                        lastPath = FileUtility.changeToSdcardPath(
                                Environment.getExternalStorageDirectory().toString());
                        iconItem.file = new VFile(lastPath);
                    }
                    holder.account_name.setText(lastPath);
                } else if (lastFileType == VFileType.TYPE_CLOUD_STORAGE) {
                    // since we don't support multiple files move to cloud storage,
                    // when previous folder is indicate to cloud storage,
                    // we will switch to external storage.
                    if (mMode == MoveToDialogFragment.MODE_MULTIPLE_MOVE_TO_IN_CATEGORY_TOP
                            || mMode == MoveToDialogFragment.MODE_HIDDEN_ZONE) {
                        iconItem.itemtype = ClickIconItem.LOCAL_STORAGE;

                        String lastPath = FileUtility.changeToSdcardPath(
                                Environment.getExternalStorageDirectory().getAbsolutePath());

                        if (lastPath != null & new VFile(lastPath).exists()) {
                            iconItem.file = new VFile(lastPath);
                        } else {
                            iconItem.file = null;
                        }
                        holder.account_name.setText(lastPath);
                    } else {
                        int type = Integer.valueOf(itemOperationUtility.getMoveToLastPath(mActivity));
                        String accountName = itemOperationUtility.getMoveToLastAccount(mActivity);
                        String title = RemoteAccountUtility.getInstance(mActivity).findCloudTitleByMsgObjType(mActivity, type);
                        int index = findStorageIndexByTitle(mActivity, title);
                        iconItem.itemtype = ClickIconItem.CLOUD_STORAGE;
                        iconItem.storageName = title;
                        iconItem.mounted = true;
                        iconItem.cloudStorageIndex = index;
                        holder.account_name.setText(title + " (" + accountName + ")");
                        ArrayList<AccountInfo> accountInfoList = getAccountInfo(mActivity, title);
                        for (AccountInfo accountInfo : accountInfoList) {
                            if (accountName.equals(accountInfo.getAccountName())) {
                                iconItem.acountInfo = accountInfo;
                            }
                        }

                        if (iconItem.acountInfo == null) {
                            iconItem.mounted = false;
                        }
                    }
                } else if (lastFileType == VFileType.TYPE_SAMBA_STORAGE) {
                    // since we don't support multiple files move to samba storage,
                    // when previous folder is indicate to samba storage,
                    // we will switch to external storage.
                    if (mMode == MoveToDialogFragment.MODE_MULTIPLE_MOVE_TO_IN_CATEGORY_TOP) {
                        iconItem.itemtype = ClickIconItem.LOCAL_STORAGE;

                        String lastPath = FileUtility.changeToSdcardPath(
                                Environment.getExternalStorageDirectory().getAbsolutePath());

                        if (lastPath != null & new VFile(lastPath).exists()) {
                            iconItem.file = new VFile(lastPath);
                        } else {
                            iconItem.file = null;
                        }
                        holder.account_name.setText(lastPath);
                    } else {
                        String title = mActivity.getResources().getString(R.string.networkplace_storage_title);
                        iconItem.itemtype = ClickIconItem.NETWORK_STORAGE;
                        iconItem.storageName = title;
                        iconItem.mounted = true;
                        iconItem.cloudStorageIndex = 0;
                        holder.account_name.setText(title);
                    }
                }
            }
            holder.name.setText(mActivity.getResources().getString(R.string.previous_folder));
            holder.icon.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.asus_ic_previous_folder_b));
            setTextFontAndSize(holder.name,
                    R.dimen.storage_list_name_size, FontType.ROBOTO_LIGHT);
            setTextFontAndSize(holder.account_name,
                    R.dimen.storage_list_connected_name_size, FontType.ROBOTO_LIGHT);
        } else if (storageItemElement.storageType == STORAGETYPE_SHORTCUT_CURRENT_FOLDER) {
            holder.icon.setVisibility(View.VISIBLE);
            holder.divider.setVisibility(View.GONE);
            holder.account_name.setVisibility(View.VISIBLE);
            iconItem.index = position;
            if (mCurrentFolder.getVFieType() == VFileType.TYPE_LOCAL_STORAGE) {
                iconItem.file = mCurrentFolder;
                iconItem.itemtype = ClickIconItem.LOCAL_STORAGE;
                String path;
                if (iconItem.file != null) {
                    path = iconItem.file.getPath().equals(File.separator) ?
                            mActivity.getResources().getString(R.string.device_root_path) : iconItem.file.getPath();
                } else {
                    path = "null";
                }
                holder.account_name.setText(path);
            } else if (mCurrentFolder.getVFieType() ==VFileType.TYPE_CLOUD_STORAGE) {
                int type = ((RemoteVFile) mCurrentFolder).getMsgObjType();
                String title = RemoteAccountUtility.getInstance(mActivity).findCloudTitleByMsgObjType(mActivity, type);
                int index = findStorageIndexByTitle(mActivity, title);
                iconItem.itemtype = ClickIconItem.CLOUD_STORAGE;
                iconItem.storageName = title;
                iconItem.mounted = true;
                iconItem.cloudStorageIndex = index;
                ArrayList<AccountInfo> accountInfoList = getAccountInfo(mActivity, title);
                for (AccountInfo accountInfo : accountInfoList) {
                    if (((RemoteVFile)mCurrentFolder).getStorageName().equals(accountInfo.getAccountName())) {
                        iconItem.acountInfo = accountInfo;
                    }
                }
                if (iconItem.acountInfo != null) {
                    holder.account_name.setText(title + " (" + iconItem.acountInfo.getAccountName() + ")");
                } else {
                    holder.account_name.setText(title);
                }
            } else if (mCurrentFolder.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                iconItem.itemtype = ClickIconItem.NETWORK_STORAGE;
                iconItem.mounted = true;
                iconItem.cloudStorageIndex = CloudStorageIndex.NETWORK_PLACE;
                holder.account_name.setText(mActivity.getResources().getString(
                        R.string.networkplace_storage_title));
            }
            holder.name.setText(mActivity.getResources().getString(R.string.current_folder));
            holder.icon.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.asus_ic_current_folder_b));
            setTextFontAndSize(holder.name,
                    R.dimen.storage_list_name_size, FontType.ROBOTO_LIGHT);
            setTextFontAndSize(holder.account_name,
                    R.dimen.storage_list_connected_name_size, FontType.ROBOTO_LIGHT);
        } else if (storageItemElement.storageType == STORAGETYPE_LOCAL) {
            holder.icon.setVisibility(View.VISIBLE);
            holder.divider.setVisibility(View.GONE);
            holder.account_name.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = holder.container.getLayoutParams();
            params.height = (int) mActivity.getResources().getDimension(
                    R.dimen.storage_list_height);
            holder.container.setLayoutParams(params);
            ((LinearLayout) holder.container).setGravity(Gravity.CENTER);
            if (holder.name.getText().toString().equals(mSelectedStorage)) {
                setTextFontAndSize(holder.name,
                        R.dimen.storage_list_name_size, FontType.ROBOTO_MEDIUM);
            } else {
                setTextFontAndSize(holder.name,
                        R.dimen.storage_list_name_size, FontType.ROBOTO_LIGHT);
            }
            holder.name.setText(VolumeInfoUtility.getInstance(mActivity).findStorageTitleByStorageVolume(mStorageItemElementList.get(position).storageVolume));
            holder.icon.setImageDrawable(VolumeInfoUtility.getInstance(mActivity).findStorageIconByStorageVolume(mStorageItemElementList.get(position).storageVolume,R.array.dialog_storage_icon));
            iconItem.itemtype = ClickIconItem.LOCAL_STORAGE;
            iconItem.index = position;
            iconItem.file = mStorageItemElementList.get(position).vFile;
            iconItem.storageName = holder.name.getText().toString();
        } else if (storageItemElement.storageType == STORAGETYPE_NETWORK) {
            holder.icon.setVisibility(View.VISIBLE);
            holder.divider.setVisibility(View.GONE);
            holder.account_name.setVisibility(View.GONE);
            ((LinearLayout) holder.container).setGravity(Gravity.CENTER);
            ViewGroup.LayoutParams params = holder.container
                    .getLayoutParams();
            params.height = (int) mActivity.getResources().getDimension(
                    R.dimen.storage_list_height);
            holder.container.setLayoutParams(params);
            if (holder.name.getText().toString().equals(mSelectedStorage)) {
                setTextFontAndSize(holder.name,
                        R.dimen.storage_list_name_size,
                        FontType.ROBOTO_MEDIUM);
            } else {
                setTextFontAndSize(holder.name,
                        R.dimen.storage_list_name_size,
                        FontType.ROBOTO_LIGHT);
            }

            holder.name
                    .setText(mNetwork_StorageTitle[CloudStorageIndex.NETWORK_PLACE]);

            holder.icon
                    .setImageDrawable(mNetworkStorageDrawable[storageItemElement.cloudStorageIndex]);
            iconItem.itemtype = ClickIconItem.NETWORK_STORAGE;
            iconItem.index = position;
            iconItem.storageName = holder.name.getText().toString();
            iconItem.cloudStorageIndex = storageItemElement.cloudStorageIndex;
            iconItem.acountInfo = storageItemElement.acountInfo;

        } else if (storageItemElement.storageType == STORAGETYPE_CLOUD) {
            // if(mIsAttachOp || mIsMultipleSelectionOp){
            // holder.container.setVisibility(View.GONE);
            // }else{
            holder.icon.setVisibility(View.VISIBLE);
            holder.divider.setVisibility(View.GONE);
            holder.account_name.setVisibility(View.GONE);
            ((LinearLayout) holder.container).setGravity(Gravity.CENTER);
            setCloudStorageItemName(holder, iconItem, storageItemElement,
                    position);

            holder.icon
                    .setImageDrawable(mCloudStorageDrawable[storageItemElement.cloudStorageIndex]);
            iconItem.itemtype = ClickIconItem.CLOUD_STORAGE;
            iconItem.index = position;
            iconItem.storageName = holder.name.getText().toString();
            iconItem.cloudStorageIndex = storageItemElement.cloudStorageIndex;
            iconItem.acountInfo = storageItemElement.acountInfo;
            // }

        } else if (storageItemElement.storageType == STORAGETYPE_ADD_CLOUD_ACCOUNT) {
            if (ConstantsUtil.IS_AT_AND_T) {// || mIsAttachOp ||
                // mIsMultipleSelectionOp
                holder.container.setVisibility(View.GONE);
            } else {
                holder.icon.setVisibility(View.VISIBLE);
                holder.divider.setVisibility(View.GONE);
                holder.account_name.setVisibility(View.GONE);
                ((LinearLayout) holder.container).setGravity(Gravity.CENTER);
                ViewGroup.LayoutParams params = holder.container
                        .getLayoutParams();
                params.height = (int) mActivity.getResources().getDimension(
                        R.dimen.storage_list_height);
                holder.container.setLayoutParams(params);
                if (holder.name.getText().toString().equals(mSelectedStorage)) {
                    setTextFontAndSize(holder.name,
                            R.dimen.storage_list_name_size,
                            FontType.ROBOTO_MEDIUM);
                } else {
                    setTextFontAndSize(holder.name,
                            R.dimen.storage_list_name_size,
                            FontType.ROBOTO_LIGHT);
                }
                holder.name.setText(storageItemElement.storageTitle);
                holder.icon.setImageDrawable(mActivity.getResources()
                        .getDrawable(R.drawable.dialog_asus_ic_add));

                iconItem.itemtype = ClickIconItem.ADD_CLOUD_ACCOUNT;
                iconItem.index = position;
                iconItem.storageName = holder.name.getText().toString();
            }

        } else if (storageItemElement.storageType == STORAGETYPE_ADD_GOOGLE_ACCOUNT) {
            if (ConstantsUtil.IS_AT_AND_T) {// || mIsAttachOp ||
                                            // mIsMultipleSelectionOp
                holder.container.setVisibility(View.GONE);
            } else {
                holder.icon.setVisibility(View.VISIBLE);
                holder.divider.setVisibility(View.GONE);
                holder.account_name.setVisibility(View.GONE);
                holder.addGoogleAccountPadding.setVisibility(View.VISIBLE);
                ((LinearLayout) holder.container).setGravity(Gravity.CENTER);
                ViewGroup.LayoutParams params = holder.container
                        .getLayoutParams();
                params.height = (int) mActivity.getResources().getDimension(
                        R.dimen.storage_list_height);
                holder.container.setLayoutParams(params);
                if (holder.name.getText().toString().equals(mSelectedStorage)) {
                    setTextFontAndSize(holder.name,
                            R.dimen.storage_list_name_size,
                            FontType.ROBOTO_MEDIUM);
                } else {
                    setTextFontAndSize(holder.name,
                            R.dimen.storage_list_name_size,
                            FontType.ROBOTO_LIGHT);
                }
                holder.name.setText(storageItemElement.storageTitle);
                holder.icon.setImageDrawable(mActivity.getResources()
                        .getDrawable(R.drawable.dialog_asus_ic_add));

                iconItem.itemtype = ClickIconItem.ADD_GOOGLE_ACCOUNT;
                iconItem.index = position;
                iconItem.storageName = holder.name.getText().toString();
            }

        }

        setupLayout(convertView, storageItemElement);

        holder.container.setTag(iconItem);
        setItemBackgroundAndFont(holder.container, holder.name.getText()
                .toString().equals(mSelectedStorage)
                || (holder.name.getText().toString() + holder.account_name
                        .getText().toString()).equals(mSelectedStorage));

        return convertView;
    }

    private void setupLayout(View convertView, StorageItemElement storageItemElement) {

        ItemOperationUtility itemOperationUtility = ItemOperationUtility.getInstance();

        int lastFileType = itemOperationUtility.getMoveToLastFileType(mActivity);
        boolean isPreviousFolderUsed = itemOperationUtility.isPreviousFolderUsed(mActivity);
        boolean isCurrentFolderUsed = itemOperationUtility.isCurrentFolderUsed(mActivity);

        if (storageItemElement.storageType == STORAGETYPE_SHORTCUT_PREVIOUS_FOLDER
                && lastFileType == -1) {
            convertView.setLayoutParams(new AbsListView.LayoutParams(-1,1));
            convertView.setVisibility(View.GONE);
        } else {
            convertView.setLayoutParams(new AbsListView.LayoutParams(-1,-2));
            convertView.setVisibility(View.VISIBLE);
        }

        ImageView newFeature = (ImageView) convertView.findViewById(R.id.new_feature_icon);
        int storageType = storageItemElement.storageType;
        /*
        if ((storageType == STORAGETYPE_SHORTCUT_PREVIOUS_FOLDER && !isPreviousFolderUsed)
                || (storageType == STORAGETYPE_SHORTCUT_CURRENT_FOLDER && !isCurrentFolderUsed)) {
            if (newFeature != null) {
                newFeature.setVisibility(View.VISIBLE);
            }
        } else {
            if (newFeature != null) {
                newFeature.setVisibility(View.GONE);
            }
        }
        */
    }

    @Override
    public boolean isEnabled(int position) {
        return (mStorageItemElementList != null && mStorageItemElementList.get(position).storageType != STORAGETYPE_TITLE);
    };

    @Override
    public void onItemClick(AdapterView<?> AdapterView, View view,
            int position, long arg3) {

        notifyDataSetInvalidated();
        int id = view.getId();
        boolean isPreviousOrCurrentFolder = false;

        String title = ((TextView) view.findViewById(R.id.storage_list_item_name)).getText().toString();

        if (title != null && title.equals(mActivity.getResources().getString(R.string.previous_folder))) {
            ItemOperationUtility itemOperationUtility = ItemOperationUtility.getInstance();
            itemOperationUtility.setPreviousFolderUsed(mActivity, true);
            GaMoveToDialog.getInstance().sendEvents(mActivity, GaMoveToDialog.CATEGORY_NAME,
                    GaMoveToDialog.ACTION_GO_TO_PREVIOUS_FOLDER, null, null);
            isPreviousOrCurrentFolder = true;
        }

        if (title != null && title.equals(mActivity.getResources().getString(R.string.current_folder))) {
            ItemOperationUtility itemOperationUtility = ItemOperationUtility.getInstance();
            itemOperationUtility.setCurrentFolderUsed(mActivity, true);
            GaMoveToDialog.getInstance().sendEvents(mActivity, GaMoveToDialog.CATEGORY_NAME,
                    GaMoveToDialog.ACTION_GO_TO_CURRENT_FOLDER, null, null);
            isPreviousOrCurrentFolder = true;
        }

        ClickIconItem itemIcon;
        itemIcon = (ClickIconItem) view.findViewById(
                R.id.storage_list_item_container).getTag();

        // setItemBackgroundAndFont(view, true);
        FileListFragment fileListFragment = (FileListFragment) mActivity
                .getFragmentManager().findFragmentById(R.id.filelist);
        fileListFragment.updateCloudStorageUsage(false, 0, 0);
        fileListFragment.finishEditMode();
        if (itemIcon.getItemType() == ClickIconItem.LOCAL_STORAGE) {

            if (itemIcon.file == null) {
                return;
            }

            boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(
                    mActivity).isNeedToWriteSdToAppFolder(
                    itemIcon.file.getAbsolutePath());
            if (bNeedWriteToAppFolder) {
                // String aNewPath =
                // FileUtility.getSecondaryStoragePath(mActivity,
                // itemIcon.file);
                WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                        .newInstance();
                warnDialog.show(mActivity.getFragmentManager(),
                        "WarnKKSDPermissionDialogFragment");

                // f (null != aNewPath)
                // startScanFile(new VFile(aNewPath));
                // else
                // startScanFile(itemIcon.file);
            } else {
                MoveToDialogFragment f = (MoveToDialogFragment) mActivity
                        .getFragmentManager().findFragmentByTag(
                                MoveToDialogFragment.DIALOG_TAG);

                if (f == null) {
                    f = (MoveToDialogFragment) mActivity
                            .getFragmentManager().findFragmentByTag(
                                    FilePickerDialogFragment.DIALOG_TAG);
                }

                f.switchFragmentView();
                startScanFile(itemIcon.file);
            }

            if (!isPreviousOrCurrentFolder) {
                GaMoveToDialog.getInstance().sendEvents(mActivity, GaMoveToDialog.CATEGORY_NAME,
                        GaMoveToDialog.ACTION_GO_TO_LOCAL_STORAGE, null, null);
            }

        } else if (itemIcon.getItemType() == ClickIconItem.NETWORK_STORAGE) {
            mSelectedStorageFile = null;
            if (!((FileManagerApplication) mActivity.getApplication())
                    .isWifiConnected()) {
                mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG,
                        VFileType.TYPE_CLOUD_STORAGE);
                return;
            }

            if (!ItemOperationUtility.getInstance().checkCtaPermission(
                    mActivity)) {
                return;
            }

            // MoveToDialogFragment f =
            // (MoveToDialogFragment)mActivity.getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
            // f.switchFragmentView();

            boolean isMounted = itemIcon.getMounted();
            int index = itemIcon.getCloudStorageIndex();
            int couldType = -1;
            int storageType = -2;
            switch (index) {
            case CloudStorageIndex.NETWORK_PLACE:
                // fileListFragment.enablePullReFresh(true);
                MoveToDialogFragment f = (MoveToDialogFragment) mActivity
                        .getFragmentManager().findFragmentByTag(
                                MoveToDialogFragment.DIALOG_TAG);
                f.switchFragmentView();
                SambaFileUtility.getInstance(mActivity).startScanNetWorkDevice(false);
                break;
            }

            /*
             * if(index == CloudStorageIndex.ASUS_HOMEBOX){
             * RemoteFileUtility.isShowDevicesList = true; }else {
             * RemoteFileUtility.isShowDevicesList = false; }
             */

            if (isMounted && index != 0) {
                openCloudStorage(itemIcon, storageType);
            } else if (index == 0) {
                GaCloudStorage.getInstance().sendEvents(mActivity, GaCloudStorage.CATEGORY_NAME,
                        GaCloudStorage.ACTION_OPEN_NETWORK_PLACE, null, null);
            } else {
                RemoteAccountUtility.getInstance(mActivity).addAccount(couldType);
            }

            if (!isPreviousOrCurrentFolder) {
                GaMoveToDialog.getInstance().sendEvents(mActivity, GaMoveToDialog.CATEGORY_NAME,
                        GaMoveToDialog.ACTION_GO_TO_SAMBA_STORAGE, null, null);
            }

        } else if (itemIcon.getItemType() == ClickIconItem.CLOUD_STORAGE) {
            mSelectedStorageFile = null;
            if (!((FileManagerApplication) mActivity.getApplication())
                    .isNetworkAvailable()) {
                mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG,
                        VFileType.TYPE_CLOUD_STORAGE);
                return;
            }

            if (!ItemOperationUtility.getInstance().checkCtaPermission(
                    mActivity)) {
                return;
            }

            // MoveToDialogFragment f =
            // (MoveToDialogFragment)mActivity.getFragmentManager().findFragmentByTag(MoveToDialogFragment.DIALOG_TAG);
            // f.switchFragmentView();

            boolean isMounted = itemIcon.getMounted();
            int index = findStorageIndexByTitle(mActivity, itemIcon.getStorageName());
            int couldType = -1;
            int storageType = -2;
            switch (index) {
            case CloudStorageIndex.ASUS_HOMEBOX:
                couldType = MsgObj.TYPE_HOMECLOUD_STORAGE;
                storageType = StorageType.TYPE_HOME_CLOUD;
                break;
            case CloudStorageIndex.ASUS_WEBSTORAGE:
                couldType = MsgObj.TYPE_ASUSWEBSTORAGE_STORAGE;
                storageType = StorageType.TYPE_ASUSWEBSTORAGE;
                break;
            case CloudStorageIndex.DROPBOX:
                couldType = MsgObj.TYPE_DROPBOX_STORAGE;
                storageType = StorageType.TYPE_DROPBOX;
                break;
            case CloudStorageIndex.BAIDUPCS:
                couldType = MsgObj.TYPE_BAIDUPCS_STORAGE;
                storageType = StorageType.TYPE_BAIDUPCS;
                break;
            case CloudStorageIndex.GOOGLDRIVE:
                couldType = MsgObj.TYPE_GOOGLE_DRIVE_STORAGE;
                storageType = StorageType.TYPE_GOOGLE_DRIVE;
                break;
            case CloudStorageIndex.SKYDRIVE:
                couldType = MsgObj.TYPE_SKYDRIVE_STORAGE;
                storageType = StorageType.TYPE_SKYDRIVE;
                break;
            case CloudStorageIndex.YANDEX:
                couldType = 9;
                storageType = StorageType.TYPE_YANDEX;
                break;
            }

            /*
             * if(index == CloudStorageIndex.ASUS_HOMEBOX){
             * RemoteFileUtility.isShowDevicesList = true; }else {
             * RemoteFileUtility.isShowDevicesList = false; }
             */

            if (isMounted && index != 0) {
                openCloudStorage(itemIcon, storageType);
            } else if (index == 0) {
                GaCloudStorage.getInstance().sendEvents(mActivity, GaCloudStorage.CATEGORY_NAME,
                        GaCloudStorage.ACTION_OPEN_NETWORK_PLACE, null, null);
            } else {
                RemoteAccountUtility.getInstance(mActivity).addAccount(couldType);
            }

            if (!isPreviousOrCurrentFolder) {
                GaMoveToDialog.getInstance().sendEvents(mActivity, GaMoveToDialog.CATEGORY_NAME,
                        GaMoveToDialog.ACTION_GO_TO_CLOUD_STORAGE, null, null);
            }

        } else if (itemIcon.getItemType() == ClickIconItem.ADD_CLOUD_ACCOUNT) {
            if (!ItemOperationUtility.getInstance().checkCtaPermission(
                    mActivity)) {
                ToastUtility.show(mActivity, mActivity.getResources()
                        .getString(R.string.network_cta_hint));
                return;
            }
            if (!((FileManagerApplication) mActivity.getApplication())
                    .isNetworkAvailable()) {
                mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG,
                        VFileType.TYPE_CLOUD_STORAGE);
            } else {
                mActivity.startActivity(new Intent(mActivity, AddCloudAccountActivity.class));
            }

        } else if (itemIcon.getItemType() == ClickIconItem.ADD_GOOGLE_ACCOUNT) {
            if (!ItemOperationUtility.getInstance().checkCtaPermission(
                    mActivity)) {
                ToastUtility.show(mActivity, mActivity.getResources()
                        .getString(R.string.network_cta_hint));
                return;
            }
            if (!((FileManagerApplication) mActivity.getApplication())
                    .isNetworkAvailable()) {
                mActivity.displayDialog(DialogType.TYPE_WIFI_TURN_ON_DIALOG,
                        VFileType.TYPE_CLOUD_STORAGE);
            } else {
                RemoteAccountUtility.getInstance(mActivity).addAccount(MsgObj.TYPE_GOOGLE_DRIVE_STORAGE);
            }

        }

    }

    /*
     * <item>@string/asushomebox_storage_title</item>
     * <item>@string/asuswebstorage_storage_title</item>
     * <item>@string/dropbox_storage_title</item>
     * <item>@string/baidu_storage_title</item>
     * <item>@string/skydrive_storage_title</item>
     * <item>@string/googledrive_storage_title</item>
     */
    public static int findStorageIndexByTitle(Context context, String title) {
        String networkPlace = context.getResources().getString(
                R.string.networkplace_storage_title);
        String baidu = context.getResources().getString(
                R.string.baidu_storage_title);
        String dropbox = context.getResources().getString(
                R.string.dropbox_storage_title);
        String homebox = context.getResources().getString(
                R.string.asushomebox_storage_title);
        String aws = context.getResources().getString(
                R.string.asuswebstorage_storage_title);
        String skydrive = context.getResources().getString(
                R.string.skydrive_storage_title);
        String googledrive = context.getResources().getString(
                R.string.googledrive_storage_title);
        String yandex = context.getResources().getString(
                R.string.yandex_storage_title);

        if (title != null) {
            if (title.equals(networkPlace)) {
                return CloudStorageIndex.NETWORK_PLACE;
            } else if (title.equals(baidu)) {
                return CloudStorageIndex.BAIDUPCS;
            } else if (title.equals(dropbox)) {
                return CloudStorageIndex.DROPBOX;
            } else if (title.equals(homebox)) {
                return CloudStorageIndex.ASUS_HOMEBOX;
            } else if (title.equals(aws)) {
                return CloudStorageIndex.ASUS_WEBSTORAGE;
            } else if (title.equals(skydrive)) {
                return CloudStorageIndex.SKYDRIVE;
            } else if (title.equals(googledrive)) {
                return CloudStorageIndex.GOOGLDRIVE;
            } else if (title.equals(yandex)) {
                return CloudStorageIndex.YANDEX;
            }
        }
        return -1;
    }

    private void setItemBackgroundAndFont(View v, boolean isSelected) {
        LinearLayout container = (LinearLayout) v;

        if (isSelected) {
            container.setBackgroundColor(v.getResources().getColor(
                    R.color.storage_list_item_bg));
            TextView name = (TextView) container
                    .findViewById(R.id.storage_list_item_name);
            mActivity.setTextViewFont(name, FontType.ROBOTO_MEDIUM);
        } else {
            container.setBackgroundColor(v.getResources().getColor(
                    android.R.color.transparent));
            TextView name = (TextView) container
                    .findViewById(R.id.storage_list_item_name);
            mActivity.setTextViewFont(name, FontType.ROBOTO_LIGHT);
        }
    }

    private void setTextFontAndSize(TextView view, int textSizeId, int fontType) {
        // Typeface tf =
        // Typeface.createFromAsset(mActivity.getResources().getAssets(), font);
        // view.setTypeface(tf);
        int colorId = ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK ? R.color.white : R.color.black;
        mActivity.setTextViewFont(view, fontType);
        view.setTextColor(mActivity.getResources().getColor(colorId));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mActivity.getResources()
                .getDimension(textSizeId));
    }

    private void setCloudStorageItemName(ViewHolder holder,
            ClickIconItem iconItem, StorageItemElement storageItemElement,
            int position) {

        if (storageItemElement.acountInfo != null) {
            holder.account_name.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams params = holder.container.getLayoutParams();
            params.height = (int) mActivity.getResources().getDimension(
                    R.dimen.storage_list_connected_height);
            holder.container.setLayoutParams(params);

            params = holder.name.getLayoutParams();
            params.height = LayoutParams.WRAP_CONTENT;
            holder.name.setLayoutParams(params);

            params = holder.account_name.getLayoutParams();
            params.height = LayoutParams.WRAP_CONTENT;
            holder.account_name.setLayoutParams(params);

            holder.name.setText(storageItemElement.storageTitle);
            holder.account_name.setText(storageItemElement.acountInfo
                    .getAccountName());
            iconItem.mounted = true;

            if (holder.name.getText().toString().equals(mSelectedStorage)
                    || (holder.name.getText().toString() + holder.account_name
                            .getText().toString()).equals(mSelectedStorage)) {
                setTextFontAndSize(holder.name,
                        R.dimen.storage_list_connected_name_size,
                        FontType.ROBOTO_MEDIUM);
                setTextFontAndSize(holder.account_name,
                        R.dimen.storage_list_name_size, FontType.ROBOTO_MEDIUM);

            } else {
                setTextFontAndSize(holder.name,
                        R.dimen.storage_list_connected_name_size,
                        FontType.ROBOTO_REGULAR);
                setTextFontAndSize(holder.account_name,
                        R.dimen.storage_list_name_size, FontType.ROBOTO_LIGHT);
            }
        } else {
            ViewGroup.LayoutParams params = holder.container.getLayoutParams();
            params.height = (int) mActivity.getResources().getDimension(
                    R.dimen.storage_list_height);
            holder.container.setLayoutParams(params);

            params = holder.name.getLayoutParams();
            params.height = LayoutParams.WRAP_CONTENT;
            holder.name.setLayoutParams(params);

            setTextFontAndSize(holder.name,
                    R.dimen.storage_list_name_size, FontType.ROBOTO_LIGHT);
            holder.account_name.setVisibility(View.GONE);
            holder.name.setText(storageItemElement.storageTitle);
            iconItem.mounted = false;

            if (holder.name.getText().toString().equals(mSelectedStorage)) {
                setTextFontAndSize(holder.name,
                        R.dimen.storage_list_name_size, FontType.ROBOTO_MEDIUM);

            } else {
                setTextFontAndSize(holder.name,
                        R.dimen.storage_list_name_size, FontType.ROBOTO_LIGHT);
            }

        }
    }

    private void openCloudStorage(ClickIconItem itemIcon, int storageType) {
        Log.d(TAG, "Open Cloud " + itemIcon.getStorageName());

        GaCloudStorage.getInstance().sendEvents(mActivity, GaCloudStorage.CATEGORY_NAME,
                itemIcon.getStorageName(), null, null);

        AccountInfo accountInfo = itemIcon.getAccountInfo();

        if (accountInfo == null) {
            Log.w(TAG, "cannot open cloud storage since account == null, storageType = "
                    + storageType);
            return;
        }

        RemoteVFile cloudRootVFile = new RemoteVFile("/"
                + accountInfo.getAccountName(), VFileType.TYPE_CLOUD_STORAGE,
                accountInfo.getAccountName(), storageType, "");
        cloudRootVFile.setStorageName(accountInfo.getAccountName());
        cloudRootVFile.setFileID("root");
        cloudRootVFile.setFromFileListItenClick(true);
        if (!RemoteAccountUtility.getInstance(mActivity).validateToken(cloudRootVFile)) {
            Log.d(TAG, "valideToken failed, to get token");
            ShortCutFragment.currentTokenFile = cloudRootVFile;
            // if(cloudRootVFile instanceof RemoteVFile &&
            // (cloudRootVFile.getStorageType()==StorageType.TYPE_HOME_CLOUD)){
            // FileListFragment fileListFragment = (FileListFragment)
            // mActivity.getFragmentManager().findFragmentById(R.id.filelist);
            // if (fileListFragment!=null) {
            // fileListFragment.setRemoteVFile(cloudRootVFile);
            // }
            // } this is for pulltoRefresh
            RemoteAccountUtility.getInstance(mActivity).getToken(cloudRootVFile);
            return;
        } else {
            ShortCutFragment.currentTokenFile = null;
            if (cloudRootVFile instanceof RemoteVFile
                    && (cloudRootVFile.getStorageType() == StorageType.TYPE_HOME_CLOUD)) {
                MoveToDialogFragment MoveToListFragment = (MoveToDialogFragment) mActivity
                        .getFragmentManager().findFragmentByTag(
                                MoveToDialogFragment.DIALOG_TAG);
                if (MoveToListFragment != null) {
                    // fileListFragment.enablePullReFresh(true);
                    MoveToListFragment.switchFragmentView();
                    MoveToListFragment.setmIndicatorFile(cloudRootVFile);
                    MoveToListFragment.setListShown(false);
                    MoveToListFragment.SetScanHostIndicatorPath(cloudRootVFile
                            .getStorageName());
                    RemoteFileUtility.getInstance(mActivity).sendCloudStorageMsg(
                                    (cloudRootVFile).getStorageName(),
                                    null,
                                    null,
                                    (cloudRootVFile).getMsgObjType(),
                                    CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_GET_DEVICE_LIST);
                }
                return;
            }
        }

        MoveToDialogFragment f = (MoveToDialogFragment) mActivity
                .getFragmentManager().findFragmentByTag(
                        MoveToDialogFragment.DIALOG_TAG);
        f.switchFragmentView();
        startScanFile(cloudRootVFile, true);
        notifyDataSetChanged();

    }

    private void startScanFile(VFile file, boolean isNeedRefreshToken) {
        Log.d(TAG, "startScanFile");
        if (file != null) {
            if (!file.exists()) {
                return;
            }
            MoveToDialogFragment fragment = (MoveToDialogFragment) mActivity
                    .getFragmentManager().findFragmentByTag(
                            MoveToDialogFragment.DIALOG_TAG);
            if (fragment != null) {
                fragment.startScanFile(file, ScanType.SCAN_CHILD, false);
            }
        }
    }

    public void updateLocalStorageList(ArrayList<Object> storageVolume) {
        final StorageManager mStorageManager = (StorageManager) mActivity
                .getSystemService(Context.STORAGE_SERVICE);
        mLocalStorageElementList.clear();
        mLocalStorageFile.clear();
        VFile[] tmpVFiles = ((FileManagerApplication) mActivity
                .getApplication()).getStorageFile();
        for (int i = 0; i < storageVolume.size(); i++) {
            if (mStorageManager != null
                    && reflectionApis
                            .getVolumeState(mStorageManager, storageVolume.get(i)).equals(Environment.MEDIA_MOUNTED)) {
                mLocalStorageElementList.add(storageVolume.get(i));
                mLocalStorageFile.add(tmpVFiles[i]);
            }
        }
        updateStorageItemElementList();
        if (mSelectedStorageFile != null) {
            boolean mIsSelectedStorageRomoved = true;
            for (int i = 0; i < mLocalStorageFile.size(); i++) {
                if (mSelectedStorageFile.getPath().equals(
                        mLocalStorageFile.get(i).getPath())) {
                    mIsSelectedStorageRomoved = false;
                    break;
                }
            }
            if (mIsSelectedStorageRomoved == true) {
                mActivity.setActionBarTitle(mActivity.getResources().getString(
                        R.string.internal_storage_title));
            }
        }

    }

    public void updateCloudStorageAccountList(AccountInfo cloudStorageAccount) {
        boolean isAccountExisted = false;
        String accountNameStorageType = cloudStorageAccount.getAccountName()
                + cloudStorageAccount.getStorageType();
        for (AccountInfo accountInfo : mConnectedCloudStorageAccountList) {
            if (accountNameStorageType.equals(accountInfo.getAccountName()
                    + accountInfo.getStorageType())) {
                isAccountExisted = true;
                break;
            }
        }
        if (!isAccountExisted) {
            mConnectedCloudStorageAccountList.add(cloudStorageAccount);
            updateStorageItemElementList();
        }
        Log.d(TAG, "mConnectedCloudStorageAccountList size = "
                + mConnectedCloudStorageAccountList.size());

    }

    public void removeCloudStorageAccountList(AccountInfo info) {
        if (mConnectedCloudStorageAccountList.remove(info))
            updateStorageItemElementList();
    }

    public void clearCloudStorageAccountList() {
        mConnectedCloudStorageAccountList.clear();
        updateStorageItemElementList();
    }

    private void updateStorageItemElementList() {
        StorageItemElement storageItemEelement = new StorageItemElement();

        mStorageItemElementList.clear();

        if (mIsSupportCurrentFolder || mIsSupportPreviousFolder) {
            storageItemEelement.storageTitle = mActivity.getResources().getString(
                    R.string.storage_type_shortcut);
            storageItemEelement.storageType = STORAGETYPE_TITLE;
            mStorageItemElementList.add(storageItemEelement);
        }

        if (mIsSupportPreviousFolder) {
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageType = STORAGETYPE_SHORTCUT_PREVIOUS_FOLDER;
            mStorageItemElementList.add(storageItemEelement);
        }

        if (mIsSupportCurrentFolder) {
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageType = STORAGETYPE_SHORTCUT_CURRENT_FOLDER;
            mStorageItemElementList.add(storageItemEelement);
        }

        if (mIsSupportLocalStorage) {
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageTitle = mActivity.getResources().getString(
                    R.string.storage_type_local);
            storageItemEelement.storageType = STORAGETYPE_TITLE;
            mStorageItemElementList.add(storageItemEelement);

            for (int i = 0; i < mLocalStorageElementList.size(); i++) {
                storageItemEelement = new StorageItemElement();
                storageItemEelement.storageVolume = mLocalStorageElementList.get(i);
                storageItemEelement.vFile = mLocalStorageFile.get(i);
                storageItemEelement.storageType = STORAGETYPE_LOCAL;
                mStorageItemElementList.add(storageItemEelement);
            }
        }

        if (mIsSupportSambaStorage) {
            storageItemEelement = new StorageItemElement();
            storageItemEelement.storageTitle = mActivity.getResources()
                    .getString(R.string.storage_type_cloud_att);
            storageItemEelement.storageType = STORAGETYPE_TITLE;
            mStorageItemElementList.add(storageItemEelement);

            if (mNetwork_StorageTitle != null) {
                for (int i = 0; i < mNetwork_StorageTitle.length; i++) {
                    storageItemEelement = new StorageItemElement();
                    storageItemEelement.storageTitle = mNetwork_StorageTitle[i];
                    storageItemEelement.storageType = STORAGETYPE_NETWORK;
                    storageItemEelement.cloudStorageIndex = i;
                    mStorageItemElementList.add(storageItemEelement);
                }
            }
        }

        if (!ConstantsUtil.IS_AT_AND_T && mIsSupportCloudStorage) {
            boolean hasCloudTitleRow = false;
            if (mCloud_StorageTitle != null) {
                for (int i = 0; i < mCloud_StorageTitle.length; i++) {
                    ArrayList<AccountInfo> accountInfoList = getAccountInfo(
                            mActivity, mCloud_StorageTitle[i]);
                    if (null == accountInfoList || !accountInfoList.isEmpty()) {
                        if (!hasCloudTitleRow) {
                            hasCloudTitleRow = true;
                            storageItemEelement = new StorageItemElement();
                            storageItemEelement.storageTitle = mActivity
                                    .getResources().getString(
                                            R.string.storage_type_cloud);
                            storageItemEelement.storageType = STORAGETYPE_TITLE;
                            mStorageItemElementList.add(storageItemEelement);
                        }
                        for (AccountInfo temAccountInfo : accountInfoList) {
                            storageItemEelement = new StorageItemElement();
                            storageItemEelement.storageTitle = mCloud_StorageTitle[i];
                            storageItemEelement.acountInfo = temAccountInfo;
                            storageItemEelement.storageType = STORAGETYPE_CLOUD;
                            storageItemEelement.cloudStorageIndex = i;
                            mStorageItemElementList.add(storageItemEelement);
                        }
                    }
                }
            }

            // FIXME:
            // We don't allow to add account here (inside move to dialog)
            // add drawer add cloud account
            // storageItemEelement = new StorageItemElement();
            // storageItemEelement.storageTitle = mActivity.getResources()
            // .getString(R.string.storagetype_add_cloud_account);
            // storageItemEelement.storageType = STORAGETYPE_ADD_CLOUD_ACCOUNT;
            // mStorageItemElementList.add(storageItemEelement);
        }

        notifyDataSetChanged();
    }

    public static ArrayList<AccountInfo> getAccountInfo(Context context, String cloudTitle) {
        int storageType = RemoteAccountUtility.getInstance(null).findMsgObjTypeByCloudTitle(context, cloudTitle);
        ArrayList<AccountInfo> accountInfoList = new ArrayList<AccountInfo>();
        for (AccountInfo tmpAccountInfo : mConnectedCloudStorageAccountList) {
            if (tmpAccountInfo.getStorageType() == storageType) {
                accountInfoList.add(tmpAccountInfo);
            }
        }
        return accountInfoList;
    }

    private void startScanFile(VFile file) {
        if (file != null) {
            if (!file.exists()) {
                return;
            }
            MoveToDialogFragment fragment = (MoveToDialogFragment) mActivity
                    .getFragmentManager().findFragmentByTag(
                            MoveToDialogFragment.DIALOG_TAG);
            if (fragment != null) {
                fragment.startScanFile(file, ScanType.SCAN_CHILD, false);
            }

            FilePickerDialogFragment filePickerDialogFragment = (FilePickerDialogFragment) mActivity
                            .getFragmentManager().findFragmentByTag(
                                        FilePickerDialogFragment.DIALOG_TAG);
            if (filePickerDialogFragment != null) {
                filePickerDialogFragment.startScanFile(file, ScanType.SCAN_CHILD, false);
            }
        }
    }

    public void setCurrentFolder(VFile currentFolder) {
        mCurrentFolder = currentFolder;
    }

    public void setMode(int mode) {
        if (mMode == mode) {
            return;
        }

        mMode = mode;

        if (mMode == MoveToDialogFragment.MODE_MOVE_TO) {
            mIsSupportPreviousFolder = true;
            mIsSupportCurrentFolder = true;
            mIsSupportSambaStorage = true;
            mIsSupportCloudStorage = true;
        } else if (mMode == MoveToDialogFragment.MODE_SINGLE_MOVE_TO_IN_CATEGORY_TOP) {
            mIsSupportPreviousFolder = true;
            mIsSupportCurrentFolder = true;
            mIsSupportSambaStorage = true;
            mIsSupportCloudStorage = true;
        } else if (mMode == MoveToDialogFragment.MODE_MULTIPLE_MOVE_TO_IN_CATEGORY_TOP) {
            mIsSupportPreviousFolder = true;
            mIsSupportCurrentFolder = true;
            mIsSupportSambaStorage = false;
            mIsSupportCloudStorage = false;
        } else if (mMode == MoveToDialogFragment.MODE_HIDDEN_ZONE) {
            mIsSupportPreviousFolder = true;
            mIsSupportCurrentFolder = false;
            mIsSupportSambaStorage = false;
            mIsSupportCloudStorage = false;
        } else if (mMode == MoveToDialogFragment.MODE_NAVIGATION_NORMAL) {
            mIsSupportPreviousFolder = true;
            mIsSupportCurrentFolder = true;
            mIsSupportLocalStorage = true;
            mIsSupportSambaStorage = true;
            mIsSupportCloudStorage = true;
        } else if (mMode == MoveToDialogFragment.MODE_NAVIGATION_LOCAL_ONLY) {
            mIsSupportPreviousFolder = false;
            mIsSupportCurrentFolder = false;
            mIsSupportLocalStorage = true;
            mIsSupportSambaStorage = false;
            mIsSupportCloudStorage = false;
        } else if (mMode == MoveToDialogFragment.MODE_NAVIGATION_SAMBA_ONLY) {
            mIsSupportPreviousFolder = false;
            mIsSupportCurrentFolder = false;
            mIsSupportLocalStorage = false;
            mIsSupportSambaStorage = true;
            mIsSupportCloudStorage = false;
        } else if (mMode == MoveToDialogFragment.MODE_NAVIGATION_CLOUD_ONLY) {
            mIsSupportPreviousFolder = false;
            mIsSupportCurrentFolder = false;
            mIsSupportLocalStorage = false;
            mIsSupportSambaStorage = false;
            mIsSupportCloudStorage = true;
        } else {
            mIsSupportPreviousFolder = false;
            mIsSupportCurrentFolder = false;
            mIsSupportSambaStorage = false;
            mIsSupportCloudStorage = false;
        }

        updateStorageItemElementList();
    }

    // private boolean getLinkToNetPermission(){
    // boolean accept =
    // ItemOperationUtility.getInstance().checkCtaPermission(mActivity);
    // if (accept) {
    // return true;
    // }
    // else {
    // return false;
    // }
    // }
}
