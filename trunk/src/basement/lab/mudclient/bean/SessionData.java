package basement.lab.mudclient.bean;

import java.util.List;

import basement.lab.mudclient.SendQueue;
import basement.lab.mudclient.utils.TerminalView;

public class SessionData {
	public Thread connection;
	public SendQueue queue;
	public List<String> commandHistory;
	public ServerInfo server;
	public TerminalView terminal;
	public ScriptEngine aliases;
}
