package vk.nas.networking.nas2box.server;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import vk.nas.networking.nas2nas.server.Nas2nasServer;
import vk.nas.networking.pojo.NasInfo;
import vk.nas.util.Utils;
import vk.nas.networking.server.BaseServer;

import java.io.File;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Service
public class Nas2boxServer extends BaseServer {

    private static final String TAG=Nas2boxServer.class.getSimpleName();

    private static final String UPDATELIST_TYPE_ADD="add";
    private static final String UPDATELIST_TYPE_DEL="del";

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_IMUM_POOL_SIZE = 255;
    private ThreadPoolExecutor mExecutor;// 线程池对象

    private Runtime mRun = Runtime.getRuntime();// 获取当前运行环境，相当于windows的cmd
    private Process mProcess = null;// 进程
    private String mPing = "ping -c 1 -w 3 ";// 其中 -c 1为发送的次数，-w 表示发送后等待响应的时间
    private String localIp;// 本机IP地址-完整
    private String mLocAddress;// 局域网IP地址头,如：192.168.1.
    private String port;

    @Autowired
    private Environment environment;
    @Autowired
    private Nas2nasServer nas2nasServer;

    public String getNasList(String ip){
        printLog(TAG, "getNasList from box "+ip);
        return StringUtils.isBlank(NASLIST)?"":NASLIST;
    }

    public String report(String ip){
        printLog(TAG, "report from box "+ip);
        updateBoxList(ip, UPDATELIST_TYPE_ADD);
        return "ok";
    }

    public String getAccessNas(String ip){
        printLog(TAG, "getAccessNas from box "+ip);
        String accessNas=BOX2NASLIST.get(ip)==null?"fail":BOX2NASLIST.get(ip);
        printLog(TAG, "getAccessNas from box "+ip+",resp accessNas:"+accessNas);
        return accessNas;
    }

    /**
     * 扫描box
     */
    public void scanBox(){
        init();

        printLog(TAG, "开始扫描box设备,本机Ip为：" + localIp);

        if (StringUtils.isBlank(mLocAddress)) {
            printLog(TAG, "请检查网络");
            return;
        }

        //主nas才做
        if(!isMainNas(localIp))
            return;

        mExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_IMUM_POOL_SIZE,
                2000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(CORE_POOL_SIZE)
        );

        //线程池
        for (int i = 1; i < 255; i++) {
            final int lastAddress = i;
            Runnable run = () -> {
                //网络探测box设备
                scanBoxDo(lastAddress);
            };
            mExecutor.execute(run);
        }
        mExecutor.shutdown();
        while (true) {
            if (mExecutor.isTerminated()) {
                printLog(TAG, "扫描box结束");
//                printList("box", BOXLIST);
                break;
            }
        }
    }

    private void scanBoxDo(int lastAddress){
        String ping = mPing + mLocAddress + lastAddress;
        String ip = mLocAddress + lastAddress;
        if (localIp.equals(ip)) //如果与本机IP地址相同,跳过
            return;

        try {
            mProcess = mRun.exec(ping);
            int result = mProcess.waitFor();
//            printLog(TAG, "ping " + ip + " " + (result==0?"true":"false"));
            if(result != 0)
                updateBoxList(ip, UPDATELIST_TYPE_DEL);
        } catch (Exception e) {
            printLog(TAG, "ping异常["+ip+"]:"+ e.toString());
        } finally {
            if (mProcess != null)
                mProcess.destroy();
        }
    }

    private synchronized void updateBoxList(String ip, String type){
//        printLog(TAG, "ready to update boxlist");
        if(type.equals(UPDATELIST_TYPE_ADD)){
            if(BOXLIST.size()>0){
                if(!BOXLIST.contains(ip)){
                    BOXLIST.add(ip);
                    printLog(TAG, "operate " + ip + " " + type);
                    printList("updateBoxList本地box", BOXLIST);
                }
            }else{
                BOXLIST.add(ip);
                printLog(TAG, "operate " + ip + " " + type);
            }
        }else if(type.equals(UPDATELIST_TYPE_DEL)){
            if(BOXLIST.size()>0){
                if(BOXLIST.contains(ip)){
                    BOXLIST.remove(ip);
                    printLog(TAG, "operate " + ip + " " + type);
                    printList("updateBoxList本地box", BOXLIST);
                }
            }
        }
    }

    public void updateBox2nasList(){
        //test
        File file = new File(Utils.getPrjPath()+File.separator+"box2naslist.txt");
        String str=getFileContent(file);
        BOX2NASLIST=convert2box2nasList(str);
        if(true) return;
        //test

        init();

        if (StringUtils.isBlank(mLocAddress)) {
            printLog(TAG, "请检查网络");
            return;
        }

        //主nas才做
        if(!isMainNas(localIp))
            return;

        /*
         * 计算：box/nas，得到商n，匹配一个nas对应n个box
         * 列表格式：{"a" : "b","b" : "c"}
         */
        Map<String, String> tempBox2nasList=new HashMap<>();
        List<NasInfo> l=convert2nasList(NASLIST);
        int nasNum=l.size();

        int boxNum=BOXLIST.size();

        int n=boxNum/nasNum;
        int left=boxNum-n*nasNum;

        /*
            boxnum=20
            n=6
            left=2
            nasnum=3
            分配：7，7，6

            boxnum=23
            n=6
            left=5
            nasnum=3
            分配：8，8，7
         */

//        List<List> list=new ArrayList<List>();
//        for(int i=0;i<nasNum;i++){
//            List<String> list1 = new ArrayList<String>();
//            for(int j=0;j<n;j++){
//                list1.add("");
//            }
//            list.add(list1);
//        }
//        for(int i=0;i<list.size();i++){
//            List list1=list.get(i);
//            for(int j=0;j<left;j++){
//
//            }
//        }

        if(tempBox2nasList.size()>0)
            BOX2NASLIST=tempBox2nasList;
        printList("updateBox2nasList本地box2nas", BOX2NASLIST);
    }

    private void init(){
        localIp = Utils.getLocalIp();// 获取本机IP地址
        mLocAddress = Utils.getIpNetworkSegment(localIp);// 获取本地ip前缀

        port = environment.getProperty("server.port");//TODO
//        port = "8001";//test
    }

    public void printAllList(){
        printLog(TAG, "printAllList本地nas列表：\n"+NASLIST);
        printList("printAllList本地box", BOXLIST);
        printList("printAllList本地box2nas", BOX2NASLIST);
    }





    private static Nas2boxServer nas2boxServer;

    public static Nas2boxServer getInstance(){
        if(nas2boxServer==null)
            nas2boxServer=new Nas2boxServer();
        return nas2boxServer;
    }

}
