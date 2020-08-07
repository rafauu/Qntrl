package com.example.qntrl;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
        implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private final float[] newSensorReading = new float[3];
    private SensorManager sensorManager;
    private double azimuthWithoutFilter;
    private double azimuthSmallFilter;
    private double azimuthBigFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequest();
            }
        });
    }

    private void sendRequest()
    {
        Thread serverRequestThread = new Thread() {
            @Override
            public void run() {
                final String serverUrl = "http://10.0.0.8:80/";
                final TextView sth = findViewById(R.id.otherText);

                final JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("angle", azimuthSmallFilter);
//                    jsonObject.put("angle", azimuth);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                        Request.Method.POST,
                        serverUrl,
                        jsonObject,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(final JSONObject response) {
                                sth.setText(response.toString());
                                findViewById(R.id.devicesButton).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        PopupMenu menu = new PopupMenu(getApplicationContext(), view);
                                        if (response.has("id")) {
                                            try {
                                                JSONArray devices = response.getJSONArray("id");
                                                for(int i=0; i<devices.length(); ++i) {
                                                    menu.getMenu().add(Menu.NONE, i+1, i+1, devices.get(i).toString());
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        menu.show();
                                        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                            @Override
                                            public boolean onMenuItemClick(MenuItem menuItem) {
                                                if (menuItem.toString().equals("device2")) {
                                                    String url = "http://www.example.com";
                                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                                    i.setData(Uri.parse(url));
                                                    startActivity(i);
                                                }
                                                else if (menuItem.toString().equals("device3")) {
                                                    String url = "http://www.google.com";
                                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                                    i.setData(Uri.parse(url));
                                                    startActivity(i);
                                                }
                                                return true;
                                            }
                                        });
                                    }
                                });
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

                Volley.newRequestQueue(MainActivity.this).add(jsonObjectRequest);
            }
        };
        serverRequestThread.start();
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();

        Sensor newSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (newSensor != null) {
            sensorManager.registerListener(this, newSensor,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(event.values, 0, newSensorReading,
                    0, newSensorReading.length);
        } else {
            return;
        }

        float[] rotationV = new float[16];
        SensorManager.getRotationMatrixFromVector(rotationV, newSensorReading);

        float[] orientationValuesV = new float[3];
        SensorManager.getOrientation(rotationV, orientationValuesV);

        final float aSmall = 0.3f;
        final float aVerySmall = 0.1f;

        double yaw = orientationValuesV[0] * 180 / Math.PI;

        azimuthWithoutFilter = yaw;
        azimuthSmallFilter = aSmall * (yaw) + (1 - aSmall) * azimuthSmallFilter;
        azimuthBigFilter = aVerySmall * (yaw) + (1 - aVerySmall) * azimuthBigFilter;

        Log.d(TAG, String.valueOf(azimuthWithoutFilter) + ";" + String.valueOf(azimuthSmallFilter) + ";" + String.valueOf(azimuthBigFilter));

        TextView t = findViewById(R.id.Magneto22);
        t.setText(String.valueOf(String.valueOf(azimuthWithoutFilter) + '\n' + String.valueOf(azimuthSmallFilter) + '\n' + String.valueOf(azimuthBigFilter)));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}