package basement.lab.mudclient.utils;

import basement.lab.mudclient.bean.Colors;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class HostDatabase extends RobustSQLiteOpenHelper {

	private final static int DB_VERSION = 1;
	private final static String DB_NAME = "colors";

	private Object dbLock;

	public HostDatabase(Context context) {
		super(context, DB_NAME, null, DB_VERSION);

		getWritableDatabase().close();

		dbLock = new Object();
	}

	public final static String TABLE_COLORS = "colors";
	public final static String FIELD_COLOR_SCHEME = "scheme";
	public final static String FIELD_COLOR_NUMBER = "number";
	public final static String FIELD_COLOR_VALUE = "value";

	public final static String TABLE_COLOR_DEFAULTS = "colorDefaults";
	public final static String FIELD_COLOR_FG = "fg";
	public final static String FIELD_COLOR_BG = "bg";

	public final static int DEFAULT_FG_COLOR = 7;
	public final static int DEFAULT_BG_COLOR = 0;
	public static final int DEFAULT_COLOR_SCHEME = 0;

	// Table creation strings
	public static final String CREATE_TABLE_COLOR_DEFAULTS = "CREATE TABLE "
			+ TABLE_COLOR_DEFAULTS + " (" + FIELD_COLOR_SCHEME
			+ " INTEGER NOT NULL, " + FIELD_COLOR_FG
			+ " INTEGER NOT NULL DEFAULT " + DEFAULT_FG_COLOR + ", "
			+ FIELD_COLOR_BG + " INTEGER NOT NULL DEFAULT " + DEFAULT_BG_COLOR
			+ ")";
	public static final String CREATE_TABLE_COLOR_DEFAULTS_INDEX = "CREATE INDEX "
			+ TABLE_COLOR_DEFAULTS
			+ FIELD_COLOR_SCHEME
			+ "index ON "
			+ TABLE_COLOR_DEFAULTS + " (" + FIELD_COLOR_SCHEME + ");";

	static {
		addTableName(TABLE_COLORS);
		addIndexName(TABLE_COLORS + FIELD_COLOR_SCHEME + "index");
		addTableName(TABLE_COLOR_DEFAULTS);
		addIndexName(TABLE_COLOR_DEFAULTS + FIELD_COLOR_SCHEME + "index");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		super.onCreate(db);

		db.execSQL("CREATE TABLE " + TABLE_COLORS
				+ " (_id INTEGER PRIMARY KEY, " + FIELD_COLOR_NUMBER
				+ " INTEGER, " + FIELD_COLOR_VALUE + " INTEGER, "
				+ FIELD_COLOR_SCHEME + " INTEGER)");

		db
				.execSQL("CREATE INDEX " + TABLE_COLORS + FIELD_COLOR_SCHEME
						+ "index ON " + TABLE_COLORS + " ("
						+ FIELD_COLOR_SCHEME + ");");
		db.execSQL(CREATE_TABLE_COLOR_DEFAULTS);
		db.execSQL(CREATE_TABLE_COLOR_DEFAULTS_INDEX);
	}

	@Override
	public void onRobustUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion) throws SQLiteException {
	}

	public Integer[] getColorsForScheme(int scheme) {
		Integer[] colors = Colors.defaults.clone();
		synchronized (dbLock) {
			SQLiteDatabase db = getReadableDatabase();

			Cursor c = db.query(TABLE_COLORS, new String[] {
					FIELD_COLOR_NUMBER, FIELD_COLOR_VALUE }, FIELD_COLOR_SCHEME
					+ " = ?", new String[] { String.valueOf(scheme) }, null,
					null, null);

			while (c.moveToNext()) {
				colors[c.getInt(0)] = new Integer(c.getInt(1));
			}

			c.close();
			db.close();
		}
		return colors;
	}

	public void setColorForScheme(int scheme, int number, int value) {
		SQLiteDatabase db;

		String schemeWhere;
		schemeWhere = FIELD_COLOR_SCHEME + " = ?";

		if (value == Colors.defaults[number]) {
			String[] whereArgs = new String[1];

			whereArgs[0] = String.valueOf(number);

			synchronized (dbLock) {
				db = getWritableDatabase();

				db.delete(TABLE_COLORS, FIELD_COLOR_NUMBER + " = ? AND "
						+ schemeWhere, new String[] { String.valueOf(number) });

				db.close();
			}
		} else {
			ContentValues values = new ContentValues();
			values.put(FIELD_COLOR_NUMBER, number);
			values.put(FIELD_COLOR_VALUE, value);

			String[] whereArgs = null;

			whereArgs = new String[] { String.valueOf(scheme) };

			synchronized (dbLock) {
				db = getWritableDatabase();
				int rowsAffected = db.update(TABLE_COLORS, values, schemeWhere,
						whereArgs);

				if (rowsAffected == 0) {
					db.insert(TABLE_COLORS, null, values);
				}

				db.close();
			}
		}
	}

	public void setGlobalColor(int number, int value) {
		setColorForScheme(DEFAULT_COLOR_SCHEME, number, value);
	}

	public int[] getDefaultColorsForScheme(int scheme) {
		int[] colors = new int[] { DEFAULT_FG_COLOR, DEFAULT_BG_COLOR };
		synchronized (dbLock) {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(TABLE_COLOR_DEFAULTS, new String[] {
					FIELD_COLOR_FG, FIELD_COLOR_BG }, FIELD_COLOR_SCHEME
					+ " = ?", new String[] { String.valueOf(scheme) }, null,
					null, null);
			if (c.moveToFirst()) {
				colors[0] = c.getInt(0);
				colors[1] = c.getInt(1);
			}
			c.close();
			db.close();
		}
		return colors;
	}

	public int[] getGlobalDefaultColors() {
		return getDefaultColorsForScheme(DEFAULT_COLOR_SCHEME);
	}

	public void setDefaultColorsForScheme(int scheme, int fg, int bg) {
		SQLiteDatabase db;

		String schemeWhere = null;
		String[] whereArgs;

		schemeWhere = FIELD_COLOR_SCHEME + " = ?";
		whereArgs = new String[] { String.valueOf(scheme) };

		ContentValues values = new ContentValues();
		values.put(FIELD_COLOR_FG, fg);
		values.put(FIELD_COLOR_BG, bg);

		synchronized (dbLock) {
			db = getWritableDatabase();

			int rowsAffected = db.update(TABLE_COLOR_DEFAULTS, values,
					schemeWhere, whereArgs);

			if (rowsAffected == 0) {
				values.put(FIELD_COLOR_SCHEME, scheme);
				db.insert(TABLE_COLOR_DEFAULTS, null, values);
			}

			db.close();
		}
	}
}
