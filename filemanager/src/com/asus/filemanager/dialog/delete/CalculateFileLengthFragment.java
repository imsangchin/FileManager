package com.asus.filemanager.dialog.delete;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;

import com.asus.filemanager.R;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.ga.GaRecycleBin;
import com.asus.filemanager.functionaldirectory.CalculateUsableSpaceTask;
import com.asus.filemanager.utility.ThemeUtility;

public class CalculateFileLengthFragment extends DeleteDialogFragment
        implements DialogInterface.OnShowListener, CalculateUsableSpaceTask.OnSpaceCalculatedListener {

    @Override
    public Dialog createDialog(Bundle savedInstanceState) {
        isPermanentlyDelete = false;
        Dialog dialog;
        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        dialog = new ProgressDialog(getActivity(),
                themeAsusLightDialogAlertId == 0 ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId);
        ((ProgressDialog) dialog).setMessage(getResources().getString(
                R.string.calculate_progress));
        ((ProgressDialog) dialog).setIndeterminate(true);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        dialog.setOnShowListener(this);
        setCancelable(false);

        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        EditPool editpool = (EditPool) getArguments().getSerializable("editpool");
        new CalculateUsableSpaceTask(this).execute(editpool.getFiles());
    }

    @Override
    public void onSpaceCalculated(boolean isSufficient) {
        dismiss();
        if (isSufficient) {
            handleDelete();
        } else {
            DeleteDialogFragment noSpaceDialogFragment =
                    DeleteDialogFragment.newInstance((EditPool) getArguments().getSerializable("editpool"),
                            Type.TYPE_NO_SPACE_DIALOG);
            noSpaceDialogFragment.show(getFragmentManager(), "DeleteDialogFragment");
        }
        GaRecycleBin.getInstance().sendDeleteEvent(getActivity(),
                getDeleteRequester(), getArguments().getInt("count"),
                isSufficient ? GaRecycleBin.DeleteCategory.RecycleBin : GaRecycleBin.DeleteCategory.InsufficientStorage);
    }
}
