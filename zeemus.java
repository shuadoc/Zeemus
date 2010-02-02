

package JD;
import robocode.*;
import java.awt.*;
import java.io.*;
import java.util.*;


public class Zeemus extends AdvancedRobot
{
	
	zRobotList robotList = new zRobotList();
	
	//define the radius for wall avoidance
	public double radius = 100;
		
	public void run() 
	{
		
		while(true)
		{
			
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			Random rand = new Random();
			//move foreward and turn in random directions
			setAhead(40000);
			if(rand.nextBoolean())
			   setTurnRight(rand.nextInt(90));
			else setTurnLeft(rand.nextInt(90));
			wallAvoidance();		
			refresh();			
		}
		
	}
	//get the bearing of the robot, turn, and fire
	public void aimFire(zRobot r)
	{
		stop();
		double neededDir = (getHeading() + r.zbearing);
		turnGunCorrectly(neededDir); 
		fire(1);
		resume();				                                        
	}  
	

	
	
	
	
	//optimize turning for right and left, but don't turn if < n degrees difference
	public void turnGunCorrectly(double dir)
  {
    double n = 0;
    
    //make sure the number is not greather than 360
    dir %= 360;
    
    //dir is now how far to turn right
    dir -= getGunHeading(); 
    
    //optimize turning right and left
    boolean right = true;
    if(dir < 0)
    {
    	dir *=-1;
    	right = !right;
    }
    else if(dir > 180){
    	dir -= 180;
    	right = !right;
    	
    }
    if(right)
      turnGunRight(dir);
     else turnGunLeft(dir);
    return;
      
  }
  
  //does not work for backwards motion
  //does not work for corners
  //would like to scale the avoidance by the incident angle
     public void wallAvoidance()
  {
    //if within one radii of the wall, begin turning around
    if(getX() < radius)
    {
      if(getHeading() >= 270)
      {
        turnRight(180);
      }
      else 
      {
      turnLeft(180);
      }
    }               
    if(getY() < radius)
    {
      if(getHeading() >= 180)
      {
        turnRight(180);
      }
      else 
      {
      turnLeft(180);
      }
    }
    if(getX() > getBattleFieldWidth()-radius)
    {
      if(getHeading() >= 90)
      {
        turnRight(180);
      }
      else 
      {
      turnLeft(180);
      }
    }
    if(getY() > getBattleFieldHeight()-radius)
    {
      if(getHeading() > 0 && getHeading() < 180)
      {
        turnRight(180);
      }
      else 
      {
      turnLeft(180);
      }
    }
    ahead(100);
  }	
	//When scanning a robot, just tell the zRobotList to deal with it
	public void onScannedRobot(ScannedRobotEvent e) 
	{
		robotList.logRobot(e, getX(), getY(), getHeading());
		aimFire(robotList.getTarget());
	}
	
	//spin the radar
	public void refresh()
	{
		turnRadarRight(360);
	}            

	
}



//Manages a list of robots

class zRobotList
{
	public static ArrayList<zRobot> list;
	
	public zRobotList()
  {
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
		
	public zRobot(ScannedRobotEvent e , double x, double y, double heading)
	{
		zheading = e.getHeading();
		//don't store bearing once fire on location is fixed
		zbearing = e.getBearing();
		zenergy = e.getEnergy();
		zdistance = e.getDistance();
		zvelocity = e.getVelocity();
		zname = e.getName();
		zloc = new location(zbearing, zdistance, x, y, heading);
				
	}
	
}

class location
{
	public static double x;
	public static double y;
	
	location(double newX, double newY)
	{
		x = newX;
		y = newY;
	}
	
	//calculate from heading, bearing, distance, and current location
	location(double zbearing, double zdistance, double newX, double newY, double heading)
	{
		
		//calculate their heading
		double zdirection = heading+zbearing;
		
		//add the x and y components of their distance to your x and y
		x = newX + zdistance * Math.sin(Math.toRadians(zdirection));
		y = newY + zdistance * Math.cos(Math.toRadians(zdirection));
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
	
                     /********* Code snippets

    	              //aim fire for locations, broken, always fires at 0
	public void aimFire(zRobot r)
	{
		stop();
		location loc = r.zloc;
		double neededDir = Math.tan((loc.getY() - getY()) / (loc.getX() - getX()));
		turnGunCorrectly(neededDir);
		fire(1);
		resume();
	}



                                            




				//Reading files		             /////////////////////////
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
		
	                                          /////////////////////////



	public static void directBullet(Bullet b){
		double heading = 90;
		double x = 200;
		double y = 200;
		double power = 3;
		String ownerName = b.getName();
		String victimName = b.getVictim();
		boolean isActive = true;
        int bulletId = b.bulletId;
		
		
		
		return new Bullet(heading, x, y, power, ownerName, victimName, isActive, bulletId);
	}
	
	
	
	
	
			
  public void wallAvoidance()
  {
    //if within one radii of the wall, begin turning around
    if(getX() < radius || getY() < radius || getX() > getBattleFieldWidth()-radius || getY() > getBattleFieldHeight()-radius)
    {
    stop();
      if(getHeading() <= 270)
      {
        turnRight(180);
      }
      else 
      {
      turnRight(180);
      }
      ahead(100);
      resume();
    }               
    
  }
			
			
			
			******/