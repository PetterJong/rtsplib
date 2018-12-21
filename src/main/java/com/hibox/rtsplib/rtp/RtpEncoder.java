package com.hibox.rtsplib.rtp;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Map;

public class RtpEncoder extends ProtocolEncoderAdapter {
    protected CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();

    public RtpEncoder() {
    }

    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        if (message instanceof RtpRequestModel) {
            RtpRequestModel request = (RtpRequestModel) message;
            IoBuffer buffer = IoBuffer.allocate(128).setAutoExpand(true);
            StringBuilder sb = new StringBuilder();
            sb.append(request.getMethod() + " " + request.getUri() + " " + request.getVersion() + "\r\n");
            Map head = request.getHeaders();
            if (head != null && head.size() != 0) {
                Iterator i$ = head.entrySet().iterator();

                while (i$.hasNext()) {
                    Map.Entry entry = (Map.Entry) i$.next();
                    sb.append((String) entry.getKey() + ": " + (String) entry.getValue() + "\r\n");
                }
            }

            sb.append("\r\n");
            buffer.putString(sb.toString(), this.encoder);
            if (request.getBody() != null) {
                buffer.put(request.getBody());
            }

            buffer.flip();
            out.write(buffer);
        }

    }
}