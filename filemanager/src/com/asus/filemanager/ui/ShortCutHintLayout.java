package com.asus.filemanager.ui;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.CategoryItem;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.utility.PrefUtils;

/**
 * Created by Tim_Lin on 2016/7/28.
 */

public class ShortCutHintLayout extends HintLayout {

    private static final String TAG = ShortCutHintLayout.class.getSimpleName();
    private int mCategoryId;

    public ShortCutHintLayout(Activity activity, Fragment fragment, int categoryId) {
        super(activity, fragment);
        mCategoryId = categoryId;
    }

    @Override
    protected void AddHintView() {
        TableLayout tableLayout = (TableLayout)mActivity.findViewById(R.id.tablelayout);
        if(tableLayout == null) {
            Log.e(TAG, "tableLayout null");
            return;
        }
        // find first icon and get position
        ImageView imageIcon = (ImageView) tableLayout.findViewWithTag(ConstantsUtil.ICON_TAG + mCategoryId);
        if(imageIcon == null){
            Log.e(TAG, "imageIcon null");
            return;
        }

        int[] currentImageIconLocation = new int[2];
        imageIcon.getLocationOnScreen(currentImageIconLocation);

        // new icon and add it to hint layout
        // new three icon to present custom animation
        ImageView duplicateIcon_b1 = newDuplicateCategoryIcon(mCategoryId, currentImageIconLocation);
        ImageView duplicateIcon_b2 = newDuplicateCategoryIcon(mCategoryId, currentImageIconLocation);
        ImageView duplicateIcon_f = newDuplicateCategoryIcon(mCategoryId, currentImageIconLocation);


        initThreeIconScaleAnimation(duplicateIcon_b1, duplicateIcon_b2, duplicateIcon_f);

        // new hint text
        int textOffset = 30;
        TextView hintText = newHintText(imageIcon.getHeight() + textOffset,
                currentImageIconLocation, getResources().getString(R.string.shortcut_hint));

        // new ok button and add it to hint layout
        int btnOffset = 170;
        // adjust btn offset by font scale value
        float fontScale = getResources().getConfiguration().fontScale;
        if(fontScale > 0) {
            btnOffset *= fontScale;
        }

        Button okBtn = newHintButton(R.id.hint_shortcut_ok);
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) getResources().getDimension(R.dimen.hintlayout_margin);
        p.setMargins(margin, 0, 0, 0);
        okBtn.setLayoutParams(p);
        // btn location need to align icon
        okBtn.setX(currentImageIconLocation[0] - imageIcon.getWidth()/3);
        okBtn.setY(currentImageIconLocation[1] + imageIcon.getHeight() + btnOffset);

        addView(duplicateIcon_b1);
        addView(duplicateIcon_b2);
        addView(duplicateIcon_f);
        addView(hintText);
        addView(okBtn);
    }

    public void show() {
        super.show();
        PrefUtils.setBooleanNeedToShowShortcutHint(mActivity, false);
    }

    private ImageView newDuplicateCategoryIcon(int id, int[] location){
        ImageView duplicateIcon = new ImageView(mActivity);
        duplicateIcon.setId(id);
        duplicateIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        duplicateIcon.setImageDrawable(CategoryItem.findDrawableById(mActivity.getResources(), id));
        duplicateIcon.setBackgroundResource(R.drawable.category_circle);
        Drawable background = duplicateIcon.getBackground();
        CategoryItem.setBackgroundColorAndRetainShape(mActivity.getResources().getColor(CategoryItem.findColorIdById(id)),background);

        duplicateIcon.setTag(ConstantsUtil.LAYOUT_HINT);
        duplicateIcon.setX(location[0]);
        duplicateIcon.setY(location[1]);
        int height = (int) getResources().getDimension(R.dimen.category_image_height);
        int width = (int) getResources().getDimension(R.dimen.category_image_height);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(height, width);
        duplicateIcon.setLayoutParams(layoutParams);
        if(mFragment != null) mFragment.registerForContextMenu(duplicateIcon);
        duplicateIcon.setOnClickListener((android.view.View.OnClickListener) mFragment);
        return duplicateIcon;
    }

}
