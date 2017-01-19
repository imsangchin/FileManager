package com.asus.filemanager.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity.DialogType;
import com.asus.filemanager.utility.ThemeUtility;
import com.asus.filemanager.utility.VFile;

public class DropDialogFragment extends DialogFragment{

    private static final String TAG = "DropDialogFragment";
    private VFile mFile;
    private int mAction;
    private String[] mItems;
    private ArrayAdapter<String> mAdapter;

    public static final int DROP_ACTION_ALL = 0;
    public static final int DROP_ACTION_COPY_ONLY = 1;

    public static DropDialogFragment newInstance(VFile file, int action) {
        DropDialogFragment fragment = new DropDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("file", file);
        args.putInt("action", action);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mFile = (VFile) getArguments().getSerializable("file");
        mAction = (int) getArguments().getInt("action");

        // +++ initialize mItems and adapter
        mItems = this.getResources().getStringArray(R.array.drop_actions);

        if (mAction == DROP_ACTION_COPY_ONLY) {
            String[] temp = {mItems[0]}; // only support copy action
            mItems = null;
            mItems = temp;
        }

        mAdapter = new ArrayAdapter<String>(this.getActivity(), R.layout.drop_action_list, mItems){

            ViewHolder holder;

            class ViewHolder {
                ImageView icon;
                TextView action;
            }

            public View getView(int position, View converView, ViewGroup parent) {

                if (converView == null) {
                    converView = getActivity().getLayoutInflater().inflate(R.layout.drop_action_list, null);
                    holder = new ViewHolder();
                    holder.icon = (ImageView) converView.findViewById(R.id.drop_icon);
                    holder.action = (TextView) converView.findViewById(R.id.drop_action);
                    converView.setTag(holder);
                } else {
                    holder = (ViewHolder) converView.getTag();
                }

                if (holder.icon != null) {
                    if (position == 0) {
                        holder.icon.setImageResource(R.drawable.asus_ep_drop_action_copy);
                    } else if (position == 1) {
                        holder.icon.setImageResource(R.drawable.asus_ep_drop_action_move);
                    }
                }

                if (holder.action != null) {
                    holder.action.setText(mItems[position]);
                }

                return converView;
            }
        };
        // ---

        final FileListFragment fileListFragment = (FileListFragment) getFragmentManager().findFragmentById(R.id.filelist);
        String targetFolderName = "";
        String targetFolderHint = this.getResources().getString(R.string.drop_target_to);
        if (mFile != null) {
            targetFolderName = mFile.getName();
        } else {
            Log.w(TAG, "Drop Dialog get mFile is null, so that it doesn't have target name");
        }


        return new AlertDialog.Builder(getActivity(),ThemeUtility.getAsusAlertDialogThemeId())
        .setTitle(targetFolderHint + "  " + targetFolderName)
        .setAdapter(mAdapter, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        dismiss();
                        if (mFile != null) {
                            fileListFragment.dropForAction(mFile, DialogType.TYPE_DROP_FOR_COPY);
                        } else {
                            Log.d(TAG, "onCreateDialog get file is null");
                        }

                        break;
                    case 1:
                        dismiss();
                        if (mFile != null) {
                            fileListFragment.dropForAction(mFile, DialogType.TYPE_DROP_FOR_CUT);
                        } else {
                            Log.d(TAG, "onCreateDialog get file is null");
                        }
                        break;
                }
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                fileListFragment.onDropSelectedItems();
            }
        })
       .create();
    }
}