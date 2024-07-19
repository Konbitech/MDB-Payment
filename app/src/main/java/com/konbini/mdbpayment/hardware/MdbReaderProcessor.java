package com.konbini.mdbpayment.hardware;

import android.hardware.mdbreader.MdbReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.konbini.mdbpayment.ui.MainActivity;

public class MdbReaderProcessor {
    private final static String TAG = "EventProcessor";

    private Handler mMainHandler = null;
    private WorkHandler mWorkHandler = null;
    private HandlerThread mWorkThread = null;
    private Object object = new Object();

    public MdbReaderInformation mReaderInfomation = new MdbReaderInformation();
    public VmcInformation mVmcInfomation = new VmcInformation();

    /**
     * MdbReader & MdbReaderEventMonitor
     */
    MdbReader mdbReader = MdbReader.getInstance();
    MdbReaderEventMonitorImpl mMdbReaderEventMonitor;

    private boolean isStart = false;

    public static final int EV_INITIAL_COMPLETE = 1;
    public static final int EV_DEDUCT_PRICE = EV_INITIAL_COMPLETE + 1;
    public static final int EV_RESET = EV_DEDUCT_PRICE + 1;

    public MdbReaderProcessor(Handler handler){

        mMainHandler = handler;
        /** create a thread to do some time-consuming work */
        mWorkThread = new HandlerThread("mdbreader_work");
        mWorkThread.start();
        mWorkHandler = new WorkHandler(mWorkThread.getLooper());

        mMdbReaderEventMonitor = new MdbReaderEventMonitorImpl(mdbReader,this);
    }

    public void start(){
        if(isStart){
            return;
        }
        mdbReader.registerMdbReaderMonitor(mMdbReaderEventMonitor);
        mdbReader.start();
        isStart = true;
        mdbReader.setLights(0xFF0000,true,200);
    }

    public void destroy(){
        /**quit work thread*/
        mWorkHandler.removeCallbacksAndMessages(null);
        mWorkThread.quitSafely();
        /**destroy mdb reader*/
        if(isStart){
            mdbReader.destroy();
            mMdbReaderEventMonitor.clear();
            isStart = false;
        }

    }

    public boolean state(){
        return isStart;
    }

    /**you should execute some time-consuming works in work thread
     * when you received event form vmc
     * */
    public void execute(int what) {
        mWorkHandler.sendEmptyMessage(what);
    }

    public void execute(int what, String data) {
        Message msg = new Message();
        msg.what = what;
        Bundle bundle = new Bundle();
        bundle.putString("msg",data);
        msg.setData(bundle);
        mWorkHandler.sendMessage(msg);
    }

    public void execute(int what, int arg) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg;
        mWorkHandler.sendMessage(msg);
    }

    public void execute(int what, int arg1, int arg2) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        mWorkHandler.sendMessage(msg);
    }

    /**
     * You can post some messages to main thread when your transaction completed
     */
    public void postMsg(int what){
        mMainHandler.sendEmptyMessage(what);
    }

    public void postMsg(int what, String data){
        Message msg = new Message();
        msg.what = what;
        Bundle bundle = new Bundle();
        bundle.putString("msg",data);
        msg.setData(bundle);
        mMainHandler.sendMessage(msg);
    }

    public void postMsg(int what, int arg) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg;
        mMainHandler.sendMessage(msg);
    }

    public void postMsg(int what, int arg1, int arg2){
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        mMainHandler.sendMessage(msg);
    }

    public void setPollReply(MdbReaderEventMonitorImpl.PollReply pollReply){
        if(isStart == false){

            return;
        }
        mMdbReaderEventMonitor.setPollReply(pollReply);
    }

    public MdbReaderEventMonitorImpl.StateMachine getStateMachine(){
        return mMdbReaderEventMonitor.getStateMachine();
    }



    /**
     * work thread to do some work after monitored mdb reader event.
     * e.g.deducts purchase price from payment media etc
     */
    private class WorkHandler extends Handler {

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case EV_INITIAL_COMPLETE:
                    String info = "initial completed..." + "\n" +
                            "VMC Level    : " + mVmcInfomation.FeatureLevel + "\n" +
                            "Reader Level : " + mReaderInfomation.FeatureLevel + "\n" +
                            "Currency Code: " + String.format("0x%02x", mReaderInfomation.CurrencyCode) + "\n" +
                            "Manufacturer : " + new String(mReaderInfomation.ManufacturerCode) + "\n" +
                            "SerialNumber : " + new String(mReaderInfomation.SerialNumber) + "\n";

                    postMsg(MainActivity.MSG_MDB_INITIAL_COMPLETE,info);

                    break;
                case EV_DEDUCT_PRICE:
                    int itemPrice = msg.arg1;
                    int itemNumber = msg.arg2;
                    Log.d(TAG, "Here to deducts purchase price from payment media");
                    //todo: To deducts purchase price

                    //todo: to notity ui update
                    postMsg(MainActivity.MSG_MDB_VEND,itemPrice,itemNumber);

                    break;
                case EV_RESET:
                    break;
                default:
                    break;
            }
        }
    }


}