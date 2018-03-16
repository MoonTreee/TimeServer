package com.njust.nio;

import java.io.IOException;

public class NioTimeServer {
    public static void main(String[] args){
        // 默认值为8080
        int port = 8080;
        // 如果有参数，port设定读取参数的值
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }

        MutiplexerTimeServer timeServer = new MutiplexerTimeServer(port);
        new Thread(timeServer, "NIO-MutiplexerTimeServer-001").start();

    }
}
