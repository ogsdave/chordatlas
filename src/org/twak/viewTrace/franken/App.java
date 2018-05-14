package org.twak.viewTrace.franken;


import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;

import org.twak.tweed.gen.SuperFace;
import org.twak.tweed.gen.skel.MiniRoof;
import org.twak.utils.collections.MultiMap;
import org.twak.utils.geom.DRectangle;
import org.twak.viewTrace.facades.FRect;
import org.twak.viewTrace.facades.HasApp;
import org.twak.viewTrace.facades.MiniFacade;

public abstract class App /*earance*/ implements Cloneable {
	
	public enum TextureUVs {
		SQUARE, ZERO_ONE, Rectangle;
	}
	
	public enum AppMode {
		Color, Bitmap, Parent, Net
	}
	
	public AppMode appMode = AppMode.Color;
	public TextureUVs textureUVs = TextureUVs.SQUARE;
	public Color color = Color.gray;
	
	public String texture;
	
	public double[] styleZ;
	HasApp hasA;
	String name;
	
	// GAN optoins
	String netName;
	int sizeZ;
	int resolution;
	public DRectangle textureRect;
	
	public App( App a ) {
		this.hasA = a.hasA;
		this.appMode = a.appMode;
		this.textureUVs = a.textureUVs;
		this.color = a.color;
		this.texture = a.texture;
		this.styleZ = a.styleZ;
		this.netName = a.netName;
		this.sizeZ = a.sizeZ;
		this.resolution = a.resolution;
		this.name = a.name;
	}
	
	public App( HasApp ha, String name, String netName, int sizeZ, int resolution ) {
		
		this.name = name;
		this.netName = name;
		this.hasA = ha; 
		this.netName = netName;
		this.styleZ = new double[sizeZ];
		this.resolution = resolution;
	}

	public static App createFor(HasApp ha) {
		
		if ( ha.getClass() == MiniFacade.class) {
			return new FacadeApp(ha);
		} else if (ha.getClass() == MiniRoof.class) {
			return new RoofApp(ha);
		} else if (ha.getClass() == FRect.class) {
			return new PanesLabelApp(ha);
		}		
		
		throw new Error("unkown to factory " + ha.getClass().getSimpleName());
	}
	
	public JComponent createUI( Runnable globalUpdate ) {
		return new SelectedApps( this ).createUI(  globalUpdate );
	}

	
	static final int Batch_Size = 16;
	
	public static void computeWithChildren (List<App> todo, int first, Runnable globalUpdate ) {
		
		
		if (first >= todo.size()) {
			System.out.println( "finishing "+ todo.get( 0 ).getClass().getSimpleName() );
			
			globalUpdate.run();
			
			MultiMap< String, App> downs = new MultiMap<>();
			for (App a : todo) 
				downs.putAll ( a.getDown() );
			
			for (String d : downs.keySet()) 
				new Thread( () ->  App.computeWithChildren( downs.get( d ), 0, globalUpdate ) ).start();
		}
		else {
			
		
			List<App> batch = new ArrayList<>();
			
			for ( int i = first; i < Math.min( todo.size(), first + Batch_Size ); i++ ) {
				App app = todo.get( i );
				if (app.appMode == AppMode.Net)
					batch.add( todo.get( i ) );
			}

			if (!batch.isEmpty()) {
				System.out.println( "batch " + first + " "+ todo.get( 0 ).getClass().getSimpleName() );
				batch.get( 0 ).computeBatch ( () -> App.computeWithChildren( todo, first + Batch_Size, globalUpdate ), 
					batch );
			}
			else
				App.computeWithChildren( todo, first + Batch_Size, globalUpdate );
		}
		
	}
	
	
//	public void computeWithChildren( Runnable globalUpdate, Runnable whenDone ) {
//
//		switch ( appMode ) {
//
//		case Color:
//			whenDone.run();
//			break;
//		case Net:
//			
//			break;
//		default:
//			color = Color.red;
//			whenDone.run();
//		}
//	}

	public String zAsString() {
		String zs = "";
		for ( double d : styleZ )
			zs += "_" + d;
		return zs;
	}
	
	public abstract App copy();
	public abstract App getUp();
	public abstract MultiMap<String, App> getDown();
	public abstract void computeBatch(Runnable whenDone, List<App> batch);
	
	
}