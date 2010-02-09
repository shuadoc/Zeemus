

package JD;
import robocode.*;
import robocode.RobocodeFileOutputStream;
import java.awt.*;
import java.io.*;
import java.util.*;


public class Zeemus extends AdvancedRobot
{
	
	zRobotList robotList = new zRobotList();
	
	//define the radius for wall avoidance
	public double radius = 250;
	//define the max random wait between shot time
	public int waitVariable = 30;
	public int waitLeft;
	public boolean waiting;
	public boolean tookShot;
		
	public void run() 
	{
		
		while(true)
		{
			
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			Random rand = new Random();
			
			
			//move foreward, turn in random directions, and spin the radar constantly
			setTurnRadarRight(3600);
			setAhead(40000);
			if(rand.nextBoolean())
			   setTurnRight(rand.nextInt(90));
			else setTurnLeft(rand.nextInt(90));
			
			
			wallAvoidance();
			
			//shoot every once in a while
			if(tookShot)
				waitLeft = rand.nextInt(waitVariable);
			if(waitLeft <=0)
				waiting = false;
			waitLeft--;		
		}
		
	}
	//get the bearing of the robot, turn, and fire if you plan on shooting
	public void aimFire(zRobot r)
	{
		if(!waiting)
		{
			tookShot = true;
			waiting = true;
			stop();
			double neededDir = (getHeading() + r.zbearing);
			turnGunCorrectly(neededDir); 
			fire(2);
			resume();	
		}			                                        
	} 
	
	
	//take robot's heading, velocity, and distace to robot
	//estimate where they will be when the bullet arrives
	public void estimateFuturePosition(zRobot r)
	{
		
		
		
		
		
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
     public void wallAvoidance()
  {
  	//store variables temporarily to decrease method calls
  	double tempHeading = getHeading();
  	double tempX = getX();
  	double tempY = getY();
  	
    //if approaching a corner, turn around
    
    //top left
    if(tempX < 2*radius && tempY < 2*radius)
    	turnGunCorrectly(45);
    //bottom right
    if(tempX < 2*radius && tempY > getBattleFieldWidth() - radius)
    	turnGunCorrectly(135);
    //top left
    if(tempX > getBattleFieldHeight() - radius && tempY < 2*radius)
    	turnGunCorrectly(225);
    //top right
    if(tempX > getBattleFieldHeight() - radius && tempY > getBattleFieldWidth() - radius)
    	turnGunCorrectly(315);

    //if within one radii of the wall, begin turning around
    //turn around at least 90 degrees, and a full 180 if you are 
    //approaching straight on
    
    //else is present not because the criteria are exclusive, but only because
    //it would not be good to enact both turning behaviors   	
    else if(tempX < radius)
    {
      if(tempHeading >= 270)
        turnRight(Math.abs(Math.cos(tempHeading))*90+90);
      else 
      	turnLeft(Math.abs(Math.cos(tempHeading))*90+90);
    }           
    else if(tempY < radius)
    {
      if(tempHeading >= 180)
        turnRight(Math.abs(Math.sin(tempHeading))*90+90);
      else 
      	turnLeft(Math.abs(Math.sin(tempHeading))*90+90);
    }
    else if(tempX > getBattleFieldWidth()-radius)
    {
      if(tempHeading >= 90)
        turnRight(Math.abs(Math.cos(tempHeading))*90+90);
      else 
      	turnLeft(Math.abs(Math.cos(tempHeading))*90+90);
    }	
    else if(tempY > getBattleFieldHeight()-radius)
    {
      if(tempHeading > 0 && tempHeading < 180)
        turnRight(Math.abs(Math.sin(tempHeading))*90+90);
      else 
      	turnLeft(Math.abs(Math.sin(tempHeading))*90+90);
    }

    ahead(100);
  }	
	//When scanning a robot, just tell the zRobotList to deal with it
	public void onScannedRobot(ScannedRobotEvent e) 
	{
		robotList.logRobot(e, getX(), getY(), getHeading());
		aimFire(robotList.getTarget());
	}
     
	
}



//Manages a list of robots

class zRobotList
{
	public static ArrayList<Stack<zRobot>> list;
	
	public zRobotList()
	{
		list = new ArrayList<Stack<zRobot>>();
	}
	
	
	//update positions of Robots based on scan event
	public static void logRobot(ScannedRobotEvent e, double x, double y, double h)
	{
		boolean newbot = true;
		for(int i=0; i<list.size(); i++)
		{
			//if this robot is already in the list then update him
			if(list.get(i).peek().zname.equals(e.getName()))
			{
				newbot = false;
				list.get(i).push(new zRobot(e,x,y,h));
				
			}
		}
		
		if(newbot){
			Stack<zRobot> temp = new Stack<zRobot>();
			temp.push(new zRobot(e,x,y,h));
			list.add(temp);
		}
	}

	
	//return the closest target
	public static zRobot getTarget()
	{
		if(isEmpty())
			return null;
		
		//check for the smallest distance
		zRobot min = list.get(0).peek();
		for(Stack<zRobot> s : list)
		{
			if(s.peek().zdistance < min.zdistance)
				min = s.peek();
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

//has a constructor which determines position from an onScannedEvent
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


class debug
{
	public static PrintStream ps;
	
	debug()
	{
		ps = null;
	}
	
	public static void print(String input)
	{
		try {
			new PrintStream(new RobocodeFileOutputStream(getDataFile("debug.dat")));
			
			ps.println("");
			
			if (ps.checkError()) {
				System.out.println("I could not write the count!");
			}
		} catch (IOException e) {
			System.out.println("IOException trying to write: ");
			e.printStackTrace(System.out);
		}
		finally {
			if (ps != null) {
				ps.close();
			}
		}
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