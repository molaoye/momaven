package vk.nas.networking.pojo;

public class NasInfo {
    private String ip;
    private String dateTime;

    //jackson反向转成bean需要空结构化方法
    public NasInfo() {
    }

    public NasInfo(String ip, String dateTime) {
        this.ip = ip;
        this.dateTime = dateTime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
