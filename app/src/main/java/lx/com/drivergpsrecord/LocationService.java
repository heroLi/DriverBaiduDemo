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

  private String LogTag = "DriverGps";

  private long traceServiceId = 109298;
  private LocationClient mLocationClient = null;

  private LBSTraceClient client = null;

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  private Trace trace;

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
    Notification.Builder builder = new Notification.Builder(getApplicationContext());
    builder.setSmallIcon(R.mipmap.ic_launcher, 0);
    builder.setContentText("一嗨司机测试程序");
    builder.setContentTitle("一嗨租车");
    builder.setPriority(Notification.PRIORITY_HIGH);
    builder.setContentIntent(pendingIntent);
    startForeground(11, builder.build());
    mLocationClient = DriverAppcation.getApplication().getLocationClient();
    mLocationClient.registerLocationListener(this);
    mLocationClient.start();

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
    stopForeground(true);
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

  @Override public void onReceiveLocation(BDLocation location) {
    StringBuffer sb = new StringBuffer();
    if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
      sb.append("speed : ");
      sb.append(location.getSpeed());// 单位：公里每小时
      sb.append("satellite : ");
      sb.append(location.getSatelliteNumber());
      sb.append("height : ");
      sb.append(location.getAltitude());// 单位：米
      sb.append("direction : ");
      sb.append(location.getDirection());// 单位度
      sb.append("addr : ");
      sb.append(location.getAddrStr());
      sb.append("describe : ");
      sb.append("gps定位成功");
    } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
      sb.append("addr : ");
      sb.append(location.getAddrStr());
      //运营商信息
      sb.append("operationers : ");
      sb.append(location.getOperators());
      sb.append("describe : ");
      sb.append("网络定位成功");
    }
    sb.append(location.getLocType());
    Logger(sb.toString());
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
