package com.github.mvollebregt.locationspy.gtalk;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.github.mvollebregt.locationspy.UserLocation;
import com.github.mvollebregt.locationspy.UserLocationPublisher;
import com.github.mvollebregt.locationspy.Serviceable;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.spark.util.DummySSLSocketFactory;

public class GtalkStatusUserLocationPublisher implements UserLocationPublisher, Serviceable {

    private XMPPConnection connection;
    private UserLocation lastKnownLocation;
    private ConnectivityManager connectivityManager;
    private boolean running;
    private String user;
    private String pass;

    public GtalkStatusUserLocationPublisher(String user, String pass) {
        this.user = user;
        this.pass = pass;
    }
//    private AlarmManager alarmManager;
//    private PendingIntent pendingIntent;

    public void publishLocation(UserLocation location) {
        lastKnownLocation = location;
        boolean success = publish();

        while (!success && running && connectivityManager.getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED ) {
            // try again in 20 seconds
            Log.i("LOCATIONSPY", "try again in 20 seconds");
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
            }
            success = publish();
//            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 20 * 1000, pendingIntent);
        }
    }

    private boolean publish() {
        try {
            connection.connect();
            connection.login(user, pass);
        } catch (Exception e) {
            Log.i("LOCATIONSPY", "Could not log in because of exception " + e.getMessage());
        } try {
            Presence presence = new Presence(Presence.Type.available, lastKnownLocation.getName(), 24, Presence.Mode.away); // priority must be equal to android client priority (24)
            connection.sendPacket(presence);
        } catch (Exception e) {
            Log.i("LOCATIONSPY", "Could not set new location because of exception " + e.getMessage());
            return false;
        }
        return true;
    }

    public void init(Context context) {
        ConnectionConfiguration xmppConfig = new ConnectionConfiguration("talk.google.com", 5223, "gmail.com");
        xmppConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        xmppConfig.setSocketFactory(new DummySSLSocketFactory());
        connection = new XMPPConnection(xmppConfig);
//        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        Intent intent = new Intent(context, GtalkStatusUserLocationPublisher.class);
//        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        lastKnownLocation = new UserLocation("");
        running = true;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        publish();
    }

    public void close(Context context) {
//        alarmManager.cancel(pendingIntent);
        running = false;
        connection.disconnect();
    }
}
