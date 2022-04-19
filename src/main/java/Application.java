import cn.hutool.core.util.RandomUtil;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * 抢菜主程序 只能用于高峰期 并且运行两分钟以内 如未抢到不要再继续执行
 */
public class Application {


    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private static boolean timeTrigger(int hour, int minute, int second) {
        sleep(1000);
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);
        int currentSecond = Calendar.getInstance().get(Calendar.SECOND);
        System.out.println("时间触发 当前时间 " + currentHour + ":" + currentMinute + ":" + currentSecond + " 目标时间 " + hour + ":" + minute + ":" + second);
        return currentHour == hour && currentMinute == minute && currentSecond >= second;
    }

    public static void main(String[] args) {
        //此为高峰期策略 通过同时获取或更新 购物车、配送、订单确认信息再进行高并发提交订单
        //一定要注意 并发量过高会导致被风控 请合理设置线程数、等待时间和执行时间 不要长时间的执行此程序（我配置的线程数和间隔 2分钟以内）
        //如果想等过高峰期后进行简陋 长时间执行 则将线程数改为1  间隔时间改为10秒以上 并发越小越像真人 不会被风控  要更真一点就用随机数（自己处理）

        //并发执行策略
        //policy设置1 人工模式 运行程序则开始抢
        //policy设置2 时间触发 运行程序后等待早上5点59分30秒开始
        //policy设置3 时间触发 运行程序后等待早上8点29分30秒开始
        int policy = 1;//默认人工模式

        //最小订单成交金额 举例如果设置成50 那么订单要超过50才会下单
        double minOrderPrice = 0;

        //基础信息执行线程数
        int baseTheadSize = 2;

        //提交订单执行线程数
        int submitOrderTheadSize = 6;

        //取随机数
        //请求间隔时间最小值
        int sleepMillisMin = 300;
        //请求间隔时间最大值
        int sleepMillisMax = 500;


        //5点59分30秒时间触发
        while (policy == 2 && !timeTrigger(5, 59, 30)) {
        }

        //8点29分30秒时间触发
        while (policy == 3 && !timeTrigger(8, 29, 30)) {
        }


        //保护线程 2分钟未下单自动终止 避免对叮咚服务器造成压力 也避免封号  如果想长时间执行 请使用Sentinel哨兵模式
        new Thread(() -> {
            for (int i = 0; i < 120 && !Api.context.containsKey("end"); i++) {
                sleep(1000);
            }
            if (!Api.context.containsKey("end")) {
                Api.context.put("end", new HashMap<>());
                sleep(3000);
                System.err.println("未成功下单，执行2分钟自动停止");
            }
        }).start();

        for (int i = 0; i < baseTheadSize; i++) {
            new Thread(() -> {
                while (!Api.context.containsKey("end")) {
                    Api.allCheck();
                    //此接口作为补充使用 并不是一定需要 所以执行间隔拉大一点
                    sleep(RandomUtil.randomInt(3000, 5000));
                }
            }).start();
        }

        for (int i = 0; i < baseTheadSize; i++) {
            new Thread(() -> {
                while (!Api.context.containsKey("end")) {
                    Map<String, Object> cartMap = Api.getCart(policy == 2 || policy == 3);
                    if (cartMap != null) {
                        if (Double.parseDouble(cartMap.get("total_money").toString()) < minOrderPrice) {
                            System.err.println("订单金额：" + cartMap.get("total_money").toString() + " 不满足最小金额设置：" + minOrderPrice + " 继续重试");
                        } else {
                            Api.context.put("cartMap", cartMap);
                        }
                    }
                    sleep(RandomUtil.randomInt(sleepMillisMin, sleepMillisMax));
                }
            }).start();
        }
        for (int i = 0; i < baseTheadSize; i++) {
            new Thread(() -> {
                while (!Api.context.containsKey("end")) {
                    sleep(RandomUtil.randomInt(sleepMillisMin, sleepMillisMax));
                    if (Api.context.get("cartMap") == null) {
                        continue;
                    }
                    Map<String, Object> multiReserveTimeMap = Api.getMultiReserveTime(UserConfig.addressId, Api.context.get("cartMap"));
                    if (multiReserveTimeMap != null) {
                        Api.context.put("multiReserveTimeMap", multiReserveTimeMap);
                    }
                }
            }).start();
        }
        for (int i = 0; i < baseTheadSize; i++) {
            new Thread(() -> {
                while (!Api.context.containsKey("end")) {
                    sleep(RandomUtil.randomInt(sleepMillisMin, sleepMillisMax));
                    if (Api.context.get("cartMap") == null || Api.context.get("multiReserveTimeMap") == null) {
                        continue;
                    }
                    Map<String, Object> checkOrderMap = Api.getCheckOrder(UserConfig.addressId, Api.context.get("cartMap"), Api.context.get("multiReserveTimeMap"));
                    if (checkOrderMap != null) {
                        Api.context.put("checkOrderMap", checkOrderMap);
                    }
                }
            }).start();
        }
        for (int i = 0; i < submitOrderTheadSize; i++) {
            new Thread(() -> {
                while (!Api.context.containsKey("end")) {
                    if (Api.context.get("cartMap") == null || Api.context.get("multiReserveTimeMap") == null || Api.context.get("checkOrderMap") == null) {
                        continue;
                    }
                    if (Api.addNewOrder(UserConfig.addressId, Api.context.get("cartMap"), Api.context.get("multiReserveTimeMap"), Api.context.get("checkOrderMap"))) {
                        System.out.println("铃声持续1分钟，终止程序即可，如果还需要下单再继续运行程序");
                        Api.play();
                    }
                }
            }).start();
        }
    }
}
