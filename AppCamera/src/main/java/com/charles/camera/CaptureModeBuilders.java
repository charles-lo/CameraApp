package com.charles.camera;

import com.charles.camera.capturemode.CaptureModeBuilder;
import com.charles.camera.capturemode.PhotoCaptureModeBuilder;
import com.charles.camera.capturemode.VideoCaptureModeBuilder;
import com.charles.camera.slowmotion.SlowMotionCaptureModeBuilder;
import com.charles.camera.timelapse.TimelapseCaptureModeBuilder;

final class CaptureModeBuilders
{
	public static final CaptureModeBuilder[] BUILDERS = new CaptureModeBuilder[]{
		new PhotoCaptureModeBuilder(),
		new VideoCaptureModeBuilder(),
		new SlowMotionCaptureModeBuilder(),
		new TimelapseCaptureModeBuilder(),
	};
}
