package icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp;

import icaro.infraestructura.entidadesBasicas.NombresPredefinidos;
import icaro.infraestructura.entidadesBasicas.comunicacion.EventoSimple;
import icaro.infraestructura.entidadesBasicas.comunicacion.MensajeSimple;
import icaro.infraestructura.patronAgenteCognitivo.factoriaEInterfacesPatCogn.AgenteCognitivo;
import icaro.infraestructura.patronAgenteCognitivo.percepcion.imp.ItfProcesadorItems;
import icaro.infraestructura.patronAgenteCognitivo.percepcion.imp.PercepcionAgenteCognitivoImp;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.ClasificadorTextual;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.ClasificadorVisual;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza.NivelTraza;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.VisualizacionTrazasContr;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.openide.util.Exceptions;


/**
 * @author Francisco J Garijo
 * @version 1.0
 * @created 17-marzo-2017 10:20:43
 */
public class RecursoTrazasImp1 extends ClaseGeneradoraRecursoTrazas implements Serializable{

	//private Properties properties;
	/**
	 * @uml.property  name="visual"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private static final int CAPACIDAD_BUZON_PORDEFECTO = 30;
//	private ItfProcesadorItems procesador;
//      private ProcesadorItems procesador ;
	private BlockingQueue<InfoTraza> buzon;
	private RecursoTrazasImp1.EnvioItemsThread trazaItems;
	
	private AgenteCognitivo agente;
        private ExecutorService executorService1;
        private Future ejecucionHebra;
	private Logger log = Logger.getLogger(PercepcionAgenteCognitivoImp.class);
	private transient ClasificadorVisual visual;
        private VisualizacionTrazasContr controlVisulizacionTrazas;
    private ProductorTrazas envioItems;
        public static enum TipoEntidad {Cognitivo,ADO,DirigidoPorObjetivos,Reactivo,Recurso,noDefinido}
	/**
	 * @uml.property  name="textual"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
	private transient ClasificadorTextual textual;
        private Boolean activacionPanelTrazas = false;
        private transient Logger logger = Logger
			.getLogger(this.getClass().getCanonicalName());

	public RecursoTrazasImp1(String id) throws RemoteException {
		super(id);
		creacionRecursoTrazas();
	}
	
	public void creacionRecursoTrazas() {
            textual = new ClasificadorTextual();
            buzon = new LinkedBlockingQueue<>(CAPACIDAD_BUZON_PORDEFECTO);
            controlVisulizacionTrazas = new VisualizacionTrazasContr ();
            trazaItems= new EnvioItemsThread();
            envioItems= new ProductorTrazas();
            if (NombresPredefinidos.ACTIVACION_PANEL_TRAZAS_DEBUG.startsWith("t")){
                activacionPanelTrazas = true;
                controlVisulizacionTrazas.activarVisualizacionTrazas();
            }else controlVisulizacionTrazas.desactivarVisualizacionTrazas();
	}

    @Override
	public void visualizacionDeTrazas(Boolean opcionTraza) {
      // Permite cambiar la opcion de visualizar o no las trazas
        //ActivacionPanelTrazas = opcionTraza;
        if (opcionTraza)this.controlVisulizacionTrazas.activarVisualizacionTrazas();
                else this.controlVisulizacionTrazas.desactivarVisualizacionTrazas();
        activacionPanelTrazas=opcionTraza;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ejecucionHebra=executor.submit(trazaItems);
        envioItems.run();
		
    }

    @Override
        public synchronized void trazar(String entidad, String infotraza, NivelTraza nivelTraza) {
         if ( activacionPanelTrazas )
            aceptaNuevaTraza(new InfoTraza(entidad,infotraza,nivelTraza ));
        }

    @Override
	public synchronized void aceptaNuevaTraza(InfoTraza traza) {  
         if ( activacionPanelTrazas )
//             try {
                 //        controlVisulizacionTrazas.visualizarNuevaTraza(traza);
                 this.envioItems.addTraza(traza);
//         } catch (InterruptedException ex) {
//             Exceptions.printStackTrace(ex);
//         }
	}
    	
    @Override
	public synchronized void visualizaNuevaTraza(InfoTraza traza) {
		//saca la informacin slo de manera visual
	 if ( activacionPanelTrazas )	
		this.aceptaNuevaTraza(traza);
		
	}
    @Override
        public void setIdentAgenteAReportar(String nombreAgente){
        controlVisulizacionTrazas.setIdentAgenteAReportar(nombreAgente);
        }
    @Override
    public void visualizarComponentesTrazables(List<String> listaElementosaTrazar)
    {
       if ( activacionPanelTrazas && (listaElementosaTrazar != null )){
        visual.visualizarElementosTrazables(listaElementosaTrazar);
        }
	}
    @Override
    public void visualizarIdentFicheroDescrOrganizacion(){
         if ( activacionPanelTrazas )
             controlVisulizacionTrazas.visualizarIdentFicheroDescrOrganizacion();
        
    }
            
        @Override
	public void termina() {
		//elimino todas las ventanas
         if ( activacionPanelTrazas )
        controlVisulizacionTrazas.cerrarVentanas();
	
		super.termina();
	}
    @Override
        public void mostrarMensajeError(String mensaje){
            JOptionPane.showMessageDialog(null, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        }
       
    @Override
	public synchronized void pedirConfirmacionTerminacionAlUsuario() {
            
            controlVisulizacionTrazas.pedirConfirmacionTerminacionAlUsuario();
		
	}

    @Override
    public synchronized void aceptaNuevaTrazaEnviarMensaje(MensajeSimple trazaMensaje) {
        try {
          if ( activacionPanelTrazas ){
                   //muestraTraza(traza);
            controlVisulizacionTrazas.visualizarEnvioMensaje(NombresPredefinidos.TipoEntidad.noDefinido, trazaMensaje);
          }
        }catch (Exception e) {
		logger.fatal("Error al mostrar la traza. Hay un problema con el recurso de trazas,",e);
            }
    }
    @Override
    public synchronized void aceptaNuevaTrazaEnviarEvento(EventoSimple trazaEvento) {
        try {
          if ( activacionPanelTrazas ){
                   //muestraTraza(traza);
               controlVisulizacionTrazas.visualizarEnvioEvento(NombresPredefinidos.TipoEntidad.noDefinido, trazaEvento);
          }
        }catch (Exception e) {
		logger.fatal("Error al mostrar la traza. Hay un problema con el recurso de trazas,",e);
            }
    }
   
    public synchronized void trazarEnviarEvento(NombresPredefinidos.TipoEntidad tipoEnt,EventoSimple trazaEvento) {
        try {
          if ( activacionPanelTrazas ){
                   //muestraTraza(traza);
               controlVisulizacionTrazas.visualizarEnvioEvento(tipoEnt, trazaEvento);
           
             
          }
        }catch (Exception e) {
		logger.fatal("Error al mostrar la traza. Hay un problema con el recurso de trazas,",e);
            }
    }
  
    @Override
    public synchronized void aceptaNuevaTrazaMensajeRecibido(MensajeSimple trazaMensaje) {
        try {
          if ( activacionPanelTrazas ){
                   //muestraTraza(traza);
              controlVisulizacionTrazas.visualizarRecibirMensaje(NombresPredefinidos.TipoEntidad.noDefinido, trazaMensaje);
        /*
            panelActual = (PanelTrazasAbstracto)tablaPanelesEspecificos.getPanelEspecifico(trazaMensaje.getReceptor().toString());
            if (panelActual == null){
                String identElementTraza = trazaMensaje.getEmisor().toString();
                panelActual =  tablaPanelesEspecificos.crearPanelparaEntidad (identElementTraza); 
                panelTrazasNiveles.visualizarElementoTrazable( identElementTraza);
            }
            panelActual.muestraMensajeRecibido(trazaMensaje);
            * 
            */
          }
        }catch (Exception e) {
		logger.fatal("Error al mostrar la traza. Hay un problema con el recurso de trazas,",e);
            }
    }
    @Override
    public synchronized void aceptaNuevaTrazaEventoRecibido(String entityId, EventoSimple trazaEvento) {
        try {
          if ( activacionPanelTrazas ){
               controlVisulizacionTrazas.visualizarRecibirEvento(NombresPredefinidos.TipoEntidad.noDefinido, trazaEvento);
              
              /*
                 panelActual = (PanelTrazasAbstracto)tablaPanelesEspecificos.getPanelEspecifico(entityId);
                if (panelActual == null){
               
                panelActual =  tablaPanelesEspecificos.crearPanelparaEntidad (entityId); 
                panelTrazasNiveles.visualizarElementoTrazable( entityId);
            }
                panelActual.muestraEventoEnviado(trazaEvento);
                * 
                */
          }
        }catch (Exception e) {
		logger.fatal("Error al mostrar la traza. Hay un problema con el recurso de trazas,",e);
            }
    }
    public synchronized void trazaEventoRecibido(String entityId,NombresPredefinidos.TipoEntidad tipoEnt, EventoSimple trazaEvento) {
        try {
          if ( activacionPanelTrazas ){
               controlVisulizacionTrazas.visualizarRecibirEvento(NombresPredefinidos.TipoEntidad.noDefinido, trazaEvento);
              /*
                 panelActual = (PanelTrazasAbstracto)tablaPanelesEspecificos.getPanelEspecifico(entityId);
                if (panelActual == null){
                String identElementTraza = trazaEvento.getOrigen();
                panelActual =  tablaPanelesEspecificos.crearPanelparaEntidad (identElementTraza,tipoEnt.name()); 
                panelTrazasNiveles.visualizarElementoTrazable( identElementTraza);
            }
                panelActual.muestraEventoRecibido(trazaEvento);
                * 
                */
          }
        }catch (Exception e) {
		logger.fatal("Error al mostrar la traza. Hay un problema con el recurso de trazas,",e);
            }
    }
     
    @Override
    public synchronized void aceptaNuevaTrazaEjecReglas(String entityId, String  infoAtrazar) {
        try {
          if ( activacionPanelTrazas ){
             controlVisulizacionTrazas.visualizarTrazaEjecReglas(entityId, infoAtrazar); 
             //muestraTraza(traza);
              /*
            panelActual = (PanelTrazasAbstracto)tablaPanelesEspecificos.getPanelEspecifico(entityId);
            if (panelActual == null){
           //     String identElementTraza = trazaEvento.getOrigen();
                panelActual =  tablaPanelesEspecificos.crearPanelparaEntidad (entityId,TipoEntidad.Cognitivo.name()); 
                panelTrazasNiveles.visualizarElementoTrazable( entityId);
            }
               panelActual.muestraTrazaEjecucionReglas(infoAtrazar);
               * 
               */
            
          }
        }catch (Exception e) {
		logger.fatal("Error al mostrar la traza. Hay un problema con el recurso de trazas,",e);
            }
    }    
       
    @Override
    public synchronized void aceptaNuevaTrazaActivReglas(String entityId, String  infoAtrazar) {
        try {
          if ( activacionPanelTrazas ){
              
              controlVisulizacionTrazas.visualizarTrazaActivacionReglas(entityId, infoAtrazar); 
              /*
            panelActual = (PanelTrazasAbstracto)tablaPanelesEspecificos.getPanelEspecifico(entityId);
            if (panelActual == null){
           //     String identElementTraza = trazaEvento.getOrigen();
                panelActual =  tablaPanelesEspecificos.crearPanelparaEntidad (entityId,TipoEntidad.Cognitivo.name()); 
                panelTrazasNiveles.visualizarElementoTrazable( entityId);
            }
               panelActual.muestraTrazaActivacionReglas(infoAtrazar);
         
         * 
         */
          }
        }catch (Exception e) {
		logger.fatal("Error al mostrar la traza. Hay un problema con el recurso de trazas,",e);
            }
     }
    
    private class EnvioItemsThread implements Runnable{

	private static final long TIEMPO_ESPERA = 1000;		
	private boolean termina;
	public EnvioItemsThread() {
//			this.setDaemon(true);
			termina = false;
	}
                @Override
	public void run() {
            while (!termina) {
		InfoTraza item = null;
		try {
			log.debug("Recogiendo item desde el buzon de items del recurso de trazas...");
			item = buzon.take();
			if (item != null) {
//						 procesador.procesarItem(item);
                          controlVisulizacionTrazas.visualizarNuevaTraza(item);
			this.wait(TIEMPO_ESPERA);
					}
			else 
			log.debug("Item == NULL!!!!!");
			} catch (InterruptedException e) {
			log.debug("Interrumpida la espera de nuevo item en el buzon de items");
		}
			}
		log.debug("Terminando EnvioItems");
	}
    
		
	public void termina() {
		this.termina = true;
//		envioItems.interrupt();
	}		
	}
     private class ProductorTrazas implements Runnable{

		private static final long TIEMPO_ESPERA = 1000;		
		private boolean termina;
                private InfoTraza traza;
                
		public ProductorTrazas() {
//			this.setDaemon(true);
			termina = false;
		}
                public void addTraza(InfoTraza trazaNueva){
                    traza=trazaNueva;
                }
            @Override
            public void run() {
                while (!termina) {
                    try {
                    log.debug("Recogiendo item desde el buzon de items del recurso de trazas...");
                    buzon.put(traza);
//                    this.wait(TIEMPO_ESPERA);
		} catch (InterruptedException e) {
                    log.debug("Interrumpida la espera de nuevo item en el buzon de items");
		}
            }
		log.debug("Encolado  de trazas ");
		}

		public void termina() {
			this.termina = true;
//			envioItems.interrupt();
		}		
	}

}