/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.map;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.TiConfig;

@Kroll.module
public class MapModule extends KrollModule
{
	private static final String LCAT = "TiMap";
	private static final boolean DBG = TiConfig.LOGD;

	@Kroll.constant public static final int ANNOTATION_RED = 1;
	@Kroll.constant public static final int ANNOTATION_GREEN = 2;
	@Kroll.constant public static final int ANNOTATION_PURPLE = 3;
    @Kroll.constant public static final int ANNOTATION_BLUE = 4;

	@Kroll.constant public static final int STANDARD_TYPE = TiMapView.MAP_VIEW_STANDARD;
	@Kroll.constant public static final int SATELLITE_TYPE = TiMapView.MAP_VIEW_SATELLITE;
	@Kroll.constant public static final int HYBRID_TYPE = TiMapView.MAP_VIEW_HYBRID;

	public MapModule(TiContext tiContext)
	{
		super(tiContext);
	}
}
