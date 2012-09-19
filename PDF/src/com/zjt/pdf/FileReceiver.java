package com.zjt.pdf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FileReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if(Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			Intent serviceIntent = new Intent();
			serviceIntent.setClass(context, FileService.class);
			context.startService(serviceIntent);
		}
	}

}
