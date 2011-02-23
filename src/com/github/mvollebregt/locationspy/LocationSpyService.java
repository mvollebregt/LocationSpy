package com.github.mvollebregt.locationspy;

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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.github.mvollebregt.locationspy.gtalk.GtalkStatusUserLocationPublisher;
import com.github.mvollebregt.locationspy.location.TravelListener;
import com.github.mvollebregt.locationspy.wifi.WifiConnectionListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The actual location spy service. As long as this service is running,
 * location spy is active.
 *
 * @author Michel Vollebregt
 */
public class LocationSpyService extends Service implements UserLocationListener {

    UserLocationDetector[] detectors;
    List<UserLocationPublisher> publishers;

    private Properties readProperties(String filename) throws IOException {
        Properties properties = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(filename);
            properties.load(in);
        } finally {
            if (in != null) in.close();
        }
        return properties;
    }

    private void setUpService() {

        // read locations from settings
        Properties locations = null;
        try {
            locations = readProperties("location.properties");
        } catch (IOException e) {
            // TODO: proper exception handling
            e.printStackTrace();
        }

        // set up location detectors
        detectors = new UserLocationDetector[] {new WifiConnectionListener(locations)}; // TODO: add TravelListener

        // read username and password from settings
        Properties accounts = null;
        try {
            accounts = readProperties("account.properties");
        } catch (IOException e) {
            // TODO: proper exception handling
            e.printStackTrace();
        }

        // set up publishers
        publishers = new ArrayList<UserLocationPublisher>();
        for (String account : accounts.stringPropertyNames()) {
            publishers.add(new GtalkStatusUserLocationPublisher(account, accounts.getProperty(account)));
        }
    }

    private void cleanUpService() {
        detectors = null;
        publishers = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        setUpService();
        for (UserLocationDetector detector : detectors) {
            if (detector instanceof Serviceable) ((Serviceable) detector).init(this);
        }
        for (UserLocationPublisher publisher : publishers) {
            if (publisher instanceof Serviceable) ((Serviceable) publisher).init(this);
        }
        locationChanged(null);
        for (UserLocationDetector detector : detectors) {
            detector.setLocationListener(this);
        }
    }

    @Override
    public void onDestroy() {
        for (UserLocationDetector detector : detectors) {
            if (detector instanceof Serviceable) ((Serviceable) detector).close(this);
        }
        for (UserLocationPublisher publisher : publishers) {
            if (publisher instanceof Serviceable) ((Serviceable) publisher).close(this);
        }
        cleanUpService();
    }

    public void locationChanged(UserLocation location) {
        UserLocation currentLocation = null;
        for (UserLocationDetector detector : detectors) {
            currentLocation = detector.currentLocation(this);
            if (currentLocation != null) break;
        }
        if (currentLocation == null) currentLocation = new UserLocation("Weg van huis");
        for (UserLocationPublisher publisher: publishers) {
            publisher.publishLocation(location);
        }
    }
}
