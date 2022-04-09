import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Application {




    public static final Map<String, Map<String, Object>> map = new ConcurrentHashMap<>();


    public static void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }

    
    public static void main(String[] args) {
//   此为单次执行模式  用于在非高峰期测试下单  也必须满足3个前提条件  1.有收货地址  2.购物车有商品 3.能选择配送信息
//        Map<String, Object> multiReserveTimeMap = Api.getMultiReserveTime(addressId, cartMap);
//        Map<String, Object> checkOrderMap = Api.getCheckOrder(addressId, cartMap, multiReserveTimeMap);
//        Api.addNewOrder(addressId, cartMap, multiReserveTimeMap, checkOrderMap);


        //此为高峰期策略 通过同时获取或更新 购物车、配送、订单确认信息再进行高并发提交订单
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    sleep();
                    Map<String, Object> cartMap = Api.getCart();
                    if (cartMap != null) {
                        map.put("cartMap", cartMap);
                    }
                }
            }).start();
        }
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    sleep();
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
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    sleep();
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
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                while (!map.containsKey("end")) {
                    if (map.get("cartMap") == null || map.get("multiReserveTimeMap") == null || map.get("checkOrderMap") == null) {
                        sleep();
                        continue;
                    }
                    Api.addNewOrder(UserConfig.addressId, map.get("cartMap"), map.get("multiReserveTimeMap"), map.get("checkOrderMap"));
                }
            }).start();
        }
    }
}
