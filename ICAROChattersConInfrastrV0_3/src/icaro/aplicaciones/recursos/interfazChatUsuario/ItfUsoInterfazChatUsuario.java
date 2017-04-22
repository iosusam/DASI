package icaro.aplicaciones.recursos.interfazChatUsuario;

/**
 * 
 *@author     
 *@created    
 */

public interface ItfUsoInterfazChatUsuario {

        public void mostrarVisualizadorChatUsuario(String nombreAgente,String tipo) throws Exception;
	public void cerrarVisualizadorChatUsuario() throws Exception;
        public void setIdentAgteAreportar(String identAgte) throws Exception;
//        public void mostrarInfoMensajeEnviado(String mensaje) throws Exception;
//        public void mostrarInfoMensajeRecibido(String mensaje) throws Exception;
//  	public void mostrarMensajeInformacion(String titulo,String mensaje) throws Exception;
        public void mostrarTexto(String testoAmostrar) throws Exception;
  	public void mostrarMensajeAviso(String titulo,String mensaje) throws Exception;
  	public void mostrarMensajeError(String titulo,String mensaje) throws Exception;	
}