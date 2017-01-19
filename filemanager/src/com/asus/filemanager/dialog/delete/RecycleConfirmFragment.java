package com.asus.filemanager.dialog.delete;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.asus.filemanager.R;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.ga.GaRecycleBin;
import com.asus.filemanager.functionaldirectory.recyclebin.RecycleBinUtility;
import com.asus.filemanager.utility.ThemeUtility;

public class RecycleConfirmFragment extends DeleteDialogFragment {

    @Override
    public Dialog createDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.asus_delete_dialog, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId());
        final CheckBox permanentlyDeleteCheckBox = (CheckBox)view.findViewById(R.id.delete_checkbox);
        permanentlyDeleteCheckBox.setChecked(RecycleBinUtility.getDeleteSetting(getActivity()));
        dialog.setTitle(getActivity().getString(R.string.delete_dialog));
        dialog.setMessage(getActivity().getString(R.string.move_or_delete_msg));
        dialog.setPositiveButton(R.string.ok, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                RecycleBinUtility.preferPermanentlyDelete(getActivity(), permanentlyDeleteCheckBox.isChecked());
                isPermanentlyDelete = permanentlyDeleteCheckBox.isChecked();
                if (isPermanentlyDelete) {
                    handleDelete();
                    GaRecycleBin.getInstance().sendDeleteEvent(getActivity(),
                            getDeleteRequester(), getArguments().getInt("count"), GaRecycleBin.DeleteCategory.PermanentlyDelete);
                }
                else {
                    DeleteDialogFragment calculateSpaceDialog =
                            DeleteDialogFragment.newInstance((EditPool) getArguments().getSerializable("editpool"),
                                    Type.TYPE_CALCULTE_SPACE_PROGRESS_DIALOG);
                    calculateSpaceDialog.show(getFragmentManager(), "DeleteDialogFragment");
                }
            }
        });
        dialog.setNegativeButton(R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handleCancel();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.setView(view);
        return dialog.create();
    }
}
