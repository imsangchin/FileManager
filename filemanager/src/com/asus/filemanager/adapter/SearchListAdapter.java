
package com.asus.filemanager.adapter;

import android.app.Activity;
import android.content.res.Configuration;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Config;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.activity.SearchResultFragment;
import com.asus.filemanager.adapter.BasicFileListAdapter.CheckResult;
import com.asus.filemanager.dialog.PasteDialogFragment;
import com.asus.filemanager.dialog.WarnKKSDPermissionDialogFragment;
import com.asus.filemanager.dialog.ZipDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.ga.GaOpenFile;
import com.asus.filemanager.ga.GaSearchFile;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.ConstantsUtil.OpenType;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.FolderElement.StorageType;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.PathIndicator;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.remote.dialog.CloudStorageLoadingDialogFragment;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand.CloudStorageServiceHandlerMsg;

import java.io.File;
import java.util.Date;

public class SearchListAdapter extends BaseAdapter implements OnClickListener, OnItemClickListener, OnTouchListener, OnItemLongClickListener, OnLongClickListener {

    private static final String TAG = "SearchListAdapter";
    private static final boolean DEBUG = Config.DEBUG;

    private String keyWord="";
    private class ViewHolder {
        CheckBox check;
        ImageView icon;
        TextView name;
        TextView location;
        TextView location_time;
        TextView size;
        TextView date;
        View action;
        RelativeLayout fileitem;
    }

    private int mOrientation;
    private SearchResultFragment mFragment;
    private VFile[] mFileArray;
    private ItemIcon mItemIcon;
    private boolean isRemoteFile = false;
    private boolean mIsShowFavoriteName = false;
    private CheckResult mSelectedResult;

    public SearchListAdapter(SearchResultFragment fragment, VFile[] files) {
        mFragment = fragment;
        mFileArray = files;
        mItemIcon = new ItemIcon(fragment.getActivity().getApplicationContext(), mFragment);
        mOrientation = fragment.getResources().getConfiguration().orientation;
    }

    public VFile[] getFiles() {
        return mFileArray;
    }

    @Override
    public int getCount() {
        return (mFileArray == null) ? 0 : mFileArray.length;
    }

    @Override
    public Object getItem(int position) {
        return (mFileArray == null) ? null : mFileArray[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.search_list_item, null);
            holder = new ViewHolder();
            holder.check = (CheckBox) convertView.findViewById(R.id.search_list_item_check);
            holder.icon = (ImageView) convertView.findViewById(R.id.search_list_item_icon);
            holder.name = (TextView) convertView.findViewById(R.id.search_list_item_name);
            holder.location = (TextView) convertView.findViewById(R.id.search_list_item_location);
            holder.location_time = (TextView) convertView.findViewById(R.id.search_list_item_location_time);
            holder.size = (TextView) convertView.findViewById(R.id.search_list_item_size);
            holder.date = (TextView) convertView.findViewById(R.id.search_list_item_date);
            convertView.setTag(holder);
            holder.action = convertView.findViewById(R.id.file_list_item_action);
            holder.fileitem = (RelativeLayout) convertView.findViewById(R.id.file_item);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mFileArray != null && position < mFileArray.length) {

            boolean isFolder = mFileArray[position].isDirectory();

            if (holder.fileitem != null) {
                holder.fileitem.setOnTouchListener(this);
            }

            if (holder.check != null) {
                holder.check.setTag(position);
                if (mFragment.isInEditMode()) {
                    holder.check.setVisibility(View.VISIBLE);
                    holder.check.setTag(position);
                    holder.check.setChecked(mFileArray[position].getChecked());
                    holder.check.setOnClickListener(this);
                } else {
                    holder.check.setVisibility(View.GONE);
                }
            }

            if (holder.icon != null) {
                holder.icon.setTag(mFileArray[position].getAbsolutePath());
                mItemIcon.setIcon(mFileArray[position], holder.icon, true);
            }

            if (holder.name != null) {
                if (mIsShowFavoriteName && mFileArray[position].isFavoriteFile()) {
                    holder.name.setText(showHighLightOnTextView(mFileArray[position].getFavoriteName()));
                } else {
                    holder.name.setText(showHighLightOnTextView(mFileArray[position].getName()));
                }
            }

            if (holder.location != null) {
                String path = mFileArray[position].getParent();
                if (mFileArray[position].getParent().startsWith("smb")) {
                    path = SearchResultFragment.getRelativePath(mFileArray[position].getParentFile().getAbsolutePath());
                }
               if (mFileArray[position].getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                   java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(mFragment.getActivity());
                   java.text.DateFormat shortTimeFormat = DateFormat.getTimeFormat(mFragment.getActivity());
                   Date date = new Date(mFileArray[position].lastModified());
                   if (((FileManagerActivity) mFragment.getActivity()).isPadMode2() && holder.location_time != null) {
                       holder.location.setText(shortDateFormat.format(date));
                       holder.location_time.setText(shortTimeFormat.format(date));
                       holder.location.setTag(mFileArray[position]);
                   } else {
                       holder.location.setText(shortDateFormat.format(date) + " " + shortTimeFormat.format(date));
                       holder.location.setTag(mFileArray[position]);
                   }

                } else {
                    holder.location.setText(path);
                    holder.location.setTag(mFileArray[position]);
                    //holder.location.getPaint().setUnderlineText(true);
                    holder.location.setOnClickListener(this);
                    holder.location.setOnTouchListener(this);
                }
            }

            if (holder.date != null) {
                if (mOrientation == Configuration.ORIENTATION_LANDSCAPE
                        && ((FileManagerActivity) mFragment.getActivity()).isPadMode()) {
                    holder.date.setVisibility(View.VISIBLE);
                    java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(mFragment.getActivity());
                    java.text.DateFormat shortTimeFormat = DateFormat.getTimeFormat(mFragment.getActivity());
                    Date date = new Date(mFileArray[position].lastModified());
                    holder.date.setText(shortDateFormat.format(date) + " " + shortTimeFormat.format(date));
                } else {
                    holder.date.setVisibility(View.GONE);
                }
            }

            if (holder.size != null) {
                if (mOrientation == Configuration.ORIENTATION_LANDSCAPE
                        && ((FileManagerActivity) mFragment.getActivity()).isPadMode()) {
                    holder.size.setVisibility(View.VISIBLE);
                    if (!isFolder) {
                        holder.size.setText(FileUtility.bytes2String(mFragment.getActivity().getApplicationContext(), mFileArray[position].length(), 1));
                    } else {
                        holder.size.setText(null);
                    }
                } else {
                    holder.size.setVisibility(View.GONE);
                }
            }
        }
        if (holder.action != null) {
            holder.action.setTag(position);
            holder.action.setOnClickListener(this);
        }
        if (holder.fileitem != null) {
            holder.fileitem.setTag(position);
            holder.fileitem.setOnClickListener(this);
            holder.fileitem.setOnLongClickListener(this);
        }
        return convertView;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void updateAdapter(VFile[] files, boolean isInitial) {
        if (isInitial) {
            mItemIcon.clearCache();

            // +++ for remote storage thumbnail, we should keep the remote storage list
            if (PathIndicator.getIndicatorVFileType() != VFileType.TYPE_LOCAL_STORAGE) {
                SearchResultFragment searchResultFragment = (SearchResultFragment) mFragment.getFragmentManager().findFragmentById(R.id.searchlist);
                searchResultFragment.setRemoteThumbnailList(files);
            }

            mFileArray = files;
        }
     /*   if (files!=null&&files[0]!=null&&files[0] instanceof RemoteVFile) {
                mFragment.showLocationOrDate(true);
        }*/
        notifyDataSetChanged();
    }

    public void updateAdapter(VFile[] files ,String keyWord, boolean isShowFavoriteName) {
        this.keyWord = keyWord;
        mItemIcon.clearCache();
        mFileArray = files;
        mIsShowFavoriteName = isShowFavoriteName;
     /*   if (files!=null&&files[0]!=null&&files[0] instanceof RemoteVFile) {
            mFragment.showLocationOrDate(true);
        } else {
            mFragment.showLocationOrDate(true);
        }*/
        notifyDataSetChanged();

    }

    private SpannableStringBuilder showHighLightOnTextView(String content) {
        SpannableStringBuilder spanSb = new SpannableStringBuilder(content);
        String lowcaseContent = content.toLowerCase();
        if (keyWord!=null&&!keyWord.trim().equals("")) {
            int length = keyWord.trim().length();
            int startIndex = lowcaseContent.indexOf(keyWord.trim().toLowerCase());
            int preendIndex = startIndex +length;
            boolean flag = false;
            while (startIndex>-1) {
                spanSb.setSpan(new BackgroundColorSpan(mFragment.getResources().getColor(R.color.search_key_background)), flag?preendIndex-length:startIndex,preendIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spanSb.setSpan(new ForegroundColorSpan(mFragment.getResources().getColor(R.color.white)), flag?preendIndex-length:startIndex,preendIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                startIndex = lowcaseContent.substring(preendIndex).indexOf(keyWord.trim().toLowerCase());
                preendIndex +=startIndex+length;
                flag=true;
            }

        /*  while (startIndex!=-1) {
                startIndex = content.substring(startIndex+length).indexOf(keyWord);
            }*/


        }
        return spanSb;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_list_item_check:
                int checkPosition = (Integer) v.getTag();
                updateEditItemStatus(checkPosition,((CheckBox) v).isChecked());
                break;
            case R.id.search_list_item_location:
                FileListFragment fragment =(FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                if (mFragment.isInEditMode()) {
                    return;
                }

                if (v.getTag() instanceof RemoteVFile) {
                    RemoteVFile  tem = ((RemoteVFile)v.getTag());
                    RemoteVFile tempVFile = ((RemoteVFile)v.getTag()).getParentFile();
                    fragment.goToLocation(tempVFile);
                } else {
                    VFile file = ((VFile)v.getTag());
                    String fileLocation = String.valueOf(((TextView)v).getText());
                    if (file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                        fileLocation = ((SambaVFile)file).getParentFile().getAbsolutePath();
                        fragment.goToLocation(new SambaVFile(fileLocation));
                    } else {
                        fragment.goToLocation(new VFile(fileLocation));
                    }
                }
                GaSearchFile.getInstance().sendEvents(mFragment.getActivity(), GaSearchFile.CATEGORY_NAME,
                        GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_GO_TO_FOLDER, null);
                break;
            case R.id.file_list_item_action:
                onPopupActionButtonClick(v);
                break;
            case R.id.file_item:
                 int position = (Integer)v.getTag();
                 if ( mFileArray != null && !(position > mFileArray.length)) {
                    VFile file = mFileArray[position];

                    if (mFragment.isInEditMode()) {
                        updateEditItemStatus(position, !file.getChecked());
                        return;
                    }

                    if (file.isDirectory()) {
                        FileListFragment fileListFragment =(FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                        if (fileListFragment != null) {
                            if (file.getBucketId() != 0) {
                                LocalVFile folder= new LocalVFile(fileListFragment.getmIndicatorFile().getAbsolutePath() + File.separator + file.getName(), VFileType.TYPE_CATEGORY_STORAGE);
                                folder.setBucketId(file.getBucketId());
                                fileListFragment.goToAlbum(folder);
                            } else {
                                fileListFragment.goToLocation(file);
                            }
                        }
                     } else {
                    if (file instanceof RemoteVFile && ((RemoteVFile)file).getVFieType() == VFileType.TYPE_CLOUD_STORAGE) {
                         RemoteVFile tempVFile = (RemoteVFile)file;
                         int openType = FileUtility.isMultiMediaFile(mFragment.getActivity(), tempVFile.getName());
                         if (openType > OpenType.OPEN_TYPE_DEFAULT && tempVFile.getStorageType() != StorageType.TYPE_YANDEX) {
                            FileUtility.openStreamFileWidth(mFragment.getActivity(), tempVFile,openType);
                            FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                            fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_RETRIEVING_URL);
                        } else {
                            VFile[] srcVFile = {tempVFile};
                            String account = tempVFile.getStorageName();
                            VFile dstVFile = new LocalVFile(mFragment.getActivity().getExternalCacheDir(), ".cfile/");
                            int type = tempVFile.getMsgObjType();
                            EditPool editPool = new EditPool();
                            editPool.setFile(tempVFile);
                            editPool.setTargetDataType(dstVFile.getVFieType());
                            editPool.setPasteDialogType(PasteDialogFragment.DIALOGTYPE_PREVIEW);
                             PasteDialogFragment pasteDialogFragment = PasteDialogFragment.newInstance(editPool);
//                              pasteDialogFragment.show(mFragment.getFragmentManager(), "PasteDialogFragment");
                            pasteDialogFragment.show(mFragment.getFragmentManager(), PasteDialogFragment.PREVIEW_DIALOG_PROCESS);
                            RemoteFileUtility.getInstance(mFragment.getActivity()).sendCloudStorageCopyMessage(account, srcVFile, dstVFile, type, CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_COPY_FILE_FROM_REMOTE, RemoteFileUtility.REMOTE_PREVIEW_ACTION, false);
                            /*
                            fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);*/
                        }
                    } else if (file.getVFieType() == VFileType.TYPE_SAMBA_STORAGE) {
                        SambaVFile sambaVFile = (SambaVFile)file;
                        SambaFileUtility sambaFileUtility = SambaFileUtility.getInstance(null);
                        if (sambaFileUtility.isMediaFile(sambaVFile.getAbsolutePath())) {
                            sambaFileUtility.playMediaFileOnLine(sambaVFile.getAbsolutePath());
                        } else {
                            VFile dstVFile = new LocalVFile(mFragment.getActivity().getExternalCacheDir(), ".cfile/");
                            SambaVFile[] files = new SambaVFile[1];
                            files[0] = sambaVFile;
                            sambaFileUtility.sendSambaMessage(SambaMessageHandle.FILE_OPEN, files, dstVFile.getAbsolutePath(), false,-1,null);
                            FileListFragment fileListFragment = (FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                            fileListFragment.showDialog(DialogType.TYPE_CLOUD_STORAGE_LOADING, CloudStorageLoadingDialogFragment.TYPE_LOADING_DIALOG);
                        }
                    } else {
                        FileUtility.openFile(mFragment.getActivity(), file, false, false);
                    }
                    GaSearchFile.getInstance().sendEvents(mFragment.getActivity(), GaSearchFile.CATEGORY_NAME,
                            GaSearchFile.ACTION_ACCESS_AFTER_SEARCH, GaSearchFile.LABEL_OPEN, null);
                    GaOpenFile.getInstance().sendEvents(mFragment.getActivity(), file);
                     }
                 }
                 break;

            default:
                break;
        }
    }

 // Reflect function called by action button
    public void onPopupActionButtonClick(View button) {

        // record which file is clicked
        VFile f = null;
        try {
            if (button != null && button.getTag() != null) {
                f = mFileArray[(Integer) button.getTag()];
            }
        } catch (Exception e) {
            Log.w(TAG, "Fail to call onPopupActionButtonClick:" + e.toString());
        }


        final VFile chosenFile = f;
        if (chosenFile == null || chosenFile.getAbsolutePath() == null) {
            return;
        }
        // setup popup menu
        PopupMenu popup = new PopupMenu(button.getContext(), button);
        popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());


        Menu menu = popup.getMenu();
        if (chosenFile.getVFieType() != VFileType.TYPE_LOCAL_STORAGE) {
            menu.findItem(R.id.menu_zip).setVisible(false);
            menu.findItem(R.id.menu_favorite_add).setVisible(false);
            menu.findItem(R.id.menu_favorite_remove).setVisible(false);
        } else {
            menu.findItem(R.id.menu_favorite_add).setVisible(chosenFile.isDirectory() && !chosenFile.isFavoriteFile());
            menu.findItem(R.id.menu_favorite_remove).setVisible(chosenFile.isDirectory() && chosenFile.isFavoriteFile());
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
        FileListFragment fileListtFragment = (FileListFragment)mFragment.getFragmentManager().findFragmentById(R.id.filelist);
            VFile[] choseArray = new VFile[1];
            choseArray[0] = chosenFile;
            Activity mActivity = mFragment.getActivity();
            switch (item.getItemId()) {
                    case R.id.menu_favorite_add:
                        mFragment.addFavoriteFile(chosenFile);
                        return true;
                    case R.id.menu_favorite_remove:
                        EditPool removeFilePool = new EditPool();
                        removeFilePool.setFiles(choseArray, false);
                        mFragment.showDialog(DialogType.TYPE_FAVORITE_ROMOVE_DIALOG, removeFilePool);
                        return true;
                    case R.id.menu_rename:
                        if (ItemOperationUtility.isItemContainDrm(choseArray,true,false)) {
                            if (mFragment != null) {
                                if (chosenFile.isFile()) {
                                    ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                                } else {
                                    ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                                }
                            }
                        } else {
                            boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mFragment.getActivity()).isNeedToWriteSdToAppFolder(chosenFile.getAbsolutePath());
                            if (bNeedWriteToAppFolder) {
                                WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                                    .newInstance();
                                warnDialog.show(mFragment.getActivity().getFragmentManager(),
                                    "WarnKKSDPermissionDialogFragment");
                            }else if (SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(chosenFile.getAbsolutePath())) {
                                if (mActivity instanceof FileManagerActivity) {
                                    ((FileManagerActivity)mActivity).callSafChoose(SafOperationUtility.ACTION_RENAME);
                                }
                            } else {
                                mFragment.showDialog(DialogType.TYPE_RENAME_DIALOG, chosenFile);
                            }
                        }
                        return true;
                    case R.id.menu_info:
                        mFragment.showDialog(DialogType.TYPE_INFO_DIALOG, chosenFile);
                        return true;
                    case R.id.menu_copy:
                        if (ItemOperationUtility.isItemContainDrm(choseArray,true,false)) {
                            if (mFragment != null) {
                                if (chosenFile.isFile()) {
                                    ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                                } else {
                                    ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                                }
                            }
                        } else {
                            VFile[] copyFile = new VFile[1];
                            copyFile[0] = chosenFile;
                             if (fileListtFragment!=null)
                            fileListtFragment.copyFileInPopup(copyFile,true);
                        }
                        return true;
                    case R.id.menu_cut:
                        if (ItemOperationUtility.isItemContainDrm(choseArray,true,false)) {
                            if (mFragment != null) {
                                if (chosenFile.isFile()) {
                                    ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                                } else {
                                    ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                                }
                            }
                        } else {
                            VFile[] cutFile = new VFile[1];
                            cutFile[0] = chosenFile;
                            if (fileListtFragment!=null)
                               fileListtFragment.cutFileInPopup(cutFile,true);
                        }
                        return true;
                    case R.id.menu_delete:
                        VFile[] deleteFile = new VFile[1];
                        deleteFile[0] = chosenFile;
                        if (mFragment!=null) {
                            mFragment.deleteFileInPopup(deleteFile);
                        }
                        return true;
                    case R.id.menu_zip:
                        VFile[] array = new VFile[1];
                        array[0] = chosenFile;

                        boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mFragment.getActivity()).isNeedToWriteSdToAppFolder(chosenFile.getAbsolutePath());
                        if (bNeedWriteToAppFolder) {
                            WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                                .newInstance();
                            warnDialog.show(mFragment.getActivity().getFragmentManager(),
                                "WarnKKSDPermissionDialogFragment");
                        }else if (SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(chosenFile.getAbsolutePath())) {
                            if (mActivity instanceof FileManagerActivity) {
                                ((FileManagerActivity)mActivity).callSafChoose(SafOperationUtility.ACTION_ZIP);
                            }
                        } else {
                            if (mFragment != null) {
                                ZipDialogFragment.showZipDialog(mFragment, array, false);
                            }
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        VFile[] choseArray = new VFile[1];
        choseArray[0] = chosenFile;
        if (ItemOperationUtility.isItemContainDrm(choseArray,true,false)) {
            if (mFragment != null) {
                ItemOperationUtility.disableItemMenu(menu.findItem(R.id.menu_cut),mFragment.getString(R.string.cut));
                ItemOperationUtility.disableItemMenu(menu.findItem(R.id.menu_rename),mFragment.getString(R.string.rename));
                ItemOperationUtility.disableItemMenu(menu.findItem(R.id.menu_copy),mFragment.getString(R.string.copy));
            }
        }
        popup.show();

        if (mFragment != null && mFragment.getActivity() != null && (mFragment.getActivity() instanceof FileManagerActivity)) {
            ((FileManagerActivity)mFragment.getActivity()).setPopupMenu(popup);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        /*if ( mFileArray != null && !(position > mFileArray.length)) {
            VFile file = mFileArray[position];
            if (file.isDirectory()) {
                FileListFragment fileListFragment =(FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);
                if (fileListFragment != null)
                    fileListFragment.goToLocation(file);
            } else {
                FileUtility.openFile(mFragment.getActivity(), file, false, false);
            }
            GATracker.sendOpenFileEvent(mFragment.getActivity(),file);
        }*/
    }

    public boolean onTouch(View v, MotionEvent event) {
        boolean isPadMode = ((FileManagerActivity)mFragment.getActivity()).isPadMode();
        if (isPadMode) {
            ((FileManagerActivity)mFragment.getActivity()).searchViewIconified(true);
        } else {
            SearchResultFragment searchResultFragment = (SearchResultFragment) mFragment.getFragmentManager().findFragmentById(R.id.searchlist);
            searchResultFragment.clearSearchViewFocus();
        }

        if (v.getId() == R.id.search_list_item_location) {
            TextView t = (TextView) v;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                t.setBackgroundResource(R.drawable.list_bg);
//                t.setBackgroundColor(Color.rgb(0xd1, 0xd1, 0xd1));
                //t.setBackgroundResource(R.drawable.list_selector_holo_light);
                }
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                t.setBackgroundDrawable(null);
        }

        //t.setPadding(15, 0, 15, 0);
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {

        FileListFragment fragment =(FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);

        VFile indicatorFile = fragment.getIndicatorFile();

//        if (fragment.mActivity.CATEGORY_IMAGE_FILE.equals(indicatorFile)
//                || fragment.mActivity.CATEGORY_MUSIC_FILE.equals(indicatorFile)
//                || fragment.mActivity.CATEGORY_VIDEO_FILE.equals(indicatorFile)) {
//            Log.w(TAG, "not support action mode in media category search result");
//            return true;
//        }

        mSelectedResult = getSelectedCount();
        VFile vFile = mFileArray[position];
        updateEditItemStatus(position, !vFile.getChecked());
        return true;
    }

    public boolean isItemsSelected() {
        boolean isItemSelected = false;
        if (mFileArray != null) {
            for (int i = 0; i < mFileArray.length; i++) {
                if (mFileArray[i].getChecked()) {
                    isItemSelected = true;
                    break;
                }
            }
        }
        return isItemSelected;
    }

    public CheckResult getSelectedCount() {
        /* if(mSelectedResult != null)
            return mSelectedResult;*/

        int count = 0;
        boolean hasDir = false;
        int dircount = 0;
        CheckResult result = new CheckResult();
        if (mFileArray != null) {
            VFile[] mFileArrayClone = mFileArray.clone();

            for (int i = 0; i < mFileArrayClone.length; i++) {
                if (mFileArrayClone[i].getChecked()) {
                    count++;

                    if (mFileArrayClone[i].isDirectory()) {
                        hasDir = true;
                        dircount++;
                    }
                }
            }
        }
        result.count = count;
        result.hasDir = hasDir;
        result.dircount = dircount;
        return result;
    }

    private void updateEditItemStatus(int checkPosition,boolean checked){
        FileListFragment fragment =(FileListFragment) mFragment.getFragmentManager().findFragmentById(R.id.filelist);

        if (mFileArray == null || checkPosition >= mFileArray.length || mFileArray[checkPosition] == null
                || mFileArray[checkPosition].getParent().equals(RemoteFileUtility.getInstance(mFragment.getActivity()).getHomeCloudRootPath())
                /*|| (mFragment.mIsMultipleSelectionOp && mFileArray[checkPosition].isDirectory())*/) {
            Log.w(TAG, "mFileArray == null || mFileArray[checkPosition] == null");
            return;
        }else{
            mFileArray[checkPosition].setChecked(checked);

            if(mFileArray[checkPosition].getChecked()){
                mSelectedResult.count = mSelectedResult.count + 1;
                if(mFileArray[checkPosition].isDirectory()){
                    mSelectedResult.dircount = mSelectedResult.dircount + 1;
                    mSelectedResult.hasDir = true;
                }
            }else{
                mSelectedResult.count = mSelectedResult.count - 1;
                if(mFileArray[checkPosition].isDirectory()){
                    mSelectedResult.dircount = mSelectedResult.dircount - 1;
                    if(mSelectedResult.dircount < 1){
                        mSelectedResult.hasDir = false;
                    }
                }
            }

            notifyDataSetChanged();
            mFragment.updateEditMode();
        }
    }

    public void setSelectAll() {
        if (mFileArray != null) {
            for (int i = 0; i < mFileArray.length; i++) {
                mFileArray[i].setChecked(true);
            }
        }

        notifyDataSetInvalidated();
    }

    public void clearItemsSelected() {
        if (mFileArray != null) {
            for (int i = 0; i < mFileArray.length; i++) {
                mFileArray[i].setChecked(false);
            }
            mSelectedResult.count = 0;
            mSelectedResult.hasDir =false;
            mSelectedResult.dircount = 0;
        }
    }
}
