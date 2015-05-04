package com.oneplus.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.oneplus.base.BaseThread;
import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Handle;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.ScreenSize;
import com.oneplus.base.component.Component;
import com.oneplus.base.component.ComponentBuilder;
import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.base.component.ComponentEventArgs;
import com.oneplus.base.component.ComponentManager;
import com.oneplus.base.component.ComponentOwner;
import com.oneplus.camera.media.AudioManager;
import com.oneplus.camera.media.MediaType;

/**
 * Camera access and control thread.
 */
public class CameraThread extends BaseThread implements ComponentOwner
{
	// Default component builders
	private static final ComponentBuilder[] DEFAULT_COMPONENT_BUILDERS = new ComponentBuilder[]{
		new CameraDeviceManagerBuilder(),
	};
	
	
	// Constants
	private static final int MSG_SCREEN_SIZE_CHANGED = 10000;
	
	
	/**
	 * Flag to indicate that operation should be performed synchronously.
	 */
	public static final int FLAG_SYNCHRONOUS = 0x1;
	
	
	/**
	 * Read-only property for available camera list.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static final PropertyKey<List<Camera>> PROP_AVAILABLE_CAMERAS = new PropertyKey<List<Camera>>("AvailableCameras", (Class)List.class, CameraThread.class, Collections.EMPTY_LIST);
	/**
	 * Read-only property for current primary camera.
	 */
	public static final PropertyKey<Camera> PROP_CAMERA = new PropertyKey<>("Camera", Camera.class, CameraThread.class, PropertyKey.FLAG_READONLY, null);
	/**
	 * Read-only property for current primary camera preview state.
	 */
	public static final PropertyKey<OperationState> PROP_CAMERA_PREVIEW_STATE = new PropertyKey<>("CameraPreviewState", OperationState.class, CameraThread.class, OperationState.STOPPED);
	/**
	 * Read-only property for current captured media type.
	 */
	public static final PropertyKey<MediaType> PROP_MEDIA_TYPE = new PropertyKey<>("MediaType", MediaType.class, CameraThread.class, MediaType.PHOTO);
	/**
	 * Read-only property for photo capture state.
	 */
	public static final PropertyKey<PhotoCaptureState> PROP_PHOTO_CAPTURE_STATE = new PropertyKey<>("PhotoCaptureState", PhotoCaptureState.class, CameraThread.class, PhotoCaptureState.PREPARING);
	/**
	 * Read-only property for screen size.
	 */
	public static final PropertyKey<ScreenSize> PROP_SCREEN_SIZE = new PropertyKey<>("ScreenSize", ScreenSize.class, CameraThread.class, ScreenSize.EMPTY);
	/**
	 * Read-only property for video capture state.
	 */
	public static final PropertyKey<VideoCaptureState> PROP_VIDEO_CAPTURE_STATE = new PropertyKey<>("VideoCaptureState", VideoCaptureState.class, CameraThread.class, VideoCaptureState.PREPARING);
	
	
	/**
	 * Event raised when unexpected camera error occurred.
	 */
	public static final EventKey<CameraEventArgs> EVENT_CAMERA_ERROR = new EventKey<>("CameraError", CameraEventArgs.class, CameraThread.class);
	/**
	 * Event raised when default photo capture process completed.
	 */
	public static final EventKey<CaptureEventArgs> EVENT_DEFAULT_PHOTO_CAPTURE_COMPLETED = new EventKey<>("DefaultPhotoCaptureCompleted", CaptureEventArgs.class, CameraThread.class);
	
	
	// Private fields
	private AudioManager m_AudioManager;
	private Handle m_BurstCaptureSoundStreamHandle;
	private final Context m_Context;
	private Handle m_CameraCaptureHandle;
	private CameraDeviceManager m_CameraDeviceManager;
	private volatile ComponentManager m_ComponentManager;
	private final PhotoCaptureHandlerHandle m_DefaultPhotoCaptureHandlerHandle = new PhotoCaptureHandlerHandle(null);
	private Handle m_DefaultShutterSoundHandle;
	private volatile int m_DefaultShutterSoundResId;
	private final VideoCaptureHandlerHandle m_DefaultVideoCaptureHandlerHandle = new VideoCaptureHandlerHandle(null);
	private boolean m_IsCapturingBurstPhotos;
	private final List<ComponentBuilder> m_InitialComponentBuilders = new ArrayList<>();
	private volatile MediaType m_InitialMediaType;
	private volatile ScreenSize m_InitialScreenSize;
	private MediaRecorder m_MediaRecorder;
	private final List<CameraPreviewStopRequest> m_PendingCameraPreviewStopRequests = new ArrayList<>();
	private PhotoCaptureHandle m_PhotoCaptureHandle;
	private PhotoCaptureHandlerHandle m_PhotoCaptureHandlerHandle;
	private List<PhotoCaptureHandlerHandle> m_PhotoCaptureHandlerHandles;
	private VideoCaptureHandle m_VideoCaptureHandle;
	private VideoCaptureHandlerHandle m_VideoCaptureHandlerHandle;
	private List<VideoCaptureHandlerHandle> m_VideoCaptureHandlerHandles;
	
	
	// Runnables.
	private final Runnable m_CloseCamerasRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			closeCamerasInternal();
		}
	};
	
	
	// Property call-backs.
	private final PropertyChangedCallback<OperationState> m_CameraPreviewStateChangedCallback = new PropertyChangedCallback<OperationState>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<OperationState> key, PropertyChangeEventArgs<OperationState> e)
		{
			onCameraPreviewStateChanged((Camera)source, e.getOldValue(), e.getNewValue());
		}
	};
	private final PropertyChangedCallback<OperationState> m_CaptureStateChangedCallback = new PropertyChangedCallback<OperationState>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<OperationState> key, PropertyChangeEventArgs<OperationState> e)
		{
			if(e.getNewValue() == OperationState.STOPPED)
				onCaptureCompleted((Camera)source);
		}
	};
	
	
	// Event handlers.
	private final EventHandler<EventArgs> m_CameraErrorHandler = new EventHandler<EventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<EventArgs> key, EventArgs e)
		{
			onCameraError((Camera)source);
		}
	};
	private final EventHandler<CameraCaptureEventArgs> m_CaptureFailedHandler = new EventHandler<CameraCaptureEventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<CameraCaptureEventArgs> key, CameraCaptureEventArgs e)
		{
			onCaptureFailed(e);
		}
	};
	private final EventHandler<CameraCaptureEventArgs> m_PictureReceivedHandler = new EventHandler<CameraCaptureEventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<CameraCaptureEventArgs> key, CameraCaptureEventArgs e)
		{
			onPictureReceived(e);
		}
	};
	private final EventHandler<CameraCaptureEventArgs> m_ShutterHandler = new EventHandler<CameraCaptureEventArgs>()
	{
		@Override
		public void onEventReceived(EventSource source, EventKey<CameraCaptureEventArgs> key, CameraCaptureEventArgs e)
		{
			onShutter(e);
		}
	};
	
	
	// Class for capture handler.
	private final class PhotoCaptureHandlerHandle extends Handle
	{
		public final PhotoCaptureHandler captureHandler;
		
		public PhotoCaptureHandlerHandle(PhotoCaptureHandler handler)
		{
			super("PhotoCaptureHandler");
			this.captureHandler = handler;
		}

		@Override
		protected void onClose(int flags)
		{
			//
		}
	}
	private final class VideoCaptureHandlerHandle extends Handle
	{
		public final VideoCaptureHandler captureHandler;
		
		public VideoCaptureHandlerHandle(VideoCaptureHandler handler)
		{
			super("VideoCaptureHandler");
			this.captureHandler = handler;
		}

		@Override
		protected void onClose(int flags)
		{
			//
		}
	}
	
	
	// Class for capture handle.
	private final class PhotoCaptureHandle extends CaptureHandle
	{
		public PhotoCaptureHandler captureHandler;
		public final int frameCount;
		
		public PhotoCaptureHandle(int frameCount)
		{
			super(MediaType.PHOTO);
			this.frameCount = frameCount;
		}

		@Override
		protected void onClose(int flags)
		{
			stopCapturePhoto(this);
		}
	}
	private final class VideoCaptureHandle extends CaptureHandle
	{
		public CamcorderProfile camcorderProfile;
		public VideoCaptureHandler captureHandler;
		
		public VideoCaptureHandle()
		{
			super(MediaType.VIDEO);
		}

		@Override
		protected void onClose(int flags)
		{
			stopCaptureVideo(this);
		}
	}
	
	
	// Class for camera preview stop request.
	private static final class CameraPreviewStopRequest
	{
		public final Camera camera;
		public final int flags;
		public final boolean[] result;
		
		public CameraPreviewStopRequest(Camera camera, boolean[] result, int flags)
		{
			this.camera = camera;
			this.flags = flags;
			this.result = result;
		}
	}
	
	
	/**
	 * Initialize new CameraThread instance.
	 * @param context Related {@link android.content.Context Context}.
	 * @param callback Call-back when camera thread starts.
	 * @param callbackHandler Handler for call-back.
	 */
	public CameraThread(Context context, ThreadStartCallback callback, Handler callbackHandler)
	{
		super("Camera Thread", callback, callbackHandler);
		if(context == null)
			throw new IllegalArgumentException("No context.");
		m_Context = context;
	}
	
	
	/**
	 * Add component builders to camera thread.
	 * @param builders Component builders to add.
	 */
	public final void addComponentBuilders(final ComponentBuilder[] builders)
	{
		if(this.isDependencyThread())
			m_ComponentManager.addComponentBuilders(builders, this);
		else
		{
			synchronized(this)
			{
				if(m_ComponentManager != null)
				{
					HandlerUtils.post(this, new Runnable()
					{
						@Override
						public void run()
						{
							m_ComponentManager.addComponentBuilders(builders, CameraThread.this);
						}
					});
				}
				else
					m_InitialComponentBuilders.addAll(Arrays.asList(builders));
			}
		}
	}
	
	
	// Bind to components.
	@SuppressWarnings("unchecked")
	private boolean bindToComponents()
	{
		// bind to AudioManager
		m_AudioManager = m_ComponentManager.findComponent(AudioManager.class, this);
		if(m_AudioManager != null)
			m_DefaultShutterSoundHandle = m_AudioManager.loadSound(m_DefaultShutterSoundResId, AudioManager.STREAM_RING, 0);
		else
			Log.w(TAG, "bindToComponents() - No AudioManager");
		
		// bind to CameraDeviceManager
		m_CameraDeviceManager = m_ComponentManager.findComponent(CameraDeviceManager.class);
		if(m_CameraDeviceManager != null)
		{
			m_CameraDeviceManager.addCallback(CameraDeviceManager.PROP_AVAILABLE_CAMERAS, new PropertyChangedCallback<List<Camera>>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<List<Camera>> key, PropertyChangeEventArgs<List<Camera>> e)
				{
					onAvailableCamerasChanged(e.getOldValue(), e.getNewValue());
				}
			});
			this.onAvailableCamerasChanged(Collections.EMPTY_LIST, m_CameraDeviceManager.get(CameraDeviceManager.PROP_AVAILABLE_CAMERAS));
		}
		else
		{
			Log.e(TAG, "bindToComponents() - No CameraDeviceManager");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	/**
	 * Get internal component manager.
	 * @return Component manager.
	 */
	protected final ComponentManager getComponentManager()
	{
		return m_ComponentManager;
	}
	
	
	/**
	 * Get related context.
	 * @return {@link android.content.Context Context}.
	 */
	public final Context getContext()
	{
		return m_Context;
	}
	
	
	/**
	 * Start photo capture.
	 * @return Capture handle.
	 */
	public final CaptureHandle capturePhoto()
	{
		return this.capturePhoto(1, 0);
	}
	
	
	/**
	 * Start photo capture.
	 * @param frameCount Target frame count, 1 for single shot; positive integer for limited burst; negative for unlimited burst.
	 * @return Capture handle.
	 */
	public final CaptureHandle capturePhoto(int frameCount)
	{
		return this.capturePhoto(frameCount, 0);
	}
	
	
	/**
	 * Start photo capture.
	 * @param frameCount Target frame count, 1 for single shot; positive integer for limited burst; negative for unlimited burst.
	 * @param flags Flags, reserved.
	 * @return Capture handle.
	 */
	public final CaptureHandle capturePhoto(final int frameCount, final int flags)
	{
		// check parameter
		if(frameCount == 0)
		{
			Log.e(TAG, "capturePhoto() - Invalid frame count");
			return null;
		}
		
		// create handle
		final PhotoCaptureHandle handle = new PhotoCaptureHandle(frameCount);
		
		// capture
		if(this.isDependencyThread())
		{
			if(this.capturePhotoInternal(handle))
				return handle;
			return null;
		}
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				capturePhotoInternal(handle);
			}
		}))
		{
			Log.v(TAG, "capturePhoto() - Create handle ", handle);
			return handle;
		}
		Log.e(TAG, "capturePhoto() - Fail to perform cross-thread operation");
		return null;
	}
	
	
	// Capture photo.
	private boolean capturePhotoInternal(PhotoCaptureHandle handle)
	{
		// check state
		if(this.get(PROP_PHOTO_CAPTURE_STATE) != PhotoCaptureState.READY)
		{
			Log.e(TAG, "capturePhotoInternal() - Capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
			return false;
		}
		
		Log.w(TAG, "capturePhotoInternal() - Handle : " + handle);
		
		// change state
		this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.STARTING);
		
		// capture
		PhotoCaptureHandlerHandle handlerHandle = null;
		try
		{
			Camera camera = this.get(PROP_CAMERA);
			for(int i = m_PhotoCaptureHandlerHandles.size() - 1 ; i >= 0 ; --i)
			{
				handlerHandle = m_PhotoCaptureHandlerHandles.get(i);
				if(handlerHandle.captureHandler.capture(camera, handle))
				{
					Log.w(TAG, "capturePhotoInternal() - Capture process is handled by " + handlerHandle.captureHandler);
					break;
				}
				handlerHandle = null;
			}
			if(handlerHandle == null)
			{
				Log.v(TAG, "capturePhotoInternal() - Use default capture process");
				if(!this.capturePhotoInternal(handle.frameCount))
					throw new RuntimeException("Fail to use default photo capture process.");
				handlerHandle = m_DefaultPhotoCaptureHandlerHandle;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "capturePhotoInternal() - Fail to capture", ex);
			if(this.get(PROP_CAMERA_PREVIEW_STATE) == OperationState.STARTED)
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
			else
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
			return false;
		}
		
		// complete
		m_PhotoCaptureHandlerHandle = handlerHandle;
		m_PhotoCaptureHandle = handle;
		this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.CAPTURING);
		return true;
	}
	
	
	// Default photo capture process.
	private boolean capturePhotoInternal(int frameCount)
	{
		// prepare event handlers
		Camera camera = this.get(PROP_CAMERA);
		camera.addHandler(Camera.EVENT_CAPTURE_FAILED, m_CaptureFailedHandler);
		camera.addHandler(Camera.EVENT_PICTURE_RECEIVED, m_PictureReceivedHandler);
		camera.addHandler(Camera.EVENT_SHUTTER, m_ShutterHandler);
		
		// prepare property changed call-backs.
		camera.addCallback(Camera.PROP_CAPTURE_STATE, m_CaptureStateChangedCallback);
		
		// capture
		m_CameraCaptureHandle = camera.capture(frameCount, 0);
		if(!Handle.isValid(m_CameraCaptureHandle))
		{
			Log.e(TAG, "capturePhotoInternal() - Fail to capture");
			camera.removeHandler(Camera.EVENT_CAPTURE_FAILED, m_CaptureFailedHandler);
			camera.removeHandler(Camera.EVENT_PICTURE_RECEIVED, m_PictureReceivedHandler);
			camera.removeHandler(Camera.EVENT_SHUTTER, m_ShutterHandler);
			camera.removeCallback(Camera.PROP_CAPTURE_STATE, m_CaptureStateChangedCallback);
			return false;
		}
		
		// complete
		m_IsCapturingBurstPhotos = (frameCount != 1);
		return true;
	}
	
	
	/**
	 * Start video capture.
	 * @return Capture handle.
	 */
	public final CaptureHandle captureVideo()
	{
		final VideoCaptureHandle handle = new VideoCaptureHandle();
		if(this.isDependencyThread())
		{
			if(this.captureVideoInternal(handle, false))
				return handle;
			return null;
		}
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				captureVideoInternal(handle, false);
			}
		}))
		{
			return handle;
		}
		Log.e(TAG, "captureVideo() - Fail to perform cross-thread operation");
		return null;
	}
	
	
	//
	private boolean captureVideoInternal(VideoCaptureHandle handle, boolean isShutterSoundPlayed)
	{
		//
		File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "100MEDIA");
		if(!directory.exists() && !directory.mkdir())
		{
			Log.e(TAG, "captureVideoInternal() - Fail to create " + directory.getAbsolutePath());
			return false;
		}
		
		//
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		File file = new File(directory, "VID_" + dateFormat.format(new Date()) + ".mp4");
		Log.w(TAG, "captureVideoInternal() - Save video to " + file);
		
		//
		MediaRecorder mediaRecorder = new MediaRecorder();
		try
		{
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
			mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
			mediaRecorder.setOutputFile(file.getAbsolutePath());
			mediaRecorder.prepare();
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "captureVideoInternal() - Fail to prepare", ex);
			mediaRecorder.release();
			return false;
		}
		
		// connect to camera
		try
		{
			this.get(PROP_CAMERA).set(Camera.PROP_VIDEO_SURFACE, mediaRecorder.getSurface());
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "captureVideoInternal() - Fail to connect to camera", ex);
			mediaRecorder.release();
			return false;
		}
		
		// start
		try
		{
			mediaRecorder.start();
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "captureVideoInternal() - Fail to start", ex);
			this.get(PROP_CAMERA).set(Camera.PROP_VIDEO_SURFACE, null);
			mediaRecorder.release();
			return false;
		}
		
		m_MediaRecorder = mediaRecorder;
		m_VideoCaptureHandle = handle;
		
		// complete
		return true;
	}
	
	
	/**
	 * Close all cameras.
	 */
	public final void closeCameras()
	{
		if(this.isDependencyThread())
			this.closeCamerasInternal();
		else if(!HandlerUtils.post(this, m_CloseCamerasRunnable))
			Log.e(TAG, "closeCameras() - Fail to perform cross-thread operation");
	}
	
	
	// Close all cameras
	private void closeCamerasInternal()
	{
		Log.w(TAG, "closeCamerasInternal() - Start");
		List<Camera> cameras = this.get(PROP_AVAILABLE_CAMERAS);
		for(int i = cameras.size() - 1 ; i >= 0 ; --i)
			cameras.get(i).close(0);
		Log.w(TAG, "closeCamerasInternal() - End");
	}
	
	
	/**
	 * Complete media capture process.
	 * @param captureHandler Handle returned from {@link #setPhotoCaptureHandler(PhotoCaptureHandler, int)} or {@link #setVideoCaptureHandler(VideoCaptureHandler, int)}.
	 * @param handle Capture handle.
	 * @return Whether capture completes successfully or not.
	 */
	public final boolean completeCapture(Handle captureHandler, CaptureHandle handle)
	{
		// check state
		this.verifyAccess();
		if(captureHandler == null)
		{
			Log.e(TAG, "completeCapture() - No capture handler");
			return false;
		}
		if(handle == null)
		{
			Log.e(TAG, "completeCapture() - No capture handle");
			return false;
		}
		
		Log.w(TAG, "completeCapture() - Handle : " + handle);
		
		// complete capture
		switch(handle.getMediaType())
		{
			case PHOTO:
			{
				// check handles
				if(m_PhotoCaptureHandlerHandle != captureHandler)
				{
					Log.e(TAG, "completeCapture() - Invalid capture handler : " + captureHandler);
					return false;
				}
				if(handle != m_PhotoCaptureHandle)
				{
					Log.e(TAG, "completeCapture() - Invalid capture handle : " + handle);
					return false;
				}
				
				// clear states
				m_PhotoCaptureHandle = null;
				m_PhotoCaptureHandlerHandle = null;
				m_IsCapturingBurstPhotos = false;
				
				// update property
				if(this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO)
					Log.w(TAG, "completeCapture() - Complete video snapshot");
				if(this.get(PROP_CAMERA_PREVIEW_STATE) == OperationState.STARTED)
					this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
				else
					this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
				break;
			}
			case VIDEO:
			{
				// check handles
				//
				break;
			}
		}
		
		// complete
		return true;
	}
	
	
	// Find component extends or implements given type.
	@Override
	public <TComponent extends Component> TComponent findComponent(Class<TComponent> componentType)
	{
		if(m_ComponentManager != null)
			return m_ComponentManager.findComponent(componentType, this);
		return null;
	}
	
	
	// Find all components extend or implement given type.
	@SuppressWarnings("unchecked")
	@Override
	public <TComponent extends Component> TComponent[] findComponents(Class<TComponent> componentType)
	{
		if(m_ComponentManager != null)
			return m_ComponentManager.findComponents(componentType, this);
		return (TComponent[])new Component[0];
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_SCREEN_SIZE_CHANGED:
				this.setReadOnly(PROP_SCREEN_SIZE, (ScreenSize)msg.obj);
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when available camera list changes.
	private void onAvailableCamerasChanged(List<Camera> oldCameras, List<Camera> cameras)
	{
		// attach/detach call-backs
		for(int i = cameras.size() - 1 ; i >= 0 ; --i)
		{
			Camera camera = cameras.get(i);
			if(!oldCameras.contains(camera))
			{
				camera.addCallback(Camera.PROP_PREVIEW_STATE, m_CameraPreviewStateChangedCallback);
				camera.addHandler(Camera.EVENT_ERROR, m_CameraErrorHandler);
			}
		}
		for(int i = oldCameras.size() - 1 ; i >= 0 ; --i)
		{
			Camera camera = oldCameras.get(i);
			if(!cameras.contains(camera))
			{
				camera.removeCallback(Camera.PROP_PREVIEW_STATE, m_CameraPreviewStateChangedCallback);
				camera.removeHandler(Camera.EVENT_ERROR, m_CameraErrorHandler);
			}
		}
		
		// update property
		this.setReadOnly(PROP_AVAILABLE_CAMERAS, cameras);
	}
	
	
	// Called when unexpected camera error occurred.
	private void onCameraError(Camera camera)
	{
		if(this.get(PROP_CAMERA) == camera)
		{
			Log.e(TAG, "onCameraError() - Camera : " + camera);
			this.raise(EVENT_CAMERA_ERROR, new CameraEventArgs(camera));
		}
	}
	
	
	// Called when primary camera preview state changes.
	private void onCameraPreviewStateChanged(Camera camera, OperationState prevState, OperationState state)
	{
		// continue stopping preview
		if(state == OperationState.STARTED)
		{
			for(int i = m_PendingCameraPreviewStopRequests.size() - 1 ; i >= 0 ; --i)
			{
				CameraPreviewStopRequest request = m_PendingCameraPreviewStopRequests.get(i);
				if(request.camera == camera)
				{
					Log.w(TAG, "onCameraPreviewStateChanged() - Continue stopping preview for " + camera);
					m_PendingCameraPreviewStopRequests.remove(i);
					this.stopCameraPreviewInternal(camera, request.result, request.flags);
				}
			}
			if(camera.get(Camera.PROP_PREVIEW_STATE) != state)
				return;
		}
		
		// check camera
		if(this.get(PROP_CAMERA) != camera)
			return;
		
		// update preview state property
		this.setReadOnly(PROP_CAMERA_PREVIEW_STATE, state);
		
		// release media recorder
		if(m_VideoCaptureHandle == null && m_MediaRecorder != null)
		{
			if(state == OperationState.STARTED || state == OperationState.STOPPED)
			{
				Log.v(TAG, "onCameraPreviewStateChanged() - Release media recorder");
				m_MediaRecorder.release();
				m_MediaRecorder = null;
			}
		}
		
		// update capture state properties
		if(state == OperationState.STARTED)
		{
			// change capture state
			if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.PREPARING)
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.READY);
			if(this.get(PROP_MEDIA_TYPE) == MediaType.VIDEO && this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.PREPARING)
				this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.READY);
		}
		else
		{
			// change capture state
			if(this.get(PROP_PHOTO_CAPTURE_STATE) == PhotoCaptureState.READY)
				this.setReadOnly(PROP_PHOTO_CAPTURE_STATE, PhotoCaptureState.PREPARING);
			if(this.get(PROP_VIDEO_CAPTURE_STATE) == VideoCaptureState.READY)
				this.setReadOnly(PROP_VIDEO_CAPTURE_STATE, VideoCaptureState.PREPARING);
		}
	}
	
	
	// Called when capture completed.
	private void onCaptureCompleted(Camera camera)
	{
		// remove handlers and call-backs
		camera.removeHandler(Camera.EVENT_CAPTURE_FAILED, m_CaptureFailedHandler);
		camera.removeHandler(Camera.EVENT_PICTURE_RECEIVED, m_PictureReceivedHandler);
		camera.removeHandler(Camera.EVENT_SHUTTER, m_ShutterHandler);
		camera.removeCallback(Camera.PROP_CAPTURE_STATE, m_CaptureStateChangedCallback);
		
		// reset state
		m_CameraCaptureHandle = null;
		
		// raise event
		this.raise(EVENT_DEFAULT_PHOTO_CAPTURE_COMPLETED, new CaptureEventArgs(m_PhotoCaptureHandle));
		
		// complete capture
		this.completeCapture(m_DefaultPhotoCaptureHandlerHandle, m_PhotoCaptureHandle);
	}
	
	
	// Called when capture failed.
	private void onCaptureFailed(CameraCaptureEventArgs e)
	{
		//
	}
	
	
	// Called when receiving captured picture.
	private void onPictureReceived(CameraCaptureEventArgs e)
	{
		Log.v(TAG, "onPictureReceived() - Index : ", e.getFrameIndex());
		
		//
		File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "100MEDIA");
		if(!directory.exists() && !directory.mkdir())
		{
			Log.e(TAG, "onPictureReceived() - Fail to create " + directory.getAbsolutePath());
			return;
		}
		
		//
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		File file = new File(directory, "IMG_" + dateFormat.format(new Date()) + ".jpg");
		Log.w(TAG, "onPictureReceived() - Write picture to " + file);
		
		//
		try(FileOutputStream stream = new FileOutputStream(file))
		{
			stream.write(e.getPicture());
			Log.w(TAG, "onPictureReceived() - Picture saved");
		} 
		catch (Throwable ex)
		{
			Log.e(TAG, "onPictureReceived() - Fail to write " + file, ex);
		}
	}
	
	
	// Called when starting capturing picture.
	private void onShutter(CameraCaptureEventArgs e)
	{
		Log.v(TAG, "onShutter() - Index : ", e.getFrameIndex());
		
		// play shutter sound
		if(e.getFrameIndex() == 0)
		{
			if(m_IsCapturingBurstPhotos)
			{
				//if(Handle.isValid(m_DefaultShutterSoundHandle))
					//m_BurstCaptureSoundStreamHandle = m_AudioManager.playSound(m_DefaultShutterSoundHandle, AudioManager.FLAG_LOOP);
				//else
					//Log.w(TAG, "onShutter() - No sound for burst capture");
			}
			else
				this.playDefaultShutterSound();
		}
	}
	
	
	// Called when thread starts.
	@Override
	protected void onStarted()
	{
		// call super
		super.onStarted();
		
		// create component with HIGH priority
		m_ComponentManager.createComponents(ComponentCreationPriority.HIGH, this);
		
		// bind to components
		if(!this.bindToComponents())
			throw new RuntimeException("Fail to bind components.");
	}
	
	
	// Called when starting thread.
	@Override
	protected void onStarting()
	{
		// call super
		super.onStarting();
		
		// enable logs
		this.enablePropertyLogs(PROP_CAMERA_PREVIEW_STATE, LOG_PROPERTY_CHANGE);
		
		// create handle lists
		m_PhotoCaptureHandlerHandles = new ArrayList<>();
		m_VideoCaptureHandlerHandles = new ArrayList<>();
		
		// setup initial states
		synchronized(this)
		{
			// setup screen size
			if(m_InitialScreenSize != null)
			{
				Log.v(TAG, "onStarting() - Initial screen size : ", m_InitialScreenSize);
				this.setReadOnly(PROP_SCREEN_SIZE, m_InitialScreenSize);
				m_InitialScreenSize = null;
			}
			
			// setup media type
			if(m_InitialMediaType != null)
			{
				Log.v(TAG, "onStarting() - Initial media type : ", m_InitialMediaType);
				this.setReadOnly(PROP_MEDIA_TYPE, m_InitialMediaType);
			}
			
			// create component manager
			m_ComponentManager = new ComponentManager();
			m_ComponentManager.addComponentBuilders(DEFAULT_COMPONENT_BUILDERS, this);
			m_ComponentManager.addHandler(ComponentManager.EVENT_COMPONENT_ADDED, new EventHandler<ComponentEventArgs<Component>>()
			{
				@Override
				public void onEventReceived(EventSource source, EventKey<ComponentEventArgs<Component>> key, ComponentEventArgs<Component> e)
				{
					CameraThread.this.raise(EVENT_COMPONENT_ADDED, e);
				}
			});
			m_ComponentManager.addHandler(ComponentManager.EVENT_COMPONENT_REMOVED, new EventHandler<ComponentEventArgs<Component>>()
			{
				@Override
				public void onEventReceived(EventSource source, EventKey<ComponentEventArgs<Component>> key, ComponentEventArgs<Component> e)
				{
					CameraThread.this.raise(EVENT_COMPONENT_REMOVED, e);
				}
			});
			if(!m_InitialComponentBuilders.isEmpty())
			{
				ComponentBuilder[] builders = new ComponentBuilder[m_InitialComponentBuilders.size()];
				m_InitialComponentBuilders.toArray(builders);
				m_InitialComponentBuilders.clear();
				m_ComponentManager.addComponentBuilders(builders, this);
			}
		}
		
		// create component with LAUNCH priority
		m_ComponentManager.createComponents(ComponentCreationPriority.LAUNCH, this);
	}
	
	
	// Called before stopping thread.
	@Override
	protected void onStopping()
	{
		// close all cameras
		this.closeCamerasInternal();
		
		// call super
		super.onStopping();
	}
	
	
	/**
	 * Open given camera.
	 * @param camera Camera to open.
	 * @return Whether camera opening starts successfully or not.
	 */
	public final boolean openCamera(Camera camera)
	{
		return this.openCamera(camera, 0);
	}
	
	
	/**
	 * Open given camera.
	 * @param camera Camera to open.
	 * @param flags Flags, reserved.
	 * @return Whether camera opening starts successfully or not.
	 */
	public final boolean openCamera(final Camera camera, final int flags)
	{
		if(camera == null)
		{
			Log.e(TAG, "openCamera() - No camera");
			return false;
		}
		if(this.isDependencyThread())
			return this.openCameraInternal(camera, flags);
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				openCameraInternal(camera, flags);
			}
		}))
		{
			return true;
		}
		Log.e(TAG, "openCamera() - Fail to perform cross-thread operation");
		return false;
	}
	
	
	// Open camera.
	private boolean openCameraInternal(Camera camera, int flags)
	{
		// check camera list
		if(!this.get(PROP_AVAILABLE_CAMERAS).contains(camera))
		{
			Log.e(TAG, "openCameraInternal() - Camera " + camera + " is not contained in available camera list");
			return false;
		}
		
		// check state
		switch(camera.get(Camera.PROP_STATE))
		{
			case OPENING:
			case OPENED:
				return true;
			default:
				break;
		}
		
		// open camera
		Log.v(TAG, "openCameraInternal() - Open ", camera);
		try
		{
			if(!camera.open(0))
			{
				Log.e(TAG, "openCameraInternal() - Fail to open " + camera);
				return false;
			}
		}
		catch(Throwable ex)
		{
			return false;
		}
		
		// update property
		this.setReadOnly(PROP_CAMERA, camera);
		
		// complete
		return true;
	}
	
	
	/**
	 * Play default shutter sound.
	 */
	public void playDefaultShutterSound()
	{
		// check state
		this.verifyAccess();
		if(!Handle.isValid(m_DefaultShutterSoundHandle))
		{
			Log.w(TAG, "playDefaultShutterSound() - No shutter sound to play");
			return;
		}
		
		// play sound
		m_AudioManager.playSound(m_DefaultShutterSoundHandle, 0);
	}
	
	
	// Release and remove given component.
	@Override
	public void removeComponent(Component component)
	{
		this.verifyAccess();
		m_ComponentManager.removeComponent(component);
	}
	
	
	/**
	 * Set default shutter sound before starting thread.
	 * @param resId Shutter sound resource ID.
	 */
	public final synchronized void setDefaultShutterSound(int resId)
	{
		// check state
		if(this.get(PROP_IS_STARTED))
			throw new RuntimeException("Cannot change default shutter sound after starting");
		
		// save state
		m_DefaultShutterSoundResId = resId;
	}
	
	
	/**
	 * Change current media type.
	 * @param mediaType New media type.
	 * @return Whether media type changes successfully or not.
	 */
	public boolean setMediaType(final MediaType mediaType)
	{
		if(this.isDependencyThread())
			return this.setMediaTypeInternal(mediaType);
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				setMediaTypeInternal(mediaType);
			}
		}))
		{
			return true;
		}
		Log.e(TAG, "setMediaType() - Fail to perform cross-thread operation");
		return false;
	}
	
	
	// Change current media type.
	private boolean setMediaTypeInternal(MediaType mediaType)
	{
		// check state
		if(this.get(PROP_MEDIA_TYPE) == mediaType)
			return true;
		Log.v(TAG, "setMediaTypeInternal() - Media type : ", mediaType);
		switch(mediaType)
		{
			case PHOTO:
			{
				switch(this.get(PROP_VIDEO_CAPTURE_STATE))
				{
					case PREPARING:
					case READY:
						break;
					default:
						Log.e(TAG, "setMediaTypeInternal() - Current video capture state is " + this.get(PROP_VIDEO_CAPTURE_STATE));
						return false;
				}
				break;
			}
			
			case VIDEO:
			{
				switch(this.get(PROP_PHOTO_CAPTURE_STATE))
				{
					case PREPARING:
					case READY:
						break;
					default:
						Log.e(TAG, "setMediaTypeInternal() - Current photo capture state is " + this.get(PROP_PHOTO_CAPTURE_STATE));
						return false;
				}
				break;
			}
			
			default:
				Log.e(TAG, "setMediaTypeInternal() - Unknown media type : " + mediaType);
				return false;
		}
		
		// stop preview first
		Camera camera = this.get(PROP_CAMERA);
		boolean needRestartPreview;
		switch(this.get(PROP_CAMERA_PREVIEW_STATE))
		{
			case STARTING:
			case STARTED:
				Log.w(TAG, "setMediaTypeInternal() - Stop preview to change media type");
				needRestartPreview = true;
				if(!this.stopCameraPreview(camera))
				{
					Log.e(TAG, "setMediaTypeInternal() - Fail to stop preview");
					return false;
				}
				break;
			default:
				needRestartPreview = false;
				break;
		}
		
		// change media type
		this.setReadOnly(PROP_MEDIA_TYPE, mediaType);
		
		// start preview
		if(needRestartPreview)
		{
			Log.w(TAG, "setMediaTypeInternal() - Restart preview");
			if(!this.startCameraPreview(camera, null))
				Log.e(TAG, "setMediaTypeInternal() - Fail to restart preview");
		}
		
		// complete
		return true;
	}
	
	
	//
	public final Handle setPhotoCaptureHandler(PhotoCaptureHandler handler, int flags)
	{
		return null;
	}
	
	
	/**
	 * Set screen size.
	 * @param size Screen size.
	 */
	final void setScreenSize(ScreenSize size)
	{
		if(size == null)
			throw new IllegalArgumentException("No screen size.");
		if(this.isDependencyThread())
			this.setReadOnly(PROP_SCREEN_SIZE, size);
		else
		{
			synchronized(this)
			{
				if(!HandlerUtils.sendMessage(this, MSG_SCREEN_SIZE_CHANGED, 0, 0, size))
					m_InitialScreenSize = size;
			}
		}
	}
	
	
	//
	public final Handle setVideoCaptureHandler(VideoCaptureHandler handler, int flags)
	{
		return null;
	}
	
	
	// Start camera thread with given media type.
	public synchronized void start(MediaType mediaType)
	{
		this.start();
		m_InitialMediaType = mediaType;
	}
	
	
	/**
	 * Start camera preview.
	 * @param camera Camera to start preview.
	 * @param receiver Camera preview receiver, Null to use current receiver.
	 * @return Whether camera preview starts successfully or not.
	 */
	public final boolean startCameraPreview(Camera camera, Object receiver)
	{
		return this.startCameraPreview(camera, receiver, 0);
	}
	
	
	/**
	 * Start camera preview.
	 * @param camera Camera to start preview.
	 * @param receiver Camera preview receiver, Null to use current receiver.
	 * @param flags Flags, reserved.
	 * @return Whether camera preview starts successfully or not.
	 */
	public final boolean startCameraPreview(final Camera camera, final Object receiver, final int flags)
	{
		// check parameter
		if(camera == null)
		{
			Log.e(TAG, "startCameraPreview() - No camera");
			return false;
		}
		if(this.isDependencyThread())
			return this.startCameraPreviewInternal(camera, receiver, flags);
		else if(HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				startCameraPreviewInternal(camera, receiver, flags);
			}
		}))
		{
			return true;
		}
		Log.e(TAG, "startCameraPreview() - Fail to perform cross-thread operation");
		return false;
	}
	
	
	// Start camera preview.
	@SuppressWarnings("incomplete-switch")
	private boolean startCameraPreviewInternal(Camera camera, Object receiver, int flags)
	{
		// open camera first
		if(!this.openCameraInternal(camera, 0))
		{
			Log.e(TAG, "startCameraPreviewInternal() - Fail to open camera");
			return false;
		}
		
		// check state
		switch(camera.get(Camera.PROP_PREVIEW_STATE))
		{
			case STOPPED:
				break;
			case STARTING:
			case STARTED:
				if(receiver != null && camera.get(Camera.PROP_PREVIEW_RECEIVER) != receiver)
				{
					Log.w(TAG, "startCameraPreviewInternal() - Preview receiver changed, stop preview first");
					camera.stopPreview(0);
				}
				break;
		}
		
		// set receiver
		if(receiver != null)
		{
			Log.w(TAG, "startCameraPreviewInternal() - Change preview receiver to " + receiver);
			camera.set(Camera.PROP_PREVIEW_RECEIVER, receiver);
		}
		
		// start preview
		Log.w(TAG, "startCameraPreviewInternal() - Start preview for camera " + camera);
		if(!camera.startPreview(0))
		{
			Log.e(TAG, "startCameraPreviewInternal() - Fail to start preview for camera " + camera);
			return false;
		}
		
		// complete
		return true;
	}
	
	
	/**
	 * Stop camera preview.
	 * @param camera Camera to stop preview.
	 * @return Camera preview stops successfully or not.
	 */
	public final boolean stopCameraPreview(Camera camera)
	{
		return this.stopCameraPreview(camera, 0);
	}
	
	
	/**
	 * Stop camera preview.
	 * @param camera Camera to stop preview.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_SYNCHRONOUS}</li>
	 * </ul>
	 * @return Camera preview stops successfully or not.
	 */
	public final boolean stopCameraPreview(final Camera camera, final int flags)
	{
		if(camera == null)
		{
			Log.e(TAG, "stopCameraPreview() - No camera");
			return false;
		}
		if(this.isDependencyThread())
			return this.stopCameraPreviewInternal(camera, null, flags);
		else
		{
			final boolean isSync = ((flags & FLAG_SYNCHRONOUS) != 0);
			final boolean[] result = new boolean[]{ false };
			synchronized(result)
			{
				if(!HandlerUtils.post(this, new Runnable()
				{
					@Override
					public void run()
					{
						stopCameraPreviewInternal(camera, (isSync ? result : null), flags);
					}
				}))
				{
					Log.e(TAG, "stopCameraPreview() - Fail to perform cross-thread operation");
					return false;
				}
				if(isSync)
				{
					try
					{
						Log.w(TAG, "stopCameraPreview() - Wait for camera thread [start]");
						result.wait(5000);
						Log.w(TAG, "stopCameraPreview() - Wait for camera thread [end]");
						if(result[0])
							return true;
						Log.e(TAG, "stopCameraPreview() - Timeout");
						return false;
					}
					catch(InterruptedException ex)
					{
						Log.e(TAG, "stopCameraPreview() - Interrupted", ex);
						return false;
					}
				}
				return true;
			}
		}
	}
	
	
	// Stop camera preview
	private boolean stopCameraPreviewInternal(final Camera camera, final boolean[] result, int flags)
	{
		try
		{
			// stop video recording first
			if(Handle.isValid(m_VideoCaptureHandle))
			{
				Log.w(TAG, "stopCameraPreviewInternal() - Stop video recording first");
				stopCaptureVideoInternal(m_VideoCaptureHandle);
			}
			
			// waiting for starting preview
			if(camera.get(Camera.PROP_PREVIEW_STATE) == OperationState.STARTING)
			{
				if(result != null)
				{
					Log.w(TAG, "stopCameraPreviewInternal() - Wait for preview start");
					m_PendingCameraPreviewStopRequests.add(new CameraPreviewStopRequest(camera, result, flags));
					return true;
				}
			}
			
			// stop preview
			Log.v(TAG, "stopCameraPreviewInternal() - Stop preview [start]");
			camera.stopPreview(0);
			Log.v(TAG, "stopCameraPreviewInternal() - Stop preview [end]");
			
			// notify waiting thread
			if(result != null)
			{
				if(camera.get(Camera.PROP_PREVIEW_STATE) != OperationState.STOPPING)
				{
					synchronized(result)
					{
						Log.w(TAG, "stopCameraPreviewInternal() - Notify waiting thread");
						result[0] = true;
						result.notifyAll();
					}
				}
				else
				{
					Log.w(TAG, "stopCameraPreviewInternal() - Wait for camera preview stop");
					camera.addCallback(Camera.PROP_PREVIEW_STATE, new PropertyChangedCallback<OperationState>()
					{
						@Override
						public void onPropertyChanged(PropertySource source, PropertyKey<OperationState> key, PropertyChangeEventArgs<OperationState> e)
						{
							if(e.getOldValue() == OperationState.STOPPING)
							{
								synchronized(result)
								{
									Log.w(TAG, "stopCameraPreviewInternal() - Notify waiting thread");
									result[0] = true;
									result.notifyAll();
								}
								camera.removeCallback(Camera.PROP_PREVIEW_STATE, this);
							}
						}
					});
				}
			}
			return true;
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "stopCameraPreviewInternal() - Error stopping camera preview", ex);
			if(result != null)
			{
				synchronized(result)
				{
					Log.w(TAG, "stopCameraPreviewInternal() - Notify waiting thread");
					result[0] = true;
					result.notifyAll();
				}
			}
			return false;
		}
	}
	
	
	// Stop photo capture.
	private void stopCapturePhoto(final PhotoCaptureHandle handle)
	{
		if(this.isDependencyThread())
			this.stopCapturePhotoInternal(handle);
		else if(!HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				stopCapturePhotoInternal(handle);
			}
		}))
		{
			Log.e(TAG, "stopCapturePhoto() - Fail to perform cross-thread operation");
		}
	}
	
	
	// Stop photo capture.
	private void stopCapturePhotoInternal(PhotoCaptureHandle handle)
	{
		// check handle
		if(m_PhotoCaptureHandle != handle)
		{
			Log.e(TAG, "stopCapturePhotoInternal() - Invalid handle");
			return;
		}
		
		Log.v(TAG, "stopCapturePhotoInternal() - Handle : ", handle);
		
		// check camera
		Camera camera = this.get(PROP_CAMERA);
		if(camera == null)
		{
			Log.e(TAG, "stopCapturePhotoInternal() - No camera");
			return;
		}
		
		// stop capture
		try
		{
			if(handle.captureHandler == null)
			{
				Log.w(TAG, "stopCapturePhotoInternal() - Use default photo capture stop process");
				m_CameraCaptureHandle = Handle.close(m_CameraCaptureHandle);
				m_BurstCaptureSoundStreamHandle = Handle.close(m_BurstCaptureSoundStreamHandle);
			}
			else
			{
				Log.w(TAG, "stopCapturePhotoInternal() - Use " + handle.captureHandler + " to stop capture");
				if(!handle.captureHandler.stopCapture(camera, handle))
					Log.e(TAG, "stopCapturePhotoInternal() - Fail to stop capture");
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "stopCapturePhotoInternal() - Fail to stop capture", ex);
		}
	}
	
	
	// Stop video recording.
	private void stopCaptureVideo(final VideoCaptureHandle handle)
	{
		if(this.isDependencyThread())
			this.stopCaptureVideoInternal(handle);
		else if(!HandlerUtils.post(this, new Runnable()
		{
			@Override
			public void run()
			{
				stopCaptureVideoInternal(handle);
			}
		}))
		{
			Log.e(TAG, "stopCaptureVideo() - Fail to perform cross-thread operation");
		}
	}
	
	
	// Stop video recording.
	private void stopCaptureVideoInternal(VideoCaptureHandle handle)
	{
		try
		{
			m_MediaRecorder.stop();
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "stopCaptureVideoInternal() - Fail to stop recorder", ex);
		}
		
		m_VideoCaptureHandle = null;
		
		Camera camera = this.get(PROP_CAMERA);
		camera.set(Camera.PROP_VIDEO_SURFACE, null);
		if(camera.get(Camera.PROP_PREVIEW_STATE) == OperationState.STOPPING)
		{
			Log.w(TAG, "stopCaptureVideoInternal() - Release media recorder after preview ready");
			return;
		}
		
		m_MediaRecorder.release();
		m_MediaRecorder = null;
	}
}