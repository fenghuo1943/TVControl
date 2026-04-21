# 🔍 蓝牙鼠标连接调试指南

## ✅ 已完成的修改

### 1. **BleHidService.kt** - 增强日志和设备信息追踪
- ✅ 添加 `deviceName` 变量存储设备名称
- ✅ 添加 `getConnectedDeviceAddress()` 方法获取已连接设备的 MAC 地址
- ✅ 添加 `getConnectedDeviceName()` 方法获取已连接设备的名称
- ✅ 增强连接/断开连接的日志输出，显示详细信息
- ✅ 启动广播时记录设备名称

### 2. **BluetoothMouseScreen.kt** - UI 显示连接详情
- ✅ 连接时显示 MAC 地址
- ✅ 连接时显示设备名称
- ✅ 未连接时显示配对提示

### 3. **BluetoothMouseActivity.kt** - 自动管理蓝牙名称
- ✅ 进入界面时自动设置蓝牙名称为 "TVControl Mouse"
- ✅ 退出界面时恢复原始蓝牙名称
- ✅ 在 onResume 时确保名称正确

---

## 📊 如何查看连接的设备信息

### 方法一：通过 UI 查看（最简单）

1. 打开应用，进入"🔵 蓝牙鼠标"界面
2. 当有设备连接时，状态卡片会显示：
   ```
   ✓ 已连接
   MAC: XX:XX:XX:XX:XX:XX
   设备: XXXX
   ```
3. 对比这个 MAC 地址是否是你的电脑

### 方法二：通过 Logcat 查看（最详细）

```bash
adb logcat | grep BleHidService
```

**连接时的日志示例：**
```
D/BleHidService: ✅ 设备已连接
D/BleHidService:    - 名称: DESKTOP-XXXX
D/BleHidService:    - MAC: AA:BB:CC:DD:EE:FF
D/BleHidService:    - 状态码: 0
```

**断开时的日志示例：**
```
D/BleHidService: ❌ 设备已断开
D/BleHidService:    - 名称: DESKTOP-XXXX
D/BleHidService:    - MAC: AA:BB:CC:DD:EE:FF
```

**启动广播时的日志：**
```
D/BleHidService: BLE 广播启动成功，设备名称: TVControl Mouse
```

---

## 🔧 排查步骤

### 问题：显示的 MAC 地址不是我的电脑

#### 步骤 1：确认电脑的蓝牙 MAC 地址

**Windows:**
1. 打开"设置" → "蓝牙和其他设备"
2. 点击"更多蓝牙选项"
3. 切换到"硬件"选项卡
4. 查看"地址"字段

**或者使用命令提示符：**
```cmd
ipconfig /all
```
查找"蓝牙网络连接"部分的"物理地址"

**macOS:**
1. 点击苹果菜单 → "关于本机"
2. 点击"系统报告"
3. 选择左侧的"蓝牙"
4. 查看"地址"字段

**Linux:**
```bash
hciconfig
# 或
bluetoothctl show
```

#### 步骤 2：对比 MAC 地址

将电脑上查到的 MAC 地址与应用中显示的对比：
- ✅ 如果一致：说明连接的是你的电脑
- ❌ 如果不一致：说明连接了其他设备

#### 步骤 3：清除所有配对记录

**在手机上：**
1. 打开"设置" → "蓝牙"
2. 找到所有已配对的设备
3. 逐个取消配对或删除

**在电脑上：**
1. 打开"设置" → "蓝牙和其他设备"
2. 找到 "TVControl Mouse" 或你的手机名称
3. 点击"删除设备"

#### 步骤 4：重启蓝牙

**手机：**
```bash
# 方法 1：通过设置
设置 → 蓝牙 → 关闭 → 等待 5 秒 → 开启

# 方法 2：通过 ADB
adb shell svc bluetooth disable
sleep 5
adb shell svc bluetooth enable
```

**电脑：**
- Windows: 设置 → 蓝牙 → 关闭 → 开启
- macOS: 系统偏好设置 → 蓝牙 → 关闭 → 开启
- Linux: `sudo systemctl restart bluetooth`

#### 步骤 5：重新配对

1. 确保只有目标电脑在范围内
2. 打开手机应用，进入蓝牙鼠标界面
3. 在电脑上搜索新设备
4. 应该看到 "TVControl Mouse"
5. 点击配对
6. 观察手机界面显示的 MAC 地址

---

## 🐛 常见问题

### Q1: 为什么显示已连接，但我没有操作电脑？

**可能原因：**
1. 之前配对过的其他设备自动重连（平板、另一台电脑等）
2. 测试时留下的配对记录

**解决方法：**
- 查看 UI 或 Logcat 中的 MAC 地址
- 确认是哪个设备
- 如果不是目标设备，清除配对记录后重新配对

### Q2: 多个设备同时连接怎么办？

**当前实现：**
- BLE GATT 服务器只记录最后一个连接的设备
- 如果有多个设备连接，只会显示最后一个

**改进方案（可选）：**
如果需要支持多设备管理，可以修改代码维护一个设备列表。

### Q3: 如何只允许特定设备连接？

可以在 `onConnectionStateChange` 中添加白名单检查：

```kotlin
override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
    if (newState == BluetoothProfile.STATE_CONNECTED) {
        val allowedMac = "AA:BB:CC:DD:EE:FF" // 你电脑的 MAC 地址
        if (device?.address != allowedMac) {
            Log.w(TAG, "拒绝未授权设备: ${device?.address}")
            gattServer?.cancelConnection(device)
            return
        }
    }
    // ... 原有代码
}
```

### Q4: 蓝牙名称没有改变？

**检查清单：**
1. 确认已授予蓝牙权限
2. 查看 Logcat 是否有 "设置蓝牙名称失败" 的错误
3. 某些手机系统可能限制修改蓝牙名称
4. 尝试手动在设置中修改蓝牙名称

---

## 📝 日志关键字速查

| 日志内容 | 含义 |
|---------|------|
| `BLE 广播启动成功` | HID 服务已启动，可以被发现 |
| `✅ 设备已连接` | 有设备成功配对并连接 |
| `❌ 设备已断开` | 设备断开连接 |
| `描述符写入` | 客户端启用了通知功能 |
| `GATT 服务器设置完成` | GATT 服务初始化成功 |

---

## 💡 最佳实践

1. **首次使用前**：清除所有旧的配对记录
2. **每次测试前**：查看 Logcat 确认连接的设备
3. **生产环境**：考虑添加设备白名单功能
4. **调试时**：始终记录 MAC 地址以便追踪

---

## 🎯 验证清单

使用此清单确认功能正常：

- [ ] 进入蓝牙鼠标界面时，蓝牙名称变为 "TVControl Mouse"
- [ ] 电脑能搜索到 "TVControl Mouse" 设备
- [ ] 配对成功后，手机显示 "✓ 已连接"
- [ ] UI 显示的 MAC 地址与电脑的一致
- [ ] Logcat 显示正确的设备名称和 MAC 地址
- [ ] 移动触摸板，电脑光标跟随移动
- [ ] 退出界面后，蓝牙名称恢复原样
- [ ] 退出后，设备自动断开连接

---

**最后更新**: 2026-03-31  
**版本**: 1.1
