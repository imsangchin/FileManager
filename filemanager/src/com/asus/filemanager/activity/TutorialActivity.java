package com.asus.filemanager.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.TutorialViewPagerAdapter;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.PrefUtils;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.util.ArrayList;
import java.util.List;


public class TutorialActivity extends BaseActivity implements OnClickListener{

    private static final String TAG = "TutorialActivity";

    private static final int PROGRESS_STATE_UNCURRENT = 0;
    private static final int PROGRESS_STATE_CURRENT = 1;

    public static final String TUTORIAL_SD_PERMISSION = "TUTORIAL_SD_PERMISSION";
    public static final String TUTORIAL_DISKLABEL = "TUTORIAL_DISKLABEL";

    private ViewPager mViewPager;
    private TutorialViewPagerAdapter mAdapter;
    private Bitmap[] mProgressStates;
    private Button mPrevButton;
    private Button mNextButton;
    // private TextView mTutorialTitle;
    // private TextView mTutorialContent;
    private List<View> mViewLists = new ArrayList<>();
    private List<SpannableString> mTitleLists = new ArrayList<>();
    private List<SpannableString> mContentLists = new ArrayList<>();
    private ImageView[] mProgressImages;
    private CheckBox mDontShowCheckBox;
    private boolean mBSDPermissionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tutorial_activity);

        mPrevButton = (Button) findViewById(R.id.previous);
        mPrevButton.setOnClickListener(this);
        mNextButton = (Button) findViewById(R.id.next);
        mNextButton.setOnClickListener(this);

        mProgressImages = new ImageView[4];
        mProgressImages[0] = (ImageView)findViewById(R.id.progress_image1);
        mProgressImages[1] = (ImageView)findViewById(R.id.progress_image2);
        mProgressImages[2] = (ImageView)findViewById(R.id.progress_image3);
        mProgressImages[3] = (ImageView)findViewById(R.id.progress_image4);

        mDontShowCheckBox = (CheckBox)findViewById(R.id.tutorial_activity_dont_show_checkbox);

        // mTutorialTitle = (TextView)findViewById(R.id.textView_title);
        // mTutorialContent = (TextView)findViewById(R.id.textView_content);

        handleIntent(getIntent());

        initProgressStates();
        setCurrentPage(0);

        mAdapter = new TutorialViewPagerAdapter(mViewLists, mTitleLists, mContentLists);
        mViewPager = (ViewPager) findViewById(R.id.tutorial_viewpager);
        mViewPager.setAdapter(mAdapter);
        /*
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                setCurrentPage(position);
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        */

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position)
            {
                setCurrentPage(position);
            }
        });

        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            // set windowTranslucentStatus to false to prevent status bar overlay main layout
            clearWindowTranslucentStatus();
        }

    }

    // Enter split-screen DIRECTLY seems not trigger this method.
    // @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode)
    {
        super.onMultiWindowModeChanged(isInMultiWindowMode);

        Log.i(TAG, "onMultiWindowModeChanged, isInMultiWindowMode: " + isInMultiWindowMode);

        // Exit tutorial page when enter multi window mode based on spec.
        if(isInMultiWindowMode)
        {
            if(PrefUtils.getBooleanFirstEnterMultiWindowMode(this))
            {
                PrefUtils.setBooleanFirstEnterMultiWindowMode(this, false);
                // exit();
            }
        }
    }

    @SuppressLint("InflateParams")
    private void handleIntent(Intent intent) {
        Boolean SD_PERMISSION = intent.getBooleanExtra(TutorialActivity.TUTORIAL_SD_PERMISSION, false);
        String DISKLABEL  = intent.getStringExtra(TutorialActivity.TUTORIAL_DISKLABEL);

        if (SD_PERMISSION) {
            mBSDPermissionMode = true;
            if (!TextUtils.isEmpty(DISKLABEL)){
                mViewLists.add(getLayoutInflater().inflate(R.layout.sd_tutorial_viewpager_1, null));
                mViewLists.add(getLayoutInflater().inflate(R.layout.sd_tutorial_viewpager_2, null));
                mViewLists.add(getLayoutInflater().inflate(R.layout.sd_tutorial_viewpager_3, null));
                mTitleLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_title_v2, DISKLABEL)));
                mTitleLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_title_v2, DISKLABEL)));
                mTitleLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_title_v2, DISKLABEL)));
                mContentLists.add(new SpannableString(getResources().getString(R.string.saf_permission_tutorial_content_v2, DISKLABEL)));
                mContentLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_step_two_v2, DISKLABEL)));
                mContentLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_step_three_v2, DISKLABEL)));
            }else{
                mViewLists.add(getLayoutInflater().inflate(R.layout.sd_tutorial_viewpager_1, null));
                mViewLists.add(getLayoutInflater().inflate(R.layout.sd_tutorial_viewpager_2, null));
                mViewLists.add(getLayoutInflater().inflate(R.layout.sd_tutorial_viewpager_3, null));
                mTitleLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_title)));
                mTitleLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_title)));
                mTitleLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_title)));
                mContentLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_step_one)));
                mContentLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_step_two)));
                mContentLists.add(new SpannableString(getResources().getString(R.string.saf_tutorial_step_three)));
            }
            mProgressImages[0].setVisibility(View.VISIBLE);
            mProgressImages[1].setVisibility(View.VISIBLE);
            mProgressImages[2].setVisibility(View.VISIBLE);
            mProgressImages[3].setVisibility(View.GONE);
        }else {
            mBSDPermissionMode = false;

            mViewLists.add(getLayoutInflater().inflate(R.layout.tutorial_viewpager_1, null));
            mViewLists.add(getLayoutInflater().inflate(R.layout.tutorial_viewpager_2, null));
            mViewLists.add(getLayoutInflater().inflate(R.layout.tutorial_viewpager_3, null));
            mTitleLists.add(new SpannableString(getResources().getString(R.string.tutorial_category_title)));
            mTitleLists.add(new SpannableString(getResources().getString(R.string.tutorial_cloud_title)));
            mTitleLists.add(new SpannableString(getResources().getString(R.string.tools_storage_analyzer)));

            mContentLists.add(new SpannableString(getResources().getString(R.string.tutorial_category_content)));
            String text = getResources().getString(R.string.tutorial_cloud_content);
            SpannableString spannableString = new SpannableString(text);

            /*
            // Drawable drawable = getResources().getDrawable(R.drawable.asussl_ic_menu_moreoverflow);
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.asussl_ic_menu_moreoverflow);
            int index = text.indexOf("+");
            if (drawable != null && index >= 0) {
                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
                spannableString.setSpan(imageSpan, index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                float textSize = mTutorialContent.getTextSize();
                // the image is just bigger than text for 1.5x.
                drawable.setBounds(0, 0, (int) (textSize * 1.0f) , (int) (textSize * 1.0f));
            }
            */

            mContentLists.add(spannableString);
            mContentLists.add(new SpannableString(getResources().getString(R.string.tutorial_analyzer_content)));
            mProgressImages[0].setVisibility(View.VISIBLE);
            mProgressImages[1].setVisibility(View.VISIBLE);
            mProgressImages[2].setVisibility(View.VISIBLE);
            mProgressImages[3].setVisibility(View.GONE);

            if(WrapEnvironment.IS_VERIZON)
                mDontShowCheckBox.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("deprecation")
    private void initProgressStates() {
        mProgressStates = new Bitmap[2];

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;

        // The following two are ignore in api level 21.
        opt.inPurgeable = true;
        opt.inInputShareable = true;

        try{
            mProgressStates[PROGRESS_STATE_UNCURRENT] = BitmapFactory.decodeResource(getResources(),R.drawable.asus_tutorial_indicator_off,opt);
            mProgressStates[PROGRESS_STATE_CURRENT] = BitmapFactory.decodeResource(getResources(),R.drawable.asus_tutorial_indicator_on,opt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCurrentPage(int position) {
        if (mProgressImages == null)
            return;

        for(int i = 0; i < mProgressImages.length; i++) {
            mProgressImages[i].setImageBitmap((position == i) ? mProgressStates[PROGRESS_STATE_CURRENT] : mProgressStates[PROGRESS_STATE_UNCURRENT]);
        }

        if (position == mViewLists.size() - 1) {
            mNextButton.setText(getString(R.string.tutorial_done));
            mNextButton.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.tutorial_button_done, 0);
        } else {
            mNextButton.setText(getString(R.string.tutorial_skip));
            mNextButton.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.tutorial_button_right, 0);
        }

        mPrevButton.setVisibility(View.INVISIBLE);
    }

    private void exit() {
        if (mBSDPermissionMode){
            FileUtility.saveFirstSDPermission(this);
        }else{
            if(WrapEnvironment.IS_VERIZON) {
                FileUtility.saveVerizonTipsStartup(this,!mDontShowCheckBox.isChecked());
            }
            FileUtility.saveFirstStartup(this);
        }
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void clearWindowTranslucentStatus() {
        final WindowManager.LayoutParams attrs = getWindow()
                .getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setAttributes(attrs);
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }


    @Override
    public void onClick(View view) {

        switch(view.getId()) {
            case R.id.previous:
                if (mViewPager.getCurrentItem() == 0) {
                    if (!mBSDPermissionMode){
                        exit();
                    }
                } else {
                    int leftPos = mViewPager.getCurrentItem() - 1;
                    mViewPager.setCurrentItem(leftPos);
                }
                break;
            case R.id.next:
                exit();
                break;
        }
    }
}
