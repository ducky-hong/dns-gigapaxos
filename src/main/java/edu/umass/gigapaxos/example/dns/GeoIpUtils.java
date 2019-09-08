package edu.umass.gigapaxos.example.dns;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.util.List;

public class GeoIpUtils {

    private static DatabaseReader reader;

    public static LatLng getApproximateLocation(final String ipAddress) {
        if (reader == null) {
            final File database = new File("GeoLite2-City.mmdb");
            try {
                reader = new DatabaseReader.Builder(database).build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            final CityResponse city = reader.city(address);
            return new LatLng(city.getLocation().getLatitude(),
                    city.getLocation().getLongitude());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeoIp2Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getClosestAddress(final String sourceAddress, final List<String> candidateAddresses) {
        if (candidateAddresses == null || candidateAddresses.isEmpty()) {
            return null;
        }
        if (candidateAddresses.size() == 1) {
            return candidateAddresses.get(0);
        }

        final LatLng sourceLocation = getApproximateLocation(sourceAddress);
        Double minDistance = null;
        String closestCandidate = null;
        for (String candidate : candidateAddresses) {
            final LatLng candidateLocation = getApproximateLocation(candidate);
            final double distance = LatLngTool.distance(sourceLocation, candidateLocation, LengthUnit.KILOMETER);
            if (minDistance == null || distance < minDistance) {
                minDistance = distance;
                closestCandidate = candidate;
            }
        }
        return closestCandidate;
    }
}
