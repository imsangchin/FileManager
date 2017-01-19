
package com.asus.filemanager.dialog;

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

import com.asus.filemanager.R;
import com.asus.filemanager.editor.Mutex;
import com.asus.filemanager.provider.ProviderUtility.Thumbnail;
import com.asus.filemanager.functionaldirectory.Movable;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.MimeMapUtility;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public class MoveDstExistDialogFragment extends DialogFragment implements OnClickListener {

    private static final String TAG = "FileExistDialogFragment";

    public static MoveDstExistDialogFragment newInstance(Movable movable) {
        MoveDstExistDialogFragment fragment = new MoveDstExistDialogFragment();
        Bundle args = new Bundle();
        args.putString("dst", movable.getDestination());
        args.putSerializable("src", movable.getSourceFile());
        fragment.setArguments(args);
        return fragment;
    }

    public enum Action {
        KEEP,
        REPLACE,
        CANCEL
    }

    private Action selectedAction = Action.CANCEL;
    private boolean alwaysApply = false;

    public Action getSelectedAction() {
        return selectedAction;
    }

    public boolean isAlwaysApply() {
        return alwaysApply;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        File dstFile = new LocalVFile(getArguments().getString("dst"));
        File srcFile = (File) getArguments().getSerializable("src");

        Bitmap oldImage =
                Thumbnail.getThumbnailItem(getActivity().getApplicationContext().getContentResolver(), dstFile.getAbsolutePath()).thumbnail;
        Bitmap newImage =
                Thumbnail.getThumbnailItem(getActivity().getApplicationContext().getContentResolver(), srcFile.getAbsolutePath()).thumbnail;

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View fileExitView = inflater.inflate(R.layout.dialog_exist_file, null);

        TextView fileName = (TextView) fileExitView.findViewById(R.id.ExistFile);
        fileName.setText(getString(R.string.paste_exist_file, dstFile.getName()));

        ImageView oldIcon = (ImageView) fileExitView.findViewById(R.id.ExistOldIcon);
        if(oldImage != null)
            oldIcon.setImageBitmap(oldImage);
        else
            oldIcon.setImageResource(MimeMapUtility.getIconRes(new VFile(dstFile)));

        ImageView newIcon = (ImageView) fileExitView.findViewById(R.id.ExistNewIcon);
        if(newImage != null)
            newIcon.setImageBitmap(newImage);
        else
            newIcon.setImageResource(MimeMapUtility.getIconRes(new VFile(srcFile)));

        TextView oldSize = (TextView) fileExitView.findViewById(R.id.ExistOldSize);
        TextView newSize = (TextView) fileExitView.findViewById(R.id.ExistNewSize);

        if (dstFile.isDirectory()) {
            oldSize.setVisibility(View.GONE);
            newSize.setVisibility(View.GONE);
        } else {
            oldSize.setVisibility(View.VISIBLE);
            newSize.setVisibility(View.VISIBLE);
            oldSize.setText(FileUtility.bytes2String(getActivity().getApplicationContext(), dstFile.length(), 1));
            newSize.setText(FileUtility.bytes2String(getActivity().getApplicationContext(), srcFile.length(), 1));
        }

        TextView oldDate = (TextView) fileExitView.findViewById(R.id.ExistOldDate);
        oldDate.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(
                new Date(dstFile.lastModified())));

        TextView newDate = (TextView) fileExitView.findViewById(R.id.ExistNewDate);
        newDate.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(
                new Date(srcFile.lastModified())));

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
                selectedAction = Action.REPLACE;
            else
                selectedAction = Action.KEEP;
            alwaysApply = checkApply.isChecked();
            Mutex.Unlock();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        Log.d(TAG, "onCancel");
        Mutex.Unlock();
    }
}
