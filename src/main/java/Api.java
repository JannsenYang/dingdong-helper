import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.ImmutableMap;

import java.util.*;

/**
 * 接口封装
 */
public class Api {

    /**
     * 获取有效的默认收货地址id
     *
     * @return 收货地址id
     */
    public static String getAddressId() {
        boolean noAddress = false;
        try {
            System.out.println("开始获取收货人信息");
            HttpRequest httpRequest = HttpUtil.createGet("https://sunquan.api.ddxq.mobi/api/v1/user/address/");
            httpRequest.addHeaders(UserConfig.getHeaders());
            httpRequest.formStr(UserConfig.getBody());

            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            boolean success = object.getBool("success");
            if (!success) {
                if("您的访问已过期".equals(object.getStr("message"))){
                    System.err.println("用户信息失效，请确保UserConfig参数准确，并且微信上的叮咚小程序不能退出登录");
                    Application.map.put("end", new HashMap<>());
                    return null;
                }
                System.err.println("获取默认收货地址失败");
                return null;
            }
            JSONArray validAddress = object.getJSONObject("data").getJSONArray("valid_address");
            System.out.println("获取可用的收货地址条数：" + validAddress.size());
            for (int i = 0; i < validAddress.size(); i++) {
                JSONObject address = validAddress.getJSONObject(i);
                if (address.getBool("is_default")) {
                    System.out.println("获取默认收货地址成功：" + address.getStr("addr_detail") + " 手机号：" + address.getStr("mobile"));
                    return address.getStr("id");
                }
            }
            noAddress = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (noAddress) {
            System.err.println("没有可用的默认收货地址，请自行登录叮咚设置该站点可用的默认收货地址");
            Application.map.put("end", new HashMap<>());
        }
        return null;
    }


    /**
     * 获取购物车信息
     *
     * @return 购物车信息
     */
    public static Map<String, Object> getCart() {
        boolean noProducts = false;
        try {
            HttpRequest httpRequest = HttpUtil.createGet("https://maicai.api.ddxq.mobi/cart/index");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, String> request = UserConfig.getBody();
            request.put("is_load", "1");
            httpRequest.formStr(request);

            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            boolean success = object.getBool("success");
            if (!success) {
                if("您的访问已过期".equals(object.getStr("message"))){
                    System.err.println("用户信息失效，请确保UserConfig参数准确，并且微信上的叮咚小程序不能退出登录");
                    Application.map.put("end", new HashMap<>());
                    return null;
                }
                System.err.println("更新购物车数据失败");
                return null;
            }
            JSONObject data = object.getJSONObject("data");

            if (data.getJSONArray("new_order_product_list").size() == 0) {
                noProducts = true;
            } else {
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
                map.put("total_origin_money", newOrderProduct.get("parent_order_info"));
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
                System.out.println("更新购物车数据成功");
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (noProducts) {
            System.err.println("购物车无可买的商品");
            Application.map.put("end", new HashMap<>());
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
        boolean noReserveTime = false;
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://maicai.api.ddxq.mobi/order/getMultiReserveTime");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getBody();
            request.put("addressId", addressId);
            request.put("products", "[" + JSONUtil.toJsonStr(cartMap.get("products")) + "]");
            request.put("group_config_id", "");
            request.put("isBridge", "false");
            httpRequest.form(request);
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            boolean success = object.getBool("success");
            if (!success) {
                if("您的访问已过期".equals(object.getStr("message"))){
                    System.err.println("用户信息失效，请确保UserConfig参数准确，并且微信上的叮咚小程序不能退出登录");
                    Application.map.put("end", new HashMap<>());
                    return null;
                }
                System.err.println("更新配送时间失败");
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            JSONArray times = object.getJSONArray("data").getJSONObject(0).getJSONArray("time").getJSONObject(0).getJSONArray("times");
            for (int i = 0; i < times.size(); i++) {
                JSONObject time = times.getJSONObject(i);
                if (time.getInt("disableType") == 0) {
                    map.put("reserved_time_start", time.get("start_timestamp"));
                    map.put("reserved_time_end", time.get("end_timestamp"));
                    System.out.println("更新配送时间成功");
                    return map;
                }
            }
            noReserveTime = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (noReserveTime) {
            System.err.println("无可选的配送时间");
            Application.map.remove("multiReserveTimeMap");
            //此处不停止程序 可在开放之前提前执行
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
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getBody();
            request.put("addressId", addressId);
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
            httpRequest.form(request);

            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            boolean success = object.getBool("success");
            if (!success) {
                if("您的访问已过期".equals(object.getStr("message"))){
                    System.err.println("用户信息失效，请确保UserConfig参数准确，并且微信上的叮咚小程序不能退出登录");
                    Application.map.put("end", new HashMap<>());
                    return null;
                }
                System.err.println("更新订单确认信息失败");
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
            System.out.println("更新订单确认信息成功");
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
    public static void addNewOrder(String addressId, Map<String, Object> cartMap, Map<String, Object> multiReserveTimeMap, Map<String, Object> checkOrderMap) {
        boolean submitSuccess = false;
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://maicai.api.ddxq.mobi/order/addNewOrder");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getBody();
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
            packagesMap.put("first_selected_big_time", 1);
            request.put("package_order", JSONUtil.toJsonStr(packageOrderMap));

            httpRequest.form(request);
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            boolean success = object.getBool("success");
            if (!success) {
                if("您的访问已过期".equals(object.getStr("message"))){
                    System.err.println("用户信息失效，请确保UserConfig参数准确，并且微信上的叮咚小程序不能退出登录");
                    Application.map.put("end", new HashMap<>());
                    return;
                }
                System.err.println("提交订单失败");
                return;
            }
            submitSuccess = object.getJSONObject("data").getStr("pay_url").length() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (submitSuccess) {
            System.out.println("恭喜你，已成功下单");
            Application.map.put("end", new HashMap<>());
        }
    }

}
