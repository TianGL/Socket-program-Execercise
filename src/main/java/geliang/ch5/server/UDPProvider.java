package geliang.ch5.server;


import geliang.ch5.utils.ByteUtils;
import geliang.ch5.constants.UDPConstants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UDPProvider {

    private static Provider PROVIDER_INSTANCE;

    static void start(int port) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn.getBytes(), port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }


    private static class Provider extends Thread {
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;
        // 消息buffer
        final byte[] buffer = new byte[128];

        public Provider(byte[] sn, int port) {
            this.sn = sn;
            this.port = port;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("UDPProvider started...");

            try {
                // 监听端口
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                // 传输数据报
                DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    ds.receive(receivePack);

                    // print信息
                    String clientIP = receivePack.getAddress().getHostAddress();
                    int clientPort = receivePack.getPort();
                    int clientDataLen = receivePack.getLength();
                    byte[] clientData = receivePack.getData();
                    // 头长度+short指令（2字节）+port
                    boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4)
                            && ByteUtils.startWith(clientData, UDPConstants.HEADER);

                    System.out.println("UDPProvider revceive form ip: " + clientIP + "\tport: " + clientPort + "\tdataValid: " + isValid);

                    if (!isValid) {
                        continue;
                    }

                    // 解析命令
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
                    int responsePort = ((clientData[index++] << 24) | ((clientData[index++] & 0xff) << 16) |
                            ((clientData[index++] & 0xff) << 8) |
                            ((clientData[index++] & 0xff)));

                    if (cmd == 1 && responsePort > 0) {
                        // 构建会送
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short)2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();
                        // 直接根据发送者狗构建一份回送数据
                        DatagramPacket responsePacket = new DatagramPacket(buffer, len, receivePack.getAddress(),
                                responsePort);
                        ds.send(responsePacket);
                        System.out.println("UDPProvider response to: " + clientIP + "\tport: " + responsePort + "\tdataLen: " + len);
                    } else {
                        System.out.println("UDPProvider receive cmd nonsupport; cmd:" + cmd + "\tport: " + port);
                    }
                }

            } catch (Exception e) {

            }
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        void exit() {
            done = true;
            close();
        }
    }


}
