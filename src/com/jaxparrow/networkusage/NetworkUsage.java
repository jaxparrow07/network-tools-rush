package com.jaxparrow.networkusage;


import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.YailList;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.provider.Settings;
import android.telephony.TelephonyManager;

import android.net.TrafficStats;
import android.net.ConnectivityManager;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;


import java.lang.System;
import java.lang.Thread;


public class NetworkUsage extends AndroidNonvisibleComponent {

    private Context mContext;
    private Activity mActivity;
    public TrafficSpeedMeasurer mTrafficSpeedMeasurer;
    private static boolean SHOW_SPEED_IN_BITS = false;
    private static boolean FORMATTED_TRAFFIC = true;


    public NetworkUsage(ComponentContainer container) {
      super(container.$form());
      this.mContext = container.$context();
      this.mActivity = (Activity) this.mContext;
    }

    @SimpleProperty(description = "Data type : Wifi")
    public int DataWifi(){
      return 2;
    }
    @SimpleProperty(description = "Data type : Mobile")
    public int DataMobile(){
      return 1;
    }
    @SimpleProperty(description = "Data type : ( Wifi, Mobile )")
    public int DataAll(){
      return 0;
    }


    @SimpleProperty(description = "[Set State] Return in Bits instead of Bytes")
    public void ReturnBits(boolean bool){
      this.SHOW_SPEED_IN_BITS = bool;
    }
    @SimpleProperty(description = "[Get State] Return in Bits instead of Bytes")
    public boolean ReturnBits(){
      return SHOW_SPEED_IN_BITS;
    }


    @SimpleProperty(description = "[Set State] Human Readable Output - Kb,Mb,Gb")
    public void FormattedTraffic(boolean bool){
      this.FORMATTED_TRAFFIC = bool;
    }
    @SimpleProperty(description = "[Get State] Human Readable Output - Kb,Mb,Gb")
    public boolean FormattedTraffic(){
      return FORMATTED_TRAFFIC;
    }



    @SimpleFunction(description = "Get Uploaded Data in Bytes by type. Set the formatted to get formatted output like Kbps,Mbps. You can also use bits by setting the property.")
    public String GetUploaded(int type, boolean formatted) {

      long getbytes;

      if (type == 1) {

        getbytes = TrafficStats.getMobileTxBytes();

      } else if (type == 2) {

        getbytes = TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes();

      } else {

        getbytes = TrafficStats.getTotalTxBytes();

      }

      long val = SHOW_SPEED_IN_BITS ? getbytes * 8 : getbytes;

      return formatted ? Utils.parseUsage( (double) getbytes, SHOW_SPEED_IN_BITS ) : String.valueOf(val);

    }
    @SimpleFunction(description = "Get Downloaded Data in Bytes by type.You can also get bits by setting the property.")
    public String GetDownloaded(int type, boolean formatted) {

      long getbytes;

      if (type == 1) {

        getbytes = TrafficStats.getMobileRxBytes();

      } else if (type == 2) {

        getbytes = TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes();

      } else {

        getbytes = TrafficStats.getTotalRxBytes();

      }

      long val = SHOW_SPEED_IN_BITS ? getbytes * 8 : getbytes;

      return formatted ? Utils.parseUsage( (double) getbytes , SHOW_SPEED_IN_BITS ) : String.valueOf(val);

    }

    @SimpleFunction(description = "Get Upload Packets. Use the type property to get specific information.")
    public String GetUploadPackets(int type) {

      long getpacks;

      if (type == 1) {

        getpacks = TrafficStats.getMobileTxPackets();

      } else if (type == 2) {

        getpacks = TrafficStats.getTotalTxPackets() - TrafficStats.getMobileTxPackets();

      } else {

        getpacks = TrafficStats.getTotalTxPackets();

      }

      return String.valueOf(getpacks);

    }
    @SimpleFunction(description = "Get Download Packets. Use the type property to get specific information.")
    public String GetDownloadPackets(int type) {

      long getpacks;

      if (type == 1) {

        getpacks = TrafficStats.getMobileRxPackets();

      } else if (type == 2) {

        getpacks = TrafficStats.getTotalRxPackets() - TrafficStats.getMobileRxPackets();

      } else {

        getpacks = TrafficStats.getTotalRxPackets();

      }

      return String.valueOf(getpacks);

    }


    @SimpleFunction(description = "Convert bytes into readable data ( Supports bits too, eg. will be converted to Megabits intead of Megabytes.")
    public String FormatBytes(double bytes, boolean useBits) {
      return Utils.parseUsage(bytes,useBits);
    }


    // Get by Package

    // Permission
    @SimpleFunction(description = "Check Usage Access Permission")
    public boolean isPermissionGranted() {

      AppOpsManager appOps = (AppOpsManager) mActivity.getSystemService(Context.APP_OPS_SERVICE);
      int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), mActivity.getPackageName());

      return (mode == AppOpsManager.MODE_ALLOWED);

    }
    @SimpleFunction(description = "Open Usage Access Permission Screen")
    public void OpenUsageAccessSettings() {

      mActivity.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));

    }

    // For android versions below Marshmallow ( Api 23 )
    private void PackageStatsOld(int uid, String packageName, boolean formatted) {

      long up_bytes = SHOW_SPEED_IN_BITS ? TrafficStats.getUidTxBytes(uid) * 8 : TrafficStats.getUidTxBytes(uid);
      String upload = formatted ? Utils.parseUsage( (double) up_bytes , SHOW_SPEED_IN_BITS ) : String.valueOf(up_bytes);

      long down_bytes = SHOW_SPEED_IN_BITS ? TrafficStats.getUidRxBytes(uid) * 8 : TrafficStats.getUidRxBytes(uid);
      String download = formatted ? Utils.parseUsage( (double) down_bytes , SHOW_SPEED_IN_BITS ) : String.valueOf(down_bytes);

      String upload_packets = String.valueOf(TrafficStats.getUidTxPackets(uid));
      String download_packets = String.valueOf(TrafficStats.getUidRxPackets(uid)); 

      GotPackageStats(packageName, upload, download, upload_packets, download_packets);

    }


    // For android versions above Marshmallow ( Api 23 )
    private void PackageStatsNew(int uid, String packageName, boolean formatted, int type) {

      long up_bytes = 0L;
      long down_bytes = 0L;

      long up_packets = 0L;
      long down_packets = 0L;

      boolean isMobile = (type == 1) ? true : false;
      boolean isAndroidNew = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q);

      TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE); 
      
      NetworkStatsManager networkStatsManager = (NetworkStatsManager) mContext.getSystemService(Context.NETWORK_STATS_SERVICE);

      NetworkStats networkStats = networkStatsManager.queryDetailsForUid( isMobile ? ConnectivityManager.TYPE_MOBILE : ConnectivityManager.TYPE_WIFI , isMobile || !isAndroidNew ? telephonyManager.getSubscriberId() : null , 0, System.currentTimeMillis(), uid);
      
      NetworkStats.Bucket bucket = new NetworkStats.Bucket();

      while (networkStats.hasNextBucket()) {     

           networkStats.getNextBucket(bucket);

           up_bytes += bucket.getTxBytes();
           down_bytes += bucket.getRxBytes();

           up_packets += bucket.getTxPackets();
           down_packets += bucket.getRxPackets();

      }

      networkStats.close();

      String upload = formatted ? Utils.parseUsage( (double) up_bytes , SHOW_SPEED_IN_BITS ) : String.valueOf(up_bytes);
      String download = formatted ? Utils.parseUsage( (double) down_bytes , SHOW_SPEED_IN_BITS ) : String.valueOf(down_bytes);

      String upload_packets = String.valueOf(up_packets);
      String download_packets = String.valueOf(down_packets);

      GotPackageStats(packageName, upload, download, upload_packets, download_packets);

    }


    @SimpleFunction(description = "Get informaton by packagename")
    public void GetPackageStats(String packageName, boolean formatted, int type) {

      mActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {

          try {

            int app_uid = mContext.getPackageManager().getApplicationInfo(packageName, 0).uid;

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
              PackageStatsOld(app_uid, packageName, formatted);
            } else {
              PackageStatsNew(app_uid, packageName, formatted, type);
            }

          } catch (PackageManager.NameNotFoundException e) {
            PackageNotFound(packageName);
          }

        }
      });


    }


    @SimpleEvent(description = "Informaton by packagename")
    public void GotPackageStats(String packageName,String upload, String download, String uploadPackets, String downloadPackets){
       EventDispatcher.dispatchEvent(this,"GotPackageStats", packageName, upload, download, uploadPackets, downloadPackets);
    }

    @SimpleEvent(description = "Fired when provided package isn't found")
    public void PackageNotFound(String packageName){
       EventDispatcher.dispatchEvent(this,"PackageNotFound", packageName);
    }


    // Traffic Measuring
    private void TrafChange(String upload,String download, String upload_b,String download_b) {
      if (FORMATTED_TRAFFIC) {
        TrafficChanged(upload,download);
      } else {
        TrafficChanged(upload_b,download_b);
      }
    }

    private ITrafficSpeedListener mStreamSpeedListener = new ITrafficSpeedListener() {
        @Override
        public void onTrafficSpeedMeasured(final double upStream, final double downStream) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                      String upStreamSpeed = Utils.parseSpeed(upStream, SHOW_SPEED_IN_BITS);
                      String downStreamSpeed = Utils.parseSpeed(downStream,SHOW_SPEED_IN_BITS);

                      String up_b = String.valueOf(SHOW_SPEED_IN_BITS ? upStream * 8 : upStream);
                      String down_b = String.valueOf(SHOW_SPEED_IN_BITS ? downStream * 8 : downStream);

                      TrafChange(upStreamSpeed, downStreamSpeed, up_b, down_b);                   
                }
            });;


        }
    };
  
    @SimpleEvent(description = "Upload/Download ( Current data traffic/usage )")
    public void TrafficChanged(String upload, String download){
       EventDispatcher.dispatchEvent(this,"TrafficChanged",upload,download);
    }

    @SimpleFunction(description = "Initialize Traffic Measurer")
    public void InitializeTrafficMeasure(int type) {

        mTrafficSpeedMeasurer = new TrafficSpeedMeasurer(type);
        mTrafficSpeedMeasurer.startMeasuring();

    }

    @SimpleFunction(description = "Stops the Traffic Measurer ( Will throw error if it is not Initialized at the first place.")
    public void StopTrafficMeasure() {
      mTrafficSpeedMeasurer.stopMeasuring();
    }

    @SimpleFunction(description = "Pauses the Traffic Measurer ( Useful for activity onPause )")
    public void PauseTrafficMeasure() {
      mTrafficSpeedMeasurer.removeListener();
    }

    @SimpleFunction(description = "Resumes the Traffic Measurer ( Useful for activity onResume )")
    public void ResumeTrafficMeasure() {
       mTrafficSpeedMeasurer.registerListener(mStreamSpeedListener);
    }

}
