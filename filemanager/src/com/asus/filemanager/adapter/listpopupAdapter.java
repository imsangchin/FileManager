package com.asus.filemanager.adapter;

import java.util.ArrayList;
import java.util.List;
import com.asus.filemanager.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class listpopupAdapter extends ArrayAdapter<String> {
	ArrayList<Integer> mNewFeature;

	public listpopupAdapter(Context context, int resource,
			int textViewResourceId, List<String> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public listpopupAdapter(Context context, int resource,
			int textViewResourceId, String[] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public listpopupAdapter(Context context, int resource,
			int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public listpopupAdapter(Context context, int resource, List<String> objects) {
		super(context, resource, objects);
	}

	public listpopupAdapter(Context context, int resource, String[] objects) {
		super(context, resource, objects);
	}

	public listpopupAdapter(Context context, int resource) {
		super(context, resource);
	}

	public void setIsNewFeature(ArrayList<Integer> newFeature){
		mNewFeature = newFeature;
	}
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String text = getItem(position);    
        boolean bIsNew = false;
        for (int i=0;i<mNewFeature.size();i++){
        	if (mNewFeature.get(i).intValue() == position){
        		bIsNew = true;
        		break;
        	}
        }
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
        	convertView = LayoutInflater.from(getContext()).inflate(R.layout.popup_menu_item_layout, parent, false);
        }
        // Lookup view for data population
        TextView tvText= (TextView) convertView.findViewById(R.id.title);
        tvText.setText(text);
        
        ImageView ImgNew = (ImageView) convertView.findViewById(R.id.icon);
        if (bIsNew){
        	ImgNew.setVisibility(View.VISIBLE);
        }else{
        	ImgNew.setVisibility(View.GONE);
        }
        return convertView;
   }
}
