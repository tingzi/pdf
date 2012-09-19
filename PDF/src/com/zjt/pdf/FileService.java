package com.zjt.pdf;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;

public class FileService extends Service {
	
	private final static String TAG = "FileService";
	
	private List<SDCardListener > mFileObserverList = new ArrayList<SDCardListener>();
	
	private SQLiteDatabase mDb;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		
		mDb = Database.getDb(this);
		
		SDCardListener fileObserver = new SDCardListener(Environment.getExternalStorageDirectory().getPath());
		mFileObserverList.add(fileObserver);
        setFileObserver(Environment.getExternalStorageDirectory().getPath());
        
        for(SDCardListener listener : mFileObserverList) {
        	listener.startWatching();
        }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		for(SDCardListener listener : mFileObserverList) {
        	listener.stopWatching();
        }
	}	
	
	private List<File> setFileObserver(String dir) {
		ArrayList<File> fileList = new ArrayList<File>();
		LinkedList<File> list=new LinkedList<File>();
		File fileDir = new File(dir);
        File file[]=fileDir.listFiles();
        for(int i=0;i<file.length;i++){
        	if(file[i].isDirectory()) {
        		list.add(file[i]);
        		SDCardListener fileObserver = new SDCardListener(file[i].getAbsolutePath());
        		mFileObserverList.add(fileObserver);
        	}
        }
        File tmp;
        while(!list.isEmpty()){
        	tmp=list.removeFirst();
        	if(tmp.isDirectory()){
        		SDCardListener fileObserver = new SDCardListener(tmp.getAbsolutePath());
        		mFileObserverList.add(fileObserver);
        		file=tmp.listFiles();
        		if(file==null)continue;
        		for(int i=0;i<file.length;i++){
        			if(file[i].isDirectory()) {
        				list.add(file[i]);
        				SDCardListener fileObserver1 = new SDCardListener(file[i].getAbsolutePath());
                		mFileObserverList.add(fileObserver1);
        			}
        		}
        	}
        }
        return fileList;
	}
	
    class SDCardListener extends FileObserver {
    	
    	private String mAbsolutePath;
        
        public SDCardListener(String path, int mask) {
            super(path, mask);
        }

        public SDCardListener(String path) {
            super(path);
            mAbsolutePath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            final int action = event & FileObserver.ALL_EVENTS;
            switch (action) {
    /*        case FileObserver.ACCESS:
            	Log.d(TAG, "event: 文件或目录被访问, path: " + path);
                break;*/
                
            case FileObserver.DELETE:
            	if(path.endsWith(".pdf")) {
            		Log.d(TAG, "event: 文件或目录被删除, path: " + mAbsolutePath + path);
            		Database.deleteMessageFromDb(mDb, mAbsolutePath + path);
            	}
                break;
                
    /*        case FileObserver.OPEN:
            	Log.d(TAG, "event: 文件或目录被打开, path: " + path);
                break;
                
            case FileObserver.MODIFY:
            	Log.d(TAG, "event: 文件或目录被修改, path: " + path);
                break;*/
            case FileObserver.CREATE:
            	if(path.endsWith(".pdf")) {
            		Log.d(TAG, "event: 文件或目录被创建, path: " + mAbsolutePath + path);
            		Database.writeMessageToDb(mDb, path, mAbsolutePath + path);
            	}
                break;
            }
        }
    }
}
