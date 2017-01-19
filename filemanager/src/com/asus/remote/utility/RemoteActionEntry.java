package com.asus.remote.utility;

import java.util.ArrayList;
import java.util.List;

import com.asus.filemanager.utility.VFile;

// used for remote storage copy
public class RemoteActionEntry {
        private static final String TAG = "RemoteActionEntry";

        public VFile[] files;
        public VFile pasteVFile;
        public String dstFolder;
        public String action;
        private boolean mContainsRetrictFiles = false;

        public RemoteActionEntry(VFile[] src, VFile vFile, String act, boolean isSupportRestrictFiles) {
            List<VFile> list = expandFiles(src, isSupportRestrictFiles);

            files = new VFile[list.size()];
            list.toArray(files);
            pasteVFile = vFile;
            action = act;
        }

        private List<VFile> expandFiles(VFile[] src, boolean isSupportRestrictFiles) {
            List<VFile> list = new ArrayList<VFile>();
            if (src != null) {
                for (VFile file : src) {
                    list.add(file);
                    if (isSupportRestrictFiles) {
                        if (file.getHasRetrictFiles()) {
                            mContainsRetrictFiles = true;
                            VFile[] retrictFiles = new VFile[file.getRestrictFiles().size()];
                            file.getRestrictFiles().toArray(retrictFiles);
                            list.addAll(expandFiles(retrictFiles, isSupportRestrictFiles));
                        }
                    } else {
                        // no need to expand files when remote side doesn't support restrict files.
                    }
                }
            }
            return list;
        }

        public boolean containsRetrictFiles() {
            return mContainsRetrictFiles;
        }
    }