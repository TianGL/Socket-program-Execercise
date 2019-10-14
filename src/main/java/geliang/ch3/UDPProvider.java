package geliang.ch3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.UUID;

public class UDPProvider {
    public static void main(String[] args) throws IOException {
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn);
        provider.start();

        System.in.read();
        provider.exit();
    }

    private static class Provider extends Thread {
        private String sn;
        private boolean done = false;
        private DatagramSocket ds = null;

        public Provider(String sn) {
            this.sn = sn;
        }

        @Override
        public void run() {
            super.run();

            try {
                ds = new DatagramSocket(20000);

                while (!done) {
                    System.out.println("UDP Provider Started...");

                    final byte[] buf = new byte[512];
                    DatagramPacket receivePack = new DatagramPacket(buf, buf.length);

                    ds.receive(receivePack);

                    String ip = receivePack.getAddress().getHostAddress();
                    int port = receivePack.getPort();
                    int dataLen = receivePack.getLength();
                    String data = new String(receivePack.getData(), 0, dataLen);
                    System.out.println("UDPProvider receive from ip: " + ip + "\tPort: " + port + "\tdata: " + data);

                    int responsePort = MessageCreator.parsePort(data);

                    if (responsePort != -1) {
                        // 回送
                        String responseData = MessageCreator.buildWithSN(sn);
                        byte[] responseDataBytes = responseData.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(responseDataBytes, responseDataBytes.length,
                                receivePack.getAddress(), responsePort);

                        ds.send(responsePacket);
                    }

                }
            } catch (Exception e) {

            } finally {
                close();
            }
            System.out.println("UDPProvider Finished...");


        }


        private void close()
        {
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
