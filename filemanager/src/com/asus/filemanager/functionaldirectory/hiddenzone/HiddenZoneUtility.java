package com.asus.filemanager.functionaldirectory.hiddenzone;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.asus.filemanager.R;
import com.asus.filemanager.dialog.AddToHiddenZoneDialogFragment;
import com.asus.filemanager.editor.EditPool;
import com.asus.filemanager.editor.EditorAsyncHelper;
import com.asus.filemanager.functionaldirectory.CalculateUsableSpaceTask;
import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.functionaldirectory.IFunctionalDirectory;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.ToastUtility;
import com.asus.filemanager.utility.VFile;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Yenju_Lai on 2016/5/10.
 */
public class HiddenZoneUtility implements IFunctionalDirectory<HiddenZoneDisplayItem> {
    public static final int MSG_HIDE_COMPLETE = 400;
    public static final String HIDDEN_ZONE_NAME = "HiddenCabinet";
    static final String HIDDEN_ZONE_PATH = "/.HiddenCabinet";
    public static final String HIDDEN_ZONE_DIRECTORY_NAME = ".HiddenCabinet";


    private static HiddenZoneUtility hiddenZoneUtility;

    public static HiddenZoneUtility getInstance() {
        if (hiddenZoneUtility == null)
            hiddenZoneUtility = new HiddenZoneUtility(FunctionalDirectoryUtility.getInstance());
        return hiddenZoneUtility;
    }

    private WeakReference<FunctionalDirectoryUtility> utilityReference;
    HiddenZoneUtility(FunctionalDirectoryUtility functionalDirectoryUtility) {
        utilityReference = new WeakReference<>(functionalDirectoryUtility);
    }

    public static void moveToHiddenZone(final Activity activity, final Handler handler, final EditPool editPool, final boolean inCategory) {
        final AddToHiddenZoneDialogFragment hiddenZoneDialog = AddToHiddenZoneDialogFragment.newInstance(editPool, inCategory);
        hiddenZoneDialog.show(activity.getFragmentManager(), "AddToHiddenZoneDialogFragment");
        new CalculateUsableSpaceTask(new CalculateUsableSpaceTask.OnSpaceCalculatedListener() {
            @Override
            public void onSpaceCalculated(boolean isSufficient) {
                if (isSufficient)
                    EditorAsyncHelper.moveToHiddenZone(editPool.getFiles(), handler, inCategory);
                else {
                    hiddenZoneDialog.dismissAllowingStateLoss();
                    ToastUtility.show(activity, R.string.no_space_fail, Toast.LENGTH_LONG);
                }
            }
        }).execute(editPool.getFiles());
    }

    public List<HiddenZoneDisplayItem> listHiddenZoneFiles(VFile target, HiddenZoneDisplayItem currentItem) {
        if (currentItem == null)
            return listHiddenZoneRootFiles(target);
        else
            return listChild(target, currentItem);
    }

    private List<HiddenZoneDisplayItem> listChild(VFile target, HiddenZoneDisplayItem currentItem) {
        List<HiddenZoneDisplayItem> files = new ArrayList<>();
        if (target.listVFiles() == null)
            return files;
        for (VFile file : target.listVFiles()) {
            try {
                files.add(new HiddenZoneDisplayItem(file, currentItem));
            } catch (RuntimeException e) {
                //do nothing
            }
        }
        return files;
    }

    public List<HiddenZoneDisplayItem> listHiddenZoneRootFiles(VFile storageRoot) {
        VFile hiddenZoneDirectory = new LocalVFile(storageRoot, HIDDEN_ZONE_DIRECTORY_NAME);
        List<HiddenZoneDisplayItem> files = new ArrayList<>();
        if (hiddenZoneDirectory == null || !hiddenZoneDirectory.exists() || hiddenZoneDirectory.listVFiles() == null)
            return files;
        boolean isInExternalStorage = utilityReference.get().isInExternalStorage(hiddenZoneDirectory.getPath());
        for (VFile file : hiddenZoneDirectory.listVFiles()) {
            try {
                files.add(new HiddenZoneDisplayItem(file, isInExternalStorage));
            } catch (RuntimeException e) {
                //do nothing
                Log.d("test", e.toString());
            }
        }
        return files;
    }

    public String getHiddenZoneFileName(File file, String rootPath) {
        String hiddenZonePath = rootPath + HIDDEN_ZONE_PATH;
        if (!file.getAbsolutePath().startsWith(hiddenZonePath)) {
            return null;
        }
        else if (file.getParent().compareTo(hiddenZonePath) == 0) {
            Pattern pattern =
                    Pattern.compile("\\.([0-9]+)-(.+)");
            Matcher matcher = pattern.matcher(file.getName());
            if (matcher.find()) {
                return Encryptor.getEncryptor().decode(matcher.group(2));
            }
        }
        else if (file.getName().startsWith(".")) {
            return Encryptor.getEncryptor().decode(file.getName().substring(1));
        }
        return null;
    }

    private long parseHiddenZoneName(String name) {
        Pattern pattern =
                Pattern.compile("\\.([0-9]+)");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return -1;
    }

    @Override
    public List<HiddenZoneDisplayItem> listFiles(VFile currentFile, HiddenZoneDisplayItem currentItem) {
        if (currentItem == null)
            return listHiddenZoneRootFiles(currentFile);
        else
            return listChild(currentFile, currentItem);
    }

    @Override
    public boolean inFunctionalDirectory(File file) {
        /*Pattern pattern =
                Pattern.compile(HIDDEN_ZONE_PATH + "/" + "\\.([0-9]+)-(.+)" + "/.*");
        Matcher matcher = pattern.matcher(file.getAbsolutePath());
        return matcher.find();*/
        return file.getAbsolutePath().contains(HIDDEN_ZONE_PATH);
    }

    @Override
    public String getFunctionalDirectoryRootPath(String volumeRootPath) {
        return volumeRootPath+HIDDEN_ZONE_PATH;
    }

    @Override
    public String getExcludeDirectorySelection(String pathColumnName, String... volumeRootPaths) {
        if(volumeRootPaths==null)
            return null;
        if(volumeRootPaths.length==0)
            return "(" + pathColumnName + " NOT LIKE '%" + HIDDEN_ZONE_PATH
                    + "/%' )";

        String selection ="(";
        for(int i=0;i<volumeRootPaths.length;i++)
        {
            if(i!=0)
                selection += " OR ";
            //avoid SQLite injection
            volumeRootPaths[i] = volumeRootPaths[i].replaceAll("'","''");
            selection += pathColumnName + " not like'" + getFunctionalDirectoryRootPath(volumeRootPaths[i])+"%'";
        }
        selection += ")";
        return selection;
    }
}
