
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
		
	public void run() 
	{
		targets = new TargetList(getName());
	    	
		predictor = new Predictor();
         
		env = new Environment((int)getBattleFieldWidth(), (int)getBattleFieldHeight(), targets, predictor);
		
		predictor.initialize(env.getWidth(), env.getHeight());
				
		plan  = new Planner(targets, env, predictor);
	      
        //Make the gun and radar turn independently
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true); 
		

		while(true)
		{
			//log yourself.  This robot will always occupy targets.get(0);
			targets.logSelf(getTime(), getEnergy(), getHeading(), getVelocity(), getRadarHeading(), getGunHeading(), new Location(getX(), getY()));
			
			//check the planned directions for obstacles, make changes as needed
			plan.navigate();
			
			//get the next direction
			Direction dir = plan.getNextDirection();
			setDirection(dir);
			
			//debug();
			
			//execute set directions
			execute();
			
			if(dir.getPower() > 0)
		    	fire(dir.getPower());
	
		}
	}	
	
	public void setDirection(Direction dir)
	{ 
        setTurnGunRight(dir.getGunRight());
        setAhead(dir.getSpeed());
		setTurnRight(dir.getTurnRight());
		setTurnRadarRight(dir.getRadarRight());
	}

	
	//Whenever this robot sees another robot, log their information
	public void onScannedRobot(ScannedRobotEvent e) 
	{
	   	Location enemyLoc = env.calculateLoc(e.getBearing(), e.getDistance(), getX(), getY(), getHeading());
		targets.logInstance(e, enemyLoc);
		
	}
	
	public void onPaint(Graphics2D g)
	{
		env.paint(g);
	}
	
	public void onHitWall(HitWallEvent e)
	{
		System.out.println("Hit Wall!");
	}
	
	public void onSkippedTurn(SkippedTurnEvent e)
	{
		System.out.println("Skipped Turn");
	}
	
	public void debug()
	{
  		System.out.println("current state:");
    	System.out.println(targets.getSelf().getLastInstance());
    	
    	System.out.println("First direction:");
    	System.out.println(plan.directions.get(0));
    	
    	System.out.println("Second direction:");
    	System.out.println(plan.directions.get(1));
    	
    	System.out.println("simulated state after next 2 directions:");
    	Instance next = plan.simulateDirection(targets.getSelf().getLastInstance(), plan.directions.get(0));
    	System.out.println(plan.simulateDirection(next, plan.directions.get(1)));
  
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
	//avoidance replaces this many directions at a time
	int AVOIDANCE_REPETITIONS;
	
	public Planner(TargetList tl, Environment e, Predictor p)
	{
    	targets = tl;
		env = e;
    	directions  = new LinkedList<Direction>();
    	predictor = p;
    
    	DIRECTION_REPETITIONS = 20;
    
    	upkeepListSize();	
	}
	
	public void navigate()
	{
		setupMap();
		upkeepListSize();		
		avoidObstacles();		
		setRadar();
		setGun();
		setFiring();
	}
  	
  	public void upkeepListSize()
  	{
  		//there always needs to be this many directions in the queue
		if(directions.size() <= DIRECTION_REPETITIONS)
			repeatedAdd(new Direction(true));
  	}
  	
  	public void repeatedAddFirst(Direction dir)
  	{
  		for(int i=0; i<DIRECTION_REPETITIONS; i++)
			directions.addFirst(dir);
  	}
  	
  	public void repeatedAdd(Direction dir)
  	{
  		for(int i=0; i<DIRECTION_REPETITIONS; i++)
			directions.add(dir);
  	}
  	
  	public void repeatedRemove()
  	{
  		for(int i=0; i<DIRECTION_REPETITIONS; i++)
			directions.remove();
  	}
  	
	//returns whatever direction is scheduled to be executed next
	public Direction getNextDirection()
	{
		return (Direction)directions.remove();
	}
	
	private void setRadar()
	{
	    double RADAR_SLIP_COMPENSATION = 1.2;
	    int TIME_TILL_REACQUIRE = 5;
	
	    if(targets.isEmpty())
	    {
        	directions.get(0).setRadarRight(360);
      }
	    
	    else
      	{
	        int timeSinceLastScan = (int) (targets.getSelf().getLastInstance().getTime() - targets.getTarget().getLastInstance().getTime());
	        
	        //anticipate where this robot will be next timestep   
	    	Location futureLoc = simulateDirection(targets.getSelf().getLastInstance(), directions.get(0)).getLocation();
	    	//predict where they should be next timestep
	        Location predictedTargetLoc = predictor.predictFutureLocation(timeSinceLastScan + 1, targets.getTarget());
	                                                                                
	        //MarkovRecurser mr = predictor.predictFutureLocationMarkov(timeSinceLastScan +1, targets.getTarget());
	          
	        double currentRadar = targets.getSelf().getLastInstance().getRadarHeading();
	        double neededRadar = futureLoc.degreeTo(predictedTargetLoc);
	        double changeInRadar = neededRadar - currentRadar;
	        
	        if(changeInRadar < -180)
		        changeInRadar += 360;
		    if(changeInRadar > 180)
		    	changeInRadar -= 360;
	        
	        //when a small amount of time has elapsed since last scan, turn past the area where you think you need to
	        changeInRadar *= Math.pow(RADAR_SLIP_COMPENSATION, timeSinceLastScan-1);
	        
	        //when a significat amount of time has elapsed, re-acquire.
	        if(timeSinceLastScan > TIME_TILL_REACQUIRE)
	        	changeInRadar = 360;
	          
	        directions.get(0).setRadarRight((int)changeInRadar);
    	}
	}	
  
	private void setGun()
	{
		if(!targets.isEmpty())
		{
			//anticipate where this robot will be next timestep   
			Location futureLoc = simulateDirection(targets.getSelf().getLastInstance(), directions.get(0)).getLocation();
			//anticipate where the enemy will be after a bullet travels to their position
			Location targetingLoc = predictor.getFiringPosition(3, futureLoc, targets.getTarget());
			
			double currentGun = targets.getSelf().getLastInstance().getGunHeading();
			double neededGun = futureLoc.degreeTo(targetingLoc);
			double changeInGun = neededGun - currentGun;
         
			if(changeInGun < -180)
				changeInGun += 360;
			if(changeInGun > 180)
				changeInGun -= 360;
     
			directions.get(0).setGunRight((int)changeInGun);
		}
	}
	
	private void setFiring()
	{
		if(targets.getSelf().getLastInstance().getTime() % 10 == 0)
			directions.get(0).setPower(3);
	}
	
	
	
	//adjusts planned movement for obstacle avoidance
	private void avoidObstacles()
	{
        		
		//check if the next n directions will cause the robot to collide with anything
		if(checkDirections() > 0)
		{
			
			int min = 10000000;
			Direction minDir = new Direction(true);
				
			//loop through the possible directions		
			for(int i=0; i<8; i++)
			{
				Direction dir = new Direction();
				
				switch(i)
                {
				    case(0):dir.setSpeed(100);
				            dir.setTurnRight(100);
				            break;
				    case(1):dir.setSpeed(100);
				            dir.setTurnRight(-100);
				            break;
				    case(2):dir.setSpeed(100);
				            dir.setTurnRight(0);
				            break;
				    case(3):dir.setSpeed(-100);
				            dir.setTurnRight(100);
				            break;
				    case(4):dir.setSpeed(-100);
				            dir.setTurnRight(-100);
				            break;
				    case(5):dir.setSpeed(-100);
				            dir.setTurnRight(0);
				            break;
				    case(6):dir.setSpeed(0);
				            dir.setTurnRight(100);
				            break;
				    case(7):dir.setSpeed(0);
				            dir.setTurnRight(-100);
				            break;
				}
				
				//append the direction to be checked to the beginning of the list
				repeatedAddFirst(dir);
				
				//check to see if the added direction will result in a collision
				int heat = checkDirections();
				
				//if this direction causes the least damage, keep it
				if(heat < min){
					min = heat;
					minDir = dir;
				}
                		
				
				//return the direction list to its original state
				repeatedRemove();                
			}
			
			//remove the directions which hit obstacles and add the direction with a clear path
			repeatedRemove();
			repeatedAddFirst(minDir);
		}
	}

	
	//check the set of n repeated directions for obstacles
	private int checkDirections()
	{	
		Instance inst = targets.getSelf().getLastInstance();	
		int sum = 0;
		
		for(int i=0; i<DIRECTION_REPETITIONS; i++){                
			
	    	Direction dir = directions.get(i);
	    	
	    	inst = simulateDirection(inst, dir);
	    	
			sum += env.checkMap(i, inst.getLocation().getX(), inst.getLocation().getY());
				
		}
		return sum;		
	}

	//returns the simulated change a direction would cause
	public Instance simulateDirection(Instance inst, Direction dir)
	{
		double heading = inst.getHeading();
		double velocity = inst.getVelocity();
		double x = inst.getLocation().getX();
		double y = inst.getLocation().getY();
		
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
		
		return new Instance(inst.getTime() + 1 , inst.getEnergy(), heading, velocity, inst.getRadarHeading(), inst.getGunHeading(), new Location(x,y));
	}
	
	//reset the map and add values to the dangerous locations
	public void setupMap(){env.setupMap();}
	
}

////////////////////////////////////////////////////////////////////////////////////////////////////
class Direction
{
	int speed;
	int turnRight;
	int radarRight;
	int gunRight;
	int power;
  
	public Direction()
	{
		speed = 0;
		turnRight = 0;
		radarRight = 0;
		gunRight = 0;
		power = 0;
		
	}
	
	//creates a random direction regardless of boolean
	public Direction(boolean t)
	{
	    Random rand = robocode.util.Utils.getRandom();
	    // either forward( 0 + 1) or backward( -2 +1 )
	    speed = (rand.nextInt(2)*-2 + 1) * 100;
	    // left (-1), straight ( 0 ), or right (1);
	    turnRight = (rand.nextInt(3)-1) * 100;

      power = 0;		
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
	  
	public void setGunRight(int r){
		gunRight = r;
	}
	
	public void setPower(int p){
    power = p;
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
	  
	public int getGunRight(){
		return gunRight;
	}
  
	public int getPower(){
		return power;
	}
	
	public String toString()
	{
		
	return "Direction to implement---- "+ "\n" + 
		"Speed: "+ speed + "\n" +
		"Turn Right: "+ turnRight + "\n" +
		"Radar Right: "+ radarRight + "\n" +
		"GunRight: "+ gunRight + "\n" +
		"Power: "+ power + "\n" +
		"-" + "\n";
	}

}

////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Predictor
{
	int width;
	int height;	
	
	public Predictor(){};
	
	public void initialize(int w, int h)
	{		
	    width = w;
	    height = h;
	}
	

    /**
    *         Linear Estimation
    * take robot's heading, velocity, and distace to robot
    * estimate where they will be when the bullet arrives
    *        
    */

	public  Location getFiringPosition(int power , Location current, Target t)
	{
		return linearHelper(current, power, t.getLastInstance().getVelocity(), t.getLastInstance().getHeading(), 0, t.getLastLocation(), 4);	
	}
	
	//Helper method uses recursion to approximate future location
	private Location linearHelper(Location current, double power, double velocity, double heading, double priorDistance, Location loc, int recurseNum)
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
		Location update = predictFutureLocationLinear(loc, velocity, time, heading);
		
		return linearHelper(current, power, velocity, heading, distance+priorDistance, update, recurseNum-1);
		
	}

	
	public Location predictFutureLocationLinear(Location loc, double velocity, double time, double heading)
	{
		double x = loc.getX() + velocity * time * (Math.sin(Math.toRadians(heading)));
		double y = loc.getY() + velocity * time * (Math.cos(Math.toRadians(heading)));
		
		x = Math.max(0, Math.min(x, width) );
		y = Math.max(0, Math.min(y, height) );
		
		return new Location(x,y);
		
	}
	
	public Location predictFutureLocation(int time, Target t)
	{
		return predictFutureLocationLinear(t.getLastLocation(), t.getLastInstance().getVelocity(), (double)time, t.getLastInstance().getHeading());
	}
	
    /**
    *         Markov chain estimation
    * traverse through a sequence of enemy states
    * choose the path of maximum likelyhood
    * (the state they have most often gone to from each previous state)    
    *        
    */
	
		
	public MarkovRecurser predictFutureLocationMarkov(int time, Target t)
	{
	    MarkovRecurser mi = new MarkovRecurser(t.getLastInstance());
	    
	    //return the MarkovRecurser which contains the location you should fire upon
	    //also contains the probability of a robot actually occupying that location
	    return recurseMarkov(mi,t.mmap);
	}
	
	private MarkovRecurser recurseMarkov(MarkovRecurser mr, MarkovMap map)
	{
	
	    if(mr.states.size() > 30)
	       return mr;
	
	    ArrayList<Connection> connections = map.get(mr.states.get(mr.states.size()-1));
	    
	    //find out how many times it has been in this state
	    int total = 0;
        for(int i=0; i<connections.size(); i++)
        {
            total += connections.get(i).size();        
        }
        
        //find the highest probability for the next state
        double[] probabilites = new double[connections.size()];
        double max = 0;
        Connection maxConn = connections.get(0);
        
        for(int i=0; i<probabilites.length; i++)
        { 
            probabilites[i] = 1.0 * connections.get(i).size() / total;
            
            if(probabilites[i] > max)
            {
                max = probabilites[i];
                maxConn = connections.get(i);
            }
        }
        
        mr.states.add(maxConn.destinations.get(0));
        return mr;
	}
	
	
}
    /**
    *         MarkovRecurser
    * the object which is pushed through a markov chain and used to store the 
    * intermediate results       
    *        
    */
class MarkovRecurser
{
    //a list of the states this robot is likely to proceed through
    public ArrayList<Instance> states;                                
    
    //The probability that this location will be correct
    public double probability;
    
    //the data is really only usefull if you have been in each of the traversed states 
    //a few times before, otherwise you end up with 100% probability based on one case
    public int lowestOccupiedState;
    
    public MarkovRecurser(Instance i){
        states = new ArrayList<Instance>();
        states.add(i);
        probability = 1;
        lowestOccupiedState = 10000;
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/**
    * 
    * 
    *
    *        
    */
class Environment
{
	public int height;
	public int width;

	TargetList targets;
	
	Predictor predictor;
	
	//the location this robot thinks it should fire upon
	Location targetingLocation;
	//for painting where avoidance looks at
	ArrayList<Location> checkedPositions = new ArrayList<Location>();
    //for setting up a map of dangerous locations, where a zero indicates safe and an integer
    //indicates a degree of danger
	int[][][] hotspots;
	//the amount of pixels per index in the map
	int GRANULARITY = 10;
	//the amount of timesteps in the map
	//does not imply an ability to correctly predict this far, simply must always be larger than directions.size()
	int TIMESCALE = 42;
	//How many cells around the enemy to consider dangerous
	int AVOIDANCE_RADIUS = 3;
  
	public Environment(int w, int h, TargetList tl, Predictor p)
	{
		height = (h - 1);
		width = (w - 1);
		targets = tl;
		predictor = p;
		hotspots = new int[TIMESCALE][(int)w/GRANULARITY][(int)h/GRANULARITY];	
			
		for(int t=0; t<TIMESCALE; t++)
		{
			resetMap(t);
		}
  	}

	
    /**
    * Sets what positions in timestep t are dangerous
    * Based on where the Predictor says the enemy will be it this timestep
    */
    
	public void setupMap()
	{
	    //reset all of the positions checked for avoiding obstacles
		checkedPositions = new ArrayList<Location>();
	
		if(!targets.isEmpty())
		{
		    //what location to fire upon to intersect the enemies path 
			Location current = targets.getSelf().getLastLocation();
			targetingLocation = predictor.getFiringPosition(3, current, targets.getTarget());
		
		    //loop through each timestep and set where the enemy will be
			for(int t = 0; t < TIMESCALE; t++)
			{   
            
                resetMap(t);
                
			    //the enemies position at time = t
				Location predictedLoc = predictor.predictFutureLocation(t, targets.getTarget());


        		///////////set all of the Dangerous positions of the map
        		int x = (int) (predictedLoc.getX()/GRANULARITY);
        	    int y = (int) (predictedLoc.getY()/GRANULARITY);
        		
        		
        		
        		for(int i=0; i<AVOIDANCE_RADIUS; i++)
                { 
                  
                    //Each line represents a direction away from the position of the enemy
                    //i represents how far in that direction to go
                    if(y < (height/GRANULARITY) - i)
                        hotspots[t][x][y+i] += 1;
                    if(y > 0 +i)
                        hotspots[t][x][y-i] += 1;
                    if(x < (width/GRANULARITY) - i)
                        hotspots[t][x+i][y] += 1;
                    if((y < (height/GRANULARITY) -i) && (x < (width/GRANULARITY) - i))
                        hotspots[t][x+i][y+i] += 1;
                    if((y > 0 +i) && (x < (width/GRANULARITY) - i))
                        hotspots[t][x+i][y-i] += 1;
                    if((x > 0 +i))
                        hotspots[t][x-i][y] += 1; 
                    if((y < (height/GRANULARITY) -i)  &&  (x > 0 +i))
                        hotspots[t][x-i][y+i] += 1;
                    if((y > 0 +i) &&  (x > 0 +i))
                        hotspots[t][x-i][y-i] += 1;
        		}
			}
		}
	}
	 
	//set up so that the edges of the map are dangerous and all other locations are zero
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
	
    // See if this location of the hotspot map[t][x][y] contains a non-zero		
	public int checkMap(int t, double x, double y)
	{
	
		x = Math.max(0, Math.min(x, width));
		y = Math.max(0, Math.min(y, height));
	   
		checkedPositions.add(new Location(x,y));
		return hotspots[t][(int)x/GRANULARITY][(int)y/GRANULARITY];
	}
		
	public int getTimescale(){
    	return TIMESCALE;
    }
    public int getWidth(){
        return width;
    }
    public int getHeight(){
        return height;
    }    
    
    
    
    //onPaint only allows a few hundred paint events 
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
		if(targetingLocation != null)
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
		x = Math.max(0, Math.min(x, width));
		y = Math.max(0, Math.min(y, height));
		
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

	public void logSelf(long time, double energy, double heading, double velocity, double radar, double gun, Location loc)
	{
		tlist.get(0).logSelf(time, energy, heading, velocity, radar, gun, loc);
	}		
				
	//return the first target seen for now
	public  Target getTarget()
	{
		if(isEmpty())
			return null;
		
        double min = 20000;
        int minIndex = 1;
        
        for(int i=1; i<tlist.size(); i++){
        
            long timeSinceLastSeen = tlist.get(0).getLastInstance().getTime() - tlist.get(i).getLastInstance().getTime();
            if(tlist.get(i).getLastInstance().getDistance() < min  && timeSinceLastSeen < 20){
            
                min = tlist.get(i).getLastInstance().getDistance();
                minIndex = i;
            }
        }
        return tlist.get(minIndex);

	}
	
	public Target getSelf()
	{
		return tlist.get(0);
	}
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Target
{
    private String name;
	private LinkedList<Instance> ilog;
    public MarkovMap mmap;
	
	public Target(String n)
	{
		name = n;
		ilog = new LinkedList<Instance>();
        mmap = new MarkovMap();
	}
	
	public void logEnemy(ScannedRobotEvent e, Location loc)
	{   
	    //log the robot in the Instance Log
	    double angularMomentum = 0;
	    if(ilog.size() >1)
	    {
	        Instance last = getLastInstance();
            angularMomentum = e.getHeading() - last.getHeading();
	    }
	    
        ilog.add(new Instance(e, loc, angularMomentum));
        
        //log the robot in the markov chain
        if(ilog.size() > 2)
        {
            mmap.put(getInstanceBeforeLast(), getLastInstance());
            
            System.out.println("total states: " + mmap.map.size());
            System.out.println(mmap.get(getInstanceBeforeLast()).get(0));
        }
		
	}
	
	public void logSelf(long time, double energy, double heading, double velocity, double radar, double gun, Location loc)
	{
		Instance temp = new Instance(time, energy, heading, velocity, radar, gun, loc);
		ilog.add(temp);
	}
	
	public String getName()
	{
		return name;
	}
	
	public Instance getLastInstance()
	{
		return ilog.get(ilog.size()-1);
	}
	
	public Instance getInstanceBeforeLast()
	{
	    return ilog.get(ilog.size() - 2);
	}
	
	public Location getLastLocation()
	{
		return getLastInstance().getLocation();
	}
	
	public boolean isEmpty()
	{
	    return ilog.size() < 1;
	}
	

}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//container for storing information on scanned robots
class Instance
{
	private double heading;
	private double angularMomentum;
	private double energy;
	private double distance;
	private double velocity;
	private double radar;
	private double gun;
	private long time;
	private Location loc;
		
	public Instance(ScannedRobotEvent e , Location l, double aM)
	{
		heading = e.getHeading();
		angularMomentum = aM;
		energy = e.getEnergy();
		distance = e.getDistance();
		velocity = e.getVelocity();
		time = e.getTime();
		loc = l;
		radar = 0;
		gun = 0;
				
	}
	
	public Instance(long t, double e, double h, double v, double r, double g, Location l)
	{
		time = t;
		heading = h;
		energy = e;
		velocity = v;
		loc = l;
		distance = 0;
		angularMomentum = 0;
   		radar = r;
    	gun = g;		
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
	public double getRadarHeading(){
		return radar;
	}
	public double getGunHeading(){
		return gun;
	}
	public double getAngularMomentum(){
	   return angularMomentum;
	}
  
	public String toString()
	{
		return "Instance at time: "+ time + "\n" + 
		"heading: "+ heading + "\n" +
		"energy: "+ energy + "\n" +
		"velocity: "+ velocity + "\n" +
		"distance: "+ distance + "\n" +
		"radar: "+ radar + "\n" +
		"gun: "+ gun + "\n" +
		"x: "+ loc.getX() + "\n" +
		"y: " + loc.getY() + "\n" +
		"-" + "\n";
	}

	//if this = 4, angular momentum is regarded in intervals of .25
    int RECIPROCOL_GRANULARITY_AM = 4;
     
    public boolean equals(Object o)
    {
        int vDiff = (int)(velocity -((Instance)o).velocity);
        if(vDiff == 0)
        {
            return ((int)(angularMomentum * RECIPROCOL_GRANULARITY_AM) ==  (int)(((Instance)o).angularMomentum * RECIPROCOL_GRANULARITY_AM) );
        }
        return false;
    }
    
    
   public int hashCode()
    {
        return ((int)velocity) +  (((int)(angularMomentum * RECIPROCOL_GRANULARITY_AM)) * 10);
    } 

	
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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


class MarkovMap
{
	public HashMap<Instance, ArrayList<Connection>> map; 

    public MarkovMap()
    {
		map = new HashMap<Instance, ArrayList<Connection>>();
    }
    
    public ArrayList<Connection> get(Instance key)
    {
        return map.get(key);
    }
    
    //updates the MarkovModel to include or increment this connection 
    public void put(Instance origin, Instance destination)
    {
        
        Connection newConn = new Connection(origin, destination);
        ArrayList<Connection> connections = new ArrayList<Connection>();
        
        //if the robot has already been seen in this state
        if(map.containsKey(origin))
        {
            connections = map.get(origin);
            int index = connections.indexOf(newConn);
        
            //if this connection already exists
            if(index != -1)
            {
                //add this occourance of the connection
                connections.get(index).add(newConn);    
            }
            
            //otherwise add a new connection
            else
            {
                connections.add(newConn);
            }
        }
        
        //if this is a new state for the robot
        else
        {
            connections.add(newConn);
            map.put(origin, connections);
        }
    
    }

}

// A connection is a change from one state to another
//i.e. moving from v=0 to v=1;
class Connection
{
    //each connection remembers the entire state of the robot each time 
    //it's velocity was this and it's angularMomentum was this
    public ArrayList<Instance> origins;
    //the state of the robot after the change
    public ArrayList<Instance> destinations;
    
    

    public Connection(Instance o, Instance d)
    {
        origins = new ArrayList<Instance>();
        origins.add(o);
        destinations = new ArrayList<Instance>();
        destinations.add(d);
    }
    
    public boolean equals(Object o)
    {
        //if tbe origin and destination instance of one connection are both equal to their corresponding instance in the second connection
        return (origins.get(0).equals( ((Connection)o).origins.get(0) )) && (destinations.get(0).equals( ((Connection)o).origins.get(0) ));
    }
    
    public void add(Connection c)
    {
        origins.add(c.origins.get(0));
        destinations.add(c.destinations.get(0));
    }
    
    public int size()
    {
        return origins.size();
    }
    
    public String toString()
    {
        return 
        "Connection-------" +
        "\norigins: " + size() +
        "\n origin[0]:" +
        "\n velocity: "+
        origins.get(0).getVelocity() +
        "\n angularMomentum: "+
        origins.get(0).getAngularMomentum() +
        "\ndestination[0]:" +
        "\n velocity: "+
        destinations.get(0).getVelocity() +
        "\n angularMomentum: "+
        destinations.get(0).getAngularMomentum() +
        "\n" ;
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
	
	
			
	////////////////////////////////////////////////////////////Debug class/////////
  

public class Debug
{
    boolean debugOn;

    public Debug()
    {debugOn = false;}
    
    public debugOn()
    {
        debugOn = true;
    }
    
    public debugOff()
    {
        debugOn = false;
    }

}  		
			
////////////////////////////////////////////////////////////////////////////////

class MarkovNode implements Comparable
{
    public ArrayList<Connection> outgoing;
    public ArrayList<Connection> incoming;

    public double angularMomentum;
    public int velocity;
    
    public int visitCount;
    
    public MarkovNode(int h, double am)
    {

        //make angularMomentum be a multiple of .25
        angularMomentum = Math.rint(am*4) / 4;
        velocity = v;
        
        visitCount = 0;
        
        outgoing = new ArrayList<Connection>();
        incoming = new ArrayList<Connection>();    
    }
}			

	

			
			******/
			
			
			
			




    //Notes

    //Order of game actions ////////////////////////////////////////////////  
       	
  	//Paint
  	//execute code until action
  	//time++
  	//bullets move, collisions

	  //acceleration -> velocity -> distance  	
	  //perform scans
	  //Robots resumed to take new actions
	  //process event queue








