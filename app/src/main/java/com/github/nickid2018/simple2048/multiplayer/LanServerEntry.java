package com.github.nickid2018.simple2048.multiplayer;

import java.net.InetAddress;
import java.util.Objects;

public class LanServerEntry {

    public final String name;
    public final String link;

    public LanServerEntry(String data, InetAddress address) {
        String host = address.getHostAddress();
        String[] entries = data.split(";", 2);
        String[] portData = entries[0].split("=", 2);
        if (!portData[0].equals("port"))
            throw new IllegalArgumentException("Unknown announcement");
        int port = Integer.parseInt(portData[1]);
        link = host + ":" + port;
        String[] nameData = entries[1].split("=", 2);
        if (!nameData[0].equals("name"))
            throw new IllegalArgumentException("Unknown announcement");
        name = nameData[1];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanServerEntry that = (LanServerEntry) o;
        return Objects.equals(name, that.name) && Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, link);
    }

    @Override
    public String toString() {
        return "LanServerEntry{" +
                "name='" + name + '\'' +
                ", link='" + link + '\'' +
                '}';
    }
}
