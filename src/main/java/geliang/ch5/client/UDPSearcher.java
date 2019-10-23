package geliang.ch5.client;

import geliang.ch5.client.bean.ServerInfo;
import geliang.ch5.utils.ByteUtils;
import geliang.ch5.constants.UDPConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UDPSearcher {
    public static final int LISTEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE;

    public static ServerInfo searcherServer(int timeout) {
        System.out.println("UDPSearcher Started.");

        // 成功收到回送的栅栏
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            listener = listener(receiveLatch);
            sendBroadcast();
            receiveLatch.await(timeout, TimeUnit.MICROSECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 完成
        System.out.println("UDPSearcher Finished.");
        if (listener == null) {
            return null;
        }
        List<ServerInfo> serverInfoList = listener.getDevicesAndClose();
        if (serverInfoList.size() > 0) {
            return serverInfoList.get(0);
        }
        return null;
    }

    private static Listener listener(CountDownLatch receiveLatch) throws InterruptedException {
        System.out.println("UDPSearcher start listener.");
        CountDownLatch startDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, startDownLatch, receiveLatch);
        listener.start();
        startDownLatch.await();
        return listener;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("UDP Searcher sendBroadcast Started...");

        DatagramSocket ds = new DatagramSocket();

        // 构建请求数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        // 头部
        byteBuffer.put(UDPConstants.HEADER);
        // CMD命令
        byteBuffer.putShort((short)1);
        // 回送端口
        byteBuffer.putInt(LISTEN_PORT);

        // 构建请求Packet
        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        // 广播地址
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        // 设置服务器端口
        requestPacket.setPort(UDPConstants.PORT_SERVER);

        // 发送
        ds.send(requestPacket);
        System.out.println("UDP Searcher sendBroadcast Finished...");
        ds.close();
    }


    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch startDownLatch;
        private final CountDownLatch receiveDownLatch;
        private final List<ServerInfo> serverInfoList = new ArrayList<>();
        private final int minLen = UDPConstants.HEADER.length + 2 + 4;
        private boolean done = false;
        private DatagramSocket ds = null;

        public Listener(int listenPort, CountDownLatch startDownLatch, CountDownLatch receiveDownLatch) {
            this.listenPort = listenPort;
            this.startDownLatch = startDownLatch;
            this.receiveDownLatch = receiveDownLatch;
        }

        @Override
        public void run() {
            super.run();
            startDownLatch.countDown();
            try {

                ds = new DatagramSocket(listenPort);

                while (!done) {
                    // 接收
                    final byte[] buf = new byte[512];
                    DatagramPacket receivePack = new DatagramPacket(buf, buf.length);

                    ds.receive(receivePack);

                    String ip = receivePack.getAddress().getHostAddress();
                    int port = receivePack.getPort();
                    int dataLen = receivePack.getLength();
                    byte[] data = receivePack.getData();
                    boolean isValid = dataLen >= minLen && ByteUtils.startWith(data, UDPConstants.HEADER);
                    System.out.println("Searcher receive from ip: " + ip + "\tPort: " + port + "\tdataValid: " + isValid);

                    if (!isValid) {
                        continue;
                    }

                    ByteBuffer byteBuffer = ByteBuffer.wrap(data, UDPConstants.HEADER.length, dataLen);
                    final short cmd = byteBuffer.getShort();
                    final int serverPort = byteBuffer.getInt();
                    if (cmd != 2 || serverPort <= 0) {
                        System.out.println("UDPSearcher receive cmd: " + cmd + "\tserverPort: " + serverPort);
                    }

                    String sn = new String(buf, minLen, dataLen - minLen);
                    ServerInfo info = new ServerInfo(serverPort, ip, sn);
                    serverInfoList.add(info);
                    receiveDownLatch.countDown();
                }

            } catch (Exception e) {

            } finally {
                close();
            }
            System.out.println("UDPSearcher listener finished...");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        List<ServerInfo> getDevicesAndClose() {
            done = true;
            close();
            return serverInfoList;
        }
    }
}
