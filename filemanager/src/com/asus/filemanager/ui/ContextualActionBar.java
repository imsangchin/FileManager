package com.asus.filemanager.ui;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.ShoppingCart;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ContextualActionBar extends LinearLayout {
    public static int FLAG_HAS_DIRECTORY = 0x1;
    public static int FLAG_IS_IN_FAVORITE_CATEGORY = 0x2;

    private int mFlag = 0;
    private ImageView mMoveToView;
    private ImageView mCopyToView;
    private ImageView mShareView;
    private ImageView mDeleteView;
    private TextView mNumberOfSelect;

    public ContextualActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.e(TAG, "1");
        mContext = context;
        initViews();
    }

    public ContextualActionBar(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.e(TAG, "2");
        // mContext = context;
        // initViews();
    }

    public ContextualActionBar(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Log.e(TAG, "3");
        // mContext = context;
        // initViews();
    }

    private static final String TAG = "ContextualActionBar";
    private ShoppingCart mShoppingCart;
    private ContextualActionButtonListener mContextualActionButtonListener;
    private Context mContext;
    private OnClickListener innerListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            Log.e(TAG, "onClick: " + v);
            if (mContextualActionButtonListener != null) {
                mContextualActionButtonListener.onContextualActionButtonClick(v);
            }
        }
    };

    public interface ContextualActionButtonListener {
        public void onContextualActionButtonClick(View v);
    }

    private void initViews() {
        Log.e(TAG, "initViews");
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.cab_bar, this);

        mMoveToView = (ImageView) view.findViewById(R.id.cab_move_to);
        if (mMoveToView != null) {
            mMoveToView.setOnClickListener(innerListener);
        }
        mCopyToView = (ImageView) view.findViewById(R.id.cab_copy_to);
        if (mCopyToView != null) {
            mCopyToView.setOnClickListener(innerListener);
        }
        mShareView = (ImageView) view.findViewById(R.id.cab_share);
        if (mShareView != null) {
            mShareView.setOnClickListener(innerListener);
        }
        mDeleteView = (ImageView) view.findViewById(R.id.cab_delete);
        if (mDeleteView != null) {
            mDeleteView.setOnClickListener(innerListener);
        }
        mNumberOfSelect = (TextView) view.findViewById(R.id.cab_number_of_select);
        if (mNumberOfSelect != null) {
            mNumberOfSelect.setOnClickListener(innerListener);
        }
    }

    public void updateItemVisibility(int flag) {
        mFlag = flag;
        updateMoveToItemVisibility();
        updateCopyToItemVisibility();
        updateShareItemVisibility();
        updateDeleteItemVisibility();
    }

    private void updateMoveToItemVisibility() {
        if (mMoveToView == null) {
            return;
        }
        mMoveToView.setVisibility(
            (mFlag & FLAG_IS_IN_FAVORITE_CATEGORY) != 0 ? View.GONE : View.VISIBLE);
    }

    private void updateCopyToItemVisibility() {
        if (mCopyToView == null) {
            return;
        }
        mCopyToView.setVisibility(
                (mFlag & FLAG_IS_IN_FAVORITE_CATEGORY) != 0 ? View.GONE : View.VISIBLE);
    }

    private void updateShareItemVisibility() {
        if (mShareView == null) {
            return;
        }
        mShareView.setVisibility(
                (mFlag & FLAG_HAS_DIRECTORY) != 0 ? View.GONE : View.VISIBLE);
    }

    private void updateDeleteItemVisibility() {
        if (mDeleteView == null) {
            return;
        }
        mDeleteView.setVisibility(
                (mFlag & FLAG_IS_IN_FAVORITE_CATEGORY) != 0 ? View.GONE : View.VISIBLE);
    }

    public void notifyDataSetChanged() {
        if (mNumberOfSelect != null) {
            mNumberOfSelect.setText(String.valueOf(mShoppingCart.getSize()));
        }
    }

    public void setShoppingCart(ShoppingCart shoppingCart) {
        mShoppingCart = shoppingCart;
    }

    public void setContextualActionButtonListener(ContextualActionButtonListener listener) {
        mContextualActionButtonListener = listener;
    }

    public void removeContextualActionButtonListeners() {
        mContextualActionButtonListener = null;
    }

}
