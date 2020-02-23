package com.charles.camera.scene;

import com.charles.base.component.ComponentCreationPriority;
import com.charles.camera.CameraActivity;
import com.charles.camera.CameraComponent;
import com.charles.camera.UIComponentBuilder;

/**
 * Component builder for {@link SceneManager}.
 */
public final class SceneManagerBuilder extends UIComponentBuilder
{
	/**
	 * Initialize new SceneManagerBuilder instance.
	 */
	public SceneManagerBuilder()
	{
		super(ComponentCreationPriority.ON_DEMAND, SceneManagerImpl.class);
	}

	
	// Create component.
	@Override
	protected CameraComponent create(CameraActivity cameraActivity)
	{
		return new SceneManagerImpl(cameraActivity);
	}
}
