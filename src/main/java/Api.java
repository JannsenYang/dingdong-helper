import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.ImmutableMap;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口封装
 */
public class Api {

    public static final Map<String, Map<String, Object>> context = new ConcurrentHashMap<>();


    private static Invocable invocable;

    private static boolean jdk8Warning = false;

    /**
     * 签名
     *
     * @param body
     */
    private static Map sign(Map body) {
        if (jdk8Warning) {
            return body;
        }
        try {
            if (invocable == null) {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByExtension("js");
                if (engine == null) {
                    if (!jdk8Warning) {
                        System.err.println("请使用jdk1.8版本，高版本不支持请求中的签名参数，不影响功能，只是参数中不带签名");
                        jdk8Warning = true;
                    }
                    return body;
                }
                engine.eval(FileUtil.readString(new File("sign.js"), "UTF-8"));
                invocable = (Invocable) engine;
            }
            Object object = invocable.invokeFunction("sign", JSONUtil.toJsonStr(body));
            Map signMap = JSONUtil.toBean(object.toString(), Map.class);
            body.put("nars", signMap.get("nars"));
            body.put("sesi", signMap.get("sesi"));
        } catch (ScriptException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return body;
    }

    /**
     * 时间触发模式和哨兵模式播放音效提醒 请将电脑声音开到合适音量
     */
//    @SneakyThrows 不少人没有安装lombok插件 还是用传统的try catch吧
    public static void play() {
        try {
            //这里还可以使用企业微信或者钉钉的提供的webhook  自己写代码 很简单 就是按对应数据格式发一个请求到企业微信或者钉钉
            AudioClip audioClip = Applet.newAudioClip(new File("ding-dong.wav").toURL());
            audioClip.loop();
            Thread.sleep(60000);//响铃60秒
        } catch (InterruptedException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 抢菜程序如果终止则不输出无意义信息
     *
     * @param normal  false 输出error级别
     * @param message 输出信息
     */
    private static void print(boolean normal, String message) {
        if (Api.context.containsKey("end")) {
            return;
        }
        if (normal) {
            System.out.println(message);
        } else {
            System.err.println(message);
        }
    }


    /**
     * 验证请求是否成功
     *
     * @param object     返回体
     * @param actionName 动作名称
     * @return 是否成功
     */
    private static boolean isSuccess(JSONObject object, String actionName) {
        Boolean success = object.getBool("success");
        if (success == null) {
            if ("405".equals(object.getStr("code"))) {
                print(false, actionName + "失败:" + "出现此问题有三个可能 1.偶发，无需处理 2.一个账号一天只能下两单  3.不要长时间运行程序，目前已知有人被风控了，暂时未确认风控的因素是ip还是用户或设备相关信息，如果要测试用单次执行模式，并发只能用于6点、8点半的前一分钟，然后执行时间不能超过2分钟，如果买不到就不要再执行程序了，切忌切忌");
                print(false, "405问题解决方案，不保证完全有效,退出App账号重新登录，尝试刷新购物车和提交订单是否正常，如果正常退出小程序重新登录后再抓包，替换UserConfig中的cookie和device_token。");
            } else {
                print(false, actionName + "失败,服务器返回无法解析的内容:" + JSONUtil.toJsonStr(object));
            }
            return false;
        }
        if (success) {
            return true;
        }
        if ("您的访问已过期".equals(object.getStr("message"))) {
            context.put("end", new HashMap<>());
            System.err.println("用户信息失效，请确保UserConfig参数准确，并且微信上的叮咚小程序不能退出登录");
            return false;
        }
        String msg = null;
        try {
            msg = object.getStr("msg");
            if (msg == null || "".equals(msg)) {
                msg = object.getJSONObject("tips").getStr("limitMsg");
            }
        } catch (Exception ignored) {

        }
        print(false, actionName + " 失败:" + (msg == null || "".equals(msg) ? "未解析返回数据内容，全字段输出:" + JSONUtil.toJsonStr(object) : msg));
        return false;
    }


    /**
     * 检查用户信息
     */
    public static void checkUserConfig() {
        try {
            System.out.println("开始获取收货人信息");
            HttpRequest httpRequest = HttpUtil.createGet("https://sunquan.api.ddxq.mobi/api/v1/user/address/");
            Map<String, String> headers = UserConfig.getHeaders();
            httpRequest.addHeaders(headers);
            httpRequest.formStr(sign(UserConfig.getBody(headers)));

            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "获取默认收货地址")) {
                return;
            }
            JSONArray validAddress = object.getJSONObject("data").getJSONArray("valid_address");
            System.out.println("获取可用的收货地址条数：" + validAddress.size());
            for (int i = 0; i < validAddress.size(); i++) {
                JSONObject address = validAddress.getJSONObject(i);
                if (address.getBool("is_default")) {
                    JSONObject stationInfo = address.getJSONObject("station_info");

                    System.out.println("获取默认收货地址成功 请仔细核对站点和收货地址信息 站点信息配置错误将导致无法下单");
                    System.out.println("1.该地址对应城市名称为：" + stationInfo.get("city_name"));
                    System.out.println("2.该地址对应站点名称为：" + stationInfo.get("name"));
                    System.out.println("3.该地址详细信息：" + address.getStr("addr_detail") + " 手机号：" + address.getStr("mobile"));
                    System.out.println("");


                    if (address.containsValue("city_number") ||  !address.getStr("city_number").equals(UserConfig.cityId)) {
                        if(address.containsValue("city_number")){
                            System.err.println("城市id配置不正确，请填入UserConfig.cityId = " + stationInfo.getStr("city_number"));
                        }else{
                            System.err.println("城市id未从接口中获取，请人工确认城市id是否正确，通过抓包可以看到请求体中有city_number字段，上海默认0101，不用改");
                        }
                    } else {
                        System.out.println("城市id配置正确");
                    }
                    if (!stationInfo.getStr("id").equals(UserConfig.stationId)) {
                        System.err.println("站点id配置不正确，请填入UserConfig.stationId = " + stationInfo.getStr("id"));
                    } else {
                        System.out.println("站点id配置正确");
                    }
                    if (!address.getStr("id").equals(UserConfig.addressId)) {
                        System.err.println("地址id配置不正确，请填入UserConfig.addressId = " + address.getStr("id"));
                    } else {
                        System.out.println("地址id配置正确");
                    }
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("没有可用的默认收货地址，请自行登录叮咚设置该站点可用的默认收货地址");
    }


    /**
     * 全选按钮
     */
    public static void allCheck() {
        try {
            HttpRequest httpRequest = HttpUtil.createGet("https://maicai.api.ddxq.mobi/cart/allCheck");
            Map<String, String> headers = UserConfig.getHeaders();
            httpRequest.addHeaders(headers);
            Map<String, String> request = UserConfig.getBody(headers);
            request.put("is_check", "1");
            httpRequest.formStr(sign(request));

            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);

            if (!isSuccess(object, "勾选购物车全选按钮")) {
                return;
            }
            print(true, "勾选购物车全选按钮成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取购物车信息
     *
     * @param noProductsContinue 无商品是否继续
     * @return 购物车信息
     */
    public static Map<String, Object> getCart(boolean noProductsContinue) {
        try {
            HttpRequest httpRequest = HttpUtil.createGet("https://maicai.api.ddxq.mobi/cart/index");
            Map<String, String> headers = UserConfig.getHeaders();
            httpRequest.addHeaders(headers);
            Map<String, String> request = UserConfig.getBody(headers);
            request.put("is_load", "1");
            request.put("ab_config", "{\"key_onion\":\"D\",\"key_cart_discount_price\":\"C\"}");


            httpRequest.formStr(sign(request));

            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);

            if (!isSuccess(object, "更新购物车数据")) {
                return null;
            }

            JSONObject data = object.getJSONObject("data");

            if (data.getJSONArray("new_order_product_list").size() == 0) {
                print(false, "购物车无可买的商品");
                if (!noProductsContinue) {
                    context.put("end", new HashMap<>());
                }
                context.put("noProduct", new HashMap<>());
                return null;
            }
            JSONObject newOrderProduct = data.getJSONArray("new_order_product_list").getJSONObject(0);
            JSONArray products = newOrderProduct.getJSONArray("products");

            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < products.size(); i++) {
                JSONObject product = products.getJSONObject(i);
                product.set("total_money", product.get("total_price"));
                product.set("total_origin_money", product.get("total_origin_price"));
            }
            map.put("products", products);
            map.put("parent_order_sign", data.getJSONObject("parent_order_info").get("parent_order_sign"));
            map.put("total_money", newOrderProduct.get("total_money"));
            map.put("total_origin_money", newOrderProduct.get("total_origin_money"));
            map.put("goods_real_money", newOrderProduct.get("goods_real_money"));
            map.put("total_count", newOrderProduct.get("total_count"));
            map.put("cart_count", newOrderProduct.get("cart_count"));
            map.put("is_presale", newOrderProduct.get("is_presale"));
            map.put("instant_rebate_money", newOrderProduct.get("instant_rebate_money"));
            map.put("coupon_rebate_money", newOrderProduct.get("coupon_rebate_money"));
            map.put("total_rebate_money", newOrderProduct.get("total_rebate_money"));
            map.put("used_balance_money", newOrderProduct.get("used_balance_money"));
            map.put("can_used_balance_money", newOrderProduct.get("can_used_balance_money"));
            map.put("used_point_num", newOrderProduct.get("used_point_num"));
            map.put("used_point_money", newOrderProduct.get("used_point_money"));
            map.put("can_used_point_num", newOrderProduct.get("can_used_point_num"));
            map.put("can_used_point_money", newOrderProduct.get("can_used_point_money"));
            map.put("is_share_station", newOrderProduct.get("is_share_station"));
            map.put("only_today_products", newOrderProduct.get("only_today_products"));
            map.put("only_tomorrow_products", newOrderProduct.get("only_tomorrow_products"));
            map.put("package_type", newOrderProduct.get("package_type"));
            map.put("package_id", newOrderProduct.get("package_id"));
            map.put("front_package_text", newOrderProduct.get("front_package_text"));
            map.put("front_package_type", newOrderProduct.get("front_package_type"));
            map.put("front_package_stock_color", newOrderProduct.get("front_package_stock_color"));
            map.put("front_package_bg_color", newOrderProduct.get("front_package_bg_color"));
            print(true, "更新购物车数据成功,订单金额：" + newOrderProduct.get("total_money"));
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取配送信息
     *
     * @param addressId 配送地址id
     * @param cartMap   购物车信息
     * @return 配送信息
     */
    public static Map<String, Object> getMultiReserveTime(String addressId, Map<String, Object> cartMap) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://maicai.api.ddxq.mobi/order/getMultiReserveTime");
            Map<String, String> headers = UserConfig.getHeaders();
            httpRequest.addHeaders(headers);
            Map<String, Object> request = UserConfig.getBody(headers);
            request.put("address_id", addressId);
            request.put("products", "[" + JSONUtil.toJsonStr(cartMap.get("products")) + "]");
            request.put("group_config_id", "");
            request.put("isBridge", "false");
            httpRequest.form(sign(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "更新配送时间")) {
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            JSONArray times = object.getJSONArray("data").getJSONObject(0).getJSONArray("time").getJSONObject(0).getJSONArray("times");

            for (int i = 0; i < times.size(); i++) {
                JSONObject time = times.getJSONObject(i);
                if (time.getInt("disableType") == 0 && !time.getStr("select_msg").contains("尽快")) {
                    map.put("reserved_time_start", time.get("start_timestamp"));
                    map.put("reserved_time_end", time.get("end_timestamp"));
                    print(true, "更新配送时间成功");
                    return map;
                }
            }
            print(false, "无可选的配送时间");
            context.put("noReserve", new HashMap<>());
            context.remove("multiReserveTimeMap");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取订单确认信息
     *
     * @param addressId           配送地址id
     * @param cartMap             购物车信息
     * @param multiReserveTimeMap 配送信息
     * @return 订单确认信息
     */
    public static Map<String, Object> getCheckOrder(String addressId, Map<String, Object> cartMap, Map<String, Object> multiReserveTimeMap) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://maicai.api.ddxq.mobi/order/checkOrder");
            Map<String, String> headers = UserConfig.getHeaders();
            httpRequest.addHeaders(headers);
            Map<String, Object> request = UserConfig.getBody(headers);
            request.put("address_id", addressId);
            request.put("user_ticket_id", "default");
            request.put("freight_ticket_id", "default");
            request.put("is_use_point", "0");
            request.put("is_use_balance", "0");
            request.put("is_buy_vip", "0");
            request.put("coupons_id", "");
            request.put("is_buy_coupons", "0");
            request.put("check_order_type", "0");
            request.put("is_support_merge_payment", "1");
            request.put("showData", "true");
            request.put("showMsg", "false");

            List<Map<String, Object>> packages = new ArrayList<>();
            Map<String, Object> packagesMap = new HashMap<>();
            packagesMap.put("products", cartMap.get("products"));
            packagesMap.put("total_money", cartMap.get("total_money"));
            packagesMap.put("total_origin_money", cartMap.get("total_money"));
            packagesMap.put("goods_real_money", cartMap.get("goods_real_money"));
            packagesMap.put("total_count", cartMap.get("total_count"));
            packagesMap.put("cart_count", cartMap.get("cart_count"));
            packagesMap.put("is_presale", cartMap.get("is_presale"));
            packagesMap.put("instant_rebate_money", cartMap.get("instant_rebate_money"));
            packagesMap.put("coupon_rebate_money", cartMap.get("coupon_rebate_money"));
            packagesMap.put("total_rebate_money", cartMap.get("total_rebate_money"));
            packagesMap.put("used_balance_money", cartMap.get("used_balance_money"));
            packagesMap.put("can_used_balance_money", cartMap.get("can_used_balance_money"));
            packagesMap.put("used_point_num", cartMap.get("used_point_num"));
            packagesMap.put("used_point_money", cartMap.get("used_point_money"));
            packagesMap.put("can_used_point_num", cartMap.get("can_used_point_num"));
            packagesMap.put("can_used_point_money", cartMap.get("can_used_point_money"));
            packagesMap.put("is_share_station", cartMap.get("is_share_station"));
            packagesMap.put("only_today_products", cartMap.get("only_today_products"));
            packagesMap.put("only_tomorrow_products", cartMap.get("only_tomorrow_products"));
            packagesMap.put("package_type", cartMap.get("package_type"));
            packagesMap.put("package_id", cartMap.get("package_id"));
            packagesMap.put("front_package_text", cartMap.get("front_package_text"));
            packagesMap.put("front_package_type", cartMap.get("front_package_type"));
            packagesMap.put("front_package_stock_color", cartMap.get("front_package_stock_color"));
            packagesMap.put("front_package_bg_color", cartMap.get("front_package_bg_color"));

            packagesMap.put("reserved_time", ImmutableMap.of(
                    "reserved_time_start", multiReserveTimeMap.get("reserved_time_start"), "reserved_time_end", multiReserveTimeMap.get("reserved_time_end")
            ));
            packages.add(packagesMap);
            request.put("packages", JSONUtil.toJsonStr(packages));
            httpRequest.form(sign(request));

            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);

            if (!isSuccess(object, "更新订单确认信息")) {
                return null;
            }

            JSONObject data = object.getJSONObject("data");
            JSONObject order = data.getJSONObject("order");
            Map<String, Object> map = new HashMap<>();
            map.put("freight_discount_money", order.get("freight_discount_money"));
            map.put("freight_money", order.get("freight_money"));
            map.put("total_money", order.get("total_money"));
            map.put("freight_real_money", order.getJSONArray("freights").getJSONObject(0).getJSONObject("freight").get("freight_real_money"));
            map.put("user_ticket_id", order.getJSONObject("default_coupon").get("_id"));
            print(true, "更新订单确认信息成功");
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 提交订单
     *
     * @param addressId           配送地址id
     * @param cartMap             购物车信息
     * @param multiReserveTimeMap 配送信息
     * @param checkOrderMap       订单确认信息
     */
    public static boolean addNewOrder(String addressId, Map<String, Object> cartMap, Map<String, Object> multiReserveTimeMap, Map<String, Object> checkOrderMap) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://maicai.api.ddxq.mobi/order/addNewOrder");
            Map<String, String> headers = UserConfig.getHeaders();
            httpRequest.addHeaders(headers);
            Map<String, Object> request = UserConfig.getBody(headers);
            request.put("showMsg", "false");
            request.put("showData", "true");
            request.put("ab_config", "{\"key_onion\":\"C\"}");

            Map<String, Object> packageOrderMap = new HashMap<>();
            Map<String, Object> paymentOrderMap = new HashMap<>();
            Map<String, Object> packagesMap = new HashMap<>();
            packageOrderMap.put("payment_order", paymentOrderMap);
            packageOrderMap.put("packages", Collections.singletonList(packagesMap));
            paymentOrderMap.put("reserved_time_start", multiReserveTimeMap.get("reserved_time_start"));
            paymentOrderMap.put("reserved_time_end", multiReserveTimeMap.get("reserved_time_end"));
            paymentOrderMap.put("price", checkOrderMap.get("total_money"));
            paymentOrderMap.put("freight_discount_money", checkOrderMap.get("freight_discount_money"));
            paymentOrderMap.put("freight_money", checkOrderMap.get("freight_money"));
            paymentOrderMap.put("order_freight", checkOrderMap.get("freight_real_money"));
            paymentOrderMap.put("parent_order_sign", cartMap.get("parent_order_sign"));
            paymentOrderMap.put("product_type", 1);
            paymentOrderMap.put("address_id", addressId);
            paymentOrderMap.put("form_id", UUID.randomUUID().toString().replaceAll("-", ""));
            paymentOrderMap.put("receipt_without_sku", null);
            paymentOrderMap.put("pay_type", 6);
            paymentOrderMap.put("user_ticket_id", checkOrderMap.get("user_ticket_id"));
            paymentOrderMap.put("vip_money", "");
            paymentOrderMap.put("vip_buy_user_ticket_id", "");
            paymentOrderMap.put("coupons_money", "");
            paymentOrderMap.put("coupons_id", "");
            packagesMap.put("products", cartMap.get("products"));
            packagesMap.put("total_money", cartMap.get("total_money"));
            packagesMap.put("total_origin_money", cartMap.get("total_money"));
            packagesMap.put("goods_real_money", cartMap.get("goods_real_money"));
            packagesMap.put("total_count", cartMap.get("total_count"));
            packagesMap.put("cart_count", cartMap.get("cart_count"));
            packagesMap.put("is_presale", cartMap.get("is_presale"));
            packagesMap.put("instant_rebate_money", cartMap.get("instant_rebate_money"));
            packagesMap.put("coupon_rebate_money", cartMap.get("coupon_rebate_money"));
            packagesMap.put("total_rebate_money", cartMap.get("total_rebate_money"));
            packagesMap.put("used_balance_money", cartMap.get("used_balance_money"));
            packagesMap.put("can_used_balance_money", cartMap.get("can_used_balance_money"));
            packagesMap.put("used_point_num", cartMap.get("used_point_num"));
            packagesMap.put("used_point_money", cartMap.get("used_point_money"));
            packagesMap.put("can_used_point_num", cartMap.get("can_used_point_num"));
            packagesMap.put("can_used_point_money", cartMap.get("can_used_point_money"));
            packagesMap.put("is_share_station", cartMap.get("is_share_station"));
            packagesMap.put("only_today_products", cartMap.get("only_today_products"));
            packagesMap.put("only_tomorrow_products", cartMap.get("only_tomorrow_products"));
            packagesMap.put("package_type", cartMap.get("package_type"));
            packagesMap.put("package_id", cartMap.get("package_id"));
            packagesMap.put("front_package_text", cartMap.get("front_package_text"));
            packagesMap.put("front_package_type", cartMap.get("front_package_type"));
            packagesMap.put("front_package_stock_color", cartMap.get("front_package_stock_color"));
            packagesMap.put("front_package_bg_color", cartMap.get("front_package_bg_color"));
            packagesMap.put("eta_trace_id", "");
            packagesMap.put("reserved_time_start", multiReserveTimeMap.get("reserved_time_start"));
            packagesMap.put("reserved_time_end", multiReserveTimeMap.get("reserved_time_end"));
            packagesMap.put("soon_arrival", "");
            packagesMap.put("first_selected_big_time", 0);
            packagesMap.put("receipt_without_sku", 0);
            request.put("package_order", JSONUtil.toJsonStr(packageOrderMap));

            httpRequest.form(sign(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);

            if (!isSuccess(object, "提交订单失败,当前下单总金额：" + cartMap.get("total_money"))) {
                return false;
            }
            context.put("success", new HashMap<>());
            context.put("end", new HashMap<>());
            for (int i = 0; i < 10; i++) {
                System.out.println("恭喜你，已成功下单 当前下单总金额：" + cartMap.get("total_money"));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
