package hackathonpack.air;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;

import hackathonpack.UtilityMethods;

public class AirPressureLow extends AirAbility implements AddonAbility, ComboAbility {

	private boolean isEnabled;
	private long cooldown;
	private long duration;
	private int particlePerTick;
	private double particleSpawnAreaRadius;
	private Location spawnLocation;
	
	private ArrayList<Location> particles;
	private ArrayList<Vector> directions;
	private ArrayList<Vector> velocities;
	
	private ArrayList<Entity> affectedEntities;
	
	public AirPressureLow(Player player) {
		super(player);
		
		if (!bPlayer.canBendIgnoreBinds(this)) {
			return;
		}
		
		if (CoreAbility.hasAbility(player, AirPressureLow.class)) {
			return;
		}
		
		this.isEnabled = ConfigManager.getConfig().getBoolean("ExtraAbilities.Hiro3.HackathonPack.Air.AirPressure.Low.Enable");
		if (!isEnabled)
			return;
		
		setField();
		start();
	}

	public void setField() {
		this.particles = new ArrayList<Location>();
		this.directions = new ArrayList<Vector>();
		this.velocities = new ArrayList<Vector>();
		this.affectedEntities = new ArrayList<Entity>();
		
		this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Air.AirPressure.Low.Cooldown");
		this.duration = 1000;
		this.particlePerTick = ConfigManager.getConfig().getInt("ExtraAbilities.Hiro3.HackathonPack.Air.AirPressure.Low.ParticlePerTick");
		this.particleSpawnAreaRadius = 8;
		this.spawnLocation = player.getLocation();
	}
	
	@Override
	public void progress() {
		
		if (System.currentTimeMillis() > getStartTime() + this.duration) {
			bPlayer.addCooldown(this);
			remove();
			return;
		}
		
		for (int i = 0; i < this.particlePerTick; i++) {
			double angle = Math.toRadians(Math.random() * 360);
			double size = Math.random() * this.particleSpawnAreaRadius + 0.001;
			Location loc = this.spawnLocation.clone().add(Math.cos(angle) * size, 0, Math.sin(angle) * size);
			this.particles.add(loc);
			Vector vec = this.spawnLocation.toVector().subtract(loc.toVector()).setY(0);
			double power = 0.1;//0.1 * vec.length() / this.particleSpawnAreaRadius;
			this.directions.add(vec.multiply(power));
			this.velocities.add(new Vector(0, 1, 0).multiply(0.2)); //0.2
		}
		
		this.affectedEntities.clear();
		for (int i = this.particles.size() - 1; i >= 0; i--) {
			Location loc = this.particles.get(i);
			player.getWorld().spawnParticle(Particle.SPELL, loc, 0);
			for (Entity e : GeneralMethods.getEntitiesAroundPoint(loc, 2)) {
				if (!this.affectedEntities.contains(e) && !e.getUniqueId().equals(player.getUniqueId())) {
					GeneralMethods.setVelocity(e, this.velocities.get(i).clone().add(this.directions.get(i)).multiply(3));
					this.affectedEntities.add(e);
				}
			}
			loc.add(this.velocities.get(i));
			loc.add(this.directions.get(i));
			this.directions.get(i).multiply(0.88);
			if (GeneralMethods.isSolid(loc.getBlock()) || loc.getY() - this.spawnLocation.getY() > 7) {
				this.particles.remove(i);
				this.directions.remove(i);
				this.velocities.remove(i);
			}
		}
		
		if (Math.random() < 0.1) {
			AirAbility.playAirbendingSound(this.spawnLocation);
		}
		
	}
	
	@Override
	public long getCooldown() {
		return this.cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}

	@Override
	public String getName() {
		return "AirPressureLow";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}
	
	@Override
	public Object createNewComboInstance(Player arg0) {
		return new AirPressureLow(arg0);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		ArrayList<AbilityInformation> combination = new ArrayList<>();
		combination.add(new AbilityInformation("AirSpout", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("AirShield", ClickType.LEFT_CLICK));

		return combination;
	}

	@Override
	public String getDescription() {
		return "Pull entities towards you and push them up by creating low pressure air field around you.";
	}
	
	@Override
	public String getInstructions() {
		return "AirSpout (Hold Sneak) -> AirShield (Left Click)";
	}
	
	@Override
	public String getAuthor() {
		return "Hiro3";
	}

	@Override
	public String getVersion() {
		return UtilityMethods.getVersion();
	}

	@Override
	public void remove() {
		super.remove();
		if (this.player != null) {
			new BukkitRunnable() {

				@Override
				public void run() {
					if (particles.size() <= 0) {
						this.cancel();
						return;
					}
					
					for (int i = particles.size() - 1; i >= 0; i--) {
						Location loc = particles.get(i);
						player.getWorld().spawnParticle(Particle.SPELL, loc, 0);
//						if (spawnLocation.getY() - loc.getY() > 3) {
//							velocities.get(i).add(directions.get(i));
//						}
						loc.add(velocities.get(i));
						loc.add(directions.get(i));
						directions.get(i).multiply(0.88);
						if (GeneralMethods.isSolid(loc.getBlock()) || loc.getY() - spawnLocation.getY() > 7) {
							particles.remove(i);
							directions.remove(i);
							velocities.remove(i);
						}
					}
				}
				
			}.runTaskTimer(ProjectKorra.plugin, 0, 1);
		}
	}
	
	@Override
	public void load() {
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirPressure.Low.Enable", true);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirPressure.Low.Cooldown", 5000);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirPressure.Low.ParticlePerTick", 2);
		ConfigManager.defaultConfig.save();
		
		ProjectKorra.log.info("Succesfully enabled " + getName() + " by " + getAuthor());
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
		super.remove();
	}

}