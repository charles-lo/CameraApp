package com.charles.camera.ui;

import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

public final class PreviewGalleryBuilder extends UIComponentBuilder
{
	public PreviewGalleryBuilder()
	{
		super(PreviewGallery.class);
	}

	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new PreviewGallery(cameraActivity);
	}
}
