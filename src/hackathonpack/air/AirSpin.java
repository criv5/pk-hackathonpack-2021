package hackathonpack.air;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;

import hackathonpack.UtilityMethods;

public class AirSpin extends AirAbility implements AddonAbility, ComboAbility {
	
	private boolean isEnabled;
	private long cooldown;
	private int range;
	
	private HashMap<LivingEntity, AirSpinBlast> affectedEntities;
	private ArrayList<LivingEntity> clearList;
	
	public AirSpin(Player player) {
		super(player);

		if (bPlayer.isOnCooldown(this)) {
			return;
		}
		
		this.isEnabled = ConfigManager.getConfig().getBoolean("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpin.Enable");
		if (!isEnabled)
			return;

		setFields();
		if (!setAffectedEntities()) {
			return;
		}

		bPlayer.addCooldown(this);
		start();
	}

	public void setFields() {
		this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpin.Cooldown");
		this.range = ConfigManager.getConfig().getInt("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpin.Range");
		affectedEntities = new HashMap<LivingEntity, AirSpinBlast>(0);
		clearList = new ArrayList<LivingEntity>(0);
	}
	
	@Override
	public void progress() {

		if (affectedEntities.isEmpty()) {
			remove();
			return;
		}
		
		for (LivingEntity e : affectedEntities.keySet()) {
			if (!e.isDead() && !affectedEntities.get(e).isReached()) {
				affectedEntities.get(e).update();
				affectedEntities.get(e).show();
			} else {
				clearList.add(e);
			}
		}
		
		for (LivingEntity e : clearList) {
			affectedEntities.remove(e);
		}
		clearList.clear();
	}

	public boolean setAffectedEntities() {
		for (Entity e : GeneralMethods.getEntitiesAroundPoint(player.getLocation(), range)) {
			if (e instanceof LivingEntity && e.getUniqueId() != player.getUniqueId()) {
				Vector dir = player.getLocation().getDirection();
	            Vector otherVec = e.getLocation().toVector().subtract(player.getLocation().toVector());
	            double angle = Math.acos( dir.dot(otherVec)  /  (dir.length() * otherVec.length()) );
	            angle = Math.toDegrees (angle);
	            if(angle < 60) {
	            	affectedEntities.put((LivingEntity) e, new AirSpinBlast(player, (LivingEntity) e));
	            }
			}
		}
		return !affectedEntities.isEmpty();
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
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
	public String getName() {
		return "AirSpin";
	}
	
	@Override
	public String getDescription() {
		return "Moves the air around your enemies and makes them spin!";
	}
	
	@Override
	public String getInstructions() {
		return "AirBlast (Hold Sneak) -> AirSuction (Release Sneak) -> AirSuction (Tap Sneak)";
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
	public Object createNewComboInstance(Player player) {
		return new AirSpin(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		ArrayList<AbilityInformation> combination = new ArrayList<>();
		combination.add(new AbilityInformation("AirBlast", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("AirSuction", ClickType.SHIFT_UP));
		combination.add(new AbilityInformation("AirSuction", ClickType.SHIFT_DOWN));

		return combination;
	}
	
	@Override
	public void load() {
		ProjectKorra.log.info("Succesfully enabled " + getName() + " by " + getAuthor());
		
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpin.Enable", true);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpin.Cooldown", 7000);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Air.AirSpin.Range", 12);
		ConfigManager.defaultConfig.save();
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
		super.remove();
	}

}
