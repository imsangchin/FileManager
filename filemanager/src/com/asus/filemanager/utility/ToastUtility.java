package com.asus.filemanager.utility;

import android.content.Context;
import android.widget.Toast;

public class ToastUtility {

    public static void show(Context context, int id)
    {
        show(context,id,Toast.LENGTH_SHORT);
    }

    public static void show(Context context, int id, int duration)
    {
        Toast toast = Toast.makeText(context, id, duration);
        toast.show();
    }

    public static void show(Context context, int id, Object... formatArgs)
    {
        Toast toast = Toast.makeText(context, context.getResources().getString(id, formatArgs), Toast.LENGTH_LONG);
        toast.show();
    }
    
    public static void show(Context context,String content){
        Toast toast = Toast.makeText(context, content, Toast.LENGTH_LONG);
        toast.show();
    }
}
