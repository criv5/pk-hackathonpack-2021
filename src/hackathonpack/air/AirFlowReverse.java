package hackathonpack.air;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.noise.PerlinNoiseGenerator;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;

import hackathonpack.UtilityMethods;

public class AirFlowReverse extends AirAbility implements AddonAbility, ComboAbility {

	private boolean isEnabled;
	private long chargeTime;
	private long launchTime;
	private long duration;
	private long cooldown;
	private double range;
	private int particlePerTick;
	private double particleSpawnAreaSize;
	private Location flowLocation;
	private Vector flowDirection;
	
	private ArrayList<Location> particles;
	private ArrayList<Entity> affectedEntities;
	
	private enum AbilityState  {
		CHARGING,
		CHARGED,
		LAUNCHED
	}
	
	private AbilityState abilityState;
	
	public AirFlowReverse(Player player) {
		super(player);
		
		if (!bPlayer.canBendIgnoreBinds(this)) {
			return;
		}
		
		this.isEnabled = ConfigManager.getConfig().getBoolean("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.Enable");
		if (!isEnabled)
			return;
		
		setField();
		start();
	}

	public void setField() {
		this.particles = new ArrayList<Location>();
		this.affectedEntities = new ArrayList<Entity>();
		
		this.chargeTime = ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.ChargeTime");
		this.duration = ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.Duration");
		this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.Cooldown");
		this.range = ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.Range");
		this.particlePerTick = ConfigManager.getConfig().getInt("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.ParticlePerTick");
		this.particleSpawnAreaSize = ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.ParticleSpawnAreaSize");
		
		this.abilityState = AbilityState.CHARGING;
	}
	
	@Override
	public void progress() {
		//Controls that will remove the move goes here.
		//(Region protection check, ability range check, ability duration check etc.)
		if (abilityState == AbilityState.LAUNCHED && System.currentTimeMillis() > this.launchTime + this.duration) {
			remove();
			return;
		}
		
		if (abilityState == AbilityState.CHARGING) {
			if (!player.isSneaking()) {
				remove();
				return; 
			} else if (System.currentTimeMillis() > getStartTime() + chargeTime) {
				abilityState = AbilityState.CHARGED;
			} else {
				//Not charged particle codes
				if (Math.random() < 0.2)
				player.spawnParticle(Particle.SPELL, player.getEyeLocation().add(player.getLocation().getDirection().multiply(12)), 0);
			}
		} else if (abilityState == AbilityState.CHARGED) {
			if (!player.isSneaking()) {
				abilityState = AbilityState.LAUNCHED;
				this.launchTime = System.currentTimeMillis();
				this.flowLocation = player.getEyeLocation().add(player.getLocation().getDirection().multiply(12));
				this.flowDirection = player.getLocation().getDirection().multiply(-1);
				player.getWorld().playSound(this.flowLocation, Sound.ITEM_ELYTRA_FLYING, 0.05f, 1);
				bPlayer.addCooldown(this);
			} else {
				//Charged particle codes
				player.spawnParticle(Particle.SPELL, player.getEyeLocation().add(player.getLocation().getDirection().multiply(12)), 0, 0.5f, 0.5f, 0.5f);
			}
		} else if (abilityState == AbilityState.LAUNCHED) {
			for (int i = 0; i < this.particlePerTick; i++) {
				Location tmpLoc = this.flowLocation.clone();
				tmpLoc.add(rotate(new Vector(Math.random() * this.particleSpawnAreaSize - this.particleSpawnAreaSize / 2, 
						Math.random() * this.particleSpawnAreaSize - this.particleSpawnAreaSize / 2, 0)));
				this.particles.add(tmpLoc);
			}
			
			//Update & Show & Clear
			this.affectedEntities.clear();
			for (int i = this.particles.size() - 1; i >= 0; i--) {
				Location l = this.particles.get(i);
				player.getWorld().spawnParticle(Particle.SPELL, l, 0);
				
				Vector vel = this.flowDirection.clone().rotateAroundY(Math.toRadians(PerlinNoiseGenerator.getNoise(l.getX(), l.getY(), l.getZ(), 10, 0.3, 1) * 90));
				vel = vel.normalize();
				for (Entity e : GeneralMethods.getEntitiesAroundPoint(l, 2)) {
					if (!this.affectedEntities.contains(e)) {
						GeneralMethods.setVelocity(e, vel.clone().add(e.getVelocity()).multiply(0.5));
						this.affectedEntities.add(e);
					}
				}
				l.add(vel.clone().multiply(0.5));
				if (l.distance(this.flowLocation) > this.range || GeneralMethods.isSolid(l.getBlock())) {
					this.particles.remove(i);
				}
			}
			if (Math.random() < 0.05)
				player.getWorld().playSound(this.flowLocation, Sound.ITEM_ELYTRA_FLYING, 0.05f, 1);
		}
	}
	
	private Vector rotate(Vector vector) {
		double yaw = Math.toRadians(-this.flowLocation.getYaw());
        double pitch = Math.toRadians(this.flowLocation.getPitch());
        double oldX = vector.getX();
        double oldY = vector.getY();
        double oldZ = vector.getZ();
        vector.setY(oldY * Math.cos(pitch) - oldZ * Math.sin(pitch));
        vector.setZ(oldY * Math.sin(pitch) + oldZ * Math.cos(pitch));
        oldY = vector.getY();
        oldZ = vector.getZ();
        vector.setX(oldX * Math.cos(yaw) + oldZ * Math.sin(yaw));
        vector.setZ(-oldX * Math.sin(yaw) + oldZ * Math.cos(yaw));
        return vector;
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
		return "AirFlowReverse";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public Object createNewComboInstance(Player arg0) {
		return new AirFlowReverse(arg0);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		ArrayList<AbilityInformation> combination = new ArrayList<>();
		combination.add(new AbilityInformation("AirBurst", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("AirSuction", ClickType.LEFT_CLICK));

		return combination;
	}

	@Override
	public String getDescription() {
		return "Create a flow of air that can carry entities towards you.";
	}
	
	@Override
	public String getInstructions() {
		return "AirBurst (Hold Sneak) -> AirSuction (Left Click)";
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
					
					affectedEntities.clear();
					for (int i = particles.size() - 1; i >= 0; i--) {
						Location l = particles.get(i);
						player.getWorld().spawnParticle(Particle.SPELL, l, 0);
						
						Vector vel = flowDirection.clone().rotateAroundY(Math.toRadians(PerlinNoiseGenerator.getNoise(l.getX(), l.getY(), l.getZ(), 10, 0.3, 1) * 90));
						vel = vel.normalize();
						for (Entity e : GeneralMethods.getEntitiesAroundPoint(l, 2)) {
							if (!affectedEntities.contains(e)) {
								GeneralMethods.setVelocity(e, vel.clone().add(e.getVelocity()).multiply(0.5));
								affectedEntities.add(e);
							}
						}
						l.add(vel.clone().multiply(0.5));
						if (l.distance(flowLocation) > range || GeneralMethods.isSolid(l.getBlock())) {
							particles.remove(i);
						}
					}
				}
				
			}.runTaskTimer(ProjectKorra.plugin, 0, 1);
		}
	}
	
	@Override
	public void load() {
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.Enable", true);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.Cooldown", 5000);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.Duration", 10000);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.ChargeTime", 500);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.Range", 10);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.ParticlePerTick", 1);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirFlow.Reverse.ParticleSpawnAreaSize", 3);
		ConfigManager.defaultConfig.save();
		
		ProjectKorra.log.info("Succesfully enabled " + getName() + " by " + getAuthor());
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
		super.remove();
	}

}
