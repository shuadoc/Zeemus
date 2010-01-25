

package JD;
import robocode.*;
import java.awt.*;
import java.io.*;
import java.util.*;


public class Zeemus extends AdvancedRobot
{


	public void run() {
		
		
	
		
		while(true) {
			
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			turnRadarRight(360);
			for(int i=0; i<12; i++){
				back(10);
				turnLeft(15);
			}
				
			
			
		/*	getBattleFieldWidth();
			getBattleFieldHeight();
			getX();
			getY();
			
			
			
			
			*/
		
		
		}
	}


	public void onScannedRobot(ScannedRobotEvent e) {
		fire(1);
	}



	public void onHitByBullet(HitByBulletEvent e) {
		turnLeft(90 - e.getBearing());
	}
	
	public void onDeath(DeathEvent e){
		
	}
	
}






/*				//Reading files		
		int roundCount;
		try {
			Scanner scan = new Scanner(new FileReader(getDataFile("count.dat")));

			// Try to get the count
			roundCount = scan.nextInt();
		} catch (IOException e) {
			// Something went wrong reading the file, reset to 0.
			roundCount = 0;
		} catch (NumberFormatException e) {
			// Something went wrong converting to ints, reset to 0
			roundCount = 0;
		}
		// Increment the # of rounds
		roundCount++;

		PrintStream w = null;
		try {
			w = new PrintStream(new RobocodeFileOutputStream(getDataFile("count.dat")));

			w.println(roundCount);
			// PrintStreams don't throw IOExceptions during prints,
			// they simply set a flag.... so check it here.
			if (w.checkError()) {
				out.println("I could not write the count!");
			}
		} catch (IOException e) {
			out.println("IOException trying to write: ");
			e.printStackTrace(out);
		} finally {
			if (w != null) {
				w.close();
			}
		}

		out.println("I have been a sitting duck for " + roundCount + " rounds"); 
		
*/	



