package basement.lab.mudclient.utils;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import basement.lab.mudclient.R;
import basement.lab.mudclient.SettingsManager;
import basement.lab.mudclient.bean.ServerInfo;

public class ServerInfoAdapter extends BaseAdapter {

	ArrayList<ServerInfo> servers;
	Activity ctx;

	public ServerInfoAdapter(Activity ctx) {
		servers = SettingsManager.getServerList(ctx);
		this.ctx = ctx;
	}

	public int getCount() {
		return servers.size();
	}

	@Override
	public void notifyDataSetChanged() {
		servers = SettingsManager.getServerList(ctx);
		super.notifyDataSetChanged();
	}

	public Object getItem(int position) {
		return servers.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View server = inflater.inflate(R.layout.serverrow, null);
		TextView nickname, host;
		nickname = (TextView) server.findViewById(R.id.nickname);
		host = (TextView) server.findViewById(R.id.host);
		nickname.setText(servers.get(position).nickName);
		host.setText(servers.get(position).IP + ":"
				+ servers.get(position).Port);
		return server;
	}
}
