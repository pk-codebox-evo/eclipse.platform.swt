package org.eclipse.swt.widgets;


/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */

import org.eclipse.swt.internal.photon.*;

class WidgetTable {
	static int FreeSlot = 0;
	static int GrowSize = 1024;
	static int [] IndexTable = new int [GrowSize];
	static Widget [] WidgetTable = new Widget [GrowSize];
	static int ArgPtr = OS.malloc (4);
	static int [] ArgBuffer = new int [1];
	static int [] SetArgs = new int [] {OS.Pt_ARG_USER_DATA, ArgPtr, 4};
	static int [] GetArgs = new int [] {OS.Pt_ARG_USER_DATA, 0, 0};
	static {
		for (int i=0; i<GrowSize-1; i++) IndexTable [i] = i + 1;
		IndexTable [GrowSize - 1] = -1;
	}
public static synchronized Widget get (int handle) {
	if (handle == 0) return null;
	GetArgs [1] = 0;
	OS.PtGetResources (handle, GetArgs.length / 3, GetArgs);
	if (GetArgs [1] == 0) return null;
	OS.memmove (ArgBuffer, GetArgs [1], 4);
	if (ArgBuffer [0] == 0) return null;
	int index = ArgBuffer [0] - 1;
	if (0 <= index && index < WidgetTable.length) return WidgetTable [index];
	return null;
}
public synchronized static void put(int handle, Widget widget) {
	if (handle == 0) return;	
	if (FreeSlot == -1) {
		int length = (FreeSlot = IndexTable.length) + GrowSize;
		int[] newIndexTable = new int[length];
		Widget[] newWidgetTable = new Widget [length];
		System.arraycopy (IndexTable, 0, newIndexTable, 0, FreeSlot);
		System.arraycopy (WidgetTable, 0, newWidgetTable, 0, FreeSlot);
		for (int i = FreeSlot; i < length - 1; i++) {
			newIndexTable[i] = i + 1;
		}
		newIndexTable[length - 1] = -1;
		IndexTable = newIndexTable;
		WidgetTable = newWidgetTable;
	}
	ArgBuffer [0] = FreeSlot + 1;
	OS.memmove (ArgPtr, ArgBuffer, 4);
	OS.PtSetResources (handle, SetArgs.length / 3, SetArgs);
	int oldSlot = FreeSlot;
	FreeSlot = IndexTable[oldSlot];
	IndexTable [oldSlot] = -2;
	WidgetTable [oldSlot] = widget;
}
public static synchronized Widget remove (int handle) {
	if (handle == 0) return null;
	GetArgs [1] = 0;
	OS.PtGetResources (handle, GetArgs.length / 3, GetArgs);
	if (GetArgs [1] == 0) return null;
	OS.memmove (ArgBuffer, GetArgs [1], 4);
	if (ArgBuffer [0] == 0) return null;
	int index = ArgBuffer [0] - 1;
	Widget widget = null;
	if (0 <= index && index < WidgetTable.length) {
		widget = WidgetTable [index];
		WidgetTable [index] = null;
		IndexTable [index] = FreeSlot;
		FreeSlot = index;
		ArgBuffer [0] = 0;
		OS.memmove (ArgPtr, ArgBuffer, 4);
		OS.PtSetResources (handle, SetArgs.length / 3, SetArgs);
	}
	return widget;
}
public static synchronized Shell [] shells () {
	int length = 0;
	for (int i=0; i<WidgetTable.length; i++) {
		Widget widget = WidgetTable [i];
		if (widget != null && widget instanceof Shell) length++;
	}
	int index = 0;
	Shell [] result = new Shell [length];
	for (int i=0; i<WidgetTable.length; i++) {
		Widget widget = WidgetTable [i];
		if (widget != null && widget instanceof Shell) {
			result [index++] = (Shell) widget;
		}
	}
	return result;
}
public static synchronized int size () {
	int size = 0;
	for (int i=0; i<WidgetTable.length; i++) {
		if (WidgetTable [i] != null) size++;
	}
	return size;
}
}
