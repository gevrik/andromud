package basement.lab.mudclient;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import basement.lab.mudclient.bean.ServerInfo;

public class SettingsManager extends PreferenceActivity {

	private static final String shakeforurl = "shakeforurl";
	private static final boolean defaultShakeforurl = true;

	private static final String notification = "notification";
	private static final boolean defaultNotification = true;

	private static final String bellVibrate = "bellVibrate";
	private static final boolean defaultBellVibrate = true;

	private static final String logPath = "logPath";
	private static final String defaultLogPath = "/sdcard/AndroMudLog.txt";

	private static final boolean defaultUseShortcut = true;
	private static final String useShortcut = "useShortcut";

	private static final boolean defaultForceBold = true;
	private static final String forceBold = "forcebold";

	private static final String fullScreen = "fullscreen";
	private static final boolean defaultFullScreen = false;

	private static final String screenBuffer = "screenBuffer";
	private static final int defaultScreenBuffer = 300;

	private static final String cmdHistory = "commandhistory";
	private static final int defaultCommandHistory = 20;

	private static final String fontSize = "fontsize";
	private static final int defaultFontSize = 11;

	private static final String encoding = "encoding";
	private static final String defaultEncoding = "UTF-8";

	private static final String shortcutkey = "shortcutkey";
	private static final String defaultShortcut = "";

	private static final String wake = "wake";
	private static final boolean defaultWake = true;

	private static final String wifi = "wifi";
	private static final boolean defaultWifi = true;

	public static final String usePostLogin = "usePostLogin";
	public static final String postLoginAsPassword = "postLoginAsPassword";

	private static final boolean defaultPostLoginAsPassword = true;
	private static final boolean defaultUsePostLogin = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (SettingsManager.isFullScreen(this)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		addPreferencesFromResource(R.xml.prefs);
		final ListPreference charsetPref = (ListPreference) findPreference(encoding);
		if (CharsetHolder.isInitialized()) {
			initCharsetPref(charsetPref);
		} else {
			String[] currentCharsetPref = new String[1];
			currentCharsetPref[0] = charsetPref.getValue();
			charsetPref.setEntryValues(currentCharsetPref);
			charsetPref.setEntries(currentCharsetPref);
			new Thread(new Runnable() {
				public void run() {
					initCharsetPref(charsetPref);
				}
			}).start();
		}
	}

	public static boolean isShakeForURL(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				shakeforurl, defaultShakeforurl);
	}

	public static boolean isNeedNotification(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				notification, defaultNotification);
	}

	public static boolean isBellVibrate(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				bellVibrate, defaultBellVibrate);
	}

	public static boolean isUseShortcut(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				useShortcut, defaultUseShortcut);
	}

	public static boolean isUsePostLogin(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				usePostLogin, defaultUsePostLogin);
	}

	public static boolean isPostLoginAsPassword(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				postLoginAsPassword, defaultPostLoginAsPassword);
	}

	public static boolean isKeepWifi(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				wifi, defaultWifi);
	}

	public static boolean isKeepAwake(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				wake, defaultWake);
	}

	public static boolean isForceBold(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				forceBold, defaultForceBold);
	}

	private void initCharsetPref(final ListPreference charsetPref) {
		charsetPref.setEntryValues(CharsetHolder.getCharsetIds());
		charsetPref.setEntries(CharsetHolder.getCharsetNames());
	}

	public static int getScreenBuffer(Context ctx) {
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
				ctx).getString(screenBuffer, defaultScreenBuffer + ""));
	}

	public static boolean isFullScreen(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
				fullScreen, defaultFullScreen);
	}

	public static int getCommandHistory(Context ctx) {
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
				ctx).getString(cmdHistory, defaultCommandHistory + ""));
	}

	public static int getFontSize(Context ctx) {
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
				ctx).getString(fontSize, defaultFontSize + ""));
	}

	public static String getEncoding(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
				encoding, defaultEncoding);
	}

	public static String getLogPath(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
				logPath, defaultLogPath);
	}

	public static class CharsetHolder {
		private static boolean initialized = false;

		private static CharSequence[] charsetIds;
		private static CharSequence[] charsetNames;

		public static CharSequence[] getCharsetNames() {
			if (charsetNames == null)
				initialize();
			return charsetNames;
		}

		public static CharSequence[] getCharsetIds() {
			if (charsetIds == null)
				initialize();
			return charsetIds;
		}

		private synchronized static void initialize() {
			if (initialized)
				return;
			List<CharSequence> charsetIdsList = new LinkedList<CharSequence>();
			List<CharSequence> charsetNamesList = new LinkedList<CharSequence>();
			for (Entry<String, Charset> entry : Charset.availableCharsets()
					.entrySet()) {
				Charset c = entry.getValue();
				if (c.canEncode() && c.isRegistered()) {
					String key = entry.getKey();
					if (key.startsWith("cp")) {
						charsetIdsList.add("CP437");
						charsetNamesList.add("CP437");
					}
					charsetIdsList.add(entry.getKey());
					charsetNamesList.add(c.displayName());
				}
			}
			charsetIds = charsetIdsList.toArray(new CharSequence[charsetIdsList
					.size()]);
			charsetNames = charsetNamesList
					.toArray(new CharSequence[charsetNamesList.size()]);
			initialized = true;
		}

		public static boolean isInitialized() {
			return initialized;
		}
	}

	public static String getShortcutCommand(Context ctx, int position) {
		String cmd = PreferenceManager.getDefaultSharedPreferences(ctx)
				.getString(shortcutkey + position, defaultShortcut).trim();
		if (cmd.length() == 0)
			return null;
		return cmd;
	}

	private final static String NICKNAME = "SERVER_NAME";
	private final static String HOST = "SERVER_IP";
	private final static String PORT = "SERVER_PORT";
	private final static String MUD_FILE = "MUD_FILE";
	private final static String POST_LOGIN = "POST_LOGIN";

	public static ArrayList<ServerInfo> getServerList(Activity ctx) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		int servers = prefs.getInt("SERVER_COUNT", 0);
		ArrayList<ServerInfo> list = new ArrayList<ServerInfo>();
		for (int x = 0; x < servers; x++) {
			list.add(new ServerInfo(prefs.getString(NICKNAME + x, "0"), prefs
					.getString(HOST + x, "0"), prefs.getInt(PORT + x, 0), prefs
					.getString(MUD_FILE + x, ""), prefs.getString(POST_LOGIN
					+ x, "")));
		}
		return list;
	}

	public static void setServerList(Activity ctx, ArrayList<ServerInfo> list) {
		SharedPreferences.Editor edit = PreferenceManager
				.getDefaultSharedPreferences(ctx).edit();
		edit.putInt("SERVER_COUNT", list.size());

		for (int x = 0; x < list.size(); x++) {
			ServerInfo s = list.get(x);
			edit.putString(NICKNAME + x, s.nickName);
			edit.putString(HOST + x, s.IP);
			edit.putInt(PORT + x, s.Port);
			edit.putString(MUD_FILE + x, s.mudfile);
			edit.putString(POST_LOGIN + x, s.postLogin);
		}
		edit.commit();
	}
}
