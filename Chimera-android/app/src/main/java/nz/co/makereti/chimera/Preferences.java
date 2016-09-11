package nz.co.makereti.chimera;

public final class Preferences {
    public static final String NAME = "GarageOpener";

    // e.g. "http://yourhouse.dyndys.com:1234/open-garage-script.cgi"
    public static final String KEY_URL = "garagedoor.url";

    // the shared secret (~password) with your server.
    public static final String KEY_SECRET = "garagedoor.secret";

    // SSID to auto-open the garage on
    public static final String KEY_SSID = "garagedoor.ssid";

    private Preferences() {}
}
