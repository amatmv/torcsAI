package scr;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: Mar 4, 2008
 * Time: 4:59:21 PM

 */
public class VoltaReconeixement extends Controller {

    private final double _targetSpeed = 40;
    private ArrayList<String> _trackInfo = new ArrayList<>();

    public Action control(SensorModel sensorModel) {
        Action action = new Action ();

        if (sensorModel.getSpeed () < _targetSpeed) {
            action.accelerate = 1;
        }

        double girActual = 0.0;
        if (sensorModel.getAngleToTrackAxis() < -0.05) { // Negative angles are left angles
            girActual = -0.5;
        } else if (sensorModel.getTrackPosition() > 1/4.0) { // Negative distances means "nearer to left edge"
            girActual = -0.25;
        } else if (sensorModel.getAngleToTrackAxis() > 0.05) { // Positive angles means that we are deviating to the right
            girActual = 0.5;
        } else if (sensorModel.getTrackPosition() < -1/4.0) { // Positive distances means "nearer to right edge"
            girActual = 0.25;
        }

        String s = "{'raced': " + sensorModel.getDistanceRaced() + ", 'startLine': " + sensorModel.getDistanceFromStartLine()  + ", 'gir':"  + girActual + "}";

        _trackInfo.add(s);

        action.steering = girActual;

        action.gear = 1;
        return action;
    }

    public void reset() {
        System.out.println("Restarting the race!");	
    }

    public void shutdown() {
        try {
            PrintWriter writer = new PrintWriter("out2.txt","UTF-8");
            for (String value: _trackInfo) {
                writer.println(value);
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
