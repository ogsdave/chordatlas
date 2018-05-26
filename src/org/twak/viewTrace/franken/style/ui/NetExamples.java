package org.twak.viewTrace.franken.style.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.twak.utils.Imagez;
import org.twak.utils.Mathz;
import org.twak.utils.Pair;
import org.twak.viewTrace.franken.App;
import org.twak.viewTrace.franken.NetInfo;
import org.twak.viewTrace.franken.Pix2Pix;
import org.twak.viewTrace.franken.Pix2Pix.Job;
import org.twak.viewTrace.franken.style.StyleSource;

public class NetExamples extends JComponent {

	BufferedImage[][] images;
	int[][] inputIdx;
	List<Pair<Integer, Integer>> randomOrder = new ArrayList<>();
	int randomLocation = 0;
	
	boolean go = true;
	List<BufferedImage> inputs = new ArrayList<>();
	StyleSource styleSource;
	
	final static int BATCH_SIZE = 16;
//	final static double scale = 0.5;
	
	final static int DRAW_SIZE = 128;
	
	final static Random randy = new Random();
	
	int hx = -1, hy = -1;
	
	NetInfo exemplar;
	
	boolean mouseDown = false;
	
	long startTime = 0, endTime = 1000, lastChanged = 0;
	
	
	static class UniqueInt {
		
		int i;
		public UniqueInt (int i) {
			this.i = i;
		}

		@Override
		public boolean equals( Object obj ) {
			return obj == this;
		}
	}
	
	public NetExamples ( StyleSource ss, int x, int y, NetInfo exemplar, File exampleFolder ) {
		
		this.styleSource = ss;
		this.exemplar = exemplar;
		
		images = new BufferedImage [x][y];
		inputIdx = new int[x][y];
		
		setPreferredSize( new Dimension ( 
				(int)( x * DRAW_SIZE) ,
				(int)( y * DRAW_SIZE) ) );
		

		for (int i = 0; i < images.length; i++)
			for (int j = 0; j < images[0].length; j++)
				randomOrder.add( new Pair (i, j) );
		Collections.shuffle( randomOrder );
		
		for (File f : exampleFolder.listFiles() ) {
			try {
				BufferedImage bi = Imagez.scaleLongest( ImageIO.read( f ), exemplar.resolution ) ;
				inputs. add ( Imagez.join( bi, bi ) );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
		
		new Thread () {
			@Override
			public void run() {
				while (go && isVisible()) {
					
					Pix2Pix p2 = new Pix2Pix( exemplar );
					
					startTime = System.currentTimeMillis();
					
					for (int i = 0; i < BATCH_SIZE; i++) {
						int index = randy.nextInt(inputs.size());
						p2.addInput( inputs.get( index ), new UniqueInt ( index ), styleSource.draw( randy, null ) );
					}
					
					p2.submitSafe( new Job() {
						public void finished(java.util.Map<Object,File> results) {
							
							endTime = System.currentTimeMillis();
							
							
							addImages ( startTime, results.entrySet().stream().map( e -> new Pair<Integer, BufferedImage>( ((UniqueInt)e.getKey()).i, Imagez.read( e.getValue() )  ) ).collect( Collectors.toList() ) );
						}

					} );
				}
			}
		}.start();
		
		MouseAdapter ml = new MouseAdapter() {
			public void mouseMoved(MouseEvent e) {
				hx = Mathz.min ( images.length-1, (int)( e.getX() / (DRAW_SIZE ) ) );
				hy = Mathz.min ( images[0].length-1, (int)( e.getY() / (DRAW_SIZE ) ) );
				
				hx = Mathz.clamp( hx, 0, images.length );
				hy = Mathz.clamp( hy, 0, images[0].length );
				repaint();
			};
			public void mouseExited(MouseEvent e) {
				hx = hy = -1;
				repaint();
			};
			
			public void mousePressed(MouseEvent e) {
				mouseDown = true;
			};
			
			public void mouseReleased(MouseEvent e) {
				mouseDown = false;
			};
		};
		
		addMouseMotionListener( ml );
		addMouseListener( ml );
	}
	
	private void addImages( long startTime, List<Pair<Integer, BufferedImage>> values ) {
		
		int interval = Math.min ( 500, (int) ((endTime - startTime) / values.size() )) ;
		
		new Thread() {
			public void run() {
				
				for ( Pair<Integer, BufferedImage> e : values ) {
					if (startTime < lastChanged)
						return;
					addImage( e.first(), e.second() );
					try {
						Thread.sleep( interval );
					} catch ( InterruptedException f ) {
						f.printStackTrace();
					}
				}

			};
		}.start();
		
	}
	
	public void changed() {
		
		lastChanged = System.currentTimeMillis();
		
		for (int i = 0; i < images.length; i++)
			for (int j = 0; j < images[0].length; j++)
				images[i][j] = null;
		
		repaint();
	}
	
	private synchronized void addImage (int src, BufferedImage b) {
		Pair<Integer, Integer> next = randomOrder.get( (randomLocation ++) % randomOrder.size() );
		images[ next.first() ][ next.second() ] = b;
		inputIdx[ next.first() ][ next.second() ] = src;
		repaint();
	}

	@Override
	protected void paintComponent( Graphics g1 ) {
		
		Graphics2D g = (Graphics2D)g1;
		
		g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		g.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR );
		g.setRenderingHint( RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY );
		
		g.setColor( Color.black );
		g.fillRect( 0, 0, getWidth(), getHeight() );
		
		
		for (int i = 0; i < images.length; i++)
			for (int j = 0; j < images[0].length; j++) {
				if (images[i][j] != null)
					g.drawImage( images[i][j], i * DRAW_SIZE, j * DRAW_SIZE, (i+1) * DRAW_SIZE, (j+1) * DRAW_SIZE,
							0, 0, images[i][j].getWidth(), images[i][j].getHeight(), null );
			}
		
		int previewSize = DRAW_SIZE * 2;
		
		if (hx >=0 && images[hx][hy] != null) {
			g.setStroke( new BasicStroke( 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
			int x = hx * DRAW_SIZE - (previewSize/4);
			int y = hy * DRAW_SIZE - (previewSize/4);

			x = Mathz.clamp( x, 0, getWidth()  - previewSize );
			y = Mathz.clamp( y, 0, getHeight() - previewSize );
			
			g.drawRect( x-1, y-1, previewSize+1, previewSize+1 );
			
			if ( mouseDown )
				g.drawImage( inputs.get( inputIdx[hx][hy] ).getSubimage( 0, 0, exemplar.resolution, exemplar.resolution ), 
						x,y, x+previewSize, y+previewSize, 0,0, exemplar.resolution, exemplar.resolution, null );
			else
				g.drawImage( images[hx][hy], x,y,x+previewSize, y+previewSize, 0,0, exemplar.resolution, exemplar.resolution, null );
			
		}
		
	}
	
	public void stop() {
		go = false;
	}
	
}