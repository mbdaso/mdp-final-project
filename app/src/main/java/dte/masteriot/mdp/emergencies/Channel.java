package dte.masteriot.mdp.emergencies;

class Channel {
    API_KEYS[] api_keys;
    int id;
    String latitude;
    String longitude;

    class API_KEYS {
        String api_key;
        Boolean write_flag;
    }
}
