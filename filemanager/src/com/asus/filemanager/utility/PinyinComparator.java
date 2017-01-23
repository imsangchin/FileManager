package com.asus.filemanager.utility;

import java.util.Comparator;

public class PinyinComparator implements Comparator<VFile> {
    CharacterParser characterParser = CharacterParser.getInstance();
    boolean asc;

    public PinyinComparator(boolean asc) {
        this.asc = asc;
    }

    @Override
    public int compare(VFile o1, VFile o2) {
        if (o2.getSortLetters().equals("#")) {
            return 1;
        } else if (o1.getSortLetters().equals("#")) {
            return -1;
        } else {
            if (asc) {
                return o1.getSortLetters().compareTo(o2.getSortLetters());
            } else {
                return o2.getSortLetters().compareTo(o1.getSortLetters());
            }
        }
    }
}
