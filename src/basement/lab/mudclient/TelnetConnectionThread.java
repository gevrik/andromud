package basement.lab.mudclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import basement.lab.mudclient.bean.ServerInfo;
import basement.lab.mudclient.command.utils.CommandIntepreter;

public class TelnetConnectionThread implements Runnable {

	public final static String EOL = "\r\n";
	public final static int BUFFER_SIZE = 4096;
	// MESSAGE Codes
	protected static final int TEXT_UPDATE = 0x0001;

	// Telnet command codes
	protected static final int TELNET_IAC = 255; // Interpret as command escape
	// sequence
	// Prefix to all telnet commands
	protected static final int TELNET_DONT = 254; // You are not to use this
	// option
	protected static final int TELNET_DO = 253; // Please, you use this option
	protected static final int TELNET_WONT = 252; // I won't use option
	protected static final int TELNET_WILL = 251; // I will use option
	protected static final int TELNET_SB = 250; // Subnegotiate
	protected static final int TELNET_GA = 249; // Go ahead
	protected static final int TELNET_EL = 248; // Erase line
	protected static final int TELNET_EC = 247; // Erase character
	protected static final int TELNET_AYT = 246; // Are you there
	protected static final int TELNET_AO = 245; // Abort output
	protected static final int TELNET_IP = 244; // Interrupt process
	protected static final int TELNET_BRK = 243; // Break
	protected static final int TELNET_DM = 242; // Data mark
	protected static final int TELNET_NOP = 241; // No operation.
	protected static final int TELNET_SE = 240; // End of subnegotiation
	protected static final int TELNET_EOR = 239; // End of record
	protected static final int TELNET_ABORT = 238; // About process
	protected static final int TELNET_SUSP = 237; // Suspend process
	protected static final int TELNET_xEOF = 236; // End of file: EOF already
	// used

	// Telnet options
	protected static final int TELNET_OPTION_BIN = 0; // Binary transmission
	protected static final int TELNET_OPTION_ECHO = 1; // Echo
	protected static final int TELNET_OPTION_RECN = 2; // Reconnection
	protected static final int TELNET_OPTION_SUPP = 3; // Suppress go ahead
	protected static final int TELNET_OPTION_APRX = 4; // Approx message size
	// negotiation
	protected static final int TELNET_OPTION_STAT = 5; // Status
	protected static final int TELNET_OPTION_TIM = 6; // Timing mark
	protected static final int TELNET_OPTION_REM = 7; // Remote controlled
	// trans/echo
	protected static final int TELNET_OPTION_OLW = 8; // Output line width
	protected static final int TELNET_OPTION_OPS = 9; // Output page size
	protected static final int TELNET_OPTION_OCRD = 10; // Out carriage-return
	// disposition
	protected static final int TELNET_OPTION_OHT = 11; // Output horizontal
	// tabstops
	protected static final int TELNET_OPTION_OHTD = 12; // Out horizontal tab
	// disposition
	protected static final int TELNET_OPTION_OFD = 13; // Output formfeed
	// disposition
	protected static final int TELNET_OPTION_OVT = 14; // Output vertical
	// tabstops
	protected static final int TELNET_OPTION_OVTD = 15; // Output vertical tab
	// disposition
	protected static final int TELNET_OPTION_OLD = 16; // Output linefeed
	// disposition
	protected static final int TELNET_OPTION_EXT = 17; // Extended ascii
	// character set
	protected static final int TELNET_OPTION_LOGO = 18; // Logout
	protected static final int TELNET_OPTION_BYTE = 19; // Byte macro
	protected static final int TELNET_OPTION_DATA = 20; // Data entry terminal
	protected static final int TELNET_OPTION_SUP = 21; // supdup protocol
	protected static final int TELNET_OPTION_SUPO = 22; // supdup output
	protected static final int TELNET_OPTION_SNDL = 23; // Send location
	protected static final int TELNET_OPTION_TERM = 24; // Terminal type
	protected static final int TELNET_OPTION_EOR = 25; // End of record
	protected static final int TELNET_OPTION_TACACS = 26; // Tacacs user
	// identification
	protected static final int TELNET_OPTION_OM = 27; // Output marking
	protected static final int TELNET_OPTION_TLN = 28; // Terminal location
	// number
	protected static final int TELNET_OPTION_3270 = 29; // Telnet 3270 regime
	protected static final int TELNET_OPTION_X3 = 30; // X.3 PAD
	protected static final int TELNET_OPTION_NAWS = 31; // Negotiate about
	// window size
	protected static final int TELNET_OPTION_TS = 32; // Terminal speed
	protected static final int TELNET_OPTION_RFC = 33; // Remote flow control
	protected static final int TELNET_OPTION_LINE = 34; // Linemode
	protected static final int TELNET_OPTION_XDL = 35; // X display location
	protected static final int TELNET_OPTION_ENVIR = 36; // Telnet environment
	// option
	protected static final int TELNET_OPTION_AUTH = 37; // Telnet authentication
	// option
	protected static final int TELNET_OPTION_NENVIR = 39; // Telnet environment
	// option
	protected static final int TELNET_OPTION_EXTOP = 255; // Extended-options-list

	private Socket skt;
	private InputStream inStream;
	private OutputStream outStream;
	private SendQueue sendData;

	private String LeftOvers;

	private ServerInfo server;
	private CharsetDecoder decoder;
	private Charset charset;
	private String encodingDisplayName;
	final private WifiLock mWifiLock;
	private boolean mLockingWifi;
	private boolean usePostLogin = false;

	private static final String TAG = "mudclient.telnet.handler";

	public TelnetConnectionThread(Context ctx, ServerInfo server, SendQueue ss,
			TelnetThreadListener tl) {
		this.server = server;
		this.DataListener = tl;
		this.sendData = ss;
		LeftOvers = "";
		usePostLogin = SettingsManager.isUsePostLogin(ctx);
		final WifiManager wm = (WifiManager) ctx
				.getSystemService(Context.WIFI_SERVICE);
		mWifiLock = wm.createWifiLock(TAG);
		mLockingWifi = SettingsManager.isKeepWifi(ctx);

		charset = Charset.forName(SettingsManager.getEncoding(ctx));
		encodingDisplayName = charset.displayName();
		decoder = charset.newDecoder();
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
	}

	public void releaseWifiLock() {
		if (mWifiLock.isHeld()) {
			mWifiLock.release();
			Log.d(TAG, "wifi lock released.");
		}
	}

	private void acquireWifiLockIfNecessary() {
		synchronized (this) {
			if (mLockingWifi && !mWifiLock.isHeld()) {
				mWifiLock.acquire();
				Log.d(TAG, "wifi lock acquired.");
			}
		}
	}

	public synchronized void run() {

		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		CoderResult result;
		ByteBuffer byteBuffer;
		CharBuffer charBuffer;
		byte[] byteArray;
		char[] charArray;

		try {
			sendMessageText("Establishing Connection to: " + server.IP + ":"
					+ server.Port + EOL);
			skt = new Socket(server.IP, server.Port);
			skt.setSoTimeout(50);
			inStream = skt.getInputStream();
			outStream = skt.getOutputStream();
			sendMessageText("Connected: \r\n");
			if (usePostLogin) {
				sendData.enqueue(server.postLogin);
			}
			acquireWifiLockIfNecessary();
			byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
			charBuffer = CharBuffer.allocate(BUFFER_SIZE);
			byteArray = byteBuffer.array();
			charArray = charBuffer.array();
			while (!Thread.currentThread().isInterrupted()) {
				byteBuffer.clear();
				charBuffer.clear();
				int bytesRead = 0;
				byteBuffer.limit(0);
				int bytesToRead;
				bytesToRead = byteBuffer.capacity();
				for (bytesRead = 0; bytesRead < bytesToRead; bytesRead++) {
					int temp = 0;
					try {
						temp = inStream.read();
						if (temp == -1) {
							Thread.currentThread().interrupt();
							break;
						}
					} catch (IOException e) {
						break;
					}
					byteArray[bytesRead] = (byte) temp;
				}

				if (bytesRead == 0) {
					Thread.yield();
				} else {
					byteBuffer.limit(byteBuffer.limit() + bytesRead);
					synchronized (this) {
						result = decoder.decode(byteBuffer, charBuffer, false);
					}
					if (result.isUnderflow()
							&& byteBuffer.limit() == byteBuffer.capacity()) {
						byteBuffer.compact();
						byteBuffer.limit(byteBuffer.position());
						byteBuffer.position(0);
					}
					parseBuffer(charArray, charBuffer.position());
				}

				if (!sendData.isEmpty()) {
					Object stackObject = sendData.dequeue();
					Class<? extends Object> objectClass = stackObject
							.getClass();
					if (objectClass == String.class) {
						String cmd = (String) stackObject;
						CommandIntepreter.sendComplexCommand(
								encodingDisplayName, cmd, outStream);
					} else {
						DataListener = (TelnetThreadListener) stackObject;
					}
				}
			}
			inStream.close();
			outStream.close();
			skt.close();
			sendMessageText("\r\nDISCONNECTED\r\n");
		} catch (UnknownHostException e1) {
			sendMessageText("\r\nUnknown Host!\r\n");
		} catch (IOException e1) {
			sendMessageText("\r\nNetwork Connection Lost!\r\n");
		} catch (IllegalStateException e) {
			sendMessageText("\r\nUnknown Error!\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		releaseWifiLock();
	}

	private void parseBuffer(char[] dataBuffer, int bufferSize) {
		String Formatted = "";
		char[] buffer = new char[bufferSize + LeftOvers.length()];

		for (int x = 0; x < LeftOvers.length(); x++) {
			buffer[x] = (char) LeftOvers.charAt(x);
		}

		for (int x = LeftOvers.length(); x < LeftOvers.length() + bufferSize; x++) {
			buffer[x] = (char) dataBuffer[x - LeftOvers.length()];
		}
		bufferSize = bufferSize + LeftOvers.length();

		for (int x = 0; x < bufferSize; x++) {
			switch (buffer[x]) {
			case TELNET_IAC: {
				if (x + 1 < bufferSize) {
					x++;
					switch (buffer[x]) {
					case TELNET_WILL: {
						if (x + 1 < bufferSize) {
							x++;
							switch (buffer[x]) {
							case TELNET_OPTION_EOR:
								sendData.enqueue((char) TELNET_IAC
										+ (char) TELNET_DONT
										+ (char) TELNET_OPTION_EOR);
								break;
							default:
								break;
							}
						}
						break;
					}
					case TELNET_DONT: {
						if (x + 1 < bufferSize) {
							x++;
							switch (buffer[x]) {
							case TELNET_OPTION_EOR:
								sendData.enqueue("" + (char) TELNET_IAC
										+ (char) TELNET_DONT
										+ (char) TELNET_OPTION_EOR);
								break;
							default:
								break;
							}
						}
						break;
					}
					case TELNET_SB: {
						for (x += 1; x < bufferSize; x++) {
							if (buffer[x] == TELNET_IAC) {
								if (buffer[x] + 1 < bufferSize) {
									x++;
									if (buffer[x] == TELNET_SE) {
										break;
									}
								}
							}
						}
						break;
					}
					default:
						if (x + 1 < bufferSize) {
							x++;
						}
						break;
					}
				}
				break;
			}
			default:
				Formatted += buffer[x];
				break;
			}
		}
		if (Formatted.length() > 0) {
			sendMessageText(Formatted);
			Formatted = "";
		}
	}

	private void sendMessageText(String text) {
		Bundle updateText = new Bundle();
		updateText.putString("text", text);
		Message m = new Message();
		m.what = TEXT_UPDATE;
		m.setData(updateText);
		DataListener.dataReady(m);
	}

	private TelnetThreadListener DataListener;

	public interface TelnetThreadListener {
		public void dataReady(Message m);
	}
}