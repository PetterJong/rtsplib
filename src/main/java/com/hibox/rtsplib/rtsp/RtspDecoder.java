package com.hibox.rtsplib.rtsp;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;


import com.hibox.rtsplib.utils.Constant;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RtspDecoder {
    //处理音视频的编解码的类MediaCodec
    private MediaCodec video_decoder;
    //显示画面的Surface
    private Surface surface;
    // 0: live, 1: playback, 2: local file
    private int state = 0;
    //视频数据
    private BlockingQueue<byte[]> video_data_Queue = new ArrayBlockingQueue<byte[]>(10000);
    //音频数据
    private BlockingQueue<byte[]> audio_data_Queue = new ArrayBlockingQueue<byte[]>(10000);

    private boolean isReady = false;
    private int fps = 0;

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private int frameCount = 0;
    private long deltaTime = 0;
    private long counterTime = System.currentTimeMillis();
    private boolean isRuning = false;

    public RtspDecoder(Surface surface, int playerState) {
        this.surface = surface;
        this.state = playerState;

    }

    public void stopRunning() {
        video_data_Queue.clear();
        audio_data_Queue.clear();
    }

    //添加视频数据
    public void setVideoData(byte[] data) {

        try {
            video_data_Queue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //添加音频数据
    public void setAudioData(byte[] data) {
        try {
            audio_data_Queue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public int getFPS() {
        return fps;
    }


    public void initial(byte[] sps) throws IOException {
        MediaFormat format = null;
        boolean isVGA = true;
        // use sps to check isVGAorHD?

        byte[] header_sps = {0, 0, 0, 1, 103, 100, 64, 41, -84, 44, -88, 10, 2, -1, -107};
        for (int i = 0; i < sps.length; i++) {
            if (header_sps[i] != sps[i]) {
                isVGA = false;
                break;
            }
        }
        if (isVGA) {
            format = MediaFormat.createVideoFormat("video/avc", 1920, 1088);
            byte[] header_pps = {0, 0, 0, 1, 104, -18, 56, -128};
            format.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
            format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 640 * 360);
        } else {
            format = MediaFormat.createVideoFormat("video/avc", 1920, 1088);
            byte[] header_HD_sps = {0, 0, 0, 1, 103, 100, 64, 41, -84, 44, -88, 5, 0, 91, -112};
            byte[] header_pps = {0, 0, 0, 1, 104, -18, 56, -128};
            format.setByteBuffer("csd-0", ByteBuffer.wrap(header_HD_sps));
            format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
            //		format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1280 * 720);
        }
        if (video_decoder != null) {
            video_decoder.stop();
            video_decoder.release();
            video_decoder = null;
        }
        video_decoder = MediaCodec.createDecoderByType("video/avc");
        if (video_decoder == null) {
            return;
        }

        video_decoder.configure(format, surface, null, 0);
        video_decoder.start();
        inputBuffers = video_decoder.getInputBuffers();
        outputBuffers = video_decoder.getOutputBuffers();
        frameCount = 0;
        deltaTime = 0;
        isRuning = true;
        runDecodeVideoThread();
    }

    /**
     * @description 解码视频流数据
     * @author ldm
     * @time 2016/12/20
     */
    private void runDecodeVideoThread() {

        Thread t = new Thread() {

            @SuppressLint("NewApi")
            public void run() {

                while (isRuning) {

                    int inIndex = -1;
                    try {
                        inIndex = video_decoder.dequeueInputBuffer(-1);
                    } catch (Exception e) {
                        return;
                    }
                    try {

                        if (inIndex >= 0) {
                            ByteBuffer buffer = inputBuffers[inIndex];
                            buffer.clear();

                            if (!video_data_Queue.isEmpty()) {
                                byte[] data;
                                data = video_data_Queue.take();
                                buffer.put(data);
                                if (state == 0) {
                                    video_decoder.queueInputBuffer(inIndex, 0, data.length, 66, 0);
                                } else {
                                    video_decoder.queueInputBuffer(inIndex, 0, data.length, 33, 0);
                                }
                            } else {
                                if (state == 0) {
                                    video_decoder.queueInputBuffer(inIndex, 0, 0, 66, 0);
                                } else {
                                    video_decoder.queueInputBuffer(inIndex, 0, 0, 33, 0);
                                }
                            }
                        } else {
                            video_decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }

                        int outIndex = video_decoder.dequeueOutputBuffer(info, 0);
                        switch (outIndex) {
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                outputBuffers = video_decoder.getOutputBuffers();
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                isReady = true;
                                break;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            default:

                                video_decoder.releaseOutputBuffer(outIndex, true);
                                frameCount++;
                                deltaTime = System.currentTimeMillis() - counterTime;
                                if (deltaTime > 1000) {
                                    fps = (int) (((float) frameCount / (float) deltaTime) * 1000);
                                    counterTime = System.currentTimeMillis();
                                    frameCount = 0;
                                }
                                break;
                        }

                        //所有流数据解码完成，可以进行关闭等操作
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.e(Constant.LOG_TAG, "BUFFER_FLAG_END_OF_STREAM");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }
        };

        t.start();
    }

}