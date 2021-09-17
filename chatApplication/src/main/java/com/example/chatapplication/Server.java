package com.example.chatapplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;

public class Server {

    private static ArrayList<String> users = new ArrayList<>();
    private static ArrayList<MessagingThread> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException, SQLException {
        ServerSocket server = new ServerSocket(8081, 10);
        System.out.println("Server is started");

        DbOperations.createUserTable("Users");
        DbOperations.createChatTable("Chat_Backup");

        while(true){
            Socket client = server.accept();
            MessagingThread thread = new MessagingThread(client);
            clients.add(thread);
            thread.start();
        }

    }

    public static void sendToAll(String userName, String message){
        for(MessagingThread client: clients){
            if(client.user.equals(userName)){
                client.sendMessageToMe(userName, message);
            }else{
                client.sendMessage(userName, message);
            }
        }
    }

    public static void sendChatOption(String userName, String message){
        for(MessagingThread client: clients){
            if(client.user.equals(userName)){
                client.sendChatOptionToMe(userName);
            }else{
                client.sendChatOptionToAll(message);
            }
        }
    }

    public static void removeChatOption(String userName){
        for(MessagingThread client: clients){
            if(client.user.equals(userName)){
                continue;
            }else{
                client.removeChat(userName);
            }
        }
    }

    static class MessagingThread extends Thread{

        BufferedReader input;
        PrintWriter output;
        String user;

        public MessagingThread(Socket client) throws IOException, SQLException {
            input = new BufferedReader(new InputStreamReader(client.getInputStream()));
            output = new PrintWriter(client.getOutputStream(), true);

            user = input.readLine();
            users.add(user);

            DbOperations.addUserInDb(user);

        }

        public void sendMessage(String userName, String message){
            output.println(userName+" : "+message);
        }

        public void sendMessageToMe(String userName, String message){
            output.println("Me: "+message);
        }

        public void sendChatOptionToMe(String userName){
            String msg = "";
            for(MessagingThread client: clients){
                if(client.user.equals(userName)){
                    continue;
                }else{
                    msg = msg.concat("$"+client.user);
                }
            }
            msg = msg.concat("$");
            output.println(msg);
        }

        public void sendChatOptionToAll(String msg){
            output.println(msg);
        }

        public void removeChat(String userName){
            output.println("&&"+userName);
        }

        public void saveInDb(String userName, String message) throws SQLException {
            String id = userName+"_"+System.currentTimeMillis();
            DbOperations.saveMessage(id, userName, message);
        }

        @Override
        public void run(){
            String line;
            try{
                sendChatOption(user, "$"+user+"$");
                while(true){
                    line = input.readLine();
                    if(line.equals("end")){
                        removeChatOption(user);
                        clients.remove(this);
                        users.remove(user);
                        break;
                    }else{
                        sendToAll(user, line);
                        saveInDb(user, line);
                    }
                }

            }catch (Exception ex){
                System.out.println(ex.getMessage());
            }
        }

    }
}
