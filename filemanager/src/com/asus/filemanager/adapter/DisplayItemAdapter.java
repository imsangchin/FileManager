package com.asus.filemanager.adapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.DateUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ChenHsin_Hsieh on 2016/2/19.
 */
public abstract class DisplayItemAdapter<T extends DisplayItemAdapter.DisplayItem> extends BaseAdapter {

    protected Context context;
    private List< T> files;
    private ItemIcon itemIcon;
    private HashMap<Integer, T> selectedItemMap;
    private OnSelectedItemsChangedListener onSelectedItemsChangedListener;
    private Comparator<? super T> sortComparator;

    private Comparator<T> defaultSortComparator = new Comparator<T>() {
        public int compare(DisplayItem file1, DisplayItem file2) {
            int result = ((Long) (file2.getDisplayTime())).compareTo(file1.getDisplayTime());
            if (result != 0)
                return result;
            else
                return file1.getName().compareTo(file2.getName());
        }
    };

    public interface DisplayItem {
        void setChecked(boolean checked);
        boolean isChecked();
        VFile getOriginalFile();

        String getName();

        boolean isInExternalStorage();

        VFile getCurrentVFile();
        long getDisplayTime();
    }
    public interface OnSelectedItemsChangedListener {
        void onSelectedItemsChanged();
    }

    public void setOnSelectedItemsChangedListener(OnSelectedItemsChangedListener onSelectedItemsChangedListener)
    {
        this.onSelectedItemsChangedListener = onSelectedItemsChangedListener;
    }

    public DisplayItemAdapter(Context context, List<T> files)
    {
        this.context = context;
        this.sortComparator = defaultSortComparator;
        selectedItemMap = new HashMap<>();
        itemIcon = new ItemIcon(context, false);
        setFiles(files);
    }

    public void release()
    {
        if(selectedItemMap !=null)
            selectedItemMap.clear();
        if(itemIcon!=null)
            itemIcon.clearCache();
    }

    public boolean isLongClickMode() {
        return !selectedItemMap.isEmpty();
    }

    public void setFiles(List<T> files) {
        this.files = files;
        Collections.sort(files, sortComparator);
        selectedItemMap.clear();
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
            view = createViewAndSetViewHolder();
        }
        holder = (ViewHolder) view.getTag();

        holder.update(position);

        final T displayItem = files.get(position);
        if(isLongClickMode())
        {
            holder.check.setVisibility(View.VISIBLE);
        }
        else
        {
            holder.check.setVisibility(View.GONE);
        }

        VFile actualFile = displayItem.getOriginalFile();
        holder.icon.setTag(actualFile.getAbsolutePath());
        itemIcon.setIcon(new VFile(actualFile) {

            @Override
            public String getName() {
                return displayItem.getName();
            }

            @Override
            public String getExtensiontName() {
                int p = displayItem.getName().lastIndexOf('.');
                return displayItem.getName().substring(p < 0? 0 : p+1);
            }
        }, holder.icon, true);
        holder.check.setChecked(displayItem.isChecked());
        holder.name.setText(displayItem.getName());
        holder.time.setText(DateUtility.formatShortDateAndTime(context, displayItem.getDisplayTime()));
        holder.sdIndicator.setVisibility(displayItem.isInExternalStorage()? View.INVISIBLE : View.VISIBLE);

        if(displayItem.isChecked())
        {
            holder.container.setBackgroundColor(ThemeUtility.getItemSelectedBackgroundColor());
        }
        else
        {
            holder.container.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        updateView(holder, displayItem);

        return view;
    }

    protected abstract void updateView(ViewHolder holder, T displayItem);

    protected abstract View createViewAndSetViewHolder();

    public void deselectItem(int position, T displayItem)
    {
        displayItem.setChecked(false);
        selectedItemMap.remove(position);
        notifyDataSetChanged();
        notifySelectedItemsChanged();
    }

    private void notifySelectedItemsChanged() {
        if (onSelectedItemsChangedListener != null)
            onSelectedItemsChangedListener.onSelectedItemsChanged();
    }

    public void selectItem(int position, T displayItem)
    {
        displayItem.setChecked(true);
        selectedItemMap.put(position, displayItem);
        notifyDataSetChanged();
        notifySelectedItemsChanged();
    }

    public void deselectAllItems()
    {
        for(DisplayItem vFile: selectedItemMap.values())
        {
            vFile.setChecked(false);
        }
        selectedItemMap.clear();
        notifyDataSetChanged();
        notifySelectedItemsChanged();
    }

    public void selectAllItems()
    {
        for(int i=0;i<files.size();i++)
        {
            files.get(i).setChecked(true);
            selectedItemMap.put(i, files.get(i));
        }
        notifyDataSetChanged();
        notifySelectedItemsChanged();
    }

    public HashMap<Integer,T> getSelectedItemMap()
    {
        return selectedItemMap;
    }

    abstract class ViewHolder {
        RelativeLayout container;
        CheckBox check;
        ImageView icon;
        TextView name;
        TextView time;
        ImageView sdIndicator;
        int position;

        public CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    selectItem(position, files.get(position));
                }
                else
                {
                    deselectItem(position, files.get(position));
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
