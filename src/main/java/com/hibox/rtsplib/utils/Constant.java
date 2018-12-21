package com.hibox.rtsplib.utils;

import java.util.Locale;

/**
 * 常量类
 *
 * @description：
 * @date 2016-12-19 上午9:56:42
 */
public class Constant {

    public static final String LOG_TAG = "RTSP";
    public static final String USER = "admin";
    public static final String PSD = "tlJwpbo6";
    public static final String topIp = "192.168.1.10";
    public static final String assIp = "192.168.1.11";
    public static final String top2Ip = "192.168.1.30";
    public static final String ass2Ip = "192.168.1.31";
    public static final String RTSP_URL = String.format("rtsp://%s:554/user=admin_password=tlJwpbo6_channel=1_stream=0.sdp?real_stream",topIp);
    public static final String ASS_RTSP_URL = String.format("rtsp://%s:554/user=admin_password=tlJwpbo6_channel=1_stream=0.sdp?real_stream", assIp);
    public static final String RTSP_URL_2 = String.format("rtsp://%s:554/user=admin_password=tlJwpbo6_channel=1_stream=0.sdp?real_stream",top2Ip);
    public static final String ASS_RTSP_URL_2 = String.format("rtsp://%s:554/user=admin_password=tlJwpbo6_channel=1_stream=0.sdp?real_stream", ass2Ip);
}
