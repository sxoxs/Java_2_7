package logik.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;

    public Server() throws SQLException {
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;
        try {
            AuthService.connect();
//            AuthService.addUser("login1", "pass1", "nick1", false);
//            AuthService.addUser("login2", "pass2", "nick2", false);
//            AuthService.addUser("login3", "pass3", "nick3", false);
//            AuthService.addUser("admin", "admin", "admin", true);
            server = new ServerSocket(8989);
            System.out.println("Сервер запущен. Ожидаем клиентов...");

            while (true) {
                socket = server.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public void subscribe(ClientHandler client) {
        clients.add(client);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler client) {
        clients.remove(client);
        broadcastClientList();
    }


    public void broadcastMsg(ClientHandler from, String msg, String clientNick) {
        for (ClientHandler o : clients) {
            if ((!AuthService.checkBlackList(o.getNick(), from.getNick())) &&
                    (!AuthService.checkBlackList("admin", from.getNick()))) {
                o.sendMsg(msg);
            }
        }
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientlist");
        for (ClientHandler o: clients) {
            sb.append(o.getNick() + " ");
        }
        String out = sb.toString();
        for(ClientHandler o: clients) {
            o.sendMsg(out);
        }
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler o: clients) {
            if(o.getNick().equalsIgnoreCase(nick)) {
                return true;
            }
        }
        return false;
    }

    public void sendPersonalMsg(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler o: clients) {
            if(o.getNick().equalsIgnoreCase(nickTo)) {
                o.sendMsg("from " + from.getNick() + ": " + msg);
                from.sendMsg("to " + nickTo + ": " + msg);
                return;
            }
        }
        from.sendMsg("Клиент с ником " + nickTo + " не найдеен в чате!");
    }



}
