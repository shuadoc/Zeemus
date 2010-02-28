

package JD;
import robocode.*;
import java.awt.*;
import java.io.*;
import java.util.*;


public class Zeemus extends AdvancedRobot
{
	TargetList targets = new TargetList();
	
	Environment env = new Environment(targets);
	
	Planner plan = new Planner(env);
	
	//for the initial setup of the environment
	boolean initialized = false;
	
	//log hit success rate
	public int shotsFired = 0;
	public int shotsHit = 0;
	//stop firing if hit percentage falls below this amount
	public double CRITICAL_PERCENTAGE = .2;
	

	public void run() 
	{
	  
		//set up the environment during the first run command
		if(!initialized)
		{
			env.initialize(getBattleFieldWidth(), getBattleFieldHeight());
	      
	      	//Make the gun and radar turn independently
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			initialized = true;
	    }
			
		while(true)
		{
			env.setupMap(0);
			
			int direction = plan.navigate();
			execute(direction);
	
		}
	}
	
	public void execute(int dir)
	{
		
  	}
	
	public void aimFire(Location loc)
	{
		//only shoot if you are hitting the enemy x percent of the time
		if(1.0 * shotsHit / shotsFired > CRITICAL_PERCENTAGE || shotsFired < 15)
		{
			stop();
			
			Location here = new Location(getX(), getY());
			//make sure the target you are firing at is in bounds
			Location temp = new Location(Math.min(getBattleFieldWidth(),Math.max(0,loc.getX())),Math.min(getBattleFieldHeight(),Math.max(0,loc.getY())));
			
			turnGunCorrectly(here.degreeTo(temp));
			fire(1);
			shotsFired++;
			
			resume();
		}
	}
	

	
	//optimize turning for right and left
	public void turnGunCorrectly(double dir)
  	{
	    //make sure the number is not greather than 360
	    robocode.util.Utils.normalAbsoluteAngleDegrees(dir);
	    
	    //dir is now how far to turn right
	    dir -= getGunHeading(); 
	    
	    //optimize turning right and left
	    boolean right = true;
	    if(dir < 0)
	    {
	    	dir *= (-1);
	    	right = !right;
	    }
	    else if(dir > 180)
	    {
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
	
	public void onPaint(Graphics2D g)
	{
	   env.paint(g);
	}
}

class Environment
{
    //for setting up a map of dangerous locations, where a zero indicates safe and an integer
    //indicates a degree of danger
	int[][][] hotspots;
	//the amount of pixels per index in the map
	int GRANULARITY = 10;
	//the amount of timesteps in the map
	int TIMESCALE = 7;
	
	TargetList targets;
  
  
  
	public Environment(TargetList t)
	{
		targets = t;
  	}	
	
	public void initialize(double w, double h)
	{
	
		hotspots = new int[TIMESCALE][(int)w/GRANULARITY][(int)h/GRANULARITY];	
		for(int t=0; t<TIMESCALE; t++)
		{
			resetMap(t);
		}
	}
	 
	//set up so that the edges of the map are dangerous and all other indices are zero
	public void resetMap(int t)
	{
		for(int c=0; c< hotspots[t].length; c++)
		{
			for(int r=0; r< hotspots[t][c].length; r++)
			{
			hotspots[t][c][r] = 0;
			hotspots[t][c][0] = 10;
			hotspots[t][c][hotspots[t][0].length-1] = 10;
			hotspots[t][0][r] = 10;
			hotspots[t][hotspots[t].length-1][r] = 10;
			}
		}
	} 
	
	//set all positions of the map which might be dangerous
	public void setupMap(int t)
	{
		resetMap(t);
		
		if(!targets.isEmpty())
		{
			Location loc = targets.getTarget().getLocation();
			
			//this line produced an arrayIndexOutOfBoundsException at -2, which must mean that targetList is 
			//producing invalid locations
			hotspots[0][(int)loc.getX()/GRANULARITY][(int)loc.getY()/GRANULARITY] += 1;
		}
	}

	
	

		
	public int getTimescale()
	{
    return TIMESCALE;
    }
  
  public void paint(Graphics2D g)
  {
    //store the painter in case there is some setting I don't know about
		Paint tempPaint = g.getPaint();  
		
		//paint the walls
		g.draw(new Rectangle(0,0,hotspots[0].length*GRANULARITY,GRANULARITY));//left	                                 
		g.draw(new Rectangle((hotspots[0].length-1)*GRANULARITY,0,hotspots[0].length*GRANULARITY,GRANULARITY));//right		
		g.draw(new Rectangle(0,(hotspots[0][0].length-1)*GRANULARITY,GRANULARITY,hotspots[0].length*GRANULARITY));//top	
		g.draw(new Rectangle(0,0,GRANULARITY,hotspots[0].length*GRANULARITY));//bottom
		

		g.setPaint(new Color(255,0,0));
		
    // just paint the first timestep for now		
		for(int t=0; t<1; t++){
			for(int r=1; r< hotspots[t].length-1; r++){
				for(int c=1; c< hotspots[t][r].length-1; c++){
					if(hotspots[t][r][c] > 0){
						Rectangle box = new Rectangle(r*GRANULARITY,c*GRANULARITY,GRANULARITY,GRANULARITY);
						g.draw(box);
					}
				}
			}
		}
		
		g.setPaint(tempPaint);
		
  } 	
}

class Planner
{

  //the timestep representing the present.  Time cycles through the last dimension of the matrix
	//for example:  when timestep is at 29, the next tick is in hotspots[r][c][0]
	int timestep;

	//temporary variable to set how far away from enemies is dangerous, should be set by a learning method of some kind
	//temporarily unused
	int avoidanceRadius;
	
	Environment env;
	
	//An array of integers, each representing a single possible action. 
//	int[] directionList = new int[env.getTimescale()];
  int[] directionList = new int[1];
  
  public Planner(Environment e)
  {
    timestep = 0;
    env = e;
    avoidanceRadius = 100;
  } 


  public int navigate()
	{
	  directionList[0] = getRandomDirection(); 
	//	int answer = recursiveAvoidance(env, directionList, getX(), getY(), getHeading(), getVelocity(), 0, 0, TIMESCALE-1);
		
		
		
		
		return 0;
	}
	
	public int getRandomDirection()
	{
    Random rand = robocode.util.Utils.getRandom();
    return rand.nextInt();
	}
	
	public int recursiveAvoidance(Environment env, int[] directionList, double x, double y, double heading, double velocity, int time, int sum, int recursesLeft)
	{
		int min = 10000000;
		
		if(recursesLeft <= 0)
			return sum;
		
		
		int xMod = 0;
		int yMod = 0;
		double headingMod = 0;
		double velocityMod = 0;
		
		switch(directionList[time]){
			
			//case for start of method
			case 0: break;
			
			//ahead left
			case 1: break;
			
			//left (will go right if velocity = -X)
			case 11: break;
			
			//back left (will go right)
			case 21: break;
			
			//ahead straight
			case 101: break;
			
			//stop
			case 111: break;
			
			//straight back
			case 121: break;
			
			//ahead right
			case 201: break;
			
			//right (will go left if velocity = -X)
			case 211: break;
			
			//back right (will go right)
			case 221: break;
			
		}
		
		//will add a granularity factor
		time += 1;
		
		sum+= env.hotspots[time][(int)x/env.GRANULARITY][(int)y/env.GRANULARITY];
		
		for(int i=0; i<9; i++)
		{
			int dir = 1;
			dir+= (i/3) * 10;
			dir+= (i%3) * 100;
			
			directionList[time] = dir;
			
			int heat = recursiveAvoidance(env, directionList, x+xMod, y+yMod, heading+headingMod, velocity+velocityMod, time, sum, recursesLeft-1);

		}

		return 0;
	}
}

// Maintains a list of targets and decides which target to pursue
// Contains a class to predict where the enemy will be next
class TargetList
{
	public  ArrayList<BotLog> list;
		
	public TargetList(){
		list = new ArrayList<BotLog>();
	}	
	
	public  boolean isEmpty()
	{
		return list.size()<1;
	}
	
	public  void logRobot(ScannedRobotEvent e, double x, double y, double heading)
	{
		boolean newbot = true;
		for(int i=0; i<list.size(); i++)
		{
			//if this robot is already in the list then update him
			if(list.get(i).getName().equals(e.getName()))
			{
				newbot = false;
				list.get(i).logRobot(e,x,y,heading);
			}
		}
		
		if(newbot){
			list.add(new BotLog(e,x,y,heading));
		}
	}
	
	
	//take robot's heading, velocity, and distace to robot
	//estimate where they will be when the bullet arrives
	public  Location estimatePos(int power , Location zee)
	{
		Bot target = getTarget();
		return estimatePostitionHelper(zee, power, target.getVelocity(), target.getHeading(), 0, target.getLocation(), 5);	

	}
	
	//Helper method uses recursion to approximate future location
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
		
		
	//return the closest target for now
	public  Bot getTarget()
	{
		if(isEmpty())
			return null;
		return list.get(0).getLast();
	}
	
	public void getTargets()
	{
		return;
	}
}



//Manages a list of events for each robot
class BotLog
{
	public  ArrayList<Bot> log;
	private  String name;
	
	public BotLog()
	{
		log = new ArrayList<Bot>();
		name = null;
	}
	
	public BotLog(ScannedRobotEvent e, double x, double y, double heading)
	{
		log = new ArrayList<Bot>();
		name = e.getName();
		logRobot(e,x,y,heading);
	}
	
	public void logRobot(ScannedRobotEvent e, double x, double y, double heading)
	{
		log.add(new Bot(e,x,y,heading));		
	}
	
	public Bot getLast(){
		return log.get(log.size()-1);
	}
	
	public String getName(){
		return name;
	}
	
	public boolean isEmpty()
	{
		return log.size()<1;
	}
}


//container for storing information on scanned robots
class Bot
{
	private double heading;
	private double energy;
	private double distance;
	private double velocity;
	private long time;
	private Location loc;
		
	public Bot(ScannedRobotEvent e , double x, double y, double h)
	{
		heading = e.getHeading();
		energy = e.getEnergy();
		distance = e.getDistance();
		velocity = e.getVelocity();
		loc = new Location(e.getBearing(), distance, x, y, h);
				
	}
	
	public  double getHeading(){
		return heading;
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
		x = Math.max(0,x);
		y = Math.max(0,y);
		
	}

//  would it be better to move this logic outside of the constructor?	
//	public Location calculateLoc(double bearing, double distance, double sentX, double sentY double heading)

	
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

   
/*
 *For setting the area around an enemy as dangerous
 *
		int xMinus = (int)(Math.max(0, loc.getX() - 100))/GRANULARITY;
		int xPlus = (int)(Math.min(hotspots[0][0].length-1, loc.getX() + 100))/GRANULARITY;
		int yMinus = (int)(Math.max(0, loc.getY() - 100))/GRANULARITY;
		int yPlus = (int)(Math.min(hotspots[0].length-1, loc.getY() + 100))/GRANULARITY;
		
		for(int i=0; i<(int)AVOIDANCE_RADIUS/GRANULARITY; i++){
			hotspots[0][xMinus+i][yMinus] +=1;
			hotspots[0][xMinus][yMinus+i] +=1;
			hotspots[0][xPlus][yPlus-i] +=1;
			hotspots[0][xPlus-i][yPlus] +=1;




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
	
	
	
	////////////////////////////////////////
	
	
		public void setMovement()
	{
		ahead(100);
	}
	
	

	
	public void setRadar()
	{
		setTurnRadarRight(360);
	}
			
			
			
			

	

			
			******/