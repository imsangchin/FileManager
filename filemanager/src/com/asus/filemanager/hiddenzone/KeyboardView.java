package com.asus.filemanager.hiddenzone;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;


import java.util.ArrayList;
import java.util.List;

import com.asus.filemanager.R;

public class KeyboardView extends RelativeLayout implements View.OnClickListener {

    private Context mContext;
    private KeyboardButtonClickedListener mKeyboardButtonClickedListener;

    private List<KeyboardButtonView> mButtons;

    public KeyboardView(Context context) {
        this(context, null);
    }

    public KeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mContext = context;
        initializeView(attrs, defStyleAttr);
    }

    private void initializeView(AttributeSet attrs, int defStyleAttr) {
        if (attrs != null && !isInEditMode()) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            KeyboardView view = (KeyboardView) inflater.inflate(R.layout.hidden_zone_view_keyboard, this);

            initKeyboardButtons(view);
        }
    }

    /**
     * Init the keyboard buttons (onClickListener)
     */
    private void initKeyboardButtons(KeyboardView view) {
        mButtons = new ArrayList<>();
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_0));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_1));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_2));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_3));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_4));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_5));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_6));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_7));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_8));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_9));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_cancel));
        mButtons.add((KeyboardButtonView) view.findViewById(R.id.pin_code_button_next));

        for(View button : mButtons) {
            button.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if(mKeyboardButtonClickedListener == null) {
            return;
        }

        int id = v.getId();
        if(id == R.id.pin_code_button_0) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_0);
        } else if(id == R.id.pin_code_button_1) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_1);
        } else if(id == R.id.pin_code_button_2) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_2);
        } else if(id == R.id.pin_code_button_3) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_3);
        } else if(id == R.id.pin_code_button_4) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_4);
        } else if(id == R.id.pin_code_button_5) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_5);
        } else if(id == R.id.pin_code_button_6) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_6);
        } else if(id == R.id.pin_code_button_7) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_7);
        } else if(id == R.id.pin_code_button_8) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_8);
        } else if(id == R.id.pin_code_button_9) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_9);
        } else if(id == R.id.pin_code_button_cancel) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_CANCEL);
        } else if (id == R.id.pin_code_button_next) {
            mKeyboardButtonClickedListener.onKeyboardClick(KeyboardButtonEnum.BUTTON_OK);
        }
    }

    public void setKeyboardButtonClickedListener(KeyboardButtonClickedListener keyboardButtonClickedListener) {
        this.mKeyboardButtonClickedListener = keyboardButtonClickedListener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for(KeyboardButtonView button : mButtons) {
            button.setEnabled(enabled);
        }
    }
}
