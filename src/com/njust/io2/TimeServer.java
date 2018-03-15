package com.njust.io2;

import com.njust.bio.TimeServerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeServer {
    public static void main(String[] args) throws IOException {
        // 默认值为8080
        int port = 8080;
        // 如果有参数，port设定读取参数的值
        if(args!=null && args.length>0){
            try{
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){
                // 采用默认值
            }
        }

        ServerSocket  server = null;
        try{
            // 参数port指定服务器要绑定的端口（服务器要监听的端口）
            server = new ServerSocket(port);
            System.out.println("the time server is start in port : " + port);
            Socket socket = null;
            // 创建I/O任务线程池
            TimeServerHandlerExecutorPool singleExecutor = new TimeServerHandlerExecutorPool(50, 1000);
            // 通过一个无限循环来监听客户端的连接
            while (true){
                //当服务器执行ServerSocket的accept()方法时，如果连接请求队列为空，
                // 服务器就会一直等待，直到接收到了客户连接才从accept()方法返回。
                socket = server.accept();
                // TimeServerHandler是一个Runable
                //new Thread(new TimeServerHandlerExecutorPool(socket)).start();
                // 将socket封装成一个task
                singleExecutor.execute(new TimeServerHandler(socket));
            }
        }finally {
            if(server!=null){
                System.out.println("the time server is close");
                server.close();
                server = null;
            }
        }

    }

}
