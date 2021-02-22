package vk.nas.networking.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import vk.nas.networking.pojo.NasInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseServer {

    private static final String TAG=BaseServer.class.getSimpleName();

    public static String NASLIST;
    public static List<String> BOXLIST=new ArrayList<>();
    public static Map<String, String> BOX2NASLIST=new HashMap<>();

    private ObjectMapper objectMapper=new ObjectMapper();

    public String getFileContent(File file){
        if (file.exists()) {
            InputStream instream = null;
            InputStreamReader inputreader = null;
            BufferedReader buffreader = null;
            try {
                instream = new FileInputStream(file);
                inputreader = new InputStreamReader(instream);
                buffreader = new BufferedReader(inputreader);
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = buffreader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    assert instream != null;
                    instream.close();
                    assert inputreader != null;
                    inputreader.close();
                    assert buffreader != null;
                    buffreader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void printList(String name, Object val){
        try {
            printLog(TAG, name+"列表："+objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(val));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public String convert2str(Object val){
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(val);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public List<NasInfo> convert2nasList(String str){
        /* 文件内容：
           [
               {"ip":"192.168.1.4","dateTime":"2020-12-15 11:11:11.333"},
               {"ip":"192.168.1.3","dateTime":"2020-12-15 11:11:21.333"}
           ]
        */
        if(StringUtils.isNotBlank(str)){
            try {
                return objectMapper.readValue(str, new TypeReference<List<NasInfo>>(){});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected List<String> convert2boxList(String str){
        if(StringUtils.isNotBlank(str)){
            try {
                return objectMapper.readValue(str, new TypeReference<List<String>>(){});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected Map<String, String> convert2box2nasList(String str){
        if(StringUtils.isNotBlank(str)){
            try {
                return objectMapper.readValue(str, new TypeReference<Map<String, String>>(){});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected boolean isMainNas(String localIp){
        String mainNasIp=getMainNasIp();
        if(!localIp.equals(mainNasIp)){
            printLog(TAG, "本机["+localIp+"]不是主nas["+mainNasIp+"]");
            return false;
        }
        return true;
    }

    private String getMainNasIp(){
        List<NasInfo> l=convert2nasList(NASLIST);
        if(l!=null && l.size()>0){
            return l.get(0).getIp();
        }
        return "";
    }

    protected void printLog(String TAG, String msg){
        System.out.println("---"+TAG+"---"+msg);
    }




    private static BaseServer baseServer;
    public static BaseServer getInstance(){
        if(baseServer==null)
            baseServer=new BaseServer();
        return baseServer;
    }

}
