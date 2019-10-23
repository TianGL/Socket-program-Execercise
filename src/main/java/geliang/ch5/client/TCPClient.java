package geliang.ch5.client;

import geliang.ch5.client.bean.ServerInfo;
import geliang.ch5.utils.CloseUtils;

import java.io.*;
import java.net.*;

public class TCPClient {
    public static void linkWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(3000);

        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getIp()), info.getServerPort()), 3000);

        System.out.println("已发起服务器连接，并进入后续流程~");
        System.out.println("客户端信息：" + socket.getLocalAddress() + " P:" + socket.getLocalPort());
        System.out.println("服务端信息：" + socket.getInetAddress() + " P:" + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();

            // 发送接收数据
            write(socket);

            // 退出
            readHandler.exit();
        } catch (Exception e) {
            System.out.println("异常关闭");
        }

        // 释放资源
        socket.close();
        System.out.println("客户端已退出~");
    }

    private static void write(Socket client) throws IOException {
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);


        do {
            String string = input.readLine();
            socketPrintStream.println(string);

            if ("00bye00".equalsIgnoreCase(string)) {
                break;
            }
        } while (true);

        socketPrintStream.close();
    }

    static class ReadHandler extends Thread {

        private boolean done = false;
        private final InputStream inputStream;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();
            try {
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));
                do {
                    String string = null;
                    try {
                        // 客户端拿到一条数据
                        string = socketInput.readLine();

                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    if (string == null) {
                        System.out.println("连接已经关闭，无法读取数据");
                        break;
                    }
                    // 打印到屏幕上
                    System.out.println(string);
                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开：" + e.getMessage());
                }

            } finally {
                CloseUtils.close(inputStream);
            }
            System.out.println("客户端已经关闭...");
        }
        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }
}
