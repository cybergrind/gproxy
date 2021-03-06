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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Random;


public class GPSMock extends Service {
    public static final String TAG = "GPSMock";
    public Handler updateHandler;

    private Thread updateThread, listener;



    private final double defaultAlt = 220;

    protected long prevTS;
    public static double lat, lng, alt, nextAlt;
    protected static float acc, bear, speed;

    protected Update target;

    {
        prevTS = System.currentTimeMillis();
        lat = 53.86783;
        lng = 27.65683;
        alt = defaultAlt;
        nextAlt = alt;
        acc = 5.00f;
        bear = 0.0f;
        speed = 0.0f;

        target = new Update(lat, lng, alt, acc, bear, speed);
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
            listener = new TCPListener(this);
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

    class UpdateThread extends Thread {

        public static final String TAG = "UpdateThread";

        @Override
        public void run() {
            Log.d(TAG, "Run update thread");
            updateHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Log.d("UpdateHandler", "Got message update "+msg.obj.toString());
                    target = (Update) msg.obj;
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
                recalculate();

                Location location = new Location(PROVIDER);
                location.setTime(System.currentTimeMillis());
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                location.setLatitude(lat);
                location.setLongitude(lng);
                location.setAltitude(nextAlt);
                location.setSpeed(speed);
                location.setBearing(bear);
                location.setAccuracy(acc);
                manager.setTestProviderLocation(PROVIDER, location);
                bcast(location);

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

    private void bcast(Location l) {
        Intent i = new Intent("location");
        i.putExtra("location", new Update(l));
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }


    private void recalculate() {
        double dist = calculateDistance(lat, lng, target.lat, target.lng);
        if (dist > 1) {
            Long interval = (System.currentTimeMillis() - prevTS);
            bear = bearing(lat, lng, target.lat, target.lng);
            updatePosition(bear, interval);
        } else {
            acc = (float) ( (13.0 - 1.3) * rand.nextFloat() + 1.3);
            speed = (float) (0.2 * rand.nextFloat());
            prevTS = System.currentTimeMillis();
        }
    }

    private Random rand;
    {
        rand = new Random();
    }

    private void updatePosition(float bearing, long interval) {
        float currSpeed = (float) ((1.8 - 0.9) * rand.nextFloat() + 0.9);
        acc = (float) ( (13.0 - 1.3) * rand.nextFloat() + 1.3);
        float distance = currSpeed*interval/1000;
        Log.d(TAG, "Distance: " + distance);
        float vDistance = distance / 6371000;

        double lat1R = Math.toRadians(lat);
        double lng1R = Math.toRadians(lng);
        double bearR = Math.toRadians(bearing);

        double latR = Math.asin( Math.sin(lat1R)*Math.cos(vDistance) +
                Math.cos(lat1R)*Math.sin(vDistance)*Math.cos(bearR));

        double lngR = lng1R + Math.atan2(Math.sin(bearR)*Math.sin(vDistance)*Math.cos(lat1R),
                Math.cos(vDistance)-Math.sin(lat1R)*Math.sin(latR));

        lat = Math.toDegrees(latR);
        lng = Math.toDegrees(lngR);
        speed = currSpeed;
        nextAlt = (rand.nextFloat() > 0.5 ? -1 : 1) * 10*rand.nextFloat() + alt;
        prevTS = System.currentTimeMillis();
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
        double y = Math.sin(longDiff)*Math.cos(latitude2);
        double x = Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        return (float) (Math.toDegrees(Math.atan2(y, x))+360)%360;
    }
}
