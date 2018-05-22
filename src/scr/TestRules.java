package scr;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;

public class TestRules {
    public static void main(String[] args) throws Exception {
        // Load from 'FCL' file
        String fileName = "torcs_rules.fcl";
        FIS fis = FIS.load(fileName);

        // Error while loading?
        if( fis == null ) {
            System.err.println("Can't load file: '" + fileName + "'");
            return;
        }

        FunctionBlock rules = fis.getFunctionBlock("torcs_rules");

        // Set inputs
        rules.setVariable("curva", 0);
        rules.setVariable("voralDret", 0);
        rules.setVariable("voralEsquerra", 0);
        rules.setVariable("velocitat",  0);
        rules.setVariable("rpm", 0);

        // Evaluate
        rules.evaluate();

        // Show output variable's chart
        int acceleracio = (int) rules.getVariable("acceleracio").getValue();
        int girDret = (int)rules.getVariable("girDret").getValue();
        int girEsquerre = (int)rules.getVariable("girEsquerre").getValue();
        int frenar = (int) rules.getVariable("frenar").getValue();
        int shiftUp = (int) rules.getVariable("shiftUp").getValue();

        // Print ruleSet
        System.out.println(acceleracio);
        System.out.println(girDret);
        System.out.println(girEsquerre);
        System.out.println(frenar);
        System.out.println(shiftUp);
    }
}