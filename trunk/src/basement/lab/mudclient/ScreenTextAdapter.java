package basement.lab.mudclient;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ScreenTextAdapter extends BaseAdapter {

	private ArrayList<String> text;
	private Context ctx;

	public ScreenTextAdapter(Context ctx, ArrayList<String> text) {
		this.text = text;
		this.ctx = ctx;
	}

	public int getCount() {
		return text.size();
	}

	public Object getItem(int position) {
		return text.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View trigger = inflater.inflate(R.layout.screen_text_row, null);
		TextView name;
		name = (TextView) trigger.findViewById(R.id.screen_text);
		name.setText(text.get(position));
		return trigger;
	}

}
