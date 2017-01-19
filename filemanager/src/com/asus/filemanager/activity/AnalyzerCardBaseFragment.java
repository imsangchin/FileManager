package com.asus.filemanager.activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.ga.GaStorageAnalyzer;


/**
 * Created by ChenHsin_Hsieh on 2016/2/4.
 */
public abstract class AnalyzerCardBaseFragment extends Fragment implements View.OnClickListener,StorageAnalyzerActivity.OnStorageChangedListener{

    public static final String TAG_BASE = "AnalyzerCardBaseFrag";

    private RelativeLayout baseLayout;
    private TextView titleView,sizesView, contentView;
    private ImageView itemImageView;
    private Intent clickIntent;
    private String gaAction;
    private boolean closeSelf = false;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_analyzer_cardbase, container, false);
        findViews(rootView);
        setListeners();
        return rootView;
    }

    private void findViews(View rootView) {
        baseLayout = (RelativeLayout) rootView.findViewById(R.id.fragment_analyzer_cardbase_baselayout);
        titleView = (TextView) rootView.findViewById(R.id.fragment_analyzer_cardbase_title);
        sizesView = (TextView) rootView.findViewById(R.id.fragment_analyzer_cardbase_sizes);
        contentView = (TextView) rootView.findViewById(R.id.fragment_analyzer_cardbase_content);
        itemImageView = (ImageView) rootView.findViewById(R.id.fragment_analyzer_cardbase_item);
    }

    private void setListeners() {
        baseLayout.setOnClickListener(this);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
    }

    public void setTitle(String title) {
        titleView.setText(title);
    }

    public void setSizes(String sizes) {
        sizesView.setText(sizes);
    }

    public void setContent(String content) {
        contentView.setText(content);
    }

    public void setItemImage(int resId){
        itemImageView.setImageResource(resId);
    }

    public void setClickIntent(Intent intent) {
        clickIntent = intent;
    }

    public void setGaAction(String gaAction)
    {
        this.gaAction = gaAction;
    }

    public void setCloseSelf(boolean close)
    {
        closeSelf = close;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

        if (clickIntent != null) {
            getActivity().startActivityForResult(clickIntent, StorageAnalyzerActivity.KEY_NOTIFY_CHANGED);
            Log.i(TAG_BASE, "AnalyzerCardBaseFragment onClick");

            if (closeSelf){
                Log.i(TAG_BASE, "AnalyzerCardBaseFragment finish");
                getActivity().finish();
            }
            if (gaAction != null) {
                GaStorageAnalyzer.getInstance().sendEvents(getActivity(), GaStorageAnalyzer.CATEGORY_NAME, gaAction, null, null);
                Log.i(TAG_BASE, "AnalyzerCardBaseFragment sendGa" + gaAction);
            }
        }
    }

}
