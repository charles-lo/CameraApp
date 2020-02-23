package com.charles.camera;

import com.charles.camera.scene.HdrSceneBuilder;
import com.charles.camera.scene.PhotoFaceBeautySceneBuilder;
import com.charles.camera.scene.SceneBuilder;

class SceneBuilders
{
	static final SceneBuilder[] BUILDERS = new SceneBuilder[]{
		new PhotoFaceBeautySceneBuilder(),
		new HdrSceneBuilder(),
	};
}
