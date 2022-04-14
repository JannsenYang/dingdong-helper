import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息
 * 通过小程序抓包购物车接口获取headers和body中的数据填入
 */
public class UserConfig {

    //收货地址id
    public static final String addressId = "";

    /**
     * 提前获取收货地址id 填写到addressId上 规则为该站点可送达的默认收货地址  如果没有请自行去APP中设置好再运行
     * 确认站点信息是否正确 如果不正确会导致购物车应该有东西显示无可购买或者下单失败
     * 每天抢之前先允许一下此接口 确认登录信息是否有效 如果失效了重新抓一次包
     */
    public static void main(String[] args) {
        String addressId = Api.getAddressId();
        System.out.println("请将addressId填入UserConfig.addressId变量中：" + addressId);
    }

    /**
     * 抓包后参考项目中的image/headers.jpeg 把信息一行一行copy到下面 没有的key不需要复制
     */
    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("ddmc-city-number", "");
        headers.put("ddmc-build-version", "");
        headers.put("ddmc-device-id", "");
        headers.put("ddmc-station-id", "");//这个是站点id 请仔细检查和确认这个参数 进入小程序之后首页左上角是不是你所在的站点 如果不是先选择好站点再抓包 不要把站点搞错了 否则不能下单
        headers.put("ddmc-channel", "applet");
        headers.put("ddmc-os-version", "[object Undefined]");
        headers.put("ddmc-app-client-id", "4");
        headers.put("cookie", "");//用户是否登录关键信息 请仔细检查
        headers.put("ddmc-ip", "");
        headers.put("ddmc-longitude", "");
        headers.put("ddmc-latitude", "");
        headers.put("ddmc-api-version", "");
        headers.put("ddmc-uid", "");
        headers.put("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 15_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.18(0x1800123c) NetType/WIFI Language/zh_CN");//有反馈说PC端小程序的UserAgent不能下单 如果是PC端抓包直接使用此Agent即可
        headers.put("referer", "https://servicewechat.com/wx1e113254eda17715/422/page-frame.html");
        return headers;
    }

    /**
     * 抓包后参考项目中的image/body.jpeg 把信息一行一行copy到下面 没有的key不需要复制
     * <p>
     * 这里不能加泛型 有些接口是params  泛型必须要求<String,String> 有些是form表单 泛型要求<String,Object> 无法统一
     */
    public static Map getBody() {
        Map body = new HashMap<>();
        body.put("uid", "");
        body.put("longitude", "");
        body.put("latitude", "");
        body.put("station_id", "");//这个是站点id 请仔细检查和确认这个参数 进入小程序之后首页左上角是不是你所在的站点 如果不是先选择好站点再抓包 不要把站点搞错了 否则不能下单
        body.put("city_number", "");
        body.put("api_version", "");
        body.put("app_version", "");
        body.put("applet_source", "");
        body.put("channel", "applet");
        body.put("app_client_id", "4");
        body.put("sharer_uid", "");
        body.put("openid", "");
        body.put("h5_source", "");
        body.put("device_token", "");//用户是否登录关键信息 请仔细检查
        return body;
    }

}
