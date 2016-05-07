package com.anthonydenaud.arkrcon.network;

import com.github.koraktor.steamcondenser.exceptions.SteamCondenserException;
import com.github.koraktor.steamcondenser.steam.SteamPlayer;
import com.github.koraktor.steamcondenser.steam.servers.GoldSrcServer;

import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import roboguice.util.Ln;

public class SteamQuery {

    private GoldSrcServer server;

    public void connect(String hostname, int port){
        try {
            server = new GoldSrcServer(hostname,port);
        } catch (SteamCondenserException e) {
            Ln.e(e);
        }
    }

    public HashMap<String,SteamPlayer> getPlayers(){

        HashMap<String, SteamPlayer> players = new HashMap<>();

        try {
            server.updatePlayers();
            players = server.getPlayers();
        } catch (SteamCondenserException | TimeoutException e) {
            Ln.e(e);
        }

        return players;
    }
}
