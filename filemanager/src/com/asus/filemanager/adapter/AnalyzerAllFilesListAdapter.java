package com.asus.filemanager.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnalyzerAllFilesListAdapter  extends BaseAdapter {

    private Context context;
    private List<VFile> files;
    private boolean isLongClickMode = false;
    private ItemIcon itemIcon;
    private HashMap<Integer,VFile> mLongClickedMap;
    private DecimalFormat percentageFormat;
    private OnCheckBoxChangedListener onCheckBoxChangedListener;

    public interface OnCheckBoxChangedListener{
        void onCheckBoxChanged(int position,boolean isChecked);
    }

    public void setOnCheckBoxChangedListener(OnCheckBoxChangedListener onCheckBoxChangedListener)
    {
        this.onCheckBoxChangedListener = onCheckBoxChangedListener;
    }

    public AnalyzerAllFilesListAdapter(Context context,List<VFile> files)
    {
        this.context = context;
        this.files = files;
        mLongClickedMap = new HashMap<>();
        itemIcon = new ItemIcon(context, false);
        percentageFormat =new DecimalFormat("0.0");


    }

    public void release()
    {
        if(mLongClickedMap !=null)
            mLongClickedMap.clear();
        if(itemIcon!=null)
            itemIcon.clearCache();
    }

    public boolean isLongClickMode() {
        return isLongClickMode;
    }

    public synchronized void setLongClickMode(boolean isLongClickMode) {
        this.isLongClickMode = isLongClickMode;
    }

    public synchronized void setFiles(List<VFile> files) {
        this.files = new ArrayList<>(files);
        notifyDataSetChanged();
    }

    /**
     * Let ViewHolder's OnCheckedChangeListener to occur notifyDataSetChanged,
     * because Android 4.x can't call notifyDataSetChanged twice, it will occur
     * ActionMode or Toolbar's view gone. ex:[TT-844868]
     * @param view listItem's view
     * @param position
     */
    public synchronized void setOnItemClick(View view,int position){
        VFile clickVFile = files.get(position);
        ViewHolder viewHolder = ((ViewHolder)view.getTag());
        viewHolder.update(position);
        viewHolder.check.setChecked(!clickVFile.getChecked());
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return files.size();
    }

    @Override
    public VFile getItem(int position) {
        // TODO Auto-generated method stub
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewHolder holder;
        View view = convertView;


        if(view==null){
            view = LayoutInflater.from(context).inflate(R.layout.analyser_allfiles_list_item, null);

            holder = new ViewHolder();
            holder.container = (RelativeLayout) view.findViewById(R.id.analyser_allfiles_list_item_root);
            holder.check = (CheckBox) view.findViewById(R.id.analyser_allfiles_list_item_checkbox);
            holder.name = (TextView) view.findViewById(R.id.analyser_allfiles_list_item_name);
            holder.icon = (ImageView) view.findViewById(R.id.analyser_allfiles_list_item_icon);
            holder.progressBar = (ProgressBar) view.findViewById(R.id.analyser_allfiles_list_item_progressbar);
            holder.percentage = (TextView) view.findViewById(R.id.analyser_allfiles_list_item_percentage);
            holder.sizes = (TextView) view.findViewById(R.id.analyser_allfiles_list_item_sizes);
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
        holder.progressBar.setProgress((int) vFile.getInStoragePercentage());
        String percentageText = percentageFormat.format(vFile.getInStoragePercentage()) + "%";
        holder.percentage.setText(percentageText);
        holder.sizes.setText(FileUtility.bytes2String(context, vFile.length(), 1));
        if(vFile.getChecked())
        {
            holder.container.setBackgroundColor(ContextCompat.getColor(context, ThemeUtility.getItemSelectedBackgroundColor()));
        }
        else
        {
            holder.container.setBackgroundColor(ContextCompat.getColor(context,android.R.color.transparent));
        }

        return view;
    }

    public void removeDeletedFiles(VFile[] deleteFiles)
    {

        for(VFile vFile:deleteFiles)
        {
            if(mLongClickedMap.containsValue(vFile))
                files.remove(vFile);
        }
        mLongClickedMap.clear();
        isLongClickMode = false;
        notifyDataSetChanged();
    }

    public synchronized void resetAllLongClicked()
    {
        for(VFile vFile: mLongClickedMap.values())
        {
            vFile.setChecked(false);
        }
        mLongClickedMap.clear();
        isLongClickMode = false;
        notifyDataSetChanged();
    }

    public synchronized void setAllLongClicked()
    {
        for(int i=0;i<files.size();i++)
        {
            files.get(i).setChecked(true);
            mLongClickedMap.put(i,files.get(i));
        }
        notifyDataSetChanged();
    }


    public HashMap<Integer,VFile> getLongClickedMap()
    {
        return mLongClickedMap;
    }


    private class ViewHolder {
        RelativeLayout container;
        CheckBox check;
        ImageView icon;
        TextView name;
        TextView sizes;
        TextView percentage;
        ProgressBar progressBar;
        int position;

        public CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    files.get(position).setChecked(isChecked);
                    if (isChecked) {
                        if(!mLongClickedMap.containsKey(position)) {
                            mLongClickedMap.put(position, files.get(position));
                            if(onCheckBoxChangedListener!=null )
                                onCheckBoxChangedListener.onCheckBoxChanged(position, true);
                            notifyDataSetChanged();
                        }
                    } else {
                        if(mLongClickedMap.containsKey(position)) {
                            mLongClickedMap.remove(position);
                            if (mLongClickedMap.size() == 0) {
                                isLongClickMode = false;
                            }
                            if(onCheckBoxChangedListener!=null )
                                onCheckBoxChangedListener.onCheckBoxChanged(position, false);
                            notifyDataSetChanged();
                        }
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
