package icaro.aplicaciones.recursos.interfazChatUsuario.imp;

import icaro.aplicaciones.recursos.interfazChatUsuario.ClaseGeneradoraInterfazChatUsuario;
import icaro.infraestructura.entidadesBasicas.comunicacion.EventoRecAgte;
import icaro.infraestructura.entidadesBasicas.NombresPredefinidos;
import icaro.infraestructura.entidadesBasicas.comunicacion.ComunicacionAgentes;
import icaro.infraestructura.entidadesBasicas.comunicacion.InfoContEvtMsgAgteReactivo;
import icaro.infraestructura.entidadesBasicas.excepciones.ExcepcionEnComponente;
import icaro.infraestructura.entidadesBasicas.interfaces.InterfazUsoAgente;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.ItfUsoRecursoTrazas;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;
import icaro.infraestructura.recursosOrganizacion.repositorioInterfaces.ItfUsoRepositorioInterfaces;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * 
 *@author    Francisco J Garijo
 *@created    Febrero 2009
 */

public class NotificacionesEventosItfUsuarioChat extends ComunicacionAgentes{
	
    protected ItfUsoRepositorioInterfaces itfUsoRepositorioInterfaces;
    protected ItfUsoRecursoTrazas trazas;
    protected ClaseGeneradoraInterfazChatUsuario generadoraVisualizador;
    protected InterfazUsoAgente itfUsoAgenteaReportar;
    protected String identificadorAgenteaReportar;
    protected String identificadordeEsteRecurso;
    
	
public NotificacionesEventosItfUsuarioChat (String identRecurso, String identAgteAreportar)throws Exception {
// Obtenemos las informaciones que necesitamos de la clase generadora del recurso
//        generadoraVisualizador = generadoraVis;
    super (identRecurso)  ;      
        trazas = NombresPredefinidos.RECURSO_TRAZAS_OBJ;
        identificadordeEsteRecurso = identRecurso;
        identificadorAgenteaReportar = identAgteAreportar;
   }
public void setIdentAgenteAReportar(String identAgenteAReportar){
        identificadorAgenteaReportar =identAgenteAReportar ;
        
    }
public void enviarTextoUsuarioAgteDialogo(String texto){
if ( identificadorAgenteaReportar!=null){
        this.enviarInfoAotroAgente(texto,identificadorAgenteaReportar);
        }
        else try {
            throw new ExcepcionEnComponente ("El identificador del Gestor de dialogo no esta definido",this.getClass().getSimpleName(),null);
        } catch (ExcepcionEnComponente ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
}
 public void peticionCierreSistema() {
     InfoContEvtMsgAgteReactivo infoAEnviar = new InfoContEvtMsgAgteReactivo("peticion_terminacion_usuario");
     this.informaraOtroAgenteReactivo(infoAEnviar, NombresPredefinidos.NOMBRE_GESTOR_ORGANIZACION);
 }
 public void notificacionCierreSistema() throws Exception{
	//cierre de ventanas que genera cierre del sistema
	
		if (identificadorAgenteaReportar == null){
                System.out.println("El identificador del agente al que  hay que enviar los eventos es null");
                NombresPredefinidos.RECURSO_TRAZAS_OBJ.aceptaNuevaTraza(new InfoTraza(this.identificadordeEsteRecurso,
					"No se puede enviar el evento pq el identificador del  agente a reportar   es null ",
					InfoTraza.NivelTraza.error));
                }else {
                if (itfUsoAgenteaReportar == null){
			 itfUsoAgenteaReportar = (InterfazUsoAgente) NombresPredefinidos.REPOSITORIO_INTERFACES_OBJ.obtenerInterfaz(NombresPredefinidos.ITF_USO+identificadorAgenteaReportar);
                         if (itfUsoAgenteaReportar == null)

            this.trazas.aceptaNuevaTraza(new InfoTraza(this.identificadordeEsteRecurso,
			"No se puede enviar el evento pq la interfaz de uso del agente "+identificadorAgenteaReportar + " es null ",
			InfoTraza.NivelTraza.error));
                                    }
                else {
        try {
            itfUsoAgenteaReportar.aceptaEvento(new EventoRecAgte("peticion_terminacion_usuario",identificadordeEsteRecurso,identificadorAgenteaReportar));
			
		} catch (Exception e) {
			System.out.println("Ha habido un error al enviar el evento termina al agente");
			e.printStackTrace();
                    }
                }
            }
        }

 
  }