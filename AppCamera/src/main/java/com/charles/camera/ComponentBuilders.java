package com.charles.camera;

import com.charles.base.component.ComponentBuilder;
import com.charles.camera.capturemode.CaptureModeManagerBuilder;
import com.charles.camera.location.LocationManagerBuilder;
import com.charles.camera.media.AudioManagerBuilder;
import com.charles.camera.scene.SceneManagerBuilder;
import com.charles.camera.slowmotion.SlowMotionControllerBuilder;
import com.charles.camera.slowmotion.SlowMotionUIBuilder;
import com.charles.camera.timelapse.TimelapseControllerBuilder;
import com.charles.camera.timelapse.TimelapseUIBuilder;
import com.charles.camera.ui.CameraPreviewGridBuilder;
import com.charles.camera.ui.CaptureBarBuilder;
import com.charles.camera.ui.CaptureModeSwitcherBuilder;
import com.charles.camera.ui.CountDownTimerIndicatorBuilder;
import com.charles.camera.ui.OptionsPanelBuilder;
import com.charles.camera.ui.PinchZoomingUIBuilder;
import com.charles.camera.ui.PreviewGalleryBuilder;
import com.charles.camera.ui.FocusExposureIndicatorBuilder;
import com.charles.camera.ui.RecordingTimerUIBuilder;
import com.charles.camera.ui.TouchFocusExposureUIBuilder;
import com.charles.camera.ui.ZoomBarBuilder;

final class ComponentBuilders
{
	static final ComponentBuilder[] BUILDERS_CAMERA_THREAD = new ComponentBuilder[]{
		new AudioManagerBuilder(),
		new LocationManagerBuilder(),
		new SlowMotionControllerBuilder(),
		new TimelapseControllerBuilder(),
		new ZoomControllerBuilder(),
	};
	
	
	static final ComponentBuilder[] BUILDERS_MAIN_ACTIVITY = new ComponentBuilder[]{
		//new AudioManagerBuilder(),
		new CameraPreviewGridBuilder(),
		new CaptureBarBuilder(),
		new CaptureModeManagerBuilder(),
		new CaptureModeSwitcherBuilder(),
		new CountDownTimerBuilder(),
		new CountDownTimerIndicatorBuilder(),
		new FlashControllerBuilder(),
		new FocusExposureIndicatorBuilder(),
		new LocationManagerBuilder(),
		new OptionsPanelBuilder(),
		new PinchZoomingUIBuilder(),
		new PreviewGalleryBuilder(),
		new RecordingTimerUIBuilder(),
		new SceneManagerBuilder(),
		new SensorFocusControllerBuilder(),
		new SlowMotionUIBuilder(),
		new TimelapseUIBuilder(),
		new TouchFocusExposureUIBuilder(),
		new ZoomBarBuilder(),
		new ZoomControllerBuilder(),
	};
}
