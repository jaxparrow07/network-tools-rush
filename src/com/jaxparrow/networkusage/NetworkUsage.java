package com.jaxparrow.networkusage;


import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.YailList;

import android.content.Context;
import android.net.TrafficStats;
import android.util.Log;
import android.content.pm.ApplicationInfo;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStats.Bucket;
import android.net.ConnectivityManager;
import android.app.Activity;

import java.lang.System;
import java.lang.Thread;


public class NetworkUsage extends AndroidNonvisibleComponent {

    private Context context;
    private Activity activity;
    public TrafficSpeedMeasurer mTrafficSpeedMeasurer;
    private static boolean SHOW_SPEED_IN_BITS = false;
    private static boolean FORMATTED_TRAFFIC = true;


    public NetworkUsage(ComponentContainer container) {
      super(container.$form());
      this.context = container.$context();
      this.activity = (Activity) this.context;
    }

    @SimpleProperty(description = "Data type : All")
    public int DataAll(){
      return 0;
    }

    @SimpleProperty(description = "Sets the returned output to bytes or in bits")
    public void ReturnBits(boolean bool){
      this.SHOW_SPEED_IN_BITS = bool;
    }


    @SimpleProperty(description = "Gets the returned output to bytes or in bits")
    public boolean ReturnBits(){
      return SHOW_SPEED_IN_BITS;
    }

    @SimpleProperty(description = "Sets the returned output of Traffic output to bytes or in bits")
    public void FormattedTraffic(boolean bool){
      this.FORMATTED_TRAFFIC = bool;
    }


    @SimpleProperty(description = "Gets the returned output of Traffic output to bytes or in bits")
    public boolean FormattedTraffic(){
      return FORMATTED_TRAFFIC;
    }

    @SimpleProperty(description = "Data type : Mobile")
    public int DataMobile(){
      return 1;
    }

    @SimpleFunction(description = "Get Downloaded Data in Bytes by type.You can also get bits by setting the property.")
    public String GetDownloaded(int type, boolean formatted) {

      double getbytes;

      if (type == 1) {
         getbytes = (double) TrafficStats.getMobileRxBytes();
      }
      else {
         getbytes = (double) TrafficStats.getTotalRxBytes();

      }

      double val = SHOW_SPEED_IN_BITS ? getbytes * 8 : getbytes;
      
      if (!formatted) {
        return String.valueOf(val);
      } else {
        return Utils.parseUsage(getbytes, SHOW_SPEED_IN_BITS);
      }

    }
    @SimpleFunction(description = "Get Upload Packets. Use the type property to get specific information.")
    public String GetUploadPackets(int type) {

      long getpacks;

      if (type == 1) {
          getpacks = TrafficStats.getMobileTxPackets();
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
      } else {
          getpacks = TrafficStats.getTotalRxPackets();
      }
      
      return String.valueOf(getpacks);

    }



    @SimpleFunction(description = "Convert bytes into readable data ( Supports bits too )")
    public String FormatBytes(double bytes, boolean useBits) {
      return Utils.parseUsage(bytes,useBits);
    }
 

    @SimpleFunction(description = "Get Uploaded Data in Bytes by type. Set the formatted to get formatted output like Kbps,Mbps. You can also use bits by setting the property.")
    public String GetUploaded(int type, boolean formatted) {

      double getbytes;

      if (type == 1) {
          getbytes = (double) TrafficStats.getMobileTxBytes();
      } else {
          getbytes = (double) TrafficStats.getTotalTxBytes();
      }

      double val = SHOW_SPEED_IN_BITS ? getbytes * 8 : getbytes;

      if (!formatted) {
        return String.valueOf(val);
      } else {
        return Utils.parseUsage(getbytes, SHOW_SPEED_IN_BITS);
      }


    }


    public void TrafChange(String upload,String download, String upload_b,String download_b) {

      if (FORMATTED_TRAFFIC) {

        TrafficChanged(upload,download);

      } else {

        TrafficChanged(upload_b,download_b);
      }



    }



    @SimpleEvent
    public void TrafficChanged(String upload, String download){
         EventDispatcher.dispatchEvent(this,"TrafficChanged",upload,download);
    }

    private ITrafficSpeedListener mStreamSpeedListener = new ITrafficSpeedListener() {
        @Override
        public void onTrafficSpeedMeasured(final double upStream, final double downStream) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                      String upStreamSpeed = Utils.parseSpeed(upStream, SHOW_SPEED_IN_BITS);
                      String downStreamSpeed = Utils.parseSpeed(downStream,SHOW_SPEED_IN_BITS);
                      double up_b = SHOW_SPEED_IN_BITS ? upStream * 8 : upStream;
                      double down_b = SHOW_SPEED_IN_BITS ? downStream * 8 : downStream;
                      TrafChange(upStreamSpeed,downStreamSpeed,String.valueOf(up_b),String.valueOf(down_b));

                   
                }
            });;


        }
    };
  

    @SimpleFunction(description = "Initialize Traffic Measurer ")
    public void InitializeTrafficMeasure(int type) {

        if (type == 1) {
          mTrafficSpeedMeasurer = new TrafficSpeedMeasurer(TrafficSpeedMeasurer.TrafficType.MOBILE);
          mTrafficSpeedMeasurer.startMeasuring();
        } else {
          mTrafficSpeedMeasurer = new TrafficSpeedMeasurer(TrafficSpeedMeasurer.TrafficType.ALL);
          mTrafficSpeedMeasurer.startMeasuring();
        }

    }

    @SimpleFunction(description = "Stops the Traffic Measurer")
    public void StopTrafficMeasure() {
      mTrafficSpeedMeasurer.stopMeasuring();
    }

    @SimpleFunction(description = "Pauses the Traffic Measurer")
    public void PauseTrafficMeasure() {
      mTrafficSpeedMeasurer.removeListener();
    }

    @SimpleFunction(description = "Resumes the Traffic Measurer")
    public void ResumeTrafficMeasure() {
       mTrafficSpeedMeasurer.registerListener(mStreamSpeedListener);
    }



  
}
