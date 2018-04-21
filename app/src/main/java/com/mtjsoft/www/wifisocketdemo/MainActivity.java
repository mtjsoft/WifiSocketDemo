package com.mtjsoft.www.wifisocketdemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static int StringProt = 3000;
    private static int FileProt = 9999;
    //存放接收到的文字信息
    private StringBuffer buffer = new StringBuffer();
    //sd卡根目录
    private String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    /**
     * 线程池
     */
    private ExecutorService executorService = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>(128));
    //发送文字信息
    private Button buttonString;
    //发送文件
    private Button buttonFile;
    //显示一些接收的消息
    private TextView showTextView;
    //输入要传入的文件路径
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = findViewById(R.id.et_send_msg);
        buttonString = findViewById(R.id.btu_send_string);
        buttonFile = findViewById(R.id.btu_send_file);
        showTextView = findViewById(R.id.tv_show);
        showTextView.setText(getlocalip());
        editText.setText(sdPath);
        buttonString.setOnClickListener(this);
        buttonFile.setOnClickListener(this);
        //创建接收文本消息的服务//作为接收端的手机，需要放开。
//        createStringServerSocket();
        //创建接收文件的服务//作为接收端的手机，需要放开。
//        createFileServerSocket();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btu_send_string:
                String msg = editText.getText().toString().trim();
                if (TextUtils.isEmpty(msg)) {
                    Toast.makeText(getBaseContext(), "请输入要发送的消息", Toast.LENGTH_LONG).show();
                    return;
                }
                String ip = getlocalip();
                if (TextUtils.isEmpty(ip)) {
                    Toast.makeText(getBaseContext(), "ip地址异常，请检查网络是否连接", Toast.LENGTH_LONG).show();
                    return;
                }
                //启动线程 向服务器发送信息//需要换成服务器端的IP地址
                sendStringMessage(msg, "10.0.0.8");
                break;
            case R.id.btu_send_file:
                String path = editText.getText().toString().trim();
                if (TextUtils.isEmpty(path)) {
                    Toast.makeText(getBaseContext(), "请输入要发送的文件路径", Toast.LENGTH_LONG).show();
                    return;
                }
                String ips = getlocalip();
                if (TextUtils.isEmpty(ips)) {
                    Toast.makeText(getBaseContext(), "ip地址异常，请检查网络是否连接", Toast.LENGTH_LONG).show();
                    return;
                }
                //启动线程 向服务器发送文件//需要换成服务器端的IP地址
                sendFileMessage(path, "10.0.0.8");
                break;
            default:
                break;
        }
    }

    /**
     * 或取本机的ip地址
     */
    private String getlocalip() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        if (ipAddress == 0) {
            return null;
        }
        return ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
                + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
    }

    /**
     * 创建服务端ServerSocket
     * 接收文本消息
     */
    private void createStringServerSocket() {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                Bundle bundle = new Bundle();
                bundle.clear();
                OutputStream out;
                //给发送端返回一个消息，告诉他链接接收成功。
                String str = "200";
                try {
                    ServerSocket serverSocket = new ServerSocket(StringProt);
                    while (true) {
                        try {
                            //此处是线程阻塞的,所以需要在子线程中
                            Socket socket = serverSocket.accept();
                            //请求成功，响应客户端的请求
                            out = socket.getOutputStream();
                            out.write(str.getBytes("utf-8"));
                            out.flush();
                            socket.shutdownOutput();
                            //获取输入流,读取客户端发送来的文本消息
                            BufferedReader bff = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String line = null;
                            while ((line = bff.readLine()) != null) {
                                buffer.append(line);
                            }
                            buffer.append("\n");
                            //
                            handler.sendEmptyMessage(1);
                            bff.close();
                            out.close();
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        };
        executorService.execute(run);
    }

    /**
     * 创建服务端ServerSocket
     * 接收文件
     */
    private void createFileServerSocket() {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                //给发送端返回一个消息，告诉他链接接收成功。
                String str = "200";
                try {
                    ServerSocket serverSocket = new ServerSocket(FileProt);
                    while (true) {
                        try {
                            //此处是线程阻塞的,所以需要在子线程中
                            Socket socket = serverSocket.accept();
                            //请求成功，响应客户端的请求
                            OutputStream out = socket.getOutputStream();
                            out.write(str.getBytes("utf-8"));
                            out.flush();
                            socket.shutdownOutput();
                            //获取输入流,读取客户端发送来的文件
                            DataInputStream dis = new DataInputStream(socket.getInputStream());
                            // 文件名和长度
                            String fileName = dis.readUTF();
                            //接收到的文件要存储的位置
                            File directory = new File(sdPath + "/imgage/");
                            if (!directory.exists()) {
                                directory.mkdirs();
                            }
                            FileOutputStream fos = new FileOutputStream(directory.getAbsolutePath() + "/" + fileName);
                            // 开始接收文件
                            byte[] bytes = new byte[1024];
                            int length = 0;
                            while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                                fos.write(bytes, 0, length);
                                fos.flush();
                            }
                            dis.close();
                            fos.close();
                            out.close();
                            socket.close();
                            handler.sendEmptyMessage(3);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        };
        executorService.execute(run);
    }

    /**
     * 启动线程 向服务器发送文本消息
     */
    private void sendStringMessage(final String txt1, final String ip) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket();
                    //端口号为30000
                    socket.connect(new InetSocketAddress(ip, StringProt));
                    //获取输出流
                    OutputStream ou = socket.getOutputStream();
                    //读取服务器响应
                    BufferedReader bff = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                    String line = null;
                    String buffer = "";
                    while ((line = bff.readLine()) != null) {
                        buffer = line + buffer;
                    }
                    //向服务器发送文本信息
                    ou.write(txt1.getBytes("utf-8"));
                    //关闭各种输入输出流
                    ou.flush();
                    bff.close();
                    ou.close();
                    socket.close();
                    //服务器返回
                    Message message = new Message();
                    message.what = 2;
                    message.obj = buffer;
                    handler.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        executorService.execute(run);
    }

    /**
     * 启动线程 向服务器发送文件
     */
    private void sendFileMessage(final String fliePath, final String ip) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, FileProt));
                    //获取输出流
                    OutputStream ou = socket.getOutputStream();
                    //读取服务器响应
                    BufferedReader bff = new BufferedReader(new InputStreamReader(
                            socket.getInputStream()));
                    String line = null;
                    String buffer = "";
                    while ((line = bff.readLine()) != null) {
                        buffer = line + buffer;
                    }
                    //向服务器发送文件
                    File file = new File(fliePath);
                    if (file.exists()) {
                        FileInputStream fileInput = new FileInputStream(fliePath);
                        DataOutputStream dos = new DataOutputStream(ou);
                        // 文件名和长度
                        dos.writeUTF(file.getName());
                        byte[] bytes = new byte[1024];
                        int length = 0;
                        while ((length = fileInput.read(bytes)) != -1) {
                            dos.write(bytes, 0, length);
                        }
                        //告诉服务端，文件已传输完毕
                        socket.shutdownOutput();
                        fileInput.close();
                        dos.close();
                        //服务器返回码
                        Message message = new Message();
                        message.what = 2;
                        message.obj = buffer;
                        handler.sendMessage(message);
                    }
                    //关闭各种输入输出流
                    ou.flush();
                    bff.close();
                    ou.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        executorService.execute(run);
    }

    /**
     * 显示一些信息
     */
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    showTextView.setText("从客户端接收到消息：\n" + buffer.toString());
                    break;
                case 3:
                    showTextView.setText(showTextView.getText().toString() + "\n从客户端接收到文件：\n成功");
                    break;
                case 2:
                    showTextView.setText(showTextView.getText().toString() + "\n从服务器端接收到返回码：\n" + msg.obj.toString());
                    break;
                default:
                    break;
            }
        }
    };
}
