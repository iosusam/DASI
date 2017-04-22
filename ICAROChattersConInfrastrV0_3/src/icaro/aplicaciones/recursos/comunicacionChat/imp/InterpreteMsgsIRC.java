/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package icaro.aplicaciones.recursos.comunicacionChat.imp;

import gate.Annotation;
import icaro.aplicaciones.informacion.gestionCitas.InfoConexionUsuario;
import icaro.aplicaciones.informacion.gestionCitas.Notificacion;
import icaro.aplicaciones.informacion.gestionCitas.VocabularioGestionCitas;
import icaro.aplicaciones.recursos.comunicacionChat.imp.util.ConexionIrc;
import static icaro.aplicaciones.recursos.comunicacionChat.imp.util.ConexionIrc.VERSION;
import icaro.aplicaciones.recursos.extractorSemantico.ItfUsoExtractorSemantico;
import icaro.infraestructura.entidadesBasicas.comunicacion.ComunicacionAgentes;
import icaro.infraestructura.entidadesBasicas.comunicacion.MensajeSimple;
import icaro.infraestructura.entidadesBasicas.excepciones.ExcepcionEnComponente;
import icaro.infraestructura.entidadesBasicas.interfaces.InterfazUsoAgente;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author FGarijo
 */
public class InterpreteMsgsIRC {
    
     private boolean _verbose = true;
    private String _userNameAgente = VocabularioGestionCitas.IdentConexionAgte;
    private String _login = "ConexionIrc";
    private String _version = "ConexionIrc " + VERSION + " Java IRC Bot - www.jibble.org";
    private String _finger = "You ought to be arrested for fingering a bot!";
    private int _maxLineLength = 512;
    private ConexionIrc conectorIrc;
    private String identAgenteGestorDialogo;
    private String identRecExtractSemantico;
    private ComunicacionAgentes comunicator;
    private MensajeSimple mensajeAenviar;
    private InterfazUsoAgente itfAgenteDialogo;
    private ItfUsoExtractorSemantico itfUsoExtractorSem;
    private HashSet anotacionesRelevantes;
    private InfoConexionUsuario infoConecxInterlocutor;
    

    public InterpreteMsgsIRC (){}
    public InterpreteMsgsIRC(ConexionIrc conexIrc) {
       conectorIrc = conexIrc;
    }
    
    public synchronized void setConectorIrc (ConexionIrc ircConect){
        conectorIrc = ircConect;
    }
    public synchronized  void setItfusoAgenteGestorDialogo(InterfazUsoAgente itfAgteDialogo){
        this.itfAgenteDialogo=itfAgteDialogo;
    }
    public synchronized  void setIdentAgenteGestorDialogo(String idAgteDialogo){
        this.identAgenteGestorDialogo=idAgteDialogo;
    }
    public synchronized  void setIdentConexion(String usnAgte){
        this._userNameAgente=usnAgte;
    }
    public synchronized  void setItfusoRecExtractorSemantico(ItfUsoExtractorSemantico itfRecExtractorSem){
        this.itfUsoExtractorSem=itfRecExtractorSem;
    }
    public void log(String line) {
        if (_verbose) {
            System.out.println(System.currentTimeMillis() + " " + line);
        } 
    }
    public final void handleLine(String line) {
        this.log(line);

        // Check for server pings.
        if (line.startsWith("PING ")) {
            conectorIrc.onServerPing(line.substring(5));
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
                        conectorIrc.onUnknown(line);
                        return;
                    }
                    String response = line.substring(line.indexOf(errorStr, senderInfo.length()) + 4, line.length());
                    conectorIrc.onServerResponse(code, response);
                    return;
                }
                else {
                    conectorIrc.onUnknown(line);
                    return;
                }
            }
            else {
                conectorIrc.onUnknown(line);
                return;
            }
        }
        else {
            // This line is not in a format that we can understand.
            conectorIrc.onUnknown(line);
            return;
        }
        
        String command = tokenizer.nextToken();
        String target = tokenizer.nextToken();

        // Check for CTCP requests.
        if (command.equals("PRIVMSG") && line.indexOf(":\u0001") > 0 && line.endsWith("\u0001")) {
            
            String request = line.substring(line.indexOf(":\u0001") + 2, line.length() - 1);

            // Check for version requests.
            if (request.equals("VERSION")) {
                conectorIrc.onVersion(sourceNick, sourceLogin, sourceHostname, target);
                return;
            }
            
            // Check for actions from users.
            if (request.startsWith("ACTION ")) {
                conectorIrc.onAction(sourceNick, sourceLogin, sourceHostname, target, request.substring(7));
                return;
            }

            // Check for ping requests.
            if (request.startsWith("PING ")) {
                conectorIrc.onPing(sourceNick, sourceLogin, sourceHostname, target, request.substring(5));
                return;
            }
            
            // Check for time requests.
            if (request.equals("TIME")) {
                conectorIrc.onTime(sourceNick, sourceLogin, sourceHostname, target);
                return;
            }

            // Check for finger requests.
            if (request.equals("FINGER")) {
                conectorIrc.onFinger(sourceNick, sourceLogin, sourceHostname, target);
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
            conectorIrc.onUnknown(line);
            return;
        }
        
        // Check for normal messages to the channel.
        if (command.equals("PRIVMSG") && (target.startsWith("#") || target.startsWith("&"))) {
            this.onMessage(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
            return;
        }
        
        // Check for private messages to us.
        if (command.equals("PRIVMSG") && target.equalsIgnoreCase(_userNameAgente)) {
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
        conectorIrc.sendRawLine("PONG " + response);
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
    protected void onPrivateMessage(String sender, String login, String hostname, String textoUsuario) {
    
    
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
    // Se envia la información al extrator semantico se traducen las anotaciones y se envia el contenido al agente de dialogo
    // de esta forma el agente recibe mensajes con entidades del modelo de información
        HashSet anotacionesBusquedaPrueba = new HashSet();
        anotacionesBusquedaPrueba.add("Saludo");
        anotacionesBusquedaPrueba.add("Lookup");
    // esto habria que pasarlo como parametro
        if(infoConecxInterlocutor==null)infoConecxInterlocutor= new InfoConexionUsuario();
        infoConecxInterlocutor.setuserName(sender);
        infoConecxInterlocutor.sethost(hostname);
        infoConecxInterlocutor.setlogin(login);
    if(itfUsoExtractorSem !=null){
        try {
            anotacionesRelevantes = itfUsoExtractorSem.extraerAnotaciones(anotacionesBusquedaPrueba, textoUsuario);
            String anot = anotacionesRelevantes.toString();
            System.out.println(System.currentTimeMillis() + " " + anot);
           ArrayList infoAenviar= interpretarAnotaciones(sender,textoUsuario,anotacionesRelevantes);
           enviarInfoExtraida ( infoAenviar, sender);
//            if ( itfAgenteDialogo!=null){
//            mensajeAenviar = new MensajeSimple(infoAenviar,sender,identAgenteGestorDialogo);
//            itfAgenteDialogo.aceptaMensaje(mensajeAenviar);
////            comunicator.enviarMsgaOtroAgente(mensajeAenviar);
//        }
        } catch (Exception ex) {
            Logger.getLogger(InterpreteMsgsIRC.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
    }

    private void enviarInfoExtraida (ArrayList infoExtraida,String sender){
        
        if ( itfAgenteDialogo!=null){
            try {
                if (infoExtraida.size()==0){
                    Notificacion infoAenviar = new Notificacion(sender);
                    infoAenviar.setTipoNotificacion(VocabularioGestionCitas.ExtraccionSemanticaNull);
                    mensajeAenviar = new MensajeSimple((Object)infoAenviar,sender,identAgenteGestorDialogo);
                }
                else if (infoExtraida.size()==1){
                    Object infoAenviar = infoExtraida.get(0);
                    mensajeAenviar = new MensajeSimple(infoAenviar,sender,identAgenteGestorDialogo);
                } else{
                    mensajeAenviar = new MensajeSimple(infoExtraida,sender,identAgenteGestorDialogo);                   
//                    mensajeAenviar.setColeccionContenido(infoExtraida); // los elementos de la colección se meterán en el motor
                }
                
                itfAgenteDialogo.aceptaMensaje(mensajeAenviar);
            } catch (RemoteException ex) {
                Logger.getLogger(InterpreteMsgsIRC.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    }
//    private void enviarInfoAgteGestorDialogo(){
//        if ( identAgenteGestorDialogo!=null){
//          MensajeSimple  mensajeAenviar = new MensajeSimple(message,sender,getIdentAgenteGestorDialogo());
//            comunicator.enviarMsgaOtroAgente(mensajeAenviar);
//        }
   
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
        conectorIrc.sendRawLine("NOTICE " + sourceNick + " :\u0001VERSION " + _version + "\u0001");
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
        conectorIrc.sendRawLine("NOTICE " + sourceNick + " :\u0001PING " + pingValue + "\u0001");
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
        conectorIrc.sendRawLine("NOTICE " + sourceNick + " :\u0001TIME " + new Date().toString() + "\u0001");
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
        conectorIrc.sendRawLine("NOTICE " + sourceNick + " :\u0001FINGER " + _finger + "\u0001");
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

    public int getMaxLineLength() {
        return _maxLineLength;
    }

    public void intentaDesconectar() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    if (conectorIrc.isConnected())conectorIrc.disconnect(); 
    }
private ArrayList interpretarAnotaciones(String interlocutor,String contextoInterpretacion,HashSet anotacionesRelevantes){
    // recorremos las anotaciones obtenidas y las traducimos a objetos del modelo de información
    ArrayList anotacionesInterpretadas =new ArrayList();
//    int i=0;
    Iterator annotTypesSal = anotacionesRelevantes.iterator();
            while(annotTypesSal.hasNext()) {
             Annotation    annot = (Annotation) annotTypesSal.next();
             String anotType=annot.getType();
             if(anotType.equalsIgnoreCase("saludo")){
                 anotacionesInterpretadas.add(interpretarAnotacionSaludo(contextoInterpretacion, annot));
//                 i++;
             }
//                 fet = annot.getFeatures();
                
//                string= (String) annot.getFeatures().get("string");
            }
     return anotacionesInterpretadas;
}
private Notificacion interpretarAnotacionSaludo(String conttextoInterpretacion,Annotation anotacionSaludo){
//    if(anotacionSaludo.getType()!="saludo"){
//        return null;
//    }
    Notificacion notif= new Notificacion(this.infoConecxInterlocutor.getuserName());
    // obtenemos el texto del saludo a partir de la anotacion
            
        int posicionComienzoTexto =anotacionSaludo.getStartNode().getOffset().intValue();
        int posicionFinTexto =anotacionSaludo.getEndNode().getOffset().intValue();
        String msgNotif =conttextoInterpretacion.substring(posicionComienzoTexto, posicionFinTexto);
        notif.setTipoNotificacion(anotacionSaludo.getType());
        notif.setMensajeNotificacion(msgNotif);
        return notif;
}
}
