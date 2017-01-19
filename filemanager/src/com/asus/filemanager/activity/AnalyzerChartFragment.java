package com.asus.filemanager.activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.StorageListAdapger;
import com.asus.filemanager.ga.GaStorageAnalyzer;
import com.asus.filemanager.ui.CircleChartView;
import com.asus.filemanager.utility.CalcCategorySizesTask;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.VolumeInfoUtility;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by ChenHsin_Hsieh on 2016/1/28.
 */
public class AnalyzerChartFragment extends Fragment implements CalcCategorySizesTask.OnCategorySizesResultListener, StorageAnalyzerActivity.OnStorageChangedListener{

    public static final String TAG = "AnalyzerChartFragment";

    private CircleChartView circleChartView;
    private ArrayList<Long> categorySizesArray;

    private TextView images,music,videos,apps,documents,others,total,free,title;
    private RelativeLayout baselayout;

    private final int[] ANALYSIS_CATEGORY_ITEMS = {CategoryItem.IMAGE,CategoryItem.MUSIC,CategoryItem.VIDEO,CategoryItem.APP,CategoryItem.DOCUMENT};
    private long totalSpace = 0;
    private long usedSpace = 0;
    private String rootPath;

    private boolean isCreated = false;
    private StorageListAdapger.StorageItemElement tmpStorageItemElement;

    private CalcCategorySizesTask[] calcCategorySizesTasks;
    private String gaAction = GaStorageAnalyzer.ACTION_ALL_FILES_EXTERNAL_PAGE;

    private Handler loadingHandler;
    private int loading = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "AnalyzerChartFragment onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_analyzer_chart, container, false);
        findViews(rootView);
        if(!isCreated && tmpStorageItemElement!=null)
        {
            //storage change but not create, start analysis
            initial(tmpStorageItemElement);
            startAnalysis();
            setClickListener();
            isCreated = true;
        }
        startLoading();
        return rootView;
    }

    public void onDetach()
    {
        super.onDetach();
        if (calcCategorySizesTasks != null)
            for (int i = 0; i < calcCategorySizesTasks.length; i++) {
                calcCategorySizesTasks[i].cancel(true);
            }
        stopLoading();
    }

    public void setGaAction(String gaAction)
    {
        this.gaAction = gaAction;
    }

    private void findViews(View rootView)
    {
        circleChartView = (CircleChartView)rootView.findViewById(R.id.fragment_analyzer_chart_chartview);
        images = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_images);
        music = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_music);
        videos = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_videos);
        apps = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_apps);
        documents = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_documents);
        others = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_others);
        total = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_total);
        free = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_free);
        title = (TextView) rootView.findViewById(R.id.fragment_analyzer_chart_title);
        baselayout = (RelativeLayout)rootView.findViewById(R.id.fragment_analyzer_chart_baselayout);
    }

    private void setClickListener()
    {
        baselayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(),AnalyzerAllFilesActivity.class);
                intent.putExtra(AnalyzerAllFilesActivity.TITLE_KEY,title.getText().toString());
                intent.putExtra(AnalyzerAllFilesActivity.TOTAL_STORAGE_KEY,totalSpace);
                intent.putExtra(AnalyzerAllFilesActivity.ROOT_PATH_KEY,rootPath);
                getActivity().startActivityForResult(intent, StorageAnalyzerActivity.KEY_NOTIFY_CHANGED);

                GaStorageAnalyzer.getInstance().sendEvents(getActivity(), GaStorageAnalyzer.CATEGORY_NAME,
                        gaAction, null, null);
                Log.i(TAG, "AnalyzerChartFragment sendGa");
            }
        });
    }

    private void startLoading()
    {
        loadingHandler = new Handler();
        loading = 0;
        loadingHandler.post(loadingRunnable);

    }

    private void stopLoading()
    {
        if(loadingHandler!=null)
            loadingHandler.removeCallbacks(loadingRunnable);

        loadingHandler = null;
    }

    private Runnable loadingRunnable = new Runnable() {
        @Override
        public void run() {
            loading++;
            circleChartView.setPercentage(loading);
            if(loading<99)
                loadingHandler.postDelayed(loadingRunnable,100);
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
    }



    @Override
    public void onStorageChanged(LinkedList<StorageListAdapger.StorageItemElement> localStorageItemElements)
    {

    }

    @Override
    public void onStorageChanged(StorageListAdapger.StorageItemElement localStorageItemElement)
    {
        Log.i(TAG, "AnalyzerChartFragment onStorageChanged");
        if(localStorageItemElement==null)
            return;

        if(!isCreated) {
            tmpStorageItemElement = localStorageItemElement;
            return;
        }
        initial(localStorageItemElement);
        startAnalysis();
    }

    private void initial(StorageListAdapger.StorageItemElement localStorageItemElement){
        Log.i(TAG, "AnalyzerChartFragment initial");
        if(getActivity()==null || isDetached())
            return;

        categorySizesArray = new ArrayList<>();
        circleChartView.clearChartDatas();
        circleChartView.setTextHintBottom(getString(R.string.loading));
//        circleChartView.setTextHintBottom(getString(R.string.fragment_analyzer_chart_used_hint));

        title.setText((VolumeInfoUtility.getInstance(getActivity()).findStorageTitleByStorageVolume(localStorageItemElement.storageVolume)));
        //get used sizes
        VFile vfile = localStorageItemElement.vFile;
        rootPath = FileUtility.getCanonicalPathNoException(vfile);
        totalSpace=vfile.getTotalSpace();
        usedSpace=(vfile.getTotalSpace() - vfile.getUsableSpace());

        total.setText(FileUtility.bytes2String(getActivity(), totalSpace, 1));
        free.setText(FileUtility.bytes2String(getActivity(), totalSpace - usedSpace, 1));
    }

    private void startAnalysis() {
        Log.i(TAG, "AnalyzerChartFragment startAnalysis");
        if (calcCategorySizesTasks != null)
            for (int i = 0; i < calcCategorySizesTasks.length; i++) {
                calcCategorySizesTasks[i].cancel(true);
            }

        calcCategorySizesTasks = new CalcCategorySizesTask[ANALYSIS_CATEGORY_ITEMS.length];

        for (int i = 0; i < ANALYSIS_CATEGORY_ITEMS.length; i++) {
            calcCategorySizesTasks[i] = new CalcCategorySizesTask(getActivity(), rootPath, ANALYSIS_CATEGORY_ITEMS[i], totalSpace, this);
            calcCategorySizesTasks[i].executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    @Override
    public void onCategorySizesResult(int categoryItemId, long sizes, float percentage)
    {
        if(getActivity()==null || isDetached())
            return;
        Log.i(TAG, "AnalyzerChartFragment onCategorySizesResult:"+categoryItemId);
        categorySizesArray.add(sizes);


        String textSizes = FileUtility.bytes2String(getActivity(), sizes, 1);
        //add category percentage to chart
        boolean addChart = (sizes!=0 && percentage>0.0001f)?true:false;


        int color = 0;
        switch (categoryItemId) {
                case CategoryItem.IMAGE:
                    images.setText(textSizes);
                    color = getResources().getColor(R.color.fragment_analyzer_chart_image_color);
                    break;
                case CategoryItem.VIDEO:
                    videos.setText(textSizes);
                    color = getResources().getColor(R.color.fragment_analyzer_chart_video_color);
                    break;
                case CategoryItem.MUSIC:
                    music.setText(textSizes);
                    color = getResources().getColor(R.color.fragment_analyzer_chart_music_color);
                    break;
                case CategoryItem.APP:
                    apps.setText(textSizes);
                    color = getResources().getColor(R.color.fragment_analyzer_chart_app_color);
                    break;
                case CategoryItem.DOCUMENT:
                    documents.setText(textSizes);
                    color = getResources().getColor(R.color.fragment_analyzer_chart_document_color);
                    break;
        }
        if(addChart)
            circleChartView.addChartData(percentage, color);


        //add category other and start animation
        if(categorySizesArray.size()==ANALYSIS_CATEGORY_ITEMS.length)
        {
            Log.i(TAG, "AnalyzerChartFragment onCategorySizesResult add others");
            float totalUsedPercentage =(usedSpace*100.0f)/totalSpace;
            circleChartView.setPercentageFormat("0");

            //cal other used space
            for(long categorySizes : categorySizesArray)
                usedSpace -= categorySizes;
            categorySizesArray.add(usedSpace);
            float otherPercentage = usedSpace/(float)totalSpace;
            others.setText(FileUtility.bytes2String(getActivity(), usedSpace, 1));

            stopLoading();

            circleChartView.addChartData(otherPercentage, getResources().getColor(R.color.fragment_analyzer_chart_other_color));
            circleChartView.sortDatas();
            circleChartView.setPercentage(totalUsedPercentage);
            circleChartView.setTextHintBottom(getString(R.string.fragment_analyzer_chart_used_hint));
//            circleChartView.recordDefault();
            circleChartView.startAnimation(500, 1000);
        }
    }
}
