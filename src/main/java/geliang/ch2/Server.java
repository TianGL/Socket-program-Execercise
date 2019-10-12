package geliang.ch2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(2000);

        System.out.println("服务器启动");
        System.out.println("服务端信息：" + server.getInetAddress() + " P:" + server.getLocalPort());

        // 等待客户端连接
        for (;;) {
            Socket client = server.accept();
            ClientHandler handler = new ClientHandler(client);
            handler.start();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket server;
        private boolean flag = true;

        public ClientHandler(Socket socket) {
            this.server = socket;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("新客户端：" + server.getInetAddress() + " P:" + server.getPort());

            try {
                PrintStream socketOutput = new PrintStream(server.getOutputStream());
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(server.getInputStream()));
                do {
                    String string = socketInput.readLine();
                    if ("bye".equalsIgnoreCase(string)) {
                        flag = false;
                        socketOutput.println("bye");
                    } else {
                        System.out.println(string);
                        socketOutput.println("回送："+ string.length());
                    }

                } while (flag);
            } catch (Exception e) {

            } finally {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("客户端已经关闭...");
        }
    }


}
