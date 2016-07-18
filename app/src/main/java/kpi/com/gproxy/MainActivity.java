package kpi.com.gproxy;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void start(View view) {
        Intent i = new Intent(MainActivity.this, GPSMock.class);
        startService(i);

        final TextView status = (TextView) findViewById(R.id.status);
        status.setText("Stared");
    }

    public void stop(View view) {
        Intent i = new Intent(this, GPSMock.class);
        stopService(i);

        final TextView status = (TextView) findViewById(R.id.status);
        status.setText("Stopped");
    }
}
