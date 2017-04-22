package icaro.aplicaciones.agentes.AgenteAplicacionDialogoCitasCognitivo.tareas;

/**
 * <p>Title: Agenda de citas vocal</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Telef�nica  I+D</p>
 * @author Jorge Gonz�lez
 * @version 1.0
 */


import icaro.aplicaciones.agentes.AgenteAplicacionDialogoCitasCognitivo.objetivos.ObtenerInfoInterlocutor;
import icaro.aplicaciones.informacion.gestionCitas.VocabularioGestionCitas;
import icaro.aplicaciones.recursos.comunicacionChat.ItfUsoComunicacionChat;
import icaro.aplicaciones.recursos.interfazChatUsuario.ItfUsoInterfazChatUsuario;
import icaro.infraestructura.entidadesBasicas.NombresPredefinidos;
import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.CausaTerminacionTarea;
import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.Focus;
import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.Objetivo;
import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.TareaSincrona;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;

public class SaludarYPresentarFuncionalidad extends TareaSincrona{

  /**
   *  Constructor
   *
   *@param           Description of the Parameter
   *@param    Description of the Parameter
   */
    private Objetivo contextoEjecucionTarea = null;
  @Override
	public void ejecutar(Object... params) {
  /**
   * Produce un saludo inicial y una presentacion de funcionalidad inicial al entrar en el sistema
   */
        
          String identInterlocutor = (String)params[0];
                    try {
//         // Se busca la interfaz del recurso en el repositorio de interfaces 
		ItfUsoInterfazChatUsuario recComunicacionChat = (ItfUsoInterfazChatUsuario) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfazUso(
						VocabularioGestionCitas.IdentRecursoComunicacionChat);          
                if (recComunicacionChat!=null){
                    recComunicacionChat.setIdentAgteAreportar(this.identAgente);
                    String mensajeAenviar = VocabularioGestionCitas.SaludoInicial2+ "  "+ identInterlocutor + "  "+
                            VocabularioGestionCitas.InfoGeneralFuncionalidad + "  "+
                            VocabularioGestionCitas.PeticionInformacionGeneral1;
//                    recComunicacionChat.enviarMensagePrivado(mensajeAenviar);
                    recComunicacionChat.mostrarTexto(mensajeAenviar);
                }
                else {
                    this.identAgente = this.getAgente().getIdentAgente();
                     this.generarInformeConCausaTerminacion(this.identTarea, contextoEjecucionTarea, this.identAgente, "Error-AlObtener:Interfaz:"+
                             VocabularioGestionCitas.IdentRecursoComunicacionChat, CausaTerminacionTarea.ERROR);
                        }
                    } catch(Exception e) {
                        this.generarInformeConCausaTerminacion(this.identTarea, contextoEjecucionTarea, this.identAgente, "Error-Acceso:Interfaz:"+
                                VocabularioGestionCitas.IdentRecursoComunicacionChat, CausaTerminacionTarea.ERROR);
			e.printStackTrace();
		}
	}
  

}
