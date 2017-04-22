/*
 *  Copyright 2001 Telefnica I+D
 *
 *
 *  All rights reserved
 */
package icaro.infraestructura.patronAgenteReactivo.percepcion.factoriaEInterfaces;

/**
 * 
 *@author     
 *@created    
 */

public interface ItfProductorPercepcion {
	/**
	 *  Aade un nuevo evento en la percepcin
	 *
	 *@param  evento  Evento que vamos a aadir
	 */
	public void produce(Object evento);


	/**
	 *  Aade un nuevo evento de forma prioritaria en la percepcin
	 *
	 *@param  evento  Evento que se consumir el primero
	 */
	public void produceParaConsumirInmediatamente(Object evento);

}
