package com.asus.filemanager.ui;

import android.app.Activity;
import android.app.Fragment;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ThemeUtility;

import static com.asus.filemanager.R.drawable.asus_ic_hint_edit;

/**
 * Created by Tim_Lin on 2016/7/28.
 */

public class EditCategoryHintLayout extends HintLayout {

    private static final String TAG = ShortCutHintLayout.class.getSimpleName();

    public EditCategoryHintLayout(Activity activity, Fragment fragment) {
        super(activity, fragment);
    }

    @Override
    protected void AddHintView() {
// find edit icon
        ImageView editIcon = (ImageView) mActivity.findViewById(R.id.category_edit);
        if(editIcon == null){
            Log.e(TAG, "editIcon null");
            return;
        }

        // get icon location
        int[] currentEditIconLocation = new int[2];
        editIcon.getLocationOnScreen(currentEditIconLocation);

        // new hint text
        int textOffset = 30;
        TextView hintText = newHintText(editIcon.getHeight() + textOffset,
                currentEditIconLocation, getResources().getString(R.string.editcategory_hint));
        RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) hintText.getLayoutParams();
        // customize edit category text to align parent right
        p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        hintText.setLayoutParams(p);
        // new ok button and add it to hint layout
        int btnOffset = 160;
        // adjust btn offset by font scale value
        float fontScale = getResources().getConfiguration().fontScale;
        if(fontScale > 0) {
            btnOffset *= fontScale;
        }
        Button okBtn = newHintButton(R.id.hint_categoryedit_ok);
        p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) getResources().getDimension(R.dimen.hintlayout_margin);
        p.setMargins(margin, 0, 0, 0);
        okBtn.setLayoutParams(p);
        // btn location need to align icon
        Log.d(TAG, "okbtn width:" + okBtn.getWidth());
        okBtn.setX(currentEditIconLocation[0] - (int) getResources().getDimension(R.dimen.normal_btn_width) + editIcon.getWidth()/2);
        okBtn.setY(currentEditIconLocation[1] + editIcon.getHeight() + btnOffset);

        // new three icon to present custom animation
        ImageView duplicateIcon_b1 = newDuplicateIcon(asus_ic_hint_edit, currentEditIconLocation);
        ImageView duplicateIcon_b2 = newDuplicateIcon(asus_ic_hint_edit, currentEditIconLocation);
        ImageView duplicateIcon_f = newDuplicateIcon(asus_ic_hint_edit, currentEditIconLocation);

        initThreeIconScaleAnimation(duplicateIcon_b1, duplicateIcon_b2, duplicateIcon_f);

        addView(duplicateIcon_b1);
        addView(duplicateIcon_b2);
        addView(duplicateIcon_f);
        addView(hintText);
        addView(okBtn);
    }
}
