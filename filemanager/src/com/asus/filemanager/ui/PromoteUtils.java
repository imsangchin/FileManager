package com.asus.filemanager.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.asus.filemanager.R;

public class PromoteUtils {
    public static final String TAG = PromoteUtils.class.getSimpleName();
    private static final int mTimeoutMs = 20000;

    public static ActivityToast showPromoteToastWithDrawable(Activity activity,
        CharSequence message, int iconId,
        String oldText, int newDrawableId,
        final OnPromoteClickListener listener,
        final String name
    ) {
        return showPromoteToast(activity, message, null, null, iconId, false, oldText, newDrawableId, listener, name);
    }

    public static ActivityToast showPromoteToast(Activity activity,
        CharSequence message, CharSequence underlineMessage, int iconId,
        final OnPromoteClickListener listener,
        final String name
    ) {
        return showPromoteToast(activity, message, underlineMessage, null, iconId, true, null, 0, listener, name);
    }

    public static ActivityToast showPromoteToastWithoutUnderline(Activity activity,
        CharSequence message, CharSequence underlineMessage, int iconId,
        final OnPromoteClickListener listener,
        final String name
    ) {
        return showPromoteToast(activity, message, underlineMessage, null, iconId, false, null, 0, listener, name);
    }

    public static ActivityToast showPromoteToastWithTargetTextUnderline(Activity activity,
        CharSequence message, CharSequence underlineMessage, CharSequence targetMessage,
        int iconId, final OnPromoteClickListener listener,
        final String name
    ) {
        return showPromoteToast(activity, message, underlineMessage, targetMessage, iconId, true, null, 0, listener, name);
    }

    private static ActivityToast showPromoteToast(Activity activity,
        CharSequence message, CharSequence underlineMessage, CharSequence targetMessage,
        int iconId, boolean underline,
        String oldText, int newDrawableId,
        final OnPromoteClickListener listener,
        final String name
    ) {
        try {
            LayoutInflater inflater = LayoutInflater.from(activity);
            View toastView = inflater.inflate(R.layout.promote_dialog, null);
            TextView permoteLink = (TextView) toastView.findViewById(R.id.promote_link);
            if (underlineMessage != null) {
                SpannableString tryItString = new SpannableString(underlineMessage);
                if (underline) {
                    int startUnderline = 0, endUnderline = tryItString.length();
                    if (targetMessage != null) {
                        int targetIndex = underlineMessage.toString().indexOf(targetMessage.toString());
                        if (targetIndex != -1) {
                            startUnderline = targetIndex;
                            endUnderline = targetIndex + targetMessage.length();
                        }
                    }
                    tryItString.setSpan(new UnderlineSpan(), startUnderline, endUnderline, 0);
                }
                permoteLink.setText(tryItString);
                permoteLink.setTextColor(Color.DKGRAY);
            } else {
                permoteLink.setVisibility(View.GONE);
            }

            ImageView cancelButton = (ImageView) toastView.findViewById(R.id.promote_cancel_button);
            cancelButton.setColorFilter(Color.DKGRAY);

            TextView promoteMessage = (TextView) toastView.findViewById(R.id.promote_message);
            promoteMessage.setText(message);
            replaceTextToDrawable(promoteMessage, oldText, newDrawableId);

            ImageView promoteIcon = (ImageView) toastView.findViewById(R.id.promote_icon);
            promoteIcon.setImageResource(iconId);

            toastView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int height = toastView.getMeasuredHeight();

            final ActivityToast toast = new ActivityToast(activity, toastView);

            Animation inTranslate = new TranslateAnimation(0, 0, height, 0.0f);
            Animation outTranslate = new TranslateAnimation(0, 0, 0, height);
            Animation inAlpha = new AlphaAnimation(0.3f, 1.0f);
            Animation outAlpha = new AlphaAnimation(1.0f, 0.3f);

            AnimationSet inAnimationSet = new AnimationSet(false);
            inAnimationSet.addAnimation(inTranslate);
            inAnimationSet.addAnimation(inAlpha);
            AnimationSet outAnimationSet = new AnimationSet(false);
            outAnimationSet.addAnimation(outTranslate);
            outAnimationSet.addAnimation(outAlpha);

            toast.setShowAnimation(inAnimationSet);
            toast.setCancelAnimation(outAnimationSet);

            toast.setDuration(mTimeoutMs, new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (name != null) {
                        /*
                        AsusGalleryTracker.sendTipClick(
                            EPhotoAppImpl.getAppContext(),
                            name,
                            AsusTracker.TrackerEvents.Label_Timeout
                        );
                        */
                    }
                }
            });

            toast.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onClick();
                        toast.cancel();
                    }
                    if (name != null) {
                        /*
                        AsusGalleryTracker.sendTipClick(
                            EPhotoAppImpl.getAppContext(),
                            name,
                            AsusTracker.TrackerEvents.Label_Edit
                        );
                        */
                    }
                }
            });

            cancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onCancel();
                        toast.cancel();
                    }
                    if (name != null) {
                        /*
                        AsusGalleryTracker.sendTipClick(
                            EPhotoAppImpl.getAppContext(),
                            name,
                            AsusTracker.TrackerEvents.Label_Close
                        );
                        */
                    }
                }
            });

            toast.show();
            return toast;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static abstract class OnPromoteClickListener {
        public abstract void onClick();
        public void onCancel() {}
    }

    private static void replaceTextToDrawable(TextView textView, String oldText, int newDrawableId) {
        if (null == oldText) {
            return ;
        }
        Context context = textView.getContext();
        Drawable drawable = context.getResources().getDrawable(newDrawableId);
        String textContent = textView.getText().toString();
        int index = textContent.indexOf(oldText);
        SpannableString spannableText = new SpannableString(textContent);
        if (drawable != null && index >= 0) {
            ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
            spannableText.setSpan(imageSpan, index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            int drawableSize = (int)(1.5f * textView.getTextSize());
            drawable.setBounds(0, 0, drawableSize, drawableSize);
        }
        textView.setText(spannableText);
    }
}
