package com.asus.filemanager.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.VFile;

public class PathIndicatorView extends LinearLayout implements OnTouchListener{

    private Context context;
    private String rootPath;
    private String[] rootNames;
    private OnPathIndicatorListener onPathIndicatorListener;
    private int rootNamesLen = 1;

    public interface OnPathIndicatorListener
    {
        public void onPathClick(String path);
    }

    public void setOnPathIndicatorListener(
            OnPathIndicatorListener onPathIndicatorListener) {
        this.onPathIndicatorListener = onPathIndicatorListener;
    }

    public PathIndicatorView(Context context) {
        this(context, null);
    }

    // Default constructor when inflating from XML file
    public PathIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PathIndicatorView(Context context, AttributeSet attrs,int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void setRootName(String... rootName)
    {
        this.rootNames = rootName;
    }

    public void setPath(String rootPath,VFile dirFile)
    {
        if(dirFile.isDirectory())
            setPath(rootPath,FileUtility.getCanonicalPathNoException(dirFile));
        else
            setPath(rootPath,FileUtility.getCanonicalPathNoException(dirFile.getParentFile()));
    }

    public void setPath(String rootPath,String filePath)
    {
        this.rootPath = rootPath;
        this.removeAllViews();

        //add root path(only show name)
        if(rootNames!=null) {
            rootNamesLen = rootNames.length;
            for(int i = 0;i<rootNamesLen;i++)
                addTextViewToLinearLayout(i, rootNames[i]);
        }else
            addTextViewToLinearLayout(0, context.getString(R.string.device_root_path));

        filePath = filePath.substring(rootPath.length(), filePath.length());
        if (true == filePath.startsWith("/")) {
            filePath = filePath.substring( 1 , filePath.length());
        }

        //add path after root
        String[] paths = filePath.split("/");
        for(int i = 0 ;i<paths.length;i++)
        {
            if(!paths[i].equals(""))
                addTextViewToLinearLayout((i + rootNamesLen), paths[i]);
        }
        TextView finalText = (TextView) this.getChildAt(this.getChildCount()-1);
        finalText.setTextColor(context.getResources().getColor(R.color.path_indicator_press));
    }

    private void addTextViewToLinearLayout(int index, String text){
        TextView textView = null;
        textView = new TextView(context);
        textView.setTag(index);
        if(index!=0)
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_arrow_right_normal, 0, 0, 0);
        textView.setText(text);
        textView.setTextColor(context.getResources().getColor(R.color.pathcolor));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.path_font_size));
        textView.setClickable(false);
        textView.setFocusable(false);
        textView.setOnTouchListener(this);
        this.addView(textView);
    }

    private String searchAbsolutePath(int index)
    {
        if(this.getChildCount()==0)
            return null;

        if(index < rootNamesLen)
        {
            return rootPath;
        }
        String absolutePath = "";
        for(int i = rootNamesLen ; i <= index ; i++)
        {
            String tmpFolderName = ((TextView)this.getChildAt(i)).getText().toString();
            absolutePath = absolutePath + "/" + tmpFolderName;
        }
        absolutePath = rootPath + absolutePath;
        return absolutePath;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // TODO Auto-generated method stub
        //the last one don't trigger
        if(view.equals(this.getChildAt(this.getChildCount()-1)))
            return false;

        int action = event.getAction();
        TextView textView = ((TextView)view);

        if(action == MotionEvent.ACTION_DOWN)
        {
            textView.setTextColor(context.getResources().getColor(R.color.path_indicator_press));
        }
        else if(action == MotionEvent.ACTION_CANCEL)
        {
            textView.setTextColor(context.getResources().getColor(R.color.pathcolor));
            return false;
        }
        else if(action == MotionEvent.ACTION_UP)
        {
            textView.setTextColor(context.getResources().getColor(R.color.pathcolor));
            if(onPathIndicatorListener!=null)
            {
                String path = searchAbsolutePath((int)textView.getTag());
                if(path!=null)
                    onPathIndicatorListener.onPathClick(path);
            }
        }
        return true;
    }


}
