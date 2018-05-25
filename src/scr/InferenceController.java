package scr;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;

public class InferenceController extends Controller {

    private double _angle_curva = 0.0;
    private int _metre_actual = 0;
    private FIS _fis;

    private Map<Integer, Double> _track_info = new HashMap<Integer, Double>();

    public InferenceController(){
        read_file("info_mapa.txt");
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

//        for (double sensor : sensorModel.getTrackEdgeSensors()) {
//            System.out.print(sensor+", ");
//        }
//        System.out.println("");

        /*----------- Bloc de regles d'acceleració -------------*/

        FunctionBlock accRules = _fis.getFunctionBlock("acceleracio");
        _metre_actual = (int) sensorModel.getDistanceFromStartLine();

        _angle_curva = getCurveAngle(_metre_actual);

        double velocitatActual = sensorModel.getSpeed();
        accRules.setVariable("curva", _angle_curva);
        accRules.setVariable("velocitat", velocitatActual);

        accRules.evaluate();

        /* -----------------------------------------------------*/

        /*---------- Bloc de regles de gir ---------------------*/
        FunctionBlock steerRules = _fis.getFunctionBlock("gir");

        double distanciaVorals = sensorModel.getTrackPosition();
        double anglePista = sensorModel.getAngleToTrackAxis();
        double distEsquerra, distDreta;
        double angleEsquerre, angleDret;

        if (distanciaVorals < 0) {
            distDreta = abs(distanciaVorals);
            distEsquerra = 0;
        } else {
            distEsquerra = abs(distanciaVorals);
            distDreta = 0;
        }

        if (anglePista < 0){
            angleEsquerre = abs(anglePista);
            angleDret = 0;
        } else {
            angleEsquerre = 0;
            angleDret = abs(anglePista);
        }

        double acceleracio = accRules.getVariable("acceleracio").getValue();
        double fre = accRules.getVariable("fre").getValue();

        steerRules.setVariable("acceleracio", acceleracio);
        steerRules.setVariable("angleDret", angleDret);
        steerRules.setVariable("angleEsquerre", angleEsquerre);
        steerRules.setVariable("distVoralDret", distDreta);
        steerRules.setVariable("distVoralEsquerre", distEsquerra);
        steerRules.setVariable("fre", fre);
        steerRules.setVariable("curva", _angle_curva);

        steerRules.evaluate();

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

        acceleracio = accRules.getVariable("acceleracio").getValue();
        double accOut = steerRules.getVariable("acceleracioOut").getValue();
        if (accOut > 0)
            acceleracio = accOut;
        double gir = steerRules.getVariable("gir").getValue();
        double outgear = gearboxRules.getVariable("outgear").getValue();
        fre = accRules.getVariable("fre").getValue();

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

        if (_metre_actual % 20 == 0 && _metre_actual != 0){
            System.out.println("------------------------ INPUT ---------------------------");
            System.out.println("METRE: "+ _metre_actual);
            System.out.println("curva"+ _angle_curva);
            System.out.println("voralDretq: "+distDreta);
            System.out.println("voralEsquerra"+distEsquerra);
            System.out.println("velocitat"+sensorModel.getSpeed());
            System.out.println("rpm"+rpms);
            System.out.println("gear"+sensorModel.getGear());
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

        return action;
    }

    private double getCurveAngle(int metre_actual) {
        double angleAcumulat = 0.0;
        for (int i = metre_actual % _track_info.size(); i < (metre_actual + 40) % _track_info.size(); i++) {
            angleAcumulat += _track_info.get(i);
        }
        return angleAcumulat;
    }


    public void reset() {
        System.out.println("Restarting the race!");
    }

    public void shutdown() {
        System.out.println("Bye bye!");
    }
}
