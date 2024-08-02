package com.konbini.mdbpayment.hardware;

import android.hardware.mdbreader.MdbReader;
import android.hardware.mdbreader.MdbReaderEventMonitor;
import android.util.Log;

import com.konbini.mdbpayment.utils.LogUtils;


/**
 * mdbReader event monitor implements.
 * All the command/response sequence should be executed in here.
 *
 * NOTE: Mdb event monitor works on a loop thread,So please don't do something
 * time-consuming work or any block work in it.
 */
public class MdbReaderEventMonitorImpl implements MdbReaderEventMonitor {
    private final static String TAG = "mdbReaderEventMonitor";

    /**
     * Response id
     */
    public final static int RESP_ID_JUST_RESET = 0x00;
    public final static int RESP_ID_READER_CONFIG_DATA  = 0x01;
    public final static int RESP_ID_DISPLAY_REQUEST  = 0x02;
    public final static int RESP_ID_BEGIN_SESSION  = 0x03;
    public final static int RESP_ID_SESSION_CANCEL_REQUEST = 0x04;
    public final static int RESP_ID_VEND_APPROVED = 0x05;
    public final static int RESP_ID_VEND_DENIED = 0x06;
    public final static int RESP_ID_END_SESSION = 0x07;
    public final static int RESP_ID_CANCELED = 0x08;
    public final static int RESP_ID_PERIPHERAL_ID = 0x09;
    public final static int RESP_ID_MALFUNCTION = 0x0A;
    public final static int RESP_ID_CMD_OUT_OF_SEQUENCE = 0x0B;
    public final static int RESP_ID_REVALUE_APPROVED = 0x0D;
    public final static int RESP_ID_REVALUE_DENIED = 0x0E;
    public final static int RESP_ID_REVALUE_REVALUE_LIMIT_AMOUNT = 0x0F;
    public final static int RESP_ID_TIME_DATE_REQUEST = 0x11;
    public final static int RESP_ID_DATA_ENTRY_REQUEST = 0x12;
    public final static int RESP_ID_DATA_ENTRY_CANCEL = 0x13;
    public final static int RESP_ID_DIAGNOSTICS = 0xFF;

    /**
     * Response code
     */
    public final static int ACK = 0x00;
    public final static int NAK = 0xFF;
    public final static int RET = 0xAA;

    /**
     * Enum for mdbReader state machine
     */
    public enum StateMachine{
        Inactive,
        Disabled,
        Enabled,
        SessionIdle,
        Vend,
        Revalue,
        NegativeVend
    };
    StateMachine mState = StateMachine.Inactive;

    /**
     * Enum for Poll reply
     */
    public enum PollReply{
        REPLY_ACK,
        REPLY_JUST_RESET,
        REPLY_READER_CFG_DATA,
        REPLY_DISPLAY_REQUEST,
        REPLY_BEGIN_SESSION,
        REPLY_SESSION_CANCEL_REQUEST,
        REPLY_VEND_APPROVED,
        REPLY_VEND_DENIED,
        REPLY_END_SESSION,
        REPLY_CANCELED,
        REPLY_PERIPHERAL_ID,
        REPLY_MALFUNCTION,
        REPLY_CMD_OUT_OF_SEQUENCE,
        REPLY_REVALUE_APPROVED,
        REPLY_REVALUE_DENIED,
        REPLY_REVALUE_LIMIT_AMOUNT,
        REPLY_TIME_DATE_REQUEST,
        REPLY_DATA_ENTRY_REQUEST,
        REPLY_DATA_ENTRY_CANCEL,
        REPLY_DIAGNOSTICS
    };
    PollReply mPollReply = PollReply.REPLY_ACK;

    /**
     * led lights color
     */
    private final static int LIGHT_COLOR_RED = 0xFF0000;
    private final static int LIGHT_COLOR_GREEN = 0x00FF00;
    private final static int LIGHT_COLOR_BLUE = 0x0000FF;

    private MdbReader mdbReader;
    private MdbReaderProcessor mProcessor;
    private boolean isInitialized = false;

    MdbReaderEventMonitorImpl(MdbReader reader, MdbReaderProcessor processor){
        mdbReader = reader;
        mProcessor = processor;
    }

    private String arrayToString(byte[] arr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arr.length; i++) {
            String b = String.format("0x%02x, ", arr[i]);
            sb.append(b);
        }
        return sb.toString();
    }

    private byte[] getReaderConfigData(){
        byte[] responseData = new byte[9];
        int checksum = 0;
        responseData[0] =  RESP_ID_READER_CONFIG_DATA;
        responseData[1] = (byte)mProcessor.mReaderInfomation.FeatureLevel;
        responseData[2] = (byte)(mProcessor.mReaderInfomation.CurrencyCode >> 8);
        responseData[3] = (byte)(mProcessor.mReaderInfomation.CurrencyCode & 0xFF);
        responseData[4] = (byte)mProcessor.mReaderInfomation.ScaleFactor;;
        responseData[5] = (byte)mProcessor.mReaderInfomation.DecimalPlaces;
        responseData[6] = (byte)mProcessor.mReaderInfomation.MaxResponseTime;
        responseData[7] = (byte)mProcessor.mReaderInfomation.MiscOptions;

        for(int i = 0;i < 8;i++){
            checksum += responseData[i];
        }

        responseData[8] = (byte)(checksum & 0xFF);

        return responseData;
    }

    private byte[] getReaderIdentificationInfo(){
        byte[] responseData = new byte[31];
        int checksum = 0;
        int index = 0;
        //todo:Response id - 1 byte
        responseData[index++] =  RESP_ID_PERIPHERAL_ID;
        //Manufacturer Code - 3 byte
        responseData[index++] = mProcessor.mReaderInfomation.ManufacturerCode[0];
        responseData[index++] = mProcessor.mReaderInfomation.ManufacturerCode[1];
        responseData[index++] = mProcessor.mReaderInfomation.ManufacturerCode[2];
        //todo:Serial Number- 12 byte
        System.arraycopy(mProcessor.mReaderInfomation.SerialNumber,0,responseData,index,12);
        index += 12;

        //todo:Model Number - 12 byte
        System.arraycopy(mProcessor.mReaderInfomation.ModelNumber,0,responseData,index,12);
        index += 12;

        //todo:Software Version - 2 byte
        responseData[index++] = mProcessor.mReaderInfomation.SoftwareVersion[0];
        responseData[index++] = mProcessor.mReaderInfomation.SoftwareVersion[1];

        //todo: checksum
        for(int i = 0;i < 30;i++){
            checksum += responseData[i];
        }
        responseData[index] = (byte)(checksum & 0xFF);

//        Log.d(TAG,",getReaderIdentificationInfo: " + arrayToString(responseData));
        return responseData;
    }

    /**
     * set response type in the next POLL
     */
    public void setPollReply(PollReply pollReply){
        mPollReply = pollReply;
    }

    public StateMachine getStateMachine(){
        return mState;
    }

    public void clear(){
        mPollReply = PollReply.REPLY_ACK;
        mState = StateMachine.Inactive;
        isInitialized = false;
    }

    public void setReaderEnable(){
        mdbReader.slaveSendAnswer(ACK);
        mState = StateMachine.Enabled;
        mPollReply = PollReply.REPLY_ACK;

        mdbReader.setLights(LIGHT_COLOR_GREEN,false,0);

        if(!isInitialized){
            isInitialized = true;

            mProcessor.execute(MdbReaderProcessor.EV_INITIAL_COMPLETE);
        }
    }

    /**
     * This event indicates that the cashless device is reset.
     * If this event is received by a cashless device it should terminate any ongoing transaction
     * (with an appropriate credit adjustment, if appropriate), eject the payment media (if applicable)
     *
     * @Feature Level: Level 01/02/03
     * @Response with:ACK
     */
    @Override
    public void onReset() {
        Log.d(TAG,"onReset");
        LogUtils.INSTANCE.logInfo("onReset");
        mdbReader.slaveSendAnswer(ACK);
        //todo:reset mdbReader config info
        mProcessor.execute(MdbReaderProcessor.EV_RESET);

        mState = StateMachine.Inactive;
        mPollReply = PollReply.REPLY_JUST_RESET;
    }

    /**
     * VMC is sending its configuration data to reader.
     * @param: featureLevel -The feature level of the VMC
     *         columnDisplay -The number of columns on the VMC's display.
     *         rowDisplay - the number of rows on the VMC's display
     *         dispalyType - Display type,
     *                       = 0: Numbers, upper case letters, blank and decimal point.
     *                       = 1: Full ASCII
     *
     * SETUP - Config Data
     * CMD Format: MainCmd + SubCmd + VMC Config + CHK
     *             0x11|0x61  0x00      Y2-Y5      checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: reply RESP_ID_READER_CONFIG_DATA
     */
    @Override
    public void onSetupConfigData(byte[] VmcConfigData) {
        Log.d(TAG,"onSetupConfigData");
        LogUtils.INSTANCE.logInfo("onSetupConfigData");
        /**
         * For some special VMCs.if current in the  initializing sequence,SETUP just reply ACK,
         * otherwise,reply reader config info to VMC
         */

            //todo:packet SETUP - Config Data response data
            byte[] configdata = getReaderConfigData();
            int[] respcode = new int[1];

            //todo:send response data to VMC
            do{
                mdbReader.slaveSendResponseData(configdata,configdata.length,respcode);
            }while(respcode[0] == RET);

            /** if NAK,should re-send config data in next Poll.*/
            if(respcode[0] == ACK) {
                mPollReply = PollReply.REPLY_ACK;
            }else if(respcode[0] == NAK){
                mPollReply = PollReply.REPLY_READER_CFG_DATA;
            }

            mState = StateMachine.Disabled;
        


        /**
         * For some special VMCs,If not support display,SETUP CMD not contain Columns andr Rows Display byte
         */
        if(VmcConfigData.length == 5){
            mProcessor.mVmcInfomation.FeatureLevel = VmcConfigData[2];
            mProcessor.mVmcInfomation.DisplayCols = 0;
            mProcessor.mVmcInfomation.DisplayRows = 0;
            mProcessor.mVmcInfomation.DispalyInfo = VmcConfigData[3];
        }else if(VmcConfigData.length == 7){
            mProcessor.mVmcInfomation.FeatureLevel = VmcConfigData[2];
            mProcessor.mVmcInfomation.DisplayCols = VmcConfigData[3];
            mProcessor.mVmcInfomation.DisplayRows = VmcConfigData[4];
            mProcessor.mVmcInfomation.DispalyInfo = VmcConfigData[5];
        }else{
            Log.d(TAG, "onSetupConfigData:Incorrect SETUP command paramter !");
        }
    }

    /**
     * This event indicates that the VMC is sending the price range to the reader.
     *
     * SETUP - Max / Min Prices
     * CMD Format: MainCmd + SubCmd + Max/Min Prices + CHK
     *             0x11|0x61  0x01      Y2-Y5          checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: ACK
     */
    @Override
    public void onSetupMaxMinPrices(byte[] MaxMinPrices) {
        Log.d(TAG,"onSetupMaxMinPrices");
        LogUtils.INSTANCE.logInfo("onSetupMaxMinPrices");
        //todo: just answer ACK to VMC
        mdbReader.slaveSendAnswer(ACK);
        mPollReply = PollReply.REPLY_ACK;

        mProcessor.mReaderInfomation.MaxPrice = (MaxMinPrices[2] << 8) | MaxMinPrices[3];
        mProcessor.mReaderInfomation.MinPrice = (MaxMinPrices[4] << 8) | MaxMinPrices[5];
        LogUtils.INSTANCE.logInfo("MaxPrice: " + mProcessor.mReaderInfomation.MaxPrice);
        LogUtils.INSTANCE.logInfo("MinPrice: " + mProcessor.mReaderInfomation.MinPrice);
    }

    /**
     * The POLL event is used by the VMC to obtain information from the payment media
     * reader.The POLL command is used by the VMC to obtain information from the payment media
     * reader.
     *
     * @Feature Level: Level 01/02/03
     * @Response with: by command sequence
     */
    @Override
    public void onPoll() {
        int checksum = 0;
        byte[] responseData = new byte[36];
        int[] respcode = new int[1];
        int index = 0;

        //Log.d(TAG,"onPoll, mPollReply: " + mPollReply);
        switch (mPollReply){
            /**
             * reply ACK indicates that no error states exist, and either no information
             * request is pending or pending information is not yet ready for transmission
             */
            case REPLY_ACK:
                mdbReader.slaveSendAnswer(ACK);
                break;
            case REPLY_JUST_RESET:
                responseData[0] = RESP_ID_JUST_RESET;
                responseData[1] = 0x00;
                mdbReader.slaveSendResponseData(responseData,2,respcode);
                break;
            case REPLY_READER_CFG_DATA:
                responseData = getReaderConfigData();
                mdbReader.slaveSendResponseData(responseData,9,respcode);
                break;
            case REPLY_DISPLAY_REQUEST:
                break;

            case REPLY_BEGIN_SESSION:
                if(mState != StateMachine.Enabled){
                    if(mState == StateMachine.SessionIdle){
                        Log.d(TAG,"mdbReader is already in Session Idle state.");
                    }else {
                        Log.d(TAG, "Session Error: mdbReader is not in Enable state.");
                    }
                    return;
                }
                mdbReader.setLights(LIGHT_COLOR_BLUE,true,200);
                /**
                 * Here Z2-Z10 just for test,
                 * User should obtain the corresponding data according to the actual situation
                 */
                if(mProcessor.mReaderInfomation.FeatureLevel == 1){
                    responseData[index++] = RESP_ID_BEGIN_SESSION;
                    /** Z2-Z3 :Funds Available */
                    responseData[index++] = (byte)0xFF;
                    responseData[index++] = (byte)0xFF;

                } else if (mProcessor.mReaderInfomation.FeatureLevel == 2 || mProcessor.mReaderInfomation.FeatureLevel == 3) {
                    responseData[index++] = RESP_ID_BEGIN_SESSION;
                    /** Z2-Z3 :Funds Available */
                    responseData[index++] = (byte)0xFF;
                    responseData[index++] = (byte)0xFF;
                    /** Z4-Z7 :Payment mediaID */
                    responseData[index++] = 0x00;
                    responseData[index++] = 0x00;
                    responseData[index++] = 0x00;
                    responseData[index++] = 0x01;
                    /** Z8 :Type of payment */
                    responseData[index++] = 0x40;
                    /** Z9-Z10: Payment data */
                    responseData[index++] = 0;
                    responseData[index++] = 0;
                }

                /** calculate checksum */
                for(int i = 0;i < index; i++){
                    checksum += responseData[i];
                }
                responseData[index] = (byte) (checksum & 0xFF);

                do {
                    mdbReader.slaveSendResponseData(responseData,index+1,respcode);
                }while (respcode[0] == RET || respcode[0] == NAK);

                /** After send BEGIN SESSION,Reader should enter the Session Idle state */
                mState = StateMachine.SessionIdle;
                mPollReply = PollReply.REPLY_ACK;
                break;

            case REPLY_SESSION_CANCEL_REQUEST:
                //todo:reply SESSION CANCEL REQUEST
                responseData[0] = RESP_ID_SESSION_CANCEL_REQUEST;
                checksum += responseData[0];
                responseData[1] = (byte)(checksum & 0xFF);
                do {
                    mdbReader.slaveSendResponseData(responseData,2,respcode);
                }while (respcode[0]==RET || respcode[0] == NAK);

                mPollReply = PollReply.REPLY_ACK;
                break;

            case REPLY_VEND_APPROVED:
                if(mState != StateMachine.Vend){
                    Log.d(TAG,"Session Error: mdbReader is not Vend state.");
                    return;
                }
                int VendAmount = 100;
                responseData[0] = RESP_ID_VEND_APPROVED;
                responseData[1] = (byte)(VendAmount >> 8);
                responseData[2] = (byte)(VendAmount & 0xFF);
                checksum = responseData[0] + responseData[1] + responseData[2];
                responseData[3] = (byte) (checksum & 0xFF);
                do {
                    mdbReader.slaveSendResponseData(responseData,4,respcode);
                }while (respcode[0]==RET || respcode[0] == NAK);

                mPollReply = PollReply.REPLY_ACK;

                break;

            case REPLY_VEND_DENIED:
                if(mState != StateMachine.Vend){
                    Log.d(TAG,"Session Error: mdbReader is not Vend state.");
                    return;
                }
                responseData[0] = RESP_ID_VEND_DENIED;
                checksum += responseData[0];
                responseData[1] = (byte)(checksum & 0xFF);
                do {
                    mdbReader.slaveSendResponseData(responseData,2,respcode);
                }while (respcode[0]==RET || respcode[0] == NAK);

                mPollReply = PollReply.REPLY_ACK;

                break;

            case REPLY_END_SESSION:
                //todo:reply END SESSION
                responseData[0] = RESP_ID_END_SESSION;
                checksum += responseData[0];
                responseData[1] = (byte)(checksum & 0xFF);
                do {
                    mdbReader.slaveSendResponseData(responseData,2,respcode);
                }while (respcode[0]==RET || respcode[0] == NAK);

                //todo: enter Enable state
                mState = StateMachine.Enabled;

                //todo:Answer ACK in the next POLL
                mPollReply = PollReply.REPLY_ACK;

                mdbReader.setLights(LIGHT_COLOR_GREEN,false,0);

                break;

            case REPLY_CANCELED:
                break;
            case REPLY_PERIPHERAL_ID:
                responseData = getReaderIdentificationInfo();
                do {
                    mdbReader.slaveSendResponseData(responseData,31,respcode);
                }while (respcode[0]==RET || respcode[0] == NAK);

                //todo:Answer ACK in the next POLL
                mPollReply = PollReply.REPLY_ACK;
                break;
            case REPLY_MALFUNCTION:
                break;
            case REPLY_CMD_OUT_OF_SEQUENCE:
                break;
            case REPLY_REVALUE_APPROVED:
                break;
            case REPLY_REVALUE_DENIED:
                break;
            case REPLY_REVALUE_LIMIT_AMOUNT:
                break;
            case REPLY_TIME_DATE_REQUEST:
                break;
            case REPLY_DATA_ENTRY_REQUEST:
                break;
            case REPLY_DATA_ENTRY_CANCEL:
                break;
            case REPLY_DIAGNOSTICS:
                break;
            default:
                Log.d(TAG,"onPoll, unknown poll action.");
                break;
        }
    }

    /**
     * This event indicates that the patron has made a selection.
     * The VMC is requesting vend approval from the payment media reader before dispensing the product.
     * @param data Y2-Y3 itemPrice- The price of the selected product.
     * @param data Y4-Y5 itemNumber- The item number of the selected product.
     *
     * CMD Format: MainCmd +  SubCmd  + Item Price + Item Number + CHK
     *             0x13|0x63  0x00      Y2-Y3        Y4-Y5         checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: reply VendApproved/VendDenied in the next POLL
     */
    @Override
    public void onVendRequest(byte[] data) {
        Log.d(TAG,"onVendRequest.");
        LogUtils.INSTANCE.logInfo("onVendRequest.");

        if(mState != StateMachine.SessionIdle){
            Log.d(TAG,"Session Error: mdbReader is not Session Idle state.");
            return;
        }
        //todo:answer ACK and enter Vend state
        mdbReader.slaveSendAnswer(ACK);
        mState = StateMachine.Vend;

        //todo:informs workThread to deducts purchase price from payment media etc
        int itemPrice = (data[2] << 8) | data[3];
        int itemNumber = (data[4] << 8) | data[5];
        mProcessor.execute(MdbReaderProcessor.EV_DEDUCT_PRICE,itemPrice,itemNumber);
    }

    /**
     * This event indicates that the VMC request to cancel a Vend Request
     *  before {@#replyVendApproved()}/{#replyVendDenied()} has been called.
     *
     * CMD Format: MainCmd +  SubCmd  + CHK
     *             0x13|0x63  0x01      checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: reply VendDenied in the next POLL
     */
    @Override
    public void onVendCancel() {
        Log.d(TAG,"onVendCancel.");
        LogUtils.INSTANCE.logInfo("onVendCancel.");

        if(mState != StateMachine.Vend){
            Log.d(TAG,"Session Error: mdbReader is not Vend state.");
            return;
        }
        //todo:answer ACK and reply VEND DENIED in the next POLL
        mdbReader.slaveSendAnswer(ACK);
        mPollReply = PollReply.REPLY_VEND_DENIED;
    }

    /**
     * This event indicates that the selected product has been successfully dispensed.
     * @param data -The item number of the selected product.
     *
     * CMD Format: MainCmd +  SubCmd + itemNumber + CHK
     *             0x13|0x63  0x02      Z2-Z3       checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with:
     */
    @Override
    public void onVendSuccess(byte[] data) {
        Log.d(TAG,"onVendSuccess.");
        LogUtils.INSTANCE.logInfo("onVendSuccess.");
        int itemNum = (data[2] << 8) | data[3];
        //todo: answer ACK and enter Session idle state
        mdbReader.slaveSendAnswer(ACK);
        mState = StateMachine.SessionIdle;
    }

    /**
     *  This event indicates that a vend has been attempted at the VMC but a problem has been detected
     *   and the vend has failed. The product was not dispensed. Funds should
     *   be refunded to user’s account.
     *
     * CMD Format: MainCmd +  SubCmd  + CHK
     *             0x13|0x63  0x03      checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: ACK or {#replyDiagnostics}
     */
    @Override
    public void onVendFailure() {
        Log.d(TAG,"onVendFailure.");
        LogUtils.INSTANCE.logInfo("onVendFailure.");
        //todo: answer ACK
        mdbReader.slaveSendAnswer(ACK);
    }

    /**
     * This tells the payment media reader that the session is complete and to
     * return to the Enabled state
     *
     * CMD Format: MainCmd +  SubCmd  + CHK
     *             0x13|0x63  0x04      checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: reply EndSession in the next POLL
     */
    @Override
    public void onSessionComplete() {
        Log.d(TAG,"onSessionComplete.");
        LogUtils.INSTANCE.logInfo("onSessionComplete.");

        //todo:answer ACK
        mdbReader.slaveSendAnswer(ACK);
        //todo:reply END SESSION in the next POLL
        mPollReply = PollReply.REPLY_END_SESSION;

    }

    /**
     * A cash sale (cash only or cash and cashless) has been successfully
     * completed by the VMC.
     *
     * CMD Format: MainCmd +  SubCmd  + Item Price + Item Number + CHK
     *             0x13|0x63  0x05      Z2-Z3         Z4-Z5        checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with:
     */
    @Override
    public void onCashSale(byte[] data) {
        Log.d(TAG,"onCashSale");
        LogUtils.INSTANCE.logInfo("onCashSale.");
    }

    /**
     * This event indicates that the patron has inserted an item. The VMC is requesting negative vend
     * approval from the payment media reader before accepting the returned
     * product.
     * @param: itemValue - The value of the inserted product
     *         itemNumber -The item number of the inserted product.
     *
     * @Feature Level: Level 03
     * @Response with: reply VendApproved/VendDenied in the next POLL
     */
    @Override
    public void onNegativeVendRequest(byte[] data) {
        LogUtils.INSTANCE.logInfo("onNegativeVendRequest.");
    }

    /**
     * This informs the payment media reader that it has been disabled, i.e. it
     * should no longer accept a patron’s payment media for the purpose of
     * vending. Vending activities may be re-enabled using the READER
     * ENABLE command. The payment media reader should retain all SETUP information.
     *
     * NOTE: Any transaction in progress will not be affected and should continue to its
     * normal completion.
     *
     * CMD Format: MainCmd + SubCmd + CHK
     *             0x14|0x64  0x00   checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: ACK
     */
    @Override
    public void onReaderDisable() {
        Log.d(TAG,"onReaderDisable");
        LogUtils.INSTANCE.logInfo("onReaderDisable.");

        //todo: answer ACK to VMC
        mdbReader.slaveSendAnswer(ACK);
        mState = StateMachine.Disabled;
        mPollReply = PollReply.REPLY_ACK;

        mdbReader.setLights(LIGHT_COLOR_RED,false,0);

    }

    /**
     * This informs the payment media reader that is has been enabled, i.e. it
     * should now accept a patron’s payment media for vending purposes. This
     * command must be issued to a reader in the Disabled state to enable
     * vending operations.
     *
     * CMD Format: MainCmd + SubCmd + CHK
     *             0x14|0x64  0x01    checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: ACK
     */
    @Override
    public void onReaderEnable() {
        Log.d(TAG,"onReaderEnable");
        LogUtils.INSTANCE.logInfo("onReaderEnable.");

        //todo: answer ACK to VMC
        mdbReader.slaveSendAnswer(ACK);
        mState = StateMachine.Enabled;
        mPollReply = PollReply.REPLY_ACK;

        mdbReader.setLights(LIGHT_COLOR_GREEN,false,0);

        if(isInitialized == false){
            isInitialized = true;

            mProcessor.execute(MdbReaderProcessor.EV_INITIAL_COMPLETE);
        }
    }

    /**
     * This command is issued to abort payment media reader activities which
     * occur in the Enabled state. It is the first part of a command/response
     * sequence which requires a CANCELLED response from the reader
     *
     *  NOTE: CANCELLED response would be send in the next POLL
     *
     * CMD Format: MainCmd + SubCmd + CHK
     *             0x14|0x64  0x02    checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: reply Cancelled in the next POLL
     */
    @Override
    public void onReaderCancel() {
        Log.d(TAG,"onReaderCancel");
        if(mState != StateMachine.Enabled){
            Log.d(TAG,"Invalid: READER CANCEL only occur in the Enable state.");
            return;
        }
        mPollReply = PollReply.REPLY_CANCELED;
    }

    /**
     * The purpose of the overall Data Entry request / response sequence is to allow the machine
     * user to enter data (i.e., a card validation number) using the selection buttons on the vending
     * machine.
     *
     * NOTE: If the reader has additional display information to send to the VMC following the
     * DATA ENTRY RESPONSE, it should send it via a DISPLAY REQUEST response to
     * one of the next POLL commands from the VMC.
     *
     * CMD Format: MainCmd + SubCmd + DataEnter  + CHK
     *             0x14|0x64  0x03      Y2-Y9      checksum
     *
     * @Feature Level: Level 03
     * @Response with:
     */
    @Override
    public void onReaderDataEntryResponse(byte[] dataEntry) {

    }

    /**
     * A balance in the VMC account because coins or bills were accepted or
     *  some balance is left after a vend. With this command the VMC tries to
     *  transfer the balance to the payment media.
     * @param data - Revalue amount
     *
     * @Feature Level: Level 02/03
     * @Response with: reply RevalueApproved()/RevalueDenied in the next POLL
     */
    @Override
    public void onRevalueRequest(byte[] data) {
        Log.d(TAG,"onRevalueRequest");
    }

    /**
     * This event request the maximum amount the payment media reader eventually will accept.
     * Especially if the bill acceptor accepts a wide range of bills.
     * Otherwise the VMC may be confronted by the situation where it accepted a high value bill
     * and is unable to pay back cash or revalue it to a payment media.
     * @ the maximum amount will be set by call {#RevalueLimitAmount()}
     *
     * @Feature Level: Level 02/03
     * @Response with: reply RevalueLimitAmount
     */
    @Override
    public void onRevalueLimitRequest() {

    }

    /**
     * The VMC is requesting payment media reader identification
     * information. The information included above (Y2-Y30) provides the
     * payment media reader with VMC identification information.
     *
     * EXPANSION - REQUEST ID
     * CMD Format: MainCmd + SubCmd + VMC ProductInfo  + CHK
     *             0x17|0x67  0x00      Y2-Y30           checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: reply 09H(PERIPHERAL ID)
     */
    @Override
    public void onExpansionRequestID(byte[] VMCInfo) {
        Log.d(TAG,"onExpansionRequestID");
        //todo:packet REQUEST ID
        byte[] identificationInfo = getReaderIdentificationInfo();
        int[] respcode = new int[1];

        //todo:send response data to VMC
        do{
            mdbReader.slaveSendResponseData(identificationInfo,identificationInfo.length,respcode);
        }while(respcode[0] == RET);

        /** if NAK,should re-send config data in next Poll.*/
        if(respcode[0] == ACK) {
            mPollReply = PollReply.REPLY_ACK;
        }else if(respcode[0] == NAK){
            mPollReply = PollReply.REPLY_PERIPHERAL_ID;
        }

    }

    /**
     * This event request reader  synchronize the real time clock of the card reader
     * with real time clock of the VMC.
     * @param: timeDate- Time/Date to synchronize the card reader real time clock.
     *                    The date bytes are BCD encoded.
     *
     * CMD Format: MainCmd + SubCmd + Time/Date  + CHK
     *             0x17|0x67  0x03      Y2-Y11     checksum
     *
     * @Feature Level: Level 02/03
     * @Response with:
     */
    @Override
    public void onExpansionWriteTimeDate(byte[] timeDate) {

    }

    /**
     * This event indicates that the VMC enable which level 3 features it desires.
     * @param: option - Optional feature bits (All features are disabled after a reset.)
     *                  b0 - File Transport Layer supported
     *                  b1 - 0 = 16 bit monetary format, 1 = 32 bit monetary format
     *                  b2 – Enable multi currency / multi lingual
     *                  b3 – Enable negative vend
     *                  b4 - Enabledata entry
     *                  b5 – Enable “Always Idle” state
     *                  b6 to b31 not used (should be set to 0)
     *
     * CMD Format: MainCmd + SubCmd + Option  + CHK
     *             0x17|0x67  0x04    Y2-Y5     checksum
     *
     * @Feature Level: Level 03
     * @Response with:
     */
    @Override
    public void onExpansionEnableOption(byte[] option) {
        Log.d(TAG,"onExpansionEnableOption");
        if(option.length == 7){
            mdbReader.level3_options = option[5];
        }
        mdbReader.slaveSendAnswer(ACK);
        mPollReply = PollReply.REPLY_ACK;
    }

    /**
     * Device manufacturer specific instruction for implementing various
     * manufacturing or test modes
     * @param: data - User Defined Data.
     *
     * CMD Format: MainCmd + SubCmd + user data  + CHK
     *             0x17|0x67  0xFF      Y2-Yn     checksum
     *
     * @Feature Level: Level 01/02/03
     * @Response with: {#replyDiagnostics()}
     */
    @Override
    public void onExpansionDiagnostics(byte[] data) {

    }

    /**
     * some other reserved command
     */
    @Override
    public void onOtherReserved(byte[] data) {

    }
}
