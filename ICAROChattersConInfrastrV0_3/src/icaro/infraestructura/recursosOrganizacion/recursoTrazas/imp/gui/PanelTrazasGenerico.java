/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * PanelTrazasEspecificas1.java
 *
 * Created on 01-dic-2010, 14:01:49
 */

package icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.gui;

import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;
import java.awt.Color;
import java.awt.Font;

/**
 *
 * @author FGarijo
 */
public class PanelTrazasGenerico extends PanelTrazasAbstracto  {

    private String nombreComponente; //identificacin de la ventana

    /** Creates new form PanelTrazasEspecificas1 */
    public PanelTrazasGenerico(String nombre, String contenido) {
        initComponents();
        this.nombreComponente = nombre;
        this.setTitle(nombre);
 //       this.labelTitulo.setText(nombreComponente);
        this.areaTrazas.setText(contenido);
        this.setResizable(true);
    }

    @Override
    public void cierraVentana(){
   	this.setVisible(false);
    }

    public String getIdentificador(){
    	return nombreComponente;
    }

    @Override
    public synchronized void muestraInfoTraza(InfoTraza traza){

    	String nivel = "";
    	Color c = new Color(0);
    	if (traza.getNivel() == InfoTraza.NivelTraza.debug){
    		nivel = "DEBUG";
    		c = Color.BLUE;
    	}
    	else if (traza.getNivel() == InfoTraza.NivelTraza.info){
    		nivel = "INFO";
    		c = Color.GREEN;
    	}
    	else if (traza.getNivel() == InfoTraza.NivelTraza.error){
    		nivel = "ERROR";
    		c = Color.ORANGE;
    	}
    	else { //fatal
    		nivel = "FATAL";
    		c = Color.RED;
    	}
    	Font f = new Font("Trebuchet",Font.PLAIN,12);
    	areaTrazas.setFont(f);
    	areaTrazas.setForeground(Color.BLUE);
    	//Concateno el nuevo mensaje con el que habia antes

    	areaTrazas.append(nivel+" : "+traza.getMensaje()+"\n");
    	//si escribo null,borra lo anterior
    }
    
    
   

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        areaTrazas = new java.awt.TextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(areaTrazas, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 472, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(areaTrazas, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
    * @param args the command line arguments
    */
//    public static void main(String args[]) {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new PanelTrazasEspecificas1().setVisible(true);
//            }
//        });
//    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.TextArea areaTrazas;
    // End of variables declaration//GEN-END:variables

}
