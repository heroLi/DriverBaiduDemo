package lx.com.drivergpsrecord;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.OnStartTraceListener;
import com.baidu.trace.OnStopTraceListener;
import com.baidu.trace.Trace;

/**
 * Created by 18834 on 2016/1/21.
 * Function：
 * Version:
 */
public class LocationService extends Service implements BDLocationListener {
  public LocationClient mLocationClient = null;

  private String LogTag = "DriverGps";

  private long traceServiceId = 109298;

  private LBSTraceClient client = null;

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  private Trace trace;

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    Notification notification = new Notification();
    PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
    Notification.Builder builder = new Notification.Builder(getApplicationContext());
    //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.));
    if (mLocationClient == null) {
      mLocationClient = new LocationClient(getApplicationContext());
      mLocationClient.registerLocationListener(this);
      initLocation();
      mLocationClient.requestNotifyLocation();
    }
    if (client == null) {
      //实例化轨迹服务客户端
      client = new LBSTraceClient(getApplicationContext());
      initTrace();
    }
    return super.onStartCommand(intent, flags, startId);
  }

  @Override public void onDestroy() {
    mLocationClient.stop();
    stopSrace();
    mLocationClient.unRegisterLocationListener(this);
    //stopForeground(true);
  }

  @Override public void onReceiveLocation(BDLocation location) {
    StringBuffer sb = new StringBuffer();
    if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
      sb.append("\nspeed : ");
      sb.append(location.getSpeed());// 单位：公里每小时
      sb.append("\nsatellite : ");
      sb.append(location.getSatelliteNumber());
      sb.append("\nheight : ");
      sb.append(location.getAltitude());// 单位：米
      sb.append("\ndirection : ");
      sb.append(location.getDirection());// 单位度
      sb.append("\naddr : ");
      sb.append(location.getAddrStr());
      sb.append("\ndescribe : ");
      sb.append("gps定位成功");
    } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
      sb.append("\naddr : ");
      sb.append(location.getAddrStr());
      //运营商信息
      sb.append("\noperationers : ");
      sb.append(location.getOperators());
      sb.append("\ndescribe : ");
      sb.append("网络定位成功");
    }
    Logger(sb.toString());
  }

  /**
   * 百度定位
   *
   * 高精度定位模式：这种定位模式下，会同时使用网络定位和GPS定位，优先返回最高精度的定位结果；
   * 低功耗定位模式：这种定位模式下，不会使用GPS，只会使用网络定位（Wi-Fi和基站定位）；
   * 仅用设备定位模式：这种定位模式下，不需要连接网络，只使用GPS进行定位，这种模式下不支持室内环境的定位。
   */
  private void initLocation() {
    LocationClientOption option = new LocationClientOption();
    option.setLocationMode(
        LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
    option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
    int span = 5000;
    option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
    option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
    option.setOpenGps(true);//可选，默认false,设置是否使用gps
    option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
    option.setIsNeedLocationDescribe(
        true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
    option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
    option.setIgnoreKillProcess(
        true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
    option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
    option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
    mLocationClient.setLocOption(option);
  }

  private void Logger(Object o) {
    Log.d(LogTag, o.toString());
  }

  private void initTrace() {

    //鹰眼服务ID
    long serviceId = traceServiceId;
    //entity标识
    String entityName = "00001";
    //轨迹服务类型（0 : 不上传位置数据，也不接收报警信息； 1 : 不上传位置数据，但接收报警信息；2 : 上传位置数据，且接收报警信息）
    int traceType = 2;
    //实例化轨迹服务
    trace = new Trace(getApplicationContext(), serviceId, entityName, traceType);
    //实例化开启轨迹服务回调接口
    OnStartTraceListener startTraceListener = new OnStartTraceListener() {
      //开启轨迹服务回调接口（arg0 : 消息编码，arg1 : 消息内容，详情查看类参考）
      @Override public void onTraceCallback(int arg0, String arg1) {
        Logger(arg0 + " message " + arg1);
      }

      //轨迹服务推送接口（用于接收服务端推送消息，arg0 : 消息类型，arg1 : 消息内容，详情查看类参考）
      @Override public void onTracePushCallback(byte arg0, String arg1) {
        Logger(arg0 + " message " + arg1);
      }
    };
    //开启轨迹服务
    client.startTrace(trace, startTraceListener);
  }

  private void stopSrace() {
    //实例化停止轨迹服务回调接口
    OnStopTraceListener stopTraceListener = new OnStopTraceListener() {
      // 轨迹服务停止成功
      @Override public void onStopTraceSuccess() {
        Logger("onStopTraceSuccess");
      }

      // 轨迹服务停止失败（arg0 : 错误编码，arg1 : 消息内容，详情查看类参考）
      @Override public void onStopTraceFailed(int arg0, String arg1) {
        Logger(arg0 + " message " + arg1);
      }
    };
    //停止轨迹服务
    client.stopTrace(trace, stopTraceListener);
  }
}
