package org.twak.viewTrace.franken;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.twak.tweed.Tweed;
import org.twak.utils.Imagez;
import org.twak.utils.collections.MultiMap;
import org.twak.utils.geom.DRectangle;
import org.twak.viewTrace.facades.FRect;
import org.twak.viewTrace.facades.HasApp;
import org.twak.viewTrace.facades.MiniFacade;
import org.twak.viewTrace.facades.Pix2Pix;
import org.twak.viewTrace.facades.Pix2Pix.Job;
import org.twak.viewTrace.facades.Pix2Pix.JobResult;

public class PanesLabelApp extends App {

	PanesTexApp child = new PanesTexApp( this );
	public String label;
	
	public PanesLabelApp(HasApp ha) {
		super(ha, "label windows", "dows2", 8, 256);
	}
	
	public PanesLabelApp(PanesLabelApp t) {
		super (t);
	}
	
	@Override
	public App getUp() {
		return ((FRect)hasA).mf.app;
	}

	@Override
	public MultiMap<String, App> getDown() {
		MultiMap<String, App>  out = new MultiMap<>();
		
		out.put( "facade texture", child );
		
		return out;
	}

	@Override
	public App copy() {
			return new PanesLabelApp( this );
	}

	@Override
	public void computeBatch(Runnable whenDone, List<App> batch) {
		
		DRectangle bounds = new DRectangle( 0, 0, 256, 256 );
		int count = 0;
		
		Map<FRect, Meta> names = new HashMap<>();
		
//		for ( MiniFacade mf : subfeatures ) {
		for ( App a : batch ) {
			try {
				MiniFacade mf = ( (FRect) a.hasA ).mf;

				//			if (mf.featureGen instanceof CGAMini)
				//				mf.featureGen = new FeatureGenerator( mf, mf.featureGen );

				BufferedImage src = ImageIO.read( Tweed.toWorkspace( mf.app.texture ) );
				DRectangle mini = Pix2Pix.findBounds( mf );

				FRect r = (FRect) a.hasA;

				if ( !mini.contains( r ) )
					return;//continue;

				DRectangle w = bounds.scale( mini.normalize( r ) );
				w.y = bounds.getMaxY() - w.y - w.height;

				BufferedImage dow =
							src.getSubimage(  
								(int) w.x, 
								(int) w.y,
								(int) w.width , 
								(int) w.height );

				DRectangle mask = new DRectangle();

				BufferedImage scaled = Imagez.scaleSquare( dow, 256, mask, Double.MAX_VALUE );
				BufferedImage toProcess = new BufferedImage( 512, 256, BufferedImage.TYPE_3BYTE_BGR );

				Graphics2D g = toProcess.createGraphics();
				g.drawImage( scaled, 256, 0, null );
				g.dispose();

				String wName = name + "_" + count + "@" + System.nanoTime();
				Pix2Pix.addInput( toProcess, wName, netName );

				//					String name = System.nanoTime() + "_" + count;
				//					ImageIO.write( toProcess, "png", new File( "/home/twak/code/pix2pix-interactive/input/"+WINDOW+"/test/" + name + ".png" ) );					

				names.put( r, new Meta( wName, mask ) );
				count++;

			} catch ( IOException e1 ) {
				e1.printStackTrace();
			}
		}
		
		Pix2Pix.submit ( new Job ( netName, System.nanoTime() + "_" + zAsString(), new JobResult() {
			@Override
			public void finished( File f ) {

				try {

					
					for ( Map.Entry<FRect, Meta> e : names.entrySet() ) {

						Meta meta = e.getValue();
						String dest = Pix2Pix.importTexture( f, meta.name, 255, null, meta.mask );
						
						if ( dest != null ) {  
							e.getKey().app.texture = dest;
							((PanesLabelApp ) e.getKey().app).label = dest;
						}
					}
					
				} catch (Throwable th) {
					th.printStackTrace();
				}
				finally {
					whenDone.run();
				}
			}
		} ) );
	}


private static class Meta {
	String name;
	DRectangle mask;
	private Meta(String name, DRectangle mask) {
		this.name = name;
		this.mask = mask;
	}
}
}