package kpi.com.gproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationReceiver = new LocationReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver,
                new IntentFilter("location"));
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
    }

    private LocationReceiver locationReceiver;

    public void start(View view) {
        Intent i = new Intent(MainActivity.this, GPSMock.class);
        startService(i);

        setContentView(R.layout.activity_main);
        final TextView status = (TextView) findViewById(R.id.status);
        status.setText("Stared");
    }

    public void stop(View view) {
        Intent i = new Intent(this, GPSMock.class);
        stopService(i);

        setContentView(R.layout.activity_main);
        final TextView status = (TextView) findViewById(R.id.status);
        status.setText("Stopped");
    }

    public void updateLocation(Update l) {
        setContentView(R.layout.activity_main);
        final TextView status = (TextView) findViewById(R.id.locationText);
        status.setText(String.format("Location: %s", l));
    }

    class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Update location = (Update) intent.getSerializableExtra("location");
            Log.d("LocationReceiver", "Get update");
            updateLocation(location);
        }
    }
}
