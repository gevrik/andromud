package basement.lab.mudclient.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import basement.lab.mudclient.SettingsManager;
import basement.lab.mudclient.TelnetConnectionThread;
import basement.lab.mudclient.bean.TriggerEngine;
import de.mud.terminal.VDUBuffer;
import de.mud.terminal.VDUDisplay;
import de.mud.terminal.vt320;

public class TerminalView extends View implements VDUDisplay {

	private final static String TAG = "mudclient.TerminalView";
	public static final long VIBRATE_DURATION = 30;

	private int mColorScheme;

	public Integer[] color;

	public int defaultFg = HostDatabase.DEFAULT_FG_COLOR;
	public int defaultBg = HostDatabase.DEFAULT_BG_COLOR;

	public vt320 buffer;

	private Context ctx;
	private int row = 30, column = 90;
	private boolean forcedSize = false;
	private Bitmap bitmap;
	public Paint defaultPaint, paint;
	public int charWidth = -1;
	public int charHeight = -1;
	private int charTop = -1;
	private float fontSize = -1;
	private final List<String> localOutput;
	private Canvas canvas = new Canvas();
	private boolean fullRedraw = false;

	private String logPath;
	private PrintWriter pw;
	private boolean isLogging = false;
	private boolean beepVibrate = false;
	private Vibrator vibrator;

	public TerminalView(Context context) {
		super(context);
		this.ctx = context;
		vibrator = (Vibrator) context
				.getSystemService(Context.VIBRATOR_SERVICE);
		beepVibrate = SettingsManager.isBellVibrate(ctx);
		defaultPaint = new Paint();
		defaultPaint.setAntiAlias(true);
		defaultPaint.setFakeBoldText(SettingsManager.isForceBold(context));
		defaultPaint.setTypeface(Typeface.MONOSPACE);
		defaultPaint.setStyle(Style.FILL);
		paint = new Paint();
		localOutput = new LinkedList<String>();
		buffer = new vt320(column, row) {
			@Override
			public void write(int b) {
			}

			@Override
			public void write(byte[] b) {
			}

			@Override
			public void debug(String notice) {
				Log.d(TAG, notice);
			}

			@Override
			public void beep() {
				if (beepVibrate) {
					vibrator.vibrate(VIBRATE_DURATION);
				}
			}
		};
		buffer.setBufferSize(SettingsManager.getScreenBuffer(context));
		buffer.setDisplay(this);
		setFontSize(SettingsManager.getFontSize(context));
		resetColors();
	}

	public void redraw() {
		postInvalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {
		onDraw();
		// draw the bridge bitmap if it exists
		canvas.drawBitmap(this.bitmap, 0, 0, paint);
	}

	public void onDraw() {
		int fg, bg;
		synchronized (buffer) {
			boolean entireDirty = buffer.update[0] || fullRedraw;
			boolean isWideCharacter = false;

			// walk through all lines in the buffer
			for (int l = 0; l < buffer.height; l++) {

				// check if this line is dirty and needs to be repainted
				// also check for entire-buffer dirty flags
				if (!entireDirty && !buffer.update[l + 1])
					continue;

				// reset dirty flag for this line
				buffer.update[l + 1] = false;

				// walk through all characters in this line
				for (int c = 0; c < buffer.width; c++) {
					int addr = 0;
					int currAttr = buffer.charAttributes[buffer.windowBase + l][c];

					{
						int fgcolor = defaultFg;
						// check if foreground color attribute is set
						if ((currAttr & VDUBuffer.COLOR_FG) != 0)
							fgcolor = ((currAttr & VDUBuffer.COLOR_FG) >> VDUBuffer.COLOR_FG_SHIFT) - 1;

						if (fgcolor < 8 && (currAttr & VDUBuffer.BOLD) != 0)
							fg = color[fgcolor + 8];
						else
							fg = color[fgcolor];
					}

					// check if background color attribute is set
					if ((currAttr & VDUBuffer.COLOR_BG) != 0)
						bg = color[((currAttr & VDUBuffer.COLOR_BG) >> VDUBuffer.COLOR_BG_SHIFT) - 1];
					else
						bg = color[defaultBg];

					// support character inversion by swapping background and
					// foreground color
					if ((currAttr & VDUBuffer.INVERT) != 0) {
						int swapc = bg;
						bg = fg;
						fg = swapc;
					}

					// set underlined attributes if requested
					defaultPaint
							.setUnderlineText((currAttr & VDUBuffer.UNDERLINE) != 0);

					isWideCharacter = (currAttr & VDUBuffer.FULLWIDTH) != 0;

					if (isWideCharacter)
						addr++;
					else {
						// determine the amount of continuous characters with
						// the same settings and print them all at once
						while (c + addr < buffer.width
								&& buffer.charAttributes[buffer.windowBase + l][c
										+ addr] == currAttr) {
							addr++;
						}
					}

					// Save the current clip region
					canvas.save(Canvas.CLIP_SAVE_FLAG);

					// clear this dirty area with background color
					defaultPaint.setColor(bg);
					if (isWideCharacter) {
						canvas.clipRect(c * charWidth, l * charHeight, (c + 2)
								* charWidth, (l + 1) * charHeight);
					} else {
						canvas.clipRect(c * charWidth, l * charHeight,
								(c + addr) * charWidth, (l + 1) * charHeight);
					}
					canvas.drawPaint(defaultPaint);

					// write the text string starting at 'c' for 'addr' number
					// of characters
					defaultPaint.setColor(fg);
					if ((currAttr & VDUBuffer.INVISIBLE) == 0)
						canvas.drawText(
								buffer.charArray[buffer.windowBase + l], c,
								addr, c * charWidth,
								(l * charHeight) - charTop, defaultPaint);

					// Restore the previous clip region
					canvas.restore();

					// advance to the next text block with different
					// characteristics
					c += addr - 1;
					if (isWideCharacter)
						c++;
				}
			}
			// reset entire-buffer flags
			buffer.update[0] = false;
		}
		fullRedraw = false;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		sizeChanged();
	}

	public void sizeChanged() {
		int width = this.getWidth();
		int height = this.getHeight();

		// Something has gone wrong with our layout; we're 0 width or height!
		if (width <= 0 || height <= 0)
			return;
		if (!forcedSize) {
			int newColumns, newRows;

			newColumns = width / charWidth;
			newRows = height / charHeight;

			if (newColumns == column && newRows == row)
				return;
			column = newColumns;
			row = newRows;
		}

		// reallocate new bitmap if needed
		boolean newBitmap = (bitmap == null)
				|| (bitmap.getWidth() != width || bitmap.getHeight() != height);
		if (newBitmap) {
			discardBitmap();
			bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
			canvas.setBitmap(bitmap);
		}
		// clear out any old buffer information
		defaultPaint.setColor(Color.BLACK);
		canvas.drawPaint(defaultPaint);

		// Stroke the border of the terminal if the size is being forced;
		if (forcedSize) {
			int borderX = (column * charWidth) + 1;
			int borderY = (row * charHeight) + 1;

			defaultPaint.setColor(Color.GRAY);
			defaultPaint.setStrokeWidth(0.0f);
			if (width >= borderX)
				canvas.drawLine(borderX, 0, borderX, borderY + 1, defaultPaint);
			if (height >= borderY)
				canvas.drawLine(0, borderY, borderX + 1, borderY, defaultPaint);
		}

		try {
			// request a terminal pty resize
			synchronized (buffer) {
				buffer.setScreenSize(column, row, true);
			}
		} catch (Exception e) {
			Log.e(TAG, "Problem while trying to resize screen or PTY", e);
		}

		synchronized (localOutput) {
			((vt320) buffer).reset();

			for (String line : localOutput)
				((vt320) buffer).putString(line);
		}
		// force full redraw with new buffer size
		fullRedraw = true;
		redraw();
	}

	public void resetColors() {
		HostDatabase db = new HostDatabase(ctx);
		mColorScheme = HostDatabase.DEFAULT_COLOR_SCHEME;
		color = db.getColorsForScheme(mColorScheme);
		int[] mDefaultColors = db.getDefaultColorsForScheme(mColorScheme);
		defaultFg = mDefaultColors[0];
		defaultBg = mDefaultColors[1];
	}

	public void setColor(int index, int red, int green, int blue) {
		if (index < color.length && index >= 16)
			color[index] = 0xff000000 | red << 16 | green << 8 | blue;
	}

	public void setVDUBuffer(VDUBuffer buffer) {
		this.buffer = (vt320) buffer;
		redraw();
	}

	public VDUBuffer getVDUBuffer() {
		return buffer;
	}

	public void updateScrollBar() {
		return;
	}

	private void discardBitmap() {
		if (bitmap != null)
			bitmap.recycle();
		bitmap = null;
	}

	/**
	 * Request a different font size. Will make call to parentChanged() to make
	 * sure we resize PTY if needed.
	 */
	private final void setFontSize(float size) {
		if (size <= 0.0)
			return;
		defaultPaint.setTextSize(size);
		fontSize = size;
		// read new metrics to get exact pixel dimensions
		FontMetrics fm = defaultPaint.getFontMetrics();
		charTop = (int) Math.ceil(fm.top);

		float[] widths = new float[1];
		defaultPaint.getTextWidths("X", widths);
		charWidth = (int) Math.ceil(widths[0]);
		charHeight = (int) Math.ceil(fm.descent - fm.top);
		redraw();
	}

	float[] width = new float[TelnetConnectionThread.BUFFER_SIZE];
	byte[] wideAttribute = new byte[TelnetConnectionThread.BUFFER_SIZE];

	public void putString(String str) {
		buffer.strBuff = "";
		putString(str.toCharArray(), 0, str.toCharArray().length);
		String strBuff = new String(buffer.strBuff);
		TriggerEngine.addText(strBuff);
		if (isLogging && pw != null) {
			pw.print(strBuff);
		}
	}

	private void putString(char[] text, int start, int len) {
		defaultPaint.getTextWidths(text, start, len, width);
		for (int i = 0; i < len - start; i++)
			wideAttribute[i] = (byte) (((int) width[i] != charWidth) ? 1 : 0);
		buffer.putString(text, wideAttribute, start, len);
	}

	public void requestLog(String filepath, boolean start) {
		this.logPath = filepath;
		if (start) {
			try {
				pw = new PrintWriter(new FileOutputStream(filepath, true));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			if (pw != null) {
				pw.close();
			}
		}
		this.isLogging = start;
	}

	// This was taken from
	// http://geekswithblogs.net/casualjim/archive/2005/12/01/61722.aspx
	private final static String urlRegex = "(?:(?:ht|f)tp(?:s?)\\:\\/\\/|~/|/)?(?:\\w+:\\w+@)?(?:(?:[-\\w]+\\.)+(?:com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|museum|travel|[a-z]{2}))(?::[\\d]{1,5})?(?:(?:(?:/(?:[-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|/)+|\\?|#)?(?:(?:\\?(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)(?:&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*(?:#(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?";
	private static Pattern urlPattern = null;

	/**
	 * @return
	 */
	public List<String> scanForURLs() {
		List<String> urls = new LinkedList<String>();

		if (urlPattern == null)
			urlPattern = Pattern.compile(urlRegex);

		char[] visibleBuffer = new char[buffer.height * buffer.width];
		for (int l = 0; l < buffer.height; l++)
			System.arraycopy(buffer.charArray[buffer.windowBase + l], 0,
					visibleBuffer, l * buffer.width, buffer.width);

		Matcher urlMatcher = urlPattern.matcher(new String(visibleBuffer));
		while (urlMatcher.find())
			urls.add(urlMatcher.group());

		return urls;
	}
}
