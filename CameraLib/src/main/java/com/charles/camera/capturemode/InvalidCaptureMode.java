package com.charles.camera.capturemode;

import android.graphics.drawable.Drawable;

import com.charles.camera.InvalidMode;
import com.charles.camera.Settings;

class InvalidCaptureMode extends InvalidMode<CaptureMode> implements CaptureMode
{
	// Get custom settings.
	@Override
	public Settings getCustomSettings()
	{
		return null;
	}

	@Override
	public String getDisplayName()
	{
		return null;
	}

	@Override
	public Drawable getImage(ImageUsage usage)
	{
		return null;
	}
}
