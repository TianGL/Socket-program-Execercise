package geliang.ch5.client.bean;

public class ServerInfo {
    private int serverPort;
    private String ip;
    private String sn;

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public ServerInfo(int serverPort, String ip, String sn) {
        this.serverPort = serverPort;
        this.ip = ip;
        this.sn = sn;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "serverPort=" + serverPort +
                ", ip='" + ip + '\'' +
                ", sn='" + sn + '\'' +
                '}';
    }
}
