package kpi.com.gproxy;

import android.location.Location;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by kpi on 7/18/16.
 */

public class Update implements Serializable {
    @SerializedName("lat")
    public double lat;
    @SerializedName("lng")
    public double lng;
    @SerializedName("alt")
    public double alt;
    @SerializedName("acc")
    public float acc;
    @SerializedName("speed")
    public float speed;
    @SerializedName("bearing")
    public float bearing;

    public Update(double lat, double lng, double alt, float acc, float speed, float bearing) {
        this.lat = lat;
        this.lng = lng;
        this.alt = alt;
        this.acc = acc;
        this.speed = speed;
        this.bearing = bearing;
    }

    public Update(Location l) {
        this.lat = l.getLatitude();
        this.lng = l.getLongitude();
        this.alt = l.getAltitude();
        this.acc = l.getAccuracy();
        this.speed = l.getSpeed();
        this.bearing = l.getBearing();
    }

    @Override
    public String toString() {
        return String.format("LLA: %f %f %f => %f %f", lat, lng, alt, acc, speed);
    }

    public HashMap<String, Object> stringify() {
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("lat", lat);
        hm.put("lng", lng);
        hm.put("alt", alt);
        hm.put("acc", acc);
        hm.put("speed", speed);
        hm.put("bearing", bearing);
        return hm;
    }
}
