package com.example.simpleprototype;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.Handler;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class webCamMode extends MainActivity {

	public static final String TAG = "webcamMode";
	private Camera mCamera;
	private CameraPreview mPreview;
	private TextView tv;
	private byte[] photo = {
	};
	private InputStream is;
	//UsbSerialDriver usb = UsbSerialProber.acquire(mUsbManager);
	String str = "";
	public static int counter = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.textView1);
		mCamera = getCameraInstance();
		
		//portList = CommPortIdentifier.getPortIdentifiers();
		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		//FrameLayout preview = (FrameLayout)findViewById(R.id.camera_preview);
		// preview.addView(mPreview);
		
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				//readingdata(msg);
				byte[] data = (byte[]) msg.obj;
				
				switch(msg.what){
				case ArduinoProtocol.UPDATE_DIGITAL_STATE:
					break;
				case ArduinoProtocol.UPDATE_ANALOG_STATE :
					//try{
					final int value = composeInt(data[1], data[2]);
					
					Log.v(TAG, "Analog id, value = " + data[0]+ "," + value);
					for(int j = 0; j < value; j++){
						
					}
					tv.setText(String.valueOf(photo) + "+" + counter++);// shows 10bits data in dec
					//}catch(IOException e){}
					break;
				default:
					break;
				}
				
			}
		};
		
	}
	private int composeInt(byte hi, byte lo){
		return (((hi & 0xff) << 8) + (lo & 0xff));
	}
	@Override
	public void onResume() {
		super.onResume();

	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResult() {
		if (mArduinoProtocol != null) {
			mArduinoProtocol.digitalWrite(0, true);
		}
	}
	public byte[] readingdata(Message msg){
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte[] rdata = (byte[])msg.obj;
		int value;
		
		while(true){
			try{
				int len = is.read(rdata);
				value = composeInt(rdata[1], rdata[2]);
				if(len < 0){
					break;
				}
			} catch(IOException e){
				break;
			}
			bout.write(rdata, 0, rdata.length);
			tv.setText(String.valueOf(value) + "+" + bout);
		}
		return rdata;
	}
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	/** A basic Camera preview class */
	public class CameraPreview extends SurfaceView implements
			SurfaceHolder.Callback {
		private SurfaceHolder mHolder;
		private Camera mCamera;

		public CameraPreview(Context context, Camera camera) {
			super(context);
			mCamera = camera;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		public void surfaceCreated(SurfaceHolder holder) {
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
				Log.d(TAG, "Error setting camera preview: " + e.getMessage());
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// empty. Take care of releasing the Camera preview in your
			// activity.
			if(mCamera != null){
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int w,
				int h) {
			// If your preview can change or rotate, take care of those events
			// here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (mHolder.getSurface() == null) {
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non-existent preview
			}

			// set preview size and make any resize, rotate or
			// reformatting changes here

			// start preview with new settings
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();

			} catch (Exception e) {
				Log.d(TAG, "Error starting camera preview: " + e.getMessage());
			}
		}
	}
}
