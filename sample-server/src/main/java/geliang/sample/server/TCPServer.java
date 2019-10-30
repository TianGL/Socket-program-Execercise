package geliang.sample.server;


import geliang.library.clink.utils.CloseUtils;
import geliang.sample.server.handle.ClientHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener listener;
    private List<ClientHandler> clientHandlers = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;
    private Selector selector;
    private ServerSocketChannel server;

    public TCPServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            // 设置为非阻塞
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));

            System.out.println("服务器信息：" + server.getLocalAddress());
            server.register(selector, SelectionKey.OP_ACCEPT);
            this.server = server;

            // 启动监听
            ClientListener listener = this.listener = new ClientListener();
            listener.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() throws IOException {
        if (listener != null) {
            listener.exit();
        }

        CloseUtils.close(server);
        CloseUtils.close(selector);
        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.exit();
            }

            clientHandlers.clear();
        }

        // 停止线程池
        forwardingThreadPoolExecutor.shutdownNow();
    }

    public synchronized void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlers) {
            clientHandler.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        // 打印到屏幕
        System.out.println("Receive-" + handler.getClientInfo() + ": " + msg);

        // 异步转发
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler clientHandler : clientHandlers) {
                    if (clientHandler.equals(handler)) {
                        // 跳过自己
                        continue;
                    }
                    // 对其他客户端发送
                    clientHandler.send(msg);
                }
            }
        });
    }

    private class ClientListener extends Thread {
        private boolean done = false;

        @Override
        public void run() {
            super.run();
            Selector selector = TCPServer.this.selector;
            System.out.println("服务器准备就绪。。。");

            do {
                // 得到客户端
                try {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        // 检查key的状态
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            // 非阻塞状态拿到客户端连接
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            try {
                                // 客户端异步线程
                                ClientHandler clientHandler = new ClientHandler(socketChannel, TCPServer.this);
                                // 读取数据并打印
                                synchronized (TCPServer.this) {
                                    clientHandlers.add(clientHandler);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常：" + e.getMessage());
                            }
                        }
                    }

                } catch (IOException e) {
                     e.printStackTrace();
                }


            } while (!done);
            System.out.println("服务器已关闭！");
        }

        private void exit() throws IOException {
            done = true;
            // 唤醒阻塞
            selector.wakeup();
//            server.close();
        }
    }
}
