package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.loader.ScanFileLoader.ScanType;
import com.asus.filemanager.saf.SafOperationUtility;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.utility.ItemOperationUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VFile.VFileType;
import com.asus.filemanager.wrap.WrapEnvironment;
import com.asus.remote.utility.RemoteFileUtility;

import java.util.ArrayList;

public class ItemSelectPopMenuDialog extends DialogFragment implements DialogInterface.OnClickListener{
    private static final String SELECT_FILE = "select_file";
    private FileListFragment mFragment = null;
    private ArrayList<String> mMenuList = new ArrayList<String>();
    private VFile mSelectFile = null;
    
    public static ItemSelectPopMenuDialog newInstance(VFile selectFile){
        ItemSelectPopMenuDialog dialog = new ItemSelectPopMenuDialog();
        Bundle args = new Bundle();
        args.putParcelable(SELECT_FILE, selectFile);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        VFile chosenFile = (VFile)getArguments().getParcelable(SELECT_FILE);
        mSelectFile = chosenFile;
        mFragment = (FileListFragment)getFragmentManager().findFragmentById(R.id.filelist);
        mMenuList.add(getResources().getString(R.string.info));
        mMenuList.add(getResources().getString(R.string.copy));
        mMenuList.add(getResources().getString(R.string.cut));
        mMenuList.add(getResources().getString(R.string.delete));
        mMenuList.add(getResources().getString(R.string.rename));
        mMenuList.add(getResources().getString(R.string.action_zip));
        
        
        if (mFragment.mIsAttachOp || mFragment.mIsMultipleSelectionOp) {
            while(mMenuList.size() > 1){
                mMenuList.remove(mMenuList.size() - 1);
            }
//            mMenuList.remove(5);
//            mMenuList.remove(4);
//            mMenuList.remove(3);
//            mMenuList.remove(4);
//            mMenuList.remove(5);
        }
        boolean isShowInfoOnly = false;

        //show info menu only for /sdcard,/Removable,/stotage
        if(chosenFile == null || chosenFile.getAbsolutePath() == null){
            return null;
        }
        if (chosenFile.getAbsolutePath().equals(WrapEnvironment.getEpadExternalStorageDirectory().getAbsolutePath())
                 ||chosenFile.getAbsolutePath().equals(WrapEnvironment.getEpadInternalStorageDirectory().getAbsolutePath())
                 || chosenFile.getVFieType() == VFileType.TYPE_SAMBA_STORAGE && ((SambaVFile)chosenFile).getParentPath().equals(SambaFileUtility.getInstance(mFragment.getActivity()).getRootScanPath())
                 || chosenFile.getParent().equals(RemoteFileUtility.getInstance(mFragment.getActivity()).getHomeCloudRootPath())) {
            while(mMenuList.size() > 1){
                mMenuList.remove(mMenuList.size() - 1);
            }
            isShowInfoOnly = true;
        }

        if(chosenFile.getVFieType() != VFileType.TYPE_LOCAL_STORAGE){
            mMenuList.remove(5);
        }else{
            if(mFragment.isInCategory()){
                mMenuList.remove(5);
            }
            if(mFragment.getmIndicatorFile() != null && (mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_IMAGE_FILE) || mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_FAVORITE_FILE))){
                while(mMenuList.size() > 1){
                    mMenuList.remove(mMenuList.size() - 1);
                }
                isShowInfoOnly = true;

                if(mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_FAVORITE_FILE)){
                    mMenuList.add(getResources().getString(R.string.open));
//                    mMenuList.add(getResources().getString(R.string.remove_favorite));
                    mMenuList.add(getResources().getString(R.string.rename));
                }
            }

            boolean isFavoriteFolder = chosenFile.isFavoriteFile();
            if(!mFragment.isInCategory() && !isFavoriteFolder && chosenFile.isDirectory()){
                mMenuList.add(getResources().getString(R.string.add_favorite));
            }
            if(isFavoriteFolder){
                mMenuList.add(getResources().getString(R.string.remove_favorite));
//                mMenuList.add(getResources().getString(R.string.rename));
            }
//            menu.findItem(R.id.menu_favorite_add).setVisible(!mFragment.isInCategory() && !isFavoriteFolder && chosenFile.isDirectory());
            
//          menu.findItem(R.id.menu_favorite_remove).setVisible(isFavoriteFolder);
        }
        
        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
        .setAdapter(new ArrayAdapter<String>(getActivity(),android.R.layout.simple_expandable_list_item_1,mMenuList), this)
        .create();
        
        return dialog;
        
    }
    
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which >= mMenuList.size()){
            return;
        }
        VFile[] choseArray = new VFile[1];
        choseArray[0] = mSelectFile;
        Activity mActivity = mFragment.getActivity();
        
        String menuTitle = mMenuList.get(which);
        if(menuTitle.equals(getResources().getString(R.string.info))){
            mFragment.showDialog(DialogType.TYPE_INFO_DIALOG, mSelectFile);
        }else if(menuTitle.equals(getResources().getString(R.string.copy))){
            if(ItemOperationUtility.isItemContainDrm(choseArray,true,false)){
                if(mFragment != null){
                    if(mSelectFile.isFile()){
                        ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                    }else{
                        ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                    }
                }
            }else{
                mFragment.copyFileInPopup(choseArray,false);
            }
        }else if(menuTitle.equals(getResources().getString(R.string.cut))){
            if(ItemOperationUtility.isItemContainDrm(choseArray,true,false)){
                if(mFragment != null){
                    if(mSelectFile.isFile()){
                        ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                    }else{
                        ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                    }
                }
            }else{
                mFragment.cutFileInPopup(choseArray,false);
            }
        }else if(menuTitle.equals(getResources().getString(R.string.delete))){
            mFragment.deleteFileInPopup(choseArray);
        }else if(menuTitle.equals(getResources().getString(R.string.action_zip))){
            VFile[] array = new VFile[1];
            array[0] = mSelectFile;

            boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(mSelectFile.getAbsolutePath());
            if (bNeedWriteToAppFolder) {
                WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                    .newInstance();
                warnDialog.show(mActivity.getFragmentManager(),
                    "WarnKKSDPermissionDialogFragment");
            }else if(SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(mSelectFile.getAbsolutePath())){
                if(mActivity instanceof FileManagerActivity){
                    ((FileManagerActivity)mActivity).callSafChoose(SafOperationUtility.ACTION_ZIP);
                }
            }else{
                ZipDialogFragment.showZipDialog(mFragment, array, false);
            }
        }else if(menuTitle.equals(getResources().getString(R.string.rename))){
            if(mFragment.getmIndicatorFile().equals(mFragment.mActivity.CATEGORY_FAVORITE_FILE)){
                mFragment.renameFavoriteFile(mSelectFile);
            }else{
                if(ItemOperationUtility.isItemContainDrm(choseArray,true,false)){
                    if(mFragment != null){
                        if(mSelectFile.isFile()){
                            ToastUtility.show(mFragment.getActivity(), R.string.single_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                        }else{
                            ToastUtility.show(mFragment.getActivity(), R.string.multi_drm_file_forbidden_operation, Toast.LENGTH_LONG);
                        }
                    }
                }else{
                    boolean bNeedWriteToAppFolder = SafOperationUtility.getInstance(mActivity).isNeedToWriteSdToAppFolder(mSelectFile.getAbsolutePath());
                    if (bNeedWriteToAppFolder) {
                        WarnKKSDPermissionDialogFragment warnDialog = WarnKKSDPermissionDialogFragment
                            .newInstance();
                        warnDialog.show(mActivity.getFragmentManager(),
                            "WarnKKSDPermissionDialogFragment");
                    } else if(SafOperationUtility.getInstance(mActivity).isNeedToShowSafDialog(mSelectFile.getAbsolutePath())){
                        if(mActivity instanceof FileManagerActivity){
                            ((FileManagerActivity)mActivity).callSafChoose(SafOperationUtility.ACTION_RENAME);
                        }
                    }else{
                        mFragment.showDialog(DialogType.TYPE_RENAME_DIALOG, mSelectFile);
                    }
                }
            }
            
        }else if(menuTitle.equals(getResources().getString(R.string.add_favorite))){
            mFragment.addFavoriteFile(mSelectFile);
            GaAccessFile.getInstance().sendEvents(mActivity, GaAccessFile.CATEGORY_NAME,
                    GaAccessFile.ACTION_ADD_TO_FAVORITE, GaAccessFile.LABEL_FROM_MENU, Long.valueOf(1));
        }else if(menuTitle.equals(getResources().getString(R.string.remove_favorite))){
            mFragment.removeFavoriteFile(choseArray, false);
        }else if(menuTitle.equals(getResources().getString(R.string.open))){
            mFragment.startScanFile(mSelectFile, ScanType.SCAN_CHILD);
        }
    }
    
    
}