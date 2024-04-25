package backend.utils;

import java.io.IOException;
import java.io.InputStream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessageV3;

public class Recver {
    public static <T extends GeneratedMessageV3.Builder<?>> boolean recvMsgFrom(T msg, InputStream in) {
        try {
            CodedInputStream CIS = CodedInputStream.newInstance(in);
            int len = CIS.readRawVarint32();
            int oldLimit = CIS.pushLimit(len);
            msg.mergeFrom(CIS);
            CIS.popLimit(oldLimit);
            return true;
        } catch (IOException e) {
            System.err.println(e.toString());
            return false;
        }
    }
}