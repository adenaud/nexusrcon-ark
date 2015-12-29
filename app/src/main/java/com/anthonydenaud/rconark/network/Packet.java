package com.anthonydenaud.rconark.network;


import com.anthonydenaud.rconark.exception.PacketParseException;

import org.apache.commons.lang3.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import roboguice.util.Ln;

/**
 * Created by Anthony on 09/10/2015.
 */
public class Packet {

    public static int PACKET_MAX_LENGTH = 8192;

    private int size;
    private int id;
    private int type;
    private String body;

    public Packet() {

    }

    public Packet(int id, int type, String body) {
        this.id = id;
        this.type = type;
        this.body = body;
    }

    public Packet(byte[] rawPacket) throws PacketParseException {
        decode(rawPacket);
    }

    public byte[] encode() throws IOException {
        ByteArrayOutputStream packetOutput = new ByteArrayOutputStream();


        packetOutput.write(getUint32Bytes(body.length() + 10));
        packetOutput.write(getUint32Bytes(id));
        packetOutput.write(getUint32Bytes(type));
        packetOutput.write((body + '\0').getBytes("US-ASCII"));
        packetOutput.write(0x00);


        return packetOutput.toByteArray();
    }

    public Packet decode(byte[] rawPacket) throws PacketParseException {

        size = getIntFromBytes(rawPacket, 0);
        id = getIntFromBytes(rawPacket, 4);
        type = getIntFromBytes(rawPacket, 8);
        body = getStringFromBytes(rawPacket, 12, size - 9);
        return this;
    }

    private int getIntFromBytes(byte[] data, int index) {
        byte[] res;
        res = Arrays.copyOfRange(data, index, index + 4);
        ArrayUtils.reverse(res);
        return (res[0] << 24) & 0xff000000 | (res[1] << 16) & 0x00ff0000 | (res[2] << 8) & 0x0000ff00 | (res[3]) & 0x000000ff;
    }

    private String getStringFromBytes(byte[] data, int index, int length) throws PacketParseException {
        String string = "";
        byte[] res;
        try {
            if (length > PACKET_MAX_LENGTH) {
                throw new PacketParseException("Error bad length ( > "+PACKET_MAX_LENGTH+" ) : " + length + " raw data : " + new String(data));
            }
            res = Arrays.copyOfRange(data, index, index + length - 1);
            string = new String(res, "US-ASCII");
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            res = "".getBytes();
            Ln.e("getStringFromBytes -> IllegalArgumentException : " + res.toString());
        }
        return string;
    }

    private byte[] getUint32Bytes(final int value) {
        byte[] result = ByteBuffer.allocate(4).putInt(value).array();
        ArrayUtils.reverse(result);
        return result;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
