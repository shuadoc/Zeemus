

package JD;
import robocode.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class Zeemus extends AdvancedRobot
{

	Environment env;
	
	Planner plan;
	
	Predictor predictor;
	
	TargetList targets;
	
	//for the initial setup of the environment
	boolean initialized = false;	
		
	public void run() 
	{
	  
		if(!initialized)
		{
		  initialize();
		  initialized = true;
		}
		
		while(true)
		{
			//log yourself.  This robot will always occupy targets.get(0);
			env.logSelf(getTime(), getEnergy(), getHeading(), getVelocity(), getRadar(), getTurret(), new Location(getX(), getY()));
			
			//reset the map of dangerous locations
			plan.setupMap();
			
			//check the planned directions for obstacles
			plan.navigate();
			
			//get the next direction and set it for the next call to execute
			Direction dir = plan.getNextDirection();
			setDirection(dir);
			
			//execute directions which have been set
			execute();
	
		}
	}
	
	//set up the environment, planner, and predictor
	public void initialize()
	{
		targets = new TargetList(getName());
	     
		env = new Environment(getBattleFieldWidth(), getBattleFieldHeight(), targets);
			
		predictor = new Predictor(env);
				
		plan  = new Planner(targets, env, predictor);
	      
	    //Make the gun and radar turn independently
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true); 
  }
	
	
	public void setDirection(Direction dir)
	{      	
		
		setAhead(dir.getSpeed());
		setTurnRight(dir.getTurnRight());
		setTurnRadarRight(dir.getRadarRight());

	}

	
	public void fire(int power, Location enemyLoc)
	{
	    //get this robot's current location
			Location here = new Location(getX(), getY());
			
			//make sure the location recieved is in bounds
			Location inBounds = new Location(Math.min(getBattleFieldWidth(),Math.max(0,enemyLoc.getX())),Math.min(getBattleFieldHeight(),Math.max(0,enemyLoc.getY())));
			
			turnGunCorrectly(here.degreeTo(inBounds));
			super.fire(power);
	}

	
	//Whenever this robot sees another robot...
	public void onScannedRobot(ScannedRobotEvent e) 
	{
	    //tell the Environment to send it to the InstanceLog
	   	Location enemyLoc = env.calculateLoc(e.getBearing(), e.getDistance(), getX(), getY(), getHeading());
		  env.logEnemy(e, enemyLoc);
		  
		  //fire!  (power, predicted enemy location)
		  fire(2, predictor.getFiringPosition(2, new Location( getX(), getY()), targets.getTarget() ));
	}
	
	public void onBulletHit(BulletHitEvent e)
	{
	}
	
	public void onPaint(Graphics2D g)
	{
	   env.paint(g);
	}
	
	public void onHitWall(HitWallEvent e)
	{
		System.out.println("Hit Wall!");
	}
	
	//optimize turning for right and left
	//!!! currently every time the enemy passes over zeemus the gun turns the wrong direction
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
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Planner
{
	TargetList targets;
	Environment env;
	Predictor predictor;
	
	//A Queue of Directions (such as turn right and go back)
	LinkedList<Direction> directions;
	
	//each direction is repeated in the queue this many times
	int DIRECTION_REPETITIONS;
	
	public Planner(TargetList tl, Environment e, Predictor p)
	{
      targets = tl;
		  env = e;
    	directions  = new LinkedList<Direction>();
    	predictor = p;
    
    	DIRECTION_REPETITIONS = 20;
    
    	//add a random direction to the queue
		  Direction dir = new Direction(true);
		  for(int i=0; i<DIRECTION_REPETITIONS; i++)
		  {
		  	directions.add(dir);
		  }	
  }
  	
  //returns whatever direction was last thought to be ok	
  public Direction getNextDirection()
  {
  	return (Direction)directions.remove();
  }
  
  
  public void navigate()
	{
		Target t = targets.getTarget(0);
		
		//there always needs to be this many directions in the queue
		if(directions.size() <= DIRECTION_REPETITIONS)
		{
			Direction dir = new Direction(true);
			for(int i=0; i<DIRECTION_REPETITIONS; i++)
				directions.add(dir);
		}
		
		//check if the next twenty directions will cause the robot to collide with anything
		if(checkDirections(t.getLastLocation().getX(), t.getLastLocation().getY(), t.getLastInstance().getHeading(), t.getLastInstance().getVelocity()) > 0)
		{
		  //if they do, find a set of directions which will not,
			Direction dir = avoidance(env,t.getLastLocation().getX(), t.getLastLocation().getY(), t.getLastInstance().getHeading(), t.getLastInstance().getVelocity(), 0);			
		
		  //remove twenty of the old directions, and add twenty of the new directions
			for(int i=0; i<20; i++)
			{
				directions.remove();
			}
			for(int i=0; i<20; i++)
			{
				directions.addFirst(dir);
			}
		}
		
		setRadar();
		
	}
	
	private void setRadar()
	{
      directions.get(0).setRadarRight(360);
  }
  
  private void setTurret()
  {
  
  
  }
	
	
	//checks the possible directions for a clear path
	private Direction avoidance(Environment env, double x, double y, double heading, double velocity, int time)
	{
		int min = 10000000;
		Direction minDir = new Direction();
		
		Target t = targets.getTarget(0);
				
		for(int i=0; i<9; i++)
		{
		  //loop through the possible directions
		  Direction dir = new Direction();
		  // i=0,3,6: back      i=1,4,7: stop       i=2,5,8: ahead
		  dir.setSpeed(((i%3)-1)*100);
		  // i=0,1,2: left      i=3,4,5: straight       i=6,7,8: right
		  dir.setTurnRight(((i/3)-1)*100);
			
			//add the new direction n times
			for(int j=0; j<DIRECTION_REPETITIONS; j++)
			{
				directions.addFirst(dir);
			}
			
			//check to see if that direction will result in a collision
			int heat = checkDirections(t.getLastLocation().getX(), t.getLastLocation().getY(), t.getLastInstance().getHeading(), t.getLastInstance().getVelocity());
			
			//look for the a direction with zero (or the lowest) future damage
			if(heat < min){
				min = heat;
				minDir = dir;
			}		
			
			//remove what was changed to the direction list
			for(int j=0; j<DIRECTION_REPETITIONS; j++)
			{
				directions.remove();
			}                 
		}
		
		return minDir;
	}

	
	//check the set of n repeated directions for obstacles
	private int checkDirections(double x, double y, double heading, double velocity)
	{		
		int sum = 0;
		
		for(int i=0; i<DIRECTION_REPETITIONS; i++){                
		
    Direction dir = directions.get(i);
    	
    	//simulate turning                          
		if(dir.getTurnRight() > 0)
		{
        	heading += (10 - 	0.75*Math.abs(velocity));	
    	}
    	else if(dir.getTurnRight() < 0)
    	{
      		heading -= (10 - 0.75*Math.abs(velocity));
    	}
			
    	//ensure heading is both positive and less than 360	
			heading += 360;
			heading %= 360;
			
    	//simulate acceleration
    	if(dir.getSpeed() > 0)
    	{
     		velocity = Math.min(8, velocity+1);
    	}
    	else if(dir.getSpeed() < 0)
    	{
			velocity = Math.max(-8, velocity - 1);
    	}
		else
		{
        	if(velocity == 1)
				velocity =0;
			else if(velocity > 0 )
				velocity -= 2;
			else if(velocity == -1)
				velocity = 0;
			else if(velocity < 0 )
				velocity +=2;
    	}
			
    	//simulate movement	
		x += Math.sin(Math.toRadians(heading)) * velocity;
		y += Math.cos(Math.toRadians(heading)) * velocity;
		
		//check for inBounds
		x = Math.max(Math.min(x,799),0);
		y = Math.max(Math.min(y,599),0);
		

		sum += env.checkMap(i, x, y);	
		}
			
		return sum;		
			
	}
	
	//reset the map and add values to the dangerous locations
	public void setupMap()
	{
		if(!targets.isEmpty())
		{
			for(int t = 0; t < env.getTimescale(); t++)
			{
				Location futureLoc = predictor.predictFutureLocation(t, targets.getTarget());
				Location tloc = new Location(0,0);
				if(!targets.isEmpty())
				{
				
				  //hmm, recheck this
					Location current = targets.getTarget(0).getLastLocation();
					tloc = predictor.getFiringPosition(2, current, targets.getTarget());
				}
				env.setupMap(t, futureLoc, tloc);
				
			}
		}
	}
}

////////////////////////////////////////////////////////////////////////////////////////////////////
class Direction
{
	
	
	int speed;
	int turnRight;
	int radarRight;
	int turretRight;
  
	public Direction()
	{
		speed = 0;
		turnRight = 0;
		radarRight = 0;
		turretRight = 0;
	}
	
	public Direction(boolean t)
	{
	    Random rand = robocode.util.Utils.getRandom();
	    speed = (rand.nextInt(3)-1) * 100;
	    turnRight = (rand.nextInt(3)-1) * 100;		
	}
	  
	public void setSpeed(int s){
		speed = s;
	}
	  
	public void setTurnRight(int r){
		turnRight = r;
	}
	  
	public void setRadarRight(int r){
		radarRight = r;
	}
	  
	public void setTurretRight(int r){
		turretRight = r;
	}
	       
	public int getSpeed(){
		return speed;
	}
	
	public int getTurnRight(){
		return turnRight;
	}
	  
	public int getRadarRight(){
		return radarRight;
	}
	  
	public int getTurretRight(){
		return turretRight;
	} 

}

////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Predictor
{
	Environment env;
	
	
	public Predictor(Environment e)
	{
		env = e;		
	}
	

	//Linear Estimation
	//take robot's heading, velocity, and distace to robot
	//estimate where they will be when the bullet arrives
	public  Location getFiringPosition(int power , Location current, Target t)
	{
		return gfpHelper(current, power, t.getLastInstance().getVelocity(), t.getLastInstance().getHeading(), 0, t.getLastLocation(), 4);	
	}
	
	//Helper method uses recursion to approximate future location
	private Location gfpHelper(Location current, double power, double velocity, double heading, double priorDistance, Location loc, int recurseNum)
	{
		//check if this is the last recurse
		if(recurseNum <= 0)
			return loc;
			
		//calculate the distance to the target from the enemies estimated position
		double distance = Math.sqrt(Math.pow(loc.getX()-current.getX() , 2) + Math.pow(loc.getY()-current.getY() , 2));
		
		//update the amount of distance the calculation is now off by, and the amount of extra time the bullet will move
		distance -= priorDistance;
		double time = distance / (20 - power*3);
		
		//where will the enemy be after that amount of time has elapsed
		Location update = predictFutureLocation(loc, velocity, time, heading);
		
		return gfpHelper(current, power, velocity, heading, distance+priorDistance, update, recurseNum-1);
		
	}
	
	public Location predictFutureLocation(Location loc, double velocity, double time, double heading)
	{
		double x = loc.getX() + velocity * time * (Math.sin(Math.toRadians(heading)));
		double y = loc.getY() + velocity * time * (Math.cos(Math.toRadians(heading)));
		
		x = Math.max(0, Math.min(x, env.width-.00001) );
		y = Math.max(0, Math.min(y, env.height-.00001) );
		
		return new Location(x,y);
		
	}
	
	public Location predictFutureLocation(int time, Target t)
	{
		return predictFutureLocation(t.getLastLocation(), t.getLastInstance().getVelocity(), (double)time, t.getLastInstance().getHeading());
	}
	
	
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Environment
{
	public double height;
	public double width;
	
	//for painting where avoidance looks at
	ArrayList<Location> checkedPositions = new ArrayList<Location>();

	TargetList targets;
	
	Location targetingLocation;
	
    //for setting up a map of dangerous locations, where a zero indicates safe and an integer
    //indicates a degree of danger
	int[][][] hotspots;
	//the amount of pixels per index in the map
	int GRANULARITY = 10;
	//the amount of timesteps in the map
	//does not imply an ability to correctly predict this far, simply must always be larger than directions.size()
	int TIMESCALE = 42;
  
	public Environment(double w, double h, TargetList tl)
	{
		height = h;
		width = w;
		targets = tl;
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
	
	//setup the map at the beginning of each timestep
	public void setupMap(int t, Location loc, Location tloc)
	{
		//reset all of the checked positions
		checkedPositions = new ArrayList<Location>();
		
		targetingLocation = tloc;
		
		resetMap(t);
		
		//set all of the Dangerous positions of the map
		hotspots[t][(int)loc.getX()/GRANULARITY][(int)loc.getY()/GRANULARITY] += 1;
	}
	
	public void logEnemy(ScannedRobotEvent e, Location loc)
	{
		targets.logInstance(e, loc);
	}
	
	public void logSelf(long time, double energy, double heading, double velocity, Location loc)
	{
		targets.logSelf(time, energy, heading, velocity, loc);
	}
	
	public int checkMap(int t, double x, double y)
	{
	
	  x = Math.max(0, Math.min(x, width-.00001) );
		y = Math.max(0, Math.min(y, height-.00001) );
	   
		checkedPositions.add(new Location(x,y));
		return hotspots[t][(int)x/GRANULARITY][(int)y/GRANULARITY];
	}
		
	public int getTimescale()
	{
    return TIMESCALE;
    }
    
    //onPaint only allows a few hundred paint events or something like that  
	public void paint(Graphics2D g)
	{
    //store the painter in case there is some setting I don't know about
		Paint tempPaint = g.getPaint();  
		
		//paint the walls (i assume that hotspots does contain these areas, it would be too many paint events to paint them
		g.draw(new Rectangle(0,0,hotspots[0].length*GRANULARITY,GRANULARITY));//left	                                 
		g.draw(new Rectangle((hotspots[0].length-1)*GRANULARITY,0,hotspots[0].length*GRANULARITY,GRANULARITY));//right		
		g.draw(new Rectangle(0,(hotspots[0][0].length-1)*GRANULARITY,GRANULARITY,hotspots[0].length*GRANULARITY));//top	
		g.draw(new Rectangle(0,0,GRANULARITY,hotspots[0].length*GRANULARITY));//bottom
		
		//paint the positions of various danger zones
		for(int t=0; t<hotspots.length; t++){
			g.setPaint(new Color((int)(255 - ( 255.0 * t/hotspots.length )), 0, 50));
			for(int r=1; r< hotspots[t].length-1; r++){
				for(int c=1; c< hotspots[t][r].length-1; c++){
					if(hotspots[t][r][c] > 0){
						Rectangle box = new Rectangle(r*GRANULARITY, c*GRANULARITY, (t==0)?(10):(1) , (t==0)?(10):(2) );
						g.draw(box);
					}
				}
			}
		}
		
		//paint the posistion where you should fire at the enemy orange
		g.setPaint(new Color(250,165,0));
		g.draw(new Rectangle((int)targetingLocation.getX(), (int)targetingLocation.getY(),GRANULARITY,GRANULARITY));
		
		
		//paint the areas of the map that avoidance is considering
		for(int i=0; i<checkedPositions.size(); i++)
		{
			g.setPaint(new Color(0, Math.max(255 - (i*10),0) , Math.min(255, 50+(i*10))));
			g.draw(new Rectangle((int)checkedPositions.get(i).getX(), (int)checkedPositions.get(i).getY(), 2,2));
		}
		
		g.setPaint(tempPaint);
	}

	public Location calculateLoc(double bearing, double distance, double sentX, double sentY, double heading) 
	{
		
		//calculate their heading
		double direction = heading+bearing;
		//remove negatives and greater than 360's
		direction = (direction + 360)%360;
		
		//add the x and y components of their distance to your x and y
		double x = sentX + distance * Math.sin(Math.toRadians(direction));
		double y = sentY + distance * Math.cos(Math.toRadians(direction));
		
		//for some reason the approximation of thier location produces out of bounds numbers occasionally
		//make sure the result is in bounds, subtract one to make sure the index never tries to access [600]
		x = Math.max(0, Math.min(x, width-.00001) );
		y = Math.max(0, Math.min(y, height-.00001) );
		
		return new Location(x,y);
	}
	
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Maintains a list of targets and decides which target to pursue
class TargetList
{
	public  ArrayList<Target> tlist;
		
	public TargetList(String name)
	{
		tlist = new ArrayList<Target>();
		tlist.add(new Target(name));
	}	
	
	//If it is empty OtherThanZeemus
	public  boolean isEmpty()
	{
		return tlist.size()<2;
	}
	
	public  void logInstance(ScannedRobotEvent e, Location loc)
	{
		boolean newbot = true;
		for(int i=0; i<tlist.size(); i++)
		{
			//if this robot is already in the list then update him
			if(tlist.get(i).getName().equals(e.getName()))
			{
				newbot = false;
				tlist.get(i).logEnemy(e, loc);
			}
		}
		
		if(newbot)
		{
			tlist.add(new Target(e.getName()));
			logInstance(e, loc);
			//run the method again to log the instance
		}
	}

	public void logSelf(long time, double energy, double heading, double velocity, Location loc)
	{
		tlist.get(0).logSelf(time, energy, heading, velocity, loc);
	}		
				
	//return the first target seen for now
	public  Target getTarget()
	{
		if(isEmpty())
			return null;
		return tlist.get(1);
	}
	
	public Target getTarget(int i)
	{
		return tlist.get(i);
	}
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Target
{
	private InstanceLog ilog;
	private String name;
	
	public Target(String n)
	{
		name = n;
		ilog = new InstanceLog();
	}
	
	public void logEnemy(ScannedRobotEvent e, Location loc)
	{
		ilog.logInstance(e, loc);
	}
	
	public void logSelf(long time, double energy, double heading, double velocity, Location loc)
	{
		ilog.logSelf(time, energy, heading, velocity, loc);
	}
	
	public String getName()
	{
		return name;
	}
	
	public Instance getLastInstance()
	{
		return ilog.getLastInstance();
	}
	
	public Location getLastLocation()
	{
		return ilog.getLastInstance().getLocation();
	}
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Manages a list of events for each robot
class InstanceLog
{
	private  ArrayList<Instance> log;
	
	public InstanceLog()
	{
		log = new ArrayList<Instance>();
	}

	
	public void logInstance(ScannedRobotEvent e, Location loc)
	{
		log.add(new Instance(e, loc));		
	}
	
	public void logSelf(long time, double energy, double heading, double velocity, Location loc)
	{
		log.add(new Instance(time, energy, heading, velocity, loc));
	}
	
	public Instance getLastInstance(){
		return log.get(log.size()-1);
	}
	
	public Instance getInstance(int i)
	{
		return log.get(i);
	}
	
	public boolean isEmpty()
	{
		return log.size()<1;
	}
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//container for storing information on scanned robots
class Instance
{
	private double heading;
	private double energy;
	private double distance;
	private double velocity;
	private long time;
	private Location loc;
	private double radar;
	private double turret;
		
	public Instance(ScannedRobotEvent e , Location l)
	{
		heading = e.getHeading();
		energy = e.getEnergy();
		distance = e.getDistance();
		velocity = e.getVelocity();
		time = e.getTime();
		loc = l;
		radar = 0;
		turret = 0;
				
	}
	
	public Instance(long t, double e, double h, double v, double r, double tur, Location l)
	{
		time = t;
		heading = h;
		energy = e;
		velocity = v;
		loc = l;
		distance = 0;
    radar = r;
    turret = tur;		
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
	public long getTime(){
		return time;
	}
	
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
	

	////////////////////////////////////////
	
	
		
	private int recursiveAvoidance(Environment env, double x, double y, double heading, double velocity, int time, int sum, int recursesLeft)
	{
		int min = 10000000;
		
		if(recursesLeft <= 0)
			return sum;
		
		
		int xMod = 0;
		int yMod = 0;
		double headingMod = 0;
		double velocityMod = 0;
		
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
	
	
			
	/////////////////////////////////////////////////////////////Correct Turning/////////
  

  		
			
			

	

			
			******/