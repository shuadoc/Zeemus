

package JD;
import robocode.*;
import java.awt.*;
import java.io.*;
import java.util.*;


//right now many methods and classes have half implementations or mixed implementations.  I really need to clean everything up




public class Zeemus extends AdvancedRobot
{
	TargetList targets;
	
	Environment env;
	
	Planner plan;
	
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
			targets = new TargetList(getBattleFieldWidth(), getBattleFieldHeight());
			env = new Environment(targets, getBattleFieldWidth(), getBattleFieldHeight());
			plan  = new Planner(env);
	      
	      	//Make the gun and radar turn independently
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			
			initialized = true;
	    }
			
		while(true)
		{
			//log yourself.  Zeemus will always occupy targets.get(0);
			targets.logSelf(getName(),getEnergy(), getHeading(), getVelocity(), getX(), getY());
			
			env.setupMap();

			plan.navigate();
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
				
				//case for start of method
				case 0:
					setTurnLeft(100);
					break;
				case 1:
					setTurnLeft(0);
					break;
				case 2:
					setTurnRight(100);
					break;
				
			}
			
		switch((dir%100)/10)
			{
				
				//case for start of method
				case 0:
					setAhead(100);
					break;
				case 1: 
					setAhead(0);
					break;
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
  
	//When scanning a robot, just tell the InstanceLog to deal with it
	public void onScannedRobot(ScannedRobotEvent e) 
	{
		targets.logInstance(e, getX(), getY(), getHeading());
//		aimFire(2, targets.getTarget().getFiringPosition(2, new Location(getX(),getY())));
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

class Environment
{
    //for setting up a map of dangerous locations, where a zero indicates safe and an integer
    //indicates a degree of danger
    //currently there is no utilization of the first dimension of the matrix
	int[][][] hotspots;
	//the amount of pixels per index in the map
	int GRANULARITY = 10;
	//the amount of timesteps in the map
	int TIMESCALE = 42;
	
	ArrayList<Location> locsToPaint= new ArrayList<Location>();
	
	TargetList targets;
	
	double height, width;
  
  
  
	public Environment(TargetList tl, double w, double h)
	{
		targets = tl;
		height = h;
		width = w;
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
	public void setupMap()
	{
		locsToPaint = new ArrayList<Location>();
		
		if(!targets.isEmpty())
		{
			for(int t = 0; t < TIMESCALE; t++)
			{
				resetMap(t);
				Location loc = targets.getTarget().predictFutureLocation(t);
				hotspots[t][(int)loc.getX()/GRANULARITY][(int)loc.getY()/GRANULARITY] += 1;
			}
		}
	}
	
	public int checkMap(int t, double x, double y)
	{
		x = Math.min(Math.max(0,x),799);
		y = Math.min(Math.max(0,y),599);
		addLoc(new Location(x,y));
		return hotspots[t][(int)x/GRANULARITY][(int)y/GRANULARITY];
	}
	
	public TargetList getTargets()
	{
		return targets;
	}

		
	public int getTimescale()
	{
    return TIMESCALE;
    }
    
    public void addLoc(Location loc)
    {
    	locsToPaint.add(loc);
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
		if(!targets.isEmpty())
		{
			//produces null pointer exceptions ocassionally
			Location zee = targets.getTarget(0).getLastLocation();
			Location fPosition = targets.getTarget().getFiringPosition(2,zee);
			g.setPaint(new Color(250,165,0));
			g.draw(new Rectangle((int)fPosition.getX(), (int)fPosition.getY(),GRANULARITY,GRANULARITY));
		}
		
		
		//paint the areas of the map that avoidance is considering
		for(int i=0; i<locsToPaint.size(); i++)
		{
			g.setPaint(new Color(0, Math.max(255 - (i*10),0) , Math.min(255, 50+(i*10))));
			g.draw(new Rectangle((int)locsToPaint.get(i).getX(), (int)locsToPaint.get(i).getY(), 2,2));
		}
		
		g.setPaint(tempPaint);
		
  } 	
}

class Planner
{	

  	//the timestep representing the present.  Time cycles through the last dimension of the matrix
	//for example:  when timestep is at 29, the next tick is in hotspots[0][r][c]
	int timestep;

	//temporary variable to set how far away from enemies is dangerous, should be set by a learning method of some kind
	//temporarily unused
	int avoidanceRadius;
	
	Environment env;
	
	//A LinkedList of integers, each representing a single possible action.
	LinkedList<Integer> directions;
	
	public Planner(Environment e)
	{
		timestep = 0;
		env = e;
    	avoidanceRadius = 100;
    	directions  = new LinkedList<Integer>();

		int dir = getRandomDirection();
		for(int i=0; i<20; i++)
			directions.add(dir);
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
			System.out.println(dir);
			
		
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
	
	private int getRandomDirection()
	{
	    Random rand = robocode.util.Utils.getRandom();
	    int dir = 1; 					 	//recursive helper dummy direction = 0;
	    dir += rand.nextInt(2) * 20; 		//either a zero or a two, ahead or back
	    dir += rand.nextInt(3) * 100; 		//left, straight, or right
	    return dir;
	}
}

// Maintains a list of targets and decides which target to pursue
// Contains a class to predict where the enemy will be next
class TargetList
{
	public  ArrayList<Target> tlist;
	private double height, width;
		
	public TargetList(double w, double h)
	{
		tlist = new ArrayList<Target>();
		height = h;
		width = w;
	}	
	
	//If it is empty OtherThanZeemus
	public  boolean isEmpty()
	{
		return tlist.size()<2;
	}
	
	public  void logInstance(ScannedRobotEvent e, double x, double y, double heading)
	{
		boolean newbot = true;
		for(int i=0; i<tlist.size(); i++)
		{
			//if this robot is already in the list then update him
			if(tlist.get(i).getName().equals(e.getName()))
			{
				newbot = false;
				tlist.get(i).logInstance(e, x, y, heading);
			}
		}
		
		if(newbot)
		{
			tlist.add(new Target(e, x, y, heading, width, height));
		}
	}
	
	//needs to start with the initialize method
	public void logSelf(String name ,double energy, double heading, double velocity, double x, double y)
	{
		boolean newbot = true;

		//if this robot is already in the list then update him
		if(tlist.size()!=0)
			tlist.get(0).logSelf(energy, heading, velocity, x, y);
		else
			tlist.add(new Target(name, energy, heading, velocity, x, y, width, height));
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
	
class Target
{
	
	private InstanceLog ilog;
	private String name;
	public double height, width;
	
	public Target()
	{
		name = "used default constructor";
		ilog = new InstanceLog();		
		
	}
	
	public Target(String n ,double energy, double heading, double velocity, double x, double y, double w, double h)
	{
		name = n;
		ilog = new InstanceLog();
		ilog.logSelf(energy, heading, velocity, x, y);
		height = h;
		width = w;
	}
	
	public Target(ScannedRobotEvent e, double x, double y, double heading, double w, double h)
	{
		name = e.getName();
		ilog = new InstanceLog();
		ilog.logInstance(e,x,y,heading);
		height = h;
		width = w;
	}
	
	public void logInstance(ScannedRobotEvent e, double x, double y, double heading)
	{
		ilog.logInstance(e,x,y,heading);
	}
	
	public void logSelf(double energy, double heading, double velocity, double x, double y)
	{
		ilog.logSelf(energy, heading, velocity, x, y);
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
	
	
	//Linear Estimation
	//take robot's heading, velocity, and distace to robot
	//estimate where they will be when the bullet arrives
	//Location zee refers to the location of this robot (zeemus)
	public  Location getFiringPosition(int power , Location zee)
	{
		return gfpHelper(zee, power, ilog.getLastInstance().getVelocity(), ilog.getLastInstance().getHeading(), 0, ilog.getLastInstance().getLocation(), 4);	
	}
	
	//Helper method uses recursion to approximate future location
	private Location gfpHelper(Location zee, double power, double velocity, double heading, double priorDistance, Location loc, int recurseNum)
	{
		//check if this is the last recurse
		if(recurseNum <= 0)
			return loc;
			
		//calculate the distance to the target from the enemies estimated position
		double distance = Math.sqrt(Math.pow(loc.getX()-zee.getX() , 2) + Math.pow(loc.getY()-zee.getY() , 2));
		
		//update the amount of distance the calculation is now of by, and the amount of extra time the bullet will move
		distance -= priorDistance;
		double time = distance / (20 - power*3);
		
		//where will the enemy be after that amount of time has elapsed
		Location update = predictFutureLocation(loc, velocity, time, heading);
		
		return gfpHelper(zee, power, velocity, heading, distance+priorDistance, update, recurseNum-1);
		
	}
	
	public Location predictFutureLocation(Location loc, double velocity, double time, double heading)
	{
		double x = loc.getX() + velocity * time * (Math.sin(Math.toRadians(heading)));
		double y = loc.getY() + velocity * time * (Math.cos(Math.toRadians(heading)));
		
		x = Math.min(Math.max(x, 0), width-1);
		y = Math.min(Math.max(y, 0), height-1);
		
		return new Location(x,y);
		
	}
	
	public Location predictFutureLocation(int time)
	{
		return predictFutureLocation(getLastLocation(), ilog.getLastInstance().getVelocity(), (double)time, ilog.getLastInstance().getHeading());
	}
}



//Manages a list of events for each robot
class InstanceLog
{
	private  ArrayList<Instance> log;
	
	public InstanceLog()
	{
		log = new ArrayList<Instance>();
	}

	
	public void logInstance(ScannedRobotEvent e, double x, double y, double heading)
	{
		log.add(new Instance(e,x,y,heading));		
	}
	
	public void logSelf(double energy, double heading, double velocity, double x, double y)
	{
		log.add(new Instance(energy, heading, velocity, x, y));
	}
	
	public Instance getLastInstance(){
		return log.get(log.size()-1);
	}
	
	public boolean isEmpty()
	{
		return log.size()<1;
	}
}


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
		
	public Instance(ScannedRobotEvent e , double x, double y, double h)
	{
		heading = e.getHeading();
		energy = e.getEnergy();
		distance = e.getDistance();
		velocity = e.getVelocity();
		loc = new Location(e.getBearing(), distance, x, y, h);
				
	}
	
	public Instance(double e, double h, double v, double x, double y)
	{
		heading = h;
		energy = e;
		velocity = v;
		loc = new Location(x,y);
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
		
		//make sure the result is in bounds
		x = Math.max(0,x);
		y = Math.max(0,y);
		
		//!!!!This method currently can return x and y values greater than the battlefield bounds
		//IS NOT A ROBUST METHOD! is there a way to do this without sending the information?
		x = Math.min(799, x);
		y = Math.min(599, y);
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
	
	//////////////////////////
	
	
		
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