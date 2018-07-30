package logik.server;

import java.sql.*;
import java.time.LocalDateTime;

public class AuthService {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:userDB.db");
            stmt = connection.createStatement();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void addUser(String login, String pass, String nick, boolean admin) {
        try {
            String query = "INSERT INTO main (login, password, nickname, administrate) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setInt(2, pass.hashCode());
            ps.setString(3, nick);
            ps.setBoolean(4, admin);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static String getNickByLoginAndPass(String login, String pass) {
        String sql = String.format("SELECT nickname, password FROM main\n" +
                "WHERE login = '%s'\n", login);
        int myHash = pass.hashCode();
        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String nick = rs.getString(1);
                int dbHash = rs.getInt(2);
                if(myHash == dbHash) {
                    return nick;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveHistory(String login, String msg) {
        LocalDateTime dateTime = LocalDateTime.now();
        String sql = String.format( "INSERT INTO history (post, nick, date, time) " +
                "VALUES ('%s', '%s', '%s', '%s')", msg, login, dateTime.getDayOfMonth() + "." + dateTime.getMonth() +
                 "." + dateTime.getYear(), dateTime.getHour() + ":" + dateTime.getMinute() );
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static StringBuilder getHistoryChat() {
        StringBuilder stringBuilder = new StringBuilder();
        String sql = String.format("SELECT nick, post, date, time from history ORDER BY ID");
        try {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                stringBuilder.append(rs.getString("date") + " " + (rs.getString("time")) + " ");
                stringBuilder.append(rs.getString("nick") + " " + rs.getString("post") + "\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stringBuilder;
    }

    public static void addBlackList(String nick, String blokNick) {
        try {
            String query = "INSERT INTO blacklist (nickname, bloc_nickname) VALUES (?, ?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1 , nick);
            ps.setString(2, blokNick);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteBlackList (String nick, String blocNick) {

        String sql = String.format("DELETE FROM blacklist" +
                "WHERE nickname = '" + nick + "' AND bloc_nickname = '" + blocNick +"'");

        String sq2 = String.format("DELETE FROM blacklist" +
                "WHERE nickname = '" + nick + "' AND bloc_nickname = '" + blocNick +"'");

        try {
            stmt.executeUpdate(sql);


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkBlackList (String nick, String blocNick) {
        try {
            String query = "SELECT * FROM blacklist";
            PreparedStatement ps = connection.prepareStatement(query);

            ResultSet resultSet = ps.executeQuery();

            while (resultSet.next()){
                if ( (nick.equalsIgnoreCase(resultSet.getString("nickname"))) &&
                        ( blocNick.equalsIgnoreCase(resultSet.getString("bloc_nickname"))) ) {
                    return true;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
