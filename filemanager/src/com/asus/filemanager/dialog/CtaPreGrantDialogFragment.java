package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.wrap.WrapEnvironment;

public class CtaPreGrantDialogFragment extends DialogFragment {

    public final static String DIALOG_TAG = "CtaPreGrantDialogFragment";
    private final static String PRE_GRANT_PERMISSION = "PRE_GRANT_PERMISSION";
    public static final int CTA_NO_RECORD = -1;
    public static final int CTA_REMEMBER_REFUSE = 0;
    public static final int CTA_REMEMBER_AGREE = 1;

    private OnCtaPreGrantDialogFragmentListener mListener;

    public interface OnCtaPreGrantDialogFragmentListener {
        public void onCtaPreGrantDialogConfirmed();
        public void onCtaPreGrantDialogDismissed();
    }


    public static CtaPreGrantDialogFragment newInstance() {
        CtaPreGrantDialogFragment fragment = new CtaPreGrantDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View cta_view = inflater.inflate(R.layout.cta_pre_grant_dialog, null);
        final CheckBox checkBox = (CheckBox)cta_view.findViewById(R.id.cta_pre_grant_dialog_checkBox);
        if(getPreGrantPermission(getActivity()) == CTA_NO_RECORD){
            checkBox.setChecked(true);
        }

        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(R.string.permission_dialog_title)
            .setPositiveButton(R.string.cta_agree, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onCtaPreGrantDialogConfirmed();
                        if(checkBox.isChecked()){
                            setPreGrantPermission(getActivity(),CTA_REMEMBER_AGREE);
                        }
                    }
                }
            })
            .setNegativeButton(R.string.cta_disagree, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onCtaPreGrantDialogDismissed();
                        if(checkBox.isChecked()){
                            setPreGrantPermission(getActivity(), CTA_REMEMBER_REFUSE);
                        }
                    }
                }
            })
            .create();
        dialog.setView(cta_view, spacing_left, spacing_top, spacing_right, spacing_bottom);
        setCancelable(false);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (checkBox.isChecked()) {
                    dialog.getButton(
                            DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                }
            }
        });
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if (isChecked) {
                    dialog.getButton(
                            DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                } else {
                    dialog.getButton(
                            DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
                }
            }
        });
        return dialog;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        manager.executePendingTransactions();
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnCtaPreGrantDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCtaPreGrantDialogFragmentListener");
        }
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
//        if (null != mListener)
//            mListener.onCtaPreGrantDialogDismissed();
    }


    private void setPreGrantPermission(Context context,int value){
        SharedPreferences sp = context.getSharedPreferences("MyPrefsFile", 0);
        if(sp != null){
            SharedPreferences.Editor ed = sp.edit();
            ed.putInt(PRE_GRANT_PERMISSION, value);
            ed.commit();
        }
    }

    private static int getPreGrantPermission(Context context){
        int value = CTA_NO_RECORD;
        SharedPreferences sp = context.getSharedPreferences("MyPrefsFile", 0);
        if(sp != null){
            value = sp.getInt(PRE_GRANT_PERMISSION, CTA_NO_RECORD);
        }
        return value;
    }

    public static int checkCtaPreGrantPermission(Activity anActivity){
        return checkCtaPreGrantPermission(anActivity,true);
    }

    public static int checkCtaPreGrantPermission(Activity anActivity, boolean bShowPopup){
        if(!WrapEnvironment.IS_CN_DEVICE){
            return CTA_REMEMBER_AGREE;
        }
        int status = getPreGrantPermission(anActivity);
        if(status!=CTA_REMEMBER_AGREE && bShowPopup) {
            CtaPreGrantDialogFragment ctaDialog = CtaPreGrantDialogFragment.newInstance();
            ctaDialog.show(anActivity.getFragmentManager(), CtaPreGrantDialogFragment.DIALOG_TAG);
        }
        return status;
    }
}
