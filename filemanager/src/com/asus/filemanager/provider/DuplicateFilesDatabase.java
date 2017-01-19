package com.asus.filemanager.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DuplicateFilesDatabase {

    private static DatabaseHelper databaseHelper;
    private SQLiteDatabase db;
    private static final Object lock = new Object();

    public interface DuplicateFilesTable {
        String TABLE_NAME = "duplicatefiles";
        String ID = "`_id`";
        String PATH = "`path`";
        String MD5 = "`md5`";
        String LASTMODIFIED = "`lastmodified`";
        String SIZES = "`sizes`";
        String HAD_DUPLICATE = "`had_duplicate`";
    }

    public DuplicateFilesDatabase(Context context) {
        initialDatabaseHelper(context);
        db = databaseHelper.getWritableDatabase();
    }

    private void initialDatabaseHelper(Context context) {
        if (databaseHelper == null) {
            synchronized (lock) {
                if (databaseHelper == null) {
                    databaseHelper = new DatabaseHelper(context);
                }
            }
        }
    }

    public void close()
    {
        if(db!=null)
        {
            db.close();
        }
        if(databaseHelper!=null)
        {
            databaseHelper.close();
            databaseHelper = null;
        }
    }

    public void insertVFiles(List<VFile> vfileList)
    {
        synchronized (lock) {
            db.beginTransaction();
            try {
                String sql = "INSERT INTO "+DuplicateFilesTable.TABLE_NAME+" VALUES (?,?,?,?,?,?);";
                SQLiteStatement statement = db.compileStatement(sql);
                for(int i = 0;i<vfileList.size();i++)
                {
                    VFile vfile = vfileList.get(i);
                    statement.bindString(2, FileUtility.getCanonicalPathNoException(vfile));
                    statement.bindString(3, vfile.getMD5());
                    statement.bindLong(4, vfile.lastModified());
                    statement.bindLong(5, vfile.length());
                    statement.execute();
                }
                db.setTransactionSuccessful();
            }finally {
                db.endTransaction();
            }
        }
    }

    public void dropTableAndInsertVFiles(List<VFile> vfileList)
    {
        synchronized (lock) {
            //recreate table
            databaseHelper.dropTable(db);
            databaseHelper.createTable(db);

            db.beginTransaction();
            try {
                String sql = "INSERT INTO "+DuplicateFilesTable.TABLE_NAME+" VALUES (?,?,?,?,?,?);";
                SQLiteStatement statement = db.compileStatement(sql);
                for(int i = 0;i<vfileList.size();i++)
                {
                    VFile vfile = vfileList.get(i);
                    statement.bindString(2, FileUtility.getCanonicalPathNoException(vfile));
                    statement.bindString(3, vfile.getMD5());
                    statement.bindLong(4, vfile.lastModified());
                    statement.bindLong(5, vfile.length());
                    statement.execute();
                }
                db.setTransactionSuccessful();
            }finally {
                db.endTransaction();
            }
        }
    }


    public void insert(String path, String md5, long lastmodified, long sizes)
    {
        synchronized (lock) {
            ContentValues values = new ContentValues();
            values.put(DuplicateFilesTable.PATH, path);
            values.put(DuplicateFilesTable.MD5, md5);
            values.put(DuplicateFilesTable.LASTMODIFIED, lastmodified);
            values.put(DuplicateFilesTable.SIZES, sizes);
            db.insert(DuplicateFilesTable.TABLE_NAME, null, values);
        }
    }

    public HashMap<String, String> getPathToMD5Map()
    {
        return getPathToMD5Map(-1);
    }

    /**
     * get path to md5 HashMap from file sizes
     * @param sizes file.length(), if sizes==-1 query all
     * @return key(file path), value(md5)
     */
    public HashMap<String, String> getPathToMD5Map(long sizes)
    {
        synchronized (lock) {
            HashMap<String, String> pathToMD5Map = new HashMap<>();
            ArrayList<Integer> conflictList = new ArrayList<>();
            String ion = (sizes==-1)?null:DuplicateFilesTable.SIZES+"='"+sizes+"'";
            Cursor cursor = db.query(DuplicateFilesTable.TABLE_NAME, new String[]{DuplicateFilesTable.ID,DuplicateFilesTable.PATH,DuplicateFilesTable.MD5}, ion,
                    null, null, null, DuplicateFilesTable.LASTMODIFIED+" DESC");
            cursor.moveToFirst();
            for(int i=0;i<cursor.getCount() ; i++) {
                String path = cursor.getString(1);
                String md5 = cursor.getString(2);

                if(pathToMD5Map.containsKey(path))
                    conflictList.add(cursor.getInt(0));
                else
                    pathToMD5Map.put(path, md5);
                cursor.moveToNext();
            }
            cursor.close();

            //delete conflict
            delete(conflictList);

            return pathToMD5Map;
        }
    }

    public HashMap<String, List<VFile>> getDuplicateFileMap(String... rootPaths)
    {
        synchronized (lock) {
            HashMap<String, List<VFile>> duplicateFileMap = new HashMap<>();
            List<Integer> notExistFileIdArray = new ArrayList<>();
            String selection = DuplicateFilesTable.HAD_DUPLICATE+" = '1'";
            if (rootPaths != null) {
                selection +=" AND (";
                for (int i = 0; i < rootPaths.length; i++) {
                    if (i != 0)
                        selection += " OR ";
                    //avoid SQLite injection
                    rootPaths[i] = rootPaths[i].replaceAll("'","''");
                    selection = selection + DuplicateFilesTable.PATH + " like'" + rootPaths[i] + "%'";
                }
                selection +=")";
            }

            Cursor cursor = db.query(DuplicateFilesTable.TABLE_NAME, new String[]{DuplicateFilesTable.PATH, DuplicateFilesTable.MD5,DuplicateFilesTable.ID}, selection,
                    null, null, null, DuplicateFilesTable.LASTMODIFIED + " DESC");
            cursor.moveToFirst();
            for(int i=0;i<cursor.getCount() ; i++) {
                String path = cursor.getString(0);
                String md5 = cursor.getString(1);
                VFile vFile = new LocalVFile(path);
                if(vFile.exists()) {
                    if (duplicateFileMap.containsKey(md5)) {
                        duplicateFileMap.get(md5).add(vFile);
                    } else {
                        ArrayList<VFile> tmpList = new ArrayList<>();
                        tmpList.add(vFile);
                        duplicateFileMap.put(md5, tmpList);
                    }
                }else{
                    notExistFileIdArray.add(cursor.getInt(2));
                }
                cursor.moveToNext();
            }
            cursor.close();

            //delete duplicateFilesDatabase not exist file
            delete(notExistFileIdArray);

            return duplicateFileMap;
        }
    }

    public long getDuplicateFilesTotalSizes(String... rootPaths)
    {
        synchronized (lock) {
            String sql = "SELECT sum(" + DuplicateFilesTable.SIZES + ") FROM " + DuplicateFilesTable.TABLE_NAME + " where" + DuplicateFilesTable.HAD_DUPLICATE + "='1'";
            if (rootPaths != null && rootPaths.length!=0) {
                sql += " AND (";
                for (int i = 0; i < rootPaths.length; i++) {
                    if (i != 0)
                        sql += " OR ";
                    //avoid SQLite injection
                    rootPaths[i] = rootPaths[i].replaceAll("'","''");
                    sql = sql + DuplicateFilesTable.PATH + " like '" + rootPaths[i] + "%'";
                }
                sql += ")";
            }
            sql += ";";
            long total = 0;
            Cursor c = db.rawQuery(sql, null);
            if (c.moveToFirst()) {
                total = c.getLong(0);
            }
            c.close();

            return total;
        }
    }

    public void updateHadDuplicate(List<List<VFile>> vfileList)
    {
        synchronized (lock) {
            db.beginTransaction();
            try {
                String sql = "UPDATE "+DuplicateFilesTable.TABLE_NAME+" set "+DuplicateFilesTable.HAD_DUPLICATE+"='1' where "+DuplicateFilesTable.PATH+"=?;";
                SQLiteStatement statement = db.compileStatement(sql);
                for(int i = 0;i<vfileList.size();i++)
                {
                    for(int j=0;j<vfileList.get(i).size();j++) {
                        statement.bindString(1, FileUtility.getCanonicalPathNoException(vfileList.get(i).get(j)));
                        statement.execute();
                    }
                }
                db.setTransactionSuccessful();
            }finally {
                db.endTransaction();
            }
        }
    }

    public void updateHadDuplicate(List<VFile> vfileList,boolean hadDuplicate)
    {
        synchronized (lock) {
            if(vfileList.size()==0)
                return;

            db.beginTransaction();
            try {
                int flag = hadDuplicate?1:0;
                String sql = "UPDATE "+DuplicateFilesTable.TABLE_NAME+" set "+DuplicateFilesTable.HAD_DUPLICATE+"='"+flag+"' where "+DuplicateFilesTable.PATH+"=?;";
                SQLiteStatement statement = db.compileStatement(sql);
                for(int i = 0;i<vfileList.size();i++)
                {
                    statement.bindString(1, FileUtility.getCanonicalPathNoException(vfileList.get(i)));
                    statement.execute();
                }
                db.setTransactionSuccessful();
            }finally {
                db.endTransaction();
            }
        }
    }

    /**
     * for delete conflict path
     * @param deleteIdList
     */
    private void delete(List<Integer> deleteIdList)
    {
        if(deleteIdList.size()==0)
            return;

        db.beginTransaction();
        try {
            String sql = "DELETE FROM "+DuplicateFilesTable.TABLE_NAME+" WHERE " + DuplicateFilesTable.ID + " = ?";
            SQLiteStatement statement = db.compileStatement(sql);
            for(int i =0;i<deleteIdList.size();i++)
            {
                statement.bindLong(1, deleteIdList.get(i));
                statement.execute();
            }
            db.setTransactionSuccessful();
        }finally {
            db.endTransaction();
        }

    }

    public void delete(VFile[] deleteFilesList)
    {
        if(deleteFilesList.length==0)
            return;

        db.beginTransaction();
        try {
            String sql = "DELETE FROM "+DuplicateFilesTable.TABLE_NAME+" WHERE " + DuplicateFilesTable.PATH + " = ?";
            SQLiteStatement statement = db.compileStatement(sql);
            for(int i =0;i<deleteFilesList.length;i++)
            {
                statement.bindString(1, FileUtility.getCanonicalPathNoException(deleteFilesList[i]));
                statement.execute();
            }
            db.setTransactionSuccessful();
        }finally {
            db.endTransaction();
        }

    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        private final static String DATABASE_NAME = "duplicate_files_db";
        private final static int DATABASE_VERSION = 2;

        private DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            createTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
            dropTable(db);
            createTable(db);
        }

        public void createTable(SQLiteDatabase db)
        {
            String sql = "CREATE TABLE " + DuplicateFilesTable.TABLE_NAME + " ("
                    + DuplicateFilesTable.ID+ " INTEGER PRIMARY KEY autoincrement,"
                    + DuplicateFilesTable.PATH + " TEXT not null,"
                    + DuplicateFilesTable.MD5 + " TEXT not null,"
                    + DuplicateFilesTable.LASTMODIFIED + " INTEGER not null,"
                    + DuplicateFilesTable.SIZES + " INTEGER not null,"
                    + DuplicateFilesTable.HAD_DUPLICATE + " INTEGER DEFAULT 0);";
            db.execSQL(sql);
        }

        public void dropTable(SQLiteDatabase db)
        {
            db.execSQL("DROP TABLE IF EXISTS " + DuplicateFilesTable.TABLE_NAME);
        }
    }
}
