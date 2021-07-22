package hackathonpack.air;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;

import hackathonpack.UtilityMethods;

public class AirSpray extends AirAbility implements AddonAbility, ComboAbility {
	
	private Location tmp;
	
	private boolean isEnabled;
	private long cooldown;
	private int tick;
	private int maximumTick;
	private ArrayList<Boid> boids;
	private int amount;
	private double damage;
	private double range;
	
	private double alignmentPower;
	private double seperationPower;
	private double cohesionPower;
	private double playerDirectionPower;
	private double knockbackPower;
	
	public AirSpray(Player player) {
		super(player);
		
		if (!bPlayer.canBendIgnoreBinds(this))
			return;
		
		this.isEnabled = ConfigManager.getConfig().getBoolean("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Enable");
		if (!isEnabled)
			return;
		
		if (hasAbility(player, AirSpray.class))
			return;
		
		if (player.getLocation().getBlock().isLiquid())
			return;
		
		setField();
		start();
	}
	
	public void setField() {
		this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Cooldown");
		this.tick = 1;
		this.boids = new ArrayList<Boid>();
		this.maximumTick = (int) (ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Duration") / 50);
		this.range = ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Range");
		this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Damage");
		this.amount = ConfigManager.getConfig().getInt("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.ParticlePerTick");
		this.setAlignmentPower(ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.AlignmentPower"));
		this.setSeperationPower(ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.SeperationPower"));
		this.setCohesionPower(ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.CohesionPower"));
		this.setPlayerDirectionPower(ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.PlayerDirectionPower"));
		this.setKnockbackPower(ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.KnockbackPower"));
		
		PotionEffect pe = new PotionEffect(PotionEffectType.SLOW_DIGGING, maximumTick, 100);
		player.addPotionEffect(pe);
		
		if (player.getMainHand().equals(MainHand.RIGHT)) {
			tmp = getRightSide(player.getLocation().add(0, 1, 0), 0.2);
		} else {
			tmp = getLeftSide(player.getLocation().add(0, 1, 0), 0.2);
		}
		
		for (int i = 0; i < this.amount; i++) {
			boids.add(new Boid(player, this, tmp.clone().add(Math.random() * 0.5 - 0.25, Math.random() * 0.5 - 0.25, Math.random() * 0.5 - 0.25), player.getLocation().getDirection()
					, this.damage, this.range));
		}
	}

	@Override
	public void progress() {
		if ((bPlayer.getBoundAbilityName().equalsIgnoreCase(null) || !bPlayer.getBoundAbilityName().equalsIgnoreCase("AirBurst")) 
				&& bPlayer.canBendIgnoreBinds(CoreAbility.getAbility("AirSpray"))) {
			setTick(getMaximumTick());
		}
		
		if (boids.isEmpty()) {
			remove();
			if (tick <= maximumTick) {
				bPlayer.addCooldown(this);
				player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
			}
			return;
		}
		
		if (tick <= maximumTick) {
			if (tick == maximumTick) {
				bPlayer.addCooldown(this);
				player.removePotionEffect(PotionEffectType.SLOW_DIGGING);
			}
			
			if (!player.getLocation().getBlock().isLiquid()) {
				if (tick % 5 == 0) {
					AirAbility.playAirbendingSound(player.getLocation());
				}
				
				for (int i = 0; i < this.amount; i++) {
					if (player.getMainHand().equals(MainHand.RIGHT)) {
						tmp = getRightSide(player.getLocation().add(0, 1, 0), 0.2);
					} else {
						tmp = getLeftSide(player.getLocation().add(0, 1, 0), 0.2);
					}
					boids.add(new Boid(player, this, tmp.clone().add(Math.random() * 0.5 - 0.25, Math.random() * 0.5 - 0.25, Math.random() * 0.5 - 0.25), player.getLocation().getDirection()
							, this.damage, this.range));
				}
			}
		}
		
		for (Boid b : boids) {
			b.update();
			b.show();
		}
		
		for (int i = boids.size()-1; i >= 0; i--) {
			if (boids.get(i).getTicksLived() > this.maximumTick)
				boids.remove(boids.get(i));
		}
		
		tick++;
	}
	
	public Location getRightSide(Location location, double distance) {
		Vector dir = player.getLocation().getDirection();
		Vector rightTmpVec = new Vector(-dir.getZ(), 0, +dir.getX());
		rightTmpVec.normalize();
		rightTmpVec.multiply(distance);
		return location.clone().add(rightTmpVec);
	}
	
	public Location getLeftSide(Location location, double distance) {
		Vector dir = player.getLocation().getDirection();
		Vector leftTmpVec = new Vector(+dir.getZ(), 0, -dir.getX());
		leftTmpVec.normalize();
		leftTmpVec.multiply(distance);
		return location.clone().add(leftTmpVec);
	}
	
	public ArrayList<Boid> getBoids() {
		return this.boids;
	}
	
	public int getMaximumTick() {
		return this.maximumTick;
	}
	
	public void setTick(int tick) {
		this.tick = tick;
	}
	
	public int getTick() {
		return this.tick;
	}
	
	public double getAlignmentPower() {
		return alignmentPower;
	}

	public void setAlignmentPower(double alignmentPower) {
		this.alignmentPower = alignmentPower;
	}

	public double getSeperationPower() {
		return seperationPower;
	}

	public void setSeperationPower(double seperationPower) {
		this.seperationPower = seperationPower;
	}

	public double getCohesionPower() {
		return cohesionPower;
	}

	public void setCohesionPower(double cohesionPower) {
		this.cohesionPower = cohesionPower;
	}

	public double getPlayerDirectionPower() {
		return playerDirectionPower;
	}

	public void setPlayerDirectionPower(double playerDirectionPower) {
		this.playerDirectionPower = playerDirectionPower;
	}

	public double getKnockbackPower() {
		return knockbackPower;
	}

	public void setKnockbackPower(double knockbackPower) {
		this.knockbackPower = knockbackPower;
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
		return "AirSpray";
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
		return new AirSpray(arg0);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		ArrayList<AbilityInformation> combination = new ArrayList<>();
		combination.add(new AbilityInformation("AirBlast", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("AirBurst", ClickType.LEFT_CLICK));

		return combination;
	}
	
	@Override
	public String getDescription() {
		return "Release a spray of air that can bounce from terrain. \n(Knocback entities and deal damage to the ones around walls.)";
	}

	@Override
	public String getInstructions() {
		return "AirBlast (Hold Sneak) -> AirBurst (Left Click)";
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
	public void load() {		
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Enable", true);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Cooldown", 5000);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Duration", 3000);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Range", 18);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.Damage", 1);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.ParticlePerTick", 2);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.AlignmentPower", 0.2);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.SeperationPower", 1);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.CohesionPower", 0.1);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.PlayerDirectionPower", 0.1);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpray.KnockbackPower", 0.05);
		ConfigManager.defaultConfig.save();
		
		ProjectKorra.log.info("Succesfully enabled " + getName() + " by " + getAuthor());
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
		super.remove();
	}

}