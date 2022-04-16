import cn.hutool.core.util.RandomUtil;
import org.apache.commons.cli.CommandLine;

import java.util.Map;

/**
 * 哨兵捡漏模式 可长时间运行 此模式不能用于高峰期下单
 */
public class Sentinel {

    //最小订单成交金额 举例如果设置成50 那么订单要超过50才会下单
    private final double minOrderPrice;

    //执行任务请求间隔时间最小值
    private final int sleepMillisMin;

    //执行任务请求间隔时间最大值
    private final int sleepMillisMax;

    //单轮轮询时请求异常（叮咚服务器高峰期限流策略）尝试次数
    private final int loopTryCount;

    public Sentinel(double minOrderPrice, int sleepMillisMin, int sleepMillisMax, int loopTryCount) {
        this.minOrderPrice = minOrderPrice;
        this.sleepMillisMin = sleepMillisMin;
        this.sleepMillisMax = sleepMillisMax;
        this.loopTryCount = loopTryCount;
    }

    public Sentinel(CommandLine cli) {
        this.minOrderPrice = Double.parseDouble(cli.getOptionValue("m","0"));
        this.sleepMillisMin = Integer.parseInt(cli.getOptionValue("smin","30000"));
        this.sleepMillisMax = Integer.parseInt(cli.getOptionValue("smax","60000"));
        this.loopTryCount = Integer.parseInt(cli.getOptionValue("l", "10"));
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    public void run() {
        System.out.println("此模式模拟真人执行操作间隔不并发，不支持6点和8点30高峰期下单，如果需要在6点和8点30下单，请使用Application，设置policy = 2（6点）或 policy = 3(8点30)");
        System.out.println("3秒后执行，请确认上述内容");
        sleep(3000);

        //60次以后长时间等待10分钟左右
        int longWaitCount = 0;

        boolean first = true;
        while (!Api.context.containsKey("end")) {
            try {
                if (first) {
                    first = false;
                } else {
                    if (longWaitCount++ > 60) {
                        longWaitCount = 0;
                        System.out.println("执行60次循环后，休息10分钟左右再继续");
                        sleep(RandomUtil.randomInt(50000, 70000));
                    } else {
                        sleep(RandomUtil.randomInt(sleepMillisMin, sleepMillisMax));
                    }
                }
                Api.allCheck();

                Map<String, Object> cartMap = null;
                for (int i = 0; i < loopTryCount && cartMap == null && !Api.context.containsKey("noProduct"); i++) {
                    sleep(RandomUtil.randomInt(500, 2000));
                    cartMap = Api.getCart(true);
                }
                if (cartMap == null) {
                    Api.context.remove("noProduct");
                    continue;
                }

                if (Double.parseDouble(cartMap.get("total_money").toString()) < minOrderPrice) {
                    System.err.println("订单金额：" + cartMap.get("total_money").toString() + " 不满足最小金额设置：" + minOrderPrice + " 等待重试");
                    continue;
                }

                Map<String, Object> multiReserveTimeMap = null;
                for (int i = 0; i < loopTryCount && multiReserveTimeMap == null && !Api.context.containsKey("noReserve"); i++) {
                    sleep(RandomUtil.randomInt(500, 2000));
                    multiReserveTimeMap = Api.getMultiReserveTime(UserConfig.addressId, cartMap);
                }
                if (multiReserveTimeMap == null) {
                    Api.context.remove("noReserve");
                    continue;
                }

                Map<String, Object> checkOrderMap = null;
                for (int i = 0; i < loopTryCount && checkOrderMap == null; i++) {
                    sleep(RandomUtil.randomInt(500, 2000));
                    checkOrderMap = Api.getCheckOrder(UserConfig.addressId, cartMap, multiReserveTimeMap);
                }
                if (checkOrderMap == null) {
                    continue;
                }

                for (int i = 0; i < loopTryCount; i++) {
                    sleep(RandomUtil.randomInt(500, 2000));
                    if (Api.addNewOrder(UserConfig.addressId, cartMap, multiReserveTimeMap, checkOrderMap)) {
                        System.out.println("铃声持续1分钟，终止程序即可，如果还需要下单再继续运行程序");
                        Api.play();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
