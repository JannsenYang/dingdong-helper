import cn.hutool.core.io.FastByteArrayOutputStream;
import cn.hutool.core.io.IoUtil;
import cn.hutool.json.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 用户信息
 * 通过小程序抓包购物车接口获取headers和body中的数据填入
 */
public class UserConfig {

    //城市
    public static final String cityId = "0101";//默认上海

    //站点id
    public static final String stationId;

    //收货地址id
    public static final String addressId;

    //需要抓包填写的配置文件
    public static final JSONObject config;

    static {
//        final String path = Objects.requireNonNull(Api.class.getClassLoader().getResource("config.json")).getPath();
//        config = JSONUtil.readJSONObject(new File(path), UTF_8);
        final InputStream inputStream = Objects.requireNonNull(Api.class.getClassLoader().getResourceAsStream("config.json"));
        config = JSONUtil.parseObj(IoUtil.read(inputStream, UTF_8));

        stationId = config.getJSONObject("headers").getStr("ddmc-station-id");
        addressId = config.getJSONObject("headers").getStr("ddmc-address-id");

    }

    /**
     * 确认收货地址id和站点id
     * 每天抢之前先允许一下此接口 确认登录信息是否有效 如果失效了重新抓一次包
     */
    public static void main(String[] args) {
        Api.checkUserConfig();
    }

    /**
     * 抓包后参考项目中的image/headers.jpeg 把信息一行一行copy到下面 没有的key不需要复制
     */
    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("ddmc-city-number", cityId);
        headers.put("ddmc-time", String.valueOf(new Date().getTime() / 1000));
        headers.put("ddmc-build-version", "2.83.0");
        headers.put("ddmc-station-id", stationId);
        headers.put("ddmc-channel", "applet");
        headers.put("ddmc-os-version", "[object Undefined]");
        headers.put("ddmc-app-client-id", "4");
        headers.put("ddmc-ip", "");
        headers.put("ddmc-api-version", "9.50.0");
        headers.put("accept-encoding", "gzip,compress,br,deflate");
        headers.put("referer", "https://servicewechat.com/wx1e113254eda17715/425/page-frame.html");

        // ------------  填入以下6项 上面不要动 ------------
        final JSONObject configHeaders = config.getJSONObject("headers");
        headers.put("ddmc-device-id", configHeaders.getStr("ddmc-device-id"));
        headers.put("cookie", configHeaders.getStr("cookie"));
        headers.put("ddmc-longitude", configHeaders.getStr("ddmc-longitude"));
        headers.put("ddmc-latitude", configHeaders.getStr("ddmc-latitude"));
        headers.put("ddmc-uid", configHeaders.getStr("ddmc-uid"));
        headers.put("user-agent", configHeaders.getStr("user-agent"));
        return headers;
    }

    /**
     * 抓包后参考项目中的image/body.jpeg 把信息一行一行copy到下面 没有的key不需要复制
     * <p>
     * 这里不能加泛型 有些接口是params  泛型必须要求<String,String> 有些是form表单 泛型要求<String,Object> 无法统一
     */
    public static Map getBody(Map<String, String> headers) {
        Map body = new HashMap<>();
        body.put("uid", headers.get("ddmc-uid"));
        body.put("longitude", headers.get("ddmc-longitude"));
        body.put("latitude ", headers.get("ddmc-latitude"));
        body.put("station_id", headers.get("ddmc-station-id"));
        body.put("city_number", headers.get("ddmc-city-number"));
        body.put("api_version", headers.get("ddmc-api-version"));
        body.put("app_version ", headers.get("ddmc-build-version"));
        body.put("applet_source", "");
        body.put("channel", "applet");
        body.put("app_client_id", "4");
        body.put("sharer_uid", "");
        body.put("h5_source", "");
        body.put("time", headers.get("ddmc-time"));

        // ------------  填入这3项上面不要动 ------------
        final JSONObject configBody = config.getJSONObject("body");
        body.put("s_id", configBody.getStr("s_id"));
        body.put("openid", configBody.getStr("openid"));
        body.put("device_token", configBody.getStr("device_token"));
        return body;
    }
}
