package kpi.com.gproxy;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class GPSMock extends Service {
    public static final String TAG = "GPSMock";
    public Handler updateHandler;

    private Thread updateThread, listener;



    protected long prevTS;
    protected double lat, lng, alt;
    protected float acc, bear, speed;

    {
        prevTS = System.currentTimeMillis();
        lat = 53.86783;
        lng = 27.65683;
        alt = 213.0;
        acc = 5.00f;
        bear = 0.0f;
        speed = 0.0f;
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
                Update up = new Update(lat-0.0001, lng + 0.0001, 213.0, 5.0f, 2.0f, 0.0f);
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
                    bear = bearing(lat, lng, up.lat, up.lng);
                    speed = calculateSpeed(up);
                    lat = up.lat;
                    lng = up.lng;
                    super.handleMessage(msg);
                }
            };
        }

        float calculateSpeed(Update up) {
            double dist = calculateDistance(lat, lng, up.lat, up.lng);
            double ts = System.currentTimeMillis();
            double speed = dist / (ts - prevTS) * 1000;
            prevTS = System.currentTimeMillis();
            return (float) speed;
        }

        double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lng2 - lng1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1))
                    * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                    * Math.sin(dLon / 2);
            double c = 2 * Math.asin(Math.sqrt(a));
            return 6371000 * c;
        }

        float bearing(double lat1, double lng1, double lat2, double lng2) {
            double latitude1 = Math.toRadians(lat1);
            double latitude2 = Math.toRadians(lat2);
            double longDiff= Math.toRadians(lng2 - lng1);
            double y= Math.sin(longDiff)*Math.cos(latitude2);
            double x=Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

            return (float) (Math.toDegrees(Math.atan2(y, x))+360)%360;
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
                location.setSpeed(speed);
                location.setBearing(bear);
                location.setAccuracy(acc);
                manager.setTestProviderLocation(PROVIDER, location);
                Log.d(TAG, "Update test location "+location.toString());

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
