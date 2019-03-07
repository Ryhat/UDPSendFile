package com.chrisfeb.app.udpsendfiletwo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.TtsSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.DatagramSocket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String PATH_ROOT = "//storage/emulated/0/";
    private static final int REQUEST_CODE = 43;

    private EditText mEditText[];
    private EditText mEditTextPort;
    private EditText mEditTextFile;
    private ProgressBar progressBar;
    private String mPath;
    private String mAddress;
    private String fileName;

    private int mPort;
    private File file;
    private MySocket mySocket;

    private RecyclerView mRecyclerView;
    private LogAdapter mAdapter;

    private Calendar calendar;
    private static final int PERMISSIONS_REQUEST = 8848;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEditText = new EditText[4];
        mEditText[0] = findViewById(R.id.editText1);
        mEditText[1] = findViewById(R.id.editText2);
        mEditText[2] = findViewById(R.id.editText3);
        mEditText[3] = findViewById(R.id.editText4);
        mEditTextPort = findViewById(R.id.editText5);
        mEditTextFile = findViewById(R.id.editText6);
        progressBar = findViewById(R.id.progress_bar);
        setEditTextListener();

        mRecyclerView = findViewById(R.id.rv_log);

        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true);

        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new LogAdapter();
        mRecyclerView.setAdapter(mAdapter);
        calendar = Calendar.getInstance();
    }

    private void setEditTextListener() {
        //1
        for (int i = 0; i < mEditText.length; i++) {
            final int curIndex = i;
            mEditText[i].addTextChangedListener(new TextWatcher() {
                int maxLength = 3;
                int restLength = maxLength;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (restLength > 0) {
                        restLength = maxLength - mEditText[curIndex].getText().length();
                    }
                    if (restLength == 0) {
                        int nextIndex = curIndex + 1;

                        if (nextIndex >= mEditText.length) {
                            mEditTextPort.requestFocus();
                            return;
                        }

                        mEditText[nextIndex].requestFocus();
                    }
                }

            });
        }

    }

    private String getCurrentTime(){

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        return simpleDateFormat.format(date);
    }

    public void sendFile(View view) {

        if (mySocket.isConnect){
            mySocket.startUdpService();
        } else {
            mAdapter.addItem("还没连接，你干啥呢。", getCurrentTime());
        }
    }
    public void connect(View view) {
        mAddress = getIp();
        mPort = Integer.parseInt(mEditTextPort.getText().toString());
        mAdapter.addItem("发送文件IP地址：" + mAddress, getCurrentTime());
        mAdapter.addItem( "发送文件端口: " + mPort, getCurrentTime());

        mySocket = new MySocket(mAddress, mPath, mPort, fileName, this, progressBar);

        mySocket.setLogArrayHandler(new MySocket.OnAddLogListener() {
            @Override
            public void addLog(String log, String date) {
                mAdapter.addItem(log, date);
            }
        });
    }
    public void disconnect(View view) {
        mySocket.stopService();
    }

    private String getIp() {
        String ip = "";
        for (EditText aMEditText : mEditText) {
            ip = ip + aMEditText.getText().toString() + ".";
        }

        return ip.substring(0, ip.length() - 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode){
            case PERMISSIONS_REQUEST : {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG, "onRequestPermissionsResult: OK");
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: NO");
                }
            }
        }
    }

    public void chooseFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();

                String dirtyFileName = uri.getPath();
                String dirtyFileName2 = dirtyFileName.substring(dirtyFileName.lastIndexOf(":") + 1, dirtyFileName.length());
                Log.d(TAG, "onActivityResult: " + dirtyFileName);
                Log.d(TAG, "onActivityResult: " + dirtyFileName2);
                fileName = dirtyFileName2.substring(dirtyFileName2.lastIndexOf("/") + 1, dirtyFileName2.length());

                Log.d(TAG, "onActivityResult: " + fileName);
                mPath = PATH_ROOT + dirtyFileName2;
                file = new File(mPath);
                if (!file.exists()) {
                    mAdapter.addItem("打开文件失败。", getCurrentTime());
                } else {
                    mAdapter.addItem("打开文件: " + mPath, getCurrentTime());
                }
                mEditTextFile.setText(mPath);
            }
        }
    }



}
