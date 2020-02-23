package com.charles.camera.io;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraThread;
import com.charles.camera.CameraThreadComponent;
import com.charles.camera.CameraThreadComponentBuilder;

public class FileManagerBuilder extends CameraThreadComponentBuilder
{
	public FileManagerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, FileManagerImpl.class);
	}

	@Override
	protected CameraThreadComponent create(CameraThread cameraThread)
	{
		return new FileManagerImpl(cameraThread);
	}
}
