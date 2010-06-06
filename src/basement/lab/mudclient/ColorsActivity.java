package basement.lab.mudclient;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import basement.lab.mudclient.bean.Colors;
import basement.lab.mudclient.utils.HostDatabase;
import basement.lab.mudclient.utils.UberColorPickerDialog;
import basement.lab.mudclient.utils.UberColorPickerDialog.OnColorChangedListener;

public class ColorsActivity extends Activity implements OnItemClickListener,
		OnColorChangedListener, OnItemSelectedListener {
	private GridView mColorGrid;
	private Spinner mFgSpinner;
	private Spinner mBgSpinner;

	private int mColorScheme;
	private List<Integer> mColorList;
	private int[] mDefaultColors;
	private HostDatabase hostdb;

	private int mCurrentColor = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (SettingsManager.isFullScreen(this)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		setContentView(R.layout.act_colors);

		mColorScheme = HostDatabase.DEFAULT_COLOR_SCHEME;
		hostdb = new HostDatabase(this);

		mColorList = Arrays.asList(hostdb.getColorsForScheme(mColorScheme));
		mDefaultColors = hostdb.getDefaultColorsForScheme(mColorScheme);

		mColorGrid = (GridView) findViewById(R.id.color_grid);
		mColorGrid.setAdapter(new ColorsAdapter(true));
		mColorGrid.setOnItemClickListener(this);
		mColorGrid.setSelection(0);

		mFgSpinner = (Spinner) findViewById(R.id.fg);
		mFgSpinner.setAdapter(new ColorsAdapter(false));
		mFgSpinner.setSelection(mDefaultColors[0]);
		mFgSpinner.setOnItemSelectedListener(this);

		mBgSpinner = (Spinner) findViewById(R.id.bg);
		mBgSpinner.setAdapter(new ColorsAdapter(false));
		mBgSpinner.setSelection(mDefaultColors[1]);
		mBgSpinner.setOnItemSelectedListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (hostdb != null) {
			hostdb.close();
			hostdb = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (hostdb == null)
			hostdb = new HostDatabase(this);
	}

	private class ColorsAdapter extends BaseAdapter {
		private boolean mSquareViews;

		public ColorsAdapter(boolean squareViews) {
			mSquareViews = squareViews;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ColorView c;

			if (convertView == null) {
				c = new ColorView(ColorsActivity.this, mSquareViews);
			} else {
				c = (ColorView) convertView;
			}

			c.setColor(mColorList.get(position));
			c.setNumber(position + 1);

			return c;
		}

		public int getCount() {
			return mColorList.size();
		}

		public Object getItem(int position) {
			return mColorList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}
	}

	private class ColorView extends View {
		private boolean mSquare;

		private Paint mTextPaint;
		private Paint mShadowPaint;

		// Things we paint
		private int mBackgroundColor;
		private String mText;

		private int mAscent;
		private int mWidthCenter;
		private int mHeightCenter;

		public ColorView(Context context, boolean square) {
			super(context);

			mSquare = square;

			mTextPaint = new Paint();
			mTextPaint.setAntiAlias(true);
			mTextPaint.setTextSize(16);
			mTextPaint.setColor(0xFFFFFFFF);
			mTextPaint.setTextAlign(Paint.Align.CENTER);

			mShadowPaint = new Paint(mTextPaint);
			mShadowPaint.setStyle(Paint.Style.STROKE);
			mShadowPaint.setStrokeCap(Paint.Cap.ROUND);
			mShadowPaint.setStrokeJoin(Paint.Join.ROUND);
			mShadowPaint.setStrokeWidth(4f);
			mShadowPaint.setColor(0xFF000000);

			setPadding(10, 10, 10, 10);
		}

		public void setColor(int color) {
			mBackgroundColor = color;
		}

		public void setNumber(int number) {
			mText = Integer.toString(number);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int width = measureWidth(widthMeasureSpec);

			int height;
			if (mSquare)
				height = width;
			else
				height = measureHeight(heightMeasureSpec);

			mAscent = (int) mTextPaint.ascent();
			mWidthCenter = width / 2;
			mHeightCenter = height / 2 - mAscent / 2;

			setMeasuredDimension(width, height);
		}

		private int measureWidth(int measureSpec) {
			int result = 0;
			int specMode = MeasureSpec.getMode(measureSpec);
			int specSize = MeasureSpec.getSize(measureSpec);

			if (specMode == MeasureSpec.EXACTLY) {
				// We were told how big to be
				result = specSize;
			} else {
				// Measure the text
				result = (int) mTextPaint.measureText(mText) + getPaddingLeft()
						+ getPaddingRight();
				if (specMode == MeasureSpec.AT_MOST) {
					// Respect AT_MOST value if that was what is called for by
					// measureSpec
					result = Math.min(result, specSize);
				}
			}

			return result;
		}

		private int measureHeight(int measureSpec) {
			int result = 0;
			int specMode = MeasureSpec.getMode(measureSpec);
			int specSize = MeasureSpec.getSize(measureSpec);

			mAscent = (int) mTextPaint.ascent();
			if (specMode == MeasureSpec.EXACTLY) {
				// We were told how big to be
				result = specSize;
			} else {
				// Measure the text (beware: ascent is a negative number)
				result = (int) (-mAscent + mTextPaint.descent())
						+ getPaddingTop() + getPaddingBottom();
				if (specMode == MeasureSpec.AT_MOST) {
					// Respect AT_MOST value if that was what is called for by
					// measureSpec
					result = Math.min(result, specSize);
				}
			}
			return result;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			canvas.drawColor(mBackgroundColor);

			canvas.drawText(mText, mWidthCenter, mHeightCenter, mShadowPaint);
			canvas.drawText(mText, mWidthCenter, mHeightCenter, mTextPaint);
		}
	}

	private void editColor(int colorNumber) {
		mCurrentColor = colorNumber;
		new UberColorPickerDialog(this, this, mColorList.get(colorNumber))
				.show();
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		editColor(position);
	}

	public void onNothingSelected(AdapterView<?> arg0) {
	}

	public void colorChanged(int value) {
		hostdb.setGlobalColor(mCurrentColor, value);
		mColorList.set(mCurrentColor, value);
		mColorGrid.invalidateViews();
	}

	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		boolean needUpdate = false;
		if (parent == mFgSpinner) {
			if (position != mDefaultColors[0]) {
				mDefaultColors[0] = position;
				needUpdate = true;
			}
		} else if (parent == mBgSpinner) {
			if (position != mDefaultColors[1]) {
				mDefaultColors[1] = position;
				needUpdate = true;
			}
		}

		if (needUpdate)
			hostdb.setDefaultColorsForScheme(mColorScheme, mDefaultColors[0],
					mDefaultColors[1]);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem reset = menu.add(R.string.menu_colors_reset);
		reset.setAlphabeticShortcut('r');
		reset.setNumericShortcut('1');
		reset.setIcon(android.R.drawable.ic_menu_revert);
		reset.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem arg0) {
				// Reset each individual color to defaults.
				for (int i = 0; i < Colors.defaults.length; i++) {
					if (mColorList.get(i) != Colors.defaults[i]) {
						hostdb.setGlobalColor(i, Colors.defaults[i]);
						mColorList.set(i, Colors.defaults[i]);
					}
				}
				mColorGrid.invalidateViews();

				// Reset the default FG/BG colors as well.
				mFgSpinner.setSelection(HostDatabase.DEFAULT_FG_COLOR);
				mBgSpinner.setSelection(HostDatabase.DEFAULT_BG_COLOR);
				hostdb.setDefaultColorsForScheme(
						HostDatabase.DEFAULT_COLOR_SCHEME,
						HostDatabase.DEFAULT_FG_COLOR,
						HostDatabase.DEFAULT_BG_COLOR);

				return true;
			}
		});

		return true;
	}
}
