package net.nexusrcon.nexusrconark.network;

import net.nexusrcon.nexusrconark.event.OnConnectListener;
import net.nexusrcon.nexusrconark.event.OnReceiveListener;
import net.nexusrcon.nexusrconark.event.ReceiveEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import roboguice.util.Ln;

/**
 * Created by Anthony on 09/10/2015.
 */
public class SRPConnection {

    private int sequenceNumber;

    private Thread connectionThread;
    private Thread receiveThread;
    private Thread connectionEventThread;


    private Socket client;
    private Map<Integer, Packet> outgoingPackets;

    private boolean runReceiveThread;
    private boolean runConnectionThread;
    private boolean isConnected = false;

    private String hostname;
    private OnReceiveListener onReceiveListener;

    private OnConnectListener onConnectListener;

    public SRPConnection() {
        client = new Socket();
        outgoingPackets = new ConcurrentHashMap<>();
    }

    public void open(final String hostname, final int port) {
        if (!isConnected) {
            runConnectionThread = true;
            connectionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        Ln.d("opening connection ...");
                        client.connect(new InetSocketAddress(hostname, port));

                        runReceiveThread = true;
                        beginReceive();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            connectionThread.setName("ConnectionThread");
            connectionThread.start();

            connectionEventThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (runConnectionThread) {

                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if (client.isConnected() && !isConnected) {
                            isConnected = true;
                            onConnectListener.onConnect();
                        }
                        if (!client.isConnected() && isConnected) {
                            isConnected = false;
                            onConnectListener.onDisconnect();
                        }
                    }
                }
            });
            connectionEventThread.setName("ConnectionEventThread");
            connectionEventThread.start();
        }
    }

    public synchronized int getSequenceNumber() {
        return ++sequenceNumber;
    }


    public void send(final Packet packet) throws IOException {

        if (client.isConnected()) {
            byte[] data = packet.encode();
            OutputStream outputStream = client.getOutputStream();
            outputStream.write(data);
            this.outgoingPackets.put(getSequenceNumber(), packet);

            Ln.e("Sending packet");

        } else {
            Ln.e("Connection is closed");
        }
    }

    private void beginReceive() {
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receive();
            }
        });
        receiveThread.setName("ReceiverThread");
        receiveThread.start();
    }

    private void receive() {
        if (runReceiveThread) {
            InputStream inputStream;
            try {

                byte[] response = new byte[4096];
                inputStream = client.getInputStream();

                inputStream.read(response, 0, response.length);

                Packet packet = new Packet(response);
                Ln.d("receive : " + packet.getBody());

                if (onReceiveListener != null) {
                    onReceiveListener.onReceive(new ReceiveEvent(SRPConnection.this, packet));
                }

                beginReceive();

            } catch (IOException e) {
                Ln.d("End of receiving : Connection closed");
            }
        }
    }

    public void setOnReceiveListener(OnReceiveListener onReceiveListener) {
        this.onReceiveListener = onReceiveListener;
    }

    public void setOnConnectListener(OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }
}