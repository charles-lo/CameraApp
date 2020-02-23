package com.charles.camera.scene;

import com.charles.camera.CameraActivity;

/**
 * Builder for face beauty (photo) scene.
 */
public final class PhotoFaceBeautySceneBuilder implements SceneBuilder
{
	// Create scene.
	@Override
	public Scene createScene(CameraActivity cameraActivity)
	{
		return new PhotoFaceBeautyScene(cameraActivity);
	}
}
