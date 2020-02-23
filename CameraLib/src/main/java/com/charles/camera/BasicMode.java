package com.charles.camera;

import com.charles.base.HandlerBaseObject;
import com.charles.base.Log;
import com.charles.base.PropertyChangeEventArgs;
import com.charles.base.PropertyChangedCallback;
import com.charles.base.PropertyKey;
import com.charles.base.PropertySource;
import com.charles.camera.media.MediaType;

/**
 * Base implementation of {@link Mode} interface.
 */
public abstract class BasicMode<T extends Mode<?>> extends HandlerBaseObject implements Mode<T>
{
	// Private fields.
	private final CameraActivity m_CameraActivity;
	private PropertyChangedCallback<Boolean> m_IsCameraThreadStartedCallback;
	private State m_State = State.EXITED;
	
	
	/**
	 * Initialize new BasicMode instance.
	 * @param cameraActivity Camera activity.
	 * @param id Mode ID.
	 */
	protected BasicMode(CameraActivity cameraActivity, String id)
	{
		// call super
		super(true);
		
		// check parameter
		if(cameraActivity == null)
			throw new IllegalArgumentException("No camera activity");
		
		// save state
		this.setReadOnly(PROP_ID, id);
		m_CameraActivity = cameraActivity;
		
		// add call-backs
		if(!m_CameraActivity.get(CameraActivity.PROP_IS_CAMERA_THREAD_STARTED))
		{
			m_IsCameraThreadStartedCallback = new PropertyChangedCallback<Boolean>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
				{
					m_IsCameraThreadStartedCallback = null;
					source.removeCallback(key, this);
					onCameraThreadStarted();
				}
			};
			cameraActivity.addCallback(CameraActivity.PROP_IS_CAMERA_THREAD_STARTED, m_IsCameraThreadStartedCallback);
		}
	}
	
	
	// Change state.
	private State changeState(State state)
	{
		State oldState = m_State;
		if(oldState != state)
		{
			m_State = state;
			this.notifyPropertyChanged(PROP_STATE, oldState, state);
		}
		return m_State;
	}
	
	
	/**
	 * Disable this mode until calling {@link #enable()}.
	 */
	protected final void disable()
	{
		this.verifyAccess();
		switch(m_State)
		{
			case DISABLED:
			case RELEASED:
				return;
			case EXITED:
				Log.w(TAG, "disable()");
				break;
			default:
				Log.w(TAG, "disable() - Current state is " + m_State);
				break;
		}
		this.changeState(State.DISABLED);
	}
	
	
	/**
	 * Enable this mode.
	 */
	protected final void enable()
	{
		this.verifyAccess();
		if(m_State != State.DISABLED)
			return;
		Log.w(TAG, "enable()");
		this.changeState(State.EXITED);
	}
	
	
	// Enter to this mode.
	@Override
	public final boolean enter(T prevMode, int flags)
	{
		// check state
		this.verifyAccess();
		if(m_State != State.EXITED)
		{
			Log.e(TAG, "enter() - Current state is " + m_State);
			return false;
		}
		
		// change state
		if(this.changeState(State.ENTERING) != State.ENTERING)
		{
			Log.e(TAG, "enter() - Entering process was interrupted");
			return false;
		}
		
		// enter
		try
		{
			if(!this.onEnter(prevMode, flags))
			{
				Log.e(TAG, "enter() - Fail to enter");
				if(m_State == State.ENTERING)
					this.changeState(State.EXITED);
				return false;
			}
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "enter() - Fail to enter", ex);
			if(m_State == State.ENTERING)
				this.changeState(State.EXITED);
			return false;
		}
		
		// change state
		if(m_State != State.ENTERING || this.changeState(State.ENTERED) != State.ENTERED)
		{
			Log.e(TAG, "enter() - Entering process was interrupted");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	// Exit from this mode.
	@Override
	public final void exit(T nextMode, int flags)
	{
		// check state
		this.verifyAccess();
		if(m_State != State.ENTERED)
			return;
		
		// change state
		if(this.changeState(State.EXITING) != State.EXITING)
		{
			Log.w(TAG, "exit() - Exiting process was interrupted");
			return;
		}
		
		// exit
		try
		{
			this.onExit(nextMode, flags);
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "exit() - Unhandled exception occurred while exiting", ex);
		}
		
		// change state
		if(m_State == State.EXITING)
			this.changeState(State.EXITED);
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_STATE)
			return (TValue)m_State;
		return super.get(key);
	}
	
	
	/**
	 * Get current primary camera.
	 * @return Primary camera.
	 */
	protected final Camera getCamera()
	{
		return m_CameraActivity.get(CameraActivity.PROP_CAMERA);
	}
	
	
	/**
	 * Get related camera activity.
	 * @return Camera activity.
	 */
	public final CameraActivity getCameraActivity()
	{
		return m_CameraActivity;
	}
	
	
	/**
	 * Get current capture media type.
	 * @return Capture media type.
	 */
	protected final MediaType getMediaType()
	{
		return m_CameraActivity.get(CameraActivity.PROP_MEDIA_TYPE);
	}
	
	
	/**
	 * Check whether mode is entered or not.
	 * @return Whether mode is entered or not.
	 */
	public final boolean isEntered()
	{
		return (m_State == State.ENTERED);
	}
	
	
	/**
	 * Called when camera thread started.
	 */
	protected void onCameraThreadStarted()
	{}
	
	
	/**
	 * Called when entering to this mode.
	 * @param prevMode Previous mode.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CAMERA_PREVIEW_STATE}</li>
	 * </ul>
	 * @return Whether mode enters successfully or not.
	 */
	protected abstract boolean onEnter(T prevMode, int flags);
	
	
	/**
	 * Called when exiting from this mode.
	 * @param nextMode Next mode.
	 * @param flags Flags:
	 * <ul>
	 *   <li>{@link #FLAG_PRESERVE_CAMERA_PREVIEW_STATE}</li>
	 * </ul>
	 */
	protected abstract void onExit(T nextMode, int flags);
	
	
	// Called when releasing.
	@Override
	protected void onRelease()
	{
		// change state
		this.changeState(State.RELEASED);
		
		// remove call-backs
		if(m_IsCameraThreadStartedCallback != null)
		{
			this.getCameraActivity().removeCallback(CameraActivity.PROP_IS_CAMERA_THREAD_STARTED, m_IsCameraThreadStartedCallback);
			m_IsCameraThreadStartedCallback = null;
		}
		
		// call super
		super.onRelease();
	}
	
	
	// Set property value.
	@Override
	protected <TValue> boolean setReadOnly(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_STATE)
			throw new IllegalAccessError("Cannot change mode state.");
		return super.setReadOnly(key, value);
	}
	
	
	// Get string represents this capture mode.
	@Override
	public String toString()
	{
		return this.get(PROP_ID);
	}
}
