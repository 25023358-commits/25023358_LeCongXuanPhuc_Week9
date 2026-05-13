package com.auction.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;

import com.auction.entity.Message;

public class ClientConnection {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ObjectMapper objectMapper = new ObjectMapper();

    public ClientConnection() throws IOException {
        socket = new Socket("localhost", 12345);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void sendMessage(Message msg) throws IOException {
        out.println(objectMapper.writeValueAsString(msg));
    }

    public Message receiveMessage() throws IOException {
        String line = in.readLine();
        return objectMapper.readValue(line, Message.class);
    }

    private Thread listenThread;
    private java.util.function.Consumer<Message> messageListener;

    public void setMessageListener(java.util.function.Consumer<Message> listener) {
        this.messageListener = listener;
        if (listenThread == null) {
            listenThread = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        Message msg = receiveMessage();
                        if (msg != null && messageListener != null) {
                            messageListener.accept(msg);
                        }
                    }
                } catch (IOException e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            });
            listenThread.setDaemon(true);
            listenThread.start();
        }
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}