/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package icaro.aplicaciones.recursos.comunicacionChat.imp.util;

/**
 *
 * @author FGarijo
 */
public class NickAlreadyInUseException extends Exception {
     
    /**
     * Constructs a new IrcException.
     *
     * @param e The error message to report.
     */
    public NickAlreadyInUseException(String e) {
        super(e);
    }
}
