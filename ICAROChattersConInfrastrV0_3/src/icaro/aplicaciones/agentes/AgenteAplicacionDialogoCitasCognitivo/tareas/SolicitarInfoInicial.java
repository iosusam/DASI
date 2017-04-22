/*
 * SolicitarDatos.java
 *
 * Creado 23 de abril de 2007, 12:52
 *
 * Telefonica I+D Copyright 2006-2007
 */
package icaro.aplicaciones.agentes.AgenteAplicacionDialogoCitasCognitivo.tareas;

import icaro.aplicaciones.informacion.gestionCitas.VocabularioGestionCitas;
import icaro.aplicaciones.recursos.interfazChatUsuario.ItfUsoInterfazChatUsuario;
import icaro.infraestructura.entidadesBasicas.NombresPredefinidos;
import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.CausaTerminacionTarea;
import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.Objetivo;
import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.TareaSincrona;
/**
 * 
 * @author F Garijo
 */
public class SolicitarInfoInicial extends TareaSincrona {
//    private String identAgenteOrdenante ;
    private Objetivo contextoEjecucionTarea = null;
	@Override
	public void ejecutar(Object... params) {		
          String identRecursoChatSimple = (String)params[0];
                    try {
//         // Se busca la interfaz del recurso en el repositorio de interfaces 
//		ItfUsoComunicacionChat recComunicacionChat = (ItfUsoComunicacionChat) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfaz(
//						NombresPredefinidos.ITF_USO + identRecursoComunicacionChat);
                ItfUsoInterfazChatUsuario itfRecChatSimple = null;
                while (itfRecChatSimple==null ){
                    
                  itfRecChatSimple= (ItfUsoInterfazChatUsuario) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfazUso(
						identRecursoChatSimple);      
                 //esperamos y reintentamos 
                 Thread.sleep (1000);
//                    recComunicacionChat.comenzar(identAgenteOrdenante);
//                    recComunicacionChat.enviarMensagePrivado(VocabularioGestionCitas.SaludoInicial1);
                }
                if(itfRecChatSimple!=null ){
                    itfRecChatSimple.setIdentAgteAreportar(identAgente);
                    itfRecChatSimple.mostrarTexto(VocabularioGestionCitas.SaludoInicial1);
                    
                }
                else {
                    this.identAgente = this.getAgente().getIdentAgente();
                     this.generarInformeConCausaTerminacion(identTarea, contextoEjecucionTarea, identAgente, "Error-AlObtener:Interfaz:"+identRecursoChatSimple, CausaTerminacionTarea.ERROR);
                        }
                    } catch(Exception e) {
                        this.generarInformeConCausaTerminacion(identTarea, contextoEjecucionTarea, identAgente, "Error-Acceso:Interfaz:"+identRecursoChatSimple, CausaTerminacionTarea.ERROR);
			e.printStackTrace();
		}
	}
}
