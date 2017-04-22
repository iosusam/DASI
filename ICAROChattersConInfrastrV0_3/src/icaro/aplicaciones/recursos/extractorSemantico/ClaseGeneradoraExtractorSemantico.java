package icaro.aplicaciones.recursos.extractorSemantico;

import gate.Gate;
import icaro.aplicaciones.recursos.extractorSemantico.imp.ExtractorSemanticoImp;
import icaro.infraestructura.patronRecursoSimple.imp.ImplRecursoSimple;
import icaro.infraestructura.recursosOrganizacion.recursoTrazas.imp.componentes.InfoTraza;
import java.io.File;
import java.util.HashSet;

public class ClaseGeneradoraExtractorSemantico extends ImplRecursoSimple implements
		ItfUsoExtractorSemantico {

	private static final long serialVersionUID = 1L;
    private static final String DirectorioGateHome= "C:\\GATE_Developer_8.4";
//        private static final String DirectorioGateHome= "C:\\GATE_Developer_8.0";
//    public static final String DirectorioAppFile= "E:\\FicheroRed\\GatePruebas\\anniePruebaEjemplo1";
    private static final String DirectorioAppFile= "E:\\FicheroRed\\ProyectoChatters\\Prueba1.gapp";
        private ExtractorSemanticoImp extractorSem;

	public ClaseGeneradoraExtractorSemantico(String idInstanciaRecurso) throws Exception {
		
		super(idInstanciaRecurso);
// obtenemos la ruta del procesador que debe estar definida en las propiedades del recurso
//                ItfUsoConfiguracion config = (ItfUsoConfiguracion) repoIntfaces.obtenerInterfaz(NombresPredefinidos.ITF_USO+NombresPredefinidos.CONFIGURACION);
//			DescInstanciaRecursoAplicacion descRecurso = config.getDescInstanciaRecursoAplicacion(idInstanciaRecurso);
   
      File file1 = new File(this.DirectorioGateHome);          
      File gappFile = new File(this.DirectorioAppFile);
      Gate.setGateHome(file1);
     gate.Gate.init();
//    String[] paramsProcessor = new String[3];
//    paramsProcessor[0]="-g";
//    paramsProcessor[1]=ConfigRutasExtractorSemantico.DirectorioAppFile;
//    paramsProcessor[2]=ConfigRutasExtractorSemantico.DirectorioAppFile;
		try {
                        extractorSem = new ExtractorSemanticoImp(this.DirectorioAppFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.trazas.aceptaNuevaTraza(new InfoTraza(id,
  				"Se ha producido un error al crear el extractor semantico  "+e.getMessage()+
                                ": Verificar los parametros de creacion "
                                + "rutas y otros",
  				InfoTraza.NivelTraza.error));
			this.itfAutomata.transita("error");
			throw e;
		}
			
        extractorSem.incializar();
}
        @Override
	public HashSet extraerAnotaciones(HashSet anotacionesAencontrar,String textoUsuario)throws Exception{
            return extractorSem.extraerAnotaciones(anotacionesAencontrar,textoUsuario);
        }

	@Override
	public void termina() {

		try {
			super.termina();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}  
}