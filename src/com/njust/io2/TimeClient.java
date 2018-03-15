package com.njust.io2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TimeClient {
    public static void main(String[] args) {
        int port = 8080;
        if(args!=null && args.length>0){
            try{
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){
                // 不进行处理
            }
        }

        BufferedReader in = null;
        PrintWriter out = null;
        Socket socket = null;
        try{
            socket = new Socket("127.0.0.1", port);
            // 输入流 服务端提供
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //  通过现有的 OutputStream 创建新的 PrintWriter:定义输出流的位置
            // 输出流 输出给服务端
            out = new PrintWriter(socket.getOutputStream(), true);
            String currentTime = null;
            out.println("QUERY TIME ORDER");
            System.out.println("Send order 2 server succeed");
            String resp = in.readLine();
            System.out.println("Now is : " + resp);
        }catch (Exception e){
            if(in != null){
                try {
                    in.close();
                }catch (IOException e1){
                    e1.printStackTrace();
                }
            }

            if(out != null){
                out.close();
                out = null;
            }

            if(socket != null){
                try{
                   socket.close();
                }catch (IOException e2){
                    e2.printStackTrace();
                }
            }
        }

    }
}
