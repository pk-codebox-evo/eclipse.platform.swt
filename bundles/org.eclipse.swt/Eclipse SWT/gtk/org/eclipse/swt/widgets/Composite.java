/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.widgets;


import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.cairo.*;
import org.eclipse.swt.internal.gtk.*;

/**
 * Instances of this class are controls which are capable
 * of containing other controls.
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>NO_BACKGROUND, NO_FOCUS, NO_MERGE_PAINTS, NO_REDRAW_RESIZE, NO_RADIO_GROUP, EMBEDDED, DOUBLE_BUFFERED</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * <p>
 * Note: The <code>NO_BACKGROUND</code>, <code>NO_FOCUS</code>, <code>NO_MERGE_PAINTS</code>,
 * and <code>NO_REDRAW_RESIZE</code> styles are intended for use with <code>Canvas</code>.
 * They can be used with <code>Composite</code> if you are drawing your own, but their
 * behavior is undefined if they are used with subclasses of <code>Composite</code> other
 * than <code>Canvas</code>.
 * </p><p>
 * Note: The <code>CENTER</code> style, although undefined for composites, has the
 * same value as <code>EMBEDDED</code> which is used to embed widgets from other
 * widget toolkits into SWT.  On some operating systems (GTK), this may cause
 * the children of this composite to be obscured.
 * </p><p>
 * This class may be subclassed by custom control implementors
 * who are building controls that are constructed from aggregates
 * of other controls.
 * </p>
 *
 * @see Canvas
 * @see <a href="http://www.eclipse.org/swt/snippets/#composite">Composite snippets</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 */
public class Composite extends Scrollable {
	/**
	 * the handle to the OS resource
	 * (Warning: This field is platform dependent)
	 * <p>
	 * <b>IMPORTANT:</b> This field is <em>not</em> part of the SWT
	 * public API. It is marked public only so that it can be shared
	 * within the packages provided by SWT. It is not available on all
	 * platforms and should never be accessed from application code.
	 * </p>
	 *
	 * @noreference This field is not intended to be referenced by clients.
	 */
	public long /*int*/  embeddedHandle;
	long /*int*/ imHandle, socketHandle;
	Layout layout;
	Control[] tabList;
	int layoutCount, backgroundMode;

	static final String NO_INPUT_METHOD = "org.eclipse.swt.internal.gtk.noInputMethod"; //$NON-NLS-1$

Composite () {
	/* Do nothing */
}

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a widget which will be the parent of the new instance (cannot be null)
 * @param style the style of widget to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 * </ul>
 *
 * @see SWT#NO_BACKGROUND
 * @see SWT#NO_FOCUS
 * @see SWT#NO_MERGE_PAINTS
 * @see SWT#NO_REDRAW_RESIZE
 * @see SWT#NO_RADIO_GROUP
 * @see SWT#EMBEDDED
 * @see SWT#DOUBLE_BUFFERED
 * @see Widget#getStyle
 */
public Composite (Composite parent, int style) {
	super (parent, checkStyle (style));
}

static int checkStyle (int style) {
	if (OS.INIT_CAIRO) {
		style &= ~SWT.NO_BACKGROUND;
	}
	style &= ~SWT.TRANSPARENT;
	return style;
}

Control [] _getChildren () {
	long /*int*/ parentHandle = parentingHandle ();
	long /*int*/ list = OS.gtk_container_get_children (parentHandle);
	if (list == 0) return new Control [0];
	int count = OS.g_list_length (list);
	Control [] children = new Control [count];
	int i = 0;
	long /*int*/ temp = list;
	while (temp != 0) {
		long /*int*/ handle = OS.g_list_data (temp);
		if (handle != 0) {
			Widget widget = display.getWidget (handle);
			if (widget != null && widget != this) {
				if (widget instanceof Control) {
					children [i++] = (Control) widget;
				}
			}
		}
		temp = OS.g_list_next (temp);
	}
	OS.g_list_free (list);
	if (i == count) return children;
	Control [] newChildren = new Control [i];
	System.arraycopy (children, 0, newChildren, 0, i);
	return newChildren;
}

Control [] _getTabList () {
	if (tabList == null) return tabList;
	int count = 0;
	for (int i=0; i<tabList.length; i++) {
		if (!tabList [i].isDisposed ()) count++;
	}
	if (count == tabList.length) return tabList;
	Control [] newList = new Control [count];
	int index = 0;
	for (int i=0; i<tabList.length; i++) {
		if (!tabList [i].isDisposed ()) {
			newList [index++] = tabList [i];
		}
	}
	tabList = newList;
	return tabList;
}

/**
 * Clears any data that has been cached by a Layout for all widgets that
 * are in the parent hierarchy of the changed control up to and including the
 * receiver.  If an ancestor does not have a layout, it is skipped.
 *
 * @param changed an array of controls that changed state and require a recalculation of size
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the changed array is null any of its controls are null or have been disposed</li>
 *    <li>ERROR_INVALID_PARENT - if any control in changed is not in the widget tree of the receiver</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @deprecated use {@link Composite#layout(Control[], int)} instead
 * @since 3.1
 */
@Deprecated
public void changed (Control[] changed) {
	layout(changed, SWT.DEFER);
}

@Override
void checkBuffered () {
	if ((style & SWT.DOUBLE_BUFFERED) == 0 && (style & SWT.NO_BACKGROUND) != 0) {
		return;
	}
	super.checkBuffered();
}

@Override
protected void checkSubclass () {
	/* Do nothing - Subclassing is allowed */
}

@Override
long /*int*/ childStyle () {
	if (scrolledHandle != 0) return 0;
	return super.childStyle ();
}

@Override
Point computeSizeInPixels (int wHint, int hHint, boolean changed) {
	checkWidget ();
	display.runSkin();
	if (wHint != SWT.DEFAULT && wHint < 0) wHint = 0;
	if (hHint != SWT.DEFAULT && hHint < 0) hHint = 0;
	Point size;
	if (layout != null) {
		if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT) {
			changed |= (state & LAYOUT_CHANGED) != 0;
			size = DPIUtil.autoScaleUp(layout.computeSize (this, DPIUtil.autoScaleDown(wHint), DPIUtil.autoScaleDown(hHint), changed));
			state &= ~LAYOUT_CHANGED;
		} else {
			size = new Point (wHint, hHint);
		}
	} else {
		size = minimumSize (wHint, hHint, changed);
		if (size.x == 0) size.x = DEFAULT_WIDTH;
		if (size.y == 0) size.y = DEFAULT_HEIGHT;
	}
	if (wHint != SWT.DEFAULT) size.x = wHint;
	if (hHint != SWT.DEFAULT) size.y = hHint;
	Rectangle trim = DPIUtil.autoScaleUp (computeTrim (0, 0, DPIUtil.autoScaleDown(size.x), DPIUtil.autoScaleDown(size.y)));
	return new Point (trim.width, trim.height);
}

@Override
Widget [] computeTabList () {
	Widget result [] = super.computeTabList ();
	if (result.length == 0) return result;
	Control [] list = tabList != null ? _getTabList () : _getChildren ();
	for (int i=0; i<list.length; i++) {
		Control child = list [i];
		Widget [] childList = child.computeTabList ();
		if (childList.length != 0) {
			Widget [] newResult = new Widget [result.length + childList.length];
			System.arraycopy (result, 0, newResult, 0, result.length);
			System.arraycopy (childList, 0, newResult, result.length, childList.length);
			result = newResult;
		}
	}
	return result;
}

@Override
void createHandle (int index) {
	state |= HANDLE | CANVAS | CHECK_SUBWINDOW;
	boolean scrolled = (style & (SWT.H_SCROLL | SWT.V_SCROLL)) != 0;
	if (!scrolled) state |= THEME_BACKGROUND;
	createHandle (index, true, scrolled || (style & SWT.BORDER) != 0);
}

@Override
int applyThemeBackground () {
	return (backgroundAlpha == 0 || (style & (SWT.H_SCROLL | SWT.V_SCROLL)) == 0) ? 1 : 0;
}

void createHandle (int index, boolean fixed, boolean scrolled) {
	if (scrolled) {
		if (fixed) {
			fixedHandle = OS.g_object_new (display.gtk_fixed_get_type (), 0);
			if (fixedHandle == 0) error (SWT.ERROR_NO_HANDLES);
			OS.gtk_widget_set_has_window (fixedHandle, true);
		}
		long /*int*/ vadj = OS.gtk_adjustment_new (0, 0, 100, 1, 10, 10);
		if (vadj == 0) error (SWT.ERROR_NO_HANDLES);
		long /*int*/ hadj = OS.gtk_adjustment_new (0, 0, 100, 1, 10, 10);
		if (hadj == 0) error (SWT.ERROR_NO_HANDLES);
		scrolledHandle = OS.gtk_scrolled_window_new (hadj, vadj);
		if (scrolledHandle == 0) error (SWT.ERROR_NO_HANDLES);
	}
	handle = OS.g_object_new (display.gtk_fixed_get_type (), 0);
	if (handle == 0) error (SWT.ERROR_NO_HANDLES);
	OS.gtk_widget_set_has_window (handle, true);
	OS.gtk_widget_set_can_focus (handle, true);
	if ((style & SWT.EMBEDDED) == 0) {
		if ((state & CANVAS) != 0) {
			/* Prevent an input method context from being created for the Browser widget */
			if (display.getData (NO_INPUT_METHOD) == null) {
				imHandle = OS.gtk_im_multicontext_new ();
				if (imHandle == 0) error (SWT.ERROR_NO_HANDLES);
			}
		}
	}
	if (scrolled) {
		if (fixed) OS.gtk_container_add (fixedHandle, scrolledHandle);
		/*
		* Force the scrolledWindow to have a single child that is
		* not scrolled automatically.  Calling gtk_container_add()
		* seems to add the child correctly but cause a warning.
		*/
		boolean warnings = display.getWarnings ();
		display.setWarnings (false);
		OS.gtk_container_add (scrolledHandle, handle);
		display.setWarnings (warnings);

		int hsp = (style & SWT.H_SCROLL) != 0 ? OS.GTK_POLICY_ALWAYS : OS.GTK_POLICY_NEVER;
		int vsp = (style & SWT.V_SCROLL) != 0 ? OS.GTK_POLICY_ALWAYS : OS.GTK_POLICY_NEVER;
		OS.gtk_scrolled_window_set_policy (scrolledHandle, hsp, vsp);
		if (hasBorder ()) {
			OS.gtk_scrolled_window_set_shadow_type (scrolledHandle, OS.GTK_SHADOW_ETCHED_IN);
		}
	}
	if ((style & SWT.EMBEDDED) != 0) {
		if (!OS.isX11()) {
			if (Device.DEBUG) {
				new SWTError(SWT.ERROR_INVALID_ARGUMENT,"SWT.EMBEDDED is currently not yet supported in Wayland. \nPlease "
					+ "refer to https://bugs.eclipse.org/bugs/show_bug.cgi?id=514487 for development status.").printStackTrace();
			}
		} else {
			socketHandle = OS.gtk_socket_new ();
			if (socketHandle == 0) error (SWT.ERROR_NO_HANDLES);
			OS.gtk_container_add (handle, socketHandle);
		}
	}
	if ((style & SWT.NO_REDRAW_RESIZE) != 0 && (style & SWT.RIGHT_TO_LEFT) == 0) {
		OS.gtk_widget_set_redraw_on_allocate (handle, false);
	}
	/*
	* Bug in GTK.  When a widget is double buffered and the back
	* pixmap is null, the double buffer pixmap is filled with the
	* background of the widget rather than the current contents of
	* the screen.  If nothing is drawn during an expose event,
	* the pixels are altered.  The fix is to clear double buffering
	* when NO_BACKGROUND is set and DOUBLE_BUFFERED
	* is not explicitly set.
	*/
	if ((style & SWT.DOUBLE_BUFFERED) == 0 && (style & SWT.NO_BACKGROUND) != 0) {
		OS.gtk_widget_set_double_buffered (handle, false);
	}
}

@Override
long /*int*/ gtk_draw (long /*int*/ widget, long /*int*/ cairo) {
	if (OS.GTK_VERSION >= OS.VERSION(3, 16, 0)) {
		long /*int*/ context = OS.gtk_widget_get_style_context(widget);
		GtkAllocation allocation = new GtkAllocation();
		OS.gtk_widget_get_allocation (widget, allocation);
		int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		int height = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
		// We specify a 0 value for x & y as we want the whole widget to be
		// colored, not some portion of it.
		OS.gtk_render_background(context, cairo, 0, 0, width, height);
	}
	return super.gtk_draw(widget, cairo);
}

@Override
void deregister () {
	super.deregister ();
	if (socketHandle != 0) display.removeWidget (socketHandle);
}

/**
 * Fills the interior of the rectangle specified by the arguments,
 * with the receiver's background.
 *
 * <p>The <code>offsetX</code> and <code>offsetY</code> are used to map from
 * the <code>gc</code> origin to the origin of the parent image background. This is useful
 * to ensure proper alignment of the image background.</p>
 *
 * @param gc the gc where the rectangle is to be filled
 * @param x the x coordinate of the rectangle to be filled
 * @param y the y coordinate of the rectangle to be filled
 * @param width the width of the rectangle to be filled
 * @param height the height of the rectangle to be filled
 * @param offsetX the image background x offset
 * @param offsetY the image background y offset
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the gc is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the gc has been disposed</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.6
 */
public void drawBackground (GC gc, int x, int y, int width, int height, int offsetX, int offsetY) {
	checkWidget();
	Rectangle rect = DPIUtil.autoScaleUp(new Rectangle (x, y, width, height));
	offsetX = DPIUtil.autoScaleUp(offsetX);
	offsetY = DPIUtil.autoScaleUp(offsetY);
	drawBackgroundInPixels(gc, rect.x, rect.y, rect.width, rect.height, offsetX, offsetY);
}

void drawBackgroundInPixels (GC gc, int x, int y, int width, int height, int offsetX, int offsetY) {
	checkWidget ();
	if (gc == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (gc.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
	Control control = findBackgroundControl ();
	if (control != null) {
		GCData data = gc.getGCData ();
		long /*int*/ cairo = data.cairo;
		if (cairo != 0) {
			Cairo.cairo_save (cairo);
			if (control.backgroundImage != null) {
				Point pt = display.mapInPixels (this, control, 0, 0);
				Cairo.cairo_translate (cairo, -pt.x - offsetX, -pt.y - offsetY);
				x += pt.x + offsetX;
				y += pt.y + offsetY;
				long /*int*/ surface = control.backgroundImage.surface;
				if (surface == 0) {
					long /*int*/ drawable = control.backgroundImage.pixmap;
					int [] w = new int [1], h = new int [1];
					OS.gdk_pixmap_get_size (drawable, w, h);
					if (OS.isX11()) {
						long /*int*/ xDisplay = OS.gdk_x11_display_get_xdisplay(OS.gdk_display_get_default());
						long /*int*/ xVisual = OS.gdk_x11_visual_get_xvisual (OS.gdk_visual_get_system());
						long /*int*/ xDrawable = OS.GDK_PIXMAP_XID (drawable);
						surface = Cairo.cairo_xlib_surface_create (xDisplay, xDrawable, xVisual, w [0], h [0]);
					} else {
						surface = Cairo.cairo_image_surface_create(Cairo.CAIRO_FORMAT_ARGB32, w [0], h [0]);
					}
					if (surface == 0) error (SWT.ERROR_NO_HANDLES);
				} else {
					Cairo.cairo_surface_reference(surface);
				}
				long /*int*/ pattern = Cairo.cairo_pattern_create_for_surface (surface);
				if (pattern == 0) error (SWT.ERROR_NO_HANDLES);
				Cairo.cairo_pattern_set_extend (pattern, Cairo.CAIRO_EXTEND_REPEAT);
				if ((data.style & SWT.MIRRORED) != 0) {
					double[] matrix = {-1, 0, 0, 1, 0, 0};
					Cairo.cairo_pattern_set_matrix(pattern, matrix);
				}
				Cairo.cairo_set_source (cairo, pattern);
				Cairo.cairo_surface_destroy (surface);
				Cairo.cairo_pattern_destroy (pattern);
			} else {
				GdkColor color = control.getBackgroundColor ();
				GdkRGBA rgba = display.toGdkRGBA (color);
				Cairo.cairo_set_source_rgba (cairo, rgba.red, rgba.green, rgba.blue, rgba.alpha);
			}
			Cairo.cairo_rectangle (cairo, x, y, width, height);
			Cairo.cairo_fill (cairo);
			Cairo.cairo_restore (cairo);
		} else {
			long /*int*/ gdkGC = gc.handle;
			GdkGCValues values = new GdkGCValues ();
			OS.gdk_gc_get_values (gdkGC, values);
			if (control.backgroundImage != null) {
				Point pt = display.mapInPixels (this, control, 0, 0);
				OS.gdk_gc_set_fill (gdkGC, OS.GDK_TILED);
				OS.gdk_gc_set_ts_origin (gdkGC, -pt.x - offsetX, -pt.y - offsetY);
				OS.gdk_gc_set_tile (gdkGC, control.backgroundImage.pixmap);
				OS.gdk_draw_rectangle (data.drawable, gdkGC, 1, x, y, width, height);
				OS.gdk_gc_set_fill (gdkGC, values.fill);
				OS.gdk_gc_set_ts_origin (gdkGC, values.ts_x_origin, values.ts_y_origin);
			} else {
				GdkColor color = control.getBackgroundColor ();
				OS.gdk_gc_set_foreground (gdkGC, color);
				OS.gdk_draw_rectangle (data.drawable, gdkGC, 1, x, y, width, height);
				color.pixel = values.foreground_pixel;
				OS.gdk_gc_set_foreground (gdkGC, color);
			}
		}
	} else {
		gc.fillRectangle(DPIUtil.autoScaleDown(new Rectangle(x, y, width, height)));

	}
}

@Override
void enableWidget (boolean enabled) {
	if ((state & CANVAS) != 0) return;
	super.enableWidget (enabled);
}

Composite findDeferredControl () {
	return layoutCount > 0 ? this : parent.findDeferredControl ();
}

@Override
Menu [] findMenus (Control control) {
	if (control == this) return new Menu [0];
	Menu result [] = super.findMenus (control);
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		Control child = children [i];
		Menu [] menuList = child.findMenus (control);
		if (menuList.length != 0) {
			Menu [] newResult = new Menu [result.length + menuList.length];
			System.arraycopy (result, 0, newResult, 0, result.length);
			System.arraycopy (menuList, 0, newResult, result.length, menuList.length);
			result = newResult;
		}
	}
	return result;
}

@Override
void fixChildren (Shell newShell, Shell oldShell, Decorations newDecorations, Decorations oldDecorations, Menu [] menus) {
	super.fixChildren (newShell, oldShell, newDecorations, oldDecorations, menus);
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		children [i].fixChildren (newShell, oldShell, newDecorations, oldDecorations, menus);
	}
}

@Override
void fixModal(long /*int*/ group, long /*int*/ modalGroup)  {
	Control[] controls = _getChildren ();
	for (int i = 0; i < controls.length; i++) {
		controls[i].fixModal (group, modalGroup);
	}
}

@Override
void fixStyle () {
	super.fixStyle ();
	if (scrolledHandle == 0) fixStyle (handle);
	Control[] children = _getChildren ();
	for (int i = 0; i < children.length; i++) {
		children [i].fixStyle ();
	}
}

void fixTabList (Control control) {
	if (tabList == null) return;
	int count = 0;
	for (int i=0; i<tabList.length; i++) {
		if (tabList [i] == control) count++;
	}
	if (count == 0) return;
	Control [] newList = null;
	int length = tabList.length - count;
	if (length != 0) {
		newList = new Control [length];
		int index = 0;
		for (int i=0; i<tabList.length; i++) {
			if (tabList [i] != control) {
				newList [index++] = tabList [i];
			}
		}
	}
	tabList = newList;
}

void fixZOrder () {
	if ((state & CANVAS) != 0) return;
	long /*int*/ parentHandle = parentingHandle ();
	long /*int*/ parentWindow = gtk_widget_get_window (parentHandle);
	if (parentWindow == 0) return;
	long /*int*/ [] userData = new long /*int*/ [1];
	long /*int*/ windowList = OS.gdk_window_get_children (parentWindow);
	if (windowList != 0) {
		long /*int*/ windows = windowList;
		while (windows != 0) {
			long /*int*/ window = OS.g_list_data (windows);
			if (window != redrawWindow) {
				OS.gdk_window_get_user_data (window, userData);
				if (userData [0] == 0 || OS.G_OBJECT_TYPE (userData [0]) != display.gtk_fixed_get_type ()) {
					OS.gdk_window_lower (window);
				}
			}
			windows = OS.g_list_next (windows);
		}
		OS.g_list_free (windowList);
	}
}

@Override
long /*int*/ focusHandle () {
	if (socketHandle != 0) return socketHandle;
	return super.focusHandle ();
}

@Override
boolean forceFocus (long /*int*/ focusHandle) {
	if (socketHandle != 0) OS.gtk_widget_set_can_focus (focusHandle, true);
	boolean result = super.forceFocus (focusHandle);
	if (socketHandle != 0) OS.gtk_widget_set_can_focus (focusHandle, false);
	return result;
}

/**
 * Returns the receiver's background drawing mode. This
 * will be one of the following constants defined in class
 * <code>SWT</code>:
 * <code>INHERIT_NONE</code>, <code>INHERIT_DEFAULT</code>,
 * <code>INHERIT_FORCE</code>.
 *
 * @return the background mode
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SWT
 *
 * @since 3.2
 */
public int getBackgroundMode () {
	checkWidget ();
	return backgroundMode;
}

/**
 * Returns a (possibly empty) array containing the receiver's children.
 * Children are returned in the order that they are drawn.  The topmost
 * control appears at the beginning of the array.  Subsequent controls
 * draw beneath this control and appear later in the array.
 * <p>
 * Note: This is not the actual structure used by the receiver
 * to maintain its list of children, so modifying the array will
 * not affect the receiver.
 * </p>
 *
 * @return an array of children
 *
 * @see Control#moveAbove
 * @see Control#moveBelow
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Control [] getChildren () {
	checkWidget();
	return _getChildren ();
}

int getChildrenCount () {
	/*
	* NOTE: The current implementation will count
	* non-registered children.
	*/
	long /*int*/ list = OS.gtk_container_get_children (handle);
	if (list == 0) return 0;
	int count = OS.g_list_length (list);
	OS.g_list_free (list);
	return count;
}

@Override
Rectangle getClientAreaInPixels () {
	checkWidget();
	if ((state & CANVAS) != 0) {
		if ((state & ZERO_WIDTH) != 0 && (state & ZERO_HEIGHT) != 0) {
			return new Rectangle (0, 0, 0, 0);
		}
		forceResize ();
		long /*int*/ clientHandle = clientHandle ();
		GtkAllocation allocation = new GtkAllocation();
		OS.gtk_widget_get_allocation (clientHandle, allocation);
		int width = (state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		int height = (state & ZERO_HEIGHT) != 0 ? 0 : allocation.height;
		return new Rectangle (0, 0, width, height);
	}
	return super.getClientAreaInPixels();
}

/**
 * Returns layout which is associated with the receiver, or
 * null if one has not been set.
 *
 * @return the receiver's layout or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public Layout getLayout () {
	checkWidget();
	return layout;
}

/**
 * Returns <code>true</code> if the receiver has deferred
 * the performing of layout, and <code>false</code> otherwise.
 *
 * @return the receiver's deferred layout state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #setLayoutDeferred(boolean)
 * @see #isLayoutDeferred()
 *
 * @since 3.1
 */
public boolean getLayoutDeferred () {
	checkWidget ();
	return layoutCount > 0 ;
}

/**
 * Gets the (possibly empty) tabbing order for the control.
 *
 * @return tabList the ordered list of controls representing the tab order
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #setTabList
 */
public Control [] getTabList () {
	checkWidget ();
	Control [] tabList = _getTabList ();
	if (tabList == null) {
		int count = 0;
		Control [] list =_getChildren ();
		for (int i=0; i<list.length; i++) {
			if (list [i].isTabGroup ()) count++;
		}
		tabList = new Control [count];
		int index = 0;
		for (int i=0; i<list.length; i++) {
			if (list [i].isTabGroup ()) {
				tabList [index++] = list [i];
			}
		}
	}
	return tabList;
}

@Override
long /*int*/ gtk_button_press_event (long /*int*/ widget, long /*int*/ event) {
	long /*int*/ result = super.gtk_button_press_event (widget, event);
	if (result != 0) return result;
	if ((state & CANVAS) != 0) {
		if ((style & SWT.NO_FOCUS) == 0 && hooksKeys ()) {
			GdkEventButton gdkEvent = new GdkEventButton ();
			OS.memmove (gdkEvent, event, GdkEventButton.sizeof);
			if (gdkEvent.button == 1) {
				if (getChildrenCount () == 0) setFocus ();
			}
		}
	}
	return result;
}

@Override
long /*int*/ gtk_expose_event (long /*int*/ widget, long /*int*/ eventPtr) {
	if ((state & OBSCURED) != 0) return 0;
	if ((state & CANVAS) == 0) {
		return super.gtk_expose_event (widget, eventPtr);
	}
	if ((style & SWT.NO_MERGE_PAINTS) == 0) {
		return super.gtk_expose_event (widget, eventPtr);
	}
	if (!hooksPaint ()) return 0;
	GdkEventExpose gdkEvent = new GdkEventExpose ();
	OS.memmove(gdkEvent, eventPtr, GdkEventExpose.sizeof);
	long /*int*/ [] rectangles = new long /*int*/ [1];
	int [] n_rectangles = new int [1];
	OS.gdk_region_get_rectangles (gdkEvent.region, rectangles, n_rectangles);
	GdkRectangle rect = new GdkRectangle ();
	for (int i=0; i<n_rectangles[0]; i++) {
		Event event = new Event ();
		OS.memmove (rect, rectangles [0] + i * GdkRectangle.sizeof, GdkRectangle.sizeof);
		event.setBounds (DPIUtil.autoScaleDown (new Rectangle(rect.x, rect.y, rect.width, rect.height)));
		if ((style & SWT.MIRRORED) != 0) event.x = DPIUtil.autoScaleDown (getClientWidth ()) - event.width - event.x;
		long /*int*/ damageRgn = OS.gdk_region_new ();
		OS.gdk_region_union_with_rect (damageRgn, rect);
		GCData data = new GCData ();
		data.damageRgn = damageRgn;
		GC gc = event.gc = GC.gtk_new (this, data);
		sendEvent (SWT.Paint, event);
		gc.dispose ();
		OS.gdk_region_destroy (damageRgn);
		event.gc = null;
	}
	OS.g_free (rectangles [0]);
	return 0;
}

@Override
long /*int*/ gtk_key_press_event (long /*int*/ widget, long /*int*/ event) {
	long /*int*/ result = super.gtk_key_press_event (widget, event);
	if (result != 0) return result;
	/*
	* Feature in GTK.  The default behavior when the return key
	* is pressed is to select the default button.  This is not the
	* expected behavior for Composite and its subclasses.  The
	* fix is to avoid calling the default handler.
	*/
	if ((state & CANVAS) != 0 && socketHandle == 0) {
		GdkEventKey keyEvent = new GdkEventKey ();
		OS.memmove (keyEvent, event, GdkEventKey.sizeof);
		int key = keyEvent.keyval;
		switch (key) {
			case OS.GDK_Return:
			case OS.GDK_KP_Enter: return 1;
		}
	}
	return result;
}

@Override
long /*int*/ gtk_focus (long /*int*/ widget, long /*int*/ directionType) {
	if (widget == socketHandle) return 0;
	return super.gtk_focus (widget, directionType);
}

@Override
long /*int*/ gtk_focus_in_event (long /*int*/ widget, long /*int*/ event) {
	long /*int*/ result = super.gtk_focus_in_event (widget, event);
	return (state & CANVAS) != 0 ? 1 : result;
}

@Override
long /*int*/ gtk_focus_out_event (long /*int*/ widget, long /*int*/ event) {
	long /*int*/ result = super.gtk_focus_out_event (widget, event);
	return (state & CANVAS) != 0 ? 1 : result;
}

@Override
long /*int*/ gtk_map (long /*int*/ widget) {
	fixZOrder ();
	return 0;
}

@Override
long /*int*/ gtk_realize (long /*int*/ widget) {
	long /*int*/ result = super.gtk_realize (widget);
	if ((style & SWT.NO_BACKGROUND) != 0) {
		long /*int*/ window = gtk_widget_get_window (paintHandle ());
		if (window != 0) {
			if (OS.GTK3) {
				OS.gdk_window_set_background_pattern(window, 0);
			} else {
				OS.gdk_window_set_back_pixmap (window, 0, false);
			}
		}
	}
	if (socketHandle != 0) {
		embeddedHandle = OS.gtk_socket_get_id (socketHandle);
	}
	return result;
}

@Override
long /*int*/ gtk_scroll_child (long /*int*/ widget, long /*int*/ scrollType, long /*int*/ horizontal) {
	/* Stop GTK scroll child signal for canvas */
	OS.g_signal_stop_emission_by_name (widget, OS.scroll_child);
	return 1;
}

@Override
long /*int*/ gtk_style_set (long /*int*/ widget, long /*int*/ previousStyle) {
	long /*int*/ result = super.gtk_style_set (widget, previousStyle);
	if ((style & SWT.NO_BACKGROUND) != 0) {
		long /*int*/ window = gtk_widget_get_window (paintHandle ());
		if (window != 0) OS.gdk_window_set_back_pixmap (window, 0, false);
	}
	return result;
}

boolean hasBorder () {
	return (style & SWT.BORDER) != 0;
}

@Override
void hookEvents () {
	super.hookEvents ();
	if ((state & CANVAS) != 0) {
		OS.gtk_widget_add_events (handle, OS.GDK_POINTER_MOTION_HINT_MASK);
		if (scrolledHandle != 0) {
			OS.g_signal_connect_closure (scrolledHandle, OS.scroll_child, display.getClosure (SCROLL_CHILD), false);
		}
	}
}

boolean hooksKeys () {
	return hooks (SWT.KeyDown) || hooks (SWT.KeyUp);
}

@Override
long /*int*/ imHandle () {
	return imHandle;
}

/**
 * Returns <code>true</code> if the receiver or any ancestor
 * up to and including the receiver's nearest ancestor shell
 * has deferred the performing of layouts.  Otherwise, <code>false</code>
 * is returned.
 *
 * @return the receiver's deferred layout state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #setLayoutDeferred(boolean)
 * @see #getLayoutDeferred()
 *
 * @since 3.1
 */
public boolean isLayoutDeferred () {
	checkWidget ();
	return findDeferredControl () != null;
}

@Override
boolean isTabGroup() {
	if ((state & CANVAS) != 0) return true;
	return super.isTabGroup();
}

/**
 * If the receiver has a layout, asks the layout to <em>lay out</em>
 * (that is, set the size and location of) the receiver's children.
 * If the receiver does not have a layout, do nothing.
 * <p>
 * Use of this method is discouraged since it is the least-efficient
 * way to trigger a layout. The use of <code>layout(true)</code>
 * discards all cached layout information, even from controls which
 * have not changed. It is much more efficient to invoke
 * {@link Control#requestLayout()} on every control which has changed
 * in the layout than it is to invoke this method on the layout itself.
 * </p>
 * <p>
 * This is equivalent to calling <code>layout(true)</code>.
 * </p>
 * <p>
 * Note: Layout is different from painting. If a child is
 * moved or resized such that an area in the parent is
 * exposed, then the parent will paint. If no child is
 * affected, the parent will not paint.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void layout () {
	checkWidget ();
	layout (true);
}

/**
 * If the receiver has a layout, asks the layout to <em>lay out</em>
 * (that is, set the size and location of) the receiver's children.
 * If the argument is <code>true</code> the layout must not rely
 * on any information it has cached about the immediate children. If it
 * is <code>false</code> the layout may (potentially) optimize the
 * work it is doing by assuming that none of the receiver's
 * children has changed state since the last layout.
 * If the receiver does not have a layout, do nothing.
 * <p>
 * It is normally more efficient to invoke {@link Control#requestLayout()}
 * on every control which has changed in the layout than it is to invoke
 * this method on the layout itself. Clients are encouraged to use
 * {@link Control#requestLayout()} where possible instead of calling
 * this method.
 * </p>
 * <p>
 * If a child is resized as a result of a call to layout, the
 * resize event will invoke the layout of the child.  The layout
 * will cascade down through all child widgets in the receiver's widget
 * tree until a child is encountered that does not resize.  Note that
 * a layout due to a resize will not flush any cached information
 * (same as <code>layout(false)</code>).
 * </p>
 * <p>
 * Note: Layout is different from painting. If a child is
 * moved or resized such that an area in the parent is
 * exposed, then the parent will paint. If no child is
 * affected, the parent will not paint.
 * </p>
 *
 * @param changed <code>true</code> if the layout must flush its caches, and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void layout (boolean changed) {
	checkWidget ();
	if (layout == null) return;
	layout (changed, false);
}

/**
 * If the receiver has a layout, asks the layout to <em>lay out</em>
 * (that is, set the size and location of) the receiver's children.
 * If the changed argument is <code>true</code> the layout must not rely
 * on any information it has cached about its children. If it
 * is <code>false</code> the layout may (potentially) optimize the
 * work it is doing by assuming that none of the receiver's
 * children has changed state since the last layout.
 * If the all argument is <code>true</code> the layout will cascade down
 * through all child widgets in the receiver's widget tree, regardless of
 * whether the child has changed size.  The changed argument is applied to
 * all layouts.  If the all argument is <code>false</code>, the layout will
 * <em>not</em> cascade down through all child widgets in the receiver's widget
 * tree.  However, if a child is resized as a result of a call to layout, the
 * resize event will invoke the layout of the child.  Note that
 * a layout due to a resize will not flush any cached information
 * (same as <code>layout(false)</code>).
 * </p>
 * <p>
 * It is normally more efficient to invoke {@link Control#requestLayout()}
 * on every control which has changed in the layout than it is to invoke
 * this method on the layout itself. Clients are encouraged to use
 * {@link Control#requestLayout()} where possible instead of calling
 * this method.
 * </p>
 * <p>
 * Note: Layout is different from painting. If a child is
 * moved or resized such that an area in the parent is
 * exposed, then the parent will paint. If no child is
 * affected, the parent will not paint.
 * </p>
 *
 * @param changed <code>true</code> if the layout must flush its caches, and <code>false</code> otherwise
 * @param all <code>true</code> if all children in the receiver's widget tree should be laid out, and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.1
 */
public void layout (boolean changed, boolean all) {
	checkWidget ();
	if (layout == null && !all) return;
	markLayout (changed, all);
	updateLayout (all);
}

/**
 * Forces a lay out (that is, sets the size and location) of all widgets that
 * are in the parent hierarchy of the changed control up to and including the
 * receiver.  The layouts in the hierarchy must not rely on any information
 * cached about the changed control or any of its ancestors.  The layout may
 * (potentially) optimize the work it is doing by assuming that none of the
 * peers of the changed control have changed state since the last layout.
 * If an ancestor does not have a layout, skip it.
 * <p>
 * It is normally more efficient to invoke {@link Control#requestLayout()}
 * on every control which has changed in the layout than it is to invoke
 * this method on the layout itself. Clients are encouraged to use
 * {@link Control#requestLayout()} where possible instead of calling
 * this method.
 * </p>
 * <p>
 * Note: Layout is different from painting. If a child is
 * moved or resized such that an area in the parent is
 * exposed, then the parent will paint. If no child is
 * affected, the parent will not paint.
 * </p>
 *
 * @param changed a control that has had a state change which requires a recalculation of its size
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the changed array is null any of its controls are null or have been disposed</li>
 *    <li>ERROR_INVALID_PARENT - if any control in changed is not in the widget tree of the receiver</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.1
 */
public void layout (Control [] changed) {
	checkWidget ();
	if (changed == null) error (SWT.ERROR_INVALID_ARGUMENT);
	layout (changed, SWT.NONE);
}

/**
 * Forces a lay out (that is, sets the size and location) of all widgets that
 * are in the parent hierarchy of the changed control up to and including the
 * receiver.
 * <p>
 * The parameter <code>flags</code> may be a combination of:
 * <dl>
 * <dt><b>SWT.ALL</b></dt>
 * <dd>all children in the receiver's widget tree should be laid out</dd>
 * <dt><b>SWT.CHANGED</b></dt>
 * <dd>the layout must flush its caches</dd>
 * <dt><b>SWT.DEFER</b></dt>
 * <dd>layout will be deferred</dd>
 * </dl>
 * </p>
 * <p>
 * When the <code>changed</code> array is specified, the flags <code>SWT.ALL</code>
 * and <code>SWT.CHANGED</code> have no effect. In this case, the layouts in the
 * hierarchy must not rely on any information cached about the changed control or
 * any of its ancestors.  The layout may (potentially) optimize the
 * work it is doing by assuming that none of the peers of the changed
 * control have changed state since the last layout.
 * If an ancestor does not have a layout, skip it.
 * </p>
 * <p>
 * When the <code>changed</code> array is not specified, the flag <code>SWT.ALL</code>
 * indicates that the whole widget tree should be laid out. And the flag
 * <code>SWT.CHANGED</code> indicates that the layouts should flush any cached
 * information for all controls that are laid out.
 * </p>
 * <p>
 * The <code>SWT.DEFER</code> flag always causes the layout to be deferred by
 * calling <code>Composite.setLayoutDeferred(true)</code> and scheduling a call
 * to <code>Composite.setLayoutDeferred(false)</code>, which will happen when
 * appropriate (usually before the next event is handled). When this flag is set,
 * the application should not call <code>Composite.setLayoutDeferred(boolean)</code>.
 * </p>
 * <p>
 * Note: Layout is different from painting. If a child is
 * moved or resized such that an area in the parent is
 * exposed, then the parent will paint. If no child is
 * affected, the parent will not paint.
 * </p>
 *
 * @param changed a control that has had a state change which requires a recalculation of its size
 * @param flags the flags specifying how the layout should happen
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if any of the controls in changed is null or has been disposed</li>
 *    <li>ERROR_INVALID_PARENT - if any control in changed is not in the widget tree of the receiver</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @since 3.6
 */
public void layout (Control [] changed, int flags) {
	checkWidget ();
	if (changed != null) {
		for (int i=0; i<changed.length; i++) {
			Control control = changed [i];
			if (control == null) error (SWT.ERROR_INVALID_ARGUMENT);
			if (control.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
			boolean ancestor = false;
			Composite composite = control.parent;
			while (composite != null) {
				ancestor = composite == this;
				if (ancestor) break;
				composite = composite.parent;
			}
			if (!ancestor) error (SWT.ERROR_INVALID_PARENT);
		}
		int updateCount = 0;
		Composite [] update = new Composite [16];
		for (int i=0; i<changed.length; i++) {
			Control child = changed [i];
			Composite composite = child.parent;
			// Update layout when the list of children has changed.
			// See bug 497812.
			child.markLayout(false, false);
			while (child != this) {
				if (composite.layout != null) {
					composite.state |= LAYOUT_NEEDED;
					if (!composite.layout.flushCache (child)) {
						composite.state |= LAYOUT_CHANGED;
					}
				}
				if (updateCount == update.length) {
					Composite [] newUpdate = new Composite [update.length + 16];
					System.arraycopy (update, 0, newUpdate, 0, update.length);
					update = newUpdate;
				}
				child = update [updateCount++] = composite;
				composite = child.parent;
			}
		}
		if ((flags & SWT.DEFER) != 0) {
			setLayoutDeferred (true);
			display.addLayoutDeferred (this);
		}
		for (int i=updateCount-1; i>=0; i--) {
			update [i].updateLayout (false);
		}
	} else {
		if (layout == null && (flags & SWT.ALL) == 0) return;
		markLayout ((flags & SWT.CHANGED) != 0, (flags & SWT.ALL) != 0);
		if ((flags & SWT.DEFER) != 0) {
			setLayoutDeferred (true);
			display.addLayoutDeferred (this);
		}
		updateLayout ((flags & SWT.ALL) != 0);
	}
}

@Override
void markLayout (boolean changed, boolean all) {
	if (layout != null) {
		state |= LAYOUT_NEEDED;
		if (changed) state |= LAYOUT_CHANGED;
	}
	if (all) {
		Control [] children = _getChildren ();
		for (int i=0; i<children.length; i++) {
			children [i].markLayout (changed, all);
		}
	}
}

void moveAbove (long /*int*/ child, long /*int*/ sibling) {
	if (child == sibling) return;
	long /*int*/ parentHandle = parentingHandle ();
	if (OS.GTK3) {
		OS.swt_fixed_restack (parentHandle, child, sibling, true);
		return;
	}
	GtkFixed fixed = new GtkFixed ();
	OS.memmove (fixed, parentHandle);
	long /*int*/ children = fixed.children;
	if (children == 0) return;
	long /*int*/ [] data = new long /*int*/ [1];
	long /*int*/ [] widget = new long /*int*/ [1];
	long /*int*/ childData = 0, childLink = 0, siblingLink = 0, temp = children;
	while (temp != 0) {
		OS.memmove (data, temp, OS.PTR_SIZEOF);
		OS.memmove (widget, data [0], OS.PTR_SIZEOF);
		if (child == widget [0]) {
			childLink = temp;
			childData = data [0];
		} else if (sibling == widget [0]) {
			siblingLink = temp;
		}
		if (childData != 0 && (sibling == 0 || siblingLink != 0)) break;
		temp = OS.g_list_next (temp);
	}
	children = OS.g_list_remove_link (children, childLink);
	if (siblingLink == 0 || OS.g_list_previous (siblingLink) == 0) {
		OS.g_list_free_1 (childLink);
		children = OS.g_list_prepend (children, childData);
	} else {
		temp = OS.g_list_previous (siblingLink);
		OS.g_list_set_previous (childLink, temp);
		OS.g_list_set_next (temp, childLink);
		OS.g_list_set_next (childLink, siblingLink);
		OS.g_list_set_previous (siblingLink, childLink);
	}
	fixed.children = children;
	OS.memmove (parentHandle, fixed);
}

void moveBelow (long /*int*/ child, long /*int*/ sibling) {
	if (child == sibling) return;
	long /*int*/ parentHandle = parentingHandle ();
	if (sibling == 0 && parentHandle == fixedHandle) {
		moveAbove (child, scrolledHandle != 0  ? scrolledHandle : handle);
		return;
	}
	if (OS.GTK3) {
		OS.swt_fixed_restack (parentHandle, child, sibling, false);
		return;
	}
	GtkFixed fixed = new GtkFixed ();
	OS.memmove (fixed, parentHandle);
	long /*int*/ children = fixed.children;
	if (children == 0) return;
	long /*int*/ [] data = new long /*int*/ [1];
	long /*int*/ [] widget = new long /*int*/ [1];
	long /*int*/ childData = 0, childLink = 0, siblingLink = 0, temp = children;
	while (temp != 0) {
		OS.memmove (data, temp, OS.PTR_SIZEOF);
		OS.memmove (widget, data [0], OS.PTR_SIZEOF);
		if (child == widget [0]) {
			childLink = temp;
			childData = data [0];
		} else if (sibling == widget [0]) {
			siblingLink = temp;
		}
		if (childData != 0 && (sibling == 0 || siblingLink != 0)) break;
		temp = OS.g_list_next (temp);
	}
	children = OS.g_list_remove_link (children, childLink);
	if (siblingLink == 0 || OS.g_list_next (siblingLink) == 0) {
		OS.g_list_free_1 (childLink);
		children = OS.g_list_append (children, childData);
	} else {
		temp = OS.g_list_next (siblingLink);
		OS.g_list_set_next (childLink, temp);
		OS.g_list_set_previous (temp, childLink);
		OS.g_list_set_previous (childLink, siblingLink);
		OS.g_list_set_next (siblingLink, childLink);
	}
	fixed.children = children;
	OS.memmove (parentHandle, fixed);
}

@Override
void moveChildren(int oldWidth) {
	Control[] children = _getChildren ();
	for (int i = 0; i < children.length; i++) {
		Control child = children[i];
		long /*int*/ topHandle = child.topHandle ();
		GtkAllocation allocation = new GtkAllocation();
		OS.gtk_widget_get_allocation (topHandle, allocation);
		int x = allocation.x;
		int y = allocation.y;
		int controlWidth = (child.state & ZERO_WIDTH) != 0 ? 0 : allocation.width;
		if (oldWidth > 0) x = oldWidth - controlWidth - x;
		int clientWidth = getClientWidth ();
		x = clientWidth - controlWidth - x;
		if (child.enableWindow != 0) {
			OS.gdk_window_move (child.enableWindow, x, y);
		}
		child.moveHandle (x, y);
		/*
		* Cause a size allocation this widget's topHandle.  Note that
		* all calls to gtk_widget_size_allocate() must be preceded by
		* a call to gtk_widget_size_request().
		*/
		GtkRequisition requisition = new GtkRequisition ();
		gtk_widget_size_request (topHandle, requisition);
		allocation.x = x;
		allocation.y = y;
		OS.gtk_widget_size_allocate (topHandle, allocation);
		Control control = child.findBackgroundControl ();
		if (control != null && control.backgroundImage != null) {
			if (child.isVisible ()) child.redrawWidget (0, 0, 0, 0, true, true, true);
		}
	}
}

Point minimumSize (int wHint, int hHint, boolean changed) {
	Control [] children = _getChildren ();
	/*
	 * Since getClientArea can be overridden by subclasses, we cannot
	 * call getClientAreaInPixels directly.
	 */
	Rectangle clientArea = DPIUtil.autoScaleUp(getClientArea ());
	int width = 0, height = 0;
	for (int i=0; i<children.length; i++) {
		Rectangle rect = DPIUtil.autoScaleUp(children [i].getBounds ());
		width = Math.max (width, rect.x - clientArea.x + rect.width);
		height = Math.max (height, rect.y - clientArea.y + rect.height);
	}
	return new Point (width, height);
}

long /*int*/ parentingHandle () {
	if ((state & CANVAS) != 0) return handle;
	return fixedHandle != 0 ? fixedHandle : handle;
}

@Override
void printWidget (GC gc, long /*int*/ drawable, int depth, int x, int y) {
	Region oldClip = new Region (gc.getDevice ());
	Region newClip = new Region (gc.getDevice ());
	Point loc = DPIUtil.autoScaleDown(new Point (x, y));
	gc.getClipping (oldClip);
	Rectangle rect = getBounds ();
	newClip.add (oldClip);
	newClip.intersect (loc.x, loc.y, rect.width, rect.height);
	gc.setClipping (newClip);
	super.printWidget (gc, drawable, depth, x, y);
	Rectangle clientRect = getClientAreaInPixels ();
	Point pt = display.mapInPixels (this, parent, clientRect.x, clientRect.y);
	clientRect.x = x + pt.x - rect.x;
	clientRect.y = y + pt.y - rect.y;
	newClip.intersect (DPIUtil.autoScaleDown(clientRect));
	gc.setClipping (newClip);
	Control [] children = _getChildren ();
	for (int i=children.length-1; i>=0; --i) {
		Control child = children [i];
		if (child.getVisible ()) {
			Point location = child.getLocationInPixels ();
			child.printWidget (gc, drawable, depth, x + location.x, y + location.y);
		}
	}
	gc.setClipping (oldClip);
	oldClip.dispose ();
	newClip.dispose ();
}

@Override
void redrawChildren () {
	super.redrawChildren ();
	Control [] children = _getChildren ();
	for (int i = 0; i < children.length; i++) {
		Control child = children [i];
		if ((child.state & PARENT_BACKGROUND) != 0) {
			child.redrawWidget (0, 0, 0, 0, true, false, true);
			child.redrawChildren ();
		}
	}
}

@Override
void register () {
	super.register ();
	if (socketHandle != 0) display.addWidget (socketHandle, this);
}

@Override
void releaseChildren (boolean destroy) {
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		Control child = children [i];
		if (child != null && !child.isDisposed ()) {
			child.release (false);
		}
	}
	super.releaseChildren (destroy);
}

@Override
void releaseHandle () {
	super.releaseHandle ();
	socketHandle = embeddedHandle = 0;
}

@Override
void releaseWidget () {
	super.releaseWidget ();
	if (imHandle != 0) OS.g_object_unref (imHandle);
	imHandle = 0;
	layout = null;
	tabList = null;
}

void removeControl (Control control) {
	fixTabList (control);
}

@Override
void reskinChildren (int flags) {
	super.reskinChildren (flags);
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		Control child = children [i];
		if (child != null) child.reskin (flags);
	}
}

@Override
void resizeHandle (int width, int height) {
	super.resizeHandle (width, height);
	if (socketHandle != 0) {
		if (OS.GTK3) {
			OS.swt_fixed_resize (handle, socketHandle, width, height);
		} else {
			OS.gtk_widget_set_size_request (socketHandle, width, height);
		}
	}
}

/**
 * Sets the background drawing mode to the argument which should
 * be one of the following constants defined in class <code>SWT</code>:
 * <code>INHERIT_NONE</code>, <code>INHERIT_DEFAULT</code>,
 * <code>INHERIT_FORCE</code>.
 *
 * @param mode the new background mode
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see SWT
 *
 * @since 3.2
 */
public void setBackgroundMode (int mode) {
	checkWidget ();
	backgroundMode = mode;
	Control[] children = _getChildren ();
	for (int i = 0; i < children.length; i++) {
		children [i].updateBackgroundMode ();
	}
}

@Override
int setBounds (int x, int y, int width, int height, boolean move, boolean resize) {

	/* Bug 487757
	 * Deal with bug where for Gtk3.8+, ScrolledComposites appear empty or internal widgets don't
	 * get sized properly. When resizing shell, widgets appear or grow into correct size.
	 *
	 * The issue is that upon widget creation ZERO_HEIGHT/WIDTH is assigned to widget's state (createWidget());
	 * then if you call setVisible(false), setVisible(true) before a layout operation,
	 * in the setVisible(true) call, the widget is never shown. In Gtk3.8+ hidden widgets have a
	 * size of 0x0, and as such allocation is not done correctly until you manually re-size the parent.
	 *
	 * To resolve the issue we check if widget is not HIDDEN on SWT side, but is hidden on GTK side. If it
	 * is, then show it. This correctly initializes layout of internal widgets.
	 *
	 * Future note: If you observe non Composite widgets to have incorrect sizing on first display
	 * but fix themselfes upon first resize by mouse, then
	 * consider moving this fix higher into Control's setBound(...) method instead.
	 */
	long /*int*/ topHandle = topHandle ();
	if (OS.GTK_VERSION >= OS.VERSION (3, 8, 0)
			&& fixedHandle != 0 && handle != 0
			&& getVisible() && !OS.gtk_widget_get_visible(topHandle) //if SWT State is not HIDDEN, but widget is hidden on GTK side.
			&& topHandle == fixedHandle && width > 0 && height > 0 && resize) {
		OS.gtk_widget_show(topHandle);
	}

	int result = super.setBounds (x, y, width, height, move, resize);
	if ((result & RESIZED) != 0 && layout != null) {
		markLayout (false, false);
		updateLayout (false);
	}
	return result;
}

@Override
public boolean setFocus () {
	checkWidget();
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		Control child = children [i];
		if (child.getVisible () && child.setFocus ()) return true;
	}
	return super.setFocus ();
}

/**
 * Sets the layout which is associated with the receiver to be
 * the argument which may be null.
 *
 * @param layout the receiver's new layout or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setLayout (Layout layout) {
	checkWidget();
	this.layout = layout;
}

/**
 * If the argument is <code>true</code>, causes subsequent layout
 * operations in the receiver or any of its children to be ignored.
 * No layout of any kind can occur in the receiver or any of its
 * children until the flag is set to false.
 * Layout operations that occurred while the flag was
 * <code>true</code> are remembered and when the flag is set to
 * <code>false</code>, the layout operations are performed in an
 * optimized manner.  Nested calls to this method are stacked.
 *
 * @param defer the new defer state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see #layout(boolean)
 * @see #layout(Control[])
 *
 * @since 3.1
 */
public void setLayoutDeferred (boolean defer) {
	checkWidget();
	if (!defer) {
		if (--layoutCount == 0) {
			if ((state & LAYOUT_CHILD) != 0 || (state & LAYOUT_NEEDED) != 0) {
				updateLayout (true);
			}
		}
	} else {
		layoutCount++;
	}
}

@Override
void setOrientation (boolean create) {
	super.setOrientation (create);
	if (!create) {
		int flags = SWT.RIGHT_TO_LEFT | SWT.LEFT_TO_RIGHT;
		int orientation = style & flags;
		Control [] children = _getChildren ();
		for (int i=0; i<children.length; i++) {
			children[i].setOrientation (orientation);
		}
		if (((style & SWT.RIGHT_TO_LEFT) != 0) != ((style & SWT.MIRRORED) != 0)) {
			moveChildren (-1);
		}
	}
}

@Override
boolean setScrollBarVisible (ScrollBar bar, boolean visible) {
	boolean changed = super.setScrollBarVisible (bar, visible);
	if (changed && layout != null) {
		markLayout (false, false);
		updateLayout (false);
	}
	return changed;
}

@Override
boolean setTabGroupFocus (boolean next) {
	if (isTabItem ()) return setTabItemFocus (next);
	boolean takeFocus = (style & SWT.NO_FOCUS) == 0;
	if ((state & CANVAS) != 0) takeFocus = hooksKeys ();
	if (socketHandle != 0) takeFocus = true;
	if (takeFocus  && setTabItemFocus (next)) return true;
	Control [] children = _getChildren ();
	for (int i=0; i<children.length; i++) {
		Control child = children [i];
		if (child.isTabItem () && child.setTabItemFocus (next)) return true;
	}
	return false;
}

@Override
boolean setTabItemFocus (boolean next) {
	if (!super.setTabItemFocus (next)) return false;
	if (socketHandle != 0) {
		int direction = next ? OS.GTK_DIR_TAB_FORWARD : OS.GTK_DIR_TAB_BACKWARD;
		OS.GTK_WIDGET_UNSET_FLAGS (socketHandle, OS.GTK_HAS_FOCUS);
		OS.gtk_widget_child_focus (socketHandle, direction);
		OS.GTK_WIDGET_SET_FLAGS (socketHandle, OS.GTK_HAS_FOCUS);
	}
	return true;
}

/**
 * Sets the tabbing order for the specified controls to
 * match the order that they occur in the argument list.
 *
 * @param tabList the ordered list of controls representing the tab order or null
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if a widget in the tabList is null or has been disposed</li>
 *    <li>ERROR_INVALID_PARENT - if widget in the tabList is not in the same widget tree</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 */
public void setTabList (Control [] tabList) {
	checkWidget ();
	if (tabList != null) {
		for (int i=0; i<tabList.length; i++) {
			Control control = tabList [i];
			if (control == null) error (SWT.ERROR_INVALID_ARGUMENT);
			if (control.isDisposed ()) error (SWT.ERROR_INVALID_ARGUMENT);
			if (control.parent != this) error (SWT.ERROR_INVALID_PARENT);
		}
		Control [] newList = new Control [tabList.length];
		System.arraycopy (tabList, 0, newList, 0, tabList.length);
		tabList = newList;
	}
	this.tabList = tabList;
}

@Override
void showWidget () {
	super.showWidget ();
	if (socketHandle != 0) {
		OS.gtk_widget_show (socketHandle);
		embeddedHandle = OS.gtk_socket_get_id (socketHandle);
	}
	if (scrolledHandle == 0) fixStyle (handle);
}

@Override
boolean checkSubwindow () {
	return (state & CHECK_SUBWINDOW) != 0;
}

@Override
boolean translateMnemonic (Event event, Control control) {
	if (super.translateMnemonic (event, control)) return true;
	if (control != null) {
		Control [] children = _getChildren ();
		for (int i=0; i<children.length; i++) {
			Control child = children [i];
			if (child.translateMnemonic (event, control)) return true;
		}
	}
	return false;
}

@Override
int traversalCode(int key, GdkEventKey event) {
	if ((state & CANVAS) != 0) {
		if ((style & SWT.NO_FOCUS) != 0) return 0;
		if (hooksKeys ()) return 0;
	}
	return super.traversalCode (key, event);
}

@Override
boolean translateTraversal (GdkEventKey keyEvent) {
	if (socketHandle != 0) return false;
	return super.translateTraversal (keyEvent);
}

@Override
void updateBackgroundMode () {
	super.updateBackgroundMode ();
	Control [] children = _getChildren ();
	for (int i = 0; i < children.length; i++) {
		children [i].updateBackgroundMode ();
	}
}

@Override
void updateLayout (boolean all) {
	Composite parent = findDeferredControl ();
	if (parent != null) {
		parent.state |= LAYOUT_CHILD;
		return;
	}
	if ((state & LAYOUT_NEEDED) != 0) {
		boolean changed = (state & LAYOUT_CHANGED) != 0;
		state &= ~(LAYOUT_NEEDED | LAYOUT_CHANGED);
		display.runSkin();
		layout.layout (this, changed);
	}
	if (all) {
		state &= ~LAYOUT_CHILD;
		Control [] children = _getChildren ();
		for (int i=0; i<children.length; i++) {
			children [i].updateLayout (all);
		}
	}
}
}
