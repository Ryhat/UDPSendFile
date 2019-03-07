package com.chrisfeb.app.udpsendfiletwo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

class MySocket {
    private static final String TAG = "MySocket";

    DatagramSocket mDatagramSocket;
    DatagramPacket mDatagramPacket;
    RandomAccessFile accessFile;

    long fileLength;

    InetAddress mAddress;
    String mPath;
    String mFileName;
    int mPort;
    Context mContext;
    boolean isConnect = false;
    private OnAddLogListener mLogArrayHandler;
    private byte[] mBuf = new byte[1400];
    private byte[] mReceiveBuf = new byte[1];
    private static final int PERMISSIONS_REQUEST = 8848;

    private ProgressBar progressBar;

    private String startTime;
    private String endTime;
    MySocket(String address, String path, int port, String filename, Context context, ProgressBar progressBar) {
        try {
            this.mAddress = InetAddress.getByName(address);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.mPort = port;
        this.mPath = path;
        this.mFileName = filename;
        this.mContext = context;
        this.progressBar = progressBar;

        mDatagramPacket = new DatagramPacket(mBuf, mBuf.length, mAddress, mPort);
        try {
            mDatagramSocket = new DatagramSocket(mPort + 1);
            isConnect = true;
            //
            Log.d(TAG, "MySocket: 连接成功。");
        } catch (SocketException e) {
            Log.d(TAG, "MySocket: 连接失败。");
            e.printStackTrace();
        }
    }


    void startUdpService(){

        mLogArrayHandler.addLog("开始UDP服务.", getCurrentTime());

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int readSize = -1;
                    int hasReadSize = 0;

                    int permissionCheck = ContextCompat.checkSelfPermission(mContext,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    if (permissionCheck == PackageManager.PERMISSION_GRANTED){
                        try {
                            Log.d(TAG, "open file: success!");
                            accessFile = new RandomAccessFile(mPath, "r");
                            fileLength = accessFile.length();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d(TAG, "run: 请给我权限读取文件..");
                        ActivityCompat.requestPermissions((Activity) mContext,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PERMISSIONS_REQUEST
                                    );
                    }

                    byte buff[] = mFileName.getBytes();

                    mLogArrayHandler.addLog("文件名："+ mFileName, getCurrentTime());
                    mLogArrayHandler.addLog("文件大小: " + fileLength, getCurrentTime());
                    boolean nameFlag = true;

                    try {
                        while (nameFlag){
                            mDatagramPacket.setData(buff, 0, buff.length);
                            mDatagramSocket.send(mDatagramPacket);
                            startTime = getCurrentTime();
                            while (isConnect){

                                mLogArrayHandler.addLog("等待服务器...", getCurrentTime());

                                mDatagramPacket.setData(mReceiveBuf, 0, mReceiveBuf.length);
                                mDatagramSocket.receive(mDatagramPacket);


                                if (new String(mReceiveBuf, 0, mReceiveBuf.length).equals("1")){
                                    mLogArrayHandler.addLog("服务器已收到.", getCurrentTime());

                                    nameFlag = false;
                                    break;
                                } else {
                                    mLogArrayHandler.addLog("服务器没收到，继续发.", getCurrentTime());

                                    mDatagramPacket.setData(buff, 0, readSize);
                                    mDatagramSocket.send(mDatagramPacket);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    while ( isConnect && (readSize = accessFile.read(mBuf, 0, mBuf.length)) != -1){
                        hasReadSize = hasReadSize + readSize;
                        mLogArrayHandler.addLog("已读"+readSize+"字节", getCurrentTime());
                        mLogArrayHandler.addLog("发送:\n"+ new String(mBuf, 0, readSize), getCurrentTime());

                        mDatagramPacket.setData(mBuf, 0, readSize);
                        mDatagramSocket.send(mDatagramPacket);

                        while (isConnect){
                            mLogArrayHandler.addLog("等待服务器...", getCurrentTime());

                            mDatagramPacket.setData(mReceiveBuf, 0, mReceiveBuf.length);
                            mDatagramSocket.receive(mDatagramPacket);


                            if (new String(mReceiveBuf, 0, mReceiveBuf.length).equals("1")){
                                mLogArrayHandler.addLog("服务器已收到.", getCurrentTime());
                                int progress = (int) (hasReadSize * 100 / fileLength);
                                progressBar.setProgress(progress);
                                break;
                            } else {
                                mLogArrayHandler.addLog("服务器没收到，继续发.", getCurrentTime());

                                mDatagramPacket.setData(mBuf, 0, readSize);
                                mDatagramSocket.send(mDatagramPacket);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    endTime = getCurrentTime();
                    long time = getRunTime(startTime, endTime) / 1000;
                    Log.d(TAG, "runTime: " + time);
                    Log.d(TAG, "平均速度: " + (fileLength / 1024) / time + "KB/S");
                    if (accessFile != null){
                        try {
                            mLogArrayHandler.addLog("文件关闭", getCurrentTime());
                            accessFile.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mDatagramSocket != null){
                        mLogArrayHandler.addLog("UDP服务关闭", getCurrentTime());
                        mDatagramSocket.close();
                        isConnect = false;
                    }
                }
            }
        });
        
        thread.start();
    }
    public interface OnAddLogListener{
        void addLog(String log, String date);
    }

    void setLogArrayHandler(OnAddLogListener onAddLogListener){
        mLogArrayHandler = onAddLogListener;
    }

    private String getCurrentTime(){

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    public void stopService(){
        if (isConnect){
            mDatagramSocket.disconnect();
            mDatagramSocket.close();
            mLogArrayHandler.addLog("断开连接.", getCurrentTime());
            Log.d(TAG, "stopService: 断开连接");
            isConnect = false;
        }

    }

    public long getRunTime(String startTime, String endTime){
        long time = 0;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");

        try {
            time = simpleDateFormat.parse(endTime).getTime() - simpleDateFormat.parse(startTime).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return time;
    }
}
