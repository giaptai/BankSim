package server.command;

import server.TextHandler;

public class HelpStrategy implements CommandStrategy {
    private static final String HELP_TEXT = """
            Commands:
                /list                           List all active users
                /msg <clientId> <text>          Send a private 1:1 message
                /file send <clientId> <path>    Send a file transfer request
                /file accept <transferId>       Accept a file transfer request
                /file decline <transferId>      Decline a file transfer request
                /file requests                  List pending file transfer requests
                /exit                           Disconnect from server
                /group <list|my|create|join|leave|members|send>                      
            """;

    @Override
    public void execute(TextHandler sender, String args) {
        sender.notifyMessage(HELP_TEXT);
    }

}
