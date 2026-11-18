package com.alphadraft.skeleton.league;

// Sends one message to one player. The league code depends on this interface, not on a mail server.
public interface Notifier {

    void send(String to, String subject, String body);
}
