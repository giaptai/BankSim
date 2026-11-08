package data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import data.models.Message;

public class Client {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final Scanner sc;
    private String displayName;
    private volatile String activeConversation;
    private final BlockingQueue<String> listRes;
    public void connect(String host, int port) throws IOException{

    }
    public void login() throws IOException{

    }
    private void startReceiver(){

    }
    private void handleIncoming(Message msg){

    }
    private void showMenu(){

    }
    private void privateChat() throws IOException, InterruptedException{

    }
    private void groupChat() throws IOException{

    }
    private void createGroup() throws IOException{

    }
    private void sendFile() throws IOException{

    }
    private void receiveFile(String fileName, long size) throws IOException{
        
    }

    public static void main(String[] args) throws IOException {

    }
}