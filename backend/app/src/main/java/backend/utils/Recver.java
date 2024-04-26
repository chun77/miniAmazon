package backend.utils;

import java.io.*;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessageV3;

public class Recver {
    public static <T extends GeneratedMessageV3.Builder<?>> void recvMessage(T msg, InputStream in) {
        try {
            CodedInputStream CIS = CodedInputStream.newInstance(in);
            int len = CIS.readRawVarint32();
            int oldLimit = CIS.pushLimit(len);
            msg.mergeFrom(CIS);
            CIS.popLimit(oldLimit);
        } catch (IOException e) {
            System.err.println(e.toString());  
        }
    }
}