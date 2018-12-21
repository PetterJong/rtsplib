package com.hibox.rtsplib.rtp;

public interface VideoStreamInterface {
    void onVideoStream(byte[] var1);
    void releaseResource();
}