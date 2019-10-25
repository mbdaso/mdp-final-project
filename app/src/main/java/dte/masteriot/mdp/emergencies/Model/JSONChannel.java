package dte.masteriot.mdp.emergencies.Model;

public class JSONChannel {
    public API_KEYS[] api_keys;
    public int id;
    public String latitude;
    public String longitude;

    public class API_KEYS {
        public String api_key;
        public Boolean write_flag;
    }
}

