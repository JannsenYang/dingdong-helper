import org.apache.commons.cli.*;

import java.io.Reader;

/**
 * @author SoundOfAutumn
 * @date 2022/4/16 10:18
 */
public class Main {

    //命令行参数描述
    public static final String desc1 = "并发执行策略\n1为人工模式 运行程序则开始抢\n2为时间触发 运行程序后等待早上5点59分30秒开始\n3为时间触发 运行程序后等待早上8点29分30秒开始";
    public static final String desc2 = "最小订单成交金额 举例如果设置成50 那么订单要超过50才会下单";
    public static final String desc3 = "基础信息执行线程数";
    public static final String desc4 = "提交订单执行线程数";
    public static final String desc5 = "请求间隔时间最小值";
    public static final String desc6 = "请求间隔时间最大值";
    public static final String desc7 = "单轮轮询时请求异常（叮咚服务器高峰期限流策略）尝试次数";

    public static void main(String[] args) {
        //命令行参数解析
        Options options = new Options();
        options.addOption("p", "policy", true, desc1);
        options.addOption("m", "minOrderPrice", true, desc2);
        options.addOption("b", "baseTheadSize", true, desc3);
        options.addOption("st", "submitOrderTheadSize", true, desc4);
        options.addOption("smin", "sleepMillisMin", true, desc5);
        options.addOption("smax", "sleepMillisMax", true, desc6);
        options.addOption("l", "loopTryCount", true, desc7);

        options.addOption("t", "test", false, "测试是否能抢菜");
        options.addOption("v", "verify", false, "验证信息是否可用");
        options.addOption("s", "sentinel", false, "哨兵模式");

        options.addOption("h", "help", false, "打印帮助信息");

        CommandLine cli = null;
        CommandLineParser cliParser = new DefaultParser();
        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            cli = cliParser.parse(options, args);
        } catch (ParseException e) {
            helpFormatter.printHelp("dingdong-help",options);
            return;
        }
        assert cli != null;
        //打印帮助信息
        if (cli.hasOption("h")) {
            helpFormatter.printHelp("dingdong-help",options);
            return;
        }
        if (cli.hasOption("v")) {
            Api.checkUserConfig();
            return;
        }
        if (cli.hasOption("t")) {
            ApplicationTest.check();
            return;
        }
        //哨兵模式
        if (cli.hasOption("s")) {
            new Sentinel(cli).run();
            return;
        }
        //主程序
        new Application(cli).run();
    }

}
