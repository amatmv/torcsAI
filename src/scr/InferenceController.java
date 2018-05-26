package scr;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InferenceController extends Controller {

    private FIS _fis;

    private Map<Integer, Double> _track_info = new HashMap<Integer, Double>();

    protected InferenceController(){
        read_file("info_mapa_norm.txt");
        String fileName = "torcs_rules.fcl";

        _fis = FIS.load(fileName);
        if( _fis == null ) {
            System.err.println("Can't load file: '" + fileName + "'");
        }
    }

    private void read_file(String file_name){
        try(BufferedReader br = new BufferedReader(new FileReader(file_name))) {
            String line = br.readLine();

            while (line != null) {
                String [] p = line.split(" ");
                int metre = Integer.parseInt(p[0]);
                double angle = Double.parseDouble(p[1]);
                _track_info.put(metre, angle);
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Action control(SensorModel sensorModel) {

        try {
            assert _fis != null;
        }
        catch (AssertionError e) {
            System.err.println(e);
            return null;
        }

        /*----------- Bloc de regles d'acceleració -------------*/

        FunctionBlock accRules = _fis.getFunctionBlock("acceleracio");
        int metreActual = (int) sensorModel.getDistanceFromStartLine();

        double angleCurva = getCurveAngle(metreActual);

        double velocitatActual = sensorModel.getSpeed();
        accRules.setVariable("curva", angleCurva);
        accRules.setVariable("velocitat", velocitatActual);

        accRules.evaluate();

        double acceleracio = accRules.getVariable("acceleracio").getValue();

        /* -----------------------------------------------------*/

        /*---------- Bloc de regles de gir ---------------------*/

        FunctionBlock steerRules = _fis.getFunctionBlock("gir");
        double vorals = sensorModel.getTrackPosition();
        steerRules.setVariable("acceleracio", acceleracio);
        steerRules.setVariable("angleCentre", sensorModel.getAngleToTrackAxis());
        steerRules.setVariable("distVorals", vorals);
        steerRules.setVariable("fre", accRules.getVariable("fre").getValue());
        steerRules.setVariable("curva", angleCurva);


        steerRules.evaluate();

        double acceleracioGir = steerRules.getVariable("acceleracioOut").getValue();
        if (acceleracioGir > 0)
            acceleracio = acceleracioGir;

        /* -----------------------------------------------------*/


        /* ---------------- Bloc de regles del canvi de marxa ------------ */

        FunctionBlock gearboxRules = _fis.getFunctionBlock("outgear");

        double rpms = sensorModel.getRPM();
        gearboxRules.setVariable("rpm", rpms);
        gearboxRules.setVariable("acceleracio", acceleracio);

        gearboxRules.evaluate();

        /* -----------------------------------------------------*/

        /* --------------------- Codificació de l'acció -------------------*/

        Action action = new Action ();

        double fre = accRules.getVariable("fre").getValue();
        double gir = steerRules.getVariable("gir").getValue();
        double outgear = gearboxRules.getVariable("outgear").getValue();

        action.steering = gir;
        action.accelerate = acceleracio;
        action.brake = fre;

        int currentGear = sensorModel.getGear();
        if (outgear == -1 && currentGear > 0)
            action.gear = currentGear + (int) outgear;
        else if (outgear == 1 && currentGear <= 6)
            action.gear = currentGear + (int) outgear;
        else
            action.gear = currentGear;

        /* -----------------------------------------------------*/

        if (metreActual % 15 == 0 && metreActual != 0){
            System.out.println("------------------------ INPUT ---------------------------");
            System.out.println("METRE: "+ metreActual);
            System.out.println("curva"+ angleCurva);
            System.out.println("vorals: "+vorals);
            System.out.println("velocitat"+sensorModel.getSpeed());
            System.out.println("angle"+sensorModel.getAngleToTrackAxis());
            System.out.println("-----------------------------------------------------------------");
            System.out.println("");
            System.out.println("");
            System.out.println("-------------------------- OUTPUT ---------------------------");
            System.out.println("Acceleracio "+acceleracio);
            System.out.println("gir "+gir);
            System.out.println("Outgear "+outgear);
            System.out.println("fre "+fre);
            System.out.println("-----------------------------------------------------------------");
        }

        estabilitzar(sensorModel, action, gir, acceleracio, fre, angleCurva);

        hack(action, metreActual);


        return action;
    }

    private double getCurveAngle(int metre_actual) {
        double angleAcumulat = 0.0;
        int i = 0;
        while (i < 60){
            angleAcumulat += _track_info.get((metre_actual + i++) % _track_info.size());
        }
        return angleAcumulat / 12.892601299999997;
    }

    /************************** Funcions correctives ********************************/
    private static boolean foraDePista(double trackPos){
        return trackPos > 1.0 || trackPos < - 1.0;
    }

    private double calcularDireccio(double[] trackEdges){
        double forwardDist = 0;
        int direction = 0;
        for (int i = 7; i < 12; ++i) {
            if (trackEdges[i] > forwardDist) {
                forwardDist = trackEdges[i];
                direction = 9 - i;
            }
        }
        return direction;
    }

    private double calcularAngleSegonsDireccio(double direccio, double trackPos){
        if (direccio == 0) {
            return 0.1 * (trackPos - 0.5);
        }
        if (direccio == 1) {
            return 1.1 * (trackPos - 0.7);
        }
        if (direccio == -1) {
            return 1.1 * (trackPos - 0.3);
        }
        if (direccio == 2) {
            return 1.1 * (trackPos - 0.8);
        }
        if (direccio == -2) {
            return 1.1 * (trackPos - 0.2);
        }
        return 0;
    }

    private void estabilitzar(SensorModel sensorModel, Action action, double gir, double acceleracio, double fre, double angleCurva){
        double trackPos = sensorModel.getTrackPosition(); // indica la posicio respecte els vorals
        double trackAxis = sensorModel.getAngleToTrackAxis();
        double velocity = sensorModel.getSpeed();
        double[] trackEdges = sensorModel.getTrackEdgeSensors();
        double direccio = calcularDireccio(trackEdges);

        if (!foraDePista(trackPos)){
            if (Math.abs(angleCurva) >0.7) {
                if (gir > 0.4) {
                    action.steering = action.steering - 0.07;
                } else if (gir < -0.4) {
                    action.steering = action.steering + 0.07;
                }
            }

            if(Math.abs(trackPos) > 0.80){
                if (trackPos < 0){ //dreta
                    if (gir < 0){
                        action.steering = action.steering + -(action.steering/4);
                    }
                }else {
                    if (gir > 0){
                        action.steering = action.steering -(action.steering/4);
                    }
                }
            }
        }else{
            System.out.println("jesus");
            System.out.println(trackPos);
            if (acceleracio > 0.4){
                action.accelerate = 0.35;
            }
            if (velocity < 50){
                action.gear = 1;
            }
            if (velocity < 0.0 && velocity >= -10.0){
                action.gear = -1;
            }
            if (trackPos < 0){ //esq
                if (gir < 0){
                    action.steering = -1 * action.steering;//- * - = +
                }
            }else{
                if (gir > 0){
                    action.steering = -1 * action.steering;
                }
            }

        }



    }

    private void hack(Action action, int metre){

        //if (metre > 2325 & metre < 2450){
        if (metre > 2325 & metre < 2375){
            System.out.println("Hack life");
            action.accelerate = 0.2;
            action.brake = 0.0;
        }
        if (metre > 3830 & metre < 3870){
            System.out.println("Hack life2");
            action.accelerate = 0.05;
            if (Math.abs(action.steering)> 0.4){
                if (action.steering > 0){
                    action.steering= - 0.35;
                }else{
                    action.steering= + 0.2;
                }

            }
        }
    }
    /******************************************************************/


    public void reset() {
        System.out.println("Restarting the race!");
    }

    public void shutdown() {
        System.out.println("Bye bye!");
    }
}
