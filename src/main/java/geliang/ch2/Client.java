package geliang.ch2;

import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();

        socket.setSoTimeout(3000);

        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), 2000), 3000);

        System.out.println("发起服务器连接");
        System.out.println("客户端信息：" + socket.getLocalAddress() + " P:" + socket.getLocalPort());
        System.out.println("服务端信息：" + socket.getInetAddress() + " P:" + socket.getPort());

        try {
            todo(socket);
        } catch (Exception e){

        }finally {
            socket.close();
        }

    }

    private static void todo(Socket client) throws IOException {
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        InputStream inputStream = client.getInputStream();
        BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean flag = true;

        do {
            String string = input.readLine();
            socketPrintStream.println(string);

            String echo = socketBufferedReader.readLine();
            if ("bye".equalsIgnoreCase(echo)) {
                flag = false;
            } else {
                System.out.println(echo);
            }
        } while (flag);
    }
}
