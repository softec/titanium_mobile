/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.map;

import java.util.ArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiContext.OnLifecycleEvent;
import org.appcelerator.titanium.TiRootActivity;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.view.Window;

@Kroll.proxy(creatableInModule=MapModule.class)
@Kroll.dynamicApis(properties = {
	TiC.PROPERTY_ANIMATE,
	TiC.PROPERTY_ANNOTATIONS,
	TiC.PROPERTY_MAP_TYPE,
	TiC.PROPERTY_REGION,
	TiC.PROPERTY_REGION_FIT,
	TiC.PROPERTY_USER_LOCATION,
    TiC.PROPERTY_ANCHOR_POINT,
    ViewProxy.PROPERTY_DRAW_SHADOW
})
public class ViewProxy extends TiViewProxy 
	implements OnLifecycleEvent 
{
    public static final String PROPERTY_DRAW_SHADOW = "drawShadow";

	private static LocalActivityManager lam;
	private static Window mapWindow;
	private static OnLifecycleEvent rootLifecycleListener;
	private static final String LCAT = "TiMapViewProxy";
	
	/*
	 * Track whether the map activity has been destroyed (or told to destroy).
	 * Only one map activity may run, so we're tracking its life here.
	 */
	private boolean destroyed = false;

	private TiMapView mapView;
	private ArrayList<TiAnnotation> annotations;
	private ArrayList<TiMapView.SelectedAnnotation> selectedAnnotations;
	
	public ViewProxy(TiContext tiContext) {
		super(tiContext);

		eventManager.addOnEventChangeListener(this);
		tiContext.addOnLifecycleEventListener(this);

		annotations = new ArrayList<TiAnnotation>();
		selectedAnnotations = new ArrayList<TiMapView.SelectedAnnotation>();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		destroyed = false;
		if (lam == null) {
			TiContext tiContext = getTiContext();
			if (tiContext == null) {
				Log.w(LCAT, "MapView proxy context is no longer valid.  Unable to create MapView.");
				return null;
			}
			final TiRootActivity rootActivity = tiContext.getRootActivity();
			if (rootActivity == null) {
				Log.w(LCAT, "Application's root activity has been destroyed.  Unable to create MapView.");
				return null;
			}
			TiContext rootContext = rootActivity.getTiContext();
			if (rootContext == null) {
				Log.w(LCAT, "Application's root context is no longer valid.  Unable to create MapView.");
				return null;
			}
			// We need to know when root activity destroys, since this lam is
			// based on its context;
			rootLifecycleListener = new OnLifecycleEvent()
			{
				public void onStop(Activity activity){}
				public void onStart(Activity activity){}
				public void onResume(Activity activity){}
				public void onPause(Activity activity){}
				public void onDestroy(Activity activity)
				{
					if (activity != null && activity.equals(rootActivity)) {
						lam = null;
					}
				}
			};
			rootContext.addOnLifecycleEventListener(rootLifecycleListener);
			lam = new LocalActivityManager(rootActivity, true);
			lam.dispatchCreate(null);
		}

		if (mapWindow != null) {
			throw new IllegalStateException("MapView already created. Android can support one MapView per Application.");
		}

		TiApplication tiApp = getTiContext().getTiApp();
		Intent intent = new Intent(tiApp, TiMapActivity.class);
		mapWindow = lam.startActivity("TIMAP", intent);
		lam.dispatchResume();
		mapView = new TiMapView(this, mapWindow, annotations, selectedAnnotations);

		Object location = getProperty(TiC.PROPERTY_LOCATION);
		if (location != null)
		{
			if(location instanceof KrollDict)
			{
				mapView.doSetLocation((KrollDict) location);
			}
			else
			{
				Log.e(LCAT, "location is set, but the structure is not correct");
			}
		}

        Object shadow = getProperty(ViewProxy.PROPERTY_DRAW_SHADOW);
        if (shadow != null) {
            if (shadow instanceof Boolean) {
                mapView.doSetDrawShadow((Boolean)shadow);
            } else {
                Log.e(LCAT, "Incorrect drawShadow format " + shadow.getClass().getName());
            }
        }
		mapView.updateAnnotations();

		return mapView;
	}

	@Kroll.method
	public void zoom(int delta) {
		if (mapView != null) {
			mapView.changeZoomLevel(delta);
		}
	}

	@Kroll.method
	public void removeAllAnnotations(@Kroll.argument(optional=true) Boolean updateDisplay)
	{
		annotations.clear();
		if(updateDisplay && mapView != null) {
			mapView.updateAnnotations();
		}
	}

	@Kroll.method
	public String addAnnotation(Object arg, @Kroll.argument(optional=true) Boolean updateDisplay) {
		TiAnnotation annotation = null;
		if (arg instanceof AnnotationProxy) {
			annotation = TiAnnotation.fromAnnotationProxy((AnnotationProxy) arg);
		} else if (arg instanceof KrollDict) {
			annotation = TiAnnotation.fromDict(getTiContext(), (KrollDict) arg);
		} else {
			Log.d(LCAT, "Unexpected annotation type: " + arg.getClass().getName());
		}
		if (annotation != null) {
			annotations.add(annotation);

			if ((updateDisplay == null || updateDisplay.equals(Boolean.TRUE)) && mapView != null) {
				mapView.updateAnnotations();
			}
			return annotation.getId();
		} else {
			return null;
		}
	}

	@Kroll.method
	public ArrayList<String> addAnnotations(Object[] args, @Kroll.argument(optional=true) Boolean updateDisplay)
	{
		ArrayList<TiAnnotation> newAnnotations = new ArrayList<TiAnnotation>();
		ArrayList<String> result = new ArrayList<String>();
		if (args != null && args.length > 0) {
			for (int i = 0; i<args.length; i++) {
				TiAnnotation annotation = null;
				if (args[i] instanceof AnnotationProxy) {
					annotation = TiAnnotation.fromAnnotationProxy((AnnotationProxy) args[i]);
				} else if (args[i] instanceof KrollDict) {
					KrollDict params = (KrollDict) args[i];
					annotation = TiAnnotation.fromDict(getTiContext(), params);
				} else {
					Log.d(LCAT, "Unexpected parameter for addAnnotations " + args[i].getClass().getName());
				}
				if (annotation != null) {
					newAnnotations.add(annotation);
					result.add(annotation.getId());
				}
			}
		}
		annotations.addAll(newAnnotations);
		if (mapView != null && (updateDisplay == null || updateDisplay.equals(Boolean.TRUE))) {
			mapView.updateAnnotations();
		}
		return result;
	}

    @Kroll.method
    public void updateAnnotations() {
        if (mapView != null) {
            mapView.updateAnnotations();
        }
    }

	protected int findAnnotation(String title)
	{
		int existsIndex = -1;
		// Check for existence
		int len = annotations.size();
		for(int i = 0; i < len; i++) {
			TiAnnotation a = annotations.get(i);

			if (a.getTitle() != null) {
				if (title.equals(a.getTitle())) {
					existsIndex = i;
					break;
				}
			}
		}

		return existsIndex;
	}

    protected int findAnnotationById(String id) {
        return annotations.indexOf(new TiAnnotation(id));
    }

	@Kroll.method
	public void removeAnnotations(Object[] removedAnnotations, @Kroll.argument(optional=true) Boolean updateDisplay) {
		if (removedAnnotations != null && removedAnnotations.length > 0) {
			for (int i=0; i<removedAnnotations.length; i++) {
                removeAnnotation(removedAnnotations[i], false);
			}
		}

		if (mapView != null && (updateDisplay == null || updateDisplay.equals(Boolean.TRUE))) {
			mapView.updateAnnotations();
		}
	}

    @Kroll.method
	public void removeAnnotation(Object arg, @Kroll.argument(optional = true) Boolean updateDisplay)
	{
		String title = null;
		String id = null;

		if (arg != null) {
			if (arg instanceof AnnotationProxy) {
				AnnotationProxy ap = (AnnotationProxy) arg;
				if (ap.getProperties().containsKeyAndNotNull(TiC.PROPERTY_ID)) {
					id = ap.getProperties().getString(TiC.PROPERTY_ID);
				} else {
					title = TiConvert.toString(((AnnotationProxy) arg).getProperty("title"));
				}
			} else if (arg instanceof KrollDict) {
				KrollDict dict = (KrollDict) arg;
				if (dict.containsKeyAndNotNull(TiC.PROPERTY_ID)) {
					id = dict.getString(TiC.PROPERTY_ID);
				} else {
					title = dict.getString(TiC.PROPERTY_TITLE);
				}
			} else if (arg instanceof String) {
				title = (String) arg;
			} else {
				Log.d(LCAT, "Unsupported parameter: " + arg.getClass().getSimpleName());
			}
		}

		if (id != null) {
			if (annotations.remove(new TiAnnotation(id))) {
				if ((updateDisplay == null || updateDisplay.equals(Boolean.TRUE)) && mapView != null) {
					mapView.updateAnnotations();
				}
			} else {
				Log.d(LCAT, "Unable to find annotation having id: " + id);
			}
		} else if (title != null) {
			int existsIndex = findAnnotation(title);
			if (existsIndex > -1) {
				annotations.remove(existsIndex);
                if ((updateDisplay == null || updateDisplay.equals(Boolean.TRUE)) && mapView != null) {
                    mapView.updateAnnotations();
                }
			}
		}
	}

	@Kroll.method
	public void selectAnnotation(Object[] args)
	{
		String title = null;
        String id = null;
		boolean animate = false;
		boolean center = true; // keep existing default behavior

		if (args != null && args.length > 0) {
			if (args[0] instanceof KrollDict) {
				KrollDict params = (KrollDict)args[0];

				Object selectedAnnotation = params.get(TiC.PROPERTY_ANNOTATION);
				if(selectedAnnotation instanceof AnnotationProxy) {
                    AnnotationProxy ap = (AnnotationProxy) selectedAnnotation;
                    if (ap.getProperties().containsKeyAndNotNull(TiC.PROPERTY_ID)) {
                        id = ap.getProperties().getString(TiC.PROPERTY_ID);
                    }
                    if (ap.getProperties().containsKeyAndNotNull(TiC.PROPERTY_TITLE)) {
                        title = TiConvert.toString(ap.getProperty(TiC.PROPERTY_TITLE));
                    }
				} else if (selectedAnnotation instanceof KrollDict) {
                    KrollDict dict = (KrollDict) selectedAnnotation;
                    if (dict.containsKeyAndNotNull(TiC.PROPERTY_ID)) {
					    id = dict.getString(TiC.PROPERTY_ID);
                    }
                    if (dict.containsKeyAndNotNull(TiC.PROPERTY_TITLE)) {
					    title = dict.getString(TiC.PROPERTY_TITLE);
                    }
                } else {
					title = params.getString(TiC.PROPERTY_TITLE);
				}

				if (params.containsKeyAndNotNull(TiC.PROPERTY_ANIMATE)) {
					animate = params.getBoolean(TiC.PROPERTY_ANIMATE);
				}
				if (params.containsKeyAndNotNull(TiC.PROPERTY_CENTER)) {
					center = params.getBoolean(TiC.PROPERTY_CENTER);
				}

			} else {
				if (args[0] instanceof AnnotationProxy) {
                    AnnotationProxy ap = (AnnotationProxy) args[0];
                    if (ap.getProperties().containsKeyAndNotNull(TiC.PROPERTY_ID)) {
                        id = ap.getProperties().getString(TiC.PROPERTY_ID);
                    }
                    if (ap.getProperties().containsKeyAndNotNull(TiC.PROPERTY_TITLE)) {
					    title = TiConvert.toString(ap.getProperty(TiC.PROPERTY_TITLE));
                    }

				} else if (args[0] instanceof String) {
					title = TiConvert.toString(args[0]);
				}

				if (args.length > 1) {
					animate = TiConvert.toBoolean(args[1]);
				}
			}
		}

		if (title != null || id != null) {
			if (mapView == null) {
				Log.e(LCAT, "calling selectedAnnotations.add");
				selectedAnnotations.add(new TiMapView.SelectedAnnotation(id, title, animate, center));
			} else {
				Log.e(LCAT, "calling selectedAnnotations.add2");
				mapView.selectAnnotation(true, id, title, animate, center);
			}
		} else {
            Log.d(LCAT, "Unable to retrieve title or id from the parameter");
        }
	}

	@Kroll.method
	public void deselectAnnotation(Object[] args)
	{
		String title = null;
        String id = null;
		if (args.length > 0) {
			if (args[0] instanceof AnnotationProxy) {
                AnnotationProxy ap = (AnnotationProxy) args[0];
                if (ap.getProperties().containsKeyAndNotNull(TiC.PROPERTY_ID)) {
                    id = ap.getProperties().getString(TiC.PROPERTY_ID);
                }
                if (ap.getProperties().containsKeyAndNotNull(TiC.PROPERTY_TITLE)) {
				    title = TiConvert.toString(((AnnotationProxy) args[0]).getProperty("title"));
                }
			} else if (args[0] instanceof KrollDict) {
                KrollDict dict = (KrollDict) args[0];
                if (dict.containsKeyAndNotNull(TiC.PROPERTY_ID)) {
				    id = dict.getString(TiC.PROPERTY_ID);
                }
                if (dict.containsKeyAndNotNull(TiC.PROPERTY_TITLE)) {
				    title = dict.getString(TiC.PROPERTY_TITLE);
                }
            } if (args[0] instanceof String) {
				title = TiConvert.toString(args[0]);
			}
		}
		if (title != null || id != null) {
			boolean animate = false;

			if (args.length > 1) {
				animate = TiConvert.toBoolean(args[1]);
			}

			if (mapView == null) {
				int numSelectedAnnotations = selectedAnnotations.size();
				for(int i = 0; i < numSelectedAnnotations; i++) {
                    if (id != null) {
                        TiMapView.SelectedAnnotation ann = selectedAnnotations.get(i);
                        if (ann.id != null && ann.id.equals(id)) {
                            selectedAnnotations.remove(i);
                        }
                    } else if((selectedAnnotations.get(i)).title.equals(title)) {
						selectedAnnotations.remove(i);
	    		    }
				}
			} else {
				mapView.selectAnnotation(false, id, title, animate, false);
			}
		}
	}

	@Kroll.method
	public void setLocation(KrollDict location)
	{
		setProperty(TiC.PROPERTY_LOCATION, location);

		if(mapView != null)
		{
			mapView.doSetLocation(location);
		}
	}

	@Kroll.method
	public void setMapType(int mapType)
	{
		this.setProperty(TiC.PROPERTY_MAP_TYPE, mapType, true);
	}

    @Kroll.method
    public void setDrawShadow(boolean shadow) {
        setProperty(ViewProxy.PROPERTY_DRAW_SHADOW, shadow);
        if (mapView != null) {
            mapView.doSetDrawShadow(shadow);
        }
    }

	public void onDestroy(Activity activity) {
		if (lam != null && !destroyed) {
			destroyed = true;
			lam.dispatchDestroy(true);
			lam.destroyActivity("TIMAP", true);
		}
		mapWindow = null;
	}

	public void onPause(Activity activity) {
		if (lam != null) {
			lam.dispatchPause(false);
		}
	}

	public void onResume(Activity activity) {
		if (lam != null) {
			lam.dispatchResume();
		}
	}

	public void onStart(Activity activity) {
	}

	public void onStop(Activity activity) {
		if (lam != null) {
			lam.dispatchStop();
		}
	}

	@Override
	public void releaseViews()
	{
		super.releaseViews();
		onDestroy(null);
	}
}
