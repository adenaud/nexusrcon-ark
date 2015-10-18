package net.nexusrcon.nexusrconark.service;

import android.content.Context;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.nexusrcon.nexusrconark.R;
import net.nexusrcon.nexusrconark.event.ConnectionListener;
import net.nexusrcon.nexusrconark.event.OnReceiveListener;
import net.nexusrcon.nexusrconark.event.ReceiveEvent;
import net.nexusrcon.nexusrconark.event.ServerResponseDispatcher;
import net.nexusrcon.nexusrconark.event.ServerResponseEvent;
import net.nexusrcon.nexusrconark.model.Server;
import net.nexusrcon.nexusrconark.network.Packet;
import net.nexusrcon.nexusrconark.network.PacketType;
import net.nexusrcon.nexusrconark.network.SRPConnection;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import roboguice.util.Ln;

/**
 * Created by Anthony on 12/10/2015.
 */
@Singleton
public class ArkService implements OnReceiveListener {

    private final Context context;
    @Inject
    private SRPConnection connection;

    private ConnectionListener connectionListener;

    private List<ServerResponseDispatcher> serverResponseDispatchers;

    @Inject
    public ArkService(Context context)
    {
        this.context = context;
        serverResponseDispatchers = new ArrayList<>();
    }

    public void connect(final Server server) {
        connection.open(server.getHostname(), server.getPort());

        connection.setOnReceiveListener(this);
        connection.setConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect() {
                login(server.getPassword());
            }

            @Override
            public void onDisconnect() {

            }

            @Override
            public void onConnectionFail(String message) {
                if(connectionListener != null){
                    connectionListener.onConnectionFail(message);
                }
            }
        });


    }

    private void login(String password) {
        Packet packet = new Packet(connection.getSequenceNumber(),PacketType.SERVERDATA_AUTH.getValue(), password);
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listPlayers() {
        Packet packet = new Packet(connection.getSequenceNumber(),PacketType.SERVERDATA_EXECCOMMAND.getValue(), "ListPlayers");
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message) {

    }

    public void destroyWildDinos() {
        Packet packet = new Packet(connection.getSequenceNumber(),PacketType.SERVERDATA_EXECCOMMAND.getValue(), "DestroyWildDinos");
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTimeofDay(int hour, int minute) {
        String command = "SetTimeOfDay " + String.valueOf(hour) + ":" + String.valueOf(minute);
        Packet packet = new Packet(connection.getSequenceNumber(),PacketType.SERVERDATA_EXECCOMMAND.getValue(), command);
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveWorld() {
        Packet packet = new Packet(connection.getSequenceNumber(),PacketType.SERVERDATA_EXECCOMMAND.getValue(), "SaveWorld");
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(ReceiveEvent event) {
        Packet packet = event.getPacket();



        if (packet.getType() == PacketType.SERVERDATA_RESPONSE_VALUE.getValue()) {

            Packet requestPacket = connection.getRequestPacket(packet.getId());

            for (ServerResponseDispatcher dispatcher : serverResponseDispatchers) {

                if(StringUtils.isNotEmpty(requestPacket.getBody()) && requestPacket.getBody().equals("ListPlayers")){
                    dispatcher.onListPlayers(new ServerResponseEvent(packet));
                }

            }
        }

        if (packet.getType() == PacketType.SERVERDATA_AUTH_RESPONSE.getValue()) {
            if (packet.getId() == -1) {
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Ln.d("Auth Fail");
                connectionListener.onConnectionFail(context.getString(R.string.authentication_fail));
            } else {
                Ln.d("Auth success");
                connectionListener.onConnect();
            }
        }
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void disconnect() {
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addServerResponseDispatcher(ServerResponseDispatcher dispatcher) {
        this.serverResponseDispatchers.add(dispatcher);
    }

}
