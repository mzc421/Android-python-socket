package com.example.android;

import static android.util.Base64.*;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private TextView predict;
    ImageView img_photo;
    private Button client_submit;
    final int TAKE_PHOTO = 1;
    Uri imageUri;
    File outputImage;

    private static final int UPDATE_ok = 0;
    private static final int UPDATE_UI = 1;
    private static final int ERROR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.setTitle("安全帽识别系统"); //设置标题栏的内容

        initViews();
        initEvent();
    }

    //初始化控件
    private void initViews() {
        img_photo = findViewById(R.id.img_photo);
        predict = findViewById(R.id.predict);
        client_submit = findViewById(R.id.client_submit);
    }

    //进行拍照
    private void initEvent() {
        client_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename = "test.png"; //自定义的照片名称
                outputImage = new File(getExternalCacheDir(),filename);  //拍照后照片存储路径
                try {if (outputImage.exists()){
                    outputImage.delete();
                }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (Build.VERSION.SDK_INT >= 24) {
                    //图片的url
                    imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.android.fileprovider", outputImage);
                } else {
                    imageUri = Uri.fromFile(outputImage);
                }
                //跳转界面到系统自带的拍照界面
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");  //调用手机拍照功能其实就是启动一个activity
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);  //指定图片存放位置，指定后，在onActivityResult里得到的Data将为null
                startActivityForResult(intent, TAKE_PHOTO);  //开启相机
            }
        });
    }

    //照片接收
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    // 使用try让程序运行在内报错
                    try {
                        //将图片显示
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        img_photo.setImageBitmap(bitmap);  //imageview控件显示刚拍的图片
                        //启动网络线程处理数据
                        startNetThread();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    /**利用bitmap将图片转换成Base64编码的字符串**/
    public static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                //压缩图片至100kb
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                baos.flush();
                baos.close();
                //接收压缩的图片数据流，并转换成base64编码
                byte[] bitmapBytes = baos.toByteArray();
                result = encodeToString(bitmapBytes, DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**利用图片路径将图片转换成Base64编码的字符串**/
    public static String imageToBase64(File path){
        InputStream is = null;
        byte[] data = null;
        String result = null;
        try{
            is = new FileInputStream(path);
            //创建一个字符流大小的数组。
            data = new byte[is.available()];
            //写入数组
            is.read(data);
            //用默认的编码格式进行编码
            result = encodeToString(data, NO_CLOSE);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null !=is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }

    /**图片压缩: 规定尺寸等比例压缩，宽高不能超过限制要求 @param beforBitmap 要压缩的图片 @param maxWidth 最大宽度限制 @param maxHeight 最大高度限 @return 压缩后的图片**/
    static public Bitmap compressBitmap(Bitmap beforBitmap, double maxWidth, double maxHeight) {
        // 图片原有的宽度和高度
        float beforeWidth = beforBitmap.getWidth();
        float beforeHeight = beforBitmap.getHeight();
        if (beforeWidth <= maxWidth && beforeHeight <= maxHeight) {
            return beforBitmap;
        }

        // 计算宽高缩放率，等比例缩放
        float scaleWidth =  ((float) maxWidth) / beforeWidth;
        float scaleHeight = ((float)maxHeight) / beforeHeight;
        float scale = scaleWidth;
        if (scaleWidth > scaleHeight) {
            scale = scaleHeight;
        }
        Log.d("BitmapUtils", "before[" + beforeWidth + ", " + beforeHeight + "] max[" + maxWidth
                + ", " + maxHeight + "] scale:" + scale);

        // 矩阵对象
        Matrix matrix = new Matrix();
        // 缩放图片动作 缩放比例
        matrix.postScale(scale, scale);
        // 创建一个新的Bitmap 从原始图像剪切图像
        Bitmap afterBitmap = Bitmap.createBitmap(beforBitmap, 0, 0,
                (int) beforeWidth, (int) beforeHeight, matrix, true);
        return afterBitmap;
    }

    //网络通信
    private void startNetThread() {
        predict.setText("预测中...");

        new Thread() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void run() {
                try {
                    Socket socket = new Socket();
                    InetSocketAddress isa = new InetSocketAddress("192.168.123.83", 6666);
                    socket.connect(isa, 5000);

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 告诉主线程一个消息，更新ui
                    Message msg1 = new Message();
                    // 消息的代号，是一个int类型
                    msg1.what = UPDATE_ok;
                    // 要传递的消息对象
                    msg1.obj = socket;
                    // 利用handler发送消息
                    handler.sendMessage(msg1);

                    //得到socket读写流
                    OutputStream os = socket.getOutputStream();
                    PrintWriter pw = new PrintWriter(os);

                    //将图片的路径转换成base64码
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    bitmap = compressBitmap(bitmap,bitmap.getWidth() / 4, bitmap.getWidth()/4);
                    String info = bitmapToBase64(bitmap);
//                    String info = imageToBase64(newfile);

                    //利用流按照一定的操作，对socket进行读写操作
                    pw.write(info);
                    pw.flush();

                    //关闭发送数据的数据流，数据发送完毕
                    socket.shutdownOutput();

                    BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    String content = br.readLine();

                    // 告诉主线程一个消息，更新ui
                    Message msg = new Message();
                    // 消息的代号，是一个int类型
                    msg.what = UPDATE_UI;
                    // 要传递的消息对象
                    msg.obj = content;
                    // 利用handler发送消息
                    handler.sendMessage(msg);

                    socket.close();
                    os.close();
                    br.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // 主线程创建消息处理器
    Handler handler = new Handler() {
        // 但有新消息时调用
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_UI) {
                // 获取消息对象
                if(msg.obj.equals("0")){
                    predict.setText("预测失败");
                }else{
                    String content = (String) msg.obj;
                    predict.setText(content);
                }
            } else if (msg.what == ERROR) {
                // Toast也是属于UI的更新
                Toast.makeText(getApplicationContext(), "预测失败", Toast.LENGTH_LONG).show();
            }else if (msg.what == UPDATE_ok){
                Toast.makeText(getApplicationContext(), "连接服务器成功", Toast.LENGTH_LONG).show();
            }
        }
    };
}

