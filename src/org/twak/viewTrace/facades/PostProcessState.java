package org.twak.viewTrace.facades;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point2d;

import org.hsqldb.lib.HashSet;
import org.twak.utils.collections.Loop;
import org.twak.utils.geom.DRectangle;
import org.twak.viewTrace.facades.GreebleHelper.LPoint2d;

public class PostProcessState {
	
	public PostProcessState( Loop<LPoint2d> flat ) {
		this.skelFaces.add ( flat );
	}

	public PostProcessState() {
		// TODO Auto-generated constructor stub
	}

	// last perimeter from skeleton evaluation
	public List<Loop<? extends Point2d>> skelFaces = new ArrayList<>();
	public DRectangle innerFacadeRect;
	public DRectangle outerFacadeRect;
}
