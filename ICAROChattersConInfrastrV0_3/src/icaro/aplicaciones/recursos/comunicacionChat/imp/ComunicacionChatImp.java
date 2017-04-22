/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package icaro.aplicaciones.recursos.comunicacionChat.imp;

import icaro.aplicaciones.recursos.comunicacionChat.imp.util.IrcException;
import icaro.aplicaciones.recursos.comunicacionChat.imp.util.NickAlreadyInUseException;
import icaro.aplicaciones.recursos.comunicacionChat.imp.util.ConexionIrc;
import icaro.infraestructura.entidadesBasicas.comunicacion.ComunicacionAgentes;
import icaro.infraestructura.entidadesBasicas.comunicacion.MensajeSimple;
import icaro.infraestructura.entidadesBasicas.excepciones.ExcepcionEnComponente;
import icaro.infraestructura.entidadesBasicas.interfaces.InterfazUsoAgente;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author FGarijo
 */
public class ComunicacionChatImp extends ConexionIrc{
    private String identRecurso;
    private String url = null;
    private String nickname = null;
    private String chanel="#kiwiirc-default";
    private Boolean conectado=false;
    public InterfazUsoAgente itfUsoAgenteGestDialogo;
    private  String identificadorAgenteGestorDialogo;
    private ComunicacionAgentes comunicator;
    private MensajeSimple mensajeAenviar;
    public ComunicacionChatImp(String identRecurso,String url, String nickname) {
        this.url = url;
        this.nickname = nickname;    
        this.setName(this.nickname);
        comunicator = new ComunicacionAgentes(this.identRecurso);
    }
    public void setItfUsoAgenteGestorDialogo(InterfazUsoAgente itfUsoAgenteDialogo){
        this.itfUsoAgenteGestDialogo=itfUsoAgenteDialogo;
    }
    public synchronized final void setIdentAgenteGestorDialogo(String identAgenteDialogo){
        this.identificadorAgenteGestorDialogo=identAgenteDialogo;
    }
    public synchronized final String getIdentAgenteGestorDialogo(){
        return identificadorAgenteGestorDialogo;
    }
    public Boolean conectar(){
        this.setVerbose(true); //Debugging -> false
        conectado=isConnected();
        while(!conectado)
        {
            try {
                this.connect(url);
                this.joinChannel(chanel);
                this.changeNick(nickname);
                conectado= true;
//                this.sendMessage("pacopa2", "hola hola");
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            } catch (IrcException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            } catch (NickAlreadyInUseException ex) {
                Logger.getLogger(ComunicacionChatImp.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        return conectado;
    }
    public Boolean nuevaConexion (String urlNueva, String canal, String nick ){
        
        if(conectado){
            if(this.url.equals(urlNueva))
                if(!this.chanel.equals(canal))this.chanel=canal;
                    return true;
                }
        this.url=urlNueva;
        this.chanel=canal;
        this.nickname= nick;
        return this.conectar();
    }
    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message)
    {
//        if(message.matches("Hola"))
//        {
//            sendRawLine("PRIVMSG "+sender+" :Hola, "+sender+"!");
//        }else
//        {
//            sendRawLine("PRIVMSG "+sender+" :Lo siento pero no entiendo lo que dices");
//        }
//        String agteReceptor = getIdentAgenteGestorDialogo();
        if ( getIdentAgenteGestorDialogo()!=null){
            mensajeAenviar = new MensajeSimple(message,sender,getIdentAgenteGestorDialogo());
            comunicator.enviarMsgaOtroAgente(mensajeAenviar);
        }
        else try {
            throw new ExcepcionEnComponente ("El identificador del Gestor de dialogo no esta definido",this.getClass().getSimpleName(),null);
        } catch (ExcepcionEnComponente ex) {
            Logger.getLogger(ComunicacionChatImp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    public void onMessage(String chanel,String sender, String login, String hostname, String message)
    {
        this.log("chanel:"+chanel+" Sender :"+sender+ "hostname: "+hostname+ "mensaje :"+message);
        if ( getIdentAgenteGestorDialogo()!=null)comunicator.enviarInfoAotroAgente(sender+message, getIdentAgenteGestorDialogo());
        else try {
            throw new ExcepcionEnComponente ("El identificador del Gestor de dialogo no esta definido",this.getClass().getSimpleName(),null);
        } catch (ExcepcionEnComponente ex) {
            Logger.getLogger(ComunicacionChatImp.class.getName()).log(Level.SEVERE, null, ex);
        }
//        if(message.matches("hola"))
//        {
//            sendRawLine("PRIVMSG "+sender+" :Hola, "+sender+"!");
//        }else
//        {
//            sendRawLine("PRIVMSG "+sender+" :Lo siento pero no entiendo lo que dices");
//        }
    }
}
