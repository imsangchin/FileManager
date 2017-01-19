package com.asus.filemanager.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.MoveToNaviAdapter;
import com.asus.filemanager.dialog.MoveToDialogFragment;
import com.asus.filemanager.ui.ContextualActionBar;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VolumeInfoUtility;
import com.asus.filemanager.utility.reflectionApis;
import com.asus.remote.utility.RemoteAccountUtility;
import com.google.android.gms.common.api.Scope;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Wesley_Lee on 2016/12/14.
 */

public class PathIndicatorPopupWindow extends PopupWindow implements View.OnClickListener {

    private static final int STORAGETYPE_TITLE = 0;
    private static final int STORAGETYPE_LOCAL = 1;
    private static final int STORAGETYPE_CLOUD = 2;
    private static final int STORAGETYPE_ADD_GOOGLE_ACCOUNT = 3;
    private static final int STORAGETYPE_NETWORK = 4;
    private static final int STORAGETYPE_ADD_CLOUD_ACCOUNT = 5;
    private static final int STORAGETYPE_SHORTCUT_PREVIOUS_FOLDER = 6;
    private static final int STORAGETYPE_SHORTCUT_CURRENT_FOLDER = 7;

    private Context mContext;
    private Activity mActivity;
    private PathIndicatorPopupWindowListener mListener;
    private LinkedList<MoveToNaviAdapter.StorageItemElement> mStorageItemElementList = new LinkedList<MoveToNaviAdapter.StorageItemElement>();

    @Override
    public void onClick(View view) {
        if (mListener != null) {

        }
    }

    public interface PathIndicatorPopupWindowListener {
        public void onPopupWindowPress(MoveToNaviAdapter.ClickIconItem clickIconItem);
    }

    public PathIndicatorPopupWindow(Context context, View anchor, int mode) {
        super();
        mContext = context;
        mActivity = (Activity) context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout layout= new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        // TextView tv = (TextView) layout.findViewById(R.id.popview_content);
        // tv.setText("wesley");

        ((FileManagerActivity) mActivity).initCloudService();
        final MoveToNaviAdapter adapter = ((FileManagerActivity) mActivity).getMoveToNaviAdapter();
        adapter.setCurrentFolder(new VFile(Environment.getExternalStorageDirectory().getPath()));
        adapter.setMode(mode);

        ScrollView scrollView = new ScrollView(mContext);
        scrollView.addView(layout);
        setContentView(scrollView);
        for (int i = 0; i < adapter.getCount(); i++) {
            View v = adapter.getView(i, null, null);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e("test", "onClick:" + view.toString());
                    if (mListener != null) {
                        MoveToNaviAdapter.ClickIconItem itemIcon;
                        itemIcon = (MoveToNaviAdapter.ClickIconItem) view.findViewById(
                                R.id.storage_list_item_container).getTag();
                        mListener.onPopupWindowPress(itemIcon);
                    }
                }
            });
            layout.addView(v);
        }
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setBackgroundDrawable(new ColorDrawable(Color.GRAY));
        setOutsideTouchable(true);
        showAsDropDown(anchor);
    }

    public void setListener(PathIndicatorPopupWindowListener listener) {
        mListener = listener;
    }

//    public void setVolumePaths(List<Object> volumes) {
//        if (volumes == null) {
//            return;
//        }
//        ViewGroup rootView = (ViewGroup) getContentView();
//        for (final Object volume : volumes) {
//            TextView tv = new TextView(mContext);
//            tv.setBackgroundColor(Color.BLUE);
//            tv.setText(VolumeInfoUtility.getInstance((Activity) mContext).findStorageTitleByStorageVolume(volume));
//            tv.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Log.e("test", "onClick:" + view.toString());
//                    if (mListener != null) {
//                        mListener.onPopupWindowPress(reflectionApis.volvolume));
//                    }
//                }
//            });
//            rootView.addView(tv);
//        }
//    }
}
