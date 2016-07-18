package kpi.com.gproxy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class GPSMock extends Service {
    public static final String TAG = "GPSMock";
    public Handler updateHandler;

    private Thread updateThread, listener;



    protected double lat, lng, alt;
    protected float acc;

    {
        acc = 5.00f;
        lat = 53.86783;
        lng = 27.65683;
        alt = 213.0;
    }

    public GPSMock() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    GPSMocker mockThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mockThread==null) {
            Log.i(TAG, "Start threads");

            updateThread = new UpdateThread();
            updateThread.run();

            Log.d(TAG, "Start mocker");
            mockThread = new GPSMocker();
            mockThread.start();

            Log.d(TAG, "Start listener");
            listener = new TCPListener();
            listener.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (mockThread != null) {
            mockThread.interrupt();
            updateThread.interrupt();
            listener.interrupt();
        }
    }

    class TCPListener extends Thread {

        public boolean active;
        public static final String TAG = "TCPListener";

        @Override
        public void run() {
            active = true;
            while (active && !this.isInterrupted()) {
                Update up = new Update(53.86783, lng + 0.0001, 213.0, 5.0f, 2.0f, 0.0f);
                Message msg = Message.obtain(updateHandler, 0, up);
                Log.d(TAG, "send to target");
                msg.sendToTarget();

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    active = false;
                    e.printStackTrace();
                }
            }
        }
    }

    class UpdateThread extends Thread {

        public static final String TAG = "UpdateThread";

        @Override
        public void run() {
            Log.d(TAG, "Run update thread");
            updateHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Log.d("UpdateHandler", "Got message update "+msg.obj.toString());
                    Update up = (Update) msg.obj;
                    lng = up.lng;
                    super.handleMessage(msg);
                }
            };
        }
    }

    class GPSMocker extends Thread {
        public static final String PROVIDER = "gps";
        public static final String TAG = "GPSMocker";
        public boolean active;

        @Override
        public void run() {
            active = true;

            Log.d(TAG, "Start mocking");
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            manager.addTestProvider(PROVIDER, false, false, false, false, false, true, true, 1, 1);
            manager.setTestProviderEnabled(PROVIDER, true);

            while (active && !this.isInterrupted()) {
                Location location = new Location(PROVIDER);
                location.setTime(System.currentTimeMillis());
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                location.setLatitude(lat);
                location.setLongitude(lng);
                location.setSpeed(2.0f);
                location.setBearing(2.0f);
                location.setAccuracy(acc);
                manager.setTestProviderLocation(PROVIDER, location);
                Log.d(TAG, "Update test location "+this.isInterrupted());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    active = false;
                    e.printStackTrace();
                }
            }


            manager.setTestProviderEnabled(PROVIDER, false);
            manager.removeTestProvider(PROVIDER);
        }


    }
}
