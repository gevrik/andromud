package basement.lab.mudclient;

import org.connectbot.util.HelpTopicView;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ViewFlipper;

public class HelpActivity extends Activity {

	public final static String HELPDIR = "help";
	public final static String SUFFIX = ".html";

	protected ViewFlipper flipper = null;
	private Button next, prev;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (SettingsManager.isFullScreen(this)) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		setContentView(R.layout.help);

		this.flipper = (ViewFlipper) findViewById(R.id.wizard_flipper);

		// Add a view for each help topic we want the user to see.
		String[] topics = getResources().getStringArray(
				R.array.list_help_topics);
		for (String topic : topics) {
			flipper.addView(new HelpTopicView(this).setTopic(topic));
		}

		next = (Button) this.findViewById(R.id.action_next);
		next.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isLastDisplayed()) {
					HelpActivity.this.finish();
				} else {
					// show next step and update buttons
					flipper.showNext();
				}
			}
		});

		prev = (Button) this.findViewById(R.id.action_prev);
		prev.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (isFirstDisplayed()) {
					HelpActivity.this.finish();
				} else {
					// show previous step and update buttons
					flipper.showPrevious();
				}
			}
		});
	}

	protected boolean isFirstDisplayed() {
		return (flipper.getDisplayedChild() == 0);
	}

	protected boolean isLastDisplayed() {
		return (flipper.getDisplayedChild() == flipper.getChildCount() - 1);
	}
}
