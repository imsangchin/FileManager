package com.asus.filemanager.utility;

import android.content.Context;
import android.os.AsyncTask;

import com.asus.filemanager.provider.AllFilesDatabase;
import com.asus.filemanager.provider.DuplicateFilesDatabase;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ScanDuplicateFilesTask extends AsyncTask<String, Integer, Boolean> {

    public static final int MODE_RESCAN = 0;
    public static final int MODE_SCAN_RECORDED = 1;
    private int mode = MODE_RESCAN;

    private HashMap<Long, List<VFile>> fileSizesMap;
    private HashMap<String, List<VFile>> duplicateFileMap;
    private List<List<VFile>> duplicateFileResultList;
    private List<VFile> updateDatabaseFilesList;
    private List<VFile> notDuplicateFilesList;

    private AllFilesDatabase allFilesDatabase;
    private DuplicateFilesDatabase duplicateFilesDatabase;

    private boolean isRunning = true;
    private int progress = 0;
    private OnDuplicateFileResultListener onDuplicateFileResultListener;

    public interface OnDuplicateFileResultListener {
        public void onDuplicateFileProgress(int progress);
        public void OnDuplicateFileResult(List<List<VFile>> duplicateFileResultList);
    }

    public ScanDuplicateFilesTask(Context context,OnDuplicateFileResultListener onDuplicateFileResultListener)
    {
        this(context,onDuplicateFileResultListener,MODE_RESCAN);
    }

    public ScanDuplicateFilesTask(Context context,OnDuplicateFileResultListener onDuplicateFileResultListener,int mode) {
        this.onDuplicateFileResultListener = onDuplicateFileResultListener;
        this.mode = mode;

        duplicateFileMap = new HashMap<String, List<VFile>>();
        duplicateFileResultList = new ArrayList<List<VFile>>();

        duplicateFilesDatabase = new DuplicateFilesDatabase(context);
        allFilesDatabase = new AllFilesDatabase(context);
        updateDatabaseFilesList = new ArrayList<VFile>();

        notDuplicateFilesList = new ArrayList<VFile>();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setOnDuplicateFileResultListener(OnDuplicateFileResultListener onDuplicateFileResultListener)
    {
        this.onDuplicateFileResultListener = onDuplicateFileResultListener;
    }

    public void onPreExecute() {
    }

    @Override
    public Boolean doInBackground(String... rootPaths) {
        // TODO Auto-generated method stub
        forwardProgress(7);
        switch (mode) {
            case MODE_RESCAN:
                //start scan and store to duplicateFileMap
                fileSizesMap = allFilesDatabase.getSameSizesFileMap(rootPaths);
                scanDuplicateFiles(80);
                break;
            case MODE_SCAN_RECORDED:
                duplicateFileMap = duplicateFilesDatabase.getDuplicateFileMap(rootPaths);
                forwardProgress(80);
                break;
        }
        updateDatabase(1);
        addToResultList(10);
        updateHadDuplicateToDatabase(1);
        sortResultList(1);
        return true;
    }

    protected void onPostExecute(Boolean result) {

        isRunning = false;
        if(onDuplicateFileResultListener!=null)
            onDuplicateFileResultListener.OnDuplicateFileResult(duplicateFileResultList);
    }

    protected void onProgressUpdate(Integer... progress) {
        if(onDuplicateFileResultListener!=null)
            onDuplicateFileResultListener.onDuplicateFileProgress(progress[0]);
    }

    private void forwardProgress(int forward)
    {
        if(forward==0)
            return;

        progress+=forward;
        publishProgress(progress);
    }

    private void scanDuplicateFiles(int progressTaken)
    {
        //---------------for update progress--------------
        float count = 1;
        int lastPercent = 0;
        if(fileSizesMap.size()==0) {
            forwardProgress(progressTaken);
            fileSizesMap.clear();
            return;
        }
        //---------------for update progress--------------

        //prepare all path to MD5 HashMap
        HashMap<String, String> pathToMD5Map = duplicateFilesDatabase.getPathToMD5Map();
        for(List<VFile> fileslist : fileSizesMap.values())
        {
                for(int i = 0 ;i<fileslist.size();i++)
                {
                    String path = FileUtility.getCanonicalPathNoException(fileslist.get(i));
                    String md5 = (pathToMD5Map.containsKey(path))?pathToMD5Map.get(path):getFileMD5(fileslist.get(i));
                    if(md5!=null){
                        if(duplicateFileMap.containsKey(md5)){
                            duplicateFileMap.get(md5).add(fileslist.get(i));
                        }
                        else{
                            ArrayList<VFile> tmpList = new ArrayList<VFile>();
                            tmpList.add(fileslist.get(i));
                            duplicateFileMap.put(md5,tmpList);
                        }
                    }
                }

            //---------------for update progress--------------
            int currentPercent = (int) ((count /fileSizesMap.size()) * progressTaken);
            forwardProgress(currentPercent-lastPercent);
            lastPercent = currentPercent;
            count++;
            //---------------for update progress--------------
        }
        fileSizesMap.clear();
    }

    private void updateDatabase(int progressTaken)
    {
        duplicateFilesDatabase.insertVFiles(updateDatabaseFilesList);
        updateDatabaseFilesList.clear();
        forwardProgress(progressTaken);
    }

    private void addToResultList(int progressTaken)
    {
        //---------------for update progress--------------
        float count = 1;
        int lastPercent = 0;
        if(duplicateFileMap.size()==0) {
            forwardProgress(progressTaken);
        }
        //---------------for update progress--------------


        for(List<VFile> duplicateFileList : duplicateFileMap.values())
        {
            if(duplicateFileList.size()>1) {
                    duplicateFileResultList.add(duplicateFileList);
            }else{
                notDuplicateFilesList.add(duplicateFileList.get(0));
            }

            //---------------for update progress--------------
            int currentPercent = (int) ((count /duplicateFileMap.size()) * progressTaken);
            forwardProgress(currentPercent-lastPercent);
            lastPercent = currentPercent;
            count++;
            //---------------for update progress--------------

        }
        duplicateFileMap.clear();
    }

    private void updateHadDuplicateToDatabase(int progressTaken)
    {
        switch (mode){
            case MODE_RESCAN:
                duplicateFilesDatabase.updateHadDuplicate(duplicateFileResultList);
                break;
            case MODE_SCAN_RECORDED:
                duplicateFilesDatabase.updateHadDuplicate(notDuplicateFilesList,false);
                break;
        }
        forwardProgress(progressTaken);
    }

    private void sortResultList(int progressTaken)
    {
        Collections.sort(duplicateFileResultList, new Comparator<List<VFile>>() {
            @Override
            public int compare(List<VFile> vfiles1, List<VFile> vfiles2) {
                return (int)(vfiles2.get(0).length() - vfiles1.get(0).length());
            }
        });
        forwardProgress(progressTaken);

    }

    private String getFileMD5(VFile file) {
        String md5 = null;
        if (file != null && file.exists()) {
            try {
                BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                final int BUFFER_LENGTH = 4096;
                final int LIMIT_SIZES = BUFFER_LENGTH<<1;
                byte[] buffer = new byte[BUFFER_LENGTH];
                java.security.MessageDigest complete = java.security.MessageDigest.getInstance("MD5");
                int numRead;

                    numRead = inputStream.read(buffer);
                //read head 4k
                if(numRead != -1)
                {
                    complete.update(buffer, 0, numRead);
                    //file sizes > 8k
                    if(file.length()>LIMIT_SIZES) {
                        //read end 4k
                        inputStream.skip(file.length()-LIMIT_SIZES);
                        numRead = inputStream.read(buffer);
                        if (numRead != -1) {
                        complete.update(buffer, 0, numRead);
                    }
                    }
                }
                inputStream.close();

                // byte[] to HEX
                byte[] b = complete.digest();
                md5="";
                for (int i = 0; i < b.length; i++) {
                    md5 += Integer.toString((b[i] & 0xff) + 0x100, 16)
                            .substring(1);
                }

                //this file not exist in database, add to list
                file.setMD5(md5);
                updateDatabaseFilesList.add(file);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                md5 = null;
            }
        }
        return md5;
    }
}
