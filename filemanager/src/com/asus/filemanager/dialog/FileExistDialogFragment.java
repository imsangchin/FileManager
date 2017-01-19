
package com.asus.filemanager.dialog;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.editor.EditorUtility.ExistPair;
import com.asus.filemanager.editor.Mutex;
import com.asus.filemanager.provider.ProviderUtility.Thumbnail;
import com.asus.filemanager.utility.DebugLog;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.MimeMapUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public class FileExistDialogFragment extends DialogFragment implements OnClickListener {

    private static final String TAG = "FileExistDialogFragment";
    private static final boolean DEBUG = DebugLog.DEBUG;

    public static FileExistDialogFragment newInstance(ExistPair arg) {
        FileExistDialogFragment fragment = new FileExistDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("oldFile", arg.getOldFile());
        args.putSerializable("newFile", arg.getNewFile());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        File oldFile = (File) getArguments().getSerializable("oldFile");
        File newFile = (File) getArguments().getSerializable("newFile");

        Bitmap oldImage =
                Thumbnail.getThumbnailItem(getActivity().getApplicationContext().getContentResolver(), oldFile.getAbsolutePath()).thumbnail;
        Bitmap newImage =
                Thumbnail.getThumbnailItem(getActivity().getApplicationContext().getContentResolver(), newFile.getAbsolutePath()).thumbnail;

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View fileExitView = inflater.inflate(R.layout.dialog_exist_file, null);

        TextView fileName = (TextView) fileExitView.findViewById(R.id.ExistFile);
        fileName.setText(getString(R.string.paste_exist_file, oldFile.getName()));

        ImageView oldIcon = (ImageView) fileExitView.findViewById(R.id.ExistOldIcon);
        if(oldImage != null)
            oldIcon.setImageBitmap(oldImage);
        else
            oldIcon.setImageResource(MimeMapUtility.getIconRes(new VFile(oldFile)));

        ImageView newIcon = (ImageView) fileExitView.findViewById(R.id.ExistNewIcon);
        if(newImage != null)
            newIcon.setImageBitmap(newImage);
        else
            newIcon.setImageResource(MimeMapUtility.getIconRes(new VFile(newFile)));

        TextView oldSize = (TextView) fileExitView.findViewById(R.id.ExistOldSize);
        TextView newSize = (TextView) fileExitView.findViewById(R.id.ExistNewSize);

        if (oldFile.isDirectory()) {
            oldSize.setVisibility(View.GONE);
            newSize.setVisibility(View.GONE);
        } else {
            oldSize.setVisibility(View.VISIBLE);
            newSize.setVisibility(View.VISIBLE);
            oldSize.setText(FileUtility.bytes2String(getActivity().getApplicationContext(), oldFile.length(), 1));
            newSize.setText(FileUtility.bytes2String(getActivity().getApplicationContext(), newFile.length(), 1));
        }

        TextView oldDate = (TextView) fileExitView.findViewById(R.id.ExistOldDate);
        oldDate.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(
                new Date(oldFile.lastModified())));

        TextView newDate = (TextView) fileExitView.findViewById(R.id.ExistNewDate);
        newDate.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(
                new Date(newFile.lastModified())));

        int spacing_left = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_left);
        int spacing_top = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_top);
        int spacing_right = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_right);
        int spacing_bottom = (int)getResources().getDimension(R.dimen.dialog_layout_spacing_bottom);


        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.paste_exist_dialog)
                .setPositiveButton(R.string.replace, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setNeutralButton(R.string.keep, this)
                .create();

        dialog.setView(fileExitView, spacing_left, spacing_top, spacing_right, spacing_bottom);
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        CheckBox checkApply = (CheckBox) getDialog().findViewById(R.id.ExistCheckBox);
        if (which == Dialog.BUTTON_NEGATIVE) {
            onCancel(dialog);
        } else {
            if (which == Dialog.BUTTON_POSITIVE)
                EditorAsyncHelper.setPasteFileOverWrite(true, checkApply.isChecked());
            else
                EditorAsyncHelper.setPasteFileOverWrite(false, checkApply.isChecked());

            ((FileManagerActivity) getActivity()).displayDialog(DialogType.TYPE_PASTE_DIALOG, null);

            Mutex.Unlock();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (DEBUG) {
            Log.d(TAG, "onCancel");
        }
        EditorAsyncHelper.setPasteFileTerminate();
        Mutex.Unlock();
    }
}
