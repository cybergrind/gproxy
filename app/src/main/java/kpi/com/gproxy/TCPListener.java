package kpi.com.gproxy;

import android.os.Message;
import android.util.Log;

/**
 * Created by kpi on 7/19/16.
 * tcp listener
 */
class TCPListener extends Thread {

    private GPSMock gpsMock;
    public boolean active;
    public static final String TAG = "TCPListener";

    public TCPListener(GPSMock gpsMock) {
        this.gpsMock = gpsMock;
    }

    @Override
    public void run() {
        active = true;
        while (active && !this.isInterrupted()) {
            Update up = new Update(GPSMock.lat - 0.0001, GPSMock.lng + 0.0001, 213.0, 5.0f, 2.0f, 0.0f);
            Message msg = Message.obtain(gpsMock.updateHandler, 0, up);
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
