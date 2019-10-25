package geliang.sample.server;


import geliang.sample.server.handle.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener mListener;
    private List<ClientHandler> clientHandlers = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;

    public TCPServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            ClientListener listener = new ClientListener(port);
            mListener = listener;
            listener.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() throws IOException {
        if (mListener != null) {
            mListener.exit();
        }
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
        private ServerSocket server;
        private boolean done = false;

        private ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.println("服务器信息：" + server.getInetAddress() + " P: " + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();
            System.out.println("服务器准备就绪。。。");

            do {
                Socket client = null;
                try {
                    client = server.accept();
                } catch (IOException e) {
                    // e.printStackTrace();
                    continue;
                }

                try {
                    // 客户端异步线程
                    ClientHandler clientHandler = new ClientHandler(client, TCPServer.this);
                    // 读取数据并打印
                    synchronized (TCPServer.this) {
                        clientHandlers.add(clientHandler);
                    }
                    clientHandler.readToPrint();

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("客户端连接异常：" + e.getMessage());
                }

            } while (!done);
            System.out.println("服务器已关闭！");
        }

        private void exit() throws IOException {
            done = true;
            server.close();
        }
    }
}
