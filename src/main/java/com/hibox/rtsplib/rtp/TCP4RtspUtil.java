package com.hibox.rtsplib.rtp;

import android.util.Log;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TCP4RtspUtil implements RtspInterface {
    private int port;
    private String host;
    private AtomicBoolean status;
    private NioSocketConnector connector;
    private IoSession session;
    private AtomicInteger seqState = new AtomicInteger(1);
    private Map<String, String> attribute;
    private String mediaUrl;
    private RequestEntry entry;
    private VideoStreamInterface listener;
    private int sessionTimeout;
    private Date lastSessionTime;

    public TCP4RtspUtil(String mediaUrl, VideoStreamInterface listener) {
        this.mediaUrl = mediaUrl;
        this.listener = listener;
        this.status = new AtomicBoolean(false);
        this.attribute = new HashMap();
    }

    private void parseUrl(String mediaUrl) {
        String hostname = mediaUrl.replaceAll("rtsp://", "");
        hostname = hostname.substring(0, hostname.indexOf("/"));
        String[] arr = hostname.split(":");
        this.host = arr[0];
        this.port = Integer.valueOf(arr[1]).intValue();
    }

    public void doStart() throws IOException {
        if (this.status.compareAndSet(false, true)) {
            this.parseUrl(this.mediaUrl);
            this.connector = new NioSocketConnector();
            this.connector.setConnectTimeoutMillis(5000L);
            this.connector.getSessionConfig().setReadBufferSize(8192);
            this.connector.getSessionConfig().setMaxReadBufferSize(65536);
            this.connector.getSessionConfig().setReceiveBufferSize(65536);
            this.connector.getSessionConfig().setSendBufferSize(65536);
            this.connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new RtpEncoder(), new RtpDecoder()));
            this.connector.getFilterChain().addLast("_io_c_write", new ExecutorFilter(8, new IoEventType[]{IoEventType.WRITE}));
            this.connector.setHandler(new TcpMediaClientHandler());
            ConnectFuture future = this.connector.connect(new InetSocketAddress(this.host, this.port));
            future.awaitUninterruptibly();
            this.session = future.getSession();
            this.initVedio();
        }

    }

    public void doStop() {
        if (this.status.compareAndSet(true, false)) {
            this.listener.releaseResource();
            this.connector.dispose(false);
        }

    }

    private void initVedio() throws IOException {
        RtpRequestModel request = new RtpRequestModel();
        request.setMethod("OPTIONS");
        request.setUri(this.mediaUrl);
        request.setVersion("RTSP/1.0");
        RtpResponseModel responseModel = this.options(request, 15000L);
        request.setMethod("DESCRIBE");
        request.getHeaders().clear();
        this.discribe(request, 15000L);
    }

    /**
     * 询问S有哪些方法可用
     * @param message
     * @param timeout
     * @return
     * @throws IOException
     */
    private RtpResponseModel options(RtpRequestModel message, long timeout) throws IOException {
        if (this.seqState.get() == 1) {
            this.entry = new RequestEntry();
            message.getHeaders().put("CSeq", this.seqState.get() + "");
            this.session.write(message);
            return (RtpResponseModel) this.entry.get(timeout);
        } else {
            throw new IOException("invalidata request");
        }
    }

    /**
     * 检查演示或媒体对象的描述，也允许使用接收头指定用户理解的描述格式。DESCRIBE的答复-响应组成媒体RTSP初始阶段
     * @param message
     * @param timeout
     * @return
     * @throws IOException
     */
    private RtpResponseModel discribe(RtpRequestModel message, long timeout) throws IOException {
        if (this.seqState.get() != 2) {
            throw new IOException("valida seqState :" + this.seqState.get());
        } else {
            this.entry = new RequestEntry();
            message.getHeaders().put("CSeq", this.seqState.get() + "");
            message.getHeaders().put("Accept", "application/sdp");
            this.session.write(message);
            RtpResponseModel resp = (RtpResponseModel) this.entry.get(timeout);
            if (resp != null) {
                String bodyLine = new String(resp.getBody());
                this.attribute.put("base_line", bodyLine);
                StringTokenizer st = new StringTokenizer(bodyLine, "\r\n");

                String control;
                while (st.hasMoreTokens()) {
                    control = st.nextToken().trim();
                    if (control.startsWith("m=audio")) {
                        break;
                    }

                    if (control.contains("a=range:")) {
                        this.attribute.put("Range", control.substring(control.indexOf(":") + 1));
                    } else if (control.contains("a=control:")) {
                        this.attribute.put("Control", control.substring(control.indexOf(":") + 1));
                    }
                }

                control = (String) this.attribute.get("Control");
                if (control != null && !"".equals(control.trim()) && !control.startsWith("rtsp://")) {
                    this.attribute.put("Control", this.mediaUrl + "/" + control.trim());
                }
            }

            return resp;
        }
    }

    /**
     * 让服务器给流分配资源，启动RTSP连接。
     * setUp之后才能正常通信
     * @param timeout
     * @throws IOException
     */
    private void setup(long timeout) throws IOException {
        if (this.seqState.get() == 3) {
            this.entry = new RequestEntry();
            RtpRequestModel setup = new RtpRequestModel();
            setup.setMethod("SETUP");
            setup.setUri((String) this.attribute.get("Control"));
            setup.setVersion("RTSP/1.0");
            setup.getHeaders().put("Transport", "RTP/AVP/TCP;unicast;interleaved=2-3");
            setup.getHeaders().put("CSeq", this.seqState.get() + "");
            this.session.write(setup).addListener(new IoFutureListener() {
                public void operationComplete(IoFuture future) {
                    future.getSession().removeAttribute("streamId");
                }
            });
            RtpResponseModel resp = (RtpResponseModel) this.entry.get(timeout);
            if (resp != null) {
                String[] headers = ((String) resp.getHeaders().get("session")).split(";");
                this.attribute.put("Session", headers[0]);
                if ((headers.length > 1) && (headers[1].toLowerCase().indexOf("timeout") >= 0)) {
                    sessionTimeout = Integer.parseInt(headers[1].substring(headers[1].indexOf("=")+1));
                }
                else
                    sessionTimeout = 60;
            }
        } else {
            throw new IOException("valida seqState :" + this.seqState.get());
        }
    }

    /**
     * 启动SETUP分配流的数据传输。
     * @throws IOException
     */
    public void play() throws IOException {
        this.setup(15000L);
        if (this.seqState.get() == 4) {
            this.entry = null;
            RtpRequestModel play = new RtpRequestModel();
            play.setMethod("PLAY");
            play.setUri((String) this.attribute.get("Control"));
            play.setVersion("RTSP/1.0");
            play.getHeaders().put("CSeq", this.seqState.get() + "");
            play.getHeaders().put("Session", this.attribute.get("Session"));
            play.getHeaders().put("Range", this.attribute.get("Range") == null ? "npt=0.00-" : (String) this.attribute.get("Range"));
            this.session.setAttribute("streamId", Integer.valueOf(1));
            this.session.write(play);
            lastSessionTime = new Date();
        } else {
            throw new IOException("valida seqState :" + this.seqState.get());
        }
    }

    /**
     * 暂停播放。并未发送暂停命令，实时流未暂停，只是不处理收到的数据
     */
    public void pause() {
        this.seqState.compareAndSet(4, 5);
    }

    public void consume() {
        this.seqState.compareAndSet(5, 4);
    }

    public void tearDown() throws IOException {
        if (this.seqState.get() == 4) {
            final CountDownLatch latch = new CountDownLatch(1);
            RtpRequestModel stop = new RtpRequestModel();
            this.entry = null;
            stop.setMethod("TEARDOWN");
            stop.setUri((String) this.attribute.get("Control"));
            stop.setVersion("RTSP/1.0");
            stop.getHeaders().put("CSeq", this.seqState.get() + 1 + "");
            stop.getHeaders().put("Session", this.attribute.get("Session"));
            this.session.write(stop).addListener(new IoFutureListener() {
                public void operationComplete(IoFuture future) {
                    future.getSession().setAttribute("streamId", Integer.valueOf(3));
                    TCP4RtspUtil.this.seqState.decrementAndGet();
                    latch.countDown();
                }
            });

            try {
                latch.await(15000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException var4) {
                throw new IOException(var4);
            }
        } else {
            throw new IOException("invalidata request");
        }
    }

    /**
     * 发送保活请求数据
     * 请求检查URL指定的演示与媒体的参数值。没有实体体时，GET_PARAMETER也许能用来测试用户与服务器的连通情况
     * @throws IOException
     */
    public void getParameter() throws IOException {
        if (this.seqState.get() == 4) {
            this.entry = null;
            RtpRequestModel getparam = new RtpRequestModel();
            getparam.setMethod("GET_PARAMETER");
            getparam.setUri((String) this.attribute.get("Control"));
            getparam.setVersion("RTSP/1.0");
            getparam.getHeaders().put("CSeq", this.seqState.get() + "");
            getparam.getHeaders().put("Session", this.attribute.get("Session"));
            this.session.write(getparam);
        } else {
            throw new IOException("valida seqState :" + this.seqState.get());
        }
    }

    public Map<String, String> getAttribute() {
        return this.attribute;
    }

    public void setAttribute(Map<String, String> attribute) {
        this.attribute = attribute;
    }

    public String getMediaUrl() {
        return this.mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    class RequestEntry {
        Object message;
        CountDownLatch latch = new CountDownLatch(1);

        RequestEntry() {
        }

        public void fillResp(Object message) {
            this.message = message;
            TCP4RtspUtil.this.seqState.incrementAndGet();
            this.latch.countDown();
        }

        public Object get(long timeout) throws IOException {
            try {
                this.latch.await(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception var4) {
                throw new IOException(var4);
            }

            if (this.message == null) {
                throw new IOException("waite resp timout[" + timeout + "]");
            } else {
                return this.message;
            }
        }
    }

    public boolean isConnected() {
        return ((session != null) && (session.isConnected()));
    }

    class TcpMediaClientHandler extends IoHandlerAdapter {
        TcpMediaClientHandler() {
        }

        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            cause.printStackTrace();
        }

        public void sessionClosed(IoSession session) throws Exception {
            super.sessionClosed(session);
            TCP4RtspUtil.this.doStop();
            if(tcpMediaClientListener != null)
                tcpMediaClientListener.sessionClosed();
        }

        public void messageReceived(IoSession session, Object message) throws Exception {
            if (message instanceof RtpResponseModel) {
                RtpResponseModel rtp = (RtpResponseModel) message;
                if (TCP4RtspUtil.this.entry != null) {
                    TCP4RtspUtil.this.entry.fillResp(rtp);
                }
            } else if (message instanceof byte[] && TCP4RtspUtil.this.listener != null && TCP4RtspUtil.this.seqState.get() == 4) { // this.seqState.get() == 4 表示播放
                TCP4RtspUtil.this.listener.onVideoStream((byte[]) ((byte[]) message));
                Date now = new Date();
                long diff = now.getTime() - lastSessionTime.getTime();
                if (diff >= sessionTimeout * 1000 - 1) {
                    getParameter();
                    lastSessionTime = now;
                }
            }

        }
    }

    private TcpMediaClientListener tcpMediaClientListener;

    public void setTcpMediaClientListener(TcpMediaClientListener tcpMediaClientListener) {
        this.tcpMediaClientListener = tcpMediaClientListener;
    }

    public interface TcpMediaClientListener {

        void sessionClosed();

    }
}
