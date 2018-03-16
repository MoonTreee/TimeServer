package com.njust.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TimeClientHandle  implements Runnable {
    private String host;
    private int port;
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop;

    // 构造函数
    public TimeClientHandle(String host, int port) {
        // 如果host为null，则默认为"127.0.0.1"
        this.host= host==null?"127.0.0.1":host;
        this.port = port;
        try {
            // 打开一个选择器。通过调用系统级默认 SelectorProvider 对象的 openSelector 方法来创建新的选择器。
            selector = Selector.open();
            // 打开套接字通道。通过调用系统级默认 SelectorProvider 对象的 openSocketChannel 方法来创建新的通道。
            socketChannel = SocketChannel.open();
            //异步非阻塞
            socketChannel.configureBlocking(false);
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            doConnect();
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
        while (!stop){
            try{
                // 选择一组键，其相应的通道已为 I/O 操作准备就绪
                selector.select(1000);
                //  返回此选择器的已选择键集。
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while(it.hasNext()){
                    key = it.next();
                    it.remove();
                    try{
                        handleInput(key);
                    }catch (Exception e){
                        if(key!=null){
                            key.cancel();
                            if(key.channel()!=null){
                                key.channel().close();
                            }
                        }
                    }

                }
            }catch (Exception e){
                e.printStackTrace();
                System.exit(1);
            }
        }


        //多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会自动的去注册并关闭，所以不需要重复释放资源
        if(selector!=null){
            try{
                selector.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    /**/
    private void handleInput(SelectionKey key) throws IOException{
        if(key.isValid()) {
            SocketChannel sc = (SocketChannel) key.channel();
            //判断是否连接成果，如果成功：
            if (key.isConnectable()) {
                //完成套接字通道的连接过程。当且仅当已连接此通道的套接字时才返回 true
                if (sc.finishConnect()) {
                    sc.register(selector, SelectionKey.OP_READ);
                    doWrite(sc);
                } else {
                    // 连接失败，退出
                    System.exit(1);
                }
            }

            // 读取服务端的消息
            if (key.isReadable()) {
                //读取数据
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // 将字节序列从此通道中读入给定的缓冲区。
                // 返回：读取的字节数，可能为零，如果该通道已到达流的末尾，则返回 -1
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0) {
                    /*
                        0 <= 标记 <= 位置 <= 限制 <= 容量
                    *  (Buffer)缓冲区是特定基本类型元素的线性有限序列。除内容外，缓冲区的基本属性还包括容量、限制和位置：
                    *  缓冲区的容量 是它所包含的元素的数量。缓冲区的容量不能为负并且不能更改。
                    *  缓冲区的限制 是第一个不应该读取或写入的元素的索引。缓冲区的限制不能为负，并且不能大于其容量。
                    *  缓冲区的位置 是下一个要读取或写入的元素的索引。缓冲区的位置不能为负，并且不能大于其限制。
                    *  对于每个非 boolean 基本类型，此类都有一个子类与之对应。 */

                    // 反转此缓冲区。首先将限制(limit)设置为当前位置(position)，然后将位置(position)设置为 0。
                    // 如果已定义了标记，则丢弃该标记。
                    readBuffer.flip(); //也就是说调用flip之后，读写指针指到缓存头部，并且设置了最多只能读出之前写入的数据长度(而不是整个缓存的容量大小)。
                    byte[] bytes = new byte[readBuffer.remaining()];//readBuffer.remaining()返回当前位置与限制之间的元素数。
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("Now is:" + body);
                    this.stop = true;
                } else if (readBytes < 0) {
                    // 关闭链路
                    key.cancel();
                    sc.close();
                } else ; //读到0字节，忽略
            }
        }
    }

    private void doConnect() throws IOException{
        // 如果直接连接成功，则注册到多路复用器上，发送请求消息，请求应答
        if(socketChannel.connect(new InetSocketAddress(host, port))){
            socketChannel.register(selector, SelectionKey.OP_READ);
            doWrite(socketChannel);
        }else {
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void doWrite(SocketChannel sc) throws IOException{
        byte[] req = "QUERY TIME ORDER".getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        sc.write(writeBuffer);
        if(!writeBuffer.hasRemaining()){
            System.out.println("Send order 2 server succeed.");
        }
    }


}
