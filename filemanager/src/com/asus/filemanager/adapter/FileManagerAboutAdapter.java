package com.asus.filemanager.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.AnalyticsReflectionUtility;
import com.asus.filemanager.utility.ConstantsUtil;
import com.asus.filemanager.wrap.WrapEnvironment;

import java.util.ArrayList;
import java.util.HashMap;

public class FileManagerAboutAdapter extends BaseAdapter{
	
	private ArrayList<HashMap<String,String>> aboutList = new ArrayList<HashMap<String,String>>();
    private final LayoutInflater mInflater;
    private boolean inspireAsusExist = true;
    private Context mContext;
	
    public FileManagerAboutAdapter(Context aContext) {
		super();
		mContext = aContext;
        mInflater = LayoutInflater.from(mContext);
        
        inspireAsusExist = AnalyticsReflectionUtility.InspireAsusExist(mContext);

        HashMap<String,String> map;
        
        //0 tutorial
        map = new HashMap<String,String>();
        map.put("MenuTitle", aContext.getString(R.string.menu_tutorial));
        map.put("MenuSummary", "");
        map.put("MenuId", "0");
        aboutList.add(map);
        
        
        //1 open source license
        map = new HashMap<String,String>();
    	map.put("MenuTitle", aContext.getString(R.string.open_source_license_title));
    	map.put("MenuSummary", aContext.getString(R.string.open_source_license_description));
        map.put("MenuId", "1");
    	aboutList.add(map);
    	
    	//2 license user agreement
    	map = new HashMap<String,String>();
    	map.put("MenuTitle", aContext.getString(R.string.eula_title));
    	map.put("MenuSummary", aContext.getString(R.string.eula_description));
        map.put("MenuId", "2");
    	aboutList.add(map);
    	
    	//3 privacy policy
        map = new HashMap<String,String>();
    	map.put("MenuTitle", aContext.getString(R.string.privacy_policy));
    	map.put("MenuSummary", "");
        map.put("MenuId", "3");
        aboutList.add(map);
        
        //4 terms_of_service
        map = new HashMap<String,String>();
    	map.put("MenuTitle", aContext.getString(R.string.terms_of_service));
    	map.put("MenuSummary", "");
        map.put("MenuId", "4");
        aboutList.add(map);
    	
    	//5 feedback & help
        if(!WrapEnvironment.IS_VERIZON) {
            map = new HashMap<String, String>();
            map.put("MenuTitle", aContext.getString(R.string.asus_feedback_and_help));
            map.put("MenuSummary", "");
            map.put("MenuId", "5");
            aboutList.add(map);
        }
    	//6 inspire us
        if (!inspireAsusExist){
        	map = new HashMap<String,String>();
        	map.put("MenuTitle", aContext.getString(R.string.inspire_asus_title));
        	map.put("MenuSummary", aContext.getString(R.string.inspire_asus_description));
            map.put("MenuId", "6");
            aboutList.add(map);
        }

    	//7 apk version
        PackageInfo pkgInfo = null;
        try {
            pkgInfo = aContext.getPackageManager().getPackageInfo(aContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        String VersionText = "";
        if (pkgInfo != null && pkgInfo.versionName != null) {
            VersionText = String.valueOf(pkgInfo.versionName);
        }
        map = new HashMap<String,String>();
        map.put("MenuTitle", aContext.getApplicationContext().getString(R.string.version_text));
        map.put("MenuSummary", VersionText);
        map.put("MenuId", "7");
        aboutList.add(map);
    	
	}
	
	@Override
	public int getCount() {
		return aboutList.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return Long.parseLong(aboutList.get(position).get("MenuId"));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        View row = convertView;

        if (row == null) {
            row = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_about, null);
            holder = new ViewHolder(row);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }

        holder.tvTitle.setText(aboutList.get(position).get("MenuTitle"));
        holder.tvTitle.setVisibility(View.VISIBLE);
        holder.tvSingleLineTitle.setText(aboutList.get(position).get("MenuTitle"));
        holder.tvSingleLineTitle.setVisibility(View.GONE);
        holder.tvSummary.setText(aboutList.get(position).get("MenuSummary"));
        holder.tvSummary.setVisibility(View.VISIBLE);
        holder.cbInspireAsus.setVisibility(View.GONE);
        
        int menuId = Integer.parseInt(aboutList.get(position).get("MenuId"));
        switch (menuId){
        case 0:
        	holder.tvSingleLineTitle.setVisibility(View.VISIBLE);
        	holder.tvTitle.setVisibility(View.INVISIBLE);
        	holder.tvSummary.setVisibility(View.INVISIBLE);
        	break;
        case 1:
        	holder.tvSingleLineTitle.setVisibility(View.INVISIBLE);
        	holder.tvTitle.setVisibility(View.VISIBLE);
        	holder.tvSummary.setVisibility(View.VISIBLE);
        	break;
        case 2:
        	holder.tvSingleLineTitle.setVisibility(View.INVISIBLE);
        	holder.tvTitle.setVisibility(View.VISIBLE);
        	holder.tvSummary.setVisibility(View.VISIBLE);
        	break;
        case 3:
        	holder.tvSingleLineTitle.setVisibility(View.VISIBLE);
        	holder.tvTitle.setVisibility(View.INVISIBLE);
        	holder.tvSummary.setVisibility(View.INVISIBLE);
        	break;
        case 4:
        	holder.tvSingleLineTitle.setVisibility(View.VISIBLE);
        	holder.tvTitle.setVisibility(View.INVISIBLE);
        	holder.tvSummary.setVisibility(View.INVISIBLE);
        	break;
        case 5:
        	holder.tvSingleLineTitle.setVisibility(View.VISIBLE);
        	holder.tvTitle.setVisibility(View.INVISIBLE);
        	holder.tvSummary.setVisibility(View.INVISIBLE);
        	break;
        case 6:
            // inspire us
            if (!inspireAsusExist){
            	holder.tvSingleLineTitle.setVisibility(View.GONE);
            	holder.tvTitle.setVisibility(View.VISIBLE);
            	holder.tvSummary.setVisibility(View.VISIBLE);
            	holder.cbInspireAsus.setVisibility(View.VISIBLE);

                SharedPreferences settings2 = mContext.getSharedPreferences("MyPrefsFile", 0);
                Boolean bInspireUS = settings2.getBoolean(ConstantsUtil.PREV_INSPIREUS, true);
                if (bInspireUS){
                	holder.cbInspireAsus.setChecked(true);
                }else{
                	holder.cbInspireAsus.setChecked(false);
                }
            }else{
            	holder.tvSingleLineTitle.setVisibility(View.INVISIBLE);
            	holder.tvTitle.setVisibility(View.VISIBLE);
            	holder.tvSummary.setVisibility(View.VISIBLE);
            }
        	break;
        case 7:
        	holder.tvSingleLineTitle.setVisibility(View.INVISIBLE);
        	holder.tvTitle.setVisibility(View.VISIBLE);
        	holder.tvSummary.setVisibility(View.VISIBLE);
        	break;
        }
        return row;
	}
	
	class ViewHolder {
        TextView tvTitle;
        TextView tvSingleLineTitle;
        TextView tvSummary;
        CheckBox cbInspireAsus;

        public ViewHolder(View view) {
            this.tvTitle = (TextView) view.findViewById(R.id.title);
            this.tvSingleLineTitle = (TextView) view.findViewById(R.id.singlelineTitle);
            this.tvSummary = (TextView) view.findViewById(R.id.about_summary);
            this.cbInspireAsus = (CheckBox) view.findViewById(R.id.about_inspireus_checkbox);

        }
    }

}
