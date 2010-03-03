

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
			env = new Environment(getBattleFieldWidth(), getBattleFieldHeight(), getName());
			
			predictor = new Predictor(env);
				
			plan  = new Planner(env, predictor);
	      
	      	//Make the gun and radar turn independently
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			initialized = true;
	    }
			
		while(true)
		{
			//log yourself.  Zeemus will always occupy targets.get(0);
			env.logSelf(getEnergy(), getHeading(), getVelocity(), new Location(getX(), getY()));
			
			//reset the map including the dangerous locations for future timesteps
			plan.setupMap();
			
			//check the planned directions for obstacles
			plan.navigate();
			
			//get the next directions and set them for the next call to execute
			int direction = plan.getNextDirection();
			setDirection(direction);
			System.out.println("" + direction);
			
			setRadar();
			
			//execute directions which have been set
			execute();
	
		}
	}
	
	public void setDirection(int dir)
	{
		
		if(dir == 0)
			turnRadarLeft(360);
			
		switch(dir/100)
			{
				//left
				case 0:
					setTurnLeft(100);
					break;
				//straight
				case 1:
					setTurnLeft(0);
					break;
				//right
				case 2:
					setTurnRight(100);
					break;
				
			}
			
		switch((dir%100)/10)
			{
				//ahead
				case 0:
					setAhead(100);
					break;
				//stop (but still turn)
				case 1: 
					setAhead(0);
					break;
				//back
				case 2:
					setBack(100);
					break;
				
			}
  	}
  	
  	public void setRadar()
  	{
  		setTurnRadarRight(360);
  	}
	
	public void aimFire(int power, Location loc)
	{
		//only shoot if you are hitting the enemy x percent of the time
		//if(1.0 * shotsHit / shotsFired > CRITICAL_PERCENTAGE || shotsFired < 15)
		//{
			stop();
			
			Location here = new Location(getX(), getY());
			//make sure the target you are firing at is in bounds
			Location temp = new Location(Math.min(getBattleFieldWidth(),Math.max(0,loc.getX())),Math.min(getBattleFieldHeight(),Math.max(0,loc.getY())));
			
			turnGunCorrectly(here.degreeTo(temp));
			fire(power);
			shotsFired++;
			
			resume();
		//}
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
  
	//When scanning a robot, just tell the Environment to send it to the InstanceLog
	public void onScannedRobot(ScannedRobotEvent e) 
	{
		Location loc = env.calculateLoc(e.getBearing(), e.getDistance(), getX(), getY(), getHeading());
		env.logEnemy(e, loc);
		aimFire(2, predictor.getFiringPosition(2, new Location( getX(), getY()), env.getTarget() ));
	}
	
	public void onBulletHit(BulletHitEvent e)
	{
		shotsHit++;
	}
	
	public void onPaint(Graphics2D g)
	{
	   env.paint(g);
	}
	
	public void onHitWall(HitWallEvent e)
	{
		System.out.println("Hit Wall!");
	}
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Planner
{

	//temporary variable to set how far away from enemies is dangerous, should be set by a learning method of some kind
	//temporarily unused
	int avoidanceRadius;
	
	Environment env;
	
	//an object for anticipating the future movements of robots
	Predictor predictor;
	
	//A LinkedList of integers, each representing a single possible action.
	LinkedList<Integer> directions;
	
	public Planner(Environment e, Predictor p)
	{
		env = e;
    	avoidanceRadius = 100;
    	directions  = new LinkedList<Integer>();

		int dir = getRandomDirection();
		for(int i=0; i<20; i++)
			directions.add(dir);
		predictor = p;
  	}
  	
  	public int getNextDirection()
  	{
  		return (int)directions.remove();
  	}

  	public void navigate()
	{
		Target t = env.getTargets().getTarget(0);
		
		if(directions.size() <= 20)
		{
			int dir = getRandomDirection();
			for(int i=0; i<20; i++)
				directions.add(dir);
		}
		
		//check if the next five directions will hit something
		if(checkDirections(20, t.getLastLocation().getX(), t.getLastLocation().getY(), t.getLastInstance().getHeading(), t.getLastInstance().getVelocity(),0) > 0)
		{
			int dir = avoidance(env,t.getLastLocation().getX(), t.getLastLocation().getY(), t.getLastInstance().getHeading(), t.getLastInstance().getVelocity(), 0, 0, 20);			
		
			for(int i=0; i<20; i++)
			{
				directions.remove();
			}
			
			for(int i=0; i<20; i++)
			{
				directions.addFirst(dir);
			}
		}
	}
	
	
	//checks the possible directions for obstacles
	private int avoidance(Environment env, double x, double y, double heading, double velocity, int time, int sum, int n)
	{
		int min = 10000000;
		int minDir = 0;
		
		Target t = env.getTargets().getTarget(0);
				
		for(int i=0; i<9; i++)
		{
			int dir = 1;
			dir+= (i/3) * 10;
			dir+= (i%3) * 100;
			
			for(int j=0; j<n; j++)
			{
				directions.addFirst(dir);
			}
			
			int heat = checkDirections(20, t.getLastLocation().getX(), t.getLastLocation().getY(), t.getLastInstance().getHeading(), t.getLastInstance().getVelocity(),0);
			
			System.out.println(":"+heat);
			
			if(heat < min){
				min = heat;
				minDir = dir;
			}		
			
			for(int j=0; j<n; j++)
			{
				directions.remove();
			}

		}

		return minDir;
	}

	
	//check the next n directions for obstacles
	private int checkDirections(int n, double x, double y, double heading, double velocity, int t)
	{		
		int sum = 0;
		
		for(int i=0; i<n; i++){
			
			t++;
			
			if(directions.get(i) <= 0)
				return 0;
			switch(directions.get(i)/100)
				{
					
					//case for start of method
					case 0:
						heading -= (10 - 0.75*Math.abs(velocity));
						break;
					case 1: 
						break;
					case 2:
						heading += (10 - 0.75*Math.abs(velocity));
						break;
					
				}
				
				heading += 360;
				heading %= 360;
				
			switch((directions.get(i)%100)/10)
				{
					
					//case for start of method
					case 0:
						velocity = Math.min(8, velocity+1);
						break;
					case 1: 
						if(velocity == 1)
							velocity =0;
						else if(velocity > 0 )
							velocity -= 2;
						else if(velocity == -1)
							velocity = 0;
						else if(velocity < 0 )
							velocity +=2;
						break;
					case 2:
						velocity = Math.max(-8, velocity - 1);
						break;
					
				}
				
				x += Math.sin(Math.toRadians(heading)) * velocity;
				y += Math.cos(Math.toRadians(heading)) * velocity;
				
				x = Math.max(Math.min(x,800),0);
				y = Math.max(Math.min(y,600),0);
				
				sum += env.checkMap(t, x, y);
			}
			
		return sum;		
			
	}
	
	public void setupMap()
	{
		TargetList targets = env.getTargets();
			
		if(!env.getTargets().isEmpty())
		{
			for(int t = 0; t < env.getTimescale(); t++)
			{
				Location loc = predictor.predictFutureLocation(t, env.getTarget());
				Location tloc = new Location(0,0);
				if(!env.getTargets().isEmpty())
				{
					Location current = env.getTargets().getTarget(0).getLastLocation();
					tloc = predictor.getFiringPosition(2, current, env.getTarget());
				}
				env.setupMap(t, loc, tloc);
				
			}
		}
	}
	
	private int getRandomDirection()
	{
	    Random rand = robocode.util.Utils.getRandom();
	    int dir = 1; 					 	//recursive helper dummy direction = 0;
	    dir += rand.nextInt(2) * 20; 		//either a zero or a two, ahead or back
	    dir += rand.nextInt(3) * 100; 		//left, straight, or right
	    return dir;
	}
	
	public Predictor getPredictor()
	{
		return predictor;
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
  
	public Environment(double w, double h, String name)
	{
		height = h;
		width = w;
		targets = new TargetList(name);
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
	
	public void logSelf(double energy, double heading, double velocity, Location loc)
	{
		targets.logSelf(energy, heading, velocity, loc);
	}
	
	public int checkMap(int t, double x, double y)
	{
	
	  x = Math.max(0, Math.min(x, width-.00001) );
		y = Math.max(0, Math.min(y, height-.00001) );
	   
		checkedPositions.add(new Location(x,y));
		return hotspots[t][(int)x/GRANULARITY][(int)y/GRANULARITY];
	}
	
	public TargetList getTargets()
	{
		return targets;
	}
	
	public Target getTarget(){
		return targets.getTarget();
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
			g.setPaint(new Color((int)(255*(hotspots.length/(hotspots.length*(t+1)))), 0, 50));
			for(int r=1; r< hotspots[t].length-1; r++){
				for(int c=1; c< hotspots[t][r].length-1; c++){
					if(hotspots[t][r][c] > 0){
						Rectangle box = new Rectangle(r*GRANULARITY,c*GRANULARITY,Math.max(1,GRANULARITY-t/3),Math.max(1,GRANULARITY-t/3));
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

	public void logSelf(double energy, double heading, double velocity, Location loc)
	{
		tlist.get(0).logSelf(energy, heading, velocity, loc);
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
	
	public ArrayList<Target> getTargets()
	{
		return tlist;
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
	
	public void logSelf(double energy, double heading, double velocity, Location loc)
	{
		ilog.logSelf(energy, heading, velocity, loc);
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
	
	public void logSelf(double energy, double heading, double velocity, Location loc)
	{
		log.add(new Instance(energy, heading, velocity, loc));
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
//!! need to update the time for each log
//container for storing information on scanned robots
class Instance
{
	private double heading;
	private double energy;
	private double distance;
	private double velocity;
	private long time;
	private Location loc;
		
	public Instance(ScannedRobotEvent e , Location l)
	{
		heading = e.getHeading();
		energy = e.getEnergy();
		distance = e.getDistance();
		velocity = e.getVelocity();
		loc = l;
				
	}
	
	public Instance(double e, double h, double v,Location l)
	{
		heading = h;
		energy = e;
		velocity = v;
		loc = l;
		distance = 0;		
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
	
	
			
			
			
			

	

			
			******/