package icaro.infraestructura.patronAgenteReactivo.percepcion.factoriaEInterfaces.imp;
import icaro.infraestructura.entidadesBasicas.NombresPredefinidos;
import icaro.infraestructura.entidadesBasicas.comunicacion.EventoSimple;
import icaro.infraestructura.entidadesBasicas.comunicacion.EventoRecAgte;
import icaro.infraestructura.entidadesBasicas.comunicacion.MensajeSimple;
import icaro.infraestructura.patronAgenteReactivo.factoriaEInterfaces.AgenteReactivoAbstracto;
import icaro.infraestructura.patronAgenteReactivo.percepcion.factoriaEInterfaces.ExcepcionSuperadoTiempoLimite;
import icaro.infraestructura.patronAgenteReactivo.percepcion.factoriaEInterfaces.PercepcionAbstracto;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.ItfUsoRecursoTrazas;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedTransferQueue;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.apache.log4j.Logger;

/**
 * Implementation for Reactive Agent Perception
 */
public class PercepcionAgenteReactivoImp extends PercepcionAbstracto {

	private static final int CAPACIDAD_BUZON_PORDEFECTO = 15;
	private ProcesadorItemsPercepReactivo procesador;
	private BlockingDeque<Object> buzon;
	private EnvioItemsThread envioItems;
        static final AtomicLong seq = new AtomicLong(0);
        private final ExecutorService executor;
	private AgenteReactivoAbstracto agente;
        private Future ejecucionHebra;
	private Logger log = Logger.getLogger(PercepcionAgenteReactivoImp.class);
	private ItfUsoRecursoTrazas trazas=NombresPredefinidos.RECURSO_TRAZAS_OBJ ;

	public PercepcionAgenteReactivoImp() {
        this.executor = Executors.newCachedThreadPool();
	// Se crea un objeto vacio. Es necesario definir los parametros para que funcione correctamente
                 buzon = null;
                 this.agente = null;
                 this.procesador = null;
//		this.envioItems = new EnvioItemsThread();
	}
        public PercepcionAgenteReactivoImp(AgenteReactivoAbstracto agente) {
                this.executor = Executors.newCachedThreadPool();
		buzon = new LinkedBlockingDeque<>();
		this.agente = agente;
		this.procesador = new ProcesadorItemsPercepReactivo(agente, agente.getItfControl());
		this.envioItems = new EnvioItemsThread();

	}
    public PercepcionAgenteReactivoImp(int CapacidadBuzon, ProcesadorItemsPercepReactivo prItems,AgenteReactivoAbstracto agente) {
                this.executor = Executors.newCachedThreadPool();
		buzon = new LinkedBlockingDeque<>();
		this.procesador = prItems;
		this.envioItems = new EnvioItemsThread();

	}
	public PercepcionAgenteReactivoImp(AgenteReactivoAbstracto agente,ProcesadorItemsPercepReactivo procesador) {
                this.executor = Executors.newCachedThreadPool();
                this.agente = agente;
                this.procesador = procesador;
		buzon = new LinkedBlockingDeque<>();
                this.envioItems = new EnvioItemsThread();
	}
 public void SetParametrosPercepcionAgenteReactivoImp(BlockingDeque<Object> colaEvtosyMsgs, ProcesadorItemsPercepReactivo prItems,AgenteReactivoAbstracto agente) {
		this.buzon=colaEvtosyMsgs;
		this.agente = agente;
		this.procesador = prItems;
		this.envioItems = new EnvioItemsThread();

	}

	public synchronized void aceptaEvento(EventoSimple evento) throws RemoteException {
		trazas.aceptaNuevaTraza(new InfoTraza(this.agente.getIdentAgente(),"Percepcion: Ha llegado un nuevo evento desde "+ evento.getOrigen(),InfoTraza.NivelTraza.debug));
               trazas.aceptaNuevaTrazaEventoRecibido(this.agente.getIdentAgente(), evento);
		buzon.offerLast(evento);
	}


    public synchronized void aceptaEvento(EventoRecAgte evento)throws Exception {

                trazas.aceptaNuevaTraza(new InfoTraza(this.agente.getIdentAgente(),"Percepcion: Ha llegado un nuevo evento desde "+ evento.getOrigen(),InfoTraza.NivelTraza.debug));
                trazas.aceptaNuevaTrazaEventoRecibido(this.agente.getIdentAgente(), evento);
		buzon.offerLast(evento);
    }

	public synchronized void aceptaMensaje(MensajeSimple mensaje) throws RemoteException {
		trazas.aceptaNuevaTraza(new InfoTraza(this.agente.getIdentAgente(),"Percepcion: Ha llegado un nuevo mensaje desde "+mensaje.getEmisor(),InfoTraza.NivelTraza.debug));
		trazas.aceptaNuevaTrazaEnviarMensaje(mensaje);
                buzon.offerLast(mensaje);
	}


	private class EnvioItemsThread implements Runnable {

		private static final long TIEMPO_ESPERA = 100;

		private boolean termina;

		public EnvioItemsThread() {
//			this.setDaemon(true);
			termina = false;
		}

                @Override
		public void run() {
                    Object item = null;
			while (!termina) {
				try {
					
                                        if (! buzon.isEmpty()){
                                            item = buzon.takeFirst(); 
                                            log.debug("Recogiendo item desde el buzon de items de la percepcion...");
                                             procesador.procesarItem(item); 
					}
					else
                                        Thread.sleep(TIEMPO_ESPERA);
				} catch (InterruptedException e) {
					log.debug("Interrumpida la espera de nuevo item en el buzon de items");
				} catch (RemoteException ex) {
                                    java.util.logging.Logger.getLogger(PercepcionAgenteReactivoImp.class.getName()).log(Level.SEVERE, null, ex);
                            }
			}
			log.debug("Terminando EnvioItems");
		}

		public void termina() {
			this.termina = true;
		}
	}


        @Override
	public void termina() {
		this.envioItems.termina();
		this.buzon.clear();
//		this.procesador.termina();
                ejecucionHebra.cancel(true);
	}

        @Override
	public void arranca() {
//	ExecutorService executor = Executors.newSingleThreadExecutor();
//      ExecutorService executor = Executors.newCachedThreadPool();
        ejecucionHebra=executor.submit(envioItems);
//		this.procesador.arranca();

	}

        @Override
        public Object consumeConTimeout(int tiempoEnMilisegundos)throws ExcepcionSuperadoTiempoLimite {
    // No tiene mucho sentido la ponemos para compatibilizar las interfaces

    try
		{
			Object obj = buzon.poll(tiempoEnMilisegundos, TimeUnit.MILLISECONDS);
                        return obj;

		}
		catch (InterruptedException e)
		{
			throw new ExcepcionSuperadoTiempoLimite("Percepcion: Interrumpida la espera de nuevo item en el buzon de items");
		}
}
        @Override
        public synchronized void produce(Object evento)
	{
		buzon.addLast(evento);
                 log.debug("encolando el evento : " +evento.toString());
	}


	/**
	 *  Aade un nuevo evento de forma prioritaria en la percepcin
	 *
	 *@param  evento  Evento que se consumir el primero
	 */
        @Override
	public void produceParaConsumirInmediatamente(Object evento)
	{
		buzon.addFirst(evento);
               
	}
        
   
}
