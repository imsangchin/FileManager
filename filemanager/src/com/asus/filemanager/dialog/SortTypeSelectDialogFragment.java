package com.asus.filemanager.dialog;

import com.asus.filemanager.R;
import com.asus.filemanager.activity.FileListFragment;
import com.asus.filemanager.activity.FileManagerActivity;
import com.asus.filemanager.ga.GaUserPreference;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.ThemeUtility;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;

public class SortTypeSelectDialogFragment extends DialogFragment implements OnClickListener {

    private static final String TAG = "SortTypeSelectDialogFragment";
    private static final boolean DBG = true;

    private int mInitialValue;
    private String[] mItems;

    public static SortTypeSelectDialogFragment newInstance(Bundle args) {
        SortTypeSelectDialogFragment fragment = new SortTypeSelectDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mInitialValue = getArguments().getInt("initialValue");
        mItems = getArguments().getStringArray("options");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        FileManagerActivity activity = (FileManagerActivity)getActivity();
        if (activity.isPadMode()) {
            dismiss();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int themeAsusLightDialogAlertId = ThemeUtility.getAsusAlertDialogThemeId();

        AlertDialog dialog = new AlertDialog.Builder(getActivity(), ThemeUtility.getAsusAlertDialogThemeId())
            .setTitle(R.string.action_sort_by)
            .setNegativeButton(android.R.string.cancel, this)
            .setSingleChoiceItems(mItems, mInitialValue, this)
            .create();
        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which != DialogInterface.BUTTON_NEGATIVE) {
            FileListFragment fileListFragment = (FileListFragment)getActivity()
                .getFragmentManager().findFragmentById(R.id.filelist);
            which = which>0?which+1:which;
            fileListFragment.sortFiles(which);
            FileUtility.saveCurrentSortType(getActivity(), which);

            String label = null;
            switch (which) {
                case FileListFragment.SORT_TYPE:
                    label = GaUserPreference.LABEL_SORT_BY_TYPE;
                    break;
                case FileListFragment.SORT_NAME_ASCENDING:
                    label = GaUserPreference.LABEL_SORT_BY_NAME_Z_TO_A;
                    break;
                case FileListFragment.SORT_NAME_DESCENDING:
                    label = GaUserPreference.LABEL_SORT_BY_NAME_A_TO_Z;
                    break;
                case FileListFragment.SORT_DATE_ASCENDING:
                    label = GaUserPreference.LABEL_SORT_BY_DATE_OLDEST_TO_LATEST;
                    break;
                case FileListFragment.SORT_DATE_DESCENDING:
                    label = GaUserPreference.LABEL_SORT_BY_DATE_LATEST_TO_OLDEST;
                    break;
                case FileListFragment.SORT_SIZE_ASCENDING:
                    label = GaUserPreference.LABEL_SORT_BY_SIZE_LARGE_TO_SMALL;
                    break;
                case FileListFragment.SORT_SIZE_DESCENDING:
                    label = GaUserPreference.LABEL_SORT_BY_SIZE_SMALL_TO_LARGE;
                    break;
                default:
                    break;
            }

            GaUserPreference.getInstance().sendEvents(getActivity(), GaUserPreference.CATEGORY_NAME,
                    GaUserPreference.ACTION_SORT_FILE_LIST, label, null);

            dismiss();
        }
    }


}
