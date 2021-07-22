package hackathonpack.air;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.util.DamageHandler;

import hackathonpack.UtilityMethods;

public class Boid {

	private Player player;
	private AirSpray airSpray;
	private Location location;
	private Vector direction;
	private double lineOfSightRange;
	
	private double speed = 1;
	private double alignmentPower;
	private double seperationPower;
	private double cohesionPower;
	private double playerDirectionPower;
	private double knockbackPower;
	private double pushPower = 0.01;
	private double blockPushPower = 0.75;
	private double turnSpeed = 0.75;
	private Vector gravity = new Vector(0, -0.01, 0);
	private double range;
	private double damage;
	
	private int ticksLived;
	
	public Boid(Player player, AirSpray airSpray, Location loc, Vector dir, double damage, double range) {
		this.player = player;
		this.airSpray = airSpray;
		this.location = loc;
		this.direction = dir;
		this.lineOfSightRange = 2;
		this.ticksLived = 0;
		this.range = range;
		this.damage = damage;
		this.alignmentPower = airSpray.getAlignmentPower();
		this.seperationPower = airSpray.getSeperationPower();
		this.cohesionPower = airSpray.getCohesionPower();
		this.playerDirectionPower = airSpray.getPlayerDirectionPower();
		this.knockbackPower = airSpray.getKnockbackPower();
	}
	
	public void update() {
		if (this.range <= 0 || GeneralMethods.isSolid(this.location.getBlock()))
			this.ticksLived = airSpray.getMaximumTick();
		
		if (this.ticksLived > airSpray.getMaximumTick())
			return;
		this.ticksLived++;
		
		this.location.add(this.direction.normalize().multiply(this.speed));
		this.speed -= 0.01;
		this.range -= this.direction.length();
		
		Vector newDirection = this.direction.clone();
		newDirection.add(seperationVector());
		newDirection.add(alignmentVector());
		newDirection.add(cohesionVector().multiply(this.cohesionPower));
		newDirection.add(player.getLocation().getDirection().multiply(this.playerDirectionPower));
		Vector turnVector = newDirection.clone().subtract(this.direction).multiply(this.turnSpeed);
		this.direction.add(turnVector);
		this.direction.add(gravity);
		
		this.pushPower += 0.005;
	}
	
	public void show() {
		if (this.ticksLived > airSpray.getMaximumTick())
			return;
		
		AirAbility.playAirbendingParticles(this.location, 1, 0.075f, 0.075f, 0.075f);
		
		for (Entity e : GeneralMethods.getEntitiesAroundPoint(this.location, 1.5)) {
			if (e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId())) {
				if (UtilityMethods.checkBlocks((LivingEntity)e)) {
					DamageHandler.damageEntity(e, this.damage, airSpray);
				}
				e.setVelocity(e.getVelocity().add(alignmentVector().normalize().multiply(this.knockbackPower)));
			}
		}
		
	}
	
	public Vector seperationVector() {
		Vector seperationVector = new Vector(0, 0, 0);
		int flag ;
		
		for (AirSpray as : CoreAbility.getAbilities(AirSpray.class)) {
			if (as.getPlayer().getWorld().equals(player.getWorld())) {
				flag = 0;
				for (Boid b : as.getBoids()) {
					if (b.getLocation().distance(this.location) <= this.lineOfSightRange) {
						if (as.getPlayer().getUniqueId().equals(player.getUniqueId())) {
							seperationVector.add(calculatePushVector(b, this.seperationPower));
						} else {
							seperationVector.add(calculatePushVector(b, 25));
							flag = 1;
						}
					}
				}
				if (flag == 1)
					this.ticksLived += airSpray.getMaximumTick() / 3;
			}
		}
		
		flag = 0;
		for (Block b : GeneralMethods.getBlocksAroundPoint(this.location, this.lineOfSightRange - 0.5)) {
			if (GeneralMethods.isSolid(b) || b.isLiquid()) {
				seperationVector.add(calculatePushVector(b, 1));
				flag = 1;
			}
		}
		if (flag == 1)
			this.ticksLived += airSpray.getMaximumTick() / 6;
		
		return seperationVector;
	}
	
	public Vector alignmentVector() {
		Vector alignmentVector = new Vector(0, 0, 0);
		for (Boid b : airSpray.getBoids()) {
			if (b.getLocation().distance(this.location) <= this.lineOfSightRange) {
				alignmentVector.add(b.getDirection());
			}
		}
		alignmentVector.normalize().multiply(alignmentPower);
		return alignmentVector;
	}
	
	public Vector cohesionVector() {
		Vector cohesionVector = new Vector(0, 0, 0);
		Location sum = new Location(player.getWorld(),0, 0, 0);
		int count = 0;
		for (Boid b : airSpray.getBoids()) {
			if (b.getLocation().distance(this.location) <= this.lineOfSightRange) {
				sum.add(b.getLocation());
				count++;
			}
		}
		if (count != 0) {
			sum.setX(sum.getX() / count);
			sum.setY(sum.getY() / count);
			sum.setZ(sum.getZ() / count);
			Vector tmpVector = sum.clone().toVector().subtract(this.location.clone().toVector());
			if (tmpVector.length() != 0)
				cohesionVector = tmpVector.normalize().multiply(1);
		}
		
		return cohesionVector;
	}
	
	public int getTicksLived() {
		return this.ticksLived;
	}
	
	public Vector calculatePushVector(Boid b, double power) {
		double push = this.pushPower * (1/(b.getLocation().distanceSquared(this.getLocation()) + 0.1)) * power;
		return this.getLocation().clone().toVector().subtract(b.getLocation().clone().toVector()).multiply(push);
	}
	
	public Vector calculatePushVector(Block b, double power) {
		double push = this.blockPushPower * (1/(b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(this.getLocation()) + 0.1)) * power;
		return this.location.clone().toVector().subtract(b.getLocation().clone().toVector()).multiply(push);
	}
	
	public Location getLocation() {
		return this.location;
	}
	
    public Vector getDirection() {
    	return this.direction;
    }
}