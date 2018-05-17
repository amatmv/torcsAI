package scr;
import java.util.*;

import static java.lang.Math.abs;


/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: Mar 4, 2008
 * Time: 4:59:21 PM

 */
public class DeadSimpleSoloController extends Controller {

    private final double _targetSpeed = 40;
    private ArrayList<Double> _trackAngles = new ArrayList<>();
    private ArrayList<Double> _auxTrackAngles = new ArrayList<>();
    private ArrayList<Double> _trackAnglesSummarized = new ArrayList<>();
    private double _laps = 0;

    public Action control(SensorModel sensorModel) {
        Action action = new Action ();

        if (sensorModel.getSpeed () < _targetSpeed) {
            action.accelerate = 1;
        }

        if (sensorModel.getCurrentLapTime() > -0.02 && sensorModel.getCurrentLapTime() < 0.02) {
            _laps++;
        }

        if (_laps <= 1) {
            getTrackInfo(sensorModel);
        }

        if (sensorModel.getAngleToTrackAxis() < -0.15) { // Negative angles are left angles
            // Girar a la dreta
            action.steering = -1;
        } else if (sensorModel.getTrackPosition() > 1/50.0) { // Negative distances means "nearer to left edge"
            action.steering = -0.25;
        } else if (sensorModel.getAngleToTrackAxis() > 0.15) { // Positive angles means that we are deviating to the right
            // Gira a l'esquerra
            action.steering = 1;
        } else if (sensorModel.getTrackPosition() < -1/50.0) { // Positive distances means "nearer to right edge"
            action.steering = 0.25;
        }

        action.gear = 1;
        return action;
    }

    private void getTrackInfo(SensorModel sensorModel) {

        final int meanFactor = 30;

        double currentAngle = sensorModel.getAngleToTrackAxis();
        _auxTrackAngles.add(currentAngle);

        int currentTrackSize = _auxTrackAngles.size();

        if (currentTrackSize >= meanFactor){

            double meanAngle = 0;

            for (int i = currentTrackSize-1; i >= 0; i--){
                meanAngle += _auxTrackAngles.remove(i);
            }

            _trackAngles.add(meanAngle / meanFactor);

        }

    }

    public void reset() {
        System.out.println("Restarting the race!");	
    }

    public void shutdown() {
        int size = _trackAngles.size();

        int j = -1;
        for (int i = 0; i < size; i++) {
            double value = _trackAngles.get(i);
            if (i % 2 == 0)
                _trackAnglesSummarized.add(++j, value);
            else
                _trackAnglesSummarized.set(j, (_trackAnglesSummarized.get(j) + value) * 100);
        }
        System.out.println("tamany vector angles" + size);
        System.out.println("Valors del vector final" + _trackAnglesSummarized.size());
        for (double value: _trackAnglesSummarized) {
            System.out.println(value);
        }
        System.out.println("Bye bye!");		
    }
}
