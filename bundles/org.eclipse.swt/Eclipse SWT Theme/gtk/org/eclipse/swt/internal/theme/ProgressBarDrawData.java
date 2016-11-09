/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.theme;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;

public class ProgressBarDrawData extends RangeDrawData {

public ProgressBarDrawData() {
	state = new int[1];
}

@Override
void draw(Theme theme, GC gc, Rectangle bounds) {
	long /*int*/ progressHandle = theme.progressHandle;
	long /*int*/ gtkStyle = gtk_widget_get_style (progressHandle);
	long /*int*/ drawable = gc.getGCData().drawable;
	theme.transferClipping(gc, gtkStyle);
	byte[] detail = Converter.wcsToMbcs("trough", true);
	int x = bounds.x, y = bounds.y, width = bounds.width, height = bounds.height;
	gtk_render_box (gtkStyle, drawable, getStateType(DrawData.WIDGET_WHOLE), OS.GTK_SHADOW_IN, null, progressHandle, detail, x, y, width, height);
	int xthichness = OS.gtk_style_get_xthickness(gtkStyle);
	int ythichness = OS.gtk_style_get_ythickness(gtkStyle);
	if ((style & SWT.VERTICAL) != 0) {
		gtk_orientable_set_orientation (progressHandle, OS.GTK_PROGRESS_BOTTOM_TO_TOP);
		x += xthichness;
		width -= xthichness * 2;
		height -= ythichness * 2;
		height *= selection / (float)Math.max(1, (maximum - minimum));
		y += bounds.height - ythichness - height;
	} else {
		gtk_orientable_set_orientation (progressHandle, OS.GTK_PROGRESS_LEFT_TO_RIGHT);
		x += xthichness;
		y += ythichness;
		width -= xthichness * 2;
		height -= ythichness * 2;
		width *= selection / (float)Math.max(1, maximum - minimum);
	}
	detail = Converter.wcsToMbcs("bar", true);
	gtk_render_box (gtkStyle, drawable, OS.GTK_STATE_PRELIGHT, OS.GTK_SHADOW_OUT, null, progressHandle, detail, x, y, width, height);
}

@Override
int getStateType(int part) {
	return OS.GTK_STATE_NORMAL;
}

@Override
int hit(Theme theme, Point position, Rectangle bounds) {
	return bounds.contains(position) ? DrawData.WIDGET_WHOLE : DrawData.WIDGET_NOWHERE;
}

void gtk_orientable_set_orientation (long /*int*/ pbar, int orientation) {
	if (OS.GTK3) {
		switch (orientation) {
			case OS.GTK_PROGRESS_BOTTOM_TO_TOP:
				OS.gtk_orientable_set_orientation(pbar, OS.GTK_ORIENTATION_VERTICAL);
				OS.gtk_progress_bar_set_inverted(pbar, true);
				break;
			case OS.GTK_PROGRESS_LEFT_TO_RIGHT:
				OS.gtk_orientable_set_orientation(pbar, OS.GTK_ORIENTATION_HORIZONTAL);
				OS.gtk_progress_bar_set_inverted(pbar, false);
				break;
		}
	} else {
		OS.gtk_progress_bar_set_orientation(pbar, orientation);
	}
}

}
