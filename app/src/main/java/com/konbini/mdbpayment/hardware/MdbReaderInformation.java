package com.konbini.mdbpayment.hardware;

public class MdbReaderInformation {

    public static final int FeatureLevel = 0x01;

    public int CurrencyCode = 0x1840;

    public int ScaleFactor = 0x01;

    public int DecimalPlaces = 0x02;

    public int MaxResponseTime = 0x05;

    public int MiscOptions = 0x00;

    public int MaxPrice = 0xFFFF;

    public int MinPrice = 0x0000;

    public byte[] ManufacturerCode = {'c','t','k'};

    public byte[] SerialNumber = {'c','t','k','-','1','2','3','4','5','6','7','8'};

    public byte[] ModelNumber = {'c','m','3','0','1','2','3','4','5','6','7','8'};

    public byte[] SoftwareVersion = {0x00,0x01};

    public MdbReaderInformation(){
    }

    public void reset(){
        //todo:reset mdbReader config info
    }

    public void setReaderConfigData(int currencyCode, int scaleFactor,int decimalPlaces,int MaxRespTime,int misc){
        CurrencyCode = currencyCode;
        ScaleFactor = scaleFactor;
        DecimalPlaces = decimalPlaces;
        MaxResponseTime = MaxRespTime;
        MiscOptions = misc;
    }

    public void setReaderMaxMinPrice(int maxPrice,int minPrice){
        MaxPrice = maxPrice;
        MinPrice = minPrice;
    }


}
