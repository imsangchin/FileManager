package com.asus.filemanager.ui;

import com.asus.filemanager.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class HudToastAnimation implements Runnable {
    private Context context;
    private boolean mAnimationRepeat = false;
    public static int TOAST_RATING = 0;
    public static int TOAST_TUTORIAL = 1;
    private int mToastMode;

    public HudToastAnimation(Context context) {
        this.context = context;
    }

    public HudToastAnimation(Context context, int aMode) {
        this.context = context;
        this.mToastMode = aMode;
    }

    @Override
    public void run() {
        showHud();
    }

    private void showHud() {
        ViewGroup view = getRatingView(context);
        if (mToastMode == TOAST_TUTORIAL){
            TextView aTextView = (TextView)view.findViewById(R.id.text);
            if (null != aTextView){
                aTextView.setText(R.string.tools_tip_animation);
                //aTextView.setBackgroundColor(Color.argb(0,0,0,0));
            }
        }
        view.setBackgroundColor(Color.argb(80, 0, 0, 0));
        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(view);
        toast.setGravity(Gravity.FILL, 0, 100);
        toast.show();
        
        ImageView circle = (ImageView) view.findViewById(R.id.circle);
        AnimationSet animationSet = new AnimationSet(true);
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, context.getResources().getInteger(R.integer.scroll_toast_translate_y));
        translateAnimation.setDuration(1300);
        translateAnimation.setStartOffset(480);
        translateAnimation.setFillAfter(true);
//        translateAnimation.setRepeatCount(1);
//        translateAnimation.setRepeatMode(Animation.RESTART);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(500);
//        alphaAnimation.setRepeatCount(1);
        alphaAnimation.setStartOffset(0);
//        alphaAnimation.setRepeatMode(Animation.RESTART);

        ScaleAnimation scaleAnimation = new ScaleAnimation(1.5f, 1f, 1.5f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(500);
//        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setStartOffset(0);
//        scaleAnimation.setRepeatMode(Animation.RESTART);

        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.addAnimation(scaleAnimation);
        mAnimationRepeat = false;
        animationSet.setAnimationListener(new AnimationListener(){

            @Override
            public void onAnimationStart(Animation animation) {
                // TODO Auto-generated method stub
                Log.d("xxx", "onAnimationStart");
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // TODO Auto-generated method stub
                Log.d("xxx", "onAnimationEnd");
                if (!mAnimationRepeat) {
                    animation.start();
                    mAnimationRepeat = true;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub

            }

        });
        circle.startAnimation(animationSet);
    }

    public ViewGroup getRatingView(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context.getApplicationContext());
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.animation_toast, null);
        return view;
    }
}