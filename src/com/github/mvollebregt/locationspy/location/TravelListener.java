package com.github.mvollebregt.locationspy.location;

// This file is part of LocationSpy.
//
// LocationSpy is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LocationSpy is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LocationSpy.  If not, see <http://www.gnu.org/licenses/>.

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import com.github.mvollebregt.locationspy.UserLocationDetector;
import com.github.mvollebregt.locationspy.UserLocationListener;
import com.github.mvollebregt.locationspy.Serviceable;
import com.github.mvollebregt.locationspy.UserLocation;

import java.util.Date;

/**
 * @author Michel Vollebregt
 */
public class TravelListener implements UserLocationDetector, Serviceable, LocationListener {

    private UserLocationListener listener;
    private Location previousLocation;
    private Date dateOfLastLocation;
    private boolean isTravelling = false;
    private LocationManager locationManager;

    public void init(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2 * 60 * 1000, 1000, this);
    }

    public void close(Context context) {
        locationManager.removeUpdates(this);
    }

    public void setLocationListener(UserLocationListener listener) {
        this.listener = listener;
    }

    public UserLocation currentLocation(Context context) {
        return isTravelling ? new UserLocation("Onderweg") : null;
    }

    public void onLocationChanged(Location location) {
        isTravelling = false;
        if (previousLocation != null && dateOfLastLocation != null) {
            float distanceTravelled = location.distanceTo(previousLocation);
            if (distanceTravelled > 2000 && (new Date().getTime() - dateOfLastLocation.getTime()) < 5 * 60 * 1000) {
                isTravelling = true;
            }
        }
        listener.locationChanged(currentLocation(null));
        previousLocation = location;
        dateOfLastLocation = new Date();
        // TODO: switch isTravelling to false if no more location changed events are thrown!
    }

    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    public void onProviderEnabled(String s) {
    }

    public void onProviderDisabled(String s) {
    }
}
