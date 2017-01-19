package com.asus.filemanager.utility.permission;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.asus.filemanager.R;

import java.util.ArrayList;

public class PermissionDialog extends DialogFragment {

    public static final String TAG = "PermissionDialog";

    private Context mContext;
    private ArrayList<String> mPermissions;
    private int mRequest;
    private PermissionChecker mChecker;

    /**
     * View contains main information.
     */
    private ScrollView mScrollView;

    /**
     * {@link Button} shows when this page is not scrollable.
     */
    private Button mScrollViewButton;

    /**
     * {@link Button} shows when this page is scrollable.
     */
    private Button mButton;

    private TextView mInstructionTextView;

    /**
     * {@link OnClickListener} used in {@link PermissionDialog#mScrollViewButton} and {@link PermissionDialog#mButton}.
     */
    private OnClickListener mButtonListener = new OnClickListener() {

        @Override
        public void onClick(View view)
        {
            if (mRequest == PermissionManager.REQUEST_PERMISSION || mRequest == PermissionManager.RE_REQUEST_PERMISSION)
            {
                if (mChecker != null && mChecker.getManager() != null)
                {
                    mChecker.getManager().onReasonAccepted(mPermissions, mRequest);
                }
            }
            else
            {
                if (mChecker != null)
                {
                    mChecker.permissionDeniedForever(mPermissions);
                }
            }
        }
    };

    public static PermissionDialog newInstance(ArrayList<Integer> text, ArrayList<String> perms, int req) {
        PermissionDialog frag = new PermissionDialog();
        Bundle args = new Bundle();
        args.putIntegerArrayList("texts", text);
        args.putInt("req", req);
        frag.mRequest = req;
        args.putStringArrayList("perms", perms);
        frag.setArguments(args);
        return frag;
    }
    public static PermissionDialog newInstance(ArrayList<Integer> text, ArrayList<String> perms) {
        PermissionDialog frag = new PermissionDialog();
        Bundle args = new Bundle();
        args.putIntegerArrayList("texts", text);
        args.putStringArrayList("perms", perms);
        frag.setArguments(args);
        frag.mRequest = 0;
        return frag;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mChecker = (PermissionChecker) getActivity();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Activity doesn't implement PermissionChecker");
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        try {
            mChecker = (PermissionChecker) mContext;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Activity doesn't implement PermissionChecker");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mChecker = null;
        mContext = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // String message = "";
        // ArrayList<Integer> texts = getArguments().getIntegerArrayList("texts");
        mPermissions = getArguments().getStringArrayList("perms");
        if (savedInstanceState != null) {
            mRequest = savedInstanceState.getInt("req");
        } else {
            mRequest = getArguments().getInt("req");
        }

        String aPermissionGroup  = "storage";
        String aTitle = getActivity().getResources().getString(R.string.m_permission_setting_title_files);
        String aSubTitle = getActivity().getResources().getString(R.string.m_permission_setting_subtitle_files,
            getActivity().getResources().getString(R.string.file_manager),
            aPermissionGroup);

        for (String str : mPermissions){
            if (str.compareToIgnoreCase(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==0 ||
                str.compareToIgnoreCase(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 0) {
                aPermissionGroup = getPermissionGroupInfo(getActivity(),getPermissionInfo(getActivity(),str));
                aTitle = getActivity().getResources().getString(R.string.m_permission_setting_title_files);
                aSubTitle = getActivity().getResources().getString(R.string.m_permission_setting_subtitle_files,
                    getActivity().getResources().getString(R.string.file_manager),
                    aPermissionGroup);
                break;
            }
            if (str.compareToIgnoreCase(android.Manifest.permission.GET_ACCOUNTS) == 0){
                // aPermissionGroup = getPermissionGroupInfo(getActivity(),getPermissionInfo(getActivity(),str));
                aTitle = getActivity().getResources().getString(R.string.permission_dialog_title);
                aSubTitle = getActivity().getResources().getString(R.string.permission_reason_contact);
                break;
            }
        }

        Dialog dialog = new Dialog(getActivity(), R.style.Theme_FULLSCREEN_DIALOG);
        dialog.setContentView(R.layout.forced_permission_page);
        mScrollView = (ScrollView) dialog.findViewById(R.id.force_permission_scrollview);
        mScrollViewButton = (Button) dialog.findViewById(R.id.turn_on_button_scrollview);
        mButton = (Button) dialog.findViewById(R.id.turn_on_button);
        mInstructionTextView = (TextView) dialog.findViewById(R.id.force_permission_sub_instruction);
        if (mInstructionTextView != null) {
            String permissionLabel = getPermissionGroupInfo(mContext,
                    getPermissionInfo(mContext, mPermissions.get(0)));
            mInstructionTextView.setText(getResources().getString(R.string.m_permission_dialog_message,
                    getResources().getString(R.string.file_manager), permissionLabel));
        }

        TextView title = (TextView) dialog.findViewById(R.id.force_permission_title);
        title.setText(aTitle);
        TextView subtitle = (TextView) dialog.findViewById(R.id.force_permission_sub_title);
        subtitle.setText(aSubTitle);

        mScrollViewButton.setOnClickListener(mButtonListener);
        mButton.setOnClickListener(mButtonListener);

        dialog.setCancelable(true);
        setCancelable(true);
        updateInstructionLayoutVisibility();
        updateButtonText();
        checkLayout();

        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("req", mRequest);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }else{
            PermissionDialog permissionDialog = (PermissionDialog) (manager.findFragmentByTag(tag));
            permissionDialog.setRequest(this.mRequest);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (mChecker != null && mChecker.getManager() != null) {
            mChecker.getManager().onReasonRejected(mRequest);
         }
         dismissAllowingStateLoss();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Dialog dialog = getDialog();
        /*
        // get previous dialog title and subtitle
        TextView titleView = (TextView)(getDialog().getWindow().findViewById(R.id.force_permission_title));
        TextView subTitleView = (TextView)(getDialog().getWindow().findViewById(R.id.force_permission_sub_title));
        String title= titleView.getText().toString();
        String subTitle = subTitleView.getText().toString();

        // set current dialog title and subtitle
        getDialog().getWindow().setContentView(R.layout.forced_permission_page);
        TextView newTitleView = (TextView)getDialog().getWindow().findViewById(R.id.force_permission_title);
        TextView newSubTitleView = (TextView)getDialog().getWindow().findViewById(R.id.force_permission_sub_title);
        newTitleView.setText(title);
        newSubTitleView.setText(subTitle);
        */

        mScrollView = (ScrollView) dialog.findViewById(R.id.force_permission_scrollview);
        mScrollViewButton = (Button) dialog.findViewById(R.id.turn_on_button_scrollview);
        mButton = (Button) dialog.findViewById(R.id.turn_on_button);

        mScrollViewButton.setOnClickListener(mButtonListener);
        mButton.setOnClickListener(mButtonListener);

        checkLayout();
    }

    public int getRequest() {
        return mRequest;
    }

    public void setRequest(int request) {
        mRequest = request;
        updateInstructionLayoutVisibility();
        updateButtonText();
    }

    public static PermissionInfo getPermissionInfo(Context context, String requestedPerm) {
        PermissionInfo permInfo = null;
        try {
            permInfo = context.getPackageManager().getPermissionInfo(requestedPerm, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unknown permission: " + requestedPerm);
        }
        return permInfo;
    }

    public static String getPermissionGroupInfo(Context context, PermissionInfo permissionInfo) {
        PackageItemInfo groupInfo = null;
        if (permissionInfo.group != null) {
            try {
                groupInfo = context.getPackageManager().getPermissionGroupInfo(
                    permissionInfo.group, 0);
            } catch (PackageManager.NameNotFoundException e) {
                /* ignore */
            }
        }

        if(groupInfo != null)
        {
            return groupInfo.loadLabel(context.getPackageManager()).toString();
        }

        return null;
    }

    private void updateInstructionLayoutVisibility() {
        if (mRequest == PermissionManager.REQUEST_PERMISSION
                || mRequest == PermissionManager.RE_REQUEST_PERMISSION) {
            mInstructionTextView.setVisibility(View.GONE);
        } else {
            // Deny forever case
            mInstructionTextView.setVisibility(View.VISIBLE);
        }
    }

    private void updateButtonText() {
        if (mRequest == PermissionManager.REQUEST_PERMISSION
                || mRequest == PermissionManager.RE_REQUEST_PERMISSION) {
            if (mScrollViewButton != null) {
                mScrollViewButton.setText(R.string.m_permission_setting_turn_on);
            }
            if (mButton != null) {
                mButton.setText(R.string.m_permission_setting_turn_on);
            }
        } else {
            if (mScrollViewButton != null) {
                mScrollViewButton.setText(R.string.m_permission_dialog_positive_button);
            }
            if (mButton != null) {
                mButton.setText(R.string.m_permission_dialog_positive_button);
            }
        }
    }

    /**
     * Check layout of this page to determine whether we need to change
     * button which user can click it to grant permission.
     */
    private void checkLayout()
    {
        mScrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw()
            {
                if(isViewCanScroll())
                {
                    mScrollViewButton.setVisibility(View.GONE);
                    mButton.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
    }

    /**
     * Check current page that user can scroll or not.
     *
     * @return True means can scroll, false otherwise.
     */
    private boolean isViewCanScroll()
    {
        if (mScrollView.getMeasuredHeight() == 0)
        {
            // Trigger it to measure the bound size.
            final int spec = View.MeasureSpec.makeMeasureSpec(0,
                    View.MeasureSpec.UNSPECIFIED);
            mScrollView.measure(spec, spec);
        }

        View child = mScrollView.getChildAt(0);
        if (child != null)
        {
            int height = mScrollView.getHeight();
            int paddingTop = mScrollView.getPaddingTop();
            int paddingBottom = mScrollView.getPaddingBottom();
            int childHeight = child.getHeight();

            // If ScrollView's height is less than its child's height,
            // we can say now the ScrollView is scrollable.
            return height < childHeight + paddingTop + paddingBottom;
        }
        return false;
    }
}

