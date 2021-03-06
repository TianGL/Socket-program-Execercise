package geliang.sample.server;


import geliang.library.clink.core.IoContext;
import geliang.library.clink.impl.IoSelectorProvider;
import geliang.simple.foo.constants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    public static void main(String[] args) throws IOException {

        IoContext.setUp().ioProvider(new IoSelectorProvider()).start();

        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();

        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
        }

        UDPProvider.start(TCPConstants.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            tcpServer.broadcast(str);
        }  while (!"00bye00".equalsIgnoreCase(str));

        tcpServer.stop();
        UDPProvider.stop();

        IoContext.close();
    }
}
