package backend.utils;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessageV3;

import java.io.*;

public class Recver {
    /**
     * This function will only "read" the data from corresponding input stream.
     * You should handle the sendToWorld back ask by yourself.
     * @param response response(by reference)
     * @param in       input stream
     * @param <T>      generic type of the response
     * @return true receive successful
     */
    public static <T extends GeneratedMessageV3.Builder<?>> boolean recvMsgFrom(T response, InputStream in) {
        try {
            CodedInputStream codedInputStream = CodedInputStream.newInstance(in);
            int len = codedInputStream.readRawVarint32();
            int oldLimit = codedInputStream.pushLimit(len);
            response.mergeFrom(codedInputStream);
            codedInputStream.popLimit(oldLimit);
            return true;
        } catch (IOException e) {
            System.err.println("recv: " + e.toString());
            return false;
        }
    }
}