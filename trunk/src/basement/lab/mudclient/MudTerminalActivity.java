package basement.lab.mudclient;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import basement.lab.mudclient.bean.ScriptEngine;
import basement.lab.mudclient.bean.ServerInfo;
import basement.lab.mudclient.bean.SessionData;
import basement.lab.mudclient.bean.TriggerEngine;
import basement.lab.mudclient.utils.ShakeListener;
import basement.lab.mudclient.utils.TerminalView;

public class MudTerminalActivity extends Activity implements OnClickListener {
	public static final String Server = "basement.lab.mudclient.serverinfo";
	public static final String TAG = "mudclient.MudTerminal";

	private static final int MENU_CONNECT_SWITCH = 1;
	private static final int MENU_ALIAS = 3;
	private static final int MENU_CLEAR_LOG = 4;
	private static final int MENU_LOG_SWITCH = 5;
	private static final int MENU_TRIGGER_SETUP = 6;
	private static final int MENU_TRIGGER_SWITCH = 7;
	private static final int MENU_SCAN_URL = 8;

	private static final int NOTIFY_ID = 1;
	private static int HISTORY_BUFFER_SIZE = 20;

	private boolean isLogging = false;

	private String logPath;
	private String inputBuffer;
	private List<String> commandHistory;
	private SendQueue sendData;
	private ThreadListener tlisten;
	private ServerInfo server;
	private int historypos = 0;

	private EditText cmd;
	private TerminalView terminal;
	private FrameLayout container;
	private TableLayout buttonList;
	private Button[] shortcutkeys;
	private String[] shortcutList;

	private ShakeListener shaker;

	private Thread connectionThread = null;
	private TelnetConnectionThread telnet = null;
	private PowerManager.WakeLock wakelock = null;
	private NotificationManager notifier;
	final GestureDetector detect = new GestureDetector(
			new GestureDetector.SimpleOnGestureListener() {
				private float totalY = 0;

				@Override
				public boolean onScroll(MotionEvent e1, MotionEvent e2,
						float distanceX, float distanceY) {
					if (terminal == null) {
						return false;
					}

					if (e1 == null || e2 == null)
						return false;
					if (e2.getAction() == MotionEvent.ACTION_UP) {
						totalY = 0;
					}
					if (Math.abs(e1.getX() - e2.getX()) < ViewConfiguration
							.getTouchSlop() * 4) {
						totalY += distanceY;
						final int moved = (int) (totalY / terminal.charHeight);
						if (moved != 0) {
							int base = terminal.buffer.getWindowBase();
							terminal.buffer.setWindowBase(base + moved);
							totalY = 0;
							return true;
						}
					}
					return false;
				}
			});

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakelock = manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);

		notifier = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		setContentView(R.layout.main);
		HISTORY_BUFFER_SIZE = SettingsManager.getCommandHistory(this);
		cmd = (EditText) findViewById(R.id.cmdText);
		container = (FrameLayout) findViewById(R.id.terminalContainer);

		if (savedInstanceState == null) {
			commandHistory = new ArrayList<String>();
			sendData = new SendQueue();
			terminal = new TerminalView(this);
		} else {
			SessionData sd = ((SessionData) this
					.getLastNonConfigurationInstance());
			commandHistory = sd.commandHistory;
			connectionThread = sd.connection;
			sendData = sd.queue;
			tlisten = new ThreadListener();
			sendData.enqueue(tlisten);
			this.server = sd.server;
			terminal = sd.terminal;
			inputBuffer = (String) savedInstanceState.getString("INPUT_TEXT");
		}
		container.addView(terminal);
		cmd.setText(inputBuffer);
		cmd.setOnKeyListener(new CommandListener());
		terminal.setLongClickable(true);
		terminal.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
				return detect.onTouchEvent(event);
			}
		});

	}

	@Override
	protected void onStart() {
		super.onStart();
		notifier.cancel(NOTIFY_ID);
		TriggerEngine.registerMudTerminal(this);
		if (SettingsManager.isFullScreen(this)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		buttonList = (TableLayout) findViewById(R.id.buttonList);
		if (!SettingsManager.isUseShortcut(this)) {
			buttonList.setVisibility(View.GONE);
		}
		shortcutkeys = new Button[5];
		shortcutList = new String[5];
		shortcutkeys[0] = (Button) findViewById(R.id.shortcut1);
		shortcutkeys[1] = (Button) findViewById(R.id.shortcut2);
		shortcutkeys[2] = (Button) findViewById(R.id.shortcut3);
		shortcutkeys[3] = (Button) findViewById(R.id.shortcut4);
		shortcutkeys[4] = (Button) findViewById(R.id.shortcut5);

		for (int i = 0; i < 5; i++) {
			shortcutList[i] = SettingsManager.getShortcutCommand(this, i + 1);
			if (shortcutList[i] != null) {
				shortcutkeys[i].setOnClickListener(this);
			} else {
				shortcutkeys[i].setEnabled(false);
			}
		}
		if (connectionThread == null || !connectionThread.isAlive()) {
			ServerInfo server = (ServerInfo) getIntent().getSerializableExtra(
					Server);
			if (server != null) {
				this.server = server;
				try {
					ScriptEngine.init(server.mudfile);
				} catch (IOException e) {
					Toast.makeText(this,
							"Alias File Not Found. Creating a New File.", 0)
							.show();
				} catch (Exception e) {
					Toast.makeText(this, e.toString(), 0).show();
				}
				connectToServer(server);
			} else {
				Toast.makeText(this, R.string.error_unableFindServer, 0).show();
			}
		}
		if (wakelock != null && SettingsManager.isKeepAwake(this)) {
			Log.d(TAG, "screen lock acquired.");
			wakelock.acquire();
		}
		logPath = SettingsManager.getLogPath(this);
		if (SettingsManager.isShakeForURL(this)) {
			shaker = new ShakeListener(this) {
				@Override
				public void onShake() {
					scanForURL();
				}
			};
			shaker.start();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (SettingsManager.isShakeForURL(this)) {
			shaker.stop();
		}
		if (SettingsManager.isNeedNotification(this)
				&& connectionThread != null && connectionThread.isAlive()
				&& !connectionThread.isInterrupted()) {
			String text = String.format(getResources().getString(
					R.string.mud_still_running), server.nickName);
			Notification notice = new Notification(R.drawable.smallicon, text,
					0);
			String contentText = getResources().getString(
					R.string.mud_still_running_desc);
			PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(),
					0);
			notice.setLatestEventInfo(this, text, contentText, pi);
			notifier.notify(NOTIFY_ID, notice);
		} else {
			notifier.cancel(NOTIFY_ID);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		// allow the screen to dim and fall asleep
		if (wakelock != null && wakelock.isHeld()) {
			Log.d(TAG, "screen lock released.");
			wakelock.release();
		}
		if (isLogging) {
			isLogging = false;
			terminal.requestLog(null, false);
		}
	}

	public void onClick(View v) {
		int cmd = -1;
		switch (v.getId()) {
		case R.id.shortcut1:
			cmd = 0;
			break;
		case R.id.shortcut2:
			cmd = 1;
			break;
		case R.id.shortcut3:
			cmd = 2;
			break;
		case R.id.shortcut4:
			cmd = 3;
			break;
		case R.id.shortcut5:
			cmd = 4;
			break;
		}
		if (cmd != -1 && shortcutList[cmd] != null) {
			sendCommand(shortcutList[cmd]);
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		container.removeAllViews();
		SessionData session = new SessionData();
		session.connection = connectionThread;
		session.queue = sendData;
		session.commandHistory = commandHistory;
		session.server = server;
		session.terminal = this.terminal;
		return session;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("INPUT_TEXT", inputBuffer);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_CONNECT_SWITCH, 0, R.string.disconnect);
		menu.add(0, MENU_ALIAS, 2, R.string.alias);
		menu.add(2, MENU_TRIGGER_SETUP, 3, R.string.trigger_setup);
		menu.add(2, MENU_TRIGGER_SWITCH, 4, R.string.trigger_enable);
		menu.add(1, MENU_LOG_SWITCH, 5, R.string.startLog);
		menu.add(3, MENU_CLEAR_LOG, 6, R.string.clearLog);
		if (!SettingsManager.isShakeForURL(this)) {
			menu.add(3, MENU_SCAN_URL, 7, R.string.urlscan);
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(MENU_TRIGGER_SETUP).setIcon(R.drawable.trigger);
		menu.findItem(MENU_ALIAS).setIcon(android.R.drawable.ic_menu_share);
		menu.findItem(MENU_CLEAR_LOG).setIcon(
				android.R.drawable.ic_menu_recent_history);
		if (!SettingsManager.isShakeForURL(this)) {
			menu.findItem(MENU_SCAN_URL).setIcon(
					android.R.drawable.ic_menu_search);
		}
		if (isLogging) {
			menu.findItem(MENU_CLEAR_LOG).setEnabled(false);
			menu.findItem(MENU_LOG_SWITCH).setIcon(
					android.R.drawable.button_onoff_indicator_off);
			menu.findItem(MENU_LOG_SWITCH).setTitle(R.string.stopLog);
		} else {
			menu.findItem(MENU_CLEAR_LOG).setEnabled(true);
			menu.findItem(MENU_LOG_SWITCH).setIcon(
					android.R.drawable.button_onoff_indicator_on);
			menu.findItem(MENU_LOG_SWITCH).setTitle(R.string.startLog);
		}

		if (TriggerEngine.useTrigger) {
			menu.findItem(MENU_TRIGGER_SWITCH).setTitle(
					R.string.trigger_disable);
			menu.findItem(MENU_TRIGGER_SWITCH).setIcon(
					android.R.drawable.button_onoff_indicator_off);
			menu.findItem(MENU_TRIGGER_SETUP).setEnabled(false);
		} else {
			menu.findItem(MENU_TRIGGER_SETUP).setEnabled(true);
			menu.findItem(MENU_TRIGGER_SWITCH)
					.setTitle(R.string.trigger_enable);
			menu.findItem(MENU_TRIGGER_SWITCH).setIcon(
					android.R.drawable.button_onoff_indicator_on);
		}

		if (this.connectionThread != null && this.connectionThread.isAlive()) {
			menu.findItem(MENU_CONNECT_SWITCH).setIcon(
					android.R.drawable.ic_input_delete);
			menu.findItem(MENU_CONNECT_SWITCH).setTitle(R.string.disconnect);
		} else {
			menu.findItem(MENU_CONNECT_SWITCH).setIcon(
					android.R.drawable.ic_menu_call);
			menu.findItem(MENU_CONNECT_SWITCH).setTitle(R.string.reconnect);
		}
		return true;
	}

	public void connectToServer(ServerInfo server) {
		tlisten = new ThreadListener();
		if (connectionThread == null || !connectionThread.isAlive()) {
			telnet = new TelnetConnectionThread(this, server, sendData, tlisten);
			connectionThread = new Thread(telnet);
			connectionThread.start();
		}
	}

	private class ThreadListener implements
			TelnetConnectionThread.TelnetThreadListener {
		public Handler TCUpdateHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case TelnetConnectionThread.TEXT_UPDATE:
					terminal.putString(msg.getData().getString("text"));
					break;
				}
				super.handleMessage(msg);
			}
		};

		public void dataReady(Message m) {
			TCUpdateHandler.sendMessage(m);
		}
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_CONNECT_SWITCH:
			if (connectionThread != null && this.connectionThread.isAlive()) {
				connectionThread.interrupt();
			} else {
				connectToServer(server);
			}
			return true;
		case MENU_ALIAS:
			Intent i = new Intent(this, AliasListActivity.class);
			this.startActivity(i);
			return true;
		case MENU_CLEAR_LOG:
			File log = new File(logPath);
			if (log.exists()) {
				log.delete();
			}
			return true;
		case MENU_LOG_SWITCH:
			if (isLogging) {
				terminal.requestLog(null, false);
				isLogging = false;
			} else {
				terminal.requestLog(logPath, true);
				isLogging = true;
			}
			return true;
		case MENU_TRIGGER_SWITCH:
			TriggerEngine.useTrigger = !TriggerEngine.useTrigger;
			return true;
		case MENU_TRIGGER_SETUP:
			Intent tr = new Intent(this, TriggerListActivity.class);
			startActivity(tr);
			return true;
		case MENU_SCAN_URL:
			scanForURL();
			return true;
		}
		return false;
	}

	private void scanForURL() {
		List<String> urls = terminal.scanForURLs();
		if (urls.size() != 0) {
			Dialog urlDialog = new Dialog(this);
			urlDialog.setTitle(R.string.urlscan);
			ListView urlListView = new ListView(this);
			URLItemListener urlListener = new URLItemListener(this);
			urlListView.setOnItemClickListener(urlListener);
			urlListView.setAdapter(new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, urls));
			urlDialog.setContentView(urlListView);
			urlDialog.show();
		} else {
			Toast.makeText(this, R.string.urlscanerr, Toast.LENGTH_SHORT)
					.show();
		}
	}

	private class CommandListener implements OnKeyListener {
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_DOWN: {
					EditText cmd = (EditText) findViewById(R.id.cmdText);
					if (historypos > 0) {
						historypos--;
					}
					if (historypos == 0) {
						cmd.setText("");
					} else {
						cmd.setText(commandHistory.get(commandHistory.size()
								- historypos));
					}
					return true;
				}
				case KeyEvent.KEYCODE_DPAD_UP: {
					EditText cmd = (EditText) findViewById(R.id.cmdText);
					if (historypos < commandHistory.size()) {
						historypos++;
					}
					if (historypos == 0) {
						cmd.setText("");
					} else {
						cmd.setText(commandHistory.get(commandHistory.size()
								- historypos));
					}
					return true;
				}
				case KeyEvent.KEYCODE_BACK: {
					if (keyCode == KeyEvent.KEYCODE_BACK
							&& connectionThread != null
							&& connectionThread.isAlive()) {
						// Ask the user if they want to quit
						new AlertDialog.Builder(MudTerminalActivity.this)
								.setIcon(R.drawable.smallicon).setTitle(
										R.string.app_name).setMessage(
										R.string.really_quit)
								.setPositiveButton(R.string.yes,
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												connectionThread.interrupt();
												MudTerminalActivity.this
														.finish();
											}
										}).setNegativeButton(R.string.cancel,
										null).show();
						return true;
					} else {
						return false;
					}

				}
				case KeyEvent.KEYCODE_DPAD_CENTER:
				case KeyEvent.KEYCODE_ENTER:
					sendCommand();
					return true;
				}
			}
			return false;
		}
	}

	private void sendCommand() {
		sendCommand(cmd.getText().toString());
		cmd.setText("");
	}

	public void sendCommand(String cmd) {
		sendData.enqueue(cmd);
		terminal.putString(cmd + "\r\n");
		historypos = 0;
		int cmdHisSize = commandHistory.size();
		if (cmdHisSize > 1) {
			if (!(cmd.toString().compareTo(commandHistory.get(cmdHisSize - 1)) == 0)) {
				commandHistory.add(cmd.toString());
				if (commandHistory.size() > HISTORY_BUFFER_SIZE) {
					commandHistory.remove(0);
				}
			}
		} else {
			commandHistory.add(cmd.toString());
		}
		terminal.buffer.setWindowBase(terminal.buffer.screenBase);
	}

	private class URLItemListener implements OnItemClickListener {
		private WeakReference<Context> contextRef;

		URLItemListener(Context context) {
			this.contextRef = new WeakReference<Context>(context);
		}

		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long id) {
			Context context = contextRef.get();

			if (context == null)
				return;

			try {
				TextView urlView = (TextView) view;

				String url = urlView.getText().toString();
				if (url.indexOf("://") < 0)
					url = "http://" + url;

				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				context.startActivity(intent);
			} catch (Exception e) {
				Log.e(TAG, "couldn't open URL", e);
			}
		}

	}
}
