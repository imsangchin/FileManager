
package com.asus.filemanager.utility;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public class SortUtility {
    public static class SortType {
        public static final int SORT_TYPE_UP = 0;
        public static final int SORT_TYPE_DOWN = 1;
        public static final int SORT_NAME_UP = 2;
        public static final int SORT_NAME_DOWN = 3;
        public static final int SORT_DATE_UP = 4;
        public static final int SORT_DATE_DOWN = 5;
        public static final int SORT_SIZE_UP = 6;
        public static final int SORT_SIZE_DOWN = 7;
        public static final int SORT_LOCATION_UP = 8;
        public static final int SORT_LOCATION_DOWN = 9;
    }

    public static class ComparaByType implements Comparator<VFile> {

        private boolean mAscending;

        public ComparaByType(boolean asc)
        {
            mAscending = asc;
        }

        @Override
        public int compare(VFile file1, VFile file2) {
            int result = 0;

            Integer file1Type = MimeMapUtility.getFileType(file1);
            Integer file2Type = MimeMapUtility.getFileType(file2);

            if (mAscending) {
                if (file1.isDirectory() && file2.isDirectory()) {
                    result = file1.getName().compareToIgnoreCase(file2.getName());
                } else if (!file1.isDirectory() && file2.isDirectory()) {
                    result = 1;
                } else if (file1.isDirectory() && !file2.isDirectory()) {
                    result = -1;
                } else {
                    result = file1Type.compareTo(file2Type);
                    if (result == 0) {
                        String file1Extension = file1.getExtensiontName();
                        String file2Extension = file2.getExtensiontName();
                        result = file1Extension.compareToIgnoreCase(file2Extension);
                        if(result == 0) {
                            result = file1.getName().compareToIgnoreCase(file2.getName());
                        }
                    }
                }
            } else {
                if (file1.isDirectory() && file2.isDirectory()) {
                    result = file2.getName().compareToIgnoreCase(file1.getName());
                } else if (!file1.isDirectory() && file2.isDirectory()) {
                    result = 1;
                } else if (file1.isDirectory() && !file2.isDirectory()) {
                    result = -1;
                } else {
                    result = file2Type.compareTo(file1Type);
                    if (result == 0) {
                        String file1Extension = file1.getExtensiontName();
                        String file2Extension = file2.getExtensiontName();
                        result = file2Extension.compareToIgnoreCase(file1Extension);
                        if(result == 0) {
                            result = file2.getName().compareToIgnoreCase(file1.getName());
                        }
                    }
                }
            }

            return result;
        }
    }

    public static class ComparaByDate implements Comparator<VFile> {

        private boolean mAscending;

        public ComparaByDate(boolean asc)
        {
            mAscending = asc;
        }

        @Override
        public int compare(VFile file1, VFile file2) {
            int result = 0;
            ;

            Long file1Date = file1.lastModified();
            Long file2Date = file2.lastModified();

            if (!mAscending) {
                if (file1.isDirectory() && file2.isDirectory()) {
                    result = file1Date.compareTo(file2Date);
                    if (result == 0)
                        result = file1.getName().compareToIgnoreCase(file2.getName());
                } else if (!file1.isDirectory() && file2.isDirectory()) {
                    result = 1;
                } else if (file1.isDirectory() && !file2.isDirectory()) {
                    result = -1;
                } else {
                    result = file1Date.compareTo(file2Date);
                    if (result == 0)
                        result = file1.getName().compareToIgnoreCase(file2.getName());
                }
            } else {
                if (file1.isDirectory() && file2.isDirectory()) {
                    result = file2Date.compareTo(file1Date);
                    if (result == 0)
                        result = file2.getName().compareToIgnoreCase(file1.getName());
                } else if (!file1.isDirectory() && file2.isDirectory()) {
                    result = 1;
                } else if (file1.isDirectory() && !file2.isDirectory()) {
                    result = -1;
                } else {
                    result = file2Date.compareTo(file1Date);
                    if (result == 0)
                        result = file2.getName().compareToIgnoreCase(file1.getName());
                }
            }
            return result;
        }
    }

    public static class ComparaBySize implements Comparator<VFile> {

        private boolean mAscending;

        public ComparaBySize(boolean asc)
        {
            mAscending = asc;
        }

        @Override
        public int compare(VFile file1, VFile file2) {
            int result = 0;
            Long file1Size = (long) 0;
            Long file2Size = (long) 0;

            if (file1.isDirectory())
                file1Size = (long) -1;
            else
                file1Size = file1.length();

            if (file2.isDirectory())
                file2Size = (long) -1;
            else
                file2Size = file2.length();

            if (mAscending) {
                if (file1Size == -1 && file2Size == -1) {
                    result = file1.getName().compareToIgnoreCase(file2.getName());
                } else if (file1Size == -1 && file2Size != -1) {
                    result = -1;
                } else if (file1Size != -1 && file2Size == -1) {
                    result = 1;
                } else {
                    result = file1Size.compareTo(file2Size);
                    if (result == 0)
                        result = file1.getName().compareToIgnoreCase(file2.getName());
                }
            } else {
                if (file1Size == -1 && file2Size == -1) {
                    result = file2.getName().compareToIgnoreCase(file1.getName());
                } else if (file1Size == -1 && file2Size != -1) {
                    result = -1;
                } else if (file1Size != -1 && file2Size == -1) {
                    result = 1;
                } else {
                    result = file2Size.compareTo(file1Size);
                    if (result == 0)
                        result = file2.getName().compareToIgnoreCase(file1.getName());
                }
            }
            return result;
        }
    }

    public static class ComparaByName implements Comparator<VFile> {

        private boolean mAscending;

        public ComparaByName(boolean asc)
        {
            mAscending = asc;
        }

        @Override
        public int compare(VFile file1, VFile file2) {
            int result = 0;
            String s1 = file1.getName();
            String s2 = file2.getName();
            Locale locale = Locale.getDefault();
            Collator chinaCollator = Collator.getInstance(locale);
            AlphanumComparator alphanumComparator = new AlphanumComparator(chinaCollator);
            if (mAscending) {
                if (!file1.isDirectory() && file2.isDirectory()) {
                    result = 1;
                } else if (file1.isDirectory() && !file2.isDirectory()) {
                    result = -1;
                } else {
                    if (file2.getSortLetters().equals("#")) {
                        result = 1;
                    } else if (file1.getSortLetters().equals("#")) {
                        result = -1;
                    } else {
                        result = file1.getPinyin().compareTo(file2.getPinyin());
                    }
                }
            } else {
                if (!file1.isDirectory() && file2.isDirectory()) {
                    result = 1;
                } else if (file1.isDirectory() && !file2.isDirectory()) {
                    result = -1;
                } else {
                    if (file2.getSortLetters().equals("#")) {
                        result = 1;
                    } else if (file1.getSortLetters().equals("#")) {
                        result = -1;
                    } else {
                        result = file2.getPinyin().compareTo(file1.getPinyin());
                    }
                }
            }
            return result;
        }
    }

    public static class ComparaByFolderName implements Comparator<FolderElement> {

        private boolean mAscending;

        public ComparaByFolderName(boolean asc)
        {
            mAscending = asc;
        }

        @Override
        public int compare(FolderElement folderElement1, FolderElement folderElement2) {
            int result = 0;
            VFile file1 = folderElement1.getFile();
            VFile file2 = folderElement2.getFile();

            String s1 = file1.getAbsolutePath() + "/";
            String s2 = file2.getAbsolutePath() + "/";

            if (!file1.isDirectory() && file2.isDirectory()) {
                result = 1;
            } else if (file1.isDirectory() && !file2.isDirectory()) {
                result = -1;
            } else {
                result = s1.compareToIgnoreCase(s2);
            }

            if (!mAscending) {
                result = -result;
            }
            return result;
        }
    }

    public static class ComparaByPreviewName implements Comparator<UnZipPreviewData> {

        private boolean mAscending;

        public ComparaByPreviewName(boolean asc)
        {
            mAscending = asc;
        }

        @Override
        public int compare(UnZipPreviewData data1, UnZipPreviewData data2) {
            int result = 0;
            String s1 = data1.getPath();
            String s2 = data2.getPath();

            if (!data1.isFolder() && data2.isFolder()) {
                result = 1;
            } else if (data1.isFolder() && !data2.isFolder()) {
                result = -1;
            } else {
                result = s1.compareToIgnoreCase(s2);
            }

            if(!mAscending) {
                result = -result;
            }
            return result;
        }
    }

    public static Comparator<? super VFile> getComparator(int sortType) {

        Comparator<? super VFile> comparator = null;
        switch (sortType) {
            case SortType.SORT_TYPE_DOWN:
                comparator = new ComparaByType(true);
                break;
            case SortType.SORT_TYPE_UP:
                comparator = new ComparaByType(false);
                break;
            case SortType.SORT_DATE_DOWN:
                comparator = new ComparaByDate(true);
                break;
            case SortType.SORT_DATE_UP:
                comparator = new ComparaByDate(false);
                break;
            case SortType.SORT_SIZE_DOWN:
                comparator = new ComparaBySize(true);
                break;
            case SortType.SORT_SIZE_UP:
                comparator = new ComparaBySize(false);
                break;
            case SortType.SORT_NAME_DOWN:
                comparator = new ComparaByName(true);
                break;
            case SortType.SORT_NAME_UP:
                comparator = new ComparaByName(false);
                break;
            default:
                break;
        }

        return comparator;
    }

}
