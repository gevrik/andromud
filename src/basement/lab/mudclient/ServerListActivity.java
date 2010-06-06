package basement.lab.mudclient;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemLongClickListener;
import basement.lab.mudclient.bean.ServerInfo;
import basement.lab.mudclient.utils.ServerInfoAdapter;

public class ServerListActivity extends ListActivity implements
		android.view.View.OnClickListener, OnItemLongClickListener {

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case REQUEST_EDIT_SERVER:
			if (resultCode != RESULT_CANCELED) {
				adapter.notifyDataSetChanged();
			}
			break;
		}
	}

	public final static String EDIT_SERVER_POSITION = "edit_server_position";
	public final static int REQUEST_EDIT_SERVER = 1;
	private int contextSelect = 0;
	private Button newServer, cancel;
	private AlertDialog alert;
	private ServerInfoAdapter adapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.serverlist);
		setTitle(R.string.serverlist);
		newServer = (Button) findViewById(R.id.newServer);
		newServer.setOnClickListener(this);
		cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(this);
		adapter = new ServerInfoAdapter(this);
		this.setListAdapter(adapter);
		getListView().setOnItemLongClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		adapter = new ServerInfoAdapter(this);
		this.setListAdapter(adapter);
		getListView().setOnItemLongClickListener(this);
		if (adapter.getCount() == 0) {
			new AlertDialog.Builder(this).setIcon(R.drawable.smallicon)
					.setTitle(R.string.app_name).setMessage(
							R.string.noserverfound).setPositiveButton(
							R.string.ok, new OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									createDefaultServerList();
									adapter.notifyDataSetChanged();
								}
							}).setNegativeButton(R.string.no, null).create()
					.show();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent i = new Intent(this, MudTerminalActivity.class);
		i.putExtra(MudTerminalActivity.Server, SettingsManager.getServerList(
				this).get(position));
		startActivity(i);
		this.finish();
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.newServer:
			Intent i = new Intent(ServerListActivity.this,
					ConnectionEditor.class);
			startActivityForResult(i, REQUEST_EDIT_SERVER);
			break;
		case R.id.cancel:
			finish();
			break;
		}
	}

	public boolean onItemLongClick(AdapterView<?> arg0, View view,
			int position, long id) {
		contextSelect = position;
		alert = new AlertDialog.Builder(this).setIcon(R.drawable.smallicon)
				.setTitle(R.string.editserver).setPositiveButton(
						R.string.modify, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								Intent i = new Intent(ServerListActivity.this,
										ConnectionEditor.class);
								i.putExtra(EDIT_SERVER_POSITION, contextSelect);
								startActivityForResult(i, REQUEST_EDIT_SERVER);
							}
						}).setNegativeButton(R.string.delete,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								ArrayList<ServerInfo> list = SettingsManager
										.getServerList(ServerListActivity.this);
								list.remove(contextSelect);
								SettingsManager.setServerList(
										ServerListActivity.this, list);
								adapter.notifyDataSetChanged();
							}
						}).create();
		alert.show();
		return true;
	}

	private void createDefaultServerList() {
		ArrayList<ServerInfo> list = new ArrayList<ServerInfo>();
		list.add(new ServerInfo("pkuxkx_GBK", "pkuxkx.net", 8080, null, ""));
		list.add(new ServerInfo("Aardwolf_UTF8", "aardwolf.org", 23, null, ""));
		list.add(new ServerInfo("Achaea_UTF8", "achaea.com", 23, null, ""));
		SettingsManager.setServerList(this, list);
	}
}
