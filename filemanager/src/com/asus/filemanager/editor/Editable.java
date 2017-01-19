package com.asus.filemanager.editor;

import android.os.Handler;

/**
 * Created by Yenju_Lai on 2016/5/10.
 */
public interface Editable {
    Handler getEditHandler();
    EditorUtility.RequestFrom getRequester();
}
