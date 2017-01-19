package com.asus.filemanager.utility;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.DeviceListAdapter;
import com.asus.filemanager.adapter.FileListAdapter;
import com.asus.filemanager.adapter.SearchListAdapter;
import com.asus.filemanager.samba.SambaFileUtility;
import com.asus.remote.utility.RemoteFileUtility;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FixedListFragment extends Fragment {
    /* this is used for SearchResultFragment, a derived class of FixedListFragment which only support list mode*/
    protected boolean bForceListMode;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
            mGrid.focusableViewAvailable(mGrid);
        }
    };

//    final private AdapterView.OnItemClickListener mOnClickListener
//            = new AdapterView.OnItemClickListener() {
//        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
//            onListItemClick((ListView)parent, v, position, id);
//        }
//    };

    ListAdapter mAdapter;
    ListView mList;
    View mEmptyView;
    TextView mStandardEmptyView;
    View mProgressContainer;
    View mListContainer;
    CharSequence mEmptyText;
//    boolean mListShown;
    boolean mContentViewShow;
    GridView mGrid;

    public FixedListFragment() {
        bForceListMode = false;
    }

    /**
     * Attach to list view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
    }

    /**
     * Detach from list view.
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mGrid = null;
        mContentViewShow = false;
        mEmptyView = mProgressContainer = mListContainer = null;
        mStandardEmptyView = null;
        super.onDestroyView();
    }

    /**
     * This method will be called when an item in the list is selected.
     * Subclasses should override. Subclasses can call
     * getListView().getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param l The ListView where the click happened
     * @param v The view that was clicked within the ListView
     * @param position The position of the view in the list
     * @param id The row id of the item that was clicked
     */
    public void onListItemClick(ListView l, View v, int position, long id) {
    }


    public void setFixAdapter(ListAdapter adapter){
        mAdapter = adapter;
    }

//    /**
//     * Provide the cursor for the list view.
//     */
//    public void setListAdapter(ListAdapter adapter) {
//        boolean hadAdapter = mAdapter != null;
//        mAdapter = adapter;
//        if (mList != null) {
//            if (!mList.isShown() && !hadAdapter) {
//                // The list was hidden, and previously didn't have an
//                // adapter.  It is now time to show it.
//                setListShown(true, getView().getWindowToken() != null);
//            }
//            if(!mGrid.isShown()){
//                mList.setAdapter(adapter);
//            }else{
//              mGrid.setAdapter(adapter);
//            }
//        }
//    }

    public void setListAdapter(ListAdapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mList != null) {
            mList.setAdapter(adapter);
            mGrid.setAdapter(adapter);
            if(ItemOperationUtility.getInstance().isListViewMode() || SambaFileUtility.updateHostIp
                    || RemoteFileUtility.isShowDevicesList || isForceListMode()){
                if(!mList.isShown()){
                    mList.setVisibility(View.VISIBLE);
                }
                if(mGrid.isShown()){
                    mGrid.setVisibility(View.GONE);
                }
                if(adapter instanceof FileListAdapter){
                    mList.setOnItemClickListener((FileListAdapter)adapter);
                    mList.setOnItemLongClickListener((FileListAdapter)adapter);
                    mGrid.setOnItemClickListener(null);
                    mGrid.setOnItemLongClickListener(null);
                }else if(adapter instanceof SearchListAdapter){
                    mList.setOnItemClickListener((SearchListAdapter)adapter);
                }else{
                    mList.setOnItemClickListener((DeviceListAdapter)adapter);
                    mList.setOnItemLongClickListener((DeviceListAdapter)adapter);
                }
            }else{
                mList.setVisibility(View.GONE);
                if(!mGrid.isShown()){
                    mGrid.setVisibility(View.VISIBLE);
                }
                if(adapter instanceof FileListAdapter){
                    mGrid.setOnItemClickListener((FileListAdapter)adapter);
                    mGrid.setOnItemLongClickListener((FileListAdapter)adapter);
                }else if(adapter instanceof SearchListAdapter){
                    mGrid.setOnItemClickListener((SearchListAdapter)adapter);
                }
            }



            if (!mList.isShown() && !hadAdapter) {
                // The list was hidden, and previously didn't have an
                // adapter.  It is now time to show it.
                setListShown(true, getView().getWindowToken() != null);
            }
        }
    }

    /**
     * Set the currently selected list item to the specified
     * position with the adapter's data
     *
     * @param position
     */
    public void setSelection(int position) {
        ensureList();
        mList.setSelection(position);
        mGrid.setSelection(position);
    }

    /**
     * Get the position of the currently selected list item.
     */
    public int getSelectedItemPosition() {
        ensureList();
        int position = -1;
        if(ItemOperationUtility.getInstance().isListViewMode() || isForceListMode()){
            position = mList.getSelectedItemPosition();
        }else{
            position = mGrid.getSelectedItemPosition();
        }
        return position;
    }

    /**
     * Get the cursor row ID of the currently selected list item.
     */
    public long getSelectedItemId() {
        ensureList();
        long itemId = 1;
        if(ItemOperationUtility.getInstance().isListViewMode() || isForceListMode()){
            itemId = mList.getSelectedItemId();
        }else{
            itemId = mGrid.getSelectedItemId();
        }
        return itemId;
    }

    /**
     * Get the activity's list view widget.
     */
    public ListView getListView() {
        ensureList();
        return mList;
    }

    public AbsListView getShowView(){
        ensureList();
        return mGrid.isShown() ? mGrid : mList;
    }

    /**
     * Get the activity's Grid view widget.
     */
    public GridView getGridView() {
        ensureList();
        return mGrid;
    }

    /**
     * The default content for a ListFragment has a TextView that can
     * be shown when the list is empty.  If you would like to have it
     * shown, call this method to supply the text it should use.
     */
    public void setEmptyText(CharSequence text, Drawable drawable, int index) {

        SpannableString spannableString = new SpannableString(text);

        ensureList();
        if (mStandardEmptyView == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }

        if (drawable != null && index >= 0) {
            ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
            spannableString.setSpan(imageSpan, index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            float textSize = mStandardEmptyView.getTextSize();
            // the image is just bigger than text for 1.5x.
            drawable.setBounds(0, 0, (int) (textSize * 1.5f) , (int) (textSize * 1.5f));
        }

        if (mEmptyText == null) {
            mGrid.setEmptyView(mStandardEmptyView);
            mList.setEmptyView(mStandardEmptyView);
        }
        mEmptyText = spannableString;

        mStandardEmptyView.setText(spannableString);
    }

    public void setEmptyText(CharSequence text) {
        setEmptyText(text, null, -1);
    }

    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * <p>Applications do not normally need to use this themselves.  The default
     * behavior of ListFragment is to start with the list not being shown, only
     * showing it once an adapter is given with {@link #setListAdapter(ListAdapter)}.
     * If the list at that point had not been shown, when it does get shown
     * it will be do without the user ever seeing the hidden state.
     *
     * @param shown If true, the list view is shown; if false, the progress
     * indicator.  The initial value is true.
     */
    public void setListShown(boolean shown) {
        try{
        setListShown(shown, true);
        }catch(IllegalStateException e){
            e.printStackTrace();
        }
    }

    /**
     * Like {@link #setListShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    public void setListShownNoAnimation(boolean shown) {
        try{
        setListShown(shown, false);
        }catch(IllegalStateException e){
            e.printStackTrace();
        }
    }
    public boolean isListShown(){
        return mContentViewShow;//mList.isShown() || mGrid.isShown();
    }
    /**
     * Control whether the list is being displayed.  You can make it not
     * displayed if you are waiting for the initial data to show in it.  During
     * this time an indeterminant progress indicator will be shown instead.
     *
     * @param shown If true, the list view is shown; if false, the progress
     * indicator.  The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     * new state.
     */
    private void setListShown(boolean shown, boolean animate) {
        ensureList();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mContentViewShow == shown) {
            return;
        }
        mContentViewShow = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
            modifyListAndGridVisibility();
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mListContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mListContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
        }
    }

    protected void modifyListAndGridVisibility() {
        if(ItemOperationUtility.getInstance().isListViewMode() || SambaFileUtility.updateHostIp
                || RemoteFileUtility.isShowDevicesList || isForceListMode()){
            mList.setVisibility(View.VISIBLE);
            mGrid.setVisibility(View.GONE);
        }else{
            mGrid.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        }
    }

    public void setContentViewAdapter(ListAdapter adapter){
        mAdapter = adapter;
    }

    /**
     * Get the ListAdapter associated with this activity's ListView.
     */
    public ListAdapter getListAdapter() {
        return mAdapter;
    }
    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof ListView) {
            mList = (ListView)root;
        } else {
            mStandardEmptyView = (TextView)root.findViewById(android.R.id.empty);
            if (mStandardEmptyView == null) {
                mEmptyView = root.findViewById(android.R.id.empty);
            } else {
                mStandardEmptyView.setVisibility(View.GONE);
            }
            mProgressContainer = root.findViewById(R.id.progressContainer);
            mListContainer = root.findViewById(R.id.listContainer);
            View rawListView = root.findViewById(android.R.id.list);
            View rawGridView = root.findViewById(R.id.content_gird);
            if (!(rawListView instanceof ListView)) {
                throw new RuntimeException(
                        "Content has view with id attribute 'android.R.id.list' "
                        + "that is not a ListView class");
            }
            mList = (ListView)rawListView;

            if (!(rawGridView instanceof GridView)) {
                throw new RuntimeException(
                        "Content has view with id attribute 'R.id.content_grid' "
                        + "that is not a GridView class");
            }

            mGrid = (GridView)rawGridView;

            if (mList == null) {
                throw new RuntimeException(
                        "Your content must have a ListView whose id attribute is " +
                        "'android.R.id.list'");
            }

            if (mGrid == null) {
                throw new RuntimeException(
                        "Your content must have a GridView whose id attribute is " +
                        "'R.id.content_grid'");
            }

            if (mEmptyView != null) {
                mList.setEmptyView(mEmptyView);
                mGrid.setEmptyView(mEmptyView);
            } else if (mEmptyText != null) {
                mStandardEmptyView.setText(mEmptyText);
                mList.setEmptyView(mStandardEmptyView);
                mGrid.setEmptyView(mStandardEmptyView);
            }
        }
        mContentViewShow = true;
//        mList.setOnItemClickListener(mOnClickListener);
//        mGrid.setOnItemClickListener(mOnClickListener);
        if (mAdapter != null) {
            ListAdapter adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setListShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }

    protected boolean isForceListMode() {
        return bForceListMode;
    }
}
