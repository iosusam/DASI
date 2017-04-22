/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package icaro.aplicaciones.recursos.comunicacionChat.imp.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author FGarijo
 */
public class IdentServer extends Thread{
     /**
     * Constructs and starts an instance of an IdentServer that will
     * respond to a client with the provided login.  Rather than calling
     * this constructor explicitly from your code, it is recommended that
     * you use the startIdentServer method in the PircBot class.
     *  <p>
     * The ident server will wait for up to 60 seconds before shutting
     * down.  Otherwise, it will shut down as soon as it has responded
     * to an ident request.
     *
     * @param bot The PircBot instance that will be used to log to.
     * @param login The login that the ident server will respond with.
     */
      
    private ConexionIrc _bot;
    private String _login;
    private ServerSocket _ss = null;
    
    public IdentServer(ConexionIrc bot, String login) {
        _bot = bot;
        _login = login;

        try {
            _ss = new ServerSocket(113);
            _ss.setSoTimeout(60000);
        }
        catch (Exception e) {
            _bot.log("*** Could not start the ident server on port 113.");
            return;
        }
        
        _bot.log("*** Ident server running on port 113 for the next 60 seconds...");
        this.start();
    }
    
    
    /**
     * Waits for a client to connect to the ident server before making an
     * appropriate response.  Note that this method is started by the class
     * constructor.
     */
    public void run() {
        try {
            Socket socket = _ss.accept();
            socket.setSoTimeout(60000);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            String line = reader.readLine();
            if (line != null) {
                _bot.log("*** Ident request received: " + line);
                line = line + " : USERID : UNIX : " + _login;
                writer.write(line + "\r\n");
                writer.flush();
                _bot.log("*** Ident reply sent: " + line);
                writer.close();
            }
        }
        catch (Exception e) {
            // We're not really concerned with what went wrong, are we?
        }
        
        try {
            _ss.close();
        }
        catch (Exception e) {
            // Doesn't really matter...
        }
        
        _bot.log("*** The Ident server has been shut down.");
    }
  
    
}
