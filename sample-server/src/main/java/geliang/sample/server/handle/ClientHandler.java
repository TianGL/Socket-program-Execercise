package geliang.sample.server.handle;


import geliang.library.clink.core.Connector;
import geliang.library.clink.utils.CloseUtils;
import jdk.internal.org.objectweb.asm.ByteVector;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final SocketChannel socketChannel;
    private final Connector connector;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socketChannel = socketChannel;

        connector = new Connector(){
            @Override
            protected void onReceiveNewMessage(String str) {
                super.onReceiveNewMessage(str);
                clientHandlerCallback.onNewMessageArrived(ClientHandler.this, str);
            }

            @Override
            public void onChannelClosed(SocketChannel channel) {
                super.onChannelClosed(channel);
                exitBySelf();
            }
        };
        connector.setup(socketChannel);


        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);

        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端：" + clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void exit() {
        CloseUtils.close(connector);
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已经退出：" + clientInfo);
    }

    public void send(String str) {
        writeHandler.send(str);
    }

    private void exitBySelf() {
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        // 自身关闭通知
        void onSelfClosed(ClientHandler handler);

        // 收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);
    }


    class ClientWriteHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        public ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void send(String str) {
            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        void exit() {
            done = true;
            selector.wakeup();
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {

            private final String msg;

            public WriteRunnable(String msg) {

                this.msg = msg + "\n";
            }

            @Override
            public void run() {

                if (ClientWriteHandler.this.done) {
                    return;
                }

                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());
                // 反转操作，将指针从新指向起始位置
                byteBuffer.flip();

                while (!done && byteBuffer.hasRemaining()) {
                    try {
                        final int len = socketChannel.write(byteBuffer);
                        // len = 0合法
                        if (len < 0) {
                            System.out.println("客户端已经无法发送数据");
                            ClientHandler.this.exitBySelf();
                            break;
                        }

                    } catch (Exception e) {

                    }
                }
            }
        }
    }
}
