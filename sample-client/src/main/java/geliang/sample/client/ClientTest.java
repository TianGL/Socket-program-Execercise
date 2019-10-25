package geliang.sample.client;

import geliang.sample.client.TCPClient;
import geliang.sample.client.UDPSearcher;
import geliang.sample.client.bean.ServerInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {
    private static boolean done = false;

    public static void main(String[] args) throws IOException {
        ServerInfo info = UDPSearcher.searcherServer(10000);
        System.out.println("Server: " + info);

        if (info == null) {
            return;
        }

        // 当前连接数量
        int size = 0;
        List<TCPClient> tcpClients = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(info);
                if (tcpClient == null) {
                    System.out.println("连接异常");
                    continue;
                }
                tcpClients.add(tcpClient);
                System.out.println("连接成功：" + (++size));
            }catch (Exception e) {
                System.out.println("连接异常");
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.in.read();
        System.out.println("run");
        Runnable runnable = () -> {
            while (!done) {
                int i = 0;
                for (TCPClient tcpClient : tcpClients) {
                    tcpClient.send("Hello~");
                    System.out.println(i);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();

        System.in.read();
        System.out.println("fin");
        // 等待线程完成
        done = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (TCPClient tcpClient : tcpClients) {
            tcpClient.exit();
        }

    }
}
