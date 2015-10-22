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
import net.nexusrcon.nexusrconark.model.Player;
import net.nexusrcon.nexusrconark.model.Server;
import net.nexusrcon.nexusrconark.network.Packet;
import net.nexusrcon.nexusrconark.network.PacketType;
import net.nexusrcon.nexusrconark.network.SRPConnection;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private Thread chatThread;

    @Inject
    public ArkService(Context context) {
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
            public void onDisconnect() {}

            @Override
            public void onConnectionFail(String message) {
                if (connectionListener != null) {
                    connectionListener.onConnectionFail(message);
                }
            }
        });


    }

    private void login(String password) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_AUTH.getValue(), password);
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listPlayers() {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "ListPlayers");
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcast a message to all players on the server.
     * @param message
     */
    public void broadcast(String message) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "Broadcast " + message);
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a chat message to all currently connected players.
     * @param message
     */
    public void serverChat(String message){
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "ServerChat " + message);
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serverChatTo(Player player, String message){
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "ServerChatTo \"" + player.getSteamId() + "\" " + message);
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void destroyWildDinos() {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "DestroyWildDinos");
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTimeofDay(int hour, int minute) {
        String command = "SetTimeOfDay " + String.valueOf(hour) + ":" + String.valueOf(minute);
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), command);
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveWorld() {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "SaveWorld");
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Kills the specified player.
     *
     * @param player Player to kill
     */
    public void killPlayer(Player player) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "KillPlayer " + String.valueOf(player.getUe4Id()));
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Forcibly disconnect the specified player from the server.
     *
     * @param player Player to kick
     */
    public void kickPlayer(Player player) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "KickPlayer  " + player.getSteamId());
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add the specified player to the server's banned list.
     *
     * @param player
     */
    public void banPlayer(Player player) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "BanPlayer  " + player.getName());
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove the specified player from the server's banned list.
     * @param playerName
     */
    public void unBan(String playerName) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "UnbanPlayer " + playerName);
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Adds the player specified by the their Integer encoded Steam ID to the server's whitelist.
     *
     * @param player
     */
    public void allowPlayerToJoinNoCheck(Player player) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "AllowPlayerToJoinNoCheck " + player.getSteamId());
        try {
            connection.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  	Removes the specified player from the server's whitelist.
     * @param steamId
     */
    public void disallowPlayerToJoinNoCheck(String steamId){
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "DisallowPlayerToJoinNoCheck  " + steamId);
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

                if (StringUtils.isNotEmpty(requestPacket.getBody()) && requestPacket.getBody().equals("ListPlayers")) {
                    dispatcher.onListPlayers(getPlayers(packet.getBody()));
                }
                if (StringUtils.isNotEmpty(requestPacket.getBody()) && requestPacket.getBody().equals("getchat") && !packet.getBody().contains("Server received, But no response!!")) {
                    dispatcher.onGetChat(packet.getBody());
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

                startChatThread();
            }
        }
    }

    private void startChatThread() {
        chatThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (connection.isConnected()){

                    Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "getchat");
                    try {
                        connection.send(packet);
                        Thread.sleep(1000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        connectionListener.onDisconnect();
                    }
                }

            }
        }, "CHAT_THREAD");
        chatThread.start();
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

    private List<Player> getPlayers(String messageBody) {
        List<Player> players = new ArrayList<>();
        String[] playersArray = messageBody.split("\n");

        if (!messageBody.startsWith("No Players Connected")) {

            for (int i = 0; i < playersArray.length; i++) {
                if (playersArray[i].length() > 20) { // 20 = playerId + steamId min length

                    Pattern pattern = Pattern.compile("(\\d*)\\. (.+), ([0-9]+) ?");
                    Matcher matcher = pattern.matcher(playersArray[i]);

                    if (matcher.matches()) {

                        int ue4Id = Integer.parseInt(matcher.group(1));
                        String name = matcher.group(2);
                        String steamId = matcher.group(3);

                        Player player = new Player(ue4Id, name, steamId);
                        players.add(player);

                    }
                }
            }
        }
        return players;
    }
}
