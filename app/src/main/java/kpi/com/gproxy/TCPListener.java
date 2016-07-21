package kpi.com.gproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Created by kpi on 7/19/16.
 * tcp listener
 */
class TCPListener extends Thread {

    public static final String HOST = "192.168.88.33";
    public static final int PORT = 16888;
    private GPSMock gpsMock;
    public boolean active;
    public static final String TAG = "TCPListener";

    public Update currLocation, prevLocation;

    public TCPListener(GPSMock gpsMock) {
        this.gpsMock = gpsMock;
    }

    private SocketChannel sock;

    private void reconnect() {
        sockClose();
        while (active) {
            try {
                sock = SocketChannel.open(new InetSocketAddress(HOST, PORT));
                sock.configureBlocking(false);
                return;
            } catch (IOException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    active = false;
                }
            }
        }
    }

    private void sockClose() {
        if (sock != null && sock.isConnected()) {
            try {
                sock.close();
            } catch (IOException e) {
                Log.i(TAG, "Socket close");
            }
        }
    }

    private void sendMessage(String msg) {
        try {
            sock.write(ByteBuffer.wrap(msg.getBytes()));
        } catch (IOException e) {
            reconnect();
        }
    }

    private void sendMessage(Object obj) {
        Gson g = new Gson();
        sendMessage(g.toJson(obj));
    }



    class LocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Update location = (Update) intent.getSerializableExtra("location");
            currLocation = location;
        }
    }

    public void locationUpdate(Update location) {
        sendMessage(new RpcMessage("location_update", location));
    }

    LocationReceiver locationReceiver;

    @Override
    public void run() {
        active = true;
        locationReceiver = new LocationReceiver();
        LocalBroadcastManager.getInstance(gpsMock)
                .registerReceiver(locationReceiver, new IntentFilter("location"));

        reconnect();
        while (active && !this.isInterrupted()) {
            if (currLocation != null && currLocation != prevLocation){
                locationUpdate(currLocation);
                prevLocation = currLocation;
            }

            try {
                if (!sock.isConnected()){
                    reconnect();
                }
                ByteBuffer buff = ByteBuffer.allocate(2048);
                int bytesRead = sock.read(buff);
                buff.flip();
                String s = new String(StandardCharsets.UTF_8.decode(buff).array(),
                        0, buff.position());

                if (bytesRead > 0) {
                    Gson g = new Gson();
                    Log.i(TAG, "Read bytes " + bytesRead);
                    Log.i(TAG, String.format("Bytes: %s", s));
                    RpcMessage msg = g.fromJson(s, RpcMessage.class);
                    Log.i(TAG, "Got message: " + msg.type);
                }
            } catch (IOException e) {
                reconnect();
                e.printStackTrace();
            }


            Update up = new Update(GPSMock.lat - 0.0001, GPSMock.lng + 0.0001, 213.0, 5.0f, 2.0f, 0.0f);
            Message msg = Message.obtain(gpsMock.updateHandler, 0, up);
            Log.d(TAG, "send to target");
            msg.sendToTarget();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                active = false;
                sockClose();
            }
        }

        sockClose();
        LocalBroadcastManager.getInstance(gpsMock).unregisterReceiver(locationReceiver);
    }
}
