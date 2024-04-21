package backend.utils;

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.GeneratedMessageV3;

import java.io.*;

public class Sender {
    /**
     * This function will "send" the data(should follow Google Protocol Buffer) to corresponding output stream.
     * You should handle the timeout by yourself.
     * @param msg the message to be send
     * @param out output stream
     * @param <T> generic type of the data
     * @return send result
     */
    public static <T extends GeneratedMessageV3> boolean sendMsgTo(T msg, OutputStream out) {
        try {
            byte[] data = msg.toByteArray();
            CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(out);
            codedOutputStream.writeUInt32NoTag(data.length);
            codedOutputStream.writeRawBytes(data);
            // NOTE!!! always flush the result to stream
            codedOutputStream.flush();
            return true;
        } catch (IOException e) {
            System.err.println("sendToWorld: " + e.toString());
            return false;
        }
    }

    // public static <T extends GeneratedMessageV3> boolean sendMessage(T message, OutputStream outputStream) {
    //     try {
    //         byte[] data = message.toByteArray();
    //         CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
    //         writeMessageLength(codedOutputStream, data.length);
    //         writeMessageData(codedOutputStream, data);
    //         codedOutputStream.flush();
    //         return true;
    //     } catch (IOException e) {
    //         logError("Failed to send message", e);
    //         return false;
    //     }
    // }
    
    // private static void writeMessageLength(CodedOutputStream outputStream, int length) throws IOException {
    //     outputStream.writeUInt32NoTag(length);
    // }
    
    // private static void writeMessageData(CodedOutputStream outputStream, byte[] data) throws IOException {
    //     outputStream.writeRawBytes(data);
    // }
    
    // private static void logError(String message, IOException exception) {
    //     System.err.println(message + ": " + exception.toString());
    // }
    
}