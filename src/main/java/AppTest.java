import java.util.Map;

public class AppTest {

    public static void main(String[] args) {
        // 此为单次执行模式  用于在非高峰期测试下单  也必须满足3个前提条件  1.有收货地址  2.购物车有商品 3.能选择配送信息
        Api.allCheck();
        Map<String, Object> cartMap = Api.getCart();
        Map<String, Object> multiReserveTimeMap = Api.getMultiReserveTime(UserConfig.addressId, cartMap);
        Map<String, Object> checkOrderMap = Api.getCheckOrder(UserConfig.addressId, cartMap, multiReserveTimeMap);
        Api.addNewOrder(UserConfig.addressId, cartMap, multiReserveTimeMap, checkOrderMap);
    }
}
