package com.zjt.pdf;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.youmi.android.appoffers.YoumiOffersManager;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

public class MainActivity extends Activity {
	
	private static List<String> mData = new ArrayList<String>();
	
	private HashMap<String, String> mFileMap;

	private final static String TAG = "MainActivity";
	
	private SQLiteDatabase mDb;
	
	private int fileCount;
	
	private int allFileCount;
	
	private Dialog mDialog;
	
	private static TextView mText2;
	
	private static TextView mText5;
	
	private final static int PDF = 1;
	
	private final static int ALL = 2;
	
	private static ArrayAdapter<String> mAdapter;
	
	private static Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case ALL :
				mText2.setText(msg.obj + "");
				break;
			case PDF:
				mText5.setText(msg.arg1 + "");
				mData.add((String) msg.obj);
				mAdapter.notifyDataSetChanged();
				break;
			}
		}
    	
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupView();
        
        YoumiOffersManager.init(this, "您的应用的Appid", "您的应用的密码");
        
        mDb = Database.getDb(this);
        
        if(isFirst()){
        	Log.d(TAG , "first loader");        	
        	dealFirstTask("第一次启动，扫描所有文件");     	      	
        } else {
        	//load from database           
            mFileMap = Database.loadMessageFromDb(mDb);		       
            for(String name : mFileMap.keySet()) {
            	mData.add(name);
            }
        }
    }   
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		clearData();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "id:" + item.getItemId());
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Log.d(TAG, "scanning..................");
			clearData();
			dealFirstTask("重新扫描所有文件");
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private void clearData() {
		mData.clear();
		mFileMap.clear();
		Database.deleteAllMessageFromDb(mDb);
	}

	private void setupView() {
    	ListView listView = new ListView(this);
        setContentView(listView);
        mAdapter = new ArrayAdapter<String>(this, R.layout.listview_item, mData);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				startActivity(getPdfIntent(mFileMap.get(parent.getItemAtPosition(position))));
			}
     	
        });

        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(final AdapterView<?> parent, View view,
					final int position, long id) {
				new AlertDialog.Builder(MainActivity.this).setMessage("你确定要删除？")
				.setPositiveButton("取消", new OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						
					}
					
				}).setNegativeButton("确定",  new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String name = mFileMap.get(parent.getItemAtPosition(position));
						mFileMap.remove(parent.getItemAtPosition(position));
						new File(name).delete();	
						Database.deleteMessageFromDb(mDb, name);
						mData.remove(parent.getItemAtPosition(position));
						mAdapter.notifyDataSetChanged();
					}
					
				}).show();
				return false;
			}
        	
        });
    }
	
	private void dealFirstTask(String title) {
		//load from sdcard 	
    	new Thread() {
    		
    		public void run() {        			
    			mFileMap = new HashMap<String, String>();        			
            	getPdfFiles(Environment.getExternalStorageDirectory().getPath());
            	mDialog.dismiss();
            	
            	//start service  
            	Intent intent = new Intent();
                intent.setClass(MainActivity.this, FileService.class);
                startService(intent);
    		}
    	}.start(); 
    	
    	//show dialog        	
    	mDialog = new Dialog(this);
    	mDialog.setContentView(R.layout.custom_dialog);
    	mDialog.setTitle(title);
    	mDialog.setCancelable(false);
        mText2 = (TextView) mDialog.findViewById(R.id.text2);
        mText5 = (TextView) mDialog.findViewById(R.id.text5);
        mDialog.show();
	}

    private boolean isFirst() {
    	 SharedPreferences prefs = getSharedPreferences("fileloader", Context.MODE_PRIVATE);
    	 if(prefs.getBoolean("first.in", true)) {  
    	 	prefs.edit().putBoolean("first.in", false).commit();
    	 	return true;
    	 } else {
    		 return false;
    	 }
    }

	private Intent getPdfIntent(String param) {
    	Intent intent = new Intent("android.intent.action.VIEW");
		Uri uri = Uri.fromFile(new File(param));
		intent.setDataAndType(uri, "application/pdf");
		return intent;
    }
	
	private void getPdfFiles(String dir) {
		LinkedList<File> list=new LinkedList<File>();
		File fileDir = new File(dir);
        File file[]=fileDir.listFiles();
        for(int i=0;i<file.length;i++){
        	if(file[i].isDirectory())
        		list.add(file[i]);
        	else {
        		sendALLMessage();
        		if(file[i].getName().endsWith(".pdf")) {
        			dealFile(file[i]);
        			sendPDFMessage(file[i]);
        		}
        	}
        }
        File tmp;
        while(!list.isEmpty()){
        	tmp=list.removeFirst();
        	if(tmp.isDirectory()){
        		file=tmp.listFiles();
        		if(file==null)continue;
        		for(int i=0;i<file.length;i++){
        			if(file[i].isDirectory())
        				list.add(file[i]);
        			else {
        				sendALLMessage();
        				if(file[i].getName().endsWith(".pdf")) {
        					dealFile(file[i]);
        					sendPDFMessage(file[i]);
        				}
        			}
        		}
        	}else{
        		sendALLMessage();
        		if(tmp.getName().endsWith(".pdf")) {
        			dealFile(tmp);
        			sendPDFMessage(tmp);
        		}
        	}
        }
	}
	
	private void sendPDFMessage(File file) {
		Message msg = new Message();
    	msg.what = MainActivity.PDF;
    	msg.arg1 = ++fileCount;
    	msg.obj = file.getName();
    	Log.d(TAG, file.getName() + "======" + fileCount);
    	MainActivity.handler.sendMessage(msg);
	}
	
	private void sendALLMessage() {
		Message msg = new Message();
    	msg.what = MainActivity.ALL;
    	msg.obj = ++allFileCount;
    	MainActivity.handler.sendMessage(msg);
	}
	
	private void dealFile(File file) {
		mFileMap.put(file.getName(), file.getPath());
		Database.writeMessageToDb(mDb, file.getName(), file.getPath());
	}
}
