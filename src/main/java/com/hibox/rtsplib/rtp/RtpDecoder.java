package com.hibox.rtsplib.rtp;

import android.util.Log;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.Map;


public class RtpDecoder extends CumulativeProtocolDecoder {
    protected CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();

    public RtpDecoder() {
    }

    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        return this.parseRtp(in, out, session);
    }

    private boolean parseRtp(IoBuffer in, ProtocolDecoderOutput out, IoSession session) throws CharacterCodingException {
        int p = in.position();
        int len = 0;
//        byte[] bytes = in.array();
//        FileUtils.writeResoursToSDCard(FileUtils.ROOT_PATH, "test.rtp",  Arrays.copyOf(bytes, in.limit()));
        Integer streamId = (Integer) session.getAttribute("streamId");
        if (streamId != null && streamId.intValue() == 2) {
            byte[] response1 = new byte[in.limit()];
            in.get(response1);
            out.write(response1);
            return true;
        } else {
            RtpResponseModel response = (RtpResponseModel) session.getAttribute("rtsp-request");
            if (response == null) {
                response = new RtpResponseModel();
                session.setAttribute("rtsp-request", response);
            }

            if (response.getState() == 0) {
                String body = this.readLine(in).toString();
                Map headLines = this.readHeader(in);
                if (headLines == null) {
                    in.position(p);
                    return false;
                }

                int idx1 = this.findSpace(0, body);
                response.setVersion(body.substring(0, idx1));
                int idx2 = this.findNotSpace(idx1, body);
                int idx3 = this.findSpace(idx2, body);
                response.setCode(Integer.valueOf(body.substring(idx2, idx3)).intValue());
                response.setStateLine(body.substring(idx3).trim());
                response.getHeaders().putAll(headLines);
                len = headLines.containsKey("content-length") ? Integer.valueOf((String) headLines.get("content-length")).intValue() : 0;
                response.setState(1);
            }

            if (streamId != null && streamId.intValue() == 1) {
                streamId = Integer.valueOf(streamId.intValue() + 1);
                session.setAttribute("streamId", streamId);
            }

            if (len == 0) {
                session.removeAttribute("rtsp-request");
                out.write(response);
                return true;
            } else if (len <= in.remaining()) {
                byte[] body1 = new byte[len];
                in.get(body1);
                response.setBody(body1);
                session.removeAttribute("rtsp-request");
                out.write(response);
                return true;
            } else {
                Log.v("RtpDecoder","RtpDecoder false ");
                return false;
            }
        }
    }

    protected Map<String, String> readHeader(IoBuffer buffer) {
        HashMap header = new HashMap();
        StringBuilder sb = new StringBuilder();

        while (true) {
            sb.delete(0, sb.length());

            label37:
            while (true) {
                char line = (char) buffer.get();
                switch (line) {
                    case '\n':
                        break label37;
                    case '\r':
                        line = (char) buffer.get();
                        if (line == 10) {
                            break label37;
                        }
                }

                sb.append(line);
            }

            if (sb.length() == 0) {
                return header;
            }

            String var7 = sb.toString();

            int i;
            for (i = 0; i < var7.length(); ++i) {
                char c = var7.charAt(i);
                if (c == 58) {
                    break;
                }

                if (Character.isWhitespace(c)) {
                    ++i;

                    while (i < var7.length()) {
                        c = var7.charAt(i);
                        if (c == 58) {
                            break;
                        }

                        ++i;
                    }
                }
            }

            header.put(var7.substring(0, i).trim().toLowerCase(), var7.substring(i + 1).trim());
        }
    }

    private int findNotSpace(int b, String line) {
        int i;
        for (i = b; i < line.length() && Character.isWhitespace(line.charAt(i)); ++i) {
            ;
        }

        return i;
    }

    private int findSpace(int b, String line) {
        int i;
        for (i = b; i < line.length() && !Character.isWhitespace(line.charAt(i)); ++i) {
            ;
        }

        return i;
    }

    private StringBuilder readLine(IoBuffer buffer) {
        StringBuilder sb = new StringBuilder();

        byte nextByte;
        do {
            while (true) {
                nextByte = buffer.get();
                if (nextByte == 13) {
                    nextByte = buffer.get();
                    break;
                }

                if (nextByte == 10) {
                    return sb;
                }

                sb.append((char) nextByte);
            }
        } while (nextByte != 10);

        return sb;
    }
}
