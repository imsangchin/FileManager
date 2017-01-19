package com.asus.filemanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;

import java.util.HashMap;
import java.util.List;

public class AnalyzerCardListAdapter extends BaseAdapter{

	private Context context;
	private List<LocalVFile> files;
	private boolean isLongClickMode = false;
	private ItemIcon itemIcon;
	private HashMap<Integer,LocalVFile> longClickedMap;

	public AnalyzerCardListAdapter(Context context, List<LocalVFile> files)
	{
		this.context = context;
		this.files = files;
		longClickedMap = new HashMap<Integer, LocalVFile>();
		itemIcon = new ItemIcon(context, false);

	}

	public void release()
	{
		if(longClickedMap!=null)
			longClickedMap.clear();
		if(itemIcon!=null)
			itemIcon.clearCache();
	}

	public boolean isLongClickMode() {
		return isLongClickMode;
	}

	public void setLongClickMode(boolean isLongClickMode) {
		this.isLongClickMode = isLongClickMode;
	}

	public void setFiles(List<LocalVFile> files) {
		this.files = files;
	}

	public void toggleChecked(int position)
	{
		VFile clickVFile = files.get(position);
		clickVFile.setChecked(!clickVFile.getChecked());
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return files.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return files.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder holder;
		View view = convertView;

		if(view==null){
			view = LayoutInflater.from(context).inflate(R.layout.analyzer_card_list_item, null);
//
			holder = new ViewHolder();
			holder.container = (RelativeLayout) view.findViewById(R.id.analyzer_card_list_item_root);
			holder.check = (CheckBox) view.findViewById(R.id.analyzer_card_list_item_checkbox);
			holder.name = (TextView) view.findViewById(R.id.analyzer_card_list_item_name);
			holder.icon = (ImageView) view.findViewById(R.id.analyzer_card_list_item_icon);
			holder.sizes = (TextView) view.findViewById(R.id.analyzer_card_list_item_sizes);
			holder.info = (TextView) view.findViewById(R.id.analyzer_card_list_item_info);
			view.setTag(holder);
		}
		else{

			holder = (ViewHolder) view.getTag();

		}

		holder.update(position);

		VFile vFile = files.get(position);
		if(isLongClickMode)
		{
			holder.check.setVisibility(View.VISIBLE);
		}
		else
		{
			holder.check.setVisibility(View.GONE);
		}

		holder.icon.setTag(vFile.getAbsolutePath());
		itemIcon.setIcon(vFile, holder.icon, true);
		holder.check.setChecked(vFile.getChecked());
		holder.name.setText(vFile.getName());
		holder.info.setText(vFile.getAbsolutePath());
		holder.sizes.setText(FileUtility.bytes2String(context, vFile.length(), 1));

		if(vFile.getChecked())
		{
			holder.container.setBackgroundColor(ThemeUtility.getItemSelectedBackgroundColor());
		}
		else
		{
			holder.container.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
		}

		return view;
	}

	public void resetAllLongClicked()
	{
		for(VFile vFile:longClickedMap.values())
		{
			vFile.setChecked(false);
		}
		longClickedMap.clear();
		isLongClickMode = false;
		notifyDataSetChanged();
	}

	public HashMap<Integer,LocalVFile> getLongClickedMap()
	{
		return longClickedMap;
	}


	private class ViewHolder {
		RelativeLayout container;
        CheckBox check;
        ImageView icon;
        TextView name;
        TextView info;
		TextView sizes;
        int position;

        public OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked)
				{
					longClickedMap.put(position, files.get(position));
				}
				else
				{
					longClickedMap.remove(position);
				}

				if(files.get(position).getChecked() ^ isChecked)
				{
					files.get(position).setChecked(isChecked);
					notifyDataSetChanged();
				}

			}
		};

        public void update(int position)
        {
        	this.position = position;
        	if(check!=null)
        		check.setOnCheckedChangeListener(onCheckedChangeListener);
        }

    }

	
}
