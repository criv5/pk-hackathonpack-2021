package hackathonpack.air;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;

public class AirSpinBlast {

//	private Player source;
	private LivingEntity target;
	
	private Location blastLocation;
	private Vector blastDirection;
	private double blastSpeed;
	
	private boolean isReached;
	
	public AirSpinBlast(Player source, LivingEntity destination) {
		
		AirAbility.playAirbendingSound(source.getLocation());
		
//		this.source = source;
		this.target = destination;
		
		blastLocation = source.getLocation().clone().add(0, 1, 0);
		blastDirection = target.getLocation().add(0, 1, 0).toVector().subtract(source.getLocation().toVector()).normalize();
		blastSpeed = 0.05;
		
		isReached = false;
	}
	
	public void update() {
		Vector untouchVector = blastDirection.clone().multiply(blastSpeed);
		Location destinationLocation = target.getLocation().clone().add(0, 1, 0);
		Vector desiredVector = destinationLocation.toVector().subtract(blastLocation.clone().toVector());
		Vector steeringVector = desiredVector.subtract(untouchVector).multiply(0.75);
		blastDirection = blastDirection.add(steeringVector);
		
		blastLocation.add(blastDirection.clone().multiply(blastSpeed));
		if (!blastLocation.getBlock().isEmpty())
			isReached = true;
		
		for (Entity e : GeneralMethods.getEntitiesAroundPoint(blastLocation, 2)) {
			if (e.equals(target)) {
				isReached = true;
				spin();
			}
		}

	}
	
	public void show() {
		AirAbility.playAirbendingParticles(blastLocation, ConfigManager.getConfig().getInt("Abilities.Air.AirBlast.Particles"), 0.275F, 0.275F, 0.275F);
	}
	
	public void spin() {
		Vector vel = target.getVelocity();
		target.teleport(target.getLocation().setDirection(target.getLocation().getDirection().multiply(-1)));
		target.setVelocity(vel.multiply(-1));
		particles(target);
	}
	
	public void particles(LivingEntity entity) {
		double r = 0;
		double tmp = 0.05;
		double angle = 10;
		angle = Math.toRadians(angle);
		Location loc = entity.getLocation();
		loc.setY(loc.getY() - 1);
		while (r >= 0) {
			if (r >= 2) {
				tmp = -tmp;
				r = 2;
			}
			loc.setY(loc.getY() + 0.05);
			loc.setX(loc.getX() + r * Math.cos(angle));
			loc.setZ(loc.getZ() + r * Math.sin(angle));
			AirAbility.getAirbendingParticles().display(loc, 0, 0, 0, 0, 3);
			angle += 10;
			r += tmp;
		}
	}
	
	public boolean isReached() {
		return isReached;
	}
	
}
