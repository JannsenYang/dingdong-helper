import java.util.Map;

/**
 * 抢菜测试程序
 */
public class ApplicationTest {

    public static void main(String[] args) {
        if (UserConfig.addressId.length() == 0) {
            System.err.println("请先执行UserConfig获取配送地址id");
            return;
        }

        // 此为单次执行模式  用于在非高峰期测试下单  也必须满足3个前提条件  1.有收货地址  2.购物车有商品 3.能选择配送信息
        Api.allCheck();
        Map<String, Object> cartMap = Api.getCart(false);
        if (cartMap == null) {
            return;
        }
        Map<String, Object> multiReserveTimeMap = Api.getMultiReserveTime(UserConfig.addressId, cartMap);
        if (multiReserveTimeMap == null) {
            return;
        }
        Map<String, Object> checkOrderMap = Api.getCheckOrder(UserConfig.addressId, cartMap, multiReserveTimeMap);
        if (checkOrderMap == null) {
            return;
        }
        Api.addNewOrder(UserConfig.addressId, cartMap, multiReserveTimeMap, checkOrderMap);
    }
}


