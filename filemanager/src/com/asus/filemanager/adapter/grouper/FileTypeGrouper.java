package com.asus.filemanager.adapter.grouper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.asus.filemanager.R;
import com.asus.filemanager.adapter.ExpandableGroupInfo;
import com.asus.filemanager.utility.VFile;
import com.asus.filemanager.utility.reflectionApis;

import android.content.Context;
import android.webkit.MimeTypeMap;

public class FileTypeGrouper extends AbsFileGrouper {

    public FileTypeGrouper(Context context, VFile[] data) {
        super(context, data);
        startGroupProcess();
    }

    protected void startGroupProcess() {
        if (mData == null) {
            return;
        }

        ExpandableGroupInfo groupInfo;
        List<VFile> list;
        for (VFile file : mData) {

            String mimeType = reflectionApis.mediaFile_getMimeTypeForFile(file.getName().toLowerCase());

            if (mimeType == null) {
                // Get the MIME type from MimeTypeMap if it dosen't defined in MediaFile
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(file).toLowerCase());
            }

            if(mimeType != null){
                groupInfo = getGroupInfoFromMimeType(mimeType);
            }else{
                groupInfo = getGroupInfo(getFileExtension(file).toLowerCase());
            }

            if (mResultList.containsKey(groupInfo)) {
                mResultList.get(groupInfo).add(file);
            } else {
                list = new ArrayList<VFile>();
                list.add(file);
                mResultList.put(groupInfo, list);
            }
        }
    }

    private String getFileExtension(File file) {
        if (file == null) {
            return "";
        }

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private ExpandableGroupInfo getGroupInfo(String extName) {
        switch (extName) {
        case "doc":
        case "docx":
            return new ExpandableGroupInfo(mContext.getResources().getString(R.string.office_word),
                    ExpandableGroupInfo.TitleType.WORD);
        case "xls":
        case "xlsx":
            return new ExpandableGroupInfo(mContext.getResources().getString(R.string.office_excel),
                    ExpandableGroupInfo.TitleType.EXCEL);
        case "ppt":
        case "pptx":
            return new ExpandableGroupInfo(mContext.getResources().getString(R.string.office_powerpoint),
                    ExpandableGroupInfo.TitleType.POWERPOINT);
        case "pdf":
            return new ExpandableGroupInfo(mContext.getResources().getString(R.string.category_pdf1),
                    ExpandableGroupInfo.TitleType.PDF);
        case "txt":
            return new ExpandableGroupInfo(mContext.getResources().getString(R.string.group_title_txt),
                    ExpandableGroupInfo.TitleType.TXT);
        default:
            return new ExpandableGroupInfo(mContext.getResources().getString(R.string.group_title_others),
                    ExpandableGroupInfo.TitleType.OTHERS);
        }
    }
    private ExpandableGroupInfo getGroupInfoFromMimeType(String mimeType) {
        switch (mimeType) {
            case "application/msword":
                return new ExpandableGroupInfo(mContext.getResources().getString(R.string.office_word),
                        ExpandableGroupInfo.TitleType.WORD);
            case "application/vnd.ms-excel":
                return new ExpandableGroupInfo(mContext.getResources().getString(R.string.office_excel),
                        ExpandableGroupInfo.TitleType.EXCEL);
            case "application/vnd.ms-powerpoint":
            case "application/mspowerpoint":
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":
                return new ExpandableGroupInfo(mContext.getResources().getString(R.string.office_powerpoint),
                        ExpandableGroupInfo.TitleType.POWERPOINT);
            case "application/pdf":
                return new ExpandableGroupInfo(mContext.getResources().getString(R.string.category_pdf1),
                        ExpandableGroupInfo.TitleType.PDF);
            case "text/plain":
                return new ExpandableGroupInfo(mContext.getResources().getString(R.string.group_title_txt),
                        ExpandableGroupInfo.TitleType.TXT);
            default:
                return new ExpandableGroupInfo(mContext.getResources().getString(R.string.group_title_others),
                        ExpandableGroupInfo.TitleType.OTHERS);
        }
    }
}
