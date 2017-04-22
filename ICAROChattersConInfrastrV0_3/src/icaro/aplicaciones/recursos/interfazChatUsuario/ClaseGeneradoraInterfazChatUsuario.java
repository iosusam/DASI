package icaro.aplicaciones.recursos.interfazChatUsuario;

import icaro.aplicaciones.informacion.gestionCitas.VocabularioGestionCitas;
import icaro.aplicaciones.recursos.extractorSemantico.ItfUsoExtractorSemantico;
import icaro.aplicaciones.recursos.interfazChatUsuario.ItfUsoInterfazChatUsuario;
import icaro.aplicaciones.recursos.interfazChatUsuario.imp.InterpreteMsgsPanelChat;
import icaro.aplicaciones.recursos.interfazChatUsuario.imp.NotificacionesEventosItfUsuarioChat;
import icaro.aplicaciones.recursos.interfazChatUsuario.imp.VisualizadorChatSimple;
import icaro.infraestructura.entidadesBasicas.NombresPredefinidos;
import icaro.infraestructura.entidadesBasicas.interfaces.InterfazUsoAgente;
import icaro.infraestructura.patronRecursoSimple.imp.ImplRecursoSimple;
import icaro.infraestructura.recursosOrganizacion.comunicacionesOrganizacion.imp.RMI.ControlRMI;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.ItfUsoRecursoTrazas;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;
import icaro.infraestructura.recursosOrganizacion.repositorioInterfaces.ItfUsoRepositorioInterfaces;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;



/**
 * 
 *@author     Fgarijo
 *@created    12/04/2016
 */

public class ClaseGeneradoraInterfazChatUsuario extends ImplRecursoSimple implements ItfUsoInterfazChatUsuario {

	private static final long serialVersionUID = 1L;

	//Interfaz de uso del agente de acceso
	private InterfazUsoAgente itfUsoAgenteAReportar;
	
	//Ventana que gestiona este visualizador
	private ItfUsoRecursoTrazas trazas; //trazas del sistema
        private InterfazUsoAgente itfUsoAgteAreportar;
	private String identAgenteAreportar ;
        private String tipoAgenteAreportar ;
        private String nombredeEsteRecurso;
        private String agenteaReportar;
        private VisualizadorChatSimple visorChatSimple ;
        private Object notifEvt;
    private final String identExtractorSem;
    private InterpreteMsgsPanelChat interpreteMsgUsuario;
    private ItfUsoExtractorSemantico itfRecExtractorSem;
  	public ClaseGeneradoraInterfazChatUsuario(String id) throws Exception{
  		super(id);
                nombredeEsteRecurso = id;
                trazas = NombresPredefinidos.RECURSO_TRAZAS_OBJ;
//                notifAgente = new NotificacionesEventosItfUsuarioChat(nombredeEsteRecurso,nombreAgenteAreportar);
//                this.ventanaAccesoUsuario = new PanelChatUsuario(notifAgente);
//                this.visorChatSimple = new VisualizadorChatSimple();
                identExtractorSem = VocabularioGestionCitas.IdentRecursoExtractorSemantico;
  		this.inicializa();
	}
  	
  	private void inicializa() throws Exception {
            this.visorChatSimple = new VisualizadorChatSimple();
            identAgenteAreportar = VocabularioGestionCitas.IdentAgenteAplicacionDialogoCitas;
            interpreteMsgUsuario = new InterpreteMsgsPanelChat(this.nombredeEsteRecurso,this.identAgenteAreportar);
            visorChatSimple.setInterpreteTextoUsuario(interpreteMsgUsuario);
            trazas.aceptaNuevaTraza(new InfoTraza(nombredeEsteRecurso,
  				"Inicializando recurso",
  				InfoTraza.NivelTraza.debug));
  		
  	}
  	@Override
    public void setIdentAgteAreportar(String identAgte) throws Exception {
        identAgenteAreportar = identAgte;
        interpreteMsgUsuario.setIdentAgenteGestorDialogo(identAgte);
        itfUsoAgteAreportar= this.getItfUsoAgenteAReportar(identAgenteAreportar);
        interpreteMsgUsuario.setItfusoAgenteGestorDialogo(itfUsoAgteAreportar);
        itfRecExtractorSem = this.getItfUsoExtractorSemantico(identExtractorSem);
        interpreteMsgUsuario.setItfusoRecExtractorSemantico(itfRecExtractorSem);
    }
	
        @Override
	public void mostrarMensajeError(String titulo,String mensaje) {
		trazas.aceptaNuevaTraza(new InfoTraza(nombredeEsteRecurso,
  				"Mostrando mensaje de error",
  				InfoTraza.NivelTraza.debug));
      	JOptionPane.showMessageDialog(visorChatSimple,mensaje,titulo,JOptionPane.ERROR_MESSAGE);
	}	
	
	@Override
	public void termina() {
		this.visorChatSimple.dispose();
		try {
			trazas.aceptaNuevaTraza(new InfoTraza(nombredeEsteRecurso,
	  				"Terminando recurso",
	  				InfoTraza.NivelTraza.debug));
			super.termina();
		} catch (Exception e) {
			this.itfAutomata.transita("error");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
        public InterfazUsoAgente getItfUsoAgenteAReportar(String identAgteAreportar) {

             try {
		itfUsoAgteAreportar = (InterfazUsoAgente) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfaz
		(NombresPredefinidos.ITF_USO+identAgteAreportar);
		}
		catch (Exception e) {
			Logger.getLogger(NotificacionesEventosItfUsuarioChat.class.getName()).log(Level.SEVERE, null, e);
    //      Logger.error("Ha habido un problema enviar un  evento al agente "+IdentAgenteReceptor);
			this.trazas.aceptaNuevaTraza(new InfoTraza(this.nombredeEsteRecurso,
									"Ha habido un problema al obtener la interfaz de uso del agente: "+identAgteAreportar,
									InfoTraza.NivelTraza.error));
			}
             return itfUsoAgteAreportar;
	}
        
        public ItfUsoExtractorSemantico getItfUsoExtractorSemantico(String idExtractorSem) {
            ItfUsoExtractorSemantico itfUsoExtracSem = null;
             try {
	     itfUsoExtracSem = (ItfUsoExtractorSemantico) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfaz
		(NombresPredefinidos.ITF_USO+idExtractorSem);
		}
		catch (Exception e) {
			Logger.getLogger(NotificacionesEventosItfUsuarioChat.class.getName()).log(Level.SEVERE, null, e);
    //      Logger.error("Ha habido un problema enviar un  evento al agente "+IdentAgenteReceptor);
			this.trazas.aceptaNuevaTraza(new InfoTraza(this.nombredeEsteRecurso,
									"Ha habido un problema al obtener la interfaz de uso del recurso: "+idExtractorSem,
									InfoTraza.NivelTraza.error));
			}
             return itfUsoExtracSem;
	}
        public String getNombredeEsteRecurso() {
		return nombredeEsteRecurso;
	}
        
        @Override
        public void mostrarVisualizadorChatUsuario(String nombreAgente,String tipo) throws Exception{
         this.agenteaReportar =  nombreAgente;
         this.tipoAgenteAreportar = tipo;
         this.visorChatSimple.setVisible(true);
        }

        @Override
        public void mostrarTexto(String textoAmostrar) throws Exception{
            visorChatSimple.addText(textoAmostrar);
        }
        @Override
       public void cerrarVisualizadorChatUsuario() throws Exception{
            this.visorChatSimple.setVisible(false);
        }
public void informeError(String msgError) {
		this.itfAutomata.transita("error");
        trazas.aceptaNuevaTraza(new InfoTraza(nombredeEsteRecurso,
	  				"se produjo un error al enviar un evento ",
	  				InfoTraza.NivelTraza.debug));
	}

    @Override
    public void mostrarMensajeAviso(String titulo, String mensaje) throws Exception {
        JOptionPane.showMessageDialog(visorChatSimple,mensaje,titulo,JOptionPane.OK_OPTION);
    }

    
}