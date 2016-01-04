package com.anthonydenaud.rconark.service;

import android.content.Context;

import com.anthonydenaud.rconark.event.OnServerStopRespondingListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.anthonydenaud.rconark.R;
import com.anthonydenaud.rconark.event.ConnectionListener;
import com.anthonydenaud.rconark.event.OnReceiveListener;
import com.anthonydenaud.rconark.event.ReceiveEvent;
import com.anthonydenaud.rconark.event.ServerResponseDispatcher;
import com.anthonydenaud.rconark.model.Player;
import com.anthonydenaud.rconark.model.Server;
import com.anthonydenaud.rconark.network.Packet;
import com.anthonydenaud.rconark.network.PacketType;
import com.anthonydenaud.rconark.network.SRPConnection;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import roboguice.util.Ln;

@Singleton
public class ArkService implements OnReceiveListener {

    private final Context context;
    @Inject
    private SRPConnection connection;

    private List<ConnectionListener> connectionListeners;

    private final List<ServerResponseDispatcher> serverResponseDispatchers;
    private Server server;
    private Timer chatTimer;
    private Timer logTimer;


    @Inject
    public ArkService(Context context) {
        this.context = context;
        connectionListeners = new ArrayList<>();
        serverResponseDispatchers = new ArrayList<>();
    }

    public void connect(final Server server) {

        this.server = server;

        connection.open(server.getHostname(), server.getPort());

        connection.setOnReceiveListener(this);
        connection.setConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect(boolean reconnecting) {
                login(server.getPassword());
            }

            @Override
            public void onDisconnect() {
                sendOnDisconnectEvent();
            }

            @Override
            public void onConnectionFail(String message) {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnectionFail(message);
                }
            }
        });

        connection.setOnServerStopRespondingListener(new OnServerStopRespondingListener() {
            @Override
            public void onServerStopResponding() {
                chatTimer.cancel();
                logTimer.cancel();
                connection.open(server.getHostname(), server.getPort());
            }
        });


    }

    private void login(String password) {
        Ln.d("Login ...");
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_AUTH.getValue(), password);
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
        }
    }

    public void listPlayers() {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "ListPlayers");
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
        }
    }

    /**
     * Broadcast a message to all players on the server.
     *
     * @param message
     */
    public void broadcast(String message) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "Broadcast " + message);
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
        }
    }

    private String getAdminName() {
        String adminName = context.getString(R.string.default_admin_name);
        if (StringUtils.isNotEmpty(server.getAdminName())) {
            adminName = server.getAdminName();
        }
        return adminName;
    }

    /**
     * Sends a chat message to all currently connected players.
     *
     * @param message
     */
    public void serverChat(String message) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "ServerChat " + getAdminName() + " : " + message);
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
        }
    }


    public void serverChatTo(Player player, String message) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "ServerChatTo \"" + player.getSteamId() + "\" " + getAdminName() + " : " + message);
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
        }
    }


    public void destroyWildDinos() {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "DestroyWildDinos");
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
        }
    }

    public void setTimeofDay(int hour, int minute) {
        String command = "SetTimeOfDay " + String.valueOf(hour) + ":" + String.valueOf(minute);
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), command);
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
        }
    }

    public void saveWorld() {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "SaveWorld");
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
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
            sendOnDisconnectEvent();
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
            sendOnDisconnectEvent();
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
            sendOnDisconnectEvent();
        }
    }

    /**
     * Remove the specified player from the server's banned list.
     *
     * @param playerName
     */
    public void unBan(String playerName) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "UnbanPlayer " + playerName);
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
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
            sendOnDisconnectEvent();
        }
    }

    /**
     * Removes the specified player from the server's whitelist.
     *
     * @param steamId
     */
    public void disallowPlayerToJoinNoCheck(String steamId) {
        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "DisallowPlayerToJoinNoCheck  " + steamId);
        try {
            connection.send(packet);
        } catch (IOException e) {
            sendOnDisconnectEvent();
        }
    }

    @Override
    public void onReceive(ReceiveEvent event) {
        Packet packet = event.getPacket();


        if (packet.getType() == PacketType.SERVERDATA_RESPONSE_VALUE.getValue()) {

            Packet requestPacket = connection.getRequestPacket(packet.getId());

            synchronized (serverResponseDispatchers) {
                for (ServerResponseDispatcher dispatcher : serverResponseDispatchers) {

                    if (requestPacket == null) {
                        Ln.e(String.valueOf(packet.getId()) + packet.getBody());
                    }

                    if (StringUtils.isNotEmpty(requestPacket.getBody()) && requestPacket.getBody().equals("ListPlayers")) {
                        dispatcher.onListPlayers(getPlayers(packet.getBody()));
                    }
                    if (StringUtils.isNotEmpty(requestPacket.getBody()) && requestPacket.getBody().equals("getchat") && !packet.getBody().contains("Server received, But no response!!")) {
                        dispatcher.onGetChat(packet.getBody());
                    }
                    if (StringUtils.isNotEmpty(requestPacket.getBody()) && requestPacket.getBody().equals("getgamelog") && !packet.getBody().contains("Server received, But no response!!")) {
                        dispatcher.onGetLog(packet.getBody());
                    }
                }
            }
        }

        if (packet.getType() == PacketType.SERVERDATA_AUTH_RESPONSE.getValue()) {
            if (packet.getId() == -1) {

                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnectionFail(context.getString(R.string.authentication_fail));
                }
                disconnect();
            } else {
                for (ConnectionListener listener : connectionListeners) {
                    listener.onConnect(connection.isReconnecting());
                }

                startLogAndChatTimers();
            }
        }
    }

    private void startLogAndChatTimers() {

        int chatDelay = context.getResources().getInteger(R.integer.chat_timer_delay);
        int logDelay = context.getResources().getInteger(R.integer.log_timer_delay);

        chatTimer = new Timer("ChatTimer");
        chatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (connection.isConnected()) {
                    try {
                        Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "getchat");
                        connection.send(packet);
                    } catch (IOException e) {
                        disconnect();
                        Ln.e("getchat exception : " + e.getMessage(), e);
                        connection.reconnect();
                    }
                }

            }
        },chatDelay, chatDelay);

        logTimer = new Timer("LogTimer");
        logTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (connection.isConnected()) {
                    Packet packet = new Packet(connection.getSequenceNumber(), PacketType.SERVERDATA_EXECCOMMAND.getValue(), "getgamelog");
                    try {
                        connection.send(packet);
                    } catch (IOException e) {
                        disconnect();
                        Ln.e("getgamelog exception : " + e.getMessage(), e);
                        connection.reconnect();
                    }
                }
            }
        }, logDelay, logDelay);
    }
    public void disconnect() {
        logTimer.cancel();
        chatTimer.cancel();
        connection.setReconnecting(false);
        try {
            connection.close();
        } catch (IOException e) {
            Ln.e("ark service disconnect exception", e);
        }
    }
    public void addServerResponseDispatcher(ServerResponseDispatcher dispatcher) {
        synchronized (serverResponseDispatchers) {
            if (!serverResponseDispatchers.contains(dispatcher)) {
                this.serverResponseDispatchers.add(dispatcher);
            }
        }
    }

    private List<Player> getPlayers(String messageBody) {
        List<Player> players = new ArrayList<>();
        String[] playersArray = messageBody.split("\n");

        if (!messageBody.startsWith("No Players Connected")) {

            for (String aPlayersArray : playersArray) {
                if (aPlayersArray.length() > 20) { // 20 = playerId + steamId min length

                    Pattern pattern = Pattern.compile("(\\d*)\\. (.+), ([0-9]+) ?");
                    Matcher matcher = pattern.matcher(aPlayersArray);

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

    private void sendOnDisconnectEvent() {
        for (ConnectionListener listener : connectionListeners) {
            listener.onDisconnect();
        }
        removeAllConnectionListener();
    }

    public synchronized void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    public synchronized void removeAllConnectionListener() {
        connectionListeners.clear();
    }
}


