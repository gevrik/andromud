package basement.lab.mudclient.command.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import android.util.Log;
import basement.lab.mudclient.bean.ScriptEngine;

public class CommandIntepreter {

	private final static String TAG = "mudclient.CommandIntepreter";

	private static void debug(String notice) {
		Log.d(TAG, notice);
	}

	public static void sendComplexCommand(String encoding, String cmd,
			OutputStream os) throws IOException {
		HashMap<String, String> aliases = ScriptEngine.aliases;
		String[] cmdGroup = cmd.split(";");
		int cmdCount = cmdGroup.length;
		for (int i = 0; i < cmdCount; i++) {
			debug(cmdGroup[i]);
			String aliasCmd = aliases.get(cmdGroup[i]);
			if (aliasCmd != null) {
				String[] aCmds = aliasCmd.split(";");
				int aliasCmdCount = aCmds.length;
				for (int j = 0; j < aliasCmdCount; j++) {
					sendSimpleCommand(encoding, aCmds[j], os);
				}
			} else {
				sendSimpleCommand(encoding, cmdGroup[i], os);
			}
		}
	}

	private static void sendSimpleCommand(String encoding, String cmd,
			OutputStream os) throws IOException {
		if (cmd == null) {
			return;
		} else if (!cmd.startsWith("#")) {
			os.write((cmd + "\r\n").getBytes(encoding));
			os.flush();
		} else if (cmd.startsWith("#wa")) {
			try {
				int time = Integer.parseInt(cmd.substring(3).trim());
				Thread.sleep(time);
			} catch (NumberFormatException e) {
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		} else {
			int firstSpace = cmd.indexOf(' ');
			if (firstSpace == -1)
				return;
			else {
				try {
					int repeatTime = Integer.parseInt(cmd.substring(1,
							firstSpace));
					String cmdBody = cmd.substring(firstSpace).trim();
					for (int i = 0; i < repeatTime; i++) {
						sendSimpleCommand(encoding, cmdBody, os);
					}
				} catch (NumberFormatException e) {
					return;
				}
			}
		}
	}
}