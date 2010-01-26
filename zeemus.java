

package JD;
import robocode.*;
import java.awt.*;
import java.io.*;
import java.util.*;


public class Zeemus extends AdvancedRobot
{
	zRobot target;
	zRobotList bots = new zRobotList();
		
	public void run() {
		
//		trackName = null;
		
		while(true) {
			
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			//spin in a circle and scan
		//	setBack(40000);
		//	setTurnRight(90);
			refresh();
			
			target = bots.getTarget();
			aimFire(target);
			
		/*	getBattleFieldWidth();
			getBattleFieldHeight();
			getX();
			getY();
			*/
		}
		
	}
	
	public void aimFire(zRobot target)
	{
		double deg = target.zbearing;
		turnGunRight(deg);
		fire(1);
	}
	
	
	//When scanning a robot, just tell the zRobotList to deal with it
	public void onScannedRobot(ScannedRobotEvent e) {
		bots.zOnScan(e);
	}
	
	public void onHitByBullet(HitByBulletEvent e) {
	}
	
	public void refresh(){
		turnRadarRight(360);
	}
	
	public void reAcquire(){
		turnRadarRight(360);
	}
	
	
}



//Manages a list of robots
class zRobotList{
	public static ArrayList<zRobot> list;
	
	public zRobotList(){
		list = new ArrayList<zRobot>();
	}
	
	
	//update positions of Robots based on scan event
	public static void zOnScan(ScannedRobotEvent e)
	{
		zRobot a = new zRobot(e);
		zRobot.zheading = 0;
	}
	
	
	//return the closest target
	public static zRobot getTarget()
	{
		zRobot min = list.get(0);
		for(zRobot r : list)
		{
			if(r.zdistance < min.zdistance)
				min = r;
		}
		return min;
	}
}



//contains information on each robot
class zRobot{
	public static double zheading;
	public static double zbearing;
	public static double zenergy;
	public static double zdistance;
	public static double zvelocity;
	public static String zname;
		
	public zRobot(ScannedRobotEvent e){
		zheading = e.getHeading();
		zbearing = e.getBearing();
		zenergy = e.getEnergy();
		zdistance = e.getDistance();
		zvelocity = e.getVelocity();
		zname = e.getName();
	}
}




/*	public static void directBullet(Bullet b){
		double heading = 90;
		double x = 200;
		double y = 200;
		double power = 3;
		String ownerName = b.getName();
		String victimName = b.getVictim();
		boolean isActive = true;
        int bulletId = b.bulletId;
		
		
		
		return new Bullet(heading, x, y, power, ownerName, victimName, isActive, bulletId);
	}*/


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



