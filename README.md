## AndroidBleTools

一款基于 TTC_BLE_DEMO 的调试工具。

### docs

[doc](#)

### Libs 文件

[libs](#)

### Q & A

[Android BLE开发常见问题及解决方法](#)

### 升级日志

```reStructuredText
v1.0.2
针对通用UUID的数据交互修改API，并可以设置是否加密/解密通信数据。

v1.0.3
BleService类增加函数setAdvMfr(mac, mfrData)，用来设置从机广播数据中的厂商信息，并可以通过readRed(mac, BleRegConstants.REG_ADV_MFR_SPC)读取当前的厂商信息。

v1.0.4
BleService类增加函数setMaxConnectedNumber(max)，用来设置可连接的从机广数。

v1.0.5
1、同步了对List<BluetoothGatt>实例的访问；
2、BleService增加批量连接的函数connect(macList)；
3、可以读取模组的实时时钟寄存器时间；
4、开放密码寄存器。

v1.0.6
1.BleCallBack增加回调方法：onConnectionError()，onNotifyStateRead()；
2.BleService增加方法：readNotifyState()(2个同名方法)。

v1.0.7
1、增加LeScanRecord，GattAttributes类；
2、包名更改为com.ble.api；
3、去掉setCharacteristicNotification()方法中的延时读取notify状态的动作。

v1.0.8
1、支持2541 OAD；
2、BleService增加方法：addBleCallBack()，removeBleCallBack()；
3、DataUtil增加方法：loUint16()，hiUint16()，buildUint16()，splitData()；
4、重新编译 hy_api.so，支持多种架构的cpu。

v1.0.9
1、发送数据时根据特征属性以相应的方式写入数据；
2、BleCallBack增加onCharacteristicWrite()、onDescriptorRead()两个方法；
3、OADListener增加onBlockWrite()方法；
4、BleService增加refresh()方法；
5、oad包下增加OADType、OADManager、OADProxy3个类，支持4种OAD方式：
	2541 OAD、
	2541 Large Image OAD、
	2640 On-Chip OAD、
	2640 Off-Chip OAD。

v1.1.0
修复部分手机2541 OAD不成功的问题。

v1.1.1
1、连线后不去打开0x1002的notify；
2、GattAttributes增加以下几个UUID：
TI_OAD_Image_Status、TI_Reset_Service、TI_Reset、TI_OAD_Image_Status、
Model_Number、Firmware_Revision、Manufacturer_Name。
3、支持cc2640_r2_oad升级；
4、增加CC2541模块连接适配接口：AdaptionUtil类的writeAdaptionConfigs()方法。

v1.1.2
1、BleCallBack增加onMtuChanged()方法；
2、BleService增加requestMtu()、requestConnectionPriority()方法，这两个方法须手机系统为android5.0及以上版本；
3、取消断线重连时如果重连列表为空则立即撤消连接，防止重连的设备断电或者不在附近时无法快速连接新设备；
4、修复BUG：从机广播中厂商数据为1个字节时无法解析广播数据；

v1.1.3
1、支持CC2640 R1 OAD（底层SDK2.0）；
2、修复无法更改透传模块名称的问题；
3、修复部分手机获取的连线状态不准确的问题；
4、修复部分手机关闭蓝牙不触发onDisconnected()的问题；
5、调用BleService的disconnect()方法一并撤销断线重连；
6、BleService增加disconnectAll()方法，断开所有的连接并撤销断线重连；
7、BleService增加4个同名方法queueSend()，以队列形式发送数据，发送间隔跟连接间隔有关，最小发送间隔100ms；
8、BleService增加readDeviceInfo()方法，可读取模组型号、序列号、硬件版本、固件版本、软件版本、厂家名称。

v1.1.4
1、修复多次调用queueSend()发送不同数据时，只发送最后一笔数据的问题；
2、系统蓝牙关闭后，停止所有的RSSI读取任务；
3、修复正在连接设备时调用disconnect()方法无法再次连接的问题；
4、针对部分手机扫到的广播数据跟扫描回应数据分开的情况优化LeScanRecord的解析，LeScanRecor增加getFirstManufacturerSpecificData()方法；
5、增加com.ble.utils包。

v1.1.5
1、避免绑定BleService时重复加载BleCallBack回调；
2、OAD升级时连续100次发送失败停止升级后重置失败次数，避免无法重新升级；
3、BleService增加setConnectInQueue()方法，支持并发连接；
4、CC2640 R2 OAD增加升级状态回调，OADListener增加onStatusChange()回调；
5、DeviceInfo增加Device_Name；
6、优化133问题。
```

