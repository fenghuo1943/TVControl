# 🔵 蓝牙鼠标功能实现说明

## 📋 功能概述

已成功为 TVControl 应用添加了蓝牙 HID 鼠标功能，允许将手机直接作为蓝牙鼠标使用，无需在目标设备上安装任何软件。

## ✨ 新增文件

### 1. 核心服务层
- **`BleHidService.kt`** - BLE HID 服务实现
  - 位置: `app/src/main/java/com/fenghuo1943/tvcontrol/bluetooth/`
  - 功能: 实现 Bluetooth Low Energy HID Profile
  - 特性:
    - GATT 服务器管理
    - BLE 广播
    - HID 报告描述符（标准鼠标）
    - 连接状态管理

### 2. UI 层
- **`BluetoothMouseActivity.kt`** - 蓝牙鼠标活动
  - 位置: `app/src/main/java/com/fenghuo1943/tvcontrol/`
  - 功能: 权限检查、蓝牙启用、界面展示
  
- **`BluetoothMouseScreen.kt`** - 蓝牙鼠标界面
  - 位置: `app/src/main/java/com/fenghuo1943/tvcontrol/ui/`
  - 功能: 
    - 触摸板区域（手势识别）
    - 鼠标按键（左/中/右键）
    - 连接状态显示

### 3. 协议层
- **`BluetoothMouseActionsImpl.kt`** - 蓝牙鼠标动作实现
  - 位置: `app/src/main/java/com/fenghuo1943/tvcontrol/protocol/`
  - 功能: 将手势转换为 BLE HID 报告

### 4. 配置文件
- **`AndroidManifest.xml`** - 已更新
  - 添加蓝牙权限
  - 注册 BluetoothMouseActivity

- **`MainScreen.kt`** - 已更新
  - 在功能列表中添加"🔵 蓝牙鼠标"入口

## 🚀 使用方法

### 1. 启动蓝牙鼠标模式
1. 打开 TVControl 应用
2. 在主界面找到"🔵 蓝牙鼠标"卡片（在"更多"后面）
3. 点击进入蓝牙鼠标界面

### 2. 配对连接
1. 确保手机蓝牙已开启
2. 在目标设备（电脑/平板/电视）上打开蓝牙设置
3. 搜索新设备
4. 找到 "TVControl" 或类似名称的设备
5. 点击配对

### 3. 开始使用
- **移动光标**: 在触摸板区域滑动手指
- **左键点击**: 点击"左键"按钮或轻触触摸板
- **右键点击**: 点击"右键"按钮
- **中键点击**: 点击"中键"按钮
- **滚动**: 双指上下滑动

### 4. 断开连接
- 点击"返回"按钮退出蓝牙鼠标模式
- 或在目标设备上取消配对

## 📱 系统要求

- **Android 版本**: Android 9.0 (API 28) 及以上
- **硬件要求**: 支持 Bluetooth Low Energy (BLE)
- **权限**:
  - BLUETOOTH_CONNECT (Android 12+)
  - BLUETOOTH_ADVERTISE (Android 12+)
  - 蓝牙适配器访问

## 🔧 技术细节

### HID 报告格式
```
字节 0: Report ID (0x01)
字节 1: 按钮状态 (bit0=左键, bit1=右键, bit2=中键)
字节 2-3: X 轴位移 (16位有符号整数，小端序)
字节 4-5: Y 轴位移 (16位有符号整数，小端序)
字节 6: 滚轮位移 (8位有符号整数)
```

### 支持的 HID 功能
- ✅ 鼠标移动（相对坐标）
- ✅ 左/右/中键点击
- ✅ 垂直滚动
- ❌ 水平滚动（标准 HID 鼠标报告不支持）
- ❌ 键盘输入（需要单独的 HID Keyboard Profile）

### 性能指标
- **延迟**: ~5-20ms（取决于设备和距离）
- **有效距离**: ~10米（无障碍物）
- **功耗**: 低（BLE 优化）

## ⚠️ 注意事项

1. **首次使用**: 需要在目标设备上手动配对
2. **兼容性**: 大多数现代设备支持 BLE HID，但某些旧设备可能不兼容
3. **多设备**: 一次只能连接一个设备
4. **后台运行**: 退出界面后服务会停止，不会在后台持续广播
5. **键盘功能**: 当前版本仅支持鼠标，键盘功能需要额外实现

## 🐛 故障排除

### 问题1: 找不到设备
- 确保手机蓝牙已开启
- 确保目标设备支持 BLE
- 尝试重启两个设备的蓝牙
- 检查应用是否有蓝牙权限

### 问题2: 配对失败
- 确保没有其他设备正在连接
- 尝试取消配对后重新配对
- 检查目标设备是否已达到最大蓝牙连接数

### 问题3: 光标不动
- 确认已成功配对（界面显示"✓ 已连接"）
- 尝试重新进入蓝牙鼠标模式
- 检查目标设备是否正确识别为鼠标

### 问题4: 应用崩溃
- 检查 Android 版本是否为 9.0+
- 查看 Logcat 获取详细错误信息
- 确保已授予所有必要权限

## 🔮 未来改进方向

1. **键盘支持**: 实现 HID Keyboard Profile
2. **多媒体控制**: 音量、播放/暂停等
3. **自定义报告**: 支持水平滚动
4. **电量显示**: 读取目标设备电池状态
5. **自动重连**: 断线后自动重新广播
6. **多点触控**: 更复杂的手势支持
7. **DPI 调节**: 可调节鼠标灵敏度

## 📚 参考资料

- [Bluetooth HID over GATT Specification](https://www.bluetooth.com/specifications/specs/hid-over-gatt-profile-1-0/)
- [HID Usage Tables](https://usb.org/sites/default/files/hut1_2.pdf)
- [Android BLE Developer Guide](https://developer.android.com/guide/topics/connectivity/bluetooth-le)

## 💡 提示

- 在游戏场景中，WiFi 模式可能延迟更低
- 在日常办公中，蓝牙模式更方便（无需安装软件）
- 可以根据使用场景灵活切换两种模式

---

**开发者**: TVControl Team  
**版本**: 1.0  
**更新日期**: 2026-03-31
