package geliang.sample.client;


import geliang.sample.client.bean.ServerInfo;

import java.io.*;

public class Client {
    public static void main(String[] args) {
        ServerInfo info = UDPSearcher.searcherServer(10000);
        System.out.println("Server: " + info);
        if (info != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(info);
                if (tcpClient == null) {
                    return;
                }
                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
    }

    private static void write(TCPClient tcpClient) throws IOException {
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            String string = input.readLine();
            tcpClient.send(string);

            if ("00bye00".equalsIgnoreCase(string)) {
                break;
            }
        } while (true);

    }

}
