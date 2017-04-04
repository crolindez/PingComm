package es.carlosrolindez.wificomm;

import java.util.Random;

public class WiFiConstants {
    protected static final String SERVICE_NAME = "es.carlosrolindez.wificomm";
    protected static final String SERVICE_TYPE = "_http._tcp.";
    private static String SERVICE_HEX = "";

    public static String getHexName() {
        if (SERVICE_HEX.equals("")) {
            SERVICE_HEX = Integer.toHexString(new Random().nextInt(0xFFFF));
        }
        return SERVICE_HEX;
    }
}
