# dingdong-helper
叮咚自动下单 自动将购物车能买的商品全部下单 只需自行编辑购物车和最后支付即可

### 步骤
1. 通过Charles(mac)、fiddler(windows)等抓包工具抓取微信中叮咚买菜小程序中的接口信息中的用户信息配置到UserConfig.java中，比如openId、userId，详情见下截图，此操作每个用户只需要做一次，如果不会抓包请自行学习。
2. 将需要买的菜自行通过APP放入购物车并勾选
3. 等待叮咚开放购买并运行该程序，该程序会自行更新购物车（新增或者无货）和配送时间
4. 等待程序结束，如果成功则自行打开叮咚买菜app-我的订单-待支付-点击支付

### 程序自动结束的几个条件
1. 购物车无可购买商品
2. 无配送时间可选
3. 下单成功


![请求头信息](https://github.com/JannsenYang/dingdong-helper/blob/5b72bee57c06c48174b639658ab94d765b744274/headers.jpeg)
![请求体信息](https://github.com/JannsenYang/dingdong-helper/blob/5b72bee57c06c48174b639658ab94d765b744274/body.jpeg)
