package vk.nas.networking.nas2nas.server;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import vk.nas.networking.server.BaseServer;
import vk.nas.util.RestTemplateUtil;
import vk.nas.util.Utils;
import vk.nas.networking.pojo.NasInfo;

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Service
public class Nas2nasServer extends BaseServer{

    private static final String TAG=Nas2nasServer.class.getSimpleName();

    /** 核心池大小 **/
    private static final int CORE_POOL_SIZE = 1;
    /** 线程池最大线程数 **/
    private static final int MAX_IMUM_POOL_SIZE = 255;
    private ThreadPoolExecutor mExecutor;// 线程池对象

    private String localIp;// 本机IP地址-完整
    private String mLocAddress;// 局域网IP地址头,如：192.168.1.
    private String port;

    private List<String> mIpList = new ArrayList<>();// ping成功的IP地址
    private static final String UPDATELIST_TYPE_ADD="add";
    private static final String UPDATELIST_TYPE_DEL="del";

    private SimpleDateFormat sdf=new SimpleDateFormat(Utils.DATETIME_FORMAT);

    @Autowired
    private Environment environment;

    public void syncData(){
        init();

        if (StringUtils.isBlank(mLocAddress)) {
            printLog(TAG, "请检查网络");
            return;
        }

        //主nas才做
        if(!isMainNas(localIp))
            return;

        //同步的数据有：boxlist，box2naslist，请求naslist（除自己）把数据发送过去
        List<NasInfo> list=convert2nasList(NASLIST);
        if(list!=null && list.size()>0){
            for(NasInfo nasInfo:list){
                //除自己
                if(localIp.equals(nasInfo.getIp())){//TODO
                    continue;
                }

                try {
                    String ip=nasInfo.getIp();
//                    ip="192.168.1.127";//test
                    String url = "http://" + ip + ":" + port + "/nas/syncData";
                    MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
                    String boxList=BOXLIST.size()>0 ? convert2str(BOXLIST) : "";
                    String box2nasList=BOX2NASLIST.size()>0 ? convert2str(BOX2NASLIST) : "";
                    param.add("ip", localIp);
                    param.add("box2nasList", box2nasList);
                    param.add("boxList", boxList);
//                    if(ip.equals("192.168.1.125")){//test
//                        printLog(TAG, "");
////                        param.remove("nasList");
////                        param.add("nasList", "[ {  \"ip\" : \"192.168.1.128\",  \"dateTime\" : \"2020-12-18 19:42:48.023\"} ]");
//                    }
                    ResponseEntity responseEntity = RestTemplateUtil.postReq2(url, param, String.class);
                    int code = responseEntity.getStatusCodeValue();
                    Object body=responseEntity.getBody();
                    printLog(TAG, "req["+url+"] resp code:"+code+" body:\n"+body);
                } catch (Exception e) {
                    printLog(TAG, "syncData error:"+e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 被主nas同步
     */
    public void syncDataUpdate(String boxList, String box2nasList, String ip){
        printLog(TAG, "syncDataUpdate from nas "+ip+" req-----\nboxList:\n"+boxList+"\nbox2nasList:\n"+box2nasList);
        if(StringUtils.isNotBlank(boxList)){
            List<String> list=convert2boxList(boxList);
            if(list!=null && list.size()>0)
                BOXLIST=list;
        }
        printList("syncDataUpdate本地box", BOXLIST);
        if(StringUtils.isNotBlank(box2nasList)){
            Map<String, String> map=convert2box2nasList(box2nasList);
            if(map!=null && map.size()>0)
                BOX2NASLIST=map;
        }
        printList("syncDataUpdate本地box2nas", BOX2NASLIST);
    }

    /**
     * naslist
     */
    public String getNasList(String remoteNasList, String ip){
        printLog(TAG, "getNasList from nas "+ip+" req-----\nnasList:\n"+remoteNasList);
        /*
         * 若本地ip不在远程naslist TODO 并且本地naslist不含本地ip 则新增，如果包含则合并到远程nalist
         * 更新本地naslist并返回；否则不干任何事并返回空
         */
        if(StringUtils.isNotBlank(remoteNasList) && !remoteNasList.contains(localIp)){
            List l=convert2nasList(remoteNasList);
            try {
                if(l!=null && l.size()>0)
                    updateNasList(UPDATELIST_TYPE_ADD, localIp, l);

                String str=getNaslistContent();

                printLog(TAG, "getNasList from nas "+ip+" resp:\n"+str);
                return str;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        printLog(TAG, "getNasList from nas "+ip+" resp:空");
        return "";
    }

    /**
     * 扫描nas
     */
    public void scanNas() {
        init();

        printLog(TAG, "开始扫描nas设备,本机Ip为：" + localIp);

        if (StringUtils.isBlank(mLocAddress)) {
            printLog(TAG, "请检查网络");
            return;
        }

        //若本地无naslist新增本地ip
        try {
            if(!isValidOfNasList())
                updateNasList(UPDATELIST_TYPE_ADD, localIp, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        scanNasMultiThread();
    }

    private void scanNasMultiThread() {
        /**
         * 1.核心池大小 2.线程池最大线程数 3.表示线程没有任务执行时最多保持多久时间会终止
         * 4.参数keepAliveTime的时间单位，有7种取值,当前为毫秒
         * 5.一个阻塞队列，用来存储等待执行的任务，这个参数的选择也很重要，会对线程池的运行过程产生重大影响
         * ，一般来说，这里的阻塞队列有以下几种选择：
         */
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
                //网络探测nas设备
                scanNasTelnetOnly(lastAddress);
            };
            mExecutor.execute(run);
        }
        mExecutor.shutdown();
        while (true) {
            try {
                if (mExecutor.isTerminated()) {
                    printLog(TAG, "扫描nas结束,总共成功扫描到" + mIpList.size() + "个设备.");
                    if(mIpList.size()>0){
                        printList("远程nas", mIpList);
                        for(String ip:mIpList){
                            reqNasList(ip);
                        }
                    }
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Telnet(利用socket比ping更有效)之后得到有效naslist
     */
    private void scanNasTelnetOnly(int lastAddress){
        String ip = mLocAddress + lastAddress;
        if (localIp.equals(ip)) //如果与本机IP地址相同,跳过
            return;

        int port2=Integer.valueOf(port),timeout=1000;
        Socket socket = new Socket();
        boolean isConnected = false;
        try {
            socket.connect(new InetSocketAddress(ip, port2), timeout);
            isConnected = socket.isConnected();
            printLog(TAG, "telnet "+ip+" "+port+" "+isConnected);
        } catch (Exception e) {
//            printLog(TAG, "Telnet异常["+ip+"]:"+ e.toString());
        }finally{
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (isConnected) {
            mIpList.add(ip);
        } else {
            if (isValidOfNasList()) {
                try {
                    //naslist的nas访问不通删除
                    updateNasList(UPDATELIST_TYPE_DEL, ip, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void reqNasList(String ip){
        if (localIp.equals(ip)) //如果与本机IP地址相同,跳过
            return;

        try {
                        /*
                         * 请求\nas\list时若本地nas有naslist，naslist的nas访问不通删除；
                         * 请求\nas\list时把本地naslist（若本地无naslist新增本地ip）传入，然后得到naslist返回更新到本地；
                         */
            if(!isValidOfNasList())
                updateNasList(UPDATELIST_TYPE_ADD, localIp, null);
            boolean nasListIsValid=isValidOfNasList();
            boolean b=reqNasListDo(ip, nasListIsValid);
            printLog(TAG, "call "+ip+":"+port+" "+b+" naslist is valid="+nasListIsValid);
        } catch (Exception e) {
            printLog(TAG, "call异常["+ip+"]:"+ e.toString());
        } finally {
        }
    }

    /**
     * 请求naslist，得到naslist返回更新到本地
     */
    private synchronized boolean reqNasListDo(String ip, boolean nasListIsValid) {
        if(nasListIsValid) {
            try {
                String str = getNaslistContent();
                if (StringUtils.isNotBlank(str)) {
                    //传入naslist
                    String url = "http://" + ip + ":" + port + "/nas/list";
                    MultiValueMap<String, String> param = new LinkedMultiValueMap<>();
                    param.add("nasList", str);
                    param.add("ip", localIp);
//                    if(ip.equals("192.168.1.125")){//test
//                        printLog(TAG, "");
////                        param.remove("nasList");
////                        param.add("nasList", "[ {  \"ip\" : \"192.168.1.128\",  \"dateTime\" : \"2020-12-18 19:42:48.023\"} ]");
//                    }
                    ResponseEntity responseEntity = RestTemplateUtil.postReq2(url, param, String.class);
                    int code = responseEntity.getStatusCodeValue();
                    Object body=responseEntity.getBody();
                    printLog(TAG, "req["+url+"] resp code:"+code+" body:\n"+body);
                    if (code != 200)
                        return false;

                    //得到naslist返回更新到本地
                    if (body != null)
                        updateNasListContent(body.toString(), "reqNasListDo");
                    return true;
                }
            } catch (Exception e) {
//        e.printStackTrace();
                printLog(TAG, e.getMessage());
            }
        }
        return false;
    }

    private void init(){
        localIp = Utils.getLocalIp();// 获取本机IP地址
//        localIp="192.168.1.125";//test
        mLocAddress = Utils.getIpNetworkSegment(localIp);// 获取本地ip前缀

        port = environment.getProperty("server.port");//TODO
//        port = "8001";//test

        mIpList=new ArrayList<>();
    }

    private boolean isValidOfNasList(){
        if(StringUtils.isNotBlank(getNaslistContent()))
            return true;
        return false;
    }

    private synchronized String getNaslistContent(){
        return NASLIST;
    }

    /**
     * 更新naslist和本地
     * @param type add,del
     */
    private synchronized List<NasInfo> updateNasList(String type, String ip, List<NasInfo> nasList) throws Exception {
//        printLog(TAG, "ready to update nasList");
//        if(ip.equals("192.168.1.3")){//test
//            printLog(TAG, "");
//        }
        boolean needUpdate=false;
        String naslistContent=getNaslistContent();
        if (nasList == null && StringUtils.isNotBlank(naslistContent))
            nasList = convert2nasList(naslistContent);
        if (nasList != null && nasList.size() > 0) {
            if (type.equals(UPDATELIST_TYPE_ADD)) {
                nasList.add(new NasInfo(ip, sdf.format(new Date())));
                needUpdate=true;
                printLog(TAG, "operate " + ip + " " + type + " 1");
            } else if (type.equals(UPDATELIST_TYPE_DEL)) {
                for (NasInfo nasInfo : nasList) {
                    if (nasInfo.getIp().equals(ip) && !ip.equals(localIp)) {
                        nasList.remove(nasInfo);
                        needUpdate=true;
                        printLog(TAG, "operate " + ip + " " + type);
                        break;
                    }
                }
            }
        } else {
            if (type.equals(UPDATELIST_TYPE_ADD)) {
                nasList = new ArrayList<>();
                nasList.add(new NasInfo(ip, sdf.format(new Date())));
                needUpdate=true;
                printLog(TAG, "operate " + ip + " " + type + " 2");
            }
        }

//        printLog(TAG, "NasList needUpdate=" + needUpdate);
        if(needUpdate && nasList != null && nasList.size() > 0){
            //list排序
            Collections.sort(nasList, (n1, n2) -> {
                //按日期前后
                boolean b = false;
                try {
                    b = sdf.parse(n1.getDateTime()).after(sdf.parse(n2.getDateTime()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                if (b)
                    return 1;
                return -1;
            });
            //更新本地
            updateNasListContent(convert2str(nasList), "updateNasList");
        }
        return nasList;
    }

    private synchronized void updateNasListContent(String content, String from){
//        if(content.equals("null")){//test
//            printLog(TAG, "");
//        }
        printLog(TAG,
                "----------------------------------------\n" +
                        "updateNasListContent from method "+from+" content:\n" +
                        content + "\n" +
                        "----------------------------------------"
        );
        if(StringUtils.isNotBlank(content))
            NASLIST=content;
        printLog(TAG, "updateNasListContent本地nas列表：\n"+NASLIST);
    }







    private static Nas2nasServer nas2nasServer;

    public static Nas2nasServer getInstance(){
        if(nas2nasServer==null){
            nas2nasServer=new Nas2nasServer();
        }
        return nas2nasServer;
    }

}
