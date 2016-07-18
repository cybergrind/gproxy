package kpi.com.gproxy;

/**
 * Created by kpi on 7/18/16.
 */

public class Update {
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

    @Override
    public String toString() {
        return String.format("LLA: %f %f %f => %f %f", lat, lng, alt, acc, speed);
    }
}
