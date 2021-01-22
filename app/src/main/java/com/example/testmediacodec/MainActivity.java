package com.example.testmediacodec;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class MainActivity extends AppCompatActivity {
    public Button button;
    public String MIME="Video/AVC";
    public int videoW=176;
    public int videoH=144;
    public int videoBitrate=100000000;
    public int videoFrameRate=60;
    public MediaCodec codec = null;
    public InputStream is;
    public byte[] buffer;
    public byte[] tmp;
    int i=0;
    String path =  Environment.getExternalStorageDirectory()+"/output.h264" ;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println(path);

        //创建mediacodec;
        try {
            codec = MediaCodec.createEncoderByType(MIME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //配置mediacodec
        MediaFormat format = MediaFormat.createVideoFormat(MIME, videoW, videoH);
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //打开mediacodec
        codec.start();

        button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                //yuv文件转为变成多个byte[]依次编码
                        try {
                            is = getAssets().open("suzie_qcif176_144.yuv");
                            int size = is.available();
                            int len=0;
                            buffer = new byte[176*144*3/2];
                            while ((len=is.read(buffer))>0){
                                encode(buffer);  //编码
                            }
                            is.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        writeFile(path,tmp);  //保存

            }
        });
    }

    //编码
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void encode(byte[] data){
        try {
            //查询编码器可用输入缓冲区索引
            int inputBufferIndex = codec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                //根据输入缓冲区索引获取输入缓冲区
                ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                //将编码数据填充到输入缓冲区
                inputBuffer.put(data);
                //将填充好的输入缓冲器的索引提交给编码器
                codec.queueInputBuffer(inputBufferIndex, 0, data.length, System.currentTimeMillis(), 0);
            }

            //查询编码好的输出缓冲区索引
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                //根据索引获取输出缓冲区
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
                //从缓冲区获取编码成H264的byte[]
                byte[] outData = new byte[outputBuffer.remaining()];
                outputBuffer.get(outData, 0, outData.length);
                if(i==0){
                    tmp=outData;
                }else{
                    tmp=mergeBytes(tmp,outData);
                }

                codec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
                System.out.println("-----------------------------------------------------------------------------------------------------------------------------"+i++);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放资源
        if (codec != null) {
            codec.stop();
            codec.release();
            codec = null;
        }
    }
    //保存文件
    private void writeFile(String path, byte[] bytes) {
        try {
            FileOutputStream out = new FileOutputStream(path);//指定写到哪个路径中
            FileChannel fileChannel = out.getChannel();
            fileChannel.write(ByteBuffer.wrap(bytes)); //将字节流写入文件中
            fileChannel.force(true);//强制刷新
            fileChannel.close();
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //合并数组
    public static byte[] mergeBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }
}