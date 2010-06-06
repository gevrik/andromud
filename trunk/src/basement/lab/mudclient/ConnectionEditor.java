package basement.lab.mudclient;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.openintent.FileManagerIntents;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import basement.lab.mudclient.bean.ServerInfo;

public class ConnectionEditor extends Activity implements
		android.view.View.OnClickListener {

	// Constants for AndExplorer's file picking intent
	private static final String ANDEXPLORER_TITLE = "explorer_title";
	private static final String MIME_TYPE_ANDEXPLORER_FILE = "vnd.android.cursor.dir/lysesoft.andexplorer.file";
	private static final String TAG = "mudclient.connectiondialog";
	public static final int REQUEST_CODE_PICK_FILE = 36497263;

	private int position = 0;
	private EditText NameEdit, HostEdit, PortEdit, filepath, postLogin;
	private Button saveButton, browse;
	private ArrayList<ServerInfo> list;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect);
		setTitle(R.string.enterserverinfo);
		saveButton = (Button) findViewById(R.id.SaveButton);
		NameEdit = (EditText) findViewById(R.id.NameValue);
		HostEdit = (EditText) findViewById(R.id.HostValue);
		PortEdit = (EditText) findViewById(R.id.PortValue);
		postLogin = (EditText) findViewById(R.id.postLoginValue);
		if (SettingsManager.isPostLoginAsPassword(this)) {
			postLogin.setInputType(InputType.TYPE_CLASS_TEXT
					| InputType.TYPE_TEXT_VARIATION_PASSWORD);
		}
		browse = (Button) findViewById(R.id.browse);
		browse.setOnClickListener(this);
		filepath = (EditText) findViewById(R.id.filepath);
		saveButton.setOnClickListener(this);
		Intent i = getIntent();
		position = i.getIntExtra(ServerListActivity.EDIT_SERVER_POSITION, -1);
		list = SettingsManager.getServerList(this);
		if (position != -1) {
			NameEdit.setText(list.get(position).nickName);
			HostEdit.setText(list.get(position).IP);
			PortEdit.setText(String.valueOf(list.get(position).Port));
			filepath.setText(list.get(position).mudfile);
			postLogin.setText(list.get(position).postLogin);
			saveButton.setText(R.string.save);
		} else {
			NameEdit.setText("Mud");
			HostEdit.setText("");
			PortEdit.setText("80");
			filepath.setText("");
			postLogin.setText("");
			saveButton.setText(R.string.add);
		}
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.browse:
			Uri sdcard = Uri
					.fromFile(Environment.getExternalStorageDirectory());
			String pickerTitle = this.getString(R.string.mudfile);
			Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
			intent.setData(sdcard);
			intent.putExtra(FileManagerIntents.EXTRA_TITLE, pickerTitle);
			intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, this
					.getString(android.R.string.ok));

			try {
				startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
			} catch (ActivityNotFoundException e) {
				// If OI didn't work, try AndExplorer
				intent = new Intent(Intent.ACTION_PICK);
				intent.setDataAndType(sdcard, MIME_TYPE_ANDEXPLORER_FILE);
				intent.putExtra(ANDEXPLORER_TITLE, pickerTitle);
				try {
					startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
				} catch (ActivityNotFoundException e1) {
					pickFileSimple();
				}
			}
			break;
		case R.id.SaveButton:
			if (position == -1) {
				list.add(new ServerInfo(NameEdit.getText().toString(), HostEdit
						.getText().toString(), Integer.valueOf(PortEdit
						.getText().toString()), filepath.getText().toString(), postLogin.getText()
						.toString()));
			} else {
				list.get(position).nickName = NameEdit.getText().toString();
				list.get(position).IP = HostEdit.getText().toString();
				list.get(position).postLogin = postLogin.getText().toString();
				if (filepath.getText().toString().length() != 0) {
					list.get(position).mudfile = filepath.getText().toString();
				} else {
					list.get(position).mudfile = Environment
							.getExternalStorageDirectory()
							+ "/"
							+ list.get(position).nickName
							+ list.get(position).IP + ".txt";
				}
				try {
					list.get(position).Port = Integer.valueOf(PortEdit
							.getText().toString());
				} catch (NumberFormatException e) {
					Toast.makeText(this, R.string.portmustbenumber,
							Toast.LENGTH_LONG).show();
				}
			}
			SettingsManager.setServerList(this, list);
			this.setResult(ServerListActivity.REQUEST_EDIT_SERVER);
			this.finish();
			break;
		}
	}

	private void pickFileSimple() {
		final File sdcard = Environment.getExternalStorageDirectory();
		Log.d(TAG, sdcard.toString());
		final String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)
				&& !Environment.MEDIA_MOUNTED.equals(state)) {
			new AlertDialog.Builder(this).setMessage(
					R.string.alert_sdcard_absent).setNegativeButton(
					android.R.string.cancel, null).create().show();
			return;
		}

		List<String> names = new LinkedList<String>();
		{
			File[] files = sdcard.listFiles();
			if (files != null) {
				for (File file : sdcard.listFiles()) {
					if (file.isDirectory())
						continue;
					names.add(file.getName());
				}
			}
		}
		Collections.sort(names);

		final String[] namesList = names.toArray(new String[] {});
		Log.d(TAG, names.toString());
		// prompt user to select any file from the sdcard root
		new AlertDialog.Builder(this).setTitle(R.string.mudfile).setItems(
				namesList, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						String name = namesList[arg1];
						setFile(new File(sdcard, name));
					}
				}).setNegativeButton(android.R.string.cancel, null).create()
				.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		switch (requestCode) {
		case REQUEST_CODE_PICK_FILE:
			if (resultCode == RESULT_OK && intent != null) {
				Uri uri = intent.getData();
				try {
					if (uri != null) {
						setFile(new File(URI.create(uri.toString())));
					} else {
						String filename = intent.getDataString();
						if (filename != null)
							setFile(filename);
					}
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "Couldn't read from picked file", e);
				}
			}
			break;
		}
	}

	private void setFile(File mudfile) {
		if (mudfile != null) {
			setFile(mudfile.getAbsolutePath());
		}
	}

	private void setFile(String mudfile) {
		if (mudfile == null || mudfile.length() <= 0) {
			filepath.setText("");
		} else {
			filepath.setText(mudfile);
		}
		filepath.postInvalidate();
	}
}
