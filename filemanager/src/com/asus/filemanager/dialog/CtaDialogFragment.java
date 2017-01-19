package com.asus.filemanager.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.wrap.WrapEnvironment;

public class CtaDialogFragment extends DialogFragment{

    public final static String DIALOG_TAG = "CtaDialogFragment";
    private final static String ACCESS_NETWORK_USER_PERMISSION = "access_network_permission";

    public interface OnCtaDialogFragmentListener {
        public void onCtaDialogConfirmed(boolean bRemember);
        public void onCtaDialogDismissed(boolean bRemember);
    }
    private OnCtaDialogFragmentListener mListener;

    public static CtaDialogFragment newInstance() {
        CtaDialogFragment fragment = new CtaDialogFragment();
        return fragment;
    }

    private Context mContext;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View cta_view = inflater.inflate(R.layout.cta_dialog, null);
        final CheckBox aCheckBox = (CheckBox)cta_view.findViewById(R.id.cta_checkBox);
        if(getUserPermission(getActivity()) != CTA_NO_RECORD){
            aCheckBox.setChecked(true);
            // aCheckBox.setEnabled(false);
        }

        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        mContext = getActivity();
        final AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(R.string.cta_dialog_title)
            .setPositiveButton(R.string.cta_agree, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onCtaDialogConfirmed(aCheckBox.isChecked());
                        setUserPermission(mContext, aCheckBox.isChecked() ?
                                CTA_REMEMBER_AGREE : CTA_NO_RECORD);
                    }
                }
            })
            .setNegativeButton(R.string.cta_disagree, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onCtaDialogDismissed(aCheckBox.isChecked());
                        setUserPermission(mContext, aCheckBox.isChecked() ?
                                CTA_REMEMBER_REFUSE : CTA_NO_RECORD);
                    }
                }
            })
            .create();
        dialog.setView(cta_view, spacing_left, spacing_top, spacing_right, spacing_bottom);

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (aCheckBox.isChecked()) {
                    dialog.getButton(
                            DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                }
            }
        });
        aCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
    /*
    @Override
    public void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        Dialog dialog = this.getDialog();
        if (null != dialog){
            // Get posButton and set it up
            Button posButton = ((Button)dialog.findViewById(android.R.id.button1));
            if(posButton.getLayoutParams() != null) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) posButton.getLayoutParams();
                params.setMargins(25, 15, 25, 15);
                params.gravity = Gravity.CENTER;
                posButton.setLayoutParams(params);
                posButton.setPadding(25, 15, 25, 15);
                posButton.setBackgroundResource(R.drawable.round_button);
                posButton.setTextColor(Color.WHITE);
            }

            int titleDividerId = getResources().getIdentifier("titleDivider", "id", "android");
            View titleDivider = dialog.findViewById(titleDividerId);
            if (titleDivider != null)
                titleDivider.setBackgroundColor(Color.parseColor("#007fa0"));
        }
    }
    */
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
            mListener = (OnCtaDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCtaDialogFragmentListener");
        }
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        boolean bRemember = false;
        Dialog aDialog= this.getDialog();
        if (null != aDialog){
            final CheckBox aCheckBox = (CheckBox)aDialog.findViewById(R.id.cta_checkBox);
            bRemember = aCheckBox.isChecked();
        }
        if (null != mListener){
            mListener.onCtaDialogDismissed(bRemember);
        }
    }
    private static void setUserPermission(Context context,int value){
        SharedPreferences sp = context.getSharedPreferences("MyPrefsFile", 0);
        if (sp != null) {
            SharedPreferences.Editor ed = sp.edit();
            ed.putInt(ACCESS_NETWORK_USER_PERMISSION, value);
            ed.commit();
        }
    }

    private static int getUserPermission(Context context){
        int value = CTA_NO_RECORD;
        SharedPreferences sp = context.getSharedPreferences("MyPrefsFile", 0);
        if (sp != null) {
            value = sp.getInt(ACCESS_NETWORK_USER_PERMISSION, CTA_NO_RECORD);
        }
        return value;
    }
    private boolean queryPermission(Context context){
        boolean hasRecord = true;
        int status = getUserPermission(context);// -1 noRecord; 0 remember refuse ;1 remember agree
        if (status == CTA_NO_RECORD) {
            hasRecord = false;
        }
        boolean result = status == CTA_REMEMBER_AGREE ? true : false;
        return hasRecord;
    }

    public static boolean enableCtaCheck(){
        return WrapEnvironment.IS_CN_DEVICE;
    }

    public static final int CTA_NO_RECORD = -1;
    public static final int CTA_REMEMBER_REFUSE = 0;
    public static final int CTA_REMEMBER_AGREE= 1;
    public static int checkCtaPermission(Activity anActivity){
        return checkCtaPermission(anActivity,true);
    }
    public static int checkCtaPermission(Activity anActivity, boolean bShowPopup){
        if(!enableCtaCheck()){
            return CTA_REMEMBER_AGREE;
        }
        int status = getUserPermission(anActivity);// -1 noRecord; 0 remember refuse ;1 remember agree

        if (CTA_REMEMBER_REFUSE == status) {
            ToastUtility.show(anActivity, anActivity.getResources().getString(R.string.network_cta_hint));
        }else if (CTA_REMEMBER_AGREE == status){
            //nothing special
        }else if (bShowPopup){
            //ask user
            CtaDialogFragment ctaDialog = CtaDialogFragment.newInstance();
            ctaDialog.show(anActivity.getFragmentManager(), CtaDialogFragment.DIALOG_TAG);
        }
        return status;

    }
}
