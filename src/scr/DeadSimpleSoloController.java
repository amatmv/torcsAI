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

    private final double _targetSpeed = 50;
    private ArrayList<Double> _trackAngles = new ArrayList<Double>();
    private ArrayList<Double> _trackAnglesSummarized = new ArrayList<Double>();
    private int _trackDistance = 0;
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
            _trackDistance = (int) sensorModel.getDistanceFromStartLine();
            getTrackInfo(sensorModel);
        }

        if (sensorModel.getAngleToTrackAxis() < -0.02) { // Negative angles are left angles
            // Girar a la dreta al màxim
            action.steering = -0.5;
        } else if (sensorModel.getTrackPosition() > 1/15.0) { // Negative distances means "nearer to left edge"
            action.steering = -0.3;
        } else if (sensorModel.getAngleToTrackAxis() > 0.02) { // Positive angles means that we are deviating to the right
            // Gira el màxim a l'esquerra
            action.steering = 0.5;
        } else if (sensorModel.getTrackPosition() < -1/15.0) { // Positive distances means "nearer to right edge"
            action.steering = 0.3;
        }

        action.gear = 1;
        return action;
    }

    private void getTrackInfo(SensorModel sensorModel) {

        final int meanFactor = 100;

        double currentAngle = sensorModel.getAngleToTrackAxis();
        _trackAngles.add(currentAngle);

        int currentTrackSize = _trackAngles.size();

        if (currentTrackSize > 0 && currentTrackSize % meanFactor == 0){

            double meanAngle = 0;

            for (int i = currentTrackSize-1; i >= currentTrackSize-meanFactor; i--){
                meanAngle += _trackAngles.get(i);
            }
            _trackAngles.add(currentTrackSize-meanFactor+1, meanAngle /= meanFactor);

        }

    }

    public void reset() {
        System.out.println("Restarting the race!");	
    }

    public void shutdown() {
        System.out.println("Bye bye!");		
    }
}
