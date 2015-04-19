package de.mesing.benjamin.orientationsensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class MainActivity extends ActionBarActivity implements SensorEventListener {

    private SensorManager _sensorManager = null;
    private Sensor _gravitySensor = null;
    private final int SENSOR_TYPE = Sensor.TYPE_ACCELEROMETER;

    private Socket _sendSocket;
    PrintWriter _sendWriter;

    private String _remoteAddress = "192.168.1.11";
    private int _remotePort = 35462;

    public final String LOG_TAG = "MainActivity";

    private void startGravitySensor() {
        _sensorManager.registerListener(this, _gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stopGravitySensor() {
        _sensorManager.unregisterListener(this);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        _gravitySensor = _sensorManager.getDefaultSensor(SENSOR_TYPE);
        if (_gravitySensor == null)  {
            Log.e(LOG_TAG, "Sensor of type " + SENSOR_TYPE + " not found");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startGravitySensor();
        connectNetwork();
    }

    private void connectNetwork() {
        AsyncTask task = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                try {
                    _sendSocket = new Socket(_remoteAddress, _remotePort);
                    Log.d(LOG_TAG, "Successfully connected to " + _remoteAddress);
                    OutputStream outstream = _sendSocket.getOutputStream();
                    _sendWriter = new PrintWriter(outstream);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error connecting to " + _remoteAddress + "\n" + e.getLocalizedMessage());
                }
                return null;
            }
        };
        task.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopGravitySensor();
        disconnectNetwork();
    }

    private void disconnectNetwork() {
        if (_sendSocket != null) {
            _sendWriter = null;
            try {
                _sendSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error disconnecting socket");
            }
            _sendSocket = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void sendOrienationData(OrientationData data) {
        if (_sendWriter == null)
            return;
        _sendWriter.print(String.valueOf(data.xAngle) + " " + String.valueOf(data.zAngle));
        _sendWriter.flush();
    }

    static class OrientationData {
        public float xAngle;
        public float zAngle;
        public static OrientationData calculateFromGravitySensorVector(float x, float y, float z) {
            OrientationData result = new OrientationData();
            result.zAngle = x / 9.81f * 90;
            result.xAngle = y / 9.81f * 90;
            return result;
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case SENSOR_TYPE:
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                //Log.d(LOG_TAG, "Event changed: " + " x " + event.values[0] + " y " + event.values[1] + " z " + event.values[2]);
                sendOrienationData(OrientationData.calculateFromGravitySensorVector(x, y, z));
                break;
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
