package com.anthonydenaud.arkrcon.network;

import com.anthonydenaud.arkrcon.event.OnServerStopRespondingListener;

import com.anthonydenaud.arkrcon.R;
import com.anthonydenaud.arkrcon.event.ConnectionListener;
import com.anthonydenaud.arkrcon.event.OnReceiveListener;
import com.anthonydenaud.arkrcon.event.ReceiveEvent;

import org.apache.commons.collections4.map.LinkedMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;


import timber.log.Timber;

public class SRPConnection {

    private static final int TIMEOUT = 10 * 1000; // 10 Seconds timeout

    private int sequenceNumber;

    private Thread connectionThread;
    private Thread receiveThread;

    private Socket client;
    private final LinkedMap<Integer, Packet> outgoingPackets;

    private boolean runReceiveThread;
    private boolean isConnected;

    private OnReceiveListener onReceiveListener;
    private ConnectionListener connectionListener;
    private OnServerStopRespondingListener onServerStopRespondingListener;
    private boolean reconnecting = false;

    private Date lastPacketTime;

    public SRPConnection() {
        outgoingPackets = new LinkedMap<>();
    }

    public void open(final String hostname, final int port) {
        Timber.d("Connecting to %s:%d ...", hostname, port);

        lastPacketTime = new Date();
        if (!isConnected) {
            client = new Socket();
            connectionThread = new Thread(() -> {
                try {
                    isConnected = true;
                    client.connect(new InetSocketAddress(hostname, port), TIMEOUT);
                    connectionListener.onConnect(reconnecting);
                    reconnecting = false;
                    Timber.d("Connected");
                    beginReceive();

                } catch (IOException e) {
                    isConnected = false;
                    if (connectionListener != null) {
                        connectionListener.onConnectionFail();
                    }
                }
            });
            connectionThread.setName("ConnectionThread");
            connectionThread.start();
        }
    }

    public synchronized int getSequenceNumber() {
        return ++sequenceNumber;
    }

    public void send(final Packet packet) {

        synchronized (outgoingPackets) {
            this.outgoingPackets.put(packet.getId(), packet);
        }


        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] data;
                try {
                    if (client != null && client.isConnected()) {
                        data = packet.encode();
                        OutputStream outputStream = client.getOutputStream();
                        outputStream.write(data);
                    } else {
                        isConnected = false;
                        Timber.w("Unable to send packet : connection closed.");
                    }
                } catch (IOException e) {
                    connectionListener.onConnectionDrop();
                }
            }
        });
        sendThread.setName("SendThread");
        sendThread.start();
    }

    private void beginReceive() {
        receiveThread = new Thread(this::receive);
        receiveThread.setName("ReceiverThread");
        receiveThread.start();
    }

    private void receive() {
        runReceiveThread = true;
        while (runReceiveThread) {
            InputStream inputStream;
            try {

                if (new Date().getTime() - lastPacketTime.getTime() > 3000) {
                    reconnect();
                }
                inputStream = client.getInputStream();
                byte[] packetSize = new byte[4];
                int packetSizeInt = 0;
                int sizeLength = inputStream.read(packetSize, 0, packetSize.length);
                if (sizeLength == 4 && !PacketUtils.isText(packetSize)) {
                    packetSizeInt = PacketUtils.getPacketSize(packetSize) + 10;
                }

                final byte[] response;
                if (!PacketUtils.isText(packetSize)) {
                    response = new byte[packetSizeInt];
                } else {
                    response = new byte[Packet.PACKET_MAX_LENGTH];
                }

                int responseLength = inputStream.read(response, 0, response.length);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(packetSize);
                byteArrayOutputStream.write(response);
                final byte[] packetBuffer = byteArrayOutputStream.toByteArray();


                if (responseLength > 0) {

                    if (PacketUtils.isStartPacket(packetBuffer)) {
                        final Packet packet = new Packet(packetBuffer);
                        if ((packet.getId() == -1 || packet.getId() > 0) && onReceiveListener != null) {
                            lastPacketTime = new Date();

                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    onReceiveListener.onReceive(new ReceiveEvent(SRPConnection.this, packet));
                                }
                            }, "ResponseExecThread");
                            thread.start();
                        }
                    } else {
                        final Packet lastPacket = outgoingPackets.get(outgoingPackets.lastKey());

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (lastPacket.getBody().equals("getgamelog")) {
                                    Packet packet = new Packet(lastPacket.getId(), PacketType.SERVERDATA_RESPONSE_VALUE.getValue(), new String(packetBuffer));
                                    onReceiveListener.onReceive(new ReceiveEvent(SRPConnection.this, packet));
                                } else if (lastPacket.getBody().equals("ListPlayers")) {
                                    Packet packet = new Packet(getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "ListPlayers");
                                    send(packet);
                                }
                            }
                        }, "ResponseExecThread");
                        thread.start();
                    }
                }
            } catch (IOException e) {
                Timber.w("Unable to receive packet : %s", e.getMessage());
                runReceiveThread = false;
            }
        }
    }

    public void reconnect() {
        if (!reconnecting) {
            reconnecting = true;
            try {
                close();
            } catch (IOException e) {
                Timber.e("Unable to close client : %s", e.getLocalizedMessage());
            }
            Timber.w("The server has stopped to responding to RCON requests, Reconnecting ...");
            if (onServerStopRespondingListener != null) {
                onServerStopRespondingListener.onServerStopResponding();
            }
        }
    }

    public void setOnReceiveListener(OnReceiveListener onReceiveListener) {
        this.onReceiveListener = onReceiveListener;
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void close() throws IOException {
        if (client != null) {
            client.close();
            isConnected = false;
            connectionThread.interrupt();

            if(runReceiveThread){
                receiveThread.interrupt();
                runReceiveThread = false;
            }
        }
        outgoingPackets.clear();
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public synchronized Packet getRequestPacket(int id) {
        return outgoingPackets.get((Integer) id);
    }

    public boolean isReconnecting() {
        return reconnecting;
    }

    public void setReconnecting(boolean reconnecting) {
        this.reconnecting = reconnecting;
    }

    public void setOnServerStopRespondingListener(OnServerStopRespondingListener onServerStopRespondingListener) {
        this.onServerStopRespondingListener = onServerStopRespondingListener;
    }
}
