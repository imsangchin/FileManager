package com.asus.filemanager.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.asus.filemanager.R;

public class WhatsNewDialogFragment extends DialogFragment{

    public final static String DIALOG_TAG = WhatsNewDialogFragment.class.getSimpleName();

    public interface OnWhatsNewDialogFragmentListener {
        void onWhatsNewDialogConfirmed();
        void onWhatsNewDialogDismissed();
    }

    private OnWhatsNewDialogFragmentListener mListener;

    public static WhatsNewDialogFragment newInstance() {
        return new WhatsNewDialogFragment();
    }

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View aboutView = inflater.inflate(R.layout.news_message_dialog_layout, null);


        //String msg = getString(R.string.kk_sd_permission_warning);
        //((TextView) about_view.findViewById(R.id.msg)).setText(msg);
        TextView tv1 = (TextView)aboutView.findViewById(R.id.tv_news_1);
        TextView tv2 = (TextView)aboutView.findViewById(R.id.tv_news_2);
        TextView tv3 = (TextView)aboutView.findViewById(R.id.tv_news_3);
        if (null != tv1){
            tv1.setText(getResources().getText(R.string.news_v44));
        }
        if (null != tv2){
            tv2.setText(getResources().getText(R.string.news_v45));
        }
        if (null != tv3){
            tv3.setText(getResources().getText(R.string.news_v46));
        }

        /*
        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);

        int themeAsusLightDialogAlertId = ThemeUtility.sThemeAsusLightDialogAlertId;
        */

        // Set specific style for transparent background.
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.WhatsNewDialog);
        builder.setView(aboutView);

        Button button = (Button)aboutView.findViewById(R.id.news_message_dialog_button);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                if (mListener != null)
                {
                    mListener.onWhatsNewDialogConfirmed();
                }
                dismiss();
            }
        });
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        // For new style spec, we work all in custom view instead of
        // style view programmatically.
        /*
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
        */
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        manager.executePendingTransactions();
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        initListener(context);
    }

    // Use deprecation api for devices under api level 23.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        initListener(activity);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        Window window = getDialog().getWindow();
        if(window != null)
        {
            // This dialog has different layout based on different screen size.
            // When orientation change, simply set new layout to this dialog and
            // setup components in this dialog.
            window.setContentView(R.layout.news_message_dialog_layout);

            TextView tv1 = (TextView)window.findViewById(R.id.tv_news_1);
            TextView tv2 = (TextView)window.findViewById(R.id.tv_news_2);
            TextView tv3 = (TextView)window.findViewById(R.id.tv_news_3);

            if (null != tv1){
                tv1.setText(getResources().getText(R.string.news_v41));
            }
            if (null != tv2){
                tv2.setText(getResources().getText(R.string.news_v42));
            }
            if (null != tv3){
                tv3.setText(getResources().getText(R.string.news_v43));
            }

            Button button = (Button) window.findViewById(R.id.news_message_dialog_button);
            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v)
                {
                    if (mListener != null)
                    {
                        mListener.onWhatsNewDialogConfirmed();
                    }
                    dismiss();
                }
            });
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (null != mListener){
            mListener.onWhatsNewDialogDismissed();
        }
    }

    /**
     * Initialize {@link WhatsNewDialogFragment#mListener} if it is null.
     *
     * @param context The {@link Context} which this fragment is attached.
     */
    private void initListener(Context context)
    {
        if(mListener == null)
        {
            try
            {
                mListener = (OnWhatsNewDialogFragmentListener) context;
            }
            catch (ClassCastException e)
            {
                // we don't force implement
                //throw new ClassCastException(activity.toString() + " must implement OnWhatsNewDialogFragmentListener");
                //
            }
        }
    }
}
