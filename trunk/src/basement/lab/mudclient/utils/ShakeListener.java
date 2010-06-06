package basement.lab.mudclient.utils;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class ShakeListener implements SensorEventListener {
	private SensorManager sensorManager;
	private List<Sensor> sensors;
	private Sensor sensor;
	private long lastUpdate = -1;
	private long lastForce = -1;
	private long lastShake = -1;
	private long currentTime = -1;
	private float last_x, last_y, last_z;
	private float current_x, current_y, current_z, currenForce;

	private final int DATA_X = SensorManager.DATA_X;
	private final int DATA_Y = SensorManager.DATA_Y;
	private final int DATA_Z = SensorManager.DATA_Z;

	private int mShakeCount = 0;
	private static final int FORCE_THRESHOLD = 900;
	private final static int SHAKE_TIMEOUT = 1000;
	private final static long SHAKE_DURATION = 1000;
	private final static int SHAKE_COUNT = 3;

	public ShakeListener(Activity parent) {
		this.sensorManager = (SensorManager) parent
				.getSystemService(Context.SENSOR_SERVICE);
		this.sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			sensor = sensors.get(0);
		}
	}

	public void start() {
		if (sensor != null) {
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_GAME);
		}
	}

	public void stop() {
		sensorManager.unregisterListener(this);
	}

	public void onAccuracyChanged(Sensor s, int valu) {
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER
				|| event.values.length < 3)
			return;

		currentTime = System.currentTimeMillis();
		if (currentTime - lastForce > SHAKE_TIMEOUT) {
			mShakeCount = 0;
		}
		if ((currentTime - lastUpdate) > 100) {
			long diffTime = (currentTime - lastUpdate);
			current_x = event.values[DATA_X];
			current_y = event.values[DATA_Y];
			current_z = event.values[DATA_Z];
			currenForce = Math.abs(current_x + current_y + current_z - last_x
					- last_y - last_z)
					/ diffTime * 10000;
			if (currenForce > FORCE_THRESHOLD) {
				if ((++mShakeCount >= SHAKE_COUNT)
						&& (currentTime - lastShake > SHAKE_DURATION)) {
					lastShake = currentTime;
					mShakeCount = 0;
					onShake();
				}
				lastForce = currentTime;
			}
			lastUpdate = currentTime;
			last_x = current_x;
			last_y = current_y;
			last_z = current_z;
		}
	}

	public abstract void onShake();
}