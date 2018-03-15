package com.njust.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MutiplexerTimeServer implements Runnable {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private boolean stop;


    /*构造方法，在构造方法中进行资源的初始化，创建多路复用器Selector,ServerSocketChannel;
    * 对Channel的TCP参数进行配置，例如：
    * 1.serverSocketChannel.configureBlocking(false)设置为异步非阻塞
    * 2.serverSocketChannel.bind(new InetSocketAddress(port),1024)backlog设置为1024
    * 3.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)，将serverSocketChannel注册到selector，
    * 并监控SelectionKey.OP_ACCEPT的操作位
    *
    * 如果初始化失败（例如端口被占用），则退出*/
    // 初始化多路复用器，并绑定监听器
    public MutiplexerTimeServer(int port) {
        try{
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            // 配置为非阻塞
            serverSocketChannel.configureBlocking(false);
            //绑定
            serverSocketChannel.bind(new InetSocketAddress(port),1024);
            //注册，并监控
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("the time server is start in port : " + port);
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop(){
        this.stop = true;
    }

    @Override
    /*通过while循环体循环遍历selector，休眠时间设置为1000，即1s。
    * 无论是否有读写等事件发生，selector每隔1s就被唤醒一次*/
    public void run() {
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
            }catch (Throwable t){
                t.printStackTrace();
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


    /*处理新介入的客户端请求信息，通过SelectionKey key即可获知网络事件的类型*/
    private void handleInput(SelectionKey key) throws IOException{
        if(key.isValid()){
            //处理新接入的请求信息
            if(key.isAcceptable()) {
                // 接受新连接
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                /* 接受到此通道套接字的连接。
                如果此通道处于非阻塞模式，那么在不存在挂起的连接时，此方法将直接返回 null。
                否则，在新的连接可用或者发生 I/O 错误之前会无限期地阻塞它。
                不管此通道的阻塞模式如何，此方法返回的套接字通道（如果有）将处于阻塞模式。
                此方法执行的安全检查与 ServerSocket 类的 accept 方法执行的安全检查完全相同。
                也就是说，如果已安装了安全管理器，则对于每个新的连接，此方法都会验证安全管理器的
                 checkAccept 方法是否允许使用该连接的远程端点的地址和端口号。 */
                SocketChannel sc = ssc.accept();//到这里，相当于完成了TCP的三次握手，TCP的物理链路层正式建立
                // 设置为异步非阻塞
                sc.configureBlocking(false);
                // 将新连接注册到selector中，并监控
                sc.register(selector, SelectionKey.OP_READ);
            }

            // 读取客户端的请求信息
            if(key.isReadable()){
                //读取数据
                SocketChannel sc = (SocketChannel)key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // 将字节序列从此通道中读入给定的缓冲区。
                // 返回：读取的字节数，可能为零，如果该通道已到达流的末尾，则返回 -1
                int readBytes = sc.read(readBuffer);
                if(readBytes>0){
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
                    System.out.println("the time server receive order :" + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)?new java.util.Date
                            (System.currentTimeMillis()).toString():"BAD ORDER";
                    doWrite(sc, currentTime);
                }else if (readBytes<0){
                    // 关闭链路
                    key.cancel();
                    sc.close();
                }else ; //读到0字节，忽略
            }
        }
    }

    private  void doWrite(SocketChannel channel, String response) throws IOException{
        if(response!=null && response.trim().length()>0){
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);//此方法将给定的源 bytes 数组的所有内容传输到此缓冲区中。
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
    }
}
