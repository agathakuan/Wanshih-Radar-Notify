# Wanshih-Radar-Notify
Wanshih Doppler Radar with BLE detect &amp; send system background notification

## application structure
### Activities:
 - Main Activity: 
 * user set system notification setting/channel setting.
 * Start BLE scan, then switch to Device scan Activity.
 
 - DeviceScanActivity:
 * List BLE device
 
 - Radar Data Activity:
show Connect/Disconnect status,Device BLE address,time Graph,
 **User setting warning speed thershold**
### Services:
### System-permission Helper
