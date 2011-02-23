package com.github.mvollebregt.locationspy.wifi;

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.github.mvollebregt.locationspy.UserLocationDetector;
import com.github.mvollebregt.locationspy.UserLocationListener;
import com.github.mvollebregt.locationspy.Serviceable;
import com.github.mvollebregt.locationspy.UserLocation;

import java.util.Hashtable;

public class WifiConnectionListener extends BroadcastReceiver implements UserLocationDetector, Serviceable {

    private UserLocationListener listener;
    private Hashtable<String, String> ssids;

    public WifiConnectionListener(Hashtable ssids) {
        this.ssids = ssids;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i("LOCATIONSPY", "WifiConnectionListener received an event of type: " + action);
        NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        if (action.equalsIgnoreCase(ConnectivityManager.CONNECTIVITY_ACTION /*WifiManager.NETWORK_STATE_CHANGED_ACTION*/) && info.getState() == NetworkInfo.State.CONNECTED) {
            UserLocation location = currentLocation(context);
            listener.locationChanged(location);
        }
    }

    public void setLocationListener(UserLocationListener listener) {
         this.listener = listener;
    }

    public UserLocation currentLocation(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        Log.i("LOCATIONSPY", "BSSID of network: " + wifiInfo.getBSSID());
        String locationName = ssids.get(wifiInfo.getSSID());
        return locationName != null ? new UserLocation(locationName) : null;
    }

    public void init(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, filter);
    }

    public void close(Context context) {
         context.unregisterReceiver(this);
    }
}
