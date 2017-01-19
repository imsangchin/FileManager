package com.asus.filemanager.dialog;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class RecommendDialogFragment extends DialogFragment {
    private static final String TAG = "RecommendDialogFragment";
    public static final int mode_recommend_self = 1;
    public static final int mode_recommend_cm = 2;
    private final String KEY_MODE = "KEY_MODE";

    public int mode = 1;

    public static RecommendDialogFragment newInstance(int amode) {
        RecommendDialogFragment frag = new RecommendDialogFragment();
        frag.mode = amode;
        return frag;
    }

    public static RecommendDialogFragment newInstance() {
        RecommendDialogFragment frag = new RecommendDialogFragment();
        return frag;
    }

	public interface OnRecommendDialogFragmentListener {
        void onRecommendDialogDialogConfirmed(int mode);
        void onRecommendDialogDismissed(int mode);
        void onRecommendDialogNever(int mode);
    }
	private OnRecommendDialogFragmentListener mListener;

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.recommend_dialog, null);
        // recommend dialog theme no change in different theme, get lighDialogAlertId directly
        int themeAsusLightDialogAlertId = ThemeUtility.sThemeAsusLightDialogAlertId;
        int spacing_left = getResources().getDimensionPixelOffset(R.dimen.dialog_layout_spacing_left);
        int spacing_top = getResources().getDimensionPixelOffset(R.dimen.dialog_layout_spacing_top);
        int spacing_right = getResources().getDimensionPixelOffset(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = getResources().getDimensionPixelOffset(R.dimen.dialog_layout_spacing_bottom);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), themeAsusLightDialogAlertId == 0 ?
                -                        AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : themeAsusLightDialogAlertId);

        final TextView link = (TextView) view.findViewById(R.id.recommend_text);

        Typeface typeface = Typeface.createFromAsset(
                getResources().getAssets(),"fonts/Roboto-Regular.ttf");

        if (savedInstanceState != null) {
            mode = savedInstanceState.getInt(KEY_MODE);
        }
        if (mode == mode_recommend_self){
            //set content
            String aAppTitle = getResources().getString(R.string.file_manager);
            String feedbackAndHelp = getResources().getString(R.string.asus_feedback_and_help);
            String aRecommendText =
                getResources().getString(R.string.rating_dialog_message_v3_1, aAppTitle) +
                    "\n" +
                    getResources().getString(R.string.rating_dialog_message_v3_2, feedbackAndHelp);
            link.setText(aRecommendText);
            link.setMovementMethod(LinkMovementMethod.getInstance());
            link.setTypeface(typeface);

            // Change the view to new layout to meet spec.
            view.findViewById(R.id.recommend_dialog_background_layout).setVisibility(View.VISIBLE);
            view.findViewById(R.id.stars).setVisibility(View.VISIBLE);
            view.findViewById(R.id.recommend_dialog_title).setVisibility(View.VISIBLE);
            view.findViewById(R.id.imageView1).setVisibility(View.GONE);
            view.findViewById(R.id.recommend_dialog_button).setVisibility(View.VISIBLE);

            /*
            //set title
            builder.setTitle(R.string.toolbar_item_title_encourage_us);
            */
            TextView titleView = (TextView) view.findViewById(R.id.recommend_dialog_title);
            titleView.setTypeface(typeface, Typeface.BOLD);

            Button dialogButton = (Button) view.findViewById(R.id.recommend_dialog_button);
            dialogButton.setTypeface(typeface);
            dialogButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v)
                {
                    if (mListener != null)
                    {
                        mListener.onRecommendDialogDialogConfirmed(mode);
                    }
                }
            });

            //set action
            /*
            builder.setNegativeButton(R.string.rate_later, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onRecommendDialogDismissed(mode);
                    }
                }

            });
            */

            /*
            builder.setPositiveButton(R.string.rate_now, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onRecommendDialogDialogConfirmed(mode);
                    }
                }

            });
            */
        }else if (mode == mode_recommend_cm){
            //set content
            String aRecommandText = getResources().getString(R.string.recommend_clean_master_msg);
            link.setText(aRecommandText);
            link.setMovementMethod(LinkMovementMethod.getInstance());

            //set title
            builder.setTitle(R.string.recommend_clean_master_title);

            //set action
            builder.setNegativeButton(R.string.cancel, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onRecommendDialogDismissed(mode);
                    }
                }

            });

            builder.setPositiveButton(R.string.ok, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (null != mListener) {
                        mListener.onRecommendDialogDialogConfirmed(mode);
                    }
                }

            });
        }


/*
        builder.setNeutralButton(R.string.notagain, new OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
            	FlurryParams = new HashMap<String, String>(); 
                FlurryParams.put("Not Again", "true");
    	        ((GlobalVariable)getActivity().getApplicationContext()).logFlurryEvent("Main - Click encourage us",FlurryParams);
                if (null != mListener) {
            		mListener.onRecommendDialogNever();
                }		
            }

        });
*/
        AlertDialog dialog = builder.create();
        if (mode == mode_recommend_self)
        {
            dialog.setView(view);
        }
        else
        {
            dialog.setView(view, spacing_left, spacing_top, spacing_right, spacing_bottom);
        }
        dialog.setCanceledOnTouchOutside(false);

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
            mListener = (OnRecommendDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnRecommendDialogFragmentListener");
        }
    }

	/*
    @Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Dialog  dialog = this.getDialog();
		if (null != dialog){
			int titleDividerId = getResources().getIdentifier("titleDivider", "id", "android");
			View titleDivider = dialog.findViewById(titleDividerId);
			if (titleDivider != null)
				titleDivider.setBackgroundColor(getResources().getColor(R.color.app_bg));	
		}
	}	
	*/

    @Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if (null != mListener){
			mListener.onRecommendDialogDismissed(mode);
        }
	}
    private int dpTOpx(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        int px = (int) (dp * scale + 0.5f);
        return px;
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_MODE, mode);
        super.onSaveInstanceState(outState);
    }
}
