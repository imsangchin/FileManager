package com.asus.filemanager.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.commonui.swipeablelistview.SwipeableListView;
import com.asus.filemanager.R;
import com.asus.filemanager.utility.ColorfulLinearLayout;
import com.asus.filemanager.utility.ThemeUtility;

public class CategorySortingActivity extends BaseActivity implements CategoryPreference.OnSaveCallback {

    public static final String TAG = "CategorySortingActivity";
    public static final String kKeyNewFeatureIconCategoryDocument = "key_new_feature_icon_category_document";
    public static final String kKeyNewFeatureIconCategoryGame = "key_new_feature_icon_category_game";

    private static class CategorySortingAdapter extends BaseAdapter implements OnCheckedChangeListener, CategoryPreference.OnLoadCallback, OnClickListener {

        private static class ViewHolder {
            public CheckBox isChecked;
            public ImageView icon;
            public TextView name;
            public ImageView newFeature;
        };

        private Context mContext;
        private List<CategoryItem> mCategorys;
        private List<CategoryItem> mPreviousCategorys;

        public CategorySortingAdapter(Context context) {
            mContext = context;
            mCategorys = null;
            mPreviousCategorys = null;
        }

        public void updateCategorys(List<CategoryItem> categorys) {
            mCategorys = categorys;
            notifyDataSetChanged();
        }

        public List<CategoryItem> getPreivousCategorys() {
            return mPreviousCategorys;
        }

        public List<CategoryItem> getCategorys() {
            return mCategorys;
        }

        public void dragEnd(int from, int to) {
            mCategorys.add(to, mCategorys.remove(from));
            notifyDataSetChanged();
        }

        public int getCount() {
            return (null == mCategorys ? 0 : mCategorys.size());
        }

        public Object getItem(int position) {
            return mCategorys.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int pos, View contentView, ViewGroup parent) {

            // bind view
            ViewHolder holder = null;
            if (contentView == null) {
                contentView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_swipeable, null);
                holder = new ViewHolder();
                holder.isChecked = (CheckBox)contentView.findViewById(R.id.is_checked);
                holder.icon = (ImageView)contentView.findViewById(R.id.icon);
                holder.name = (TextView)contentView.findViewById(R.id.name);
                holder.newFeature = (ImageView)contentView.findViewById(R.id.new_feature_icon);

                holder.isChecked.setOnCheckedChangeListener(this);
                contentView.setOnClickListener(this);
                contentView.setTag(holder);
            } else {
                holder = (ViewHolder)contentView.getTag();
            }

            // update view's data
            CategoryItem item = mCategorys.get(pos);
            holder.isChecked.setTag(pos);
            holder.isChecked.setChecked(item.isChecked);
            if (CategoryItem.IMAGE == item.id || CategoryItem.MUSIC == item.id ||
                CategoryItem.VIDEO == item.id
            ) {
                holder.isChecked.setEnabled(false);
                contentView.setOnClickListener(null);
            } else {
                holder.isChecked.setEnabled(true);
                contentView.setOnClickListener(this);
            }
            holder.icon.setImageDrawable(item.icon);
            ThemeUtility.setItemIconColor(mContext, holder.icon.getDrawable());
            holder.name.setText(item.name);
            /*
            if (CategoryItem.DOCUMENT == item.id || CategoryItem.COMPRESSED == item.id ||
                CategoryItem.RECENT == item.id || CategoryItem.LARGE_FILE == item.id ||
                CategoryItem.PDF == item.id
            ) {
                updateNewFeatureIcon(mContext, kKeyNewFeatureIconCategoryDocument, holder.newFeature);
            } else if (CategoryItem.GAME == item.id) {
                updateNewFeatureIcon(mContext, kKeyNewFeatureIconCategoryGame, holder.newFeature);
            } else {
                holder.newFeature.setVisibility(View.GONE);
            }
            */

            return contentView;
        }

        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            int pos = (Integer)view.getTag();
            CategoryItem item = mCategorys.get(pos);
            item.isChecked = isChecked;
            mCategorys.set(pos, item);
            if (CategoryPreference.extractSelectedCategoryItems(mCategorys).size() > 9) {
                Toast.makeText(mContext, R.string.category_toast_max_item_size, Toast.LENGTH_SHORT).show();
                item.isChecked = false;
                mCategorys.set(pos, item);
                notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadDone(List<CategoryItem> categorys) {
            Log.d(TAG, "onLoadDone");
            mCategorys = categorys;
            mPreviousCategorys = categroysClone(categorys);
            CategoryItem.fillSmallDrawable(categorys, mContext.getResources());
            notifyDataSetChanged();
        }

        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick");
            int viewId = view.getId();
            if (R.id.item == viewId) {
                ViewHolder holder = (ViewHolder)view.getTag();
                holder.isChecked.setChecked(!holder.isChecked.isChecked());
            }
        }
    }

    private CategorySortingAdapter mAdapter;
    private SwipeableListView mListView;
    private SwipeableListView.DragListener mDragListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        ColorfulLinearLayout.setContentView(this, R.layout.activity_category_sorting, R.color.theme_color);
        initActionBar();
        bindView();
        CategoryPreference.postLoadFromPreference(this, mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_category_sorting, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            setResult(RESULT_CANCELED);
            finish();
            break;
        case R.id.action_save:
            if (!isValidItemSize(this, mAdapter.getCategorys())) {
                break;
            }
            CategoryPreference.postSaveToPreference(this, this, mAdapter.getCategorys());
            break;
        case R.id.action_discard:
            setResult(RESULT_CANCELED);
            finish();
            break;
//        case R.id.action_restore_default: {
//            Resources res = getResources();
//            List<CategoryItem> categorys = CategoryPreference.createDefaultCategorys(res);
//            CategoryItem.fillSmallDrawable(categorys, res);
//            mAdapter.updateCategorys(categorys);
//            GATracker.sendEvents(this, GATracker.TrackerEvents.CATEGORY_SORTING,
//              GATracker.TrackerEvents.ACTION_SORTING_RESULT_RESET, null, null
//          );
//        } break;
        } // end of switch
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveDone() {
        if (!categorysEquals(mAdapter.getPreivousCategorys(), mAdapter.getCategorys())) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    private void initActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setSplitBackgroundDrawable(getResources().getDrawable(
                R.drawable.asus_ep_edit_bar_bg_wrap));
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(this.getResources().getString(R.string.category_title_category));
        actionBar.setIcon(android.R.color.transparent);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    private void bindView() {
        mListView = (SwipeableListView) findViewById(R.id.about_list);
        // set adapter
        mAdapter = new CategorySortingAdapter(this);
        mListView.setAdapter(mAdapter);
        // set DragListener
        mDragListener = new SwipeableListView.DragListener() {
            @Override
            public void onDragEnd(int from, int to) {
                mAdapter.dragEnd(from, to);
            }

            @Override
            public void onDragStart(int pos) {
                // Do something
            }
        };
        mListView.enableDrag(true);
        mListView.setDragListener(mDragListener);
    }

    private static void updateNewFeatureIcon(Context context, String key, ImageView newFeatureIcon) {
        if (newFeatureIcon != null) {
            SharedPreferences pref = context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
            if (pref.getBoolean(key, true)) {
                newFeatureIcon.setVisibility(View.VISIBLE);
            } else {
                newFeatureIcon.setVisibility(View.GONE);
            }
        }
    }

    private static boolean isValidItemSize(Context context, List<CategoryItem> categorys) {
        List<CategoryItem> selectedCategorys = CategoryPreference.extractSelectedCategoryItems(categorys);
        int sumOfIsChecked = selectedCategorys.size();
//        Log.v(TAG, "sumOfIsChecked=" + sumOfIsChecked);
        if (sumOfIsChecked != 6 && sumOfIsChecked != 9) {
            Toast.makeText(context, R.string.category_toast_invalid_item_size_6_or_9, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private static boolean categorysEquals(List<CategoryItem> categorys1, List<CategoryItem> categorys2) {
        List<CategoryItem> selected1 = CategoryPreference.extractSelectedCategoryItems(categorys1);
        List<CategoryItem> selected2 = CategoryPreference.extractSelectedCategoryItems(categorys2);
        int size1 = selected1.size();
        int size2 = selected2.size();
//        Log.e(TAG, "size1=" + size1 + ", size2=" + size2);
        if (size1 != size2) {
            return false;
        }
        for (int i = 0; i < size1; ++i) {
            if (selected1.get(i).id != selected2.get(i).id) {
                return false;
            }
        }
        return true;
    }

    private static List<CategoryItem> categroysClone(List<CategoryItem> categorys) {
        List<CategoryItem> clone = new ArrayList<CategoryItem>();
        for (CategoryItem item: categorys) {
            clone.add(new CategoryItem(item));
        }
        return clone;
    }
}
