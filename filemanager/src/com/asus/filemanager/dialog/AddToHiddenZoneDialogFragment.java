package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.WindowManager;

import com.asus.filemanager.R;
import com.asus.filemanager.dialog.delete.DeleteDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.utility.ThemeUtility;

/**
 * Created by Yenju_Lai on 2016/5/10.
 */
public class AddToHiddenZoneDialogFragment extends DialogFragment {
    public static AddToHiddenZoneDialogFragment newInstance(EditPool editPool, boolean inCategory) {
        AddToHiddenZoneDialogFragment fragment = new AddToHiddenZoneDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("editpool", editPool);
        args.putInt("count", editPool.getFiles().length);
        args.putBoolean("inCategory", inCategory);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog;
        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        dialog = new ProgressDialog(getActivity(),
                themeAsusLightDialogAlertId == 0 ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId);
        ((ProgressDialog) dialog).setMessage(getResources().getString(R.string.move_to_hidden_zone_progress));
        ((ProgressDialog) dialog).setIndeterminate(true);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setCancelable(false);

        return dialog;
    }
    public static void dismissFragment(Activity activity) {
        DialogFragment fragment = (DialogFragment)activity.getFragmentManager().findFragmentByTag("AddToHiddenZoneDialogFragment");
        if (fragment != null)
            fragment.dismissAllowingStateLoss();
    }
}
