import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Color;

public class Server {

  private int port;
  private List<User> clients;
  private ServerSocket server;

  public static void main(String[] args) throws IOException {
    new Server(12345).run();
  }

  public Server(int port) {
    this.port = port;
    this.clients = new ArrayList<User>();
  }

  public void run() throws IOException {
    server = new ServerSocket(port) {
      protected void finalize() throws IOException {
        this.close();
      }
    };
    System.out.println("Port 12345 is now open.");

    while (true) {
      // accepts a new client
      Socket client = server.accept();

      // get nickname of newUser
      String nickname = (new Scanner ( client.getInputStream() )).nextLine();
      nickname = nickname.replace(",", ""); //  ',' use for serialisation
      nickname = nickname.replace(" ", "_");
      System.out.println("New Client: \"" + nickname + "\"\n\t     Host:" + client.getInetAddress().getHostAddress());

      // create new User
      User newUser = new User(client, nickname);

      // add newUser message to list
      this.clients.add(newUser);

      // Welcome msg
      newUser.getOutStream().println(
          "[+] Welcome " + nickname + "\n"
          );

      // create a new thread for newUser incoming messages handling
      new Thread(new UserHandler(this, newUser)).start();
    }
  }

  // delete a user from the list
  public void removeUser(User user){
    this.clients.remove(user);
  }

  // send incoming msg to all Users
  public void broadcastMessages(String msg, User userSender) {
    for (User client : this.clients) {
      client.getOutStream().println(
          "\n" + userSender.getNickname() + ": " + msg);
    }
  }
  
  // System notify
  public void serverNotify(String msg) {
    for (User client : this.clients) {
      client.getOutStream().println(
          "\n[!] " + msg);
    }
  }
}

class UserHandler implements Runnable {

  private Server server;
  private User user;

  public UserHandler(Server server, User user) {
    this.server = server;
    this.user = user;
  }

  public void run() {
    String message;

    // when there is a new message, broadcast to all
    Scanner sc = new Scanner(this.user.getInputStream());
    
    message = "User " + user.getNickname() + " has join the chat";
    this.server.serverNotify(message);
    
    while (sc.hasNextLine()) {
      message = sc.nextLine();

      server.broadcastMessages(message, user);
      
    }
    // end of Thread
    server.removeUser(user);
    message = "User " + user.getNickname() + " has left the chat";
    this.server.serverNotify(message);
    sc.close();
  }
}

class User {
  private static int nbUser = 0;
  private int userId;
  private PrintStream streamOut;
  private InputStream streamIn;
  private String nickname;
  private Socket client;
  private String color;

  // constructor
  public User(Socket client, String name) throws IOException {
    this.streamOut = new PrintStream(client.getOutputStream());
    this.streamIn = client.getInputStream();
    this.client = client;
    this.nickname = name;
    this.userId = nbUser;
    nbUser += 1;
  }

  // get output stream
  public PrintStream getOutStream(){
    return this.streamOut;
  }

  // get input stream
  public InputStream getInputStream(){
    return this.streamIn;
  }

  public String getNickname(){
    return this.nickname;
  }

}
