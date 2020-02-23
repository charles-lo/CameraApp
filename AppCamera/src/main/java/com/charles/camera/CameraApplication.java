package com.charles.camera;

import com.charles.base.Log;
import com.charles.base.ThreadMonitor;

import android.app.Application;

/**
 * Camera application.
 */
public final class CameraApplication extends Application
{
	/**
	 * Debug flag.
	 */
	public static final boolean DEBUG = BuildConfig.DEBUG;
	
	
	// Constants
	private static final String TAG = "CameraApplication";
	
	
	// Called when creating application.
	@Override
	public void onCreate()
	{
		Log.v(TAG, "onCreate()");
		
		// call super
		super.onCreate();
		
		// initialize thread monitor
		if(DEBUG)
			ThreadMonitor.prepare();
	}
}
