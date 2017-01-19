package com.asus.filemanager.activity;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.TutorialViewPagerAdapter;
import com.asus.filemanager.utility.FileUtility;

public class LowStorageTutorialActivity extends BaseActivity implements OnClickListener{
	private Button okButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.lowstorage_googledrive_tutorial);

        okButton = (Button)findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        switch(view.getId()) {
            case R.id.button_ok:
            	setResult(Activity.RESULT_OK);
            	finish();
            	break;
        }
    }
}
