package kpi.com.gproxy;

import android.location.Location;

import java.io.Serializable;

/**
 * Created by kpi on 7/18/16.
 */

public class Update implements Serializable {
    public double lat, lng, alt;
    public float acc, speed, bearing;

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
}
