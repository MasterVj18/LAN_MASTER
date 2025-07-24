package common;

public class DeviceInfo {
    public String hostname;
    public String ip;
    public String mac;

    public DeviceInfo(String hostname, String ip, String mac) {
        this.hostname = hostname;
        this.ip = ip;
        this.mac = mac;
    }

    @Override
    public String toString() {
        return hostname + " | IP: " + ip + " | MAC: " + mac;
    }
}
