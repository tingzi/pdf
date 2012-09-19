package com.zjt.pdf;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Database {
	
	private static DbOpenHelper mDbHelper;
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "pdf.db";
	
	public static void close() {
		if(mDbHelper != null) {
			mDbHelper.close();
			mDbHelper = null;
		}
	}
	
	public static SQLiteDatabase getDb(Context context) {
		DbOpenHelper helper = getDbOpenHelper(context);
		return helper.getWritableDatabase();
	}
	
	public static void writeMessageToDb(SQLiteDatabase db, String name, 
			String path) {
		ContentValues values = new ContentValues();
		values.put(PDF.name.name(), name);
		values.put(PDF.path.name(), path);
		db.insert(PDF.TABLE_NAME, null, values);
	}
	
	public static HashMap<String, String> loadMessageFromDb(SQLiteDatabase db) {
		Cursor cursor = db.query(PDF.TABLE_NAME, null, null, null, null, null, null);
		HashMap<String, String> result = new HashMap<String, String>();
		while(cursor.moveToNext()) {
			String name = cursor.getString(cursor.getColumnIndex(PDF.name.name()));
			String path = cursor.getString(cursor.getColumnIndex(PDF.path.name()));

			
			result.put(name, path);
		}
		cursor.close();
		return result;
	}
	
	public static int deleteAllMessageFromDb(SQLiteDatabase db) {
		return db.delete(PDF.TABLE_NAME, null, null);
	}
	
	public static int deleteMessageFromDb(SQLiteDatabase db, String path) {
		String[] where = new String[] {path};
		return db.delete(PDF.TABLE_NAME, PDF.path.name() + "=?", where);
	}
	
	private static DbOpenHelper getDbOpenHelper(Context context) {
		if(mDbHelper == null)
			mDbHelper = new DbOpenHelper(context, DB_NAME, DB_VERSION);
		
		return mDbHelper;
	}
	
	private static class DbOpenHelper extends SQLiteOpenHelper {

		public DbOpenHelper(Context context, String name, int version) {
			super(context, name, null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + PDF.TABLE_NAME + " (" +
					PDF._id.name() + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
					PDF.name.name() + " TEXT NOT NULL, " + 
					PDF.path.name() + " TEXT NOT NULL" +
					");"
					);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			dropTables(db);
			onCreate(db);
		}

		private void dropTables(SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS " + PDF.TABLE_NAME);
		}	
	}
	
	enum PDF {
		_id,
		name,
		path;
		
		static final String TABLE_NAME = "PDF";
	}
}
