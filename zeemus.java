

package JD;
import robocode.*;
import java.awt.*;
import java.io.*;
import java.util.*;


public class Zeemus extends AdvancedRobot
{
	
	zRobotList robotList = new zRobotList();
		
	public void run() 
	{
		
		while(true)
		{
			
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			//spin in a circle and scan
			setBack(40000);
			setTurnRight(90);
			refresh();
			if(getGunHeat()<.1)
				aimFire(robotList.getTarget());
			
		}
		
	}
	

	//When scanning a robot, just tell the zRobotList to deal with it
	public void onScannedRobot(ScannedRobotEvent e) 
	{
		
		robotList.logRobot(e, getX(), getY(), getHeading());
	}
	
	public void onHitByBullet(HitByBulletEvent e) 
	{
	}
	
	public void refresh()
	{
		turnRadarRight(360);
	}
	
	public void aimFire(zRobot r)
	{
		stop();
		double neededDir = (getHeading() + r.zbearing)%360;
		double neededTurn = neededDir - getGunHeading();
		if(neededTurn > 180)
			neededTurn -= 360;
		turnGunRight(neededDir - getGunHeading()); 
		fire(1);
		resume();		
		
		
	}
	
	
	
/*	public void aimFire(location loc)
	{
		stop();
		double neededDir = Math.tan((loc.getY() - getY()) / (loc.getX() - getX()));
		turnGunRight(neededDir - getGunHeading()); 
		fire(1);
		resume();
	}
		*/
	
}



//Manages a list of robots

class zRobotList{
	public static ArrayList<zRobot> list;
	
	public zRobotList(){
		list = new ArrayList<zRobot>();
	}
	
	
	//update positions of Robots based on scan event
	public static void logRobot(ScannedRobotEvent e, double x, double y, double h)
	{
		//is this needed?
		if(list.size() < 1)
			list.add(new zRobot(e,x,y,h));
			
		else
		{
			boolean newbot = true;
			for(int i=0; i<list.size(); i++)
			{
				//if this robot is already in the list then update him
				if(list.get(i).zname.equals(e.getName()))
				{
					newbot = false;
					list.set(i, new zRobot(e,x,y,h));
				}
			}
			
			if(newbot)
				list.add(new zRobot(e,x,y,h));
		}
	}

	
	//return the closest target
	public static zRobot getTarget()
	{
		if(isEmpty())
			return null;
		
		//check for the smallest distance
		zRobot min = list.get(0);
		for(zRobot r : list)
		{
			if(r.zdistance < min.zdistance)
				min = r;
		}
		return min;
		
	}
	
	public static boolean isEmpty()
	{
		return list.size()<1;
	}
}



//contains information on each robot, z will denote logged robot variables
//variables are public static so they can be accessed without a 'get()'
class zRobot
{
	public static double zheading;
	public static double zbearing;
	public static double zenergy;
	public static double zdistance;
	public static double zvelocity;
	public static location zloc;
	public static String zname;
		
	public zRobot(ScannedRobotEvent e , double x, double y, double h)
	{
		zheading = e.getHeading();
		//should I store bearing?
		zbearing = e.getBearing();
		zenergy = e.getEnergy();
		zdistance = e.getDistance();
		zvelocity = e.getVelocity();
		zname = e.getName();
		zloc = new location(zbearing, zdistance, x, y, h);
				
	}
	
}

class location
{
	public static double x;
	public static double y;
	
	location(double a, double b)
	{
		x = a;
		y = b;
	}
	
	//calculate from heading, bearing, distance, and current location
	location(double zbearing, double zdistance, double a, double b, double heading)
	{
		
		//calculate their heading
		double zdirection = heading+zbearing;
		
		//add the x and y components of their distance to your x and y
		x = a + zdistance * Math.sin(Math.toRadians(zdirection));
		y = b + zdistance * Math.cos(Math.toRadians(zdirection));
	}
	
	public static double getX()
	{
		return x;
	}
	public static double getY()
	{
		return y;
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


			
		/*	getBattleFieldWidth();
			getBattleFieldHeight();
			getX();
			getY();
			*/