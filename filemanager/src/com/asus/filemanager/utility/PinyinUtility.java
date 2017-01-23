package com.asus.filemanager.utility;

import android.util.Log;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class PinyinUtility {

    private static char[] hanzi={'种', '单', '解', '曾', '盖', '缪', '朴', '繁', '仇', '仇'};

    private static String[] pinyin={"shan", "xie", "zha", "zeng", "ge", "miao", "piao", "po", "qiu", "yao"};

    private static String getSpeWord(char word){
        for(int i=0; i<hanzi.length; i++){
            if (hanzi[i] == word) {
                return pinyin[i];
            }
        }
        return null;
    }

    public static String getPinyin(String fileName) {
        HanyuPinyinOutputFormat hanyuPinyinOutputFormat = new HanyuPinyinOutputFormat();
        hanyuPinyinOutputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        hanyuPinyinOutputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.setLength(0);
        char[] charArray = fileName.toCharArray();
        for (int i = 0, len = charArray.length; i < len; i++){
            try {
                char c = charArray[i];
                if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")){
                    if (i == 0) {
                        String first = getSpeWord(c);
                        if (first != null) {
                            stringBuilder.append(first);
                        } else {
                            stringBuilder.append(PinyinHelper.toHanyuPinyinStringArray(charArray[i], hanyuPinyinOutputFormat)[0]);
                        }
                    } else {
                        stringBuilder.append(PinyinHelper.toHanyuPinyinStringArray(charArray[i], hanyuPinyinOutputFormat)[0]);
                    }
                } else {
                    stringBuilder.append(charArray[i]);
                }
            } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                badHanyuPinyinOutputFormatCombination.printStackTrace();
            }
        }
        Log.i("FileManager", stringBuilder.toString().toUpperCase());
        return stringBuilder.toString().toUpperCase();
    }

    public static String getFirstChar(String fileName) {
        String firstChar = getPinyin(fileName).substring(0, 1);
        if (!firstChar.matches("[A-Z]")) {
            firstChar = "#";
        }
        return firstChar;
    }

    public static int getCompareResult(String file1Name, String file2Name) {
        return getPinyin(file1Name).compareTo(getPinyin(file2Name));
    }
}
