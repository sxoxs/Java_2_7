package logik.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalTime;


public class ClientHandler {
    private static final int TIME_IDLE = 120;
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private static Connection connection;
    private static Statement stmt;
    private long time;

    public String getNick() {
        return nick;
    }

    private String nick;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            AuthService.connect();
            Thread threadIdle = new Thread(()->{
                while (true) {
                    if ( getIdleTime() > TIME_IDLE) {
                        closeIdle();
                        break;
                    }
                }
            });

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/auth")) {
                            String[] tokens = str.split(" ");
                            String newNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                            if (newNick != null) {
                                if (!server.isNickBusy(newNick)) {
                                    sendMsg("/authok");
                                    nick = newNick;
                                    server.subscribe(this);
                                    upTime();
                                    threadIdle.start();
                                    break;
                                } else {
                                    sendMsg("Учетная запись уже используется!");
                                }

                            } else {
                                sendMsg("Неверный логин/пароль");
                            }
                        }
                    }
                    while (true) {
                        String str;
                        upTime();
                        str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                out.writeUTF("/serverclosed");
                                break;
                            }
                            if (str.startsWith("/for")) {
                                String[] tokens = str.split(" ",3);
                                server.sendPersonalMsg(this, tokens[1], tokens[2]);
                            }

                            if (str.startsWith("/blacklist")) {
                                String[] tokens = str.split(" ");
                                AuthService.addBlackList(nick, tokens[1]);
                                sendMsg("Вы добавили пользователя " + tokens[1] + " в черный список");
                            }

                            if (str.startsWith("/whitelist")) {
                                String[] tokens = str.split(" ");
                                AuthService.deleteBlackList(nick, tokens[1]);
                                sendMsg("Вы исключили пользователя " + tokens[1] + " из черного списка");
                            }

                            if (str.startsWith("/history ")) {
                              StringBuilder stringBuilder = AuthService.getHistoryChat();
                              out.writeUTF(stringBuilder.toString());
                            }

                        } else {
                            LocalTime localTime = LocalTime.now();
                            AuthService.saveHistory(nick, str);
                            StringBuilder sb = new StringBuilder("");
                            sb.append(localTime.getHour() + ":" + localTime.getMinute() + " ");
                            sb.append(nick + ": " + str);
                            server.broadcastMsg(this, sb.toString(), nick);
                        }

                        System.out.println("Client: " + str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void upTime() {
        this.time = System.currentTimeMillis();
    }

    public int getIdleTime() {
        return (int) ((System.currentTimeMillis() - this.time)/1000);
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void closeIdle(){
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.unsubscribe(this);
    }
}
