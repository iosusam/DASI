/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package icaro.aplicaciones.recursos.comunicacionChat.imp.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author FGarijo
 */
public class DccChat {
      
    
    /**
     * This constructor is used when we are accepting a DCC CHAT request
     * from somebody. It attempts to connect to the client that issued the
     * request.
     */
    private ConexionIrc _bot;
    private String _sourceNick;
    private BufferedReader _reader;
    private BufferedWriter _writer;
    private Socket _socket;
    public DccChat(ConexionIrc bot, String sourceNick, String address, int port) throws IOException {
        _bot = bot;
        _sourceNick = sourceNick;
        _socket = new Socket(address, port);
        _reader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
        _writer = new BufferedWriter(new OutputStreamWriter(_socket.getOutputStream()));
    }
    
    
    /**
     * This constructor is used after we have issued a DCC CHAT request to
     * somebody. If the client accepts the chat request, then the socket we
     * obtain is passed to this constructor.
     */
    public DccChat(ConexionIrc bot, String sourceNick, Socket socket) throws IOException {
        _bot = bot;
        _sourceNick = sourceNick;
        _socket = socket;
        _reader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
        _writer = new BufferedWriter(new OutputStreamWriter(_socket.getOutputStream()));        
    }
    
    
    /**
     * Reads the next line of text from the client at the other end of our DCC Chat
     * connection.  This method blocks until something can be returned.
     * If the connection has closed, null is returned.
     *
     * @return The next line of text from the client.  Returns null if the
     *          connection has closed.
     */
    public String readLine() throws IOException {
        String line = _reader.readLine();
        if (line != null) {
            _bot.log("+++ DCC CHAT " + _sourceNick + " " + line);
        }
        return line;
    }
    
    
    /**
     * Sends a line of text to the client at the other end of our DCC Chat
     * connection.
     * 
     * @param line The line of text to be sent.  This should not include
     *             linefeed characters.
     */
    public void sendLine(String line) throws IOException {
        // No need for synchronization here really...
        _bot.log("+++ DCC CHAT " + _sourceNick + " >>>" + line);
        _writer.write(line + "\r\n");
        _writer.flush();
    }
    
    
    /**
     * Closes the DCC Chat connection.
     */
    public void close() throws IOException {
        _bot.log("+++ DCC CHAT with " + _sourceNick + " ended by us.");
        _socket.close();
    }
    
    
}
