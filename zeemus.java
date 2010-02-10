

package JD;
import robocode.*;
import java.awt.*;
import java.io.*;
import java.util.*;


public class Zeemus extends AdvancedRobot
{
	
	TargetList targets = new TargetList();
	
	//for setting up a map of dangerous locations
	boolean initialized = false;
	int[][] hotspots;
	//the amount of pixels per index in the hotspot map
	int GRANULARITY = 10;
	
	//define the radius for wall avoidance
	public double radius = 20;
	
		
	public void run() 
	{
		if(!initialized)
			initialize();
		System.out.println(hotspots.length +":"+hotspots[0].length);
		while(true)
		{
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			setMovement();
			setRadar();		
			wallAvoidance();
	
		}
	}
	
	public void initialize()
	{
		//define each location in the hotspot map to contain multiple pixels
		hotspots = new int[(int)getBattleFieldHeight()/GRANULARITY][(int)getBattleFieldWidth()/GRANULARITY];
		
		//set up so that the edges of the map are 'dangerous'
		for(int r=0; r< hotspots.length; r++){
			for(int c=0; c< hotspots[r].length; c++){
				hotspots[0][c] = 100;
				hotspots[r][0] = 100;
				hotspots[hotspots.length-1][c] = 100;
				hotspots[r][hotspots[r].length-1] = 100;
			}
		}
		
		initialized = true;
	}
	
	public void setMovement()
	{
		ahead(100);
	//	moveRandomly();	
	}
	
	public void setRadar()
	{
		setTurnRadarRight(360);
	}
	
	
	//BROKEN - causes 10000 calls to setXX method???
	//move foreward, turn in random directions, and spin the radar constantly
	public void moveRandomly()
	{
		Random rand = new Random();
		setAhead(40000);
		if(rand.nextBoolean())
		   setTurnRight(rand.nextInt(90));
		else setTurnLeft(rand.nextInt(90));
	}
	
	public void wallAvoidance()
	{
		//if within one radii of the wall, turn around
		if(getX() < radius || getY() < radius || getX() > getBattleFieldWidth()-radius || getY() > getBattleFieldHeight()-radius)
   		{
    		stop();
    		turnRight(180);
    		ahead(100);
    		resume();
    	}
	}
	
	public void aimFire(Location loc)
	{
		stop();
		Location here = new Location(getX(), getY());
		turnGunCorrectly(here.degreeTo(loc));
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
	    	dir *= (-1);
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
  
	//When scanning a robot, just tell the BotLog to deal with it
	public void onScannedRobot(ScannedRobotEvent e) 
	{
		targets.logRobot(e, getX(), getY(), getHeading());
		aimFire(targets.getTarget().getLocation());
	}
     
	
}

class TargetList
{
	public static ArrayList<BotLog> log;
		
	public TargetList(){
		log = new ArrayList<BotLog>();
	}	
	
	public static boolean isEmpty()
	{
		return log.size()<1;
	}
	
	public static void logRobot(ScannedRobotEvent e, double x, double y, double h)
	{
		boolean newbot = true;
		for(int i=0; i<log.size(); i++)
		{
			//if this robot is already in the list then update him
			if(log.get(i).getName().equals(e.getName()))
			{
				newbot = false;
				log.get(i).logRobot(e,x,y,h);
			}
		}
		
		if(newbot){
			log.add(new BotLog(e,x,y,h));
		}
	}
	
	
	//take robot's heading, velocity, and distace to robot
	//estimate where they will be when the bullet arrives
	public static Location getFiringPosition()
	{
		return new Location(0,0);
	}
		
		
			//return the closest target
	public static Bot getTarget()
	{
		if(isEmpty())
			return null;
		else return log.get(0).getLast();
	}
}



//Manages a list of events for each robot
class BotLog
{
	public static ArrayList<Bot> list;
	private static String name;
	
	public BotLog()
	{
		list = new ArrayList<Bot>();
		name = null;
	}
	
	public BotLog(ScannedRobotEvent e, double x, double y, double h)
	{
		list = new ArrayList<Bot>();
		name = e.getName();
		logRobot(e,x,y,h);
	}
	
	public static void logRobot(ScannedRobotEvent e, double x, double y, double h)
	{
		list.add(new Bot(e,x,y,h));		
	}
	
	public static Bot getLast(){
		return list.get(0);
	}
	
	public static String getName(){
		return name;
	}
	
	public static boolean isEmpty()
	{
		return list.size()<1;
	}
}


//container for storing information on scanned robots
class Bot
{
	private static double heading;
	private static double bearing;
	private static double energy;
	private static double distance;
	private static double velocity;
	private static long time;
	private static Location loc;
		
	public Bot(ScannedRobotEvent e , double x, double y, double heading)
	{
		heading = e.getHeading();
		//don't store bearing once fire on location is fixed
		bearing = e.getBearing();
		energy = e.getEnergy();
		distance = e.getDistance();
		velocity = e.getVelocity();
		loc = new Location(bearing, distance, x, y, heading);
				
	}
	
	public static double getHeading(){
		return heading;
	}
	public static double getBearing(){
		return bearing;
	}
	public static double getEnergy(){
		return energy;
	}
	public static double getDistance(){
		return distance;
	}
	public static double getVelocity(){
		return velocity;
	}
	public static Location getLocation(){
		return loc;
	}
	
}

//has a constructor which determines position from an onScannedEvent
class Location
{
	public static double x;
	public static double y;
	
	Location(double newX, double newY)
	{
		x = newX;
		y = newY;
	}
	
	//calculate from heading, bearing, distance, and current location
	Location(double bearing, double distance, double newX, double newY, double heading)
	{
		
		//calculate their heading
		double direction = heading+bearing;
		
		//add the x and y components of their distance to your x and y
		x = newX + distance * Math.sin(Math.toRadians(direction));
		y = newY + distance * Math.cos(Math.toRadians(direction));
	}
	
	//returns what degree points to the location specified from this location
	public static double degreeTo(Location loc)
	{
		double angle = 180 + Math.toDegrees(Math.atan((x - loc.getX()) / (y - loc.getY())));
		if(y < loc.getY())
			angle = (angle + 180)%360;
		return angle;
	}
	
	public static double getX(){
		return x;
	}
	public static double getY(){
		return y;
	}
	
}

	
                     /********* Code snippets

    	              //aim fire for locations, broken, always fires at 0
	public void aimFire()
	{
		stop();
		location loc = new Location(0,0);
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
	
	
	
	//////////////////////////////
	
			

  
     public void wallAvoidance()
  {
  	//store variables temporarily to decrease method calls
  	double tempHeading = getHeading();
  	double tempX = getX();
  	double tempY = getY();
  	
    //if approaching a corner, turn around
    
    //top left
    if(tempX < 2*radius && tempY < 2*radius)
    	turnLeft(180);
    //bottom right
    if(tempX < 2*radius && tempY > getBattleFieldWidth() - radius)
    	turnLeft(180);;
    //top left
    if(tempX > getBattleFieldHeight() - radius && tempY < 2*radius)
    	turnLeft(180);
    //top right
    if(tempX > getBattleFieldHeight() - radius && tempY > getBattleFieldWidth() - radius)
    	turnLeft(180);

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
  	
			///////////////////////////////////////
				//get the bearing of the robot, turn, and fire
	public void aimFire(Bot r)
	{
		stop();
		double neededDir = (getHeading() + r.getBearing());
		turnGunCorrectly(neededDir); 
		fire(2);
		resume();	                                
	} 
	////////////////////////////////////////
			
			
			******/