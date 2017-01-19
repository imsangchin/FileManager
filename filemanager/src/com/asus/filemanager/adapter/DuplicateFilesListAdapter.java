package com.asus.filemanager.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.Layout;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerApplication;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DuplicateFilesListAdapter extends BaseExpandableListAdapter {

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm a");
    private List<List<VFile>> duplicateFilesList;
    private Context context;
    private ItemIcon itemIcon;
    private List<VFile> deleteFilesList;
    private OnChildCheckedListener onChildCheckedListener;
    private HashMap<String,String> ellipsizeTextMap;

    public interface OnChildCheckedListener{
        void onChildChecked(int groupPosition, int childPosition);
    }

    public DuplicateFilesListAdapter(Context context, List<List<VFile>> duplicateFilesList) {
        this.context = context;
        this.duplicateFilesList = duplicateFilesList;
        deleteFilesList = new ArrayList<>();
        itemIcon = new ItemIcon(context, false);
        ellipsizeTextMap = new HashMap<>();
    }

    public void setDuplicateFilesList(List<List<VFile>> duplicateFilesList) {
        if (this.duplicateFilesList != null)
            this.duplicateFilesList.clear();
        if (duplicateFilesList != null) {
            this.duplicateFilesList = duplicateFilesList;
        }
        deleteFilesList.clear();
        notifyDataSetChanged();

    }

    public List<List<VFile>> getDuplicateFilesList() {
        return duplicateFilesList;
    }

    public List<VFile> getDeleteFilesList() {
        return deleteFilesList;
    }

    private void updateDeleteFileslist(VFile vfile)
    {
        if(vfile.getChecked())
            deleteFilesList.add(vfile);
        else
            deleteFilesList.remove(vfile);
    }

    public void setChildChecked(int groupPosition, int childPosition)
    {
        VFile vfile = duplicateFilesList.get(groupPosition).get(childPosition);
        vfile.setChecked(!vfile.getChecked());
        updateDeleteFileslist(vfile);
        notifyDataSetChanged();
    }

    public void setOnChildCheckedListener(OnChildCheckedListener onChildCheckedListener)
    {
        this.onChildCheckedListener = onChildCheckedListener;
    }

    public void clearEllipsizeTextMap()
    {
        if(ellipsizeTextMap!=null)
        {
            ellipsizeTextMap.clear();
        }
    }

    @Override
    public VFile getChild(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return duplicateFilesList.get(groupPosition).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ChildViewHolder holder;
        View view = convertView;

        if(view==null){
            view = LayoutInflater.from(context).inflate(R.layout.duplicatefiles_child_list_item, null);

            holder = new ChildViewHolder();
            holder.container = (RelativeLayout) view.findViewById(R.id.duplicatefiles_child_list_item_container);
            holder.check = (CheckBox) view.findViewById(R.id.duplicatefiles_child_list_item_checkbox);
            holder.path = (TextView) view.findViewById(R.id.duplicatefiles_child_list_item_path);
            holder.externalIcon = (ImageView) view.findViewById(R.id.duplicatefiles_child_list_item_external);
            holder.datetime = (TextView) view.findViewById(R.id.duplicatefiles_child_list_item_datetime);
            view.setTag(holder);
        }
        else{
            holder = (ChildViewHolder) view.getTag();
        }

        holder.update(groupPosition, childPosition);
        if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
            if(isLastChild)
                holder.container.setBackgroundResource(R.drawable.dark_card_bottom);
            else
                holder.container.setBackgroundResource(R.drawable.dark_card_mid);
        } else {
            if(isLastChild)
                holder.container.setBackgroundResource(R.drawable.card_bottom);
            else
                holder.container.setBackgroundResource(R.drawable.card_mid);
        }

        VFile file = duplicateFilesList.get(groupPosition).get(childPosition);

        holder.check.setChecked(file.getChecked());

        if(ellipsizeTextMap.containsKey(file.getAbsolutePath()))
        {
            //had ellipsize result.
            holder.path.setText(ellipsizeTextMap.get(file.getAbsolutePath()));
        }else{
            holder.path.setText(file.getAbsolutePath());
            //do ellipsize function
            ellipsizeMultipleLine(holder.path,2);
        }
        holder.datetime.setText(dateTimeFormat.format(file.lastModified()));

        return view;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // TODO Auto-generated method stub
        if(duplicateFilesList.size()>groupPosition)
            return duplicateFilesList.get(groupPosition).size();
        else
            return 0;
    }

    @Override
    public List<VFile> getGroup(int groupPosition) {
        // TODO Auto-generated method stub
        return duplicateFilesList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        // TODO Auto-generated method stub
        return duplicateFilesList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        // TODO Auto-generated method stub
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        GroupViewHolder holder;
        View view = convertView;
        if(view==null){
            view = LayoutInflater.from(context).inflate(R.layout.duplicatefiles_group_list_item, null);

            holder = new GroupViewHolder();
            holder.container = (RelativeLayout) view.findViewById(R.id.duplicatefiles_group_list_item_container);
            holder.icon = (ImageView) view.findViewById(R.id.duplicatefiles_group_list_item_icon);
            holder.mSwitch = (ImageView) view.findViewById(R.id.duplicatefiles_group_list_item_switch);
            holder.name = (TextView) view.findViewById(R.id.duplicatefiles_group_list_item_name);
            holder.sizes = (TextView) view.findViewById(R.id.duplicatefiles_group_list_item_sizes);
            view.setTag(holder);
        }
        else{
            holder = (GroupViewHolder) view.getTag();
        }
//        change background
        if(ThemeUtility.getThemeType() == ThemeUtility.THEME.DARK) {
            if(isExpanded)
            {
                holder.mSwitch.setImageResource(R.drawable.asus_ic_close);
                holder.container.setBackgroundResource(R.drawable.dark_card_top);
            } else {
                holder.mSwitch.setImageResource(R.drawable.asus_ic_open);
                holder.container.setBackgroundResource(R.drawable.dark_card_whole);
            }
        } else {
            if(isExpanded)
            {
                holder.mSwitch.setImageResource(R.drawable.asus_ic_close);
                holder.container.setBackgroundResource(R.drawable.card_top);
            } else {
                holder.mSwitch.setImageResource(R.drawable.asus_ic_open);
                holder.container.setBackgroundResource(R.drawable.card_whole);
            }
        }

        // set mswitch color by theme
        ThemeUtility.setItemIconColor(context, holder.mSwitch.getDrawable());

        VFile file = duplicateFilesList.get(groupPosition).get(0);

        holder.icon.setTag(file.getAbsolutePath());
        itemIcon.setIcon(file, holder.icon, true);

        holder.name.setText(file.getName());
        String sizesInfo = Formatter.formatFileSize(context, file.length())+"("+duplicateFilesList.get(groupPosition).size()+")";
        holder.sizes.setText(sizesInfo);
        return view;
    }

    @Override
    public boolean hasStableIds() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return true;
    }


    private class GroupViewHolder {
        RelativeLayout container;
        ImageView icon;
        ImageView mSwitch;
        TextView name;
        TextView sizes;
    }

    private class ChildViewHolder {
        RelativeLayout container;
        CheckBox check;
        ImageView externalIcon;
        TextView path;
        TextView datetime;
        int groupPosition;
        int childPosition;

        public CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(duplicateFilesList.get(groupPosition).get(childPosition).getChecked() ^ isChecked)
                {
                    duplicateFilesList.get(groupPosition).get(childPosition).setChecked(isChecked);
                    updateDeleteFileslist(duplicateFilesList.get(groupPosition).get(childPosition));
                    notifyDataSetChanged();
                    if(onChildCheckedListener!=null)
                        onChildCheckedListener.onChildChecked(groupPosition,childPosition);
                }
            }
        };

        public void update(int groupPosition, int childPosition)
        {
            this.groupPosition = groupPosition;
            this.childPosition = childPosition;
            if(check!=null)
                check.setOnCheckedChangeListener(onCheckedChangeListener);
        }

    }

    private void ellipsizeMultipleLine(TextView textView, final int kMaxNumEllipsizeLine) {
        textView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right,
                                       int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                if (v instanceof TextView) {
                    TextView text = (TextView)v;
                    Layout textLayout = text.getLayout();
                    if(textLayout==null)
                        return;
                    int numNoEllipsizeLine = kMaxNumEllipsizeLine-1;
                    int numLine = textLayout.getLineCount();
                    String textContent = text.getText().toString();
                    StringBuilder strNoEllipsize = new StringBuilder();
                    StringBuilder strNeedToEllipsize = new StringBuilder();
                    for (int i = 0; i < numNoEllipsizeLine; ++i) {
                        strNoEllipsize.append(textContent.substring(
                                textLayout.getLineStart(i),
                                textLayout.getLineEnd(i)
                        ));
                    }
                    for (int i = numNoEllipsizeLine; i < numLine; ++i) {
                        strNeedToEllipsize.append(textContent.substring(
                                textLayout.getLineStart(i),
                                textLayout.getLineEnd(i)
                        ));
                    }
                    String strEllipsize = TextUtils.ellipsize(strNeedToEllipsize, text.getPaint(),
                            (float)text.getWidth(),
                            TextUtils.TruncateAt.MIDDLE).toString();
                    String result = strNoEllipsize + strEllipsize;

                    // save ellipsize text result
                    ellipsizeTextMap.put(textContent,result);
                    text.setText(result);
                }
            }
        });
    }
}
