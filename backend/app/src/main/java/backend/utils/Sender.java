package backend.utils;

import java.io.*;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.GeneratedMessageV3;

public class Sender {
    public static <T extends GeneratedMessageV3> void sendMessage(T msg, OutputStream out) {
        try {
            byte[] msgBytes = msg.toByteArray();
            //sendBytes(msgBytes, out);
            CodedOutputStream COS = CodedOutputStream.newInstance(out);
            COS.writeUInt32NoTag(msgBytes.length);
            COS.writeRawBytes(msgBytes);
            COS.flush();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }

    // private static void sendBytes(byte[] data, OutputStream out) throws IOException {
    //     try {
    //         out.write(data.length >> 24 & 0xFF);
    //         out.write(data.length >> 16 & 0xFF);
    //         out.write(data.length >> 8 & 0xFF);
    //         out.write(data.length & 0xFF);
    //         out.write(data);
    //         out.flush();
    //     } finally {
    //         if (out != null) {
    //             out.close();
    //         }
    //     }
    // }
    
}