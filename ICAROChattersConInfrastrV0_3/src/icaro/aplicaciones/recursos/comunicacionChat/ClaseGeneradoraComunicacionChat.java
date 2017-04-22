package icaro.aplicaciones.recursos.comunicacionChat;

import icaro.aplicaciones.informacion.gestionCitas.VocabularioGestionCitas;
import icaro.aplicaciones.recursos.comunicacionChat.imp.InterpreteMsgsIRC;
import icaro.aplicaciones.recursos.comunicacionChat.imp.util.ConexionIrc;
import icaro.aplicaciones.recursos.comunicacionChat.imp.util.IrcException;
import icaro.aplicaciones.recursos.comunicacionChat.imp.util.NickAlreadyInUseException;
import icaro.aplicaciones.recursos.extractorSemantico.ItfUsoExtractorSemantico;
import icaro.infraestructura.entidadesBasicas.interfaces.InterfazUsoAgente;
import icaro.infraestructura.patronRecursoSimple.imp.ImplRecursoSimple;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;



public class ClaseGeneradoraComunicacionChat extends ImplRecursoSimple implements
		ItfUsoComunicacionChat {

	private static final long serialVersionUID = 1L;
//	private ItfUsoRecursoTrazas trazas;
//	private ConsultaBBDD consulta;
//        private ComunicacionChatImp comunicChat;
        private ConexionIrc comunicChat ;
        private InterpreteMsgsIRC interpreteMsgIrc;
        private String identExtractorSem;
        private String url = null;
        private String nickname = null;
        private String chanel="#kiwiirc-default";
        private String identInterlocutorPruebas = "pacopa2";
        private Boolean conectado = false;
        

	public ClaseGeneradoraComunicacionChat(String idInstanciaRecurso) throws Exception {
		
		super(idInstanciaRecurso);
// obtenemos las propiedades del recurso que deben estar definidas en las propiedades del recurso
//                ItfUsoConfiguracion config = (ItfUsoConfiguracion) repoIntfaces.obtenerInterfaz(NombresPredefinidos.ITF_USO+NombresPredefinidos.CONFIGURACION);
//			DescInstanciaRecursoAplicacion descRecurso = config.getDescInstanciaRecursoAplicacion(idInstanciaRecurso);
//			nombreBD = descRecurso.getValorPropiedad("MYSQL_NAME_BD");

                url=ConfigInfoComunicacionChat.urlFeeNode;
                nickname=ConfigInfoComunicacionChat.nicknameConexionAgte;
//                identAgenteAReportar = VocabularioGestionCitas.IdentAgenteAplicacionDialogoCitas;
                identExtractorSem = VocabularioGestionCitas.IdentRecursoExtractorSemantico;
		try {
//                        comunicChat = new ComunicacionChatImp(idInstanciaRecurso,url,nickname);
                    
                    comunicChat = new ConexionIrc();
                    interpreteMsgIrc = new InterpreteMsgsIRC(comunicChat);
                    comunicChat.setInterpreteMsgs(interpreteMsgIrc);
            trazas.aceptaNuevaTraza(new InfoTraza(this.getId(),
  				"Creando el recurso "+idInstanciaRecurso,
  				InfoTraza.NivelTraza.debug));

//           InterfazUsoAgente itfAgteControlador=(InterfazUsoAgente) this.repoIntfaces.obtenerInterfazUso(identAgenteAReportar);
//           if (itfAgteControlador == null){
//               this.wait(1000);
//               itfAgteControlador=(InterfazUsoAgente) this.repoIntfaces.obtenerInterfazUso(identAgenteAReportar);
//               this.generarErrorCreacionComponente("itfAgteControlador es null");
//           }
//           ItfUsoExtractorSemantico itfExtractorSem=(ItfUsoExtractorSemantico) this.repoIntfaces.obtenerInterfazUso(identExtractorSem);
//           if (itfExtractorSem == null){
//               this.generarErrorCreacionComponente("itfExtractorSemantico es null");
//           }
//           if (itfExtractorSem == null ||itfAgteControlador == null )throw new Exception();
//           interpreteMsgIrc.setItfusoAgenteGestorDialogo(itfAgteControlador);
//           interpreteMsgIrc.setItfusoRecExtractorSemantico(itfExtractorSem);
////            comunicChat.conectar();
//            comunicChat.setVerbose(true); //Debugging -> false
//            conectado=comunicChat.isConnected();
//        while(!conectado)
//        {
//            try {
//                comunicChat.connect(url);
//                comunicChat.joinChannel(chanel);
//                comunicChat.changeNick(nickname);
//                conectado= true;
////                this.sendMessage("pacopa2", "hola hola");
//            } catch (IOException ex) {
//                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
//            } catch (IrcException ex) {
//                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
//            } catch (NickAlreadyInUseException ex) {
//                Logger.getLogger(ConexionIrc.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }           
        
        } catch (Exception e) {
                        e.printStackTrace();
                        this.trazas.aceptaNuevaTraza(new InfoTraza(id,
  				"Se ha producido un error al crear el extractor semantico  "+e.getMessage()+
                                ": Verificar los parametros de creacion "
                                + "rutas y otros",
  				InfoTraza.NivelTraza.error));
			this.itfAutomata.transita("error");
			throw e;
		}
        }
private void generarErrorCreacionComponente(String textoMensaje){
    this.trazas.aceptaNuevaTraza(new InfoTraza(id,
  				"Se ha producido un error al crear el extractor semantico  "+textoMensaje+
                                ": Verificar los parametros de creacion ",
  				InfoTraza.NivelTraza.error));
			this.itfAutomata.transita("error");
                        
}
//    public Boolean nuevaConexion (String urlNueva, String canal, String nick ){
//        
//        if(conectado){
//            if(this.url.equals(urlNueva))
//                if(!this.chanel.equals(canal))this.chanel=canal;
//                    return true;
//                }
//        this.url = urlNueva;
//        comunicChat.connect(url);
//        this.chanel=canal;
//        comunicChat.joinChannel(chanel);
//        this.nickname= nick;
//            try {
//                return comunicChat.conectar(url,chanel,nickname);
//            } catch (Exception ex) {
//                Logger.getLogger(ClaseGeneradoraComunicacionChat.class.getName()).log(Level.SEVERE, null, ex);
//            }
//    }
        @Override
    public void comenzar ( String identAgteControlador)throws Exception{
        InterfazUsoAgente itfAgteControlador;
            try {
                itfAgteControlador = (InterfazUsoAgente) this.repoIntfaces.obtenerInterfazUso(identAgteControlador);
           if (itfAgteControlador == null){
               this.generarErrorCreacionComponente("itfAgteControlador es null");
           }else interpreteMsgIrc.setItfusoAgenteGestorDialogo(itfAgteControlador);
           ItfUsoExtractorSemantico itfExtractorSem=(ItfUsoExtractorSemantico) this.repoIntfaces.obtenerInterfazUso(identExtractorSem);
           if (itfExtractorSem == null){
               this.generarErrorCreacionComponente("itfExtractorSemantico es null");
           }else interpreteMsgIrc.setItfusoRecExtractorSemantico(itfExtractorSem);
           if (itfExtractorSem == null ||itfAgteControlador == null )throw new Exception();
           else{
               interpreteMsgIrc.setIdentAgenteGestorDialogo(VocabularioGestionCitas.IdentAgenteAplicacionDialogoCitas);
               interpreteMsgIrc.setIdentConexion(VocabularioGestionCitas.IdentConexionAgte);
               comunicChat.setVerbose(true);
               conectar( url, chanel, nickname);
           }
           } catch (Exception ex) {
                Logger.getLogger(ClaseGeneradoraComunicacionChat.class.getName()).log(Level.SEVERE, null, ex);
            }
           
           
//            comunicChat.conectar();
//            comunicChat.setVerbose(true); //Debugging -> false
//            conectado=comunicChat.isConnected();
    }
        @Override
    public Boolean conectar( String urlaConectar, String canal, String nick)throws Exception{
//           conectado=comunicChat.isConnected();
        if(conectado){
            if(this.url.equals(urlaConectar))
                if(!this.chanel.equals(canal))this.chanel=canal;
                    return true;
                }else conectado=false;
           
        while(!conectado)
        {
            try {
                comunicChat.connect(url);
                comunicChat.joinChannel(chanel);
                comunicChat.changeNick(nickname);
                conectado= true;
//                this.sendMessage("pacopa2", "hola hola");
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            } catch (IrcException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            } catch (NickAlreadyInUseException ex) {
                Logger.getLogger(ConexionIrc.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return conectado;
    }
        @Override
        public void enviarMensageCanal( String mensaje)throws Exception{
            comunicChat.sendMessage(chanel, mensaje);
        }
        @Override
        public void enviarMensagePrivado( String mensaje)throws Exception{
            comunicChat.sendMessage(identInterlocutorPruebas, mensaje);
        }
        @Override
        public void desconectar( )throws Exception{
            comunicChat.disconnect();
        }
        @Override
        public void setIdentAgenteAReportar(String identAgte ){
            identAgenteAReportar = identAgte;
            InterfazUsoAgente itfAgteControlador = null;
            try {
                itfAgteControlador = (InterfazUsoAgente) this.repoIntfaces.obtenerInterfazUso(identAgenteAReportar);
            } catch (Exception ex) {
                Logger.getLogger(ClaseGeneradoraComunicacionChat.class.getName()).log(Level.SEVERE, null, ex);
            }
           if (itfAgteControlador == null) this.generarErrorCreacionComponente("itfAgteAreportar es null");
           else interpreteMsgIrc.setItfusoAgenteGestorDialogo(itfAgteControlador);
        }
	@Override
	public void termina() {
//		trazas.aceptaNuevaTraza(new InfoTraza(this.getId(),
//  				"Terminando recurso",
//  				InfoTraza.NivelTraza.debug));
	//	AccesoBBDD.desconectar();
      
		try {
			super.termina();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}  
}