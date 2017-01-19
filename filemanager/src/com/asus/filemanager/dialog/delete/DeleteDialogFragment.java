package com.asus.filemanager.dialog.delete;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.AnalyzerAllFilesActivity;
import com.asus.filemanager.activity.AnalyzerAllFilesFragment;
import com.asus.filemanager.activity.AnalyzerDupFilesActivity;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.SearchResultFragment;
import com.asus.filemanager.activity.ViewPagerActivity;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.Editable;
import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.editor.EditorUtility;
import com.asus.filemanager.ga.GaAccessFile;
import com.asus.filemanager.ga.GaRecycleBin;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.filemanager.samba.SambaMessageHandle;
import com.asus.filemanager.samba.SambaVFile;
import com.asus.filemanager.functionaldirectory.recyclebin.RecycleBinVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.remote.utility.RemoteFileUtility;
import com.asus.remote.utility.RemoteVFile;
import com.asus.service.cloudstorage.common.HandlerCommand;

public abstract class DeleteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    protected boolean isPermanentlyDelete;

    public enum  Type {
        TYPE_DELETE_DIALOG,
        TYPE_CALCULTE_SPACE_PROGRESS_DIALOG,
        TYPE_NO_SPACE_DIALOG,
        TYPE_PROGRESS_DIALOG,
        TYPE_RECYCLE_BIN_PROGRESS_DIALOG
    }

    public static DeleteDialogFragment newInstance(EditPool editPool, Type typeDialog) {
        DeleteDialogFragment fragment= createInstance(editPool, typeDialog);
        Bundle args = new Bundle();
        args.putSerializable("editpool", editPool);
        args.putSerializable("type", typeDialog);
        args.putInt("count", editPool.getFiles().length);
        fragment.setArguments(args);
        return fragment;
    }

    private static DeleteDialogFragment createInstance(EditPool editpool, Type typeDialog) {
        switch (typeDialog) {
            case TYPE_DELETE_DIALOG:
                if (editpool.getFile().alwaysPermanentlyDelete())
                    return new DeleteConfirmFragment();
                else
                    return new RecycleConfirmFragment();
            case TYPE_CALCULTE_SPACE_PROGRESS_DIALOG:
                return new CalculateFileLengthFragment();
            case TYPE_NO_SPACE_DIALOG:
                return new NoSpaceFragment();
            default:
                return new DeleteProgressFragment();
        }
    }

    protected EditorUtility.RequestFrom getDeleteRequester() {
        Activity attachedActivity = getActivity();
        return ((Editable) attachedActivity).getRequester();
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Dialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                    .create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    dialog.dismiss();
                }
            });
            return dialog;
        }
        return createDialog(savedInstanceState);
    }

    public abstract Dialog createDialog(Bundle savedInstanceState);

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == Dialog.BUTTON_POSITIVE)
            handleDelete();
        else if (which == Dialog.BUTTON_NEGATIVE)
            handleCancel();
    }

    protected void handleDelete() {
        Activity attachedActivity = getActivity();
        if (!(attachedActivity instanceof Editable))
            return;
        EditPool editpool = (EditPool) getArguments().getSerializable("editpool");
        if (attachedActivity instanceof FileManagerActivity){
            if (editpool != null && editpool.getSize() > 0) {
                if (editpool.getFile().getVFieType() == VFile.VFileType.TYPE_CLOUD_STORAGE) {
                    String accountName = ((RemoteVFile)editpool.getFile()).getStorageName();
                    RemoteVFile[] remoteVFiles = new RemoteVFile[editpool.getFiles().length];
                    VFile[] tempVFiles = editpool.getFiles();
                    for (int i=0 ; i<editpool.getSize() ; i++) {
                        remoteVFiles[i] = (RemoteVFile) tempVFiles[i];
                    }
                    int msgObjType = ((RemoteVFile)editpool.getFile()).getMsgObjType();
                    RemoteFileUtility.getInstance(getActivity()).sendCloudStorageMsg(accountName, null, remoteVFiles, msgObjType, HandlerCommand.CloudStorageServiceHandlerMsg.MSG_APP_REQUEST_DELETE_FILES);
                }//++yiqiu_huang, handle samba case
                else if (editpool.getFile().getVFieType() == VFile.VFileType.TYPE_SAMBA_STORAGE){
                    SambaVFile[] remoteVFiles = new SambaVFile[editpool.getFiles().length];
                    VFile[] tempVFiles = editpool.getFiles();
                    for (int i=0 ; i<editpool.getSize() ; i++) {
                        remoteVFiles[i] = (SambaVFile) tempVFiles[i];
                    }
                    SambaFileUtility.getInstance(getActivity()).sendSambaMessage(SambaMessageHandle.FILE_DELETE, remoteVFiles, -1, null);
                }
                ((FileManagerActivity) getActivity()).displayDialog(
                        isPermanentlyDelete ?
                                FileManagerActivity.DialogType.TYPE_DELETEPROGRESS_DIALOG
                                : FileManagerActivity.DialogType.TYPE_MOVE_TO_RECYCLEBIN_PROGRESS_DIALOG
                        , editpool);
                if (editpool.getFiles()[0] instanceof RecycleBinVFile) {
                    GaRecycleBin.getInstance().sendAction(getActivity(),
                            GaRecycleBin.Action.Delete, editpool.getFiles().length);
                } else {
                    GaAccessFile.getInstance().sendEvents(getActivity(),
                            GaAccessFile.ACTION_DELETE, editpool.getFile().getVFieType(), -1, editpool.getFiles().length);
                }
            }
            FileListFragment.sIsDeleteComplete = false;
        } else if (editpool != null && editpool.getSize() > 0) {
            if (attachedActivity instanceof ViewPagerActivity) {
                FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                    deleteFiles(editpool, (ViewPagerActivity) attachedActivity, fileListFragment != null ? fileListFragment.belongToCategoryFromMediaStore() : false);
                GaAccessFile.getInstance().sendEvents(getActivity(),
                        GaAccessFile.ACTION_DELETE, editpool.getFile().getVFieType(), -1, editpool.getFiles().length);
        }else if (attachedActivity instanceof AnalyzerAllFilesActivity){
                    deleteFiles(editpool, (AnalyzerAllFilesActivity) getActivity(), false);
                    GaStorageAnalyzer.getInstance().sendEvents(getActivity(), GaStorageAnalyzer.CATEGORY_NAME,
                            GaStorageAnalyzer.ACTION_DELETE, null, (long) editpool.getFiles().length);
            } else if (attachedActivity instanceof AnalyzerDupFilesActivity) {
                    deleteFiles(editpool, (AnalyzerDupFilesActivity) getActivity(), false);
                GaStorageAnalyzer.getInstance().sendEvents(getActivity(), GaStorageAnalyzer.CATEGORY_NAME,
                        GaStorageAnalyzer.ACTION_DELETE, null , (long)editpool.getFiles().length);
            }
        }
    }

    private void deleteFiles(EditPool editpool, Editable editable, boolean isInCategory) {
                DeleteDialogFragment deleteProgressDialogFragment = DeleteDialogFragment.newInstance(editpool,
                        isPermanentlyDelete? Type.TYPE_PROGRESS_DIALOG : Type.TYPE_RECYCLE_BIN_PROGRESS_DIALOG);
                deleteProgressDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
                // delete local file case
        EditorAsyncHelper.deletFile(editpool.getFiles(), editable.getEditHandler(), isInCategory, isPermanentlyDelete);
    }

    protected void handleCancel() {
        Activity attachedActivity = getActivity();
        if (attachedActivity instanceof FileManagerActivity){
            //FileListFragment.sIsDeleteComplete = true;
            if(((FileManagerActivity)getActivity()).getIsShowSearchFragment()){
                SearchResultFragment mSFragment = (SearchResultFragment)getFragmentManager().findFragmentById(R.id.searchlist);
                if(mSFragment != null){
                    mSFragment.deleteComplete(true);
                }
            }else{
                FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
                if (fileListFragment != null) {
                    fileListFragment.deleteComplete();
                }
            }
        }else if (attachedActivity instanceof ViewPagerActivity){
            //just dismissed and do nothing
        }else if (attachedActivity instanceof AnalyzerAllFilesActivity){
            AnalyzerAllFilesFragment analyzerAllFilesFragment = (AnalyzerAllFilesFragment) getFragmentManager().findFragmentById(R.id.activity_analyzer_allfiles_fragment);
            if(analyzerAllFilesFragment!=null) {
                analyzerAllFilesFragment.onBackPressed();
            }
        }
    }
}
