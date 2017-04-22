package icaro.infraestructura.patronAgenteCognitivo.percepcion;

import icaro.infraestructura.entidadesBasicas.interfaces.ItfEventoSimpe;
import icaro.infraestructura.entidadesBasicas.interfaces.ItfMensajeSimple;
import icaro.infraestructura.patronAgenteCognitivo.factoriaEInterfacesPatCogn.AgenteCognitivo;
import icaro.infraestructura.patronAgenteCognitivo.percepcion.imp.ProcesadorItems;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TransferQueue;


/**
 * Abstract Class for Cognitive Agent Perception
 * @author Carlos Celorrio
 *
 */
public abstract class PercepcionAgenteCognitivo implements ItfEventoSimpe, ItfMensajeSimple, ItfGestPercepcionAgenteCognitivo {
public abstract void SetParametrosPercepcionAgenteCognitivoImp(TransferQueue<Object> colaEvtosyMsgs, ProcesadorItems prItems,AgenteCognitivo agente);
}
