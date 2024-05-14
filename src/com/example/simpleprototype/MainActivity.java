package com.example.simpleprototype;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "AccossoryMode";
	private static final String ACTION_USB_PERMISSION = "aoabook.sample.aoabase.action.USB_PERMISSION";
	
	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	private ParcelFileDescriptor mFileDescriptor;
	private OutputStream mOutputStream;
	private FileInputStream mInputStream;
	
	private Button mButton;
	
	protected ArduinoProtocol mArduinoProtocol;
	protected Handler mHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		mUsbManager = UsbManager.getInstance(this);
		
		//mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver,filter);
		
		if(getLastNonConfigurationInstance() != null){
			mAccessory = (UsbAccessory)getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
		
		
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if(mFileDescriptor != null){
			return;
		}
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		mAccessory = (accessories == null ? null : accessories[0]);
		if(mAccessory != null){
			if(mUsbManager.hasPermission(mAccessory)){
				openAccessory(mAccessory);
			}else{
				synchronized(mUsbReceiver){
					if(!mPermissionRequestPending){
						mUsbManager.requestPermission(mAccessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		}else{
			Log.d(TAG, "mAccessory is null");
		}
	}
	@Override
	public void onPause(){
		super.onPause();
		closeAccessory();
	}
	
	@Override
	public void onDestroy(){
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}
	
	//open accessory
	private void openAccessory(UsbAccessory accessory){
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if(mFileDescriptor != null){
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mOutputStream = new FileOutputStream(fd);
			mInputStream = new FileInputStream(fd);
			
			mArduinoProtocol = new ArduinoProtocol(mOutputStream, mInputStream, mHandler);
		}else
			Log.d(TAG, "failed to open the accessory");
	}
	
	//close accessory
	private void closeAccessory(){
		try{
			if(mFileDescriptor != null){
				mFileDescriptor.close();
			}
			if(mArduinoProtocol != null){
				mArduinoProtocol.requestStop();
			}
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			mInputStream = null;
			mOutputStream = null;
			mFileDescriptor = null;
			mAccessory = null;
		}
	}
	
	//USB接続状態変化のインテントを受け取るレシーバー
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			if(ACTION_USB_PERMISSION.equals(action)){
				//if user clicked ok or cancel
				synchronized(this){
					//get usb accessory instance
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					//UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					//if user permits access
					if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
						openAccessory(accessory);
					}else{
						Log.d(TAG, "permission denied for accessory " + accessory);
						
					}
					mPermissionRequestPending = false;
				}
			} else if(UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)){
				//if accessory detached
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				//UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				
				if(accessory != null && accessory.equals(mAccessory)){
					closeAccessory();
				}
			}
		}
	};
	class MyRunnable implements Runnable{
		@Override
		public void run(){
			byte[] buffer = new byte[1];
			try{
				mInputStream.read(buffer);
				if(buffer[0] == 'b'){
					MainActivity.this.runOnUiThread(new Runnable(){
						@Override
						public void run(){
							mButton.setEnabled(true);
							Toast.makeText(MainActivity.this, "Message from Arduino", Toast.LENGTH_SHORT).show();
						}
					});
				}
			} catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	public void onResult() {
		// TODO Auto-generated method stub
		
	}

}
