package com.jaxparrow.networkusage;

import android.net.TrafficStats;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;


public class TrafficSpeedMeasurer {

    private ITrafficSpeedListener mTrafficSpeedListener;
    private SamplingHandler mHandler;

    private int mTrafficType;
    private long mLastTimeReading;
    private long mPreviousUpStream = -1;
    private long mPreviousDownStream = -1;

    public TrafficSpeedMeasurer(int trafficType) {
        mTrafficType = trafficType;
        HandlerThread thread = new HandlerThread("ParseThread");
        thread.start();
        mHandler = new SamplingHandler(thread.getLooper());
    }

    public void registerListener(ITrafficSpeedListener iTrafficSpeedListener) {
        mTrafficSpeedListener = iTrafficSpeedListener;
    }

    public void removeListener() {
        mTrafficSpeedListener = null;
    }

    public void startMeasuring() {
        mHandler.startSamplingThread();
        mLastTimeReading = SystemClock.elapsedRealtime();
    }

    public void stopMeasuring() {
        mHandler.stopSamplingThread();
        finalReadTrafficStats();
    }

    private void readTrafficStats() {

        long newBytesUpStream;
        long newBytesDownStream;

        if (mTrafficType == 1) {

            newBytesUpStream = TrafficStats.getMobileTxBytes() * 1024;
            newBytesDownStream = TrafficStats.getMobileRxBytes() * 1024;

        } else if (mTrafficType == 2) {

            newBytesUpStream = ( TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes() ) * 1024;
            newBytesDownStream = ( TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes() ) * 1024;

        } else {

            newBytesUpStream = TrafficStats.getTotalTxPackets() * 1024;
            newBytesDownStream = TrafficStats.getTotalRxBytes() * 1024 ;

        }

        long byteDiffUpStream = newBytesUpStream - mPreviousUpStream;
        long byteDiffDownStream = newBytesDownStream - mPreviousDownStream;

        synchronized (this) {
            long currentTime = SystemClock.elapsedRealtime();
            double bandwidthUpStream = 0;
            double bandwidthDownStream = 0;

            if (mPreviousUpStream >= 0) {
                bandwidthUpStream = (byteDiffUpStream) * 1.0 / (currentTime - mLastTimeReading);
            }
            if (mPreviousDownStream >= 0) {
                bandwidthDownStream = (byteDiffDownStream) * 1.0 / (currentTime - mLastTimeReading);
            }
            if (mTrafficSpeedListener != null) {
                mTrafficSpeedListener.onTrafficSpeedMeasured(bandwidthUpStream, bandwidthDownStream);
            }

            mLastTimeReading = currentTime;
        }

        mPreviousDownStream = newBytesDownStream;
        mPreviousUpStream = newBytesUpStream;
    }

    private void finalReadTrafficStats() {
        readTrafficStats();
        mPreviousUpStream = -1;
        mPreviousDownStream = -1;
    }

    private class SamplingHandler extends Handler {

        private static final long SAMPLE_TIME = 1000;
        private static final int MSG_START = 1;

        private SamplingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START:
                    readTrafficStats();
                    sendEmptyMessageDelayed(MSG_START, SAMPLE_TIME);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown what=" + msg.what);
            }
        }

        void startSamplingThread() {
            sendEmptyMessage(SamplingHandler.MSG_START);
        }

        void stopSamplingThread() {
            removeMessages(SamplingHandler.MSG_START);
        }

    }


}
