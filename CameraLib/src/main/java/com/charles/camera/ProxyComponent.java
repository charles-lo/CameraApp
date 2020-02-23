package com.charles.camera;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.os.Message;
import android.os.SystemClock;

import com.charles.base.EventArgs;
import com.charles.base.EventHandler;
import com.charles.base.EventKey;
import com.charles.base.EventSource;
import com.charles.base.Handle;
import com.charles.base.HandlerUtils;
import com.charles.base.Log;
import com.charles.base.PropertyChangeEventArgs;
import com.charles.base.PropertyChangedCallback;
import com.charles.base.PropertyKey;
import com.charles.base.PropertySource;
import com.charles.base.RecyclableObject;
import com.charles.base.component.Component;
import com.charles.base.component.ComponentOwner;
import com.charles.base.component.ComponentSearchCallback;
import com.charles.base.component.ComponentUtils;

/**
 * Base class for proxy component.
 * @param <TTarget> Type of target component.
 */
public abstract class ProxyComponent<TTarget extends Component> extends CameraComponent
{
	// Constants.
	private static final int MSG_TARGET_EVENT_RAISED = -10000;
	private static final int MSG_TARGET_PROPERTY_CHANGED = -10001;
	
	
	// Private fields.
	private boolean m_IsBindingToTarget;
	private final LinkedList<AsyncMethodCallHandle> m_PendingAsyncMethodCalls = new LinkedList<>();
	private TTarget m_Target;
	private final Class<? extends TTarget> m_TargetClass;
	private ComponentOwner m_TargetOwner;
	
	
	// Call-backs.
	private PropertyChangedCallback<Boolean> m_IsCameraThreadStartedCallback;
	private final ComponentSearchCallback<TTarget> m_TargetSearchCallback = new ComponentSearchCallback<TTarget>()
	{
		@Override
		public void onComponentFound(TTarget component)
		{
			onTargetFound(component);
		}
	};
	
	
	// Class for asynchronous method call.
	private final class AsyncMethodCallHandle extends Handle
	{
		public final Object[] args;
		public Runnable callingRunnable;
		public volatile Handle resultHandle;
		public final Method method;
		
		public AsyncMethodCallHandle(Method method, Object... args)
		{
			super("AsyncMethodCall");
			this.method = method;
			this.args = args;
		}

		@Override
		protected void onClose(int flags)
		{
			cancelCallingTargetMethod(this);
		}
	}
	
	
	/**
	 * Initialize new ProxyComponent instance.
	 * @param name Component name.
	 * @param activity {@link CameraActivity} instance.
	 * @param targetOwner Target component owner.
	 * @param targetType Type of target component.
	 */
	protected ProxyComponent(String name, CameraActivity activity, ComponentOwner targetOwner, Class<? extends TTarget> targetType)
	{
		super(name, activity, true);
		if(targetOwner == null)
			throw new IllegalArgumentException("No target component owner.");
		if(targetType == null)
			throw new IllegalArgumentException("No target type.");
		m_TargetClass = targetType;
		m_TargetOwner = targetOwner;
	}
	
	
	/**
	 * Initialize new ProxyComponent instance.
	 * @param name Component name.
	 * @param cameraThread {@link CameraThread} instance.
	 * @param targetOwner Target component owner.
	 * @param targetType Type of target component.
	 */
	protected ProxyComponent(String name, CameraThread cameraThread, ComponentOwner targetOwner, Class<? extends TTarget> targetType)
	{
		super(name, cameraThread, true);
		if(targetOwner == null)
			throw new IllegalArgumentException("No target component owner.");
		if(targetType == null)
			throw new IllegalArgumentException("No target type.");
		m_TargetClass = targetType;
		m_TargetOwner = targetOwner;
	}
	
	
	/**
	 * Start binding to target component explicitly.
	 * @return Whether binding starts successfully or not.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected final boolean bindToTarget()
	{
		// check state
		this.verifyAccess();
		if(m_Target != null || m_IsBindingToTarget)
			return true;
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "bindToTarget() - Component is not running");
			return false;
		}
		
		// start binding
		if(m_TargetOwner instanceof CameraThread)
		{
			CameraActivity cameraActivity = this.getCameraActivity();
			if(cameraActivity.isDependencyThread() && !cameraActivity.get(CameraActivity.PROP_IS_CAMERA_THREAD_STARTED))
			{
				Log.v(TAG, "bindToTarget() - Start binding when camera thread starts");
				m_IsBindingToTarget = true;
				m_IsCameraThreadStartedCallback = new PropertyChangedCallback<Boolean>()
				{
					@Override
					public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
					{
						onCameraThreadStarted();
					}
				};
				cameraActivity.addCallback(CameraActivity.PROP_IS_CAMERA_THREAD_STARTED, m_IsCameraThreadStartedCallback);
				return true;
			}
		}
		if(!ComponentUtils.findComponent(m_TargetOwner, (Class)m_TargetClass, this, m_TargetSearchCallback))
			m_IsBindingToTarget = true;
		
		// complete
		return true;
	}
	
	
	/**
	 * Call method in target component.
	 * @param methodName Method name.
	 * @param paramTypes Type of method parameters.
	 * @param args Arguments.
	 * @return Handle to method call.
	 */
	protected final Handle callTargetMethod(String methodName, Class<?>[] paramTypes, Object... args)
	{
		// check parameter
		if(methodName == null)
			throw new IllegalArgumentException("No target method name");
		
		// find method
		Method method;
		try
		{
			method = m_TargetClass.getMethod(methodName, paramTypes);
		}
		catch(Throwable ex)
		{
			Log.e(TAG, "callTargetMethod() - Cannot find method '" + methodName + "'", ex);
			return null;
		}
		
		// create handle
		final AsyncMethodCallHandle handle = new AsyncMethodCallHandle(method, args);
		
		// call
		if(m_Target != null && m_Target.isDependencyThread())
			this.callTargetMethod(handle);
		else
		{
			synchronized(m_PendingAsyncMethodCalls)
			{
				m_PendingAsyncMethodCalls.add(handle);
			}
			handle.callingRunnable = new Runnable()
			{
				@Override
				public void run()
				{
					synchronized(m_PendingAsyncMethodCalls)
					{
						m_PendingAsyncMethodCalls.remove(handle);
					}
					callTargetMethod(handle);
				}
			};
			HandlerUtils.post(m_TargetOwner, handle.callingRunnable);
		}
		
		// complete
		return handle;
	}
	
	
	// Call target method in target thread.
	private void callTargetMethod(AsyncMethodCallHandle handle)
	{
		try
		{
			Object result = handle.method.invoke(m_Target, handle.args);
			if(result instanceof Handle)
			{
				synchronized(handle)
				{
					if(Handle.isValid(handle))
						handle.resultHandle = (Handle)result;
					else
						Handle.close((Handle)result);
				}
			}
		} 
		catch (Throwable ex)
		{
			Log.e(TAG, "callTargetMethod() - Fail to call target method", ex);
			throw new RuntimeException("Fail to call target method.", ex);
		}
	}
	
	
	// Cancel calling target method.
	private void cancelCallingTargetMethod(final AsyncMethodCallHandle handle)
	{
		synchronized(m_PendingAsyncMethodCalls)
		{
			if(m_PendingAsyncMethodCalls.remove(handle))
				return;
		}
		synchronized(handle)
		{
			if(handle.callingRunnable != null)
				HandlerUtils.removeCallbacks(m_TargetOwner, handle.callingRunnable);
			if(Handle.isValid(handle.resultHandle))
			{
				if(!HandlerUtils.post(m_TargetOwner, new Runnable()
				{
					@Override
					public void run()
					{
						Handle.close(handle.resultHandle);
					}
				}))
				{
					Log.e(TAG, "cancelCallingTargetMethod() - Fail to close internal handle asynchronously");
				}
			}
		}
	}
	
	
	/**
	 * Get bound target component.
	 * @return Target component.
	 */
	protected final TTarget getTarget()
	{
		return m_Target;
	}
	
	
	/**
	 * Get owner of target component.
	 * @return Target component owner.
	 */
	protected final ComponentOwner getTargetOwner()
	{
		return m_TargetOwner;
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_TARGET_EVENT_RAISED:
			{
				Object[] array = (Object[])msg.obj;
				this.onTargetEventRaised((Long)array[0], (EventKey<?>)array[1], (EventArgs)array[2]);
				break;
			}
			
			case MSG_TARGET_PROPERTY_CHANGED:
			{
				Object[] array = (Object[])msg.obj;
				this.onTargetPropertyChanged((Long)array[0], (PropertyKey<?>)array[1], (PropertyChangeEventArgs<?>)array[2]);
				break;
			}
			
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	/**
	 * Check whether target component is bound or not.
	 * @return Whether target component is bound or not.
	 */
	protected final boolean isTargetBound()
	{
		return (m_Target != null);
	}
	
	
	/**
	 * Called before binding to events owned by target component.
	 * @param keys Event keys to bind.
	 */
	protected void onBindingToTargetEvents(List<EventKey<?>> keys)
	{}
	
	
	/**
	 * Called before binding to properties owned by target component.
	 * @param keys Property keys to bind.
	 */
	protected void onBindingToTargetProperties(List<PropertyKey<?>> keys)
	{}
	
	
	// Deinitialize.
	@Override
	protected void onDeinitialize()
	{
		if(m_IsCameraThreadStartedCallback != null)
		{
			this.getCameraActivity().removeCallback(CameraActivity.PROP_IS_CAMERA_THREAD_STARTED, m_IsCameraThreadStartedCallback);
			m_IsCameraThreadStartedCallback = null;
		}
		super.onDeinitialize();
	}
	
	
	// Initialize.
	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		this.bindToTarget();
	}
	
	
	/**
	 * Called when binding to target component finished.
	 * @param target Target component.
	 */
	protected void onTargetBound(TTarget target)
	{}
	
	
	// Called when camera thread started.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void onCameraThreadStarted()
	{
		// check state
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "onCameraThreadStarted() - Component is not running");
			return;
		}
		if(!m_IsBindingToTarget)
			return;
		
		// start binding
		Log.v(TAG, "onCameraThreadStarted() - Start binding");
		ComponentUtils.findComponent(m_TargetOwner, (Class)m_TargetClass, this, m_TargetSearchCallback);
	}
	
	
	// Called when target component is found.
	private void onTargetFound(final TTarget target)
	{
		// check state
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "onTargetFound() - Component is not running");
			return;
		}
		
		// update state
		m_Target = target;
		m_IsBindingToTarget = false;
		
		// get bound events and properties
		final ArrayList<EventKey<?>> eventKeys = new ArrayList<>();
		final ArrayList<PropertyKey<?>> propertyKeys = new ArrayList<>();
		this.onBindingToTargetEvents(eventKeys);
		this.onBindingToTargetProperties(propertyKeys);
		
		// bind to target events and properties
		if(!eventKeys.isEmpty() || !propertyKeys.isEmpty())
		{
			if(!HandlerUtils.post(m_TargetOwner, new Runnable()
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void run()
				{
					// bind to events
					if(!eventKeys.isEmpty())
					{
						EventHandler handler = new EventHandler()
						{
							@Override
							public void onEventReceived(EventSource source, EventKey key, EventArgs e)
							{
								HandlerUtils.sendMessage(ProxyComponent.this, MSG_TARGET_EVENT_RAISED, 0, 0, new Object[]{ SystemClock.elapsedRealtimeNanos(), key, e.clone() });
							}
						};
						for(int i = eventKeys.size() - 1 ; i >= 0 ; --i)
							target.addHandler(eventKeys.get(i), handler);
					}
					
					// bind to properties
					if(!propertyKeys.isEmpty())
					{
						PropertyChangedCallback callback = new PropertyChangedCallback()
						{
							@Override
							public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
							{
								HandlerUtils.sendMessage(ProxyComponent.this, MSG_TARGET_PROPERTY_CHANGED, 0, 0, new Object[]{ SystemClock.elapsedRealtimeNanos(), key, e.clone() });
							}
						};
						for(int i = propertyKeys.size() - 1 ; i >= 0 ; --i)
						{
							PropertyKey<?> key = propertyKeys.get(i);
							Object value = target.get(key);
							target.addCallback(key, callback);
							HandlerUtils.sendMessage(ProxyComponent.this, MSG_TARGET_PROPERTY_CHANGED, 0, 0, new Object[]{ SystemClock.elapsedRealtimeNanos(), key, PropertyChangeEventArgs.obtain(key.defaultValue, value) });
						}
					}
					
					// call target methods
					AsyncMethodCallHandle[] asyncCallHandles = null;
					synchronized(m_PendingAsyncMethodCalls)
					{
						if(!m_PendingAsyncMethodCalls.isEmpty())
						{
							asyncCallHandles = new ProxyComponent.AsyncMethodCallHandle[m_PendingAsyncMethodCalls.size()];
							m_PendingAsyncMethodCalls.toArray(asyncCallHandles);
							m_PendingAsyncMethodCalls.clear();
						}
					}
					if(asyncCallHandles != null)
					{
						for(int i = 0, count = asyncCallHandles.length ; i < count ; ++i)
							callTargetMethod(asyncCallHandles[i]);
					}
				}
			}))
			{
				Log.e(TAG, "onTargetFound() - Fail to bind to target events and properties asynchronously");
				m_Target = null;
				return;
			}
		}
		
		// complete
		this.onTargetBound(target);
	}
	
	
	/**
	 * Called after target event raised.
	 * @param time Event time, use {@link SystemClock#elapsedRealtimeNanos()} to check with current time.
	 * @param key Event key.
	 * @param e Event data.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void onTargetEventRaised(long time, EventKey<?> key, EventArgs e)
	{
		this.raise((EventKey)key, e);
		if(e instanceof RecyclableObject)
			((RecyclableObject)e).recycle();
	}
	
	
	/**
	 * Called after target property changed.
	 * @param time Changed time, use {@link SystemClock#elapsedRealtimeNanos()} to check with current time.
	 * @param key Property key.
	 * @param e Event data.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void onTargetPropertyChanged(long time, PropertyKey<?> key, PropertyChangeEventArgs<?> e)
	{
		if(key != PROP_IS_RELEASED)
		{
			if(key.isReadOnly())
				super.setReadOnly((PropertyKey)key, e.getNewValue());
			else
				super.set((PropertyKey)key, e.getNewValue());
		}
		e.recycle();
	}
}
