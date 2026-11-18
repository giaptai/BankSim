package server.command;

import server.TextHandler;

public class HelpStrategy implements CommandStrategy {
    private static final String HELP_TEXT = """
            SOAPTA / HELP

            GENERAL
                /list                           Online users
                /msg <clientId> <text>          Private message
                /exit                           Disconnect
            
            FILE TRANSFER
                /file send <clientId> <path>    Send a file
                /file accept <transferId>       Accept request
                /file decline <transferId>      Decline request
                /file requests                  Pending requests

            GROUP CHAT
                /group list                     List groups
                /group my                       My groups
                /group create <name>            Create group
                /group join <name>              Join group
                /group leave <name>             Leave group
                /group members <name>           List members
                /group send <groupId> <text>    Send group message                 
            """;

    @Override
    public void execute(TextHandler sender, String args) {
        sender.notifyMessage(HELP_TEXT);
    }

}
