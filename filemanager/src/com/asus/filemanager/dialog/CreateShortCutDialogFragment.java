package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.ga.GaShortcut;
import com.asus.filemanager.ui.HintLayout;
import com.asus.filemanager.ui.ShortCutHintLayout;
import com.asus.filemanager.utility.CreateShortcutUtil;
import com.asus.filemanager.utility.ThemeUtility;

/**
 * Created by Tim_Lin on 2016/2/23.
 */
public class CreateShortCutDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private int mViewId = -1;
    private String mPath="";
    private String mStorageName = "";

    public static CreateShortCutDialogFragment newInstance(int viewId) {
        CreateShortCutDialogFragment fragment = new CreateShortCutDialogFragment();
        Bundle args = new Bundle();
        args.putInt("viewId", viewId);
        fragment.setArguments(args);
        return fragment;
    }

    public static CreateShortCutDialogFragment newInstance(String path, String storageName) {
        CreateShortCutDialogFragment fragment = new CreateShortCutDialogFragment();
        Bundle args = new Bundle();
        args.putString("path", path);
        args.putString("storageName", storageName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mViewId = getArguments().getInt("viewId");
        mPath = getArguments().getString("path");
        mStorageName= getArguments().getString("storageName");

        AlertDialog dialog;
        TextView titleView = new TextView(getActivity().getApplicationContext());
        titleView.setText(getText(R.string.action_edit));
        titleView.setTextSize(20);
        titleView.setTextColor(Color.parseColor("#6FC9EA"));
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(20, 20, 20, 20);

        dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                .setCustomTitle(titleView)
                .setPositiveButton(R.string.create_shortcut_dialog, this)
                .create();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

        if (which == AlertDialog.BUTTON_POSITIVE && !TextUtils.isEmpty(mPath) && !TextUtils.isEmpty(mStorageName)) {
            CreateShortcutUtil.createFolderShortcut(getActivity(), mPath, mStorageName);
            GaShortcut.getInstance().sendEvents(getActivity(), GaShortcut.CATEGORY_NAME,
                GaShortcut.ACTION_CREATE_FROM_HOMEPAGE, GaShortcut.LABEL_NON_CATEGORY, null);
            // disable hint layout if need
            HintLayout hintLayout = ShortCutHintLayout.getCurrentInstance(getActivity());
            if(hintLayout != null)
                hintLayout.removeHintLayout();

        } else if (which == AlertDialog.BUTTON_POSITIVE && mViewId > -1) {
            CreateShortcutUtil.createCategoryShortcut(getActivity(), mViewId);
            GaShortcut.getInstance().sendEvents(getActivity(), GaShortcut.CATEGORY_NAME,
                GaShortcut.ACTION_CREATE_FROM_HOMEPAGE, GaShortcut.LABEL_CATEGORY, null);
            // disable hint layout if need
            HintLayout hintLayout = ShortCutHintLayout.getCurrentInstance(getActivity());
            if(hintLayout != null)
                hintLayout.removeHintLayout();
        }
    }
}
