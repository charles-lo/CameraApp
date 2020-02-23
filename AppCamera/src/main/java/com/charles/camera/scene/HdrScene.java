package com.charles.camera.scene;

import android.graphics.drawable.Drawable;
import android.hardware.camera2.CaptureRequest;

import com.charles.camera.CameraActivity;
import com.charles.camera.R;

final class HdrScene extends PhotoScene
{
	// Constructor.
	HdrScene(CameraActivity cameraActivity)
	{
		super(cameraActivity, "HDR", CaptureRequest.CONTROL_SCENE_MODE_HDR, FLAG_DISABLE_FLASH);
	}
	
	
	// Get string for display name.
	@Override
	public String getDisplayName()
	{
		return this.getCameraActivity().getString(R.string.scene_hdr);
	}


	// Get related image.
	@Override
	public Drawable getImage(ImageUsage usage)
	{
		switch(usage)
		{
			case OPTIONS_PANEL_ICON:
				return this.getCameraActivity().getDrawable(R.drawable.options_panel_icon_hdr);
			default:
				return null;
		}
	}
}
