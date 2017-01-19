package com.asus.filemanager.hiddenzone;

import com.asus.filemanager.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class KeyboardButtonView extends RelativeLayout {

    private static final String TAG = "KeyboardButtonView";
    private static final int RIPPLE_EFFECT_COLOR = Color.LTGRAY;

    private Context mContext;
    private ImageView mImageView;
    private TextView mTextView;
    private boolean mIsSetWidthAndHeightDone;

    public KeyboardButtonView(Context context) {
        this(context, null);
    }

    public KeyboardButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyboardButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mContext = context;
        initializeView(attrs, defStyleAttr);
    }

    private void initializeView(AttributeSet attrs, int defStyleAttr) {
        if (attrs != null && !isInEditMode()) {
            final TypedArray attributes = mContext.getTheme().obtainStyledAttributes(attrs, R.styleable.KeyboardButtonView,
                    defStyleAttr, 0);
            String text = attributes.getString(R.styleable.KeyboardButtonView_lp_keyboard_button_text);

            boolean supportRippleDrawable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            KeyboardButtonView view = (KeyboardButtonView) inflater.inflate(R.layout.hidden_zone_view_keyboard_button, this);

            if (text != null) {
                mTextView = (TextView) view.findViewById(R.id.keyboard_button_textview);
                mTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (!mIsSetWidthAndHeightDone) {
                            adjustTextViewSize();
                            adjustTextSize();
                        }
                    }
                });
                if (mTextView != null) {
                    mTextView.setText(text);
                    if (TextUtils.isDigitsOnly(text)) {
                        setCircularBackground();
                    }

                    // TODO: Remove it?
                    if (supportRippleDrawable) {
                        view.setBackground(new RippleDrawable(new ColorStateList(new int[][]{{}},
                                new int[]{RIPPLE_EFFECT_COLOR}), null, null));
                    } else {
                        StateListDrawable sd = new StateListDrawable();
                        sd.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.LTGRAY));
                        view.setBackground(sd);
                    }
                }
            }
        }
    }

    private void setCircularBackground() {
        if (mTextView != null) {
            mTextView.setBackgroundResource(R.drawable.textview_circular_background);
        }
    }

    private void adjustTextViewSize() {
        if (mTextView != null) {
            CharSequence text = mTextView.getText();
            int size = (int) (Math.min(((View) mTextView.getParent()).getMeasuredWidth(),
                    ((View) mTextView.getParent()).getMeasuredHeight()) - (2 * mContext.getResources()
                            .getDimension(R.dimen.lockscreen_keyboard_button_padding_top_and_bottom)));

            if (TextUtils.isDigitsOnly(text)) {
                int definedMaxTextSize = (int) (mContext.getResources().getDimensionPixelSize(
                                R.dimen.lockscreen_button_digit_font_size) * 2.0f);
                mTextView.setWidth(Math.min(definedMaxTextSize, size));
                mTextView.setHeight(Math.min(definedMaxTextSize, size));
            } else {
                mTextView.setWidth(size);
                mTextView.setHeight(size);
            }
        }
    }

    private void adjustTextSize() {
        if (mTextView != null) {
            CharSequence text = mTextView.getText();
            float textSize = 0;
            if (TextUtils.isDigitsOnly(text)) {
                textSize = mTextView.getWidth() / 2;
            } else {
                textSize = Math.min(mTextView.getWidth() / 2, mContext.getResources().getDimension(R.dimen.lockscreen_button_text_font_size));
            }
            mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mTextView != null) {
            mTextView.setEnabled(enabled);
        }
        if (mImageView != null) {
            mImageView.setEnabled(enabled);
        }
    }
}
