

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
	
	public int shotsFired;
	public int shotsHit;
	
		
	public void run() 
	{
		if(!initialized)
			initialize();
		System.out.println(hotspots.length +":"+hotspots[0].length);
		while(true)
		{
			
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			ahead(100);
			
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
		shotsFired = 0;
		shotsHit = 0;
		
		initialized = true;
	}
	
	public void setMovement()
	{
		navigate();
		ahead(100);
	//	moveRandomly();	
	}
	
	public void navigate()
	{
		
		
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
		if(1.0 * shotsHit / shotsFired > .2 || shotsFired < 15){
			stop();
			Location here = new Location(getX(), getY());
			double x,y;
			//make sure the target you are firing at is in bounds
			Location temp = new Location(Math.min(getBattleFieldWidth(),Math.max(0,loc.getX())),Math.min(getBattleFieldHeight(),Math.max(0,loc.getY())));
			turnGunCorrectly(here.degreeTo(temp));
			
			fire(1);
			shotsFired++;
			
			resume();
		}
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
		aimFire(targets.estimatePos(1, new Location(getX(),getY())));
	}
	
	public void onBulletHit(BulletHitEvent e)
	{
		shotsHit++;
	}
     
	
}

class TargetList
{
	public  ArrayList<BotLog> log;
		
	public TargetList(){
		log = new ArrayList<BotLog>();
	}	
	
	public  boolean isEmpty()
	{
		return log.size()<1;
	}
	
	public  void logRobot(ScannedRobotEvent e, double x, double y, double heading)
	{
		boolean newbot = true;
		for(int i=0; i<log.size(); i++)
		{
			//if this robot is already in the list then update him
			if(log.get(i).getName().equals(e.getName()))
			{
				newbot = false;
				log.get(i).logRobot(e,x,y,heading);
			}
		}
		
		if(newbot){
			log.add(new BotLog(e,x,y,heading));
		}
	}
	
	
	//take robot's heading, velocity, and distace to robot
	//estimate where they will be when the bullet arrives
	public  Location estimatePos(int power , Location zee)
	{
		Bot target = getTarget();
		return estimatePostitionHelper(zee, power, target.getVelocity(), target.getHeading(), 0, target.getLocation(), 5);	

	}
	
	public Location estimatePostitionHelper(Location zee, double power, double velocity, double heading, double priorDistance, Location loc, int recurseNum)
	{
		if(recurseNum <= 0)
			return loc;
		double distance = Math.sqrt(Math.pow(loc.getX()-zee.getX() , 2) + Math.pow(loc.getY()-zee.getY() , 2));
		distance -= priorDistance;
		double time = distance / (20 - power*3);
		
		double x = loc.getX() + velocity * time * (Math.sin(Math.toRadians(heading)));
		double y = loc.getY() + velocity * time * (Math.cos(Math.toRadians(heading)));
		
		Location update = new Location(x,y);
		
		return estimatePostitionHelper(zee, power, velocity, heading, distance+priorDistance, update, recurseNum-1);
		
	}
		
		
			//return the closest target
	public  Bot getTarget()
	{
		if(isEmpty())
			return null;
		return log.get(0).getLast();
	}
}



//Manages a list of events for each robot
class BotLog
{
	public  ArrayList<Bot> list;
	private  String name;
	
	public BotLog()
	{
		list = new ArrayList<Bot>();
		name = null;
	}
	
	public BotLog(ScannedRobotEvent e, double x, double y, double heading)
	{
		list = new ArrayList<Bot>();
		name = e.getName();
		logRobot(e,x,y,heading);
	}
	
	public void logRobot(ScannedRobotEvent e, double x, double y, double heading)
	{
		list.add(new Bot(e,x,y,heading));		
	}
	
	public Bot getLast(){
		return list.get(list.size()-1);
	}
	
	public String getName(){
		return name;
	}
	
	public boolean isEmpty()
	{
		return list.size()<1;
	}
}


//container for storing information on scanned robots
class Bot
{
	private double heading;
	private double bearing;
	private double energy;
	private double distance;
	private double velocity;
	private long time;
	private Location loc;
		
	public Bot(ScannedRobotEvent e , double x, double y, double h)
	{
		heading = e.getHeading();
		//don't store bearing once fire on location is fixed
		bearing = e.getBearing();
		energy = e.getEnergy();
		distance = e.getDistance();
		velocity = e.getVelocity();
		loc = new Location(bearing, distance, x, y, h);
				
	}
	
	public  double getHeading(){
		return heading;
	}
	public  double getBearing(){
		return bearing;
	}
	public  double getEnergy(){
		return energy;
	}
	public  double getDistance(){
		return distance;
	}
	public  double getVelocity(){
		return velocity;
	}
	public  Location getLocation(){
		return loc;
	}
	
}

//has a constructor which determines position from an onScannedEvent
class Location
{
	private  double x;
	private  double y;
	
	Location(double sentX, double sentY)
	{
		x = sentX;
		y = sentY;
	}
	
	//calculate from heading, bearing, distance, and current location
	Location(double bearing, double distance, double sentX, double sentY, double heading)
	{
		
		//calculate their heading
		double direction = heading+bearing;
		//remove negatives and greater than 360's
		direction = (direction + 360)%360;
		
		
		
		//add the x and y components of their distance to your x and y
		x = sentX + distance * Math.sin(Math.toRadians(direction));
		y = sentY + distance * Math.cos(Math.toRadians(direction));
		
	}
	
	//returns what degree points to the location specified from this location
	public  double degreeTo(Location loc)
	{
		// make sure that there is no divide by zero error
		if(y==loc.getY())
			y+=.0000001;
		double angle = 180 + Math.toDegrees(Math.atan((x - loc.getX()) / (y - loc.getY())));
		if(y < loc.getY())
			angle = (angle + 180)%360;
		return angle;
	}
	
	public  double getX(){
		return x;
	}
	public  double getY(){
		return y;
	}
	
}

	
                     /********* Code snippets


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



	public  void directBullet(Bullet b){
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
  	
	////////////////////////////////////////
			
			
			******/