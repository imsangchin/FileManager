package com.asus.filemanager.utility;


import com.asus.filemanager.R;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.SearchView.OnCloseListener;

public class MySearchView extends SearchView /*implements OnCloseListener*/{

/*    private Context mContext;
    private long mHoveredInTime = 0l;
    private boolean mIsHoveredIn = false;
    private PopupWindow mPopView;
    private boolean mIsOpened = false;
*/
    public MySearchView(Context context) {
        this(context, null);
    }

    public MySearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int searchButtonId = context.getResources().getIdentifier("android:id/search_button", null, null);
        ImageView searchButton = (ImageView)findViewById(searchButtonId);
        searchButton.setImageResource(R.drawable.asus_ep_ic_search);
        searchButton.setFocusable(true);

        //int queryTextViewId = context.getResources().getIdentifier("android:id/search_src_text", null, null);
        //SearchAutoComplete queryTextView = (SearchAutoComplete) findViewById(queryTextViewId);
        //queryTextView.setTextColor(Color.WHITE);
/*
 	mContext = context;
        TextView tv;
        LayoutInflater inflater =
            (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.popup_action_menu, null);
        tv = (TextView)layout.findViewById(R.id.popview_content);
        tv.setText(mContext.getString(R.string.search_action));

        layout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

        mPopView = new PopupWindow(layout,
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);
        mPopView.setWidth(layout.getMeasuredWidth());
        mPopView.setHeight(layout.getMeasuredHeight());
        mPopView.setFocusable(false);
        mPopView.setTouchable(false);
        mPopView.setOutsideTouchable(false);
        setOnCloseListener(this);*/
    }
/*
    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {

        if (shouldDispathStayedHover(event)) {
            if (mPopView.isShowing()) {
                mPopView.update(this, -1, -1);
            } else {
                mPopView.showAsDropDown(this);
            }
        } else {
            if (mPopView.isShowing()) {
                mPopView.dismiss();
            }
        }

        return true;
    }

    private boolean shouldDispathStayedHover(MotionEvent event) {
        final int action = event.getAction();

        if (action == MotionEvent.ACTION_HOVER_EXIT || mIsOpened) {
            // Reset Hovered parameters
            mIsHoveredIn = false;
            mHoveredInTime = 0;
            return false;
        }

        boolean showHover = false;
        if (action == MotionEvent.ACTION_HOVER_ENTER
                || action == MotionEvent.ACTION_HOVER_MOVE) {
            if (!mIsHoveredIn) {
                mHoveredInTime = System.currentTimeMillis();
                mIsHoveredIn = true;
                showHover = false;
            } else {
                long currentTime = System.currentTimeMillis();
                showHover = (currentTime - mHoveredInTime) > 500 ? true : false;
            }
        }
        return showHover;
    }

    public void onSearchViewClick() {
        mIsOpened = true;
        mIsHoveredIn = false;
        mHoveredInTime = 0;
        if (mPopView.isShowing()) {
            mPopView.dismiss();
        }
    }

    @Override
    public boolean onClose() {
        mIsOpened = false;
        return false;
    }*/
}
