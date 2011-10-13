/*
 * Copyright 2011 SOFTEC sa. All rights reserved.
 *
 * This source code is licensed under the Creative Commons
 * Attribution-NonCommercial-NoDerivs 3.0 Luxembourg
 * License.
 *
 * To view a copy of this license, visit
 * http://creativecommons.org/licenses/by-nc-nd/3.0/lu/
 * or send a letter to Creative Commons, 171 Second Street,
 * Suite 300, San Francisco, California, 94105, USA.
 */

package ti.modules.titanium.map;

import java.util.UUID;

import android.graphics.drawable.Drawable;
import com.google.android.maps.GeoPoint;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConvert;

import android.graphics.Color;

/**
 * Class representing an Titanium annotation
 * @author OlivierD
 *
 */
public class TiAnnotation {
    private static final String LCAT = "TiAnnotation";

	private String id;
	private String title;
	private String subtitle;
	private Double latitude;
	private Double longitude;
	private String image;
	private Integer pinColor;
	private String leftButton;
	private String rightButton;
	private TiViewProxy leftView;
	private TiViewProxy rightView;
	private Boolean     animate;
	private Boolean     center;

	public TiAnnotation() {
		id = UUID.randomUUID().toString();
	}
	
	public TiAnnotation(String id) {
		this.id = id;
	}
		
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getLeftButton() {
		return leftButton;
	}

	public void setLeftButton(String leftButton) {
		this.leftButton = leftButton;
	}

	public String getRightButton() {
		return rightButton;
	}

	public void setRightButton(String rightButton) {
		this.rightButton = rightButton;
	}

	public TiViewProxy getLeftView() {
		return leftView;
	}

	public void setLeftView(TiViewProxy leftView) {
		this.leftView = leftView;
	}

	public TiViewProxy getRightView() {
		return rightView;
	}

	public void setRightView(TiViewProxy rightView) {
		this.rightView = rightView;
	}

	public void setPinColor(String color) {
		if (color == null) {
			this.pinColor = null;
		} else {
			this.pinColor = TiConvert.toColor((String) color);
		}
	}
	
	public void setPinColor(Integer color) {
		// Assume it's a numeric
		switch(color.intValue()) {
			case 1 : // RED
				this.pinColor = Color.RED;
				break;
			case 2 : // GREEN
				this.pinColor = Color.GREEN;
				break;
			case 3 : // PURPLE
				this.pinColor = Color.argb(255,192,0,192);
				break;
            case MapModule.ANNOTATION_BLUE:
                this.pinColor = Color.BLUE;
                break;
		}
	}
	
	public Integer getPinColor() {
		return pinColor;
	}
	
	public Boolean getAnimate() {
		return animate;
	}

	public void setAnimate(Boolean animate) {
		this.animate = animate;
	}

	
	public Boolean getCenter() {
		return center;
	}

	public void setCenter(Boolean center) {
		this.center = center;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public static TiAnnotation fromAnnotationProxy(AnnotationProxy ap) {
		return fromDict(ap.getProperties());
	}
	
	
	
	@Override
	public String toString() {
		return "TiAnnotation [id=" + id + ", title=" + title + ", subtitle="
				+ subtitle + ", latitude=" + latitude + ", longitude="
				+ longitude + ", image=" + image + ", pinColor=" + pinColor
				+ ", animate=" + animate + ", center=" + center + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TiAnnotation other = (TiAnnotation) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public static TiAnnotation fromDict(final KrollDict dict) {
		TiAnnotation a = null;
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_ID)) {
			a = new TiAnnotation(dict.getString(TiC.PROPERTY_ID));
		} else {
			a = new TiAnnotation();
			dict.put(TiC.PROPERTY_ID, a.getId());
		}
		
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_TITLE)) {
			a.setTitle(dict.getString(TiC.PROPERTY_TITLE));
		}
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_SUBTITLE)) {
			a.setSubtitle(dict.getString(TiC.PROPERTY_SUBTITLE));
		}
		
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_LATITUDE)) {
			a.setLatitude(dict.getDouble(TiC.PROPERTY_LATITUDE));
		}
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_LONGITUDE)) {
			a.setLongitude(dict.getDouble(TiC.PROPERTY_LONGITUDE));
		}
		
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_IMAGE) || dict.containsKeyAndNotNull(TiC.PROPERTY_PIN_IMAGE)) {
			a.setImage(dict.getString(TiC.PROPERTY_IMAGE));
			if (a.getImage() == null) {
				a.setImage(dict.getString(TiC.PROPERTY_PIN_IMAGE));
			}
		}
		
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_PINCOLOR)) {
			Object pinColor = dict.get(TiC.PROPERTY_PINCOLOR);
			if (pinColor instanceof String) {
				a.setPinColor((String) pinColor);
			} else  {
                try {
				    a.setPinColor(dict.getInt(TiC.PROPERTY_PINCOLOR));
                } catch (NumberFormatException e) {
                    Log.e("TiAnnotation", "Unable to retrieve pin color", e);
                }
			}
		}
		
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_LEFT_BUTTON)) {
			a.setLeftButton(TiConvert.toString(dict, TiC.PROPERTY_LEFT_BUTTON));
		}
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_RIGHT_BUTTON)) {
			a.setRightButton(TiConvert.toString(dict, TiC.PROPERTY_RIGHT_BUTTON));
		}
		
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_LEFT_VIEW)) {
			Object leftView = dict.get(TiC.PROPERTY_LEFT_VIEW);
			if (leftView instanceof TiViewProxy) {
				a.setLeftView((TiViewProxy)leftView);
			}			
		}
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_RIGHT_VIEW)) {
			Object rightView = dict.get(TiC.PROPERTY_RIGHT_VIEW);
			if (rightView instanceof TiViewProxy) {
				a.setRightView((TiViewProxy)rightView);
			}
		}
		
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_ANIMATE)) {
			a.setAnimate(dict.getBoolean(TiC.PROPERTY_ANIMATE));
		} else {
			a.setAnimate(Boolean.FALSE);
		}
		
		if (dict.containsKeyAndNotNull(TiC.PROPERTY_CENTER)) {
			a.setCenter(dict.getBoolean(TiC.PROPERTY_CENTER));
		} else {
			a.setCenter(Boolean.TRUE);
		}
		return a;
	}

}
