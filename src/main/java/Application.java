import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Application {


    public static final Map<String, Map<String, Object>> map = new ConcurrentHashMap<>();


    public static void sleep(int millis) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }


    public static void main(String[] args) {

        if (UserConfig.addressId.length() == 0) {
            System.err.println("请先执行UserConfig获取配送地址id");
            return;
        }
//   此为单次执行模式  用于在非高峰期测试下单  也必须满足3个前提条件  1.有收货地址  2.购物车有商品 3.能选择配送信息
//        Api.allCheck();
//        Map<String, Object> cartMap = Api.getCart();
//        Map<String, Object> multiReserveTimeMap = Api.getMultiReserveTime(UserConfig.addressId, cartMap);
//        Map<String, Object> checkOrderMap = Api.getCheckOrder(UserConfig.addressId, cartMap, multiReserveTimeMap);
//        Api.addNewOrder(UserConfig.addressId, cartMap, multiReserveTimeMap, checkOrderMap);


        //此为高峰期策略 通过同时获取或更新 购物车、配送、订单确认信息再进行高并发提交订单

        //一定要注意 并发量过高会导致被风控 请合理设置线程数、等待时间和执行时间 不要长时间的执行此程序（2分钟以内）

        //基础信息执行线程数
        int baseTheadSize = 2;

        //提交订单执行线程数
        int submitOrderTheadSize = 6;

        //请求间隔时间
        int sleepMillis = 100;

        for (int i = 0; i < baseTheadSize; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    Api.allCheck();
                    //此接口作为补充使用 并不是一定需要 所以执行间隔拉大一点
                    sleep(1000);
                }
            }).start();
        }

        for (int i = 0; i < baseTheadSize; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    Map<String, Object> cartMap = Api.getCart();
                    if (cartMap != null) {
                        map.put("cartMap", cartMap);
                    }
                    sleep(sleepMillis);
                }
            }).start();
        }
        for (int i = 0; i < baseTheadSize; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    sleep(sleepMillis);
                    if (map.get("cartMap") == null) {
                        continue;
                    }
                    Map<String, Object> multiReserveTimeMap = Api.getMultiReserveTime(UserConfig.addressId, map.get("cartMap"));
                    if (multiReserveTimeMap != null) {
                        map.put("multiReserveTimeMap", multiReserveTimeMap);
                    }
                }
            }).start();
        }
        for (int i = 0; i < baseTheadSize; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    sleep(sleepMillis);
                    if (map.get("cartMap") == null || map.get("multiReserveTimeMap") == null) {
                        continue;
                    }
                    Map<String, Object> checkOrderMap = Api.getCheckOrder(UserConfig.addressId, map.get("cartMap"), map.get("multiReserveTimeMap"));
                    if (checkOrderMap != null) {
                        map.put("checkOrderMap", checkOrderMap);
                    }
                }
            }).start();
        }
        for (int i = 0; i < submitOrderTheadSize; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    if (map.get("cartMap") == null || map.get("multiReserveTimeMap") == null || map.get("checkOrderMap") == null) {
                        sleep(sleepMillis);
                        continue;
                    }
                    Api.addNewOrder(UserConfig.addressId, map.get("cartMap"), map.get("multiReserveTimeMap"), map.get("checkOrderMap"));
                }
            }).start();
        }
    }
}
