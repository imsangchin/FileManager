package com.asus.filemanager.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.asus.filemanager.functionaldirectory.FunctionalDirectoryUtility;
import com.asus.filemanager.utility.FileUtility;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AllFilesDatabase {

    private static DatabaseHelper databaseHelper;
    private SQLiteDatabase db;
    private static final Object lock = new Object();

    public interface AllFilesTable {
        String TABLE_NAME = "allfiles";
        String ID = "`_id`";
        String PATH = "`path`";
        String PARENT_PATH = "`parent_path`";
        String TYPE = "`type`";
        String PERCENTAGE = "`percentage`";
        String SIZES = "`sizes`";
        String GROUP_ID = "`group_id`";
    }

    public interface GroupTable {
        String TABLE_NAME = "filesgroup";
        String ID = "`_gid`";
        String ROOT_PATH = "`root_path`";
    }

    public interface RecordTable {
        String TABLE_NAME = "record";
        String ID = "`_rid`";
        String UPDATE_TIME = "`update_time`";
    }

    public AllFilesDatabase(Context context) {
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
                String sql = "INSERT INTO "+AllFilesTable.TABLE_NAME+" VALUES (?,?,?,?,?,?,?);";
                SQLiteStatement statement = db.compileStatement(sql);

                for(int i = 0;i<vfileList.size();i++)
                {
                        VFile vfile = vfileList.get(i);
                        statement.bindString(2, FileUtility.getCanonicalPathNoException(vfile));
                        statement.bindString(3, FileUtility.getCanonicalPathNoException(vfile.getParentFile()));
                        if (vfile.isDirectory())
                            statement.bindString(4, "1");
                        else
                            statement.bindString(4, "0");
                        statement.bindDouble(5, vfile.getInStoragePercentage());
                        statement.bindLong(6, vfile.length());
                        statement.bindLong(7, vfile.getGroupId());
                        statement.execute();
                }
                db.setTransactionSuccessful();
            }finally {
                db.endTransaction();
            }
        }
    }

    /**
     *
     * @param path canonicalPath
     * @return null if not exist
     */
    public VFile getFile(String path)
    {
        synchronized (lock) {
            VFile file = new VFile(path);
            //avoid SQLite injection
            path = path.replaceAll("'","''");
            String selection = AllFilesTable.PATH+" ='"+path+"'";
            Cursor cursor = db.query(AllFilesTable.TABLE_NAME, new String[]{AllFilesTable.PERCENTAGE,AllFilesTable.SIZES,AllFilesTable.GROUP_ID}, selection,
                    null, null, null, null);
            if(cursor!=null){
                cursor.moveToFirst();
                if(cursor.getCount()>0)
                {
                    file.setInStoragePercentage(cursor.getFloat(0));
                    file.setLength(cursor.getLong(1));
                    file.setGroupId(cursor.getInt(2));
                }
                cursor.close();
            }
            return file;
        }
    }

    /**
     * get file list in parent folder
     * @param parentPath <br>ex:/sdcard/1.png parentPath:/sdcard
     * @return
     */
    public List<VFile> getFileList(String parentPath)
    {
        synchronized (lock) {
            List<VFile> fileList = new ArrayList<>();
            //avoid SQLite injection
            parentPath = parentPath.replaceAll("'","''");
            String selection = AllFilesTable.PARENT_PATH+" ='"+parentPath+"'";
            Cursor cursor = db.query(AllFilesTable.TABLE_NAME, new String[]{AllFilesTable.PATH,AllFilesTable.PERCENTAGE,AllFilesTable.SIZES}, selection,
                    null, null, null, AllFilesTable.SIZES+" DESC");
            if(cursor!=null){
                cursor.moveToFirst();
                for(int i=0;i<cursor.getCount() ; i++) {
                    LocalVFile file = new LocalVFile(cursor.getString(0));
                    if(file.exists())
                    {
                        file.setInStoragePercentage(cursor.getFloat(1));
                        file.setLength(cursor.getLong(2));
                        fileList.add(file);
                    }
                    cursor.moveToNext();
                }
                cursor.close();
            }
            return fileList;
        }
    }

    /**
     * get same sizes vfile list in HashMap
     * @return key(sizes) value(vfile list *length>1)
     */
    public HashMap<Long, List<VFile>> getSameSizesFileMap(String... rootPaths)
    {
        synchronized (lock) {
            HashMap<Long, List<VFile>> sameSizesFileMap = new HashMap<>();
            boolean hadSameFile = false;
            long lastSizes = 0;
            String selection = AllFilesTable.TYPE+" ='0'";

            if(rootPaths!=null && rootPaths.length!=0) {
                selection = selection+" AND (";
                for (int i = 0; i < rootPaths.length; i++) {
                    if(i!=0)
                        selection += " OR ";
                    selection = selection + "("+AllFilesTable.GROUP_ID + " ='" + getGroupId(rootPaths[i]) + "'"
                    +" AND "+FunctionalDirectoryUtility.getInstance().getExcludeFunctionalDirectorySelection(AllFilesTable.PATH,rootPaths[i])
                    +")";
                }
                selection = selection+")";
            }

            Cursor cursor = db.query(AllFilesTable.TABLE_NAME, new String[]{AllFilesTable.PATH,AllFilesTable.SIZES},selection,
                    null, null, null, AllFilesTable.SIZES+" DESC");
            if(cursor!=null){
                cursor.moveToFirst();
                for(int i=0;i<cursor.getCount() ; i++) {
                    VFile file = new LocalVFile(cursor.getString(0));
                    if(file.exists())
                    {
                        long currentSizes = cursor.getLong(1);
                        if(currentSizes!=lastSizes)
                        {
                            if(!hadSameFile)
                            {
                                sameSizesFileMap.remove(lastSizes);
                            }
                            hadSameFile = false;
                        }

                        if(currentSizes==0)
                            break;

                        //filemap exist same sizes file put it to file map
                        if(sameSizesFileMap.containsKey(currentSizes))
                        {
                            sameSizesFileMap.get(currentSizes).add(file);
                            hadSameFile = true;
                        //filemap not exist same sizes file, create list and put it to file map
                        }else{
                            List<VFile> list = new ArrayList<VFile>();
                            list.add(file);
                            sameSizesFileMap.put(currentSizes, list);
                        }
                        lastSizes = currentSizes;
                    }
                    cursor.moveToNext();
                }
                cursor.close();
            }


            return sameSizesFileMap;
        }
    }

    public void update(VFile vfile)
    {
        synchronized (lock) {
            ContentValues values = new ContentValues();
            values.put(AllFilesTable.PERCENTAGE, vfile.getInStoragePercentage());
            values.put(AllFilesTable.SIZES, vfile.length());
            String path = FileUtility.getCanonicalPathNoException(vfile).replaceAll("'","''");
            db.update(AllFilesTable.TABLE_NAME, values, AllFilesTable.PATH + "='" + path + "'", null);
        }
    }

    public void delete(VFile[] deleteVFileList)
    {
        synchronized (lock) {

            db.beginTransaction();
            try {

                String sql = "DELETE FROM "+AllFilesTable.TABLE_NAME+" WHERE " + AllFilesTable.PATH + " = ?";
                SQLiteStatement statement = db.compileStatement(sql);
                for(int i = 0;i<deleteVFileList.length;i++)
                {
                    VFile vfile = deleteVFileList[i];

                    statement.bindString(1, FileUtility.getCanonicalPathNoException(vfile));
                    statement.execute();
                }
                db.setTransactionSuccessful();
            }finally {
                db.endTransaction();
            }

        }
    }

    public void delete(int groupId)
    {
        synchronized (lock) {

            db.delete(AllFilesTable.TABLE_NAME, AllFilesTable.GROUP_ID + " = " + groupId, null);
        }
    }

    public void deleteAll()
    {
        synchronized (lock) {

            db.delete(AllFilesTable.TABLE_NAME, null, null);
            db.delete(GroupTable.TABLE_NAME, null, null);
        }
    }

    public int getGroupId(String rootPath)
    {
        synchronized (lock) {
            //avoid SQLite injection
            rootPath = rootPath.replaceAll("'","''");
            Cursor cursor = db.query(GroupTable.TABLE_NAME, null, GroupTable.ROOT_PATH+" = '"+rootPath+"'",null, null, null,null);
            if(cursor!=null){
                cursor.moveToFirst();
                //id exist
                if(cursor.getCount()>0)
                {
                    int id = cursor.getInt(0);
                    cursor.close();
                    return id;
                }
            }
            //id not exist, insert get new id
            ContentValues contentValues = new ContentValues();
            contentValues.put(GroupTable.ROOT_PATH, rootPath);
            return (int) db.insert(GroupTable.TABLE_NAME, null, contentValues);
        }
    }

    public String getGroupRootPath(int groupId)
    {
        synchronized (lock) {

            String rootPath = null;
            Cursor cursor = db.query(GroupTable.TABLE_NAME, null, GroupTable.ID+" = '"+groupId+"'",null, null, null,null);
            if(cursor!=null){
                cursor.moveToFirst();
                //id exist
                if(cursor.getCount()>0)
                {
                    rootPath = cursor.getString(1);
                    cursor.close();

                }
            }

            return rootPath;
        }
    }

    public List<String> getGroupRootPaths(boolean onlyGetRootExist)
    {
        synchronized (lock) {
            List<String> result = new ArrayList<String>();
            Cursor cursor = db.query(GroupTable.TABLE_NAME, new String[]{GroupTable.ROOT_PATH}, null,null, null, null,null);
            if(cursor!=null){
                cursor.moveToFirst();

                //id exist
                for(int i = 0;i<cursor.getCount();i++) {
                    String rootPath = cursor.getString(0);
                    if(onlyGetRootExist) {
                        if (new File(rootPath).exists())
                            result.add(rootPath);
                    }else
                    {
                        result.add(rootPath);
                    }
                    cursor.moveToNext();
                }
                cursor.close();
            }

            return result;
        }
    }

    /**
     *
     * @return  if not exist return -1
     */
    public long getUpdateTimeMillis()
    {
        synchronized (lock) {
            //get id
            Cursor cursor = db.query(RecordTable.TABLE_NAME, null, RecordTable.ID+" = '0'",null, null, null,null);
            if(cursor!=null){
                cursor.moveToFirst();
                //id exist
                if(cursor.getCount()>0)
                {
                    long id = cursor.getLong(1);
                    cursor.close();
                    return id;
                }
            }
            return -1;
        }
    }

    public void setUpdateTime(long timeMillis){
        synchronized (lock) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(RecordTable.ID,0);
            contentValues.put(RecordTable.UPDATE_TIME,timeMillis);
            db.insertWithOnConflict(RecordTable.TABLE_NAME, RecordTable.ID, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        private final static String DATABASE_NAME = "all_files_db";
        private final static int DATABASE_VERSION = 1;

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
        }

        public void createTable(SQLiteDatabase db)
        {
            String sql = "CREATE TABLE " + AllFilesTable.TABLE_NAME + " ("
                    + AllFilesTable.ID+ " INTEGER PRIMARY KEY autoincrement,"
                    + AllFilesTable.PATH + " TEXT not null,"
                    + AllFilesTable.PARENT_PATH + " TEXT not null,"
                    + AllFilesTable.TYPE + " INTEGER not null,"
                    + AllFilesTable.PERCENTAGE + " REAL not null,"
                    + AllFilesTable.SIZES + " INTEGER not null,"
                    + AllFilesTable.GROUP_ID + " INTEGER not null);";
            db.execSQL(sql);

            sql = "CREATE TABLE " + GroupTable.TABLE_NAME + " ("
                    + GroupTable.ID+ " INTEGER PRIMARY KEY autoincrement,"
                    + GroupTable.ROOT_PATH + " TEXT not null);";

            db.execSQL(sql);

            sql = "CREATE TABLE " + RecordTable.TABLE_NAME + " ("
                    + RecordTable.ID+ " INTEGER PRIMARY KEY autoincrement,"
                    + RecordTable.UPDATE_TIME + " INTEGER not null);";

            db.execSQL(sql);
        }

        public void dropTable(SQLiteDatabase db)
        {
            db.execSQL("DROP TABLE IF EXISTS "+AllFilesTable.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS "+GroupTable.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS "+RecordTable.TABLE_NAME);
        }
    }
}
