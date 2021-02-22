package vk.nas.util;

import java.io.File;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Utils {
    public static final String DATETIME_FORMAT="yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 获取项目路径
     */
    public static String getPrjPath(){
        try {
            return new File("").getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取本机ip
     */
    public static String getLocalIp() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return hostIp;
    }

    /**
     * 获取ip网段
     * @param localIp 本机IP地址
     */
    public static String getIpNetworkSegment(String localIp) {
        if (!localIp.equals("")) {
            return localIp.substring(0, localIp.lastIndexOf(".") + 1);
        }
        return null;
    }

}
