/*
 * @(#)PathGraphics.java	1.23 04/05/26
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.print;

import java.lang.ref.SoftReference;
import java.util.Hashtable;
import sun.font.CharToGlyphMapper;
import sun.font.CompositeFont;
import sun.font.Font2D;
import sun.font.Font2DHandle;
import sun.font.FontManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Shape;

import java.text.AttributedCharacterIterator;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.PathIterator;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.VolatileImage;
import sun.awt.image.ByteComponentRaster;
import sun.awt.image.RemoteOffScreenImage;
import sun.awt.image.ToolkitImage;

import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterGraphics;
import java.awt.print.PrinterJob;

public abstract class PathGraphics extends ProxyGraphics2D {

    private Printable mPainter;
    private PageFormat mPageFormat;
    private int mPageIndex;
    private boolean mCanRedraw;

    protected PathGraphics(Graphics2D graphics, PrinterJob printerJob,
                           Printable painter, PageFormat pageFormat,
                           int pageIndex, boolean canRedraw) {
	super(graphics, printerJob);

        mPainter = painter;
        mPageFormat = pageFormat;
        mPageIndex = pageIndex;
        mCanRedraw = canRedraw;
    }

    /**
     * Return the Printable instance responsible for drawing
     * into this Graphics.
     */
    protected Printable getPrintable() {
        return mPainter;
    }

    /**
     * Return the PageFormat associated with this page of
     * Graphics.
     */
    protected PageFormat getPageFormat() {
        return mPageFormat;
    }

    /**
     * Return the page index associated with this Graphics.
     */
    protected int getPageIndex() {
        return mPageIndex;
    }

    /**
     * Return true if we are allowed to ask the application
     * to redraw portions of the page. In general, with the
     * PrinterJob API, the application can be asked to do a
     * redraw. When PrinterJob is emulating PrintJob then we
     * can not.
     */
    public boolean canDoRedraws() {
        return mCanRedraw;
    }

     /** 
      * Redraw a rectanglular area using a proxy graphics
      */
    public abstract void redrawRegion(Rectangle2D region,
                                  double scaleX, double scaleY,
                                  Rectangle2D srcRect, AffineTransform xform) 

                    throws PrinterException ;

    /** 
     * Draws a line, using the current color, between the points 
     * <code>(x1,&nbsp;y1)</code> and <code>(x2,&nbsp;y2)</code> 
     * in this graphics context's coordinate system. 
     * @param   x1  the first point's <i>x</i> coordinate.
     * @param   y1  the first point's <i>y</i> coordinate.
     * @param   x2  the second point's <i>x</i> coordinate.
     * @param   y2  the second point's <i>y</i> coordinate.
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
			
	Paint paint = getPaint();

    	try {
	    AffineTransform deviceTransform = getTransform();
	    if (getClip() != null) {
		deviceClip(getClip().getPathIterator(deviceTransform));
	    }

	    deviceDrawLine(x1, y1, x2, y2, (Color) paint);
    
	} catch (ClassCastException e) {
	    throw new IllegalArgumentException("Expected a Color instance");
    	}
    }


    /** 
     * Draws the outline of the specified rectangle. 
     * The left and right edges of the rectangle are at 
     * <code>x</code> and <code>x&nbsp;+&nbsp;width</code>. 
     * The top and bottom edges are at 
     * <code>y</code> and <code>y&nbsp;+&nbsp;height</code>. 
     * The rectangle is drawn using the graphics context's current color.
     * @param         x   the <i>x</i> coordinate 
     *                         of the rectangle to be drawn.
     * @param         y   the <i>y</i> coordinate 
     *                         of the rectangle to be drawn.
     * @param         width   the width of the rectangle to be drawn.
     * @param         height   the height of the rectangle to be drawn.
     * @see          java.awt.Graphics#fillRect
     * @see          java.awt.Graphics#clearRect
     */
    public void drawRect(int x, int y, int width, int height) {

	Paint paint = getPaint();

    	try {
	    AffineTransform deviceTransform = getTransform();
	    if (getClip() != null) {
		deviceClip(getClip().getPathIterator(deviceTransform));
	    }

	    deviceFrameRect(x, y, width, height, (Color) paint);

	} catch (ClassCastException e) {
	    throw new IllegalArgumentException("Expected a Color instance");
	}

    }

    /** 
     * Fills the specified rectangle. 
     * The left and right edges of the rectangle are at 
     * <code>x</code> and <code>x&nbsp;+&nbsp;width&nbsp;-&nbsp;1</code>. 
     * The top and bottom edges are at 
     * <code>y</code> and <code>y&nbsp;+&nbsp;height&nbsp;-&nbsp;1</code>. 
     * The resulting rectangle covers an area 
     * <code>width</code> pixels wide by 
     * <code>height</code> pixels tall.
     * The rectangle is filled using the graphics context's current color. 
     * @param         x   the <i>x</i> coordinate 
     *                         of the rectangle to be filled.
     * @param         y   the <i>y</i> coordinate 
     *                         of the rectangle to be filled.
     * @param         width   the width of the rectangle to be filled.
     * @param         height   the height of the rectangle to be filled.
     * @see           java.awt.Graphics#clearRect
     * @see           java.awt.Graphics#drawRect
     */
    public void fillRect(int x, int y, int width, int height){

	Paint paint = getPaint();

	try {
	    AffineTransform deviceTransform = getTransform();
	    if (getClip() != null) {
		deviceClip(getClip().getPathIterator(deviceTransform));
	    }

	    deviceFillRect(x, y, width, height, (Color) paint);

	} catch (ClassCastException e) {
	    throw new IllegalArgumentException("Expected a Color instance");
	}
    }

       /** 
     * Clears the specified rectangle by filling it with the background
     * color of the current drawing surface. This operation does not 
     * use the current paint mode. 
     * <p>
     * Beginning with Java&nbsp;1.1, the background color 
     * of offscreen images may be system dependent. Applications should 
     * use <code>setColor</code> followed by <code>fillRect</code> to 
     * ensure that an offscreen image is cleared to a specific color. 
     * @param       x the <i>x</i> coordinate of the rectangle to clear.
     * @param       y the <i>y</i> coordinate of the rectangle to clear.
     * @param       width the width of the rectangle to clear.
     * @param       height the height of the rectangle to clear.
     * @see         java.awt.Graphics#fillRect(int, int, int, int)
     * @see         java.awt.Graphics#drawRect
     * @see         java.awt.Graphics#setColor(java.awt.Color)
     * @see         java.awt.Graphics#setPaintMode
     * @see         java.awt.Graphics#setXORMode(java.awt.Color)
     */
    public void clearRect(int x, int y, int width, int height) {

	fill(new Rectangle2D.Float(x, y, width, height), getBackground());
    }

        /** 
     * Draws an outlined round-cornered rectangle using this graphics 
     * context's current color. The left and right edges of the rectangle 
     * are at <code>x</code> and <code>x&nbsp;+&nbsp;width</code>, 
     * respectively. The top and bottom edges of the rectangle are at 
     * <code>y</code> and <code>y&nbsp;+&nbsp;height</code>. 
     * @param      x the <i>x</i> coordinate of the rectangle to be drawn.
     * @param      y the <i>y</i> coordinate of the rectangle to be drawn.
     * @param      width the width of the rectangle to be drawn.
     * @param      height the height of the rectangle to be drawn.
     * @param      arcWidth the horizontal diameter of the arc 
     *                    at the four corners.
     * @param      arcHeight the vertical diameter of the arc 
     *                    at the four corners.
     * @see        java.awt.Graphics#fillRoundRect
     */
    public void drawRoundRect(int x, int y, int width, int height,
			      int arcWidth, int arcHeight) {
	
	draw(new RoundRectangle2D.Float(x, y,
				        width, height,
					arcWidth, arcHeight));
    }


    /** 
     * Fills the specified rounded corner rectangle with the current color.
     * The left and right edges of the rectangle 
     * are at <code>x</code> and <code>x&nbsp;+&nbsp;width&nbsp;-&nbsp;1</code>, 
     * respectively. The top and bottom edges of the rectangle are at 
     * <code>y</code> and <code>y&nbsp;+&nbsp;height&nbsp;-&nbsp;1</code>. 
     * @param       x the <i>x</i> coordinate of the rectangle to be filled.
     * @param       y the <i>y</i> coordinate of the rectangle to be filled.
     * @param       width the width of the rectangle to be filled.
     * @param       height the height of the rectangle to be filled.
     * @param       arcWidth the horizontal diameter 
     *                     of the arc at the four corners.
     * @param       arcHeight the vertical diameter 
     *                     of the arc at the four corners.
     * @see         java.awt.Graphics#drawRoundRect
     */
    public void fillRoundRect(int x, int y, int width, int height,
			      int arcWidth, int arcHeight) {

	fill(new RoundRectangle2D.Float(x, y,
				        width, height,
					arcWidth, arcHeight));
    }

    /** 
     * Draws the outline of an oval.
     * The result is a circle or ellipse that fits within the 
     * rectangle specified by the <code>x</code>, <code>y</code>, 
     * <code>width</code>, and <code>height</code> arguments. 
     * <p> 
     * The oval covers an area that is 
     * <code>width&nbsp;+&nbsp;1</code> pixels wide 
     * and <code>height&nbsp;+&nbsp;1<code> pixels tall. 
     * @param       x the <i>x</i> coordinate of the upper left 
     *                     corner of the oval to be drawn.
     * @param       y the <i>y</i> coordinate of the upper left 
     *                     corner of the oval to be drawn.
     * @param       width the width of the oval to be drawn.
     * @param       height the height of the oval to be drawn.
     * @see         java.awt.Graphics#fillOval
     * @since       JDK1.0
     */
    public void drawOval(int x, int y, int width, int height) {
	draw(new Ellipse2D.Float(x, y, width, height));
    }

        /** 
     * Fills an oval bounded by the specified rectangle with the
     * current color.
     * @param       x the <i>x</i> coordinate of the upper left corner 
     *                     of the oval to be filled.
     * @param       y the <i>y</i> coordinate of the upper left corner 
     *                     of the oval to be filled.
     * @param       width the width of the oval to be filled.
     * @param       height the height of the oval to be filled.
     * @see         java.awt.Graphics#drawOval
     */
    public void fillOval(int x, int y, int width, int height){

	fill(new Ellipse2D.Float(x, y, width, height));
    }

    /**
     * Draws the outline of a circular or elliptical arc 
     * covering the specified rectangle.
     * <p>
     * The resulting arc begins at <code>startAngle</code> and extends  
     * for <code>arcAngle</code> degrees, using the current color.
     * Angles are interpreted such that 0&nbsp;degrees 
     * is at the 3&nbsp;o'clock position. 
     * A positive value indicates a counter-clockwise rotation
     * while a negative value indicates a clockwise rotation.
     * <p>
     * The center of the arc is the center of the rectangle whose origin 
     * is (<i>x</i>,&nbsp;<i>y</i>) and whose size is specified by the 
     * <code>width</code> and <code>height</code> arguments. 
     * <p>
     * The resulting arc covers an area 
     * <code>width&nbsp;+&nbsp;1</code> pixels wide
     * by <code>height&nbsp;+&nbsp;1</code> pixels tall.
     * <p>
     * The angles are specified relative to the non-square extents of
     * the bounding rectangle such that 45 degrees always falls on the
     * line from the center of the ellipse to the upper right corner of
     * the bounding rectangle. As a result, if the bounding rectangle is
     * noticeably longer in one axis than the other, the angles to the
     * start and end of the arc segment will be skewed farther along the
     * longer axis of the bounds.
     * @param        x the <i>x</i> coordinate of the 
     *                    upper-left corner of the arc to be drawn.
     * @param        y the <i>y</i>  coordinate of the 
     *                    upper-left corner of the arc to be drawn.
     * @param        width the width of the arc to be drawn.
     * @param        height the height of the arc to be drawn.
     * @param        startAngle the beginning angle.
     * @param        arcAngle the angular extent of the arc, 
     *                    relative to the start angle.
     * @see         java.awt.Graphics#fillArc
     */
    public void drawArc(int x, int y, int width, int height,
			         int startAngle, int arcAngle) {
	draw(new Arc2D.Float(x, y, width, height,
			     startAngle, arcAngle,
			     Arc2D.OPEN));
    }


    /** 
     * Fills a circular or elliptical arc covering the specified rectangle.
     * <p>
     * The resulting arc begins at <code>startAngle</code> and extends  
     * for <code>arcAngle</code> degrees.
     * Angles are interpreted such that 0&nbsp;degrees 
     * is at the 3&nbsp;o'clock position. 
     * A positive value indicates a counter-clockwise rotation
     * while a negative value indicates a clockwise rotation.
     * <p>
     * The center of the arc is the center of the rectangle whose origin 
     * is (<i>x</i>,&nbsp;<i>y</i>) and whose size is specified by the 
     * <code>width</code> and <code>height</code> arguments. 
     * <p>
     * The resulting arc covers an area 
     * <code>width&nbsp;+&nbsp;1</code> pixels wide
     * by <code>height&nbsp;+&nbsp;1</code> pixels tall.
     * <p>
     * The angles are specified relative to the non-square extents of
     * the bounding rectangle such that 45 degrees always falls on the
     * line from the center of the ellipse to the upper right corner of
     * the bounding rectangle. As a result, if the bounding rectangle is
     * noticeably longer in one axis than the other, the angles to the
     * start and end of the arc segment will be skewed farther along the
     * longer axis of the bounds.
     * @param        x the <i>x</i> coordinate of the 
     *                    upper-left corner of the arc to be filled.
     * @param        y the <i>y</i>  coordinate of the 
     *                    upper-left corner of the arc to be filled.
     * @param        width the width of the arc to be filled.
     * @param        height the height of the arc to be filled.
     * @param        startAngle the beginning angle.
     * @param        arcAngle the angular extent of the arc, 
     *                    relative to the start angle.
     * @see         java.awt.Graphics#drawArc
     */
    public void fillArc(int x, int y, int width, int height,
				 int startAngle, int arcAngle) {

	fill(new Arc2D.Float(x, y, width, height,
			     startAngle, arcAngle,
			     Arc2D.PIE));
    }

    /** 
     * Draws a sequence of connected lines defined by 
     * arrays of <i>x</i> and <i>y</i> coordinates. 
     * Each pair of (<i>x</i>,&nbsp;<i>y</i>) coordinates defines a point.
     * The figure is not closed if the first point 
     * differs from the last point.
     * @param       xPoints an array of <i>x</i> points
     * @param       yPoints an array of <i>y</i> points
     * @param       nPoints the total number of points
     * @see         java.awt.Graphics#drawPolygon(int[], int[], int)
     * @since       JDK1.1
     */
    public void drawPolyline(int xPoints[], int yPoints[],
			     int nPoints) {
	float fromX;
	float fromY;
	float toX;
	float toY;

	if (nPoints > 0) {
	    fromX = xPoints[0];
	    fromY = yPoints[0];
	    for(int i = 1; i < nPoints; i++) {
		toX = xPoints[i];
		toY = yPoints[i];
		draw(new Line2D.Float(fromX, fromY, toX, toY));
		fromX = toX;
		fromY = toY;
	    }
	}

    }


    /** 
     * Draws a closed polygon defined by 
     * arrays of <i>x</i> and <i>y</i> coordinates. 
     * Each pair of (<i>x</i>,&nbsp;<i>y</i>) coordinates defines a point.
     * <p>
     * This method draws the polygon defined by <code>nPoint</code> line 
     * segments, where the first <code>nPoint&nbsp;-&nbsp;1</code> 
     * line segments are line segments from 
     * <code>(xPoints[i&nbsp;-&nbsp;1],&nbsp;yPoints[i&nbsp;-&nbsp;1])</code> 
     * to <code>(xPoints[i],&nbsp;yPoints[i])</code>, for 
     * 1&nbsp;&le;&nbsp;<i>i</i>&nbsp;&le;&nbsp;<code>nPoints</code>.  
     * The figure is automatically closed by drawing a line connecting
     * the final point to the first point, if those points are different.
     * @param        xPoints   a an array of <code>x</code> coordinates.
     * @param        yPoints   a an array of <code>y</code> coordinates.
     * @param        nPoints   a the total number of points.
     * @see          java.awt.Graphics#fillPolygon
     * @see          java.awt.Graphics#drawPolyline
     */
    public void drawPolygon(int xPoints[], int yPoints[],
				     int nPoints) {

	draw(new Polygon(xPoints, yPoints, nPoints));
    }

    /** 
     * Draws the outline of a polygon defined by the specified 
     * <code>Polygon</code> object. 
     * @param        p the polygon to draw.
     * @see          java.awt.Graphics#fillPolygon
     * @see          java.awt.Graphics#drawPolyline
     */
    public void drawPolygon(Polygon p) {
	draw(p);
    }

     /** 
     * Fills a closed polygon defined by 
     * arrays of <i>x</i> and <i>y</i> coordinates. 
     * <p>
     * This method draws the polygon defined by <code>nPoint</code> line 
     * segments, where the first <code>nPoint&nbsp;-&nbsp;1</code> 
     * line segments are line segments from 
     * <code>(xPoints[i&nbsp;-&nbsp;1],&nbsp;yPoints[i&nbsp;-&nbsp;1])</code> 
     * to <code>(xPoints[i],&nbsp;yPoints[i])</code>, for 
     * 1&nbsp;&le;&nbsp;<i>i</i>&nbsp;&le;&nbsp;<code>nPoints</code>.  
     * The figure is automatically closed by drawing a line connecting
     * the final point to the first point, if those points are different.
     * <p>
     * The area inside the polygon is defined using an 
     * even-odd fill rule, also known as the alternating rule.
     * @param        xPoints   a an array of <code>x</code> coordinates.
     * @param        yPoints   a an array of <code>y</code> coordinates.
     * @param        nPoints   a the total number of points.
     * @see          java.awt.Graphics#drawPolygon(int[], int[], int)
     */
    public void fillPolygon(int xPoints[], int yPoints[],
			    int nPoints) {

	fill(new Polygon(xPoints, yPoints, nPoints));
    }


    /** 
     * Fills the polygon defined by the specified Polygon object with
     * the graphics context's current color. 
     * <p>
     * The area inside the polygon is defined using an 
     * even-odd fill rule, also known as the alternating rule.
     * @param        p the polygon to fill.
     * @see          java.awt.Graphics#drawPolygon(int[], int[], int)
     */
    public void fillPolygon(Polygon p) {

	fill(p);
    }

    /** 
     * Draws the text given by the specified string, using this 
     * graphics context's current font and color. The baseline of the 
     * first character is at position (<i>x</i>,&nbsp;<i>y</i>) in this 
     * graphics context's coordinate system. 
     * @param       str      the string to be drawn.
     * @param       x        the <i>x</i> coordinate.
     * @param       y        the <i>y</i> coordinate.
     * @see         java.awt.Graphics#drawBytes
     * @see         java.awt.Graphics#drawChars
     * @since       JDK1.0
     */
    public void drawString(String str, int x, int y) {
	drawString(str, (float) x, (float) y);
    }

    public void drawString(String str, float x, float y) {
        if (str.length() == 0) {
            return;
        }
	TextLayout layout =
	    new TextLayout(str, getFont(), getFontRenderContext());
 	layout.draw(this, x, y);
    }

    protected void drawString(String str, float x, float y,
			      Font font, FontRenderContext frc, float w) {
	TextLayout layout =
	    new TextLayout(str, font, frc);
	Shape textShape =
	    layout.getOutline(AffineTransform.getTranslateInstance(x, y));
	fill(textShape);
    }

    /**
     * Draws the text given by the specified iterator, using this 
     * graphics context's current color. The iterator has to specify a font
     * for each character. The baseline of the 
     * first character is at position (<i>x</i>,&nbsp;<i>y</i>) in this 
     * graphics context's coordinate system. 
     * @param       iterator the iterator whose text is to be drawn
     * @param       x        the <i>x</i> coordinate.
     * @param       y        the <i>y</i> coordinate.
     * @see         java.awt.Graphics#drawBytes
     * @see         java.awt.Graphics#drawChars
     */
    public void drawString(AttributedCharacterIterator iterator,
			   int x, int y) {
	drawString(iterator, (float) x, (float) y);
    }
    public void drawString(AttributedCharacterIterator iterator,
			   float x, float y) {
        if (iterator == null) {
            throw 
                new NullPointerException("attributedcharacteriterator is null");
        }
	TextLayout layout =
	    new TextLayout(iterator, getFontRenderContext());
 	layout.draw(this, x, y);
    }

    /**
     * Draws a GlyphVector.
     * The rendering attributes applied include the clip, transform,
     * paint or color, and composite attributes.  The GlyphVector specifies
     * individual glyphs from a Font.
     * @param g The GlyphVector to be drawn.
     * @param x,y The coordinates where the glyphs should be drawn.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void drawGlyphVector(GlyphVector g,
				float x,
				float y) {

	if (RasterPrinterJob.shapeTextProp ||
	    !printedSimpleGlyphVector(g, x, y)) {
	    fill(g.getOutline(x, y));
	}
    }

    protected static SoftReference<Hashtable<Font2DHandle,Object>>
	fontMapRef = new SoftReference<Hashtable<Font2DHandle,Object>>(null);

    protected int platformFontCount(Font font, String str) {
	return 0;
    }

    boolean printedSimpleGlyphVector(GlyphVector g, float x, float y) {

	int flags = g.getLayoutFlags();
	if (flags != 0 && flags != GlyphVector.FLAG_HAS_POSITION_ADJUSTMENTS) {
	    /* We can't handle RTL, re-ordering, complex glyphs etc */
	    return false;
	}
	Font font = g.getFont();
	Font2D font2D = FontManager.getFont2D(font);
	if (font2D.handle.font2D != font2D) {
	    /* suspicious, may be a bad font. lets bail */
	    return false;
	}
	Hashtable<Font2DHandle,Object> fontMap;
	synchronized (PathGraphics.class) {
	    fontMap = fontMapRef.get();
	    if (fontMap == null) {
		fontMap = new Hashtable<Font2DHandle,Object>();
		fontMapRef =
		    new SoftReference<Hashtable<Font2DHandle,Object>>(fontMap);
	    }
	}

	int numGlyphs = g.getNumGlyphs();
	int[] glyphCodes = g.getGlyphCodes(0, numGlyphs, null);

	char[] glyphToCharMap = null;
	char[][] mapArray = null;
	CompositeFont cf = null;

	/* Build the needed maps for this font in a synchronized block */
	synchronized (fontMap) {
	    if (font2D instanceof CompositeFont) {
		cf = (CompositeFont)font2D;
		int numSlots = cf.getNumSlots();
		mapArray = (char[][])fontMap.get(font2D.handle);
		if (mapArray == null) {
		    mapArray = new char[numSlots][];
		    fontMap.put(font2D.handle, mapArray);
		}
		for (int i=0; i<numGlyphs;i++) {
		    int slot = glyphCodes[i] >>> 24;
		    if (slot >= numSlots) { /* shouldn't happen */
			return false;
		    }
		    if (mapArray[slot] == null) {
			Font2D slotFont = cf.getSlotFont(slot);
			char[] map = (char[])fontMap.get(slotFont.handle);
			if (map == null) {
			    map = getGlyphToCharMapForFont(slotFont);
			}
			mapArray[slot] = map;
		    }
		}
	    } else {
		glyphToCharMap = (char[])fontMap.get(font2D.handle);
		if (glyphToCharMap == null) {
		    glyphToCharMap = getGlyphToCharMapForFont(font2D);
		    fontMap.put(font2D.handle, glyphToCharMap);
		}
	    }
	}

	char[] chars = new char[numGlyphs];
	if (cf != null) {
	    for (int i=0; i<numGlyphs; i++) {
		int gc = glyphCodes[i];
		char[] map = mapArray[gc >>> 24];
		gc = gc & 0xffffff;
		if (map == null) {
		    return false;
		}
		/* X11 symbol & dingbats fonts used only for global metrics,
		 * so the glyph codes we have really refer to Lucida Sans
		 * Regular.
		 * So its possible the glyph code may appear out of range.
		 * Note that later on we double-check the glyph codes that
		 * we get from re-creating the GV from the string are the
		 * same as those we started with.
		 */
		if (gc < 0 || gc >= map.length) {
		     return false;
		}
		char ch = map[gc];
		if (ch != CharToGlyphMapper.INVISIBLE_GLYPH_ID) {
		    chars[i] = ch;
		} else {
		    return false;
		}
	    }	    
	} else {
	    for (int i=0; i<numGlyphs; i++) {
		int gc = glyphCodes[i];
		if (gc < 0 || gc >= glyphToCharMap.length) {
		    return false;
		}
		char ch = glyphToCharMap[gc];
		if (ch != CharToGlyphMapper.INVISIBLE_GLYPH_ID) {
		    chars[i] = ch;
		} else {
		    return false;
		}
	    }
	}

	FontRenderContext gvFrc = g.getFontRenderContext();
	GlyphVector gv2 = font.createGlyphVector(gvFrc, chars);
	if (gv2.getNumGlyphs() != numGlyphs) {
	    return false;
	}
	int[] glyphCodes2 = gv2.getGlyphCodes(0, numGlyphs, null);
	/*  
	 * Needed to double-check remapping of X11 symbol & dingbats.
	 */
	for (int i=0; i<numGlyphs; i++) {
	    if (glyphCodes[i] != glyphCodes2[i]) {
		return false;
	    }
	}	

	FontRenderContext g2dFrc = getFontRenderContext();
	boolean compatibleFRC = gvFrc.equals(g2dFrc);
	/* If differ only in specifying A-A or a translation, these are
	 * also compatible FRC's, and we can do one drawString call.
	 */
	if (!compatibleFRC &&
	    gvFrc.usesFractionalMetrics() == g2dFrc.usesFractionalMetrics()) {
	    AffineTransform gvAT = gvFrc.getTransform();
	    AffineTransform g2dAT = getTransform();
	    double[] gvMatrix = new double[4];
	    double[] g2dMatrix = new double[4];
	    gvAT.getMatrix(gvMatrix);
	    g2dAT.getMatrix(g2dMatrix);
	    compatibleFRC = true;
	    for (int i=0;i<4;i++) {
		if (gvMatrix[i] != g2dMatrix[i]) {
		    compatibleFRC = false;
		    break;
		}
	    }
	}

	String str = new String(chars, 0, numGlyphs);
	int numFonts = platformFontCount(font, str);
	if (numFonts == 0) {
	    return false;
	}

	if (compatibleFRC) {
	    drawString(str, x, y, font, gvFrc, 0f);
	    return true;
	}

	/* If positions have not been explicitly assigned, we can
	 * ask the string to be drawn adjusted to this width.
	 * This call is supported only in the PS generator.
	 * GDI has API to specify the advance for each glyph in a
	 * string which could be used here too, but that is not yet
	 * implemented, and we'd need to update the signature of the
	 * drawString method to take the advances (ie relative positions)
	 * and use that instead of the width.
	 */
	float[] positions = g.getGlyphPositions(0, numGlyphs, null);
	if (numFonts == 1 && canDrawStringToWidth() &&
	    ((flags & GlyphVector.FLAG_HAS_POSITION_ADJUSTMENTS) == 0 ||
	    samePositions(gv2, glyphCodes2, glyphCodes, positions))) {
	    Point2D adv = g.getGlyphPosition(numGlyphs);
	    drawString(str, x, y, font, gvFrc, (float)adv.getX());
	    return true;
	}

	/* In some scripts chars drawn individually do not have the
	 * same representation (glyphs) as when combined with other chars.
	 * All of the scripts we support, or are known to exhibit this
	 * behaviour conveniently are part of one contiguous range.
	 * Hebrew, Arabic .. Devanagari ... and on to Myanmar.
	 */
	for (int i=0; i<chars.length; i++) {
	    if ((chars[i] >= 0x0590) && (chars[i] <= 0x109f)) {
		return false;
            }
	}

	/* If we reach here we have mapped all the glyphs back
	 * one-to-one to simple unicode chars that we know are in the font.
	 * We can call "drawChars" on each one of them in turn, setting
	 * the position based on the glyph positions.
	 */
	for (int i=0; i<numGlyphs; i++) {
	    String s = new String(chars, i, 1);
	    drawString(s, x+positions[i*2], y+positions[i*2+1],
		       font, gvFrc, 0f);
	}
	return true;
    }

    /* The same codes must be in the same positions for this to return true.
     * This would look cleaner if it took the original GV as a parameter but
     * we already have the codes and will need to get the positions array
     * too in most cases anyway. So its cheaper to pass them in.
     * This call wouldn't be necessary if layout didn't always set the
     * FLAG_HAS_POSITION_ADJUSTMENTS even if the default advances are used
     * and there was no re-ordering (this should be fixed some day).
     */
    private boolean samePositions(GlyphVector gv, int[] gvcodes,
				  int[] origCodes, float[] origPositions) {

	int numGlyphs = gv.getNumGlyphs();
	float[] gvpos = gv.getGlyphPositions(0, numGlyphs, null);

	/* this shouldn't happen here, but just in case */
	if (numGlyphs != gvcodes.length ||  /* real paranoia here */
	    origCodes.length != gvcodes.length ||
	    origPositions.length != gvpos.length) {
	    return false;
	}

	for (int i=0; i<numGlyphs; i++) {
	    if (gvcodes[i] != origCodes[i] || gvpos[i] != origPositions[i]) {
		return false;
	    }
	}
	return true;
    }

    protected boolean canDrawStringToWidth() {
	return false;
    }

    /* return an array which can map glyphs back to char codes.
     * Glyphs which aren't mapped from a simple unicode code point
     * will have no mapping in this array, and will be assumed to be
     * because of some substitution that we can't handle.
     */
    private static char[] getGlyphToCharMapForFont(Font2D font2D) {
	/* NB Composites report the number of glyphs in slot 0.
	 * So if a string uses a char from a later slot, or a fallback slot,
	 * it will not be able to use this faster path.
	 */
	int numGlyphs = font2D.getNumGlyphs();
	int missingGlyph = font2D.getMissingGlyphCode();
	char[] glyphToCharMap = new char[numGlyphs];
	int glyph;

	for (int i=0;i<numGlyphs; i++) {
	    glyphToCharMap[i] = CharToGlyphMapper.INVISIBLE_GLYPH_ID;
	}

	/* Consider refining the ranges to try to map by asking the font
	 * what ranges it supports.
	 */
	for (char c=0; c<CharToGlyphMapper.HI_SURROGATE_START; c++) {
	    glyph = font2D.charToGlyph(c);
	    if (glyph != missingGlyph && glyph < numGlyphs) {
		glyphToCharMap[glyph] = c;	
	    }
	}
	return glyphToCharMap;
    }

    /**
     * Strokes the outline of a Shape using the settings of the current
     * graphics state.  The rendering attributes applied include the
     * clip, transform, paint or color, composite and stroke attributes.
     * @param s The shape to be drawn.
     * @see #setStroke
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #clip
     * @see #setClip
     * @see #setComposite
     */
    public void draw(Shape s) {

	fill(getStroke().createStrokedShape(s));
    }

    /**
     * Fills the interior of a Shape using the settings of the current
     * graphics state. The rendering attributes applied include the
     * clip, transform, paint or color, and composite.
     * @see #setPaint
     * @see java.awt.Graphics#setColor
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void fill(Shape s) {
	Paint paint = getPaint();

	try {
	    fill(s, (Color) paint);

	/* The PathGraphics class only supports filling with
	 * solid colors and so we do not expect the cast of Paint
	 * to Color to fail. If it does fail then something went
	 * wrong, like the app draw a page with a solid color but
	 * then redrew it with a Gradient.
	 */
	} catch (ClassCastException e) {
	    throw new IllegalArgumentException("Expected a Color instance");
	}
    }

    public void fill(Shape s, Color color) {
	AffineTransform deviceTransform = getTransform();

	if (getClip() != null) {
	    deviceClip(getClip().getPathIterator(deviceTransform));
	}
	deviceFill(s.getPathIterator(deviceTransform), color);
    }

    /**
     * Fill the path defined by <code>pathIter</code>
     * with the specified color.
     * The path is provided in device coordinates.
     */
    protected abstract void deviceFill(PathIterator pathIter, Color color);

    /*
     * Set the clipping path to that defined by
     * the passed in <code>PathIterator</code>.
     */
    protected abstract void deviceClip(PathIterator pathIter);

    /*
     * Draw the outline of the rectangle without using path
     * if supported by platform.
     */
    protected abstract void deviceFrameRect(int x, int y,
					    int width, int height, 
					    Color color);
	
    /*
     * Draw a line without using path if supported by platform.
     */
    protected abstract void deviceDrawLine(int xBegin, int yBegin,
					   int xEnd, int yEnd, Color color);

    /*
     * Fill a rectangle using specified color.
     */
    protected abstract void deviceFillRect(int x, int y,
					   int width, int height, Color color);

    /* Obtain a BI from known implementations of java.awt.Image
     */
    protected BufferedImage getBufferedImage(Image img) {
	if (img instanceof RemoteOffScreenImage) {
            // Really not expecting this on Windows but just
            // for completeness. Note test for this first as its
            // a subtype of BufferedImage.
            return ((RemoteOffScreenImage)img).getSnapshot();
	} else if (img instanceof BufferedImage) {
            // Otherwise we expect a BufferedImage to behave as a standard BI
            return (BufferedImage)img;
	} else if (img instanceof ToolkitImage) {
            // This can be null if the image isn't loaded yet.
            // This is fine as in that case our caller will return
            // as it will only draw a fully loaded image
            return ((ToolkitImage)img).getBufferedImage();
	} else if (img instanceof VolatileImage) {
            // VI needs to make a new BI: this is unavoidable but
            // I don't expect VI's to be "huge" in any case.
            return ((VolatileImage)img).getSnapshot();
	} else {
            // may be null or may be some non-standard Image which
            // shouldn't happen as Image is implemented by the platform
            // not by applications
            // If you add a new Image implementation to the platform you
            // will need to support it here similarly to VI.
            return null;
	}
    }

    /**
     * Return true if the BufferedImage argument has non-opaque
     * bits in it and therefore can not be directly rendered by
     * GDI. Return false if the image is opaque. If this function
     * can not tell for sure whether the image has transparent
     * pixels then it assumes that it does.
     */
    protected boolean hasTransparentPixels(BufferedImage bufferedImage) {
        ColorModel colorModel = bufferedImage.getColorModel();
        boolean hasTransparency = colorModel == null
            ? true
            : colorModel.getTransparency() != ColorModel.OPAQUE;

	/*
         * For the default INT ARGB check the image to see if any pixels are
         * really transparent. If there are no transparent pixels then the
         * transparency of the color model can be ignored.
	 * We assume that IndexColorModel images have already been
	 * checked for transparency and will be OPAQUE unless they actually
	 * have transparent pixels present.
         */
        if (hasTransparency && bufferedImage != null) {
	    if (bufferedImage.getType()==BufferedImage.TYPE_INT_ARGB ||
		bufferedImage.getType()==BufferedImage.TYPE_INT_ARGB_PRE) {
                DataBuffer db =  bufferedImage.getRaster().getDataBuffer();
		SampleModel sm = bufferedImage.getRaster().getSampleModel();
                if (db instanceof DataBufferInt &&
		    sm instanceof SinglePixelPackedSampleModel) {
		    SinglePixelPackedSampleModel psm = 
			(SinglePixelPackedSampleModel)sm;
                    int[] int_data = ((DataBufferInt)db).getData();
		    int x = bufferedImage.getMinX();
		    int y = bufferedImage.getMinY();
		    int w = bufferedImage.getWidth();
		    int h = bufferedImage.getHeight();
		    int stride = psm.getScanlineStride();
                    boolean hastranspixel = false;
		    for (int j = y; j < y+h; j++) {
			int yoff = y * stride;
			for (int i = x; i < x+w; i++) {
			    if ((int_data[yoff+i] & 0xff000000)!=0xff000000 ) {
				hastranspixel = true;
				break;
			    }
			}
			if (hastranspixel) {
			    break;
			}
		    }
		    if (hastranspixel == false) {
			hasTransparency = false;
		    }
                }
            }
        }

        return hasTransparency;
    }

    protected boolean isBitmaskTransparency(BufferedImage bufferedImage) {
        ColorModel colorModel = bufferedImage.getColorModel();
        return (colorModel != null &&
                colorModel.getTransparency() == ColorModel.BITMASK);
    }


    /* An optimisation for the special case of ICM images which have
     * bitmask transparency.
     */
    protected boolean drawBitmaskImage(BufferedImage bufferedImage,
				       AffineTransform xform,
				       Color bgcolor,
				       int srcX, int srcY,
				       int srcWidth, int srcHeight) {

        ColorModel colorModel = bufferedImage.getColorModel();
        IndexColorModel icm;
        int [] pixels;

        if (!(colorModel instanceof IndexColorModel)) {
            return false;
        } else {
            icm = (IndexColorModel)colorModel;
        }

        if (colorModel.getTransparency() != ColorModel.BITMASK) {
            return false;
        }

	// to be compatible with 1.1 printing which treated b/g colors
	// with alpha 128 as opaque
        if (bgcolor != null && bgcolor.getAlpha() < 128) {
            return false;
        }

        if ((xform.getType()
             & ~( AffineTransform.TYPE_UNIFORM_SCALE
                  | AffineTransform.TYPE_TRANSLATION
                  | AffineTransform.TYPE_QUADRANT_ROTATION
                  )) != 0) {
            return false;
        }

        if ((getTransform().getType()
             & ~( AffineTransform.TYPE_UNIFORM_SCALE
                  | AffineTransform.TYPE_TRANSLATION
                  | AffineTransform.TYPE_QUADRANT_ROTATION
                  )) != 0) {
            return false;
        }

        BufferedImage subImage = null;
        Raster raster = bufferedImage.getRaster();
        int transpixel = icm.getTransparentPixel();
	byte[] alphas = new byte[icm.getMapSize()];
	icm.getAlphas(alphas);
	if (transpixel >= 0) {
	    alphas[transpixel] = 0;
	}

        /* don't just use srcWidth & srcHeight from application - they
         * may exceed the extent of the image - may need to clip.
         * The image xform will ensure that points are still mapped properly.
         */
        int rw = raster.getWidth();
        int rh = raster.getHeight();
        if (srcX > rw || srcY > rh) {
            return false;
        }
        int right, bottom, wid, hgt;
        if (srcX+srcWidth > rw) {
            right = rw;
            wid = right - srcX;
        } else {
            right = srcX+srcWidth;
            wid = srcWidth;
        }
        if (srcY+srcHeight > rh) {
            bottom = rh;
            hgt = bottom - srcY;
        } else {
            bottom = srcY+srcHeight;
            hgt = srcHeight;
        }
        pixels = new int[wid];
        for (int j=srcY; j<bottom; j++) {
            int startx = -1;
            raster.getPixels(srcX, j, wid, 1, pixels);
            for (int i=srcX; i<right; i++) {
                if (alphas[pixels[i-srcX]] == 0) {
                    if (startx >=0) {
                        subImage = bufferedImage.getSubimage(startx, j,
                                                             i-startx, 1);
			xform.translate(startx, j);
			drawImageToPlatform(subImage, xform, bgcolor,
				      0, 0, i-startx, 1, true);
			xform.translate(-startx, -j);
                        startx = -1;
                    }
                } else if (startx < 0) {
                    startx = i;
                }
            }
            if (startx >= 0) {
                subImage = bufferedImage.getSubimage(startx, j,
                                                     right - startx, 1);
		xform.translate(startx, j);
                drawImageToPlatform(subImage, xform, bgcolor,
                              0, 0, right - startx, 1, true);
		xform.translate(-startx, -j);
            }
        }
        return true;
    }


    
    /**
     * The various <code>drawImage()</code> methods for
     * <code>PathGraphics</code> are all decomposed
     * into an invocation of <code>drawImageToPlatform</code>.
     * The portion of the passed in image defined by
     * <code>srcX, srcY, srcWidth, and srcHeight</code>
     * is transformed by the supplied AffineTransform and
     * drawn using PS to the printer context.
     * 
     * @param	img	The image to be drawn.
     *                  This method does nothing if <code>img</code> is null.
     * @param	xform	Used to tranform the image before drawing.
     *			This can be null.
     * @param	bgcolor	This color is drawn where the image has transparent
     *			pixels. If this parameter is null then the
     *			pixels already in the destination should show
     *			through.
     * @param	srcX	With srcY this defines the upper-left corner
     *			of the portion of the image to be drawn.
     *
     * @param	srcY	With srcX this defines the upper-left corner
     *			of the portion of the image to be drawn.
     * @param	srcWidth    The width of the portion of the image to
     *			    be drawn.
     * @param	srcHeight   The height of the portion of the image to
     *			    be drawn.
     * @param   handlingTransparency if being recursively called to
     *                    print opaque region of transparent image
     */
    protected abstract boolean
	drawImageToPlatform(Image img, AffineTransform xform,
			    Color bgcolor,
			    int srcX, int srcY,
			    int srcWidth, int srcHeight,
			    boolean handlingTransparency);

    /** 
     * Draws as much of the specified image as is currently available.
     * The image is drawn with its top-left corner at 
     * (<i>x</i>,&nbsp;<i>y</i>) in this graphics context's coordinate 
     * space. Transparent pixels in the image do not affect whatever 
     * pixels are already there. 
     * <p>
     * This method returns immediately in all cases, even if the
     * complete image has not yet been loaded, and it has not been dithered 
     * and converted for the current output device.
     * <p>
     * If the image has not yet been completely loaded, then
     * <code>drawImage</code> returns <code>false</code>. As more of
     * the image becomes available, the process that draws the image notifies 
     * the specified image observer.
     * @param    img the specified image to be drawn.
     * @param    x   the <i>x</i> coordinate.
     * @param    y   the <i>y</i> coordinate.
     * @param    observer    object to be notified as more of 
     *                          the image is converted.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since    JDK1.0
     */
    public boolean drawImage(Image img, int x, int y, 
			     ImageObserver observer) {

	return drawImage(img, x, y, null, observer);
    }

    /**
     * Draws as much of the specified image as has already been scaled
     * to fit inside the specified rectangle.
     * <p>
     * The image is drawn inside the specified rectangle of this 
     * graphics context's coordinate space, and is scaled if 
     * necessary. Transparent pixels do not affect whatever pixels
     * are already there. 
     * <p>
     * This method returns immediately in all cases, even if the
     * entire image has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete, then
     * <code>drawImage</code> returns <code>false</code>. As more of
     * the image becomes available, the process that draws the image notifies 
     * the image observer by calling its <code>imageUpdate</code> method.
     * <p>
     * A scaled version of an image will not necessarily be
     * available immediately just because an unscaled version of the
     * image has been constructed for this output device.  Each size of
     * the image may be cached separately and generated from the original
     * data in a separate image production sequence.
     * @param    img    the specified image to be drawn.
     * @param    x      the <i>x</i> coordinate.
     * @param    y      the <i>y</i> coordinate.
     * @param    width  the width of the rectangle.
     * @param    height the height of the rectangle.
     * @param    observer    object to be notified as more of 
     *                          the image is converted.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since    JDK1.0
     */
    public boolean drawImage(Image img, int x, int y,
			     int width, int height, 
			     ImageObserver observer) {

	return drawImage(img, x, y, width, height, null, observer);

    }

    /*
     * Draws as much of the specified image as is currently available.
     * The image is drawn with its top-left corner at 
     * (<i>x</i>,&nbsp;<i>y</i>) in this graphics context's coordinate 
     * space.  Transparent pixels are drawn in the specified
     * background color.
     * <p> 
     * This operation is equivalent to filling a rectangle of the
     * width and height of the specified image with the given color and then
     * drawing the image on top of it, but possibly more efficient.
     * <p>
     * This method returns immediately in all cases, even if the
     * complete image has not yet been loaded, and it has not been dithered 
     * and converted for the current output device.
     * <p>
     * If the image has not yet been completely loaded, then
     * <code>drawImage</code> returns <code>false</code>. As more of
     * the image becomes available, the process that draws the image notifies 
     * the specified image observer.
     * @param    img    the specified image to be drawn.
     *                  This method does nothing if <code>img</code> is null.
     * @param    x      the <i>x</i> coordinate.
     * @param    y      the <i>y</i> coordinate.
     * @param    bgcolor the background color to paint under the
     *                   non-opaque portions of the image.
     *			 In this WPathGraphics implementation,
     *			 this parameter can be null in which
     *			 case that background is made a transparent
     *			 white.
     * @param    observer    object to be notified as more of 
     *                          the image is converted.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since    JDK1.0
     */
    public boolean drawImage(Image img, int x, int y, 
			     Color bgcolor,
			     ImageObserver observer) {

	if (img == null) {
	    return true;
	}

	boolean result;
	int srcWidth = img.getWidth(null);
	int srcHeight = img.getHeight(null);

	if (srcWidth < 0 || srcHeight < 0) {
	    result = false;
	} else {
	    result = drawImage(img, x, y, srcWidth, srcHeight, bgcolor, observer);
	}

	return result;
    }

    /**
     * Draws as much of the specified image as has already been scaled
     * to fit inside the specified rectangle.
     * <p>
     * The image is drawn inside the specified rectangle of this 
     * graphics context's coordinate space, and is scaled if 
     * necessary. Transparent pixels are drawn in the specified
     * background color. 
     * This operation is equivalent to filling a rectangle of the
     * width and height of the specified image with the given color and then
     * drawing the image on top of it, but possibly more efficient.
     * <p>
     * This method returns immediately in all cases, even if the
     * entire image has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete then
     * <code>drawImage</code> returns <code>false</code>. As more of
     * the image becomes available, the process that draws the image notifies 
     * the specified image observer.
     * <p>
     * A scaled version of an image will not necessarily be
     * available immediately just because an unscaled version of the
     * image has been constructed for this output device.  Each size of
     * the image may be cached separately and generated from the original
     * data in a separate image production sequence.
     * @param    img       the specified image to be drawn.
     *                     This method does nothing if <code>img</code> is null.
     * @param    x         the <i>x</i> coordinate.
     * @param    y         the <i>y</i> coordinate.
     * @param    width     the width of the rectangle.
     * @param    height    the height of the rectangle.
     * @param    bgcolor   the background color to paint under the
     *                         non-opaque portions of the image.
     * @param    observer    object to be notified as more of 
     *                          the image is converted.
     * @see      java.awt.Image
     * @see      java.awt.image.ImageObserver
     * @see      java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since    JDK1.0
     */
    public boolean drawImage(Image img, int x, int y,
			     int width, int height, 
			     Color bgcolor,
			     ImageObserver observer) {

	if (img == null) {
	    return true;
	}

	boolean result;
	int srcWidth = img.getWidth(null);
	int srcHeight = img.getHeight(null);

	if (srcWidth < 0 || srcHeight < 0) {
	    result = false;
	} else {
	    result = drawImage(img,
			 x, y, x + width, y + height,
			 0, 0, srcWidth, srcHeight,
			 observer);
	}

	return result;
    }

    /**
     * Draws as much of the specified area of the specified image as is
     * currently available, scaling it on the fly to fit inside the
     * specified area of the destination drawable surface. Transparent pixels 
     * do not affect whatever pixels are already there.
     * <p>
     * This method returns immediately in all cases, even if the
     * image area to be drawn has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete then
     * <code>drawImage</code> returns <code>false</code>. As more of
     * the image becomes available, the process that draws the image notifies 
     * the specified image observer.
     * <p>
     * This method always uses the unscaled version of the image
     * to render the scaled rectangle and performs the required
     * scaling on the fly. It does not use a cached, scaled version
     * of the image for this operation. Scaling of the image from source
     * to destination is performed such that the first coordinate
     * of the source rectangle is mapped to the first coordinate of
     * the destination rectangle, and the second source coordinate is
     * mapped to the second destination coordinate. The subimage is
     * scaled and flipped as needed to preserve those mappings.
     * @param       img the specified image to be drawn
     * @param       dx1 the <i>x</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dy1 the <i>y</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dx2 the <i>x</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       dy2 the <i>y</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       sx1 the <i>x</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sy1 the <i>y</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sx2 the <i>x</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       sy2 the <i>y</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       observer object to be notified as more of the image is
     *                    scaled and converted.
     * @see         java.awt.Image
     * @see         java.awt.image.ImageObserver
     * @see         java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since       JDK1.1
     */
    public boolean drawImage(Image img,
			     int dx1, int dy1, int dx2, int dy2,
			     int sx1, int sy1, int sx2, int sy2,
			     ImageObserver observer) {

	return drawImage(img,
			 dx1, dy1, dx2, dy2,
			 sx1, sy1, sx2, sy2,
			 null, observer);
    }

    /**
     * Draws as much of the specified area of the specified image as is
     * currently available, scaling it on the fly to fit inside the
     * specified area of the destination drawable surface. 
     * <p>
     * Transparent pixels are drawn in the specified background color. 
     * This operation is equivalent to filling a rectangle of the
     * width and height of the specified image with the given color and then
     * drawing the image on top of it, but possibly more efficient.
     * <p>
     * This method returns immediately in all cases, even if the
     * image area to be drawn has not yet been scaled, dithered, and converted
     * for the current output device.
     * If the current output representation is not yet complete then
     * <code>drawImage</code> returns <code>false</code>. As more of
     * the image becomes available, the process that draws the image notifies 
     * the specified image observer.
     * <p>
     * This method always uses the unscaled version of the image
     * to render the scaled rectangle and performs the required
     * scaling on the fly. It does not use a cached, scaled version
     * of the image for this operation. Scaling of the image from source
     * to destination is performed such that the first coordinate
     * of the source rectangle is mapped to the first coordinate of
     * the destination rectangle, and the second source coordinate is
     * mapped to the second destination coordinate. The subimage is
     * scaled and flipped as needed to preserve those mappings.
     * @param       img the specified image to be drawn
     *                  This method does nothing if <code>img</code> is null.
     * @param       dx1 the <i>x</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dy1 the <i>y</i> coordinate of the first corner of the
     *                    destination rectangle.
     * @param       dx2 the <i>x</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       dy2 the <i>y</i> coordinate of the second corner of the
     *                    destination rectangle.
     * @param       sx1 the <i>x</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sy1 the <i>y</i> coordinate of the first corner of the
     *                    source rectangle.
     * @param       sx2 the <i>x</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       sy2 the <i>y</i> coordinate of the second corner of the
     *                    source rectangle.
     * @param       bgcolor the background color to paint under the
     *                    non-opaque portions of the image.
     * @param       observer object to be notified as more of the image is
     *                    scaled and converted.
     * @see         java.awt.Image
     * @see         java.awt.image.ImageObserver
     * @see         java.awt.image.ImageObserver#imageUpdate(java.awt.Image, int, int, int, int, int)
     * @since       JDK1.1
     */
    public boolean drawImage(Image img,
			     int dx1, int dy1, int dx2, int dy2,
			     int sx1, int sy1, int sx2, int sy2,
			     Color bgcolor,
			     ImageObserver observer) {

	if (img == null) {
	    return true;
	}

	int srcWidth = sx2 - sx1;
	int srcHeight = sy2 - sy1;

        /* Create a transform which describes the changes
         * from the source coordinates to the destination
         * coordinates. The scaling is determined by the
         * ratio of the two rectangles, while the translation
         * comes from the difference of their origins.
         */
        float scalex = (float) (dx2 - dx1) / srcWidth;
        float scaley = (float) (dy2 - dy1) / srcHeight;
        AffineTransform xForm
            = new AffineTransform(scalex,
                                  0,
                                  0,
                                  scaley,
                                  dx1 - (sx1 * scalex),
                                  dy1 - (sy1 * scaley));

	return drawImageToPlatform(img, xForm, bgcolor,
				   sx1, sy1, srcWidth, srcHeight, false);


    }

    /**
     * Draws an image, applying a transform from image space into user space
     * before drawing.
     * The transformation from user space into device space is done with
     * the current transform in the Graphics2D.
     * The given transformation is applied to the image before the
     * transform attribute in the Graphics2D state is applied.
     * The rendering attributes applied include the clip, transform,
     * and composite attributes. Note that the result is
     * undefined, if the given transform is noninvertible.
     * @param img The image to be drawn.
     *            This method does nothing if <code>img</code> is null.
     * @param xform The transformation from image space into user space.
     * @param obs The image observer to be notified as more of the image
     * is converted.
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public boolean drawImage(Image img,
			     AffineTransform xform,
			     ImageObserver obs) {

	if (img == null) {
	    return true;
	}

	boolean result;
	int srcWidth = img.getWidth(null);
	int srcHeight = img.getHeight(null);

	if (srcWidth < 0 || srcHeight < 0) {
	    result = false;
	} else {
	    result = drawImageToPlatform(img, xform, null,
					 0, 0, srcWidth, srcHeight, false);
	}

	return result;
    }

    /**
     * Draws a BufferedImage that is filtered with a BufferedImageOp.
     * The rendering attributes applied include the clip, transform
     * and composite attributes.  This is equivalent to:
     * <pre>
     * img1 = op.filter(img, null);
     * drawImage(img1, new AffineTransform(1f,0f,0f,1f,x,y), null);
     * </pre>
     * @param op The filter to be applied to the image before drawing.
     * @param img The BufferedImage to be drawn.
     *            This method does nothing if <code>img</code> is null.
     * @param x,y The location in user space where the image should be drawn.
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void drawImage(BufferedImage img,
			  BufferedImageOp op,
			  int x,
			  int y) {

	if (img == null) {
	    return;
	}

	boolean result;
	int srcWidth = img.getWidth(null);
	int srcHeight = img.getHeight(null);

        if (op != null) {
            img = op.filter(img, null);
        }
	if (srcWidth < 0 || srcHeight < 0) {
	    result = false;
	} else {
	    AffineTransform xform = new AffineTransform(1f,0f,0f,1f,x,y);
	    result = drawImageToPlatform(img, xform, null,
					 0, 0, srcWidth, srcHeight, false);
	}

    }

    /**
     * Draws an image, applying a transform from image space into user space
     * before drawing.
     * The transformation from user space into device space is done with
     * the current transform in the Graphics2D.
     * The given transformation is applied to the image before the
     * transform attribute in the Graphics2D state is applied.
     * The rendering attributes applied include the clip, transform,
     * and composite attributes. Note that the result is
     * undefined, if the given transform is noninvertible.
     * @param img The image to be drawn.
     *            This method does nothing if <code>img</code> is null.
     * @param xform The transformation from image space into user space.
     * @see #transform
     * @see #setTransform
     * @see #setComposite
     * @see #clip
     * @see #setClip
     */
    public void drawRenderedImage(RenderedImage img,
				  AffineTransform xform) {

	if (img == null) {
	    return;
	}

	BufferedImage bufferedImage = null;
	int srcWidth = img.getWidth();
	int srcHeight = img.getHeight();

	if (img instanceof BufferedImage) {
	    bufferedImage = (BufferedImage) img;
	} else {
	    bufferedImage = new BufferedImage(srcWidth, srcHeight,
					      BufferedImage.TYPE_INT_ARGB);
	    Graphics2D imageGraphics = bufferedImage.createGraphics();
	    imageGraphics.drawRenderedImage(img, xform);
	}

	drawImageToPlatform(bufferedImage, xform, null,
			    0, 0, srcWidth, srcHeight, false);

    }

}
