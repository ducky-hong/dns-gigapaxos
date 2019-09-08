package edu.umass.gigapaxos.example.dns;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class GeoIpUtilsTest {

    @Test
    public void getClosestAddress() {
        final String source = "128.119.202.40";
        final List<String> candidates = Arrays.asList("3.14.208.251", "52.36.51.74", "13.209.66.6");
        final String closestAddress = GeoIpUtils.getClosestAddress(source, candidates);
        assertEquals("3.14.208.251", closestAddress);
    }
}