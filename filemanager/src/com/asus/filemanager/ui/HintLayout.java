package com.asus.filemanager.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.ConstantsUtil;

/**
 * Created by Tim_Lin on 2016/1/4.
 */

public abstract class HintLayout extends RelativeLayout {
    protected Activity mActivity;
    protected Fragment mFragment;

//    private HintLayout mHintLayout = null;
    private static final String TAG = HintLayout.class.getSimpleName();

    private int BACKGROUND_COLOR = 0xE6000000;

    public HintLayout(Activity activity, Fragment fragment) {
        super((Context) activity);
        mActivity = activity;
        mFragment = fragment;
        initLayout();

        AddHintView();
    }

    public static HintLayout getCurrentInstance(Activity activity){
        return (HintLayout) activity.findViewById(R.id.hint_layout);
    }

    public void removeHintLayout(){
        RelativeLayout hintLayout = (RelativeLayout) mActivity.findViewById(R.id.hint_layout);
        if(hintLayout != null && hintLayout.isShown()){
            ViewGroup vg = (ViewGroup)(mActivity.getWindow().getDecorView().getRootView());
            if(vg == null) {
                Log.e(TAG, "vg null");
                return;
            }

            vg.removeView(hintLayout);
        }
    }

    public void show() {
        ViewGroup vg = (ViewGroup)(mActivity.getWindow().getDecorView().getRootView());
        if(vg == null) {
            Log.e(TAG, "vg null");
            return;
        }
        vg.addView(this);
    }

    private void initLayout() {
        setId(R.id.hint_layout);
        setTag(ConstantsUtil.LAYOUT_HINT);
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(p);
        setBackgroundColor(BACKGROUND_COLOR);
        bringToFront();
        setClickable(true);
    }

    protected abstract void AddHintView();

    protected void initThreeIconScaleAnimation(ImageView backImage1, ImageView backImage2, ImageView frontImage){
        backImage2.setScaleX(0.9f);
        backImage2.setScaleY(0.9f);
        backImage2.setAlpha(0.5f);

        frontImage.setScaleX(0.8f);
        frontImage.setScaleY(0.8f);

        ObjectAnimator animAlpha = ObjectAnimator.ofFloat(backImage1, "alpha", 0.3f, 1f);

        animAlpha.setRepeatMode(ValueAnimator.REVERSE);
        animAlpha.setRepeatCount(ValueAnimator.INFINITE);
        animAlpha.setDuration(800);

        AnimatorSet scaleDown = new AnimatorSet();

        scaleDown.play(animAlpha);
        scaleDown.start();
    }

    protected ImageView newDuplicateIcon(int drawableId, int[] location) {
        ImageView duplicateIcon = new ImageView(mActivity);
        duplicateIcon.setX(location[0]);
        duplicateIcon.setY(location[1]);
        duplicateIcon.setImageDrawable(ContextCompat.getDrawable(mActivity, drawableId));
        return duplicateIcon;
    }

    protected TextView newHintText(int offset, int[] location, String text){
        DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
        TextView hintText = new TextView(mActivity);
        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        int margin = (int) getResources().getDimension(R.dimen.hintlayout_margin);
        p.setMargins(margin, 0, margin, 0);
        hintText.setLayoutParams(p);
        hintText.setId(R.id.hint_text);
        hintText.setY(offset + location[1]);
        hintText.setWidth((int) (displayMetrics.widthPixels * 3 / 4));
        hintText.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.normal_btn_font_size));
        hintText.setTextColor(Color.WHITE);
        hintText.setText(text);
        return hintText;
    }

    protected Button newHintButton(int id){
        Button okBtn = new Button(mActivity);
        okBtn.setWidth((int) getResources().getDimension(R.dimen.normal_btn_width));
        okBtn.setHeight((int) getResources().getDimension(R.dimen.normal_btn_height));
        okBtn.setTag(ConstantsUtil.LAYOUT_HINT);
        okBtn.setId(id);
        okBtn.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.normal_btn_font_size));
        okBtn.setText(R.string.ok);
        okBtn.setTextColor(Color.WHITE);
        okBtn.setBackgroundResource(R.drawable.dialog_button_background);
        okBtn.setOnClickListener((android.view.View.OnClickListener) mFragment);
        return okBtn;
    }
}
