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
package org.eclipse.swt.internal.win32;

public class NMTVITEMCHANGE extends NMHDR {
	public int uChanged;
	/** @field cast=(HTREEITEM) */
	public long /*int*/ hItem;
	public int uStateNew;
	public int uStateOld;
	public long /*int*/ lParam;
	public static int sizeof = OS.NMTVITEMCHANGE_sizeof ();
}
