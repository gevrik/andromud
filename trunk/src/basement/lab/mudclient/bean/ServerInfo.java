package basement.lab.mudclient.bean;

import java.io.Serializable;

import android.os.Environment;

public class ServerInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -23583398711400931L;
	public String nickName;
	public String IP;
	public String mudfile;
	public int Port;
	public String postLogin;

	public ServerInfo(String name, String ip, int port, String mudfile,
			String postLogin) {
		nickName = name;
		IP = ip;
		Port = port;
		if (mudfile == null || mudfile.length() == 0)
			this.mudfile = generateDefaultMudFilePath();
		else
			this.mudfile = mudfile;
		if (postLogin == null)
			this.postLogin = "";
		else
			this.postLogin = postLogin;
	}

	private String generateDefaultMudFilePath() {
		return Environment.getExternalStorageDirectory() + "/" + nickName + IP
				+ ".txt";
	}
}