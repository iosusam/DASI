/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package icaro.aplicaciones.recursos.interfazChatUsuario.imp;

import icaro.aplicaciones.recursos.comunicacionChat.imp.*;
import gate.Annotation;
import icaro.aplicaciones.informacion.gestionCitas.InfoConexionUsuario;
import icaro.aplicaciones.informacion.gestionCitas.Notificacion;
import icaro.aplicaciones.informacion.gestionCitas.VocabularioGestionCitas;
import icaro.aplicaciones.recursos.comunicacionChat.imp.util.ConexionIrc;
import static icaro.aplicaciones.recursos.comunicacionChat.imp.util.ConexionIrc.VERSION;
import icaro.aplicaciones.recursos.extractorSemantico.ItfUsoExtractorSemantico;
import icaro.infraestructura.entidadesBasicas.NombresPredefinidos;
import icaro.infraestructura.entidadesBasicas.comunicacion.ComunicacionAgentes;
import icaro.infraestructura.entidadesBasicas.comunicacion.MensajeSimple;
import icaro.infraestructura.entidadesBasicas.excepciones.ExcepcionEnComponente;
import icaro.infraestructura.entidadesBasicas.interfaces.InterfazUsoAgente;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.ItfUsoRecursoTrazas;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;
import icaro.infraestructura.recursosOrganizacion.repositorioInterfaces.ItfUsoRepositorioInterfaces;
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
public class InterpreteMsgsPanelChat {
    
     private boolean _verbose = true;
    private String _userNameAgente = VocabularioGestionCitas.IdentConexionAgte;
    private int _maxLineLength = 512;
    private ConexionIrc conectorIrc;
     protected ItfUsoRepositorioInterfaces itfUsoRepositorioInterfaces;
    protected ItfUsoRecursoTrazas trazas;
    private String identAgenteGestorDialogo;
    private String identRecExtractSemantico;
    private ComunicacionAgentes comunicator;
    private MensajeSimple mensajeAenviar;
    private InterfazUsoAgente itfAgenteDialogo;
    private ItfUsoExtractorSemantico itfUsoExtractorSem;
    private HashSet anotacionesRelevantes;
    private InfoConexionUsuario infoConecxInterlocutor;
    private String identificadordeEsteRecurso;
    private String identificadorAgenteaReportar;
    private InterfazUsoAgente itfUsoAgenteaReportar;
    

    public InterpreteMsgsPanelChat (){}
    public InterpreteMsgsPanelChat(String identRecurso, String identAgteAreportar) {
       trazas = NombresPredefinidos.RECURSO_TRAZAS_OBJ;
        identificadordeEsteRecurso = identRecurso;
        identificadorAgenteaReportar = identAgteAreportar;
//        if (identificadorAgenteaReportar != null){
//           try {
//               itfUsoAgenteaReportar = (InterfazUsoAgente) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfaz(NombresPredefinidos.ITF_USO+identificadorAgenteaReportar);
//           } catch (Exception ex) {
//               Logger.getLogger(InterpreteMsgsPanelChat.class.getName()).log(Level.SEVERE, null, ex);
//           }
//            }
//            else{
//            itfUsoAgenteaReportar = null;
//            trazas.trazar(identRecurso, "La interfaz del agente a reportar es null. Verificar la definición del identificador del agente", InfoTraza.NivelTraza.error);
//        }
    }
    private InterfazUsoAgente buscarItfAgente(String identAgente){
        if (identAgente != null){
           try {
               itfUsoAgenteaReportar = (InterfazUsoAgente) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfaz(NombresPredefinidos.ITF_USO+identAgente);
           } catch (Exception ex) {
               Logger.getLogger(InterpreteMsgsPanelChat.class.getName()).log(Level.SEVERE, null, ex);
           }
            }
            else{
            itfUsoAgenteaReportar = null;
//            trazas.trazar(identRecurso, "La interfaz del agente a reportar es null. Verificar la definición del identificador del agente", InfoTraza.NivelTraza.error);
        }
        return itfUsoAgenteaReportar;
    }
    
    
    public synchronized  void setItfusoAgenteGestorDialogo(InterfazUsoAgente itfAgteDialogo){
        this.itfAgenteDialogo=itfAgteDialogo;
    }
    public synchronized  void setIdentAgenteGestorDialogo(String idAgteDialogo){
        this.identAgenteGestorDialogo=idAgteDialogo;
    }
    
    public synchronized  void setItfusoRecExtractorSemantico(ItfUsoExtractorSemantico itfRecExtractorSem){
        this.itfUsoExtractorSem=itfRecExtractorSem;
    }
    public void log(String line) {
        if (_verbose) {
            System.out.println(System.currentTimeMillis() + " " + line);
        } 
    }
    
    public void interpetarTextoUsuario(String textoUsuario) {
    
    // Se envia la información al extractor semantico se traducen las anotaciones y se envia el contenido al agente de dialogo
    // de esta forma el agente recibe mensajes con entidades del modelo de información
        HashSet anotacionesBusquedaPrueba = new HashSet();
        anotacionesBusquedaPrueba.add("saludo");
        anotacionesBusquedaPrueba.add("Lookup");
    // esto habria que pasarlo como parametro      
    if(itfUsoExtractorSem ==null){
        itfUsoExtractorSem= getItfUsoExtractorSemantico( VocabularioGestionCitas.IdentRecursoExtractorSemantico);
        if(itfUsoExtractorSem ==null) trazas.trazar(this.identificadordeEsteRecurso, "La interfaz del recurso de extracion semantica es null. Verificar la definición del identificador del recurso", InfoTraza.NivelTraza.error);
    }else {
        try {
            anotacionesRelevantes = itfUsoExtractorSem.extraerAnotaciones(anotacionesBusquedaPrueba, textoUsuario);
            String anot = anotacionesRelevantes.toString();
            System.out.println(System.currentTimeMillis() + " " + anot);
           ArrayList infoAenviar= interpretarAnotaciones(this.identificadordeEsteRecurso,textoUsuario,anotacionesRelevantes);
           enviarInfoExtraida ( infoAenviar, this.identificadordeEsteRecurso);
//            if ( itfAgenteDialogo!=null){
//            mensajeAenviar = new MensajeSimple(infoAenviar,sender,identAgenteGestorDialogo);
//            itfAgenteDialogo.aceptaMensaje(mensajeAenviar);
////            comunicator.enviarMsgaOtroAgente(mensajeAenviar);
//        }
        } catch (Exception ex) {
            Logger.getLogger(InterpreteMsgsPanelChat.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
    }

    private void enviarInfoExtraida (ArrayList infoExtraida,String remitente){
        
        if ( itfAgenteDialogo!=null){
            try {
                if (infoExtraida.size()==0){
                    Notificacion infoAenviar = new Notificacion(remitente);
                    infoAenviar.setTipoNotificacion(VocabularioGestionCitas.ExtraccionSemanticaNull);
                    mensajeAenviar = new MensajeSimple((Object)infoAenviar,remitente,identAgenteGestorDialogo);
                }
                else if (infoExtraida.size()==1){
                    Object infoAenviar = infoExtraida.get(0);
                    mensajeAenviar = new MensajeSimple(infoAenviar,remitente,identAgenteGestorDialogo);
                } else{
                    mensajeAenviar = new MensajeSimple(infoExtraida,remitente,identAgenteGestorDialogo);                   
//                    mensajeAenviar.setColeccionContenido(infoExtraida); // los elementos de la colección se meterán en el motor
                }
                
                itfAgenteDialogo.aceptaMensaje(mensajeAenviar);
            } catch (RemoteException ex) {
                Logger.getLogger(InterpreteMsgsPanelChat.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    }
//    private void enviarInfoAgteGestorDialogo(){
//        if ( identAgenteGestorDialogo!=null){
//          MensajeSimple  mensajeAenviar = new MensajeSimple(message,sender,getIdentAgenteGestorDialogo());
//            comunicator.enviarMsgaOtroAgente(mensajeAenviar);
//        }
   
   
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
    String identLocutor=null;
    if(infoConecxInterlocutor !=null) identLocutor = this.infoConecxInterlocutor.getuserName();
    Notificacion notif= new Notificacion(identLocutor);
    // obtenemos el texto del saludo a partir de la anotacion     
        int posicionComienzoTexto =anotacionSaludo.getStartNode().getOffset().intValue();
        int posicionFinTexto =anotacionSaludo.getEndNode().getOffset().intValue();
        String msgNotif =conttextoInterpretacion.substring(posicionComienzoTexto, posicionFinTexto);
        notif.setTipoNotificacion(anotacionSaludo.getType());
        notif.setMensajeNotificacion(msgNotif);
        return notif;
}
 public ItfUsoExtractorSemantico getItfUsoExtractorSemantico(String idExtractorSem) {
            ItfUsoExtractorSemantico itfUsoExtracSem = null;
//            while (itfUsoExtracSem == null&& numIntentos<0 ){
             try {
               Thread.sleep(1000);
	     itfUsoExtracSem = (ItfUsoExtractorSemantico) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfaz
		(NombresPredefinidos.ITF_USO+idExtractorSem);
		}
		catch (Exception e) {
			Logger.getLogger(NotificacionesEventosItfUsuarioChat.class.getName()).log(Level.SEVERE, null, e);
    //      Logger.error("Ha habido un problema enviar un  evento al agente "+IdentAgenteReceptor);
			this.trazas.aceptaNuevaTraza(new InfoTraza(this.getClass().getSimpleName(),
									"Ha habido un problema al obtener la interfaz de uso del recurso: "+idExtractorSem,
									InfoTraza.NivelTraza.error));
			}
            
//            }
             return itfUsoExtracSem;
	}
}
