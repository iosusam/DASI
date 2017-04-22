/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package icaro.infraestructura.patronAgenteCognitivo.percepcion.imp;

import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.Evidencia;
import icaro.infraestructura.entidadesBasicas.procesadorCognitivo.ExtractedInfo;
import icaro.infraestructura.patronAgenteCognitivo.factoriaEInterfacesPatCogn.AgenteCognitivo;
import icaro.infraestructura.patronAgenteCognitivo.percepcion.FactoriaPercepcionAgenteCognitivo;
import icaro.infraestructura.patronAgenteCognitivo.percepcion.PercepcionAgenteCognitivo;
import icaro.infraestructura.patronAgenteCognitivo.procesadorObjetivos.factoriaEInterfacesPrObj.ItfProcesadorObjetivos;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

/**
 *
 * @author Francisco J Garijo
 */
public class FactoriaPercepcionAgenteCognitivoImp2 extends FactoriaPercepcionAgenteCognitivo {
    private static final int CAPACIDAD_BUZON_MENSAJES = 15;
    private static final int CAPACIDAD_BUZON_CONT_MESSG = 15;
    private TransferQueue<Object> buzonMsges;
    private BlockingQueue<ExtractedInfo> extractedInfoQ;
//    private ItfProcesadorItems itfProcesador;  //no se usa
    private ProcesadorItems procesItems;
    private PercepcionAgenteCognitivo percepcionAgteCognitivo;
    @Override
	public PercepcionAgenteCognitivo crearPercepcion( AgenteCognitivo agenteCognitivo, ItfProcesadorObjetivos itfProcesadorExtractedInfo) {
// Primero se se crean los objetos que forman la percepcion y luego se ensamblan segun sus dependencias
// En el ensamblaje quedan claras las dependencias entre ellas

// Creacion de los elementos
        buzonMsges = new LinkedTransferQueue<>();
        extractedInfoQ= new LinkedBlockingQueue<>(CAPACIDAD_BUZON_CONT_MESSG);
        percepcionAgteCognitivo = new PercepcionAgenteCognitivoImp();
// El procesador de items produce elementos extraidos de los mensajes o de los eventos        
        procesItems = new ProcesadorItems();
        
//        itfProcesador = new ProcesadorItems();
        
// Ensamblaje de los elementos pasando a cada uno los elementos necesarios para su funcionamiento
        percepcionAgteCognitivo.SetParametrosPercepcionAgenteCognitivoImp(buzonMsges, procesItems, agenteCognitivo);
        procesItems.SetParametrosProcesadorItems(agenteCognitivo, extractedInfoQ,itfProcesadorExtractedInfo);
        return percepcionAgteCognitivo;
	}
  
}
