# Wanshih-Radar-Notify
Wanshih Doppler Radar with BLE detect &amp; send system background notification
## application structure
### Activities:
#### Main Activity: 
 * user set system notification setting/channel setting.
 * Start BLE scan, then switch to Device scan Activity. 
#### DeviceScanActivity:
 * List BLE device 
#### Radar Data Activity:
* show Connect/Disconnect status,Device BLE address,time Graph,
* **User setting warning speed thershold**
### Services:
#### BluetoothLe service
* remain connection and notify BLE GATT characteristic value changed
#### Notificatiopn service
* send notification in background execution Service type, *will not intertupt by activity onPause*
### System-permission Helper
#### Notification Helper
* extra setting of Android Notification/channel setting.
* send a notificaiton with system date time stamp.

### Broadcast system
* ble receive characteristic changed send broadcast to RadarData activity to update UI/graph.
* User update speed thershold at Radar Data activity.If changed, send broadcast to Notification service. 
* ble receive characteristic changed send broadcast to Notification service to check if over speed thershold
```
Give an example
- ble service has no broadcast receiver
- radar data activity has ble service broadcast receiver
- notification has broadcast receiver to filter radar data activity & ble service
```
