/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package icaro.aplicaciones.recursos.comunicacionChat.imp.util;
import icaro.aplicaciones.recursos.comunicacionChat.imp.InterpreteMsgsIRC;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

/**
 *
 * @author 
 */
//public abstract class ConexionIrc {
   public  class ConexionIrc { 
    /**
     * The definitive version number of this release .
     * (Note: Change this before automatically building releases)
     */
    public static final String VERSION = "0.9.11";
    // Connection stuff.
    private Socket _socket = null;
    private InputThread _inputThread = null;
    private OutputThread _outputThread = null;
    private boolean _isConnected = false;

    // Details about the last server that we connected to.
    private String _server = null;
    private int _port = -1;
    private String _password = null;
    
    // Outgoing message stuff.
    private Queue _outQueue = new Queue();
    private long _messageDelay = 5000;
    private int _maxLineLength = 512;
    
    // Default settings for the ConexionIrc.
    private boolean _verbose = false;
    private String _name = "ConexionIrc";
    private String _login = "ConexionIrc";
    private String _version = "ConexionIrc " + VERSION + " Java IRC Bot - www.jibble.org";
    private String _finger = "You ought to be arrested for fingering a bot!";
    private InterpreteMsgsIRC interpMesgs;

    
    /**
     * Constructs a  with the default settings.  Your own constructors
     * in classes which extend the ConexionIrc abstract class should be responsible
     * for changing the default settings if required.
     */
    public  ConexionIrc() {}
//    public void ConexionIrc(InterpreteMsgsIRC interprete) {
//         this.interpMesgs =interprete;
//     
//     }
    
    
    /**
     * Attempt to connect to the specified IRC server.
     * The onConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * 
     * @throws IOException if it was not possible to connect to the server.
     * @throws icaro.aplicaciones.recursos.comunicacionChat.imp.util.IrcException
     * @throws icaro.aplicaciones.recursos.comunicacionChat.imp.util.NickAlreadyInUseException
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the server.
     */
    public final synchronized void connect(String hostname) throws IOException, IrcException, NickAlreadyInUseException {
        this.connect(hostname, 6667, null);
//        this.connect(hostname, 8080, null);
    }


    /**
     * Attempt to connect to the specified IRC server and port number.
     * The onConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * @param port The port number to connect to on the server.
     * 
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the server.
     */
    public final synchronized void connect(String hostname, int port) throws IOException, IrcException, NickAlreadyInUseException {
        this.connect(hostname, port, null);
    }
    
    
    /**
     * Attempt to connect to the specified IRC server using the supplied
     * password.
     * The onConnect method is called upon success.
     *
     * @param hostname The hostname of the server to connect to.
     * @param port The port number to connect to on the server.
     * @param password The password to use to join the server.
     *
     * @throws IOException if it was not possible to connect to the server.
     * @throws IrcException if the server would not let us join it.
     * @throws NickAlreadyInUseException if our nick is already in use on the server.
     */
    public final synchronized void connect(String hostname, int port, String password) throws IOException, IrcException, NickAlreadyInUseException {

        _server = hostname;
        _port = port;
        _password = password;
        
        if (isConnected()) {
            throw new IOException("The ConexionIrc is already connected to an IRC server.  Disconnect first.");
        }
        
        // Don't clear the outqueue - there might be something important in it!
        
        // Connect to the server.
        _socket =  new Socket(hostname, port);
        this.log("*** Connected to server.");
        
        BufferedReader breader = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
        BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(_socket.getOutputStream()));
        
        // Attempt to join the server.
        if (password != null && !password.equals("")) {
            bwriter.write("PASS " + password + "\r\n");
        }
        bwriter.write("NICK " + _name + "\r\n");
        bwriter.write("USER " + _login + " 8 * :" + _version + "\r\n");
        bwriter.flush();
        
        // Read stuff back from the server to see if we connected.
        String line = null;
        while ((line = breader.readLine()) != null) {
            this.log(line);
            if (line.startsWith("PING ")) {
                bwriter.write("PONG " + line.substring(5) + "\r\n");
                bwriter.flush();
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(line);
            tokenizer.nextToken();
            String code = tokenizer.nextToken();
            if (code.equals("004")) {
                break;
            }
            else if (code.equals("433")) {
                if (!line.contains(this._name))
                throw new NickAlreadyInUseException(line);
            }
            else if (code.startsWith("5") || code.startsWith("4")) {
                throw new IrcException("Could not log into the IRC server: " + line);
            }
        }
        
        this.log("*** Logged onto server.");
        
        // This makes the socket timeout on read operations after 5 minutes.
        // Maybe in some future version I will let the user change this at runtime.
        _socket.setSoTimeout(5*60*1000);
        
        // Now start the InputThread to read all other lines from the server.
        // pasamos como parametro el interprete de mensajes para que procese las lineas
        _inputThread = new InputThread(interpMesgs, breader, bwriter);
        _inputThread.start();
        
        // Now start the outputThread that will be used to send all messages.
        if (_outputThread == null) {
            _outputThread = new OutputThread(this, _outQueue);
            _outputThread.start();
        }
        
        _isConnected = true;
        this.onConnect();
        
    }
    
    
    /**
     * Reconnects to the IRC server that we were previously connected to.
     * If necessary, the appropriate port number and password will be used.
     * This method will throw an IrcException if we have never connected
     * to an IRC server previously.
     */
    public final synchronized void reconnect() throws IOException, IrcException, NickAlreadyInUseException{
        if (getServer() == null) {
            throw new IrcException("Cannot reconnect to an IRC server because we were never connected to one previously!");
        }
        connect(getServer(), getPort(), getPassword());
    }


    /**
     * This method disconnects from the server by closing the socket and
     * ending the threads that read from and write to the server.  This method
     * is called when the connection to the server fails to ensure that the
     * threads are stopped.  It may also be called explicitly to disconnect
     * from a server.  You may wish to use the quitServer method to exit an
     * IRC server normally.
     */
    public final synchronized void disconnect() {
        _isConnected = false;
        this.log("*** Disconnected.");
        try {
            _socket.close();
        }
        catch (IOException e) {
            // If something went wrong, let's assume the socket is already closed.
        }
        this.onDisconnect();
    }
    
    
    /**
     * Starts an ident server (Identification Protocol Server, RFC 1413).
     *  <p>
     * Most IRC servers attempt to contact the ident server on connecting
     * hosts in order to determine the user's identity.  A few IRC servers
     * will not allow you to connect unless this information is provided.
     *  <p>
     * So when a ConexionIrc is run on a machine that does not run an ident server,
     * it may be necessary to call this method to start one up.
     *  <p>
     * Calling this method starts up an ident server which will respond with
     * the login provided by calling getLogin() and then shut down immediately.
     * It will also be shut down if it has not been contacted within 60 seconds
     * of creation.
     *  <p>
     * If you require an ident response, then the correct procedure is to start
     * the ident server and then connect to the IRC server.  The IRC server may
     * then contact the ident server to get the information it needs.
     *  <p>
     * The ident server will fail to start if there is already an ident server
     * running on port 113, or if you are running as an unprivileged user who
     * is unable to create a server socket on that port number.
     *  <p>
     * If it is essential for you to use an ident server when connecting to an
     * IRC server, then make sure that port 113 on your machine is visible to
     * the IRC server so that it may contact the ident server.
     */
    public void startIdentServer() {
        new IdentServer(this, getLogin());
    }

    
    /**
     * Joins a channel.
     * 
     * @param channel The name of the channel to join (eg "#cs").
     */
    public final void joinChannel(String channel) {
        this.sendRawLine("JOIN " + channel);
    }
    
    
    /**
     * Joins a channel with a key.
     * 
     * @param channel The name of the channel to join (eg "#cs").
     * @param key The key that will be used to join the channel.
     */
    public final void joinChannel(String channel, String key) {
        this.joinChannel(channel + " " + key);
    }
    

    /**
     * Parts a channel.
     *
     * @param channel The name of the channel to leave.
     */
    public final void partChannel(String channel) {
        this.sendRawLine("PART " + channel);
    }
    
    
    /**
     * Parts a channel, giving a reason.
     *
     * @param channel The name of the channel to leave.
     * @param reason  The reason for parting the channel.
     */
    public final void partChannel(String channel, String reason) {
        this.sendRawLine("PART " + channel + " :" + reason);
    }


    /**
     * Quits from the IRC server.
     */
    public final void quitServer() {
        this.quitServer("");
    }
    
    
    /**
     * Quits from the IRC server with a reason.
     *
     * @param reason The reason for quitting the server.
     */
    public final void quitServer(String reason) {
        this.sendRawLine("QUIT :" + reason);
    }
    
    
    /**
     * Sends a raw line to the IRC server as soon as possible, bypassing the
     * outgoing message queue.
     *
     * @param line The raw line to send to the IRC server.
     */
    public final synchronized void sendRawLine(String line) {
        if (isConnected()) {
            _inputThread.sendRawLine(line);
        }
    }
    
    
    /**
     * Sends a message to a channel or a private message to a user.  These
     * messages are added to the outgoing message queue and sent at the
     * earliest possible opportunity.
     *
     * @param target The name of the channel or user nick to send to.
     * @param message The message to send.
     */
    public final void sendMessage(String target, String message) {
        _outQueue.add("PRIVMSG " + target + " :" + message);
    }
    
    
    /**
     * Sends an action to the channel or to a user.
     *
     * @param target The name of the channel or user nick to send to.
     * @param action The action to send.
     */
    public final void sendAction(String target, String action) {
        sendCTCPCommand(target, "ACTION " + action);
    }
    
    
    /**
     * Sends a notice to the channel or to a user.
     *
     * @param target The name of the channel or user nick to send to.
     * @param notice The notice to send.
     */
    public final void sendNotice(String target, String notice) {
        _outQueue.add("NOTICE " + target + " :" + notice);
    }
    
    
    /**
     * Sends a CTCP command to a channel or user.  (Client to client protocol).
     * Examples of such commands are "PING <number>", "FINGER", "VERSION", etc.
     * For example, if you wish to request the version of a user called "Dave",
     * then you would call <code>sendCTCPCommand("Dave", "VERSION");</code>.
     * The type of response to such commands is largely dependant on the target
     * client software.
     *
     * @param target The name of the channel or user to send the CTCP message to.
     * @param command The CTCP command to send.
     */
    public final void sendCTCPCommand(String target, String command) {
        _outQueue.add("PRIVMSG " + target + " :\u0001" + command + "\u0001");
    }
    
    
    /**
     * Attempt to change the current nick of this user.
     *
     * @param newNick The new nick to use.
     */
    public final void changeNick(String newNick) {
        this.sendRawLine("NICK " + newNick);
    }


    /**
     * Set the mode of a channel.
     * This method attempts to set the mode of a channel.  This
     * may require the bot to have operator status on the channel.
     * For example, if the bot has operator status, we can grant
     * operator status to "Dave" on the #cs channel
     * by calling setMode("#cs", "+o Dave");
     * An alternative way of doing this would be to use the op method.
     * 
     * @param channel The channel on which to perform the mode change.
     * @param mode    The new mode to apply to the channel.  This may include
     *                zero or more arguments if necessary.
     * 
     */
    public final void setMode(String channel, String mode) {
        this.sendRawLine("MODE " + channel + " " + mode);
    }
    
    
    /**
     * Sends an invitation to join a channel.  Some channels can be marked
     * as "invite-only", so it may be useful to allow a bot to invite people
     * into it.
     * 
     * @param nick    The nick of the user to invite
     * @param channel The channel you are inviting the user to join.
     * 
     */
    public final void sendInvite(String nick, String channel) {
        this.sendRawLine("INVITE " + nick + " :" + channel);
    }    


    /**
     * Bans a user from a channel.  An example of a valid hostmask is
     * "*!*compu@*.18hp.net".  This may be used in conjunction with the
     * kick method to permanently remove a user from a channel.
     * Successful use of this method may require the bot to have operator
     * status itself.
     * 
     * @param channel The channel to ban the user from.
     * @param hostmask A hostmask representing the user we're banning.
     */
    public final void ban(String channel, String hostmask) {
        this.sendRawLine("MODE " + channel + " +b " + hostmask);
    }


    /**
     * Unbans a user from a channel.  An example of a valid hostmask is
     * "*!*compu@*.18hp.net".
     * Successful use of this method may require the bot to have operator
     * status itself.
     * 
     * @param channel The channel to unban the user from.
     * @param hostmask A hostmask representing the user we're unbanning.
     */
    public final void unBan(String channel, String hostmask) {
        this.sendRawLine("MODE " + channel + " -b " + hostmask);
    }


    /**
     * Grants operator privilidges to a user on a channel.
     * Successful use of this method may require the bot to have operator
     * status itself.
     * 
     * @param channel The channel we're opping the user on.
     * @param nick The nick of the user we are opping.
     */
    public final void op(String channel, String nick) {
        this.setMode(channel, "+o " + nick);
    }


    /**
     * Removes operator privilidges from a user on a channel.
     * Successful use of this method may require the bot to have operator
     * status itself.
     * 
     * @param channel The channel we're deopping the user on.
     * @param nick The nick of the user we are deopping.
     */
    public final void deOp(String channel, String nick) {
        this.setMode(channel, "-o " + nick);
    }
    
    
    /**
     * Grants voice privilidges to a user on a channel.
     * Successful use of this method may require the bot to have operator
     * status itself.
     * 
     * @param channel The channel we're voicing the user on.
     * @param nick The nick of the user we are voicing.
     */
    public final void voice(String channel, String nick) {
        this.setMode(channel, "+v " + nick);
    }


    /**
     * Removes voice privilidges from a user on a channel.
     * Successful use of this method may require the bot to have operator
     * status itself.
     * 
     * @param channel The channel we're devoicing the user on.
     * @param nick The nick of the user we are devoicing.
     */
    public final void deVoice(String channel, String nick) {
        this.setMode(channel, "-v " + nick);
    }


    /**
     * Set the topic for a channel.
     * This method attempts to set the topic of a channel.  This
     * may require the bot to have operator status if the topic
     * is protected.
     * 
     * @param channel The channel on which to perform the mode change.
     * @param topic   The new topic for the channel.
     * 
     */
    public final void setTopic(String channel, String topic) {
        this.sendRawLine("TOPIC " + channel + " :" + topic);
    }


    /**
     * Kicks a user from a channel.
     * This method attempts to kick a user from a channel and
     * may require the bot to have operator status in the channel.
     * 
     * @param channel The channel to kick the user from.
     * @param nick    The nick of the user to kick.
     */
    public final void kick(String channel, String nick) {
        this.kick(channel, nick, "");
    }


    /**
     * Kicks a user from a channel, giving a reason.
     * This method attempts to kick a user from a channel and
     * may require the bot to have operator status in the channel.
     * 
     * @param channel The channel to kick the user from.
     * @param nick    The nick of the user to kick.
     * @param reason  A description of the reason for kicking a user.
     */
    public final void kick(String channel, String nick, String reason) {
        this.sendRawLine("KICK " + channel + " " + nick + " :" + reason);
    }
    
    
    /**
     * Issues a DCC SEND request to the specified nick.
     * After doing this, the recipient is allowed up to <b>timeout</b> milliseconds
     * to accept the request by connecting to the ConexionIrc to start downloading
     * the file, otherwise the chance is lost.  Once the recipient has connected,
     * we close the connection if the download is completed, if the download fails
     * or if no data is sent for a 30 second period.  This methods starts a
     * new Thread to operate in and thus returns immediately.  The method
     * performs no action if the filename does not exist.
     * 
     * @param filename A File object representing the file to send to the recipient.
     * @param nick The nick of the recipient.
     * @param timeout The number of milliseconds to wait for the recipient to
     *                accept the file (we recommend about 120000).
     */
    public final void dccSendFile(final File file, final String nick, final int timeout) {
        final int bufferSize = 1024;
        
        if (!file.exists() || file.isDirectory() || !file.canRead()) {
            this.log("+++ Failed DCC SEND: Cannot read from " + file.getPath());
            return;
        }
        
        new Thread() {
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(0);
                    ss.setSoTimeout(timeout);
                    int port = ss.getLocalPort();
                    byte[] ip = InetAddress.getLocalHost().getAddress();
                    long ipNum = ipToLong(ip);
                    
                    // Rename the filename so it has no whitespace in it when we send it.
                    // .... I really should do this a bit more nicely at some point ....
                    String safeFilename = file.getName().replace(' ', '_');
                    safeFilename = safeFilename.replace('\t', '_');
                    
                    // Send the message to the user, telling them where to connect to in order to get the file.
                    sendCTCPCommand(nick, "DCC SEND " + safeFilename + " " + ipNum + " " + port + " " + file.length());

                    // The client may now connect to us and download the file.
                    Socket socket = ss.accept();
                    socket.setSoTimeout(30000);
                    
                    // Might as well close the server socket now; it's finished with.
                    ss.close();
                    
                    BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                    BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                    BufferedInputStream finput = new BufferedInputStream(new FileInputStream(file));
                    
                    byte[] outBuffer = new byte[bufferSize];
                    byte[] inBuffer = new byte[4];
                    int bytesRead = 0;
                    while ((bytesRead = finput.read(outBuffer, 0, outBuffer.length)) != -1) {
                        output.write(outBuffer, 0, bytesRead);
                        output.flush();
                        input.read(inBuffer, 0, inBuffer.length);
                    }
                    output.close();
                    input.close();
                    log("+++ DCC SEND Completed to " + nick + " (" + file.getPath() + ")");
                }
                catch (InterruptedIOException e) {
                    log("+++ Aborted DCC SEND: " + nick + " did not accept any data for too long when sending " + file.getPath());
                }
                catch (Exception e) {
                    log("+++ Failed DCC SEND: Could not send " + file.getPath() + " to " + nick);
                }
            }
        }.start();
    }
    
    
    /**
     * Receives a file that is being sent to us by a DCC SEND request.
     * When we receive a DCC SEND request, we use this method to connect to
     * the sender and download the file.  This method should only be called
     * by the onDccSendRequest method if you choose to do so.  If we do not
     * receive any data for more than 30 seconds, then we close the connection.
     * This method will overwrite any existing file which has the same filename.
     *
     * @param filename Where to save the downloaded file.
     * @param address The host address of the initiator as an integer.
     * @param port The port number to connect to in order to download the file.
     * @param size The size of the file.  If not known, this must be -1.
     */
    protected final void dccReceiveFile(File file, long address, int port, int size) {
        final int bufferSize = 1024;
        
        if (file.isDirectory()) {
            this.log("+++ Cannot write to " + file.getPath());
            return;
        }
        
        try {
            file.createNewFile();
        }
        catch (IOException e) {
            this.log("Cannot write anything to " + file.getPath());
            return;
        }

        try {

            // Convert the integer address to a proper IP address.
            int[] ip = longToIp(address);
            String ipStr = ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
            
            // Connect the socket and set a timeout.
            Socket socket = new Socket(ipStr, port);
            socket.setSoTimeout(30*1000);
            
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
            BufferedOutputStream foutput = new BufferedOutputStream(new FileOutputStream(file));
            
            byte[] inBuffer = new byte[bufferSize];
            byte[] outBuffer = new byte[4];
            int bytesRead = 0;
            int totalBytesRead = 0;
            while ((bytesRead = input.read(inBuffer, 0, inBuffer.length)) != -1) {
                foutput.write(inBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                // Send back an acknowledgement of how many bytes we have got so far.
                outBuffer[0] = (byte) ((totalBytesRead >> 24) & 0xff);
                outBuffer[1] = (byte) ((totalBytesRead >> 16) & 0xff);
                outBuffer[2] = (byte) ((totalBytesRead >> 8) & 0xff);
                outBuffer[3] = (byte) ((totalBytesRead >> 0) & 0xff);
                output.write(outBuffer);
                output.flush();
            }
            foutput.flush();
            foutput.close();
            input.close();
            this.log("+++ DCC download finished (got " + totalBytesRead + " bytes of " + file.getPath() + ")");
        }
        catch (Exception e) {
            this.log("+++ Could not complete DCC download to " + file.getPath() + e);
        }
    }
    
    /**
     * Attempts to establish a DCC CHAT session with a client.  This method
     * issues the connection request to the client and then waits for the
     * client to respond.  If the connection is successfully made, then a
     * DccChat object is returned by this method.  If the connection is not
     * made within the time limit specified by the timeout value, then null
     * is returned.
     *  <p>
     * It is strongly recommended that you call this method within a new
     * Thread, as it may take a long time to return.
     *
     * @param nick The nick of the user we are trying to establish a chat with.
     * @param timeout The number of milliseconds to wait for the recipient to
     *                accept the chat connection (we recommend about 120000).
     * 
     * @return a DccChat object that can be used to send and recieve lines of
     *         text.  Returns <b>null</b> if the connection could not be made.
     */
    public final DccChat dccSendChatRequest(String nick, int timeout) {
        DccChat chat = null;
        try {
            ServerSocket ss = new ServerSocket(0);
            ss.setSoTimeout(timeout);
            int port = ss.getLocalPort();
            byte[] ip = InetAddress.getLocalHost().getAddress();
            long ipNum = ipToLong(ip);

            log("+++ DCC CHAT request being sent to " + nick);
            sendCTCPCommand(nick, "DCC CHAT chat " + ipNum + " " + port);
            
            // The client may now connect to us and download the file.
            Socket socket = ss.accept();
            
            // Close the server socket now that we've finished with it.
            ss.close();
            
            chat = new DccChat(this, nick, socket);
            log("+++ DCC CHAT request accepted by " + nick);
        }
        catch (Exception e) {
            log("+++ DCC CHAT request failed to " + nick);
        }
        return chat;
    }

    
    /**
     * Attempts to accept a DCC CHAT request by a client.  This method tries
     * to establish the connection to the client.  Once the connection has been
     * established, a DccChat object is returned.  The DccChat object can be
     * used to send and receive lines of text over the direct connection.
     * If a connection could not be established, the DccChat object returned is
     * null.  This method is typically called by the onDccChatRequest method.
     *
     * @return a DccChat object that can be used to send and receive lines of
     *         text.  Returns <b>null</b> if the connection could not be made.
     */
    protected final DccChat dccAcceptChatRequest(String sourceNick, long address, int port) {
        int[] ip = longToIp(address);
        String ipStr = ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
        
        DccChat chat = null;
        
        try {
            chat = new DccChat(this, sourceNick, ipStr, port);
        }
        catch (Exception e) {
            this.log("+++ Could not accept the DCC CHAT request from " + ipStr);
        }
        
        return chat;
    }


    /**
     * Adds a line to the log.  This log is currently output to the standard
     * output and is in the correct format for use by tools such as pisg, the
     * Perl IRC Statistics Generator.  You may override this method if you wish
     * to do something else with log entries.
     * Each line in the log begins with a number which
     * represents the logging time (as the number of milliseconds since the
     * epoch).  This timestamp and the following log entry are separated by
     * a single space character, " ".  Outgoing messages are distinguishable
     * by a log entry that has ">>>" immediately following the space character
     * after the timestamp.  DCC events use "+++" and warnings about unhandled
     * Exceptions and Errors use "###".
     *  <p>
     * This implementation of the method will only cause log entries to be
     * output if the ConexionIrc has had its verbose mode turned on by calling
     * setVerbose(true);
     * 
     * @param line The line to add to the log.
     */
    public void log(String line) {
        if (_verbose) {
            System.out.println(System.currentTimeMillis() + " " + line);
        }
    }


    /**
     * This method handles events when any line of text arrives from the server,
     * then calling the appropriate method in the ConexionIrc.  This method is
     * protected and only called by the InputThread for this instance.
     * 
     * @param line The raw line of text from the server.
     */
    protected final void handleLine(String line) {
        this.log(line);

        // Check for server pings.
        if (line.startsWith("PING ")) {
            this.onServerPing(line.substring(5));
            return;
        }

        String sourceNick = null;
        String sourceLogin = null;
        String sourceHostname = null;

        StringTokenizer tokenizer = new StringTokenizer(line);
        String senderInfo = tokenizer.nextToken();
        int exclamation = senderInfo.indexOf("!");
        int at = senderInfo.indexOf("@");
        if (senderInfo.startsWith(":")) {
            if (exclamation > 0 && at > 0 && exclamation < at) {
                sourceNick = senderInfo.substring(1, exclamation);
                sourceLogin = senderInfo.substring(exclamation + 1, at);
                sourceHostname = senderInfo.substring(at + 1);
            }
            else if (tokenizer.hasMoreTokens()) {
                String errorStr = tokenizer.nextToken();
                if (errorStr.length() == 3) {
                    int code = 0;
                    try {
                        code = Integer.parseInt(errorStr);
                    }
                    catch (NumberFormatException e) {
                        this.onUnknown(line);
                        return;
                    }
                    String response = line.substring(line.indexOf(errorStr, senderInfo.length()) + 4, line.length());
                    onServerResponse(code, response);
                    return;
                }
                else {
                    this.onUnknown(line);
                    return;
                }
            }
            else {
                this.onUnknown(line);
                return;
            }
        }
        else {
            // This line is not in a format that we can understand.
            this.onUnknown(line);
            return;
        }
        
        String command = tokenizer.nextToken();
        String target = tokenizer.nextToken();

        // Check for CTCP requests.
        if (command.equals("PRIVMSG") && line.indexOf(":\u0001") > 0 && line.endsWith("\u0001")) {
            
            String request = line.substring(line.indexOf(":\u0001") + 2, line.length() - 1);

            // Check for version requests.
            if (request.equals("VERSION")) {
                this.onVersion(sourceNick, sourceLogin, sourceHostname, target);
                return;
            }
            
            // Check for actions from users.
            if (request.startsWith("ACTION ")) {
                this.onAction(sourceNick, sourceLogin, sourceHostname, target, request.substring(7));
                return;
            }

            // Check for ping requests.
            if (request.startsWith("PING ")) {
                this.onPing(sourceNick, sourceLogin, sourceHostname, target, request.substring(5));
                return;
            }
            
            // Check for time requests.
            if (request.equals("TIME")) {
                this.onTime(sourceNick, sourceLogin, sourceHostname, target);
                return;
            }

            // Check for finger requests.
            if (request.equals("FINGER")) {
                this.onFinger(sourceNick, sourceLogin, sourceHostname, target);
                return;
            }
            
            // Check for DCC SEND or CHAT requests.
            tokenizer = new StringTokenizer(request);
            if (tokenizer.countTokens() >= 5 && tokenizer.nextToken().equals("DCC")) {
                String type = tokenizer.nextToken();
                String filename = tokenizer.nextToken();
                try {
                    long address = Long.parseLong(tokenizer.nextToken());
                    int port = Integer.parseInt(tokenizer.nextToken());
                    int size = -1;
                    if (tokenizer.hasMoreTokens()) {
                        try {
                            size = Integer.parseInt(tokenizer.nextToken());
                        }
                        catch (NumberFormatException e) {
                            // Stick with the value we already had.
                        }
                    }
                    final String sourceNick2 = sourceNick;
                    final String sourceLogin2 = sourceLogin;
                    final String sourceHostname2 = sourceHostname;
                    final String filename2 = filename;
                    final long address2 = address;
                    final int port2 = port;
                    final int size2 = size;
                    if (type.equals("SEND")) {
                        new Thread() {
                            public void run() {
                                onDccSendRequest(sourceNick2, sourceLogin2, sourceHostname2, filename2, address2, port2, size2);
                            }
                        }.start();
                    }
                    else if (type.equals("CHAT")) {
                        new Thread() {
                            public void run() {
                                onDccChatRequest(sourceNick2, sourceLogin2, sourceHostname2, address2, port2);
                            }
                        }.start();
                    }
                }
                catch (NumberFormatException e) {
                    this.log("+++ Invalid DCC SEND request received: " + request);
                }
                return;
            }
            
            // An unknown CTCP message - ignore it.
            this.onUnknown(line);
            return;
        }
        
        // Check for normal messages to the channel.
        if (command.equals("PRIVMSG") && (target.startsWith("#") || target.startsWith("&"))) {
            this.onMessage(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
            
            return;
        }
        
        // Check for private messages to us.
        if (command.equals("PRIVMSG") && target.equalsIgnoreCase(_name)) {
            this.onPrivateMessage(sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
            return;
        }
        
        // Check for people joining channels.
        if (command.equals("JOIN")) {
            this.onJoin(line.substring(line.indexOf(" :") + 2), sourceNick, sourceLogin, sourceHostname);
            return;
        }

        // Check for people parting channels.
        if (command.equals("PART")) {
            this.onPart(target, sourceNick, sourceLogin, sourceHostname);
            return;
        }
        
        // Check for nick changes.
        if (command.equals("NICK")) {
            this.onNickChange(sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
            return;
        }
        
        // Check for notices.
        if (command.equals("NOTICE")) {
            this.onNotice(sourceNick, sourceLogin, sourceHostname, target, line.substring(line.indexOf(" :") + 2));
            return;
        }

        // Check for quits.
        if (command.equals("QUIT")) {
            this.onQuit(sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
            return;
        }

        // Check for kicks.
        if (command.equals("KICK")) {
            String kickee = tokenizer.nextToken();
            this.onKick(target, sourceNick, sourceLogin, sourceHostname, kickee, line.substring(line.indexOf(" :") + 2));
            return;
        }
        
        // Check for mode changes.
        if (command.equals("MODE")) {
            this.onMode(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(target) + target.length() + 1));
            return;
        }
        
        // Check for invites.
        if (command.equals("INVITE")) {
            this.onInvite(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
            return;
        }
        
        // If we reach this point, then we've found something that the ConexionIrc
        // Doesn't currently deal with.
        this.onUnknown(line);
        return;
        
    }
    
    
    /**
     * This method is called once the ConexionIrc has successfully connected to
     * the IRC server.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     */
    protected void onConnect() {
        
    }
    
    
    /**
     * This method carries out the actions to be performed when the ConexionIrc
     * gets disconnected.  This may happen if the ConexionIrc quits from the
     * server, or if the connection is unexpectedly lost.
     *  <p>
     * Disconnection from the IRC server is detected immediately if either
     * we or the server close the connection normally. If the connection to
     * the server is lost, but neither we nor the server have explicitly closed
     * the connection, then it may take a few minutes to detect (this is
     * commonly referred to as a "ping timeout").
     *  <p>
     * If you wish to get your IRC bot to automatically rejoin a server after
     * the connection has been lost, then this is probably the ideal method to
     * override to implement such functionality.
     *  <p>
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     */
    protected void onDisconnect() {}
    
    
    /**
     * This method is called when we receive a numeric response from the
     * IRC server.
     *  <p> 
     * Numerics in the range from 001 to 099 are used for client-server
     * connections only and should never travel between servers.  Replies
     * generated in response to commands are found in the range from 200
     * to 399.  Error replies are found in the range from 400 to 599.
     *  <p>
     * For example, we can use this method to discover the topic of a
     * channel when we join it.  If we join the channel #test which
     * has a topic of &quot;I am King of Test&quot; then the response
     * will be &quot;<code>ConexionIrc #test :I Am King of Test</code>&quot;
     * with a code of 332 to signify that this is a topic.  Check the
     * IRC RFC for the full list of command response codes.
     *  <p>
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param code The three-digit numerical code for the response.
     * @param response The full response from the IRC server.
     * 
     */
    public void onServerResponse(int code, String response) {}
    
    
    /**
     * The actions to perform when a PING request comes from the server.
     * This sends back a correct response, so if you override this method,
     * be sure to either mimic its functionality or to call
     * super.onServerPing(response);
     *
     * @param response The response that should be given back in your PONG.
     */
    public void onServerPing(String response) {
        this.sendRawLine("PONG " + response);
    }
    
    
    /**
     * This method is called whenever a message is sent to a channel.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel to which the message was sent.
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {}


    /**
     * This method is called whenever a private message is sent to the ConexionIrc.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param sender The nick of the person who sent the private message.
     * @param login The login of the person who sent the private message.
     * @param hostname The hostname of the person who sent the private message.
     * @param message The actual message.
     */
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {}
    
    
    /**
     * This method is called whenever an ACTION is sent from a user.  E.g.
     * such events generated by typing "/me goes shopping" in most IRC clients.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param sender The nick of the user that sent the action.
     * @param login The login of the user that sent the action.
     * @param hostname The hostname of the user that sent the action.
     * @param target The target of the action, be it a channel or our nick.
     * @param action The action carried out by the user.
     */
    public void onAction(String sender, String login, String hostname, String target, String action) {}
    
    
    /**
     * This method is called whenever someone (possibly us) joins a channel
     * which we are on.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel which somebody joined.
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
    protected void onJoin(String channel, String sender, String login, String hostname) {}
    
    
    /**
     * This method is called whenever someone (possibly us) parts a channel
     * which we are on.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel which somebody parted from.
     * @param sender The nick of the user who parted from the channel.
     * @param login The login of the user who parted from the channel.
     * @param hostname The hostname of the user who parted from the channel.
     */
    protected void onPart(String channel, String sender, String login, String hostname) {}


    /**
     * This method is called whenever someone (possibly us) changes nick on any
     * of the channels that we are on.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The hostname of the user.
     * @param newNick The new nick.
     */
    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {}
    
    
    /**
     * This method is called whenever someone (possibly us) is kicked from
     * any of the channels that we are in.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param channel The channel from which the recipient was kicked.
     * @param kickerNick The nick of the user who performed the kick.
     * @param kickerLogin The login of the user who performed the kick.
     * @param kickerHostname The hostname of the user who performed the kick.
     * @param recipient The unfortunate recipient of the kick.
     * @param reason The reason given by the user who performed the kick.
     */
    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {}
    
    
    /**
     * This method is called whenever someone (possibly us) quits from the
     * server.  We will only observe this if the user was in one of the
     * channels to which we are connected.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param sourceNick The nick of the user that quit from the server.
     * @param sourceLogin The login of the user that quit from the server.
     * @param sourceHostname The hostname of the user that quit from the server.
     * @param reason The reason given for quitting the server.
     */
    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {}
    
    
    /**
     * Called when the mode of a channel is set.  This method is used to
     * decode the meaning of the mode string and then call onOp, onDeOp,
     * onVoice, onDeVoice, onChannelKey, onDeChannelKey, onChannelLimit,
     * onDeChannelLimit, onChannelBan or onDeChannelBan as appropriate.
     *  <p>
     * If you override this method, be sure to call super.onMode(...) in
     * order to make sure that the appropriate onXxx methods are also
     * called.
     *
     * @param channel The channel that the mode operation applies to.
     * @param sourceNick The nick of the user that set the mode.
     * @param sourceLogin The login of the user that set the mode.
     * @param sourceHostname The hostname of the user that set the mode.
     * @param mode  The mode that has been set.
     * 
     */
    protected void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
        StringTokenizer tok = new StringTokenizer(mode);
        String[] params = new String[tok.countTokens()];
 
        int t = 0;
        while (tok.hasMoreTokens()) {
            params[t] = tok.nextToken();
            t++;
        }
        
        char pn = ' ';
        int p = 1;
 
        // All of this is very large and ugly, but it's the only way of providing
        // what the users want :-/
        for (int i = 0; i < params[0].length(); i++) {
            char atPos = params[0].charAt(i);
 
            if (atPos == '+' || atPos == '-') {
                pn = atPos;
            }
            else if (atPos == 'o') {
                if (pn == '+') {
                    onOp(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                }
                else {
                    onDeop(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                }
                p++;
            }
            else if (atPos == 'v') {
                if (pn == '+') {
                    onVoice(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                }
                else {
                    onDeVoice(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                }
                p++;
            }
            else if (atPos == 'k') {
                if (pn == '+') {
                    onSetChannelKey(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                }
                else {
                    onRemoveChannelKey(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                }
                p++;
            }
            else if (atPos == 'l') {
                if (pn == '+') {
                    onSetChannelLimit(channel, sourceNick, sourceLogin, sourceHostname, Integer.parseInt(params[p]));
                    p++;
                }
                else {
                    onRemoveChannelLimit(channel, sourceNick, sourceLogin, sourceHostname);
                }
            }
            else if (atPos == 'b') {
                if (pn == '+') {
                    onSetChannelBan(channel, sourceNick, sourceLogin, sourceHostname,params[p]);
                }
                else {
                    onRemoveChannelBan(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                }
                p++;
            }
            else if (atPos == 't') {
                if (pn == '+') {
                    onSetTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
                }
                else {
                    onRemoveTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
                }
            }
            else if (atPos == 'n') {
                if (pn == '+') {
                    onSetNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
                }
                else {
                    onRemoveNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
                }
            }
            else if (atPos == 'i') {
                if (pn == '+') {
                    onSetInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
                }
                else {
                    onRemoveInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
                }
            }
            else if (atPos == 'm') {
                if (pn == '+') {
                    onSetModerated(channel, sourceNick, sourceLogin, sourceHostname);
                }
                else {
                    onRemoveModerated(channel, sourceNick, sourceLogin, sourceHostname);
                }
            }
            else if (atPos == 'p') {
                if (pn == '+') {
                    onSetPrivate(channel, sourceNick, sourceLogin, sourceHostname);
                }
                else {
                    onRemovePrivate(channel, sourceNick, sourceLogin, sourceHostname);
                }
            }
            else if (atPos == 's') {
                if (pn == '+') {
                    onSetSecret(channel, sourceNick, sourceLogin, sourceHostname);
                }
                else {
                    onRemoveSecret(channel, sourceNick, sourceLogin, sourceHostname);
                }
            }
        }
    }
    
    
    /**
     * Called when a user (possibly us) gets granted operator status for a channel.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient The nick of the user that got 'opped'.
     */
    protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {}


    /**
     * Called when a user (possibly us) gets operator status taken away.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient The nick of the user that got 'deopped'.
     */
    protected void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {}


    /**
     * Called when a user (possibly us) gets voice status granted in a channel.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient The nick of the user that got 'voiced'.
     */
    protected void onVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {}


    /**
     * Called when a user (possibly us) gets voice status removed.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient The nick of the user that got 'devoiced'.
     */
    protected void onDeVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {}


    /**
     * Called when a channel key is set.  When the channel key has been set,
     * other users may only join that channel if they know the key.  Channel keys
     * are sometimes referred to as passwords.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param key The new key for the channel.
     */
    protected void onSetChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {}


    /**
     * Called when a channel key is removed.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param key The key that was in use before the channel key was removed.
     */
    protected void onRemoveChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {}


    /**
     * Called when a user limit is set for a channel.  The number of users in
     * the channel cannot exceed this limit.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param limit The maximum number of users that may be in this channel at the same time.
     */
    protected void onSetChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname, int limit) {}


    /**
     * Called when the user limit is removed for a channel.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}


    /**
     * Called when a user (possibly us!) gets banned from a channel.  Being
     * banned from a channel prevents any user with a matching hostmask from
     * joining the channel.  For this reason, most bans are usually directly
     * followed by the user being kicked :-)
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param hostmask The hostmask of the user that has been banned.
     */
    protected void onSetChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {}


    /**
     * Called when a hostmask ban is removed from a channel.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param hostmask
     */
    protected void onRemoveChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {}

    
    /**
     * Called when topic protection is enabled for a channel.  Topic protection
     * means that only operators in a channel may change the topic.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when topic protection is removed for a channel.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel is set to only allow messages from users that
     * are in the channel.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel is set to allow messages from any user, even
     * if they are not actually in the channel.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel is set to 'invite only' mode.  A user may only
     * join the channel if they are invited by someone who is already in the
     * channel.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel has 'invite only' removed.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel is set to 'moderated' mode.  If a channel is
     * moderated, then only users who have been 'voiced' or 'opped' may speak
     * or change their nicks.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel has moderated mode removed.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel is marked as being in private mode.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetPrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel is marked as not being in private mode.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemovePrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel is set to be in 'secret' mode.  Such channels
     * typically do not appear on a server's channel listing.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onSetSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when a channel has 'secret' mode removed.
     * This is a type of mode change and is called by the onMode method
     * of the ConexionIrc class.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     *
     * @param channel The channel in which the mode change took place.
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     */
    protected void onRemoveSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {}
    
    
    /**
     * Called when we are invited to a channel by a user.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param targetNick The nick of the user being invited - should be us!
     * @param sourceNick The nick of the user that sent the invitation.
     * @param sourceLogin The login of the user that sent the invitation.
     * @param sourceHostname The hostname of the user that sent the invitation.
     * @param channel The channel that we're being invited to.
     */
    protected void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel)  {}    


    /**
     * This method is called whenever a DCC SEND request is sent to the ConexionIrc.
     * This means that a client has requested to send a file to us.
     * This abstract implementation performs no action, which means that all
     * DCC SEND requests will be ignored by default.  If you wish to save the
     * file, then you may override this method and call the dccReceiveFile
     * method from it, which connects to the sender and downloads the file, eg: -
     * <br>
     * <br>
     * <code>
     * public void onDccSendRequest(String sender, String login, String hostname, String filename, long address, int port, int size) {<br>
     * &nbsp; &nbsp; // Check to see if we trust the sender first?<br>
     * &nbsp; &nbsp; // Check that the filename isn't going to overwrite anything important?<br>
     * &nbsp; &nbsp; dccReceiveFile(new File(filename), address, port, size);<br>
     * &nbsp; &nbsp; // Method finishes when the download completes or if it fails.<br>
     * }
     * </code>
     * <p>
     * Each time this method is called, it is called from within a
     * new Thread in order to allow multiple files to be downloaded by the
     * ConexionIrc at the same time.
     *
     * <b>Warning:</b> Implementing this method and subsequently calling the
     * dccReceiveFile method will cause a file to be written to disk.  Please
     * ensure that you make adequate security checks to make sure that this
     * file will not overwrite anything important!
     * 
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param sender The nick of the user that sent the DCC SEND request.
     * @param login The login of the user that sent the request.
     * @param hostname The hostname of the user that sent the request.
     * @param filename The suggested filename to be used when saving the file.
     * @param address The host address of the initiator as an integer.
     * @param port The port number to connect to if we want to receive the file.
     * @param size The size of the file in bytes.  Older clients do not support
     *             this, in which case a value of -1 will be used.
     */
    protected void onDccSendRequest(String sourceNick, String sourceLogin, String sourceHostname, String filename, long address, int port, int size) {}
    
    
    /**
     * This method is called whenever a DCC CHAT request is sent to the ConexionIrc.
     * This means that a client has requested to chat to us directly rather than
     * via the IRC server.  This is useful for sending many lines of text to and
     * from the bot without having to worry about flooding the server or any
     * operators of the server being able to "spy" on what is being said.
     * This abstract implementation performs no action, which means that all
     * DCC CHAT requests will be ignored by default.  If you wish to accept
     * the connection, then you may override this method and call the dccAcceptChatRequest
     * method from it, which connects to the sender of the chat request and
     * returns a DccChat object which can be used to read lines from the user
     * and to send lines back.  Here is an example of how to use the DccChat object: -
     * <br>
     * <br>
     * <code>
     * public void onDccChatRequest(String sourceNick, String sourceLogin, String sourceHostname, long address, int port) {<br>
     * &nbsp; &nbsp; // Check to see if we want to accept the chat connection first?<br>
     * &nbsp; &nbsp; DccChat chat = dccAcceptChatRequest(sourceNick, address, port);<br>
     * &nbsp; &nbsp; if (chat != null) {<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; try {<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; chat.sendLine(Hi, what's your name?");<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; String name = chat.readLine();<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; chat.sendLine("Hello, " + name);<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; chat.close();<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; }<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; catch (IOException e) {<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; // The connection was lost<br>
     * &nbsp; &nbsp; &nbsp; &nbsp; }<br>
     * &nbsp; &nbsp; }<br>
     * }
     * </code>
     * <p>
     * Each time this method is called, it is called from within a
     * new Thread in order to allow multiple DCC Chat sessions to run at the
     * same time.
     * <p>
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param sender The nick of the user that sent the DCC CHAT request.
     * @param login The login of the user that sent the request.
     * @param hostname The hostname of the user that sent the request.
     * @param address The host address of the initiator as an integer.
     * @param port The port number to connect to if we want to chat.
     */
    protected void onDccChatRequest(String sourceNick, String sourceLogin, String sourceHostname, long address, int port) {}
    
    
    /**
     * This method is called whenever we receive a VERSION request.
     * This abstract implementation responds with the ConexionIrc's _version string,
     * so if you override this method, be sure to either mimic its functionality
     * or to call super.onVersion(...);
     * 
     * @param sourceNick The nick of the user that sent the VERSION request.
     * @param sourceLogin The login of the user that sent the VERSION request.
     * @param sourceHostname The hostname of the user that sent the VERSION request.
     * @param target The target of the VERSION request, be it our nick or a channel name.
     */
    public void onVersion(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :\u0001VERSION " + _version + "\u0001");
    }
    
    
    /**
     * This method is called whenever we receive a PING request.
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onPing(...);
     * 
     * @param sourceNick The nick of the user that sent the PING request.
     * @param sourceLogin The login of the user that sent the PING request.
     * @param sourceHostname The hostname of the user that sent the PING request.
     * @param target The target of the PING request, be it our nick or a channel name.
     * @param pingValue The value that was supplied as an argument to the PING command.
     */
    public void onPing(String sourceNick, String sourceLogin, String sourceHostname, String target, String pingValue) {
        this.sendRawLine("NOTICE " + sourceNick + " :\u0001PING " + pingValue + "\u0001");
    }
    
    
    /**
     * This method is called whenever we receive a TIME request.
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onTime(...);
     * 
     * @param sourceNick The nick of the user that sent the TIME request.
     * @param sourceLogin The login of the user that sent the TIME request.
     * @param sourceHostname The hostname of the user that sent the TIME request.
     * @param target The target of the TIME request, be it our nick or a channel name.
     */
    public void onTime(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :\u0001TIME " + new Date().toString() + "\u0001");
    }
    
    
    /**
     * This method is called whenever we receive a FINGER request.
     * This abstract implementation responds correctly, so if you override this
     * method, be sure to either mimic its functionality or to call
     * super.onFinger(...);
     * 
     * @param sourceNick The nick of the user that sent the FINGER request.
     * @param sourceLogin The login of the user that sent the FINGER request.
     * @param sourceHostname The hostname of the user that sent the FINGER request.
     * @param target The target of the FINGER request, be it our nick or a channel name.
     */
    public void onFinger(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :\u0001FINGER " + _finger + "\u0001");
    }
    
    
    /**
     * This method is called whenever we receive a notice.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param sourceNick The nick of the user that sent the notice.
     * @param sourceLogin The login of the user that sent the notice.
     * @param sourceHostname The hostname of the user that sent the notice.
     * @param target The target of the notice, be it our nick or a channel name.
     * @param notice The notice message.
     */
    protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {}
    
    
    /**
     * This method is called whenever we receive a line from the server that
     * the ConexionIrc has not been programmed to recognise.
     * The implementation of this method in the ConexionIrc abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param line The raw line that was received from the server.
     */
    public void onUnknown(String line) {
        // And then there were none :)
    }
        
    
    /**
     * Sets the verbose mode.  If verbose mode is set to true, then log entries
     * will be made available.
     *
     * @param verbose true if verbose mode is to be used.  Default is false.
     */
    public final void setVerbose(boolean verbose) {
        _verbose = verbose;
    }
    
    
    /**
     * Sets the internal name of the Bot.  This should be set before joining
     * any servers, otherwise the default nick will be used.  You would
     * typically call this method from the contstructor of your class that
     * extends ConexionIrc.
     * The changeNick method should be used if you wish to change your nick
     * when you are connected to a server.
     *
     * @param name The new name of the Bot.
     */
    protected final void setName(String name) {
        _name = name;
    }


    /**
     * Sets the interal login of the Bot.  This should be set before joining
     * any servers.
     *
     * @param login The new login of the Bot.
     */
    protected final void setLogin(String login) {
        _login = login;
    }
    
public final void setInterpreteMsgs(InterpreteMsgsIRC intpMsgs) {
        interpMesgs = intpMsgs;
    }
    /**
     * Sets the internal version of the Bot.  This should be set before joining
     * any servers.
     *
     * @param version The new version of the Bot.
     */
    protected final void setVersion(String version) {
        _version = version;
    }


    /**
     * Sets the interal finger message.  This should be set before joining
     * any servers.
     *
     * @param finger The new finger message for the Bot.
     */
    protected final void setFinger(String finger) {
        _finger = finger;
    }
    
    
    /**
     * Gets the internal name of the ConexionIrc.
     *
     * @return The name of the ConexionIrc.
     */
    public final String getName() {
        return _name;
    }


    /**
     * Gets the internal login of the ConexionIrc.
     *
     * @return The login of the ConexionIrc.
     */
    public final String getLogin() {
        return _login;
    }
    

    /**
     * Gets the internal version of the ConexionIrc.
     *
     * @return The version of the ConexionIrc.
     */
    public final String getVersion() {
        return _version;
    }
    
    
    /**
     * Gets the internal finger message of the ConexionIrc.
     *
     * @return The finger message of the ConexionIrc.
     */
    public final String getFinger() {
        return _finger;
    }
    
    
    /**
     * Returns whether or not the ConexionIrc is currently connected to a server.
     *
     * @return True if and only if the ConexionIrc is currently connected to a server.
     */
    public final boolean isConnected() {
        return _isConnected;
    }
    
    
    /**
     * Sets the number of milliseconds to delay between consecutive
     * messages when there are multiple messages waiting in the
     * outgoing message queue.  This has a default value of 1000ms.
     * It is a good idea to stick to this default value, as it will
     * prevent your bot from spamming servers and facing the subsequent
     * wrath!  However, if you do need to change this delay value (<b>not
     * recommended</b>), then this is the method to use.
     *
     * @param delay The number of milliseconds between each outgoing message.
     * 
     */
    public final void setMessageDelay(long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Cannot have a negative time.");
        }
        _messageDelay = delay;
    }
    
    
    /**
     * Returns the number of milliseconds that will be used to separate
     * consecutive messages to the server from the outgoing message queue.
     *
     * @return Number of milliseconds.
     */
    public final long getMessageDelay() {
        return _messageDelay;
    }
    
    
    /**
     * Gets the maximum length of any line that is sent via the IRC protocol.
     * The IRC RFC specifies that line lengths, including the trailing \r\n
     * must not exceed 512 bytes.  Hence, there is currently no option to
     * change this value in ConexionIrc.  All lines greater than this length
     * will be truncated before being sent to the IRC server.
     * 
     * @return The maximum line length (currently fixed at 512)
     */
    public final int getMaxLineLength() {
        return _maxLineLength;
    }
    
    
    /**
     * Gets the number of lines currently waiting in the outgoing message Queue.
     * If this returns 0, then the Queue is empty and any new message is likely
     * to be sent to the IRC server immediately.
     *
     * @return The number of lines in the outgoing message Queue.
     */
    public final int getOutgoingQueueSize() {
        return _outQueue.size();
    }
    
    
    /**
     * Returns the name of the last IRC server the ConexionIrc tried to connect to.
     * This does not imply that the connection attempt to the server was
     * successful (we suggest you look at the onConnect method).
     * A value of null is returned if the ConexionIrc has never tried to connect
     * to a server.
     * 
     * @return The name of the last machine we tried to connect to. Returns
     * null if no connection attempts have ever been made.
     */
    public final String getServer() {
        return _server;
    }
    
    
    /**
     * Returns the port number of the last IRC server that the ConexionIrc tried
     * to connect to.
     * This does not imply that the connection attempt to the server was
     * successful (we suggest you look at the onConnect method).
     * A value of -1 is returned if the ConexionIrc has never tried to connect
     * to a server.
     * 
     * @return The port number of the last IRC server we connected to.
     *         Returns -1 if no connection attempts have ever been made.
     * 
     */
    public final int getPort() {
        return _port;
    }
    
    
    /**
     * Returns the last password that we used when connecting to an IRC server.
     * This does not imply that the connection attempt to the server was
     * successful (we suggest you look at the onConnect method).
     * A value of null is returned if the ConexionIrc has never tried to connect
     * to a server using a password.
     * 
     * @return The last password that we used when connecting to an IRC server.
     *         Returns null if we have not previously connected using a password.
     * 
     */
    public final String getPassword() {
        return _password;
    }
    
    
    /**
     * A convenient method that accepts an IP address represented as a
     * long and returns an integer array of size 4 representing the same
     * IP address.
     *
     * @param address the long value representing the IP address.
     * 
     * @return An int[] of size 4.
     */
    public int[] longToIp(long address) {
        int[] ip = new int[4];
        for (int i = 3; i >= 0; i--) {
            ip[i] = (int) (address % 256);
            address = address / 256;
        }
        return ip;
    }

    
    /**
     * A convenient method that accepts an IP address represented by a byte[]
     * of size 4 and returns this as a long representation of the same IP
     * address.
     *
     * @param address the byte[] of size 4 representing the IP address.
     * 
     * @return a long representation of the IP address.
     */
    public long ipToLong(byte[] address) {
        long ipNum = 0;
        long multiplier = 1;
        for (int i = 3; i >= 0; i--) {
            int byteVal = (address[i] + 256) % 256;
            ipNum += byteVal*multiplier;
            multiplier *= 256;
        }
        return ipNum;
    }
    
    
    /**
     * Returns true if and only if the object being compared is the exact
     * same instance as this ConexionIrc. This may be useful if you are writing
     * a multiple server IRC bot that uses more than one instance of ConexionIrc.
     *
     * @return true if and only if Object o is a ConexionIrc and equal to this.
     */
    public boolean equals(Object o) {
        // This probably has the same effect as Object.equals, but that may change...
        if (o instanceof ConexionIrc) {
            ConexionIrc other = (ConexionIrc) o;
            return other == this;
        }
        return false;
    }
    
    
    /**
     * Returns the hashCode of this ConexionIrc. This method can be called by hashed
     * collection classes and is useful for managing multiple instances of
     * ConexionIrcs in such collections.
     * 
     * @return the hash code for this instance of ConexionIrc.
     */
    public int hashCode() {
        return super.hashCode();
    }
    
    
    /**
     * Returns a String representation of this object.
     * You may find this useful for debugging purposes, particularly
     * if you are using more than one ConexionIrc instance to achieve
     * multiple server connectivity. The format of
     * this String may change between different versions of ConexionIrc
     * but is currently something of the form
     * <code>
     *   Version{ConexionIrc x.y.z Java IRC Bot - www.jibble.org}
     *   Connected{true}
     *   Server{irc.dal.net}
     *   Port{6667}
     *   Password{}
     * </code>
     *
     * @return a String representation of this object.
     */
    public String toString() {
        return "Version{" + _version + "}"
               + " Connected{" + _isConnected + "}"
               + " Server{" + _server + "}"
               + " Port{" + _port + "}"
               + " Password{" + _password + "}";
    }
    

    
   
}
