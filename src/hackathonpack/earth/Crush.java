package hackathonpack.earth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.earthbending.EarthSmash;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;

import hackathonpack.UtilityMethods;

public class Crush extends EarthAbility implements ComboAbility, AddonAbility {
	
	private Location startLocation;
	
	private boolean isEnabled;
	private long cooldown;
	private double damage;
	private double detectRange;
	private int maxBfsLevel;
	private int maxBlockCount;
	private int currentBlockCount;
	
	private boolean isRegenOn;
	private long latency;
	
	private Block head;
	
	private HashMap<Location, Material> firstBlockMaterials;
	private HashMap<Location, BlockData> firstBlockDatas;
	private ArrayList<FallingBlock> fallingBlocks;
	
	private enum AbilityState {
			WAITING,
			STARTED,
			PROGRESSING;
	}
	
	private AbilityState state;
	
	public Crush(Player player) {
		super(player);
		
		if (bPlayer.isOnCooldown(this)) {
			return;
		}

		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			return;
		}
		
		this.isEnabled = ConfigManager.getConfig().getBoolean("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Enable");
		if (!isEnabled)
			return;
		
		setField();
		start();
	}

	public void setField() {
		this.startLocation = player.getLocation();
		
		this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Cooldown");
		this.damage = ConfigManager.getConfig().getDouble("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Damage");
		this.detectRange = ConfigManager.getConfig().getInt("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Range");
		this.maxBfsLevel = ConfigManager.getConfig().getInt("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.MaximumDepth");
		this.maxBlockCount = ConfigManager.getConfig().getInt("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.MaximumBlockAmount");
		this.currentBlockCount = 0;
		
		this.isRegenOn = ConfigManager.getConfig().getBoolean("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Revert.Enable");
		this.latency = ConfigManager.getConfig().getLong("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Revert.Latency");
		
		this.firstBlockMaterials = new HashMap<Location, Material>(0);
		this.firstBlockDatas = new HashMap<Location, BlockData>(0);
		this.fallingBlocks = new ArrayList<FallingBlock>(0);
		
		this.state = AbilityState.WAITING;
	}
	
	@Override
	public void progress() {
		
		if (state == AbilityState.WAITING) {
			if (!player.isSneaking()) {
				remove();
				return;
			}
			//ArrayList<Block> detectBlocks = getTargetBlocks(player, detectRange);
			//for (Block b : detectBlocks) {
				if (!checkCollision()) {
					Location tmpLoc = getTargetLocation(player, detectRange);
					Block b = tmpLoc.getBlock();
					this.startLocation = b.getLocation();
					if (isEarthbendable(b)) {
						this.head = b;
						state = AbilityState.STARTED;
						//break;
					} else {
						player.spawnParticle(Particle.SMOKE_NORMAL, tmpLoc, 0);
					}
				}
			//}
		} else if (state == AbilityState.STARTED) {
			bPlayer.addCooldown(this);
			ArrayList<Location> affectedBlockLocations = new ArrayList<Location>(0);
			affectedBlockLocations.add(head.getLocation());
			this.currentBlockCount++;
			breakBlocks(affectedBlockLocations);
			bfs(head);
			state = AbilityState.PROGRESSING;
		} else if (state == AbilityState.PROGRESSING) {
			for (int i = fallingBlocks.size()-1; i >= 0; i--) {
				if (!fallingBlocks.get(i).isDead()) {
					for (Entity e : GeneralMethods.getEntitiesAroundPoint(fallingBlocks.get(i).getLocation(), 2)) {
						if (e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId())) {
							DamageHandler.damageEntity(e, damage, this);
						}
					}
				} else {
					fallingBlocks.remove(i);
				}
			}
		}
		
	}
	
	public void bfs(Block head) {
		Queue<Block> q = new LinkedList<Block>();
		ArrayList<Location> visited = new ArrayList<Location>();
		
		visited.add(head.getLocation());
		q.add(head);
		
		new BukkitRunnable() {

			int bfsLevel = 1;
			int previousNeighborAmount = 1;
			
			@Override
			public void run() {
				if (q.isEmpty() || bfsLevel > maxBfsLevel || currentBlockCount > maxBlockCount) {
					this.cancel();
					regen();
					remove();
					return;
				}
				
				int previousBlockCount = currentBlockCount;
				for (int i = 0; i < previousNeighborAmount; i++) {
					if (q.isEmpty() || bfsLevel > maxBfsLevel || currentBlockCount > maxBlockCount) {
						this.cancel();
						regen();
						remove();
						return;
					}
					
					ArrayList<Location> neighbors = new ArrayList<Location>();
					Block b = q.poll();
					for (Block y : GeneralMethods.getBlocksAroundPoint(b.getLocation(), 1.5)) {
						if (isEarthbendable(y) && !visited.contains(y.getLocation()) && currentBlockCount <= maxBlockCount) {
							visited.add(y.getLocation());
							q.add(y);
							neighbors.add(y.getLocation());
							currentBlockCount++;
						}
					}
//					Block y = b.getRelative(BlockFace.UP);
//					if (isEarthbendable(y) && !visited.contains(y.getLocation()) && currentBlockCount <= maxBlockCount) {
//						visited.add(y.getLocation());
//						q.add(y);
//						neighbors.add(y.getLocation());
//						currentBlockCount++;
//					}
//					y = b.getRelative(BlockFace.DOWN);
//					if (isEarthbendable(y) && !visited.contains(y.getLocation()) && currentBlockCount <= maxBlockCount) {
//						visited.add(y.getLocation());
//						q.add(y);
//						neighbors.add(y.getLocation());
//						currentBlockCount++;
//					}
//					y = b.getRelative(BlockFace.NORTH);
//					if (isEarthbendable(y) && !visited.contains(y.getLocation()) && currentBlockCount <= maxBlockCount) {
//						visited.add(y.getLocation());
//						q.add(y);
//						neighbors.add(y.getLocation());
//						currentBlockCount++;
//					}
//					y = b.getRelative(BlockFace.SOUTH);
//					if (isEarthbendable(y) && !visited.contains(y.getLocation()) && currentBlockCount <= maxBlockCount) {
//						visited.add(y.getLocation());
//						q.add(y);
//						neighbors.add(y.getLocation());
//						currentBlockCount++;
//					}
//					y = b.getRelative(BlockFace.WEST);
//					if (isEarthbendable(y) && !visited.contains(y.getLocation()) && currentBlockCount <= maxBlockCount) {
//						visited.add(y.getLocation());
//						q.add(y);
//						neighbors.add(y.getLocation());
//						currentBlockCount++;
//					}
//					y = b.getRelative(BlockFace.EAST);
//					if (isEarthbendable(y) && !visited.contains(y.getLocation()) && currentBlockCount <= maxBlockCount) {
//						visited.add(y.getLocation());
//						q.add(y);
//						neighbors.add(y.getLocation());
//						currentBlockCount++;
//					}
					
					breakBlocks(neighbors);
				}
				previousNeighborAmount = currentBlockCount - previousBlockCount;
				bfsLevel++;
			}
			
		}.runTaskTimer(ProjectKorra.plugin, 0, 2);
	}
	
	@SuppressWarnings("deprecation")
	public void breakBlocks(ArrayList<Location> affectedBlockLocations) {
		Vector pDirection = player.getLocation().getDirection().clone();
		int flag = 0;
		if (pDirection.getY() <= -0.35) {
			pDirection.setY(pDirection.getY() * -1);
			flag = 2;
		}
		for (Location loc : affectedBlockLocations) {
			if (!firstBlockMaterials.containsKey(loc)) {
				this.firstBlockMaterials.put(loc, loc.getBlock().getType());
				this.firstBlockDatas.put(loc, loc.getBlock().getBlockData());
			}
			FallingBlock temp = player.getWorld().spawnFallingBlock(loc, loc.getBlock().getType(), loc.getBlock().getData());
			temp.setMetadata("CrushFallingBlock", new FixedMetadataValue(ProjectKorra.plugin, null));
			temp.setVelocity(rotateVectorAroundXZ(pDirection, Math.random()*60 - 20).multiply(0.5));
			temp.setVelocity(temp.getVelocity().add(rotateVectorAroundY(temp.getVelocity(), Math.random()*90*(1 + flag) - 45*(1 + flag))).multiply(0.5));
			//temp.setVelocity(temp.getVelocity().add(temp.getVelocity().clone().multiply(player.getVelocity().length())));
			if (flag != 0) {
				temp.setVelocity(temp.getVelocity().multiply(2));
			}
			temp.setDropItem(false);
			this.fallingBlocks.add(temp);
			
			loc.getBlock().setType(Material.AIR);
		}
		if (affectedBlockLocations.size() != 0)
			playEarthbendingSound(affectedBlockLocations.get(0));
	}
	
	public ArrayList<Block> getTargetBlocks(Player player, double range) {
		Vector direction = player.getLocation().getDirection().clone().multiply(0.1);
		Location loc = player.getEyeLocation().clone();
		Location startLoc = loc.clone();
		
		do {
			loc.add(direction);
		} while (startLoc.distance(loc) < range && !GeneralMethods.isSolid(loc.getBlock()));
		
		return (ArrayList<Block>) GeneralMethods.getBlocksAroundPoint(loc, 2);
	}
	
	public Location getTargetLocation(Player player, double range) {
		Vector direction = player.getLocation().getDirection().clone().multiply(0.1);
		Location loc = player.getEyeLocation().clone();
		Location startLoc = loc.clone();
		
		do {
			loc.add(direction);
		} while (startLoc.distance(loc) < range && !GeneralMethods.isSolid(loc.getBlock()));
		
		return loc;
	}
	
	public static Vector rotateVectorAroundY(Vector vector, double degrees) {
	    double rad = Math.toRadians(degrees);
	   
	    double currentX = vector.getX();
	    double currentZ = vector.getZ();
	   
	    double cosine = Math.cos(rad);
	    double sine = Math.sin(rad);
	   
	    return new Vector((cosine * currentX - sine * currentZ), vector.getY(), (sine * currentX + cosine * currentZ));
	}
	
	public static Vector rotateVectorAroundXZ(Vector vector, double degrees) {
		Vector rightTmpVec = new Vector(-vector.getZ(), 0, +vector.getX());
		
		double rad = Math.toRadians(degrees);
		Vector v1 = vector.clone().multiply(Math.cos(rad));
		Vector v2 = vector.clone().crossProduct(rightTmpVec);
		v2.multiply(Math.sin(rad));
		return v1.add(v2);
	}
	
	public boolean checkCollision() {
		for ( EarthSmash es : getAbilities(EarthSmash.class) ) {
			Location esLoc = es.getLocation();
			if (esLoc != null && esLoc.getWorld().equals(player.getWorld()) && es.getLocation().distance(this.getLocation()) <= this.getCollisionRadius()) {
				handleEarthSmash(es);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public long getCooldown() {
		return cooldown;
	}

	@Override
	public Location getLocation() {
//		if (head == null) {
//			return getTargetBlocks(player, detectRange).get(0).getLocation();
//		}
//		return head.getLocation();
		//return getTargetLocation(player, detectRange);
		return startLocation;
		//return player.getLocation();
	}
	
	@Override
	public double getCollisionRadius() {
		return 5;
	}
	
	public void handleEarthSmash(EarthSmash smash) {
		ArrayList<Block> blocks = (ArrayList<Block>) smash.getBlocks();
		ArrayList<Material> materials = new ArrayList<Material>();
		for (TempBlock tb : smash.getAffectedBlocks()) {
			materials.add(tb.getBlock().getType());
		}
		smash.remove();
		
		int i = 0;
		for (Block b : blocks) {
			if (materials.size() > i)
				b.setType(materials.get(i));
			i++;
		}
		
		this.isRegenOn = false;
		this.startLocation = blocks.get(0).getLocation();
		this.head = blocks.get(0);
		this.state = AbilityState.STARTED;
	}
	
	public ArrayList<FallingBlock> getFallingBlocks() {
		return this.fallingBlocks;
	}
	
	@Override
	public String getName() {
		return "Crush";
	}

	@Override
	public String getDescription() {
		return "Crush the obstacles you face! With this ability you can destroy earth blocks, EarthSmashes and EarthBlasts.";
	}

	@Override
	public String getInstructions() {
		return "Shockwave (Sneak) -> EarthSmash (Left Click)";
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
	public String getAuthor() {
		return "Hiro3";
	}

	@Override
	public String getVersion() {
		return UtilityMethods.getVersion();
	}

	@Override
	public Object createNewComboInstance(Player player) {
		return new Crush(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		ArrayList<AbilityInformation> combination = new ArrayList<>();
		combination.add(new AbilityInformation("Shockwave", ClickType.SHIFT_DOWN));
		combination.add(new AbilityInformation("EarthSmash", ClickType.LEFT_CLICK));

		return combination;
	}
	
	@Override
	public void load() {
		ProjectKorra.log.info("Succesfully enabled " + getName() + " by " + getAuthor());
		
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Enable", true);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Cooldown", 5000);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Damage", 3);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Range", 4);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.MaximumBlockAmount", 40);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.MaximumDepth", 30);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Revert.Enable", true);
		ConfigManager.getConfig().addDefault("ExtraAbilities.Hiro3.HackathonPack.Earth.Crush.Revert.Latency", 30);
		ConfigManager.defaultConfig.save();
	}

	@Override
	public void stop() {
		ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
		super.remove();
	}

	public void regen() {
		if (this.isRegenOn) {
//			for (FallingBlock fb : fallingBlocks) {
//				new BukkitRunnable() {
//	
//					@Override
//					public void run() {
//						if (!fb.isDead())
//							fb.remove();
//						else if (!firstBlockMaterials.containsKey(fb.getLocation())) {
//							fb.getLocation().getBlock().setType(Material.AIR);
//						}
//					}
//					
//				}.runTaskLater(ProjectKorra.plugin, latency * 20);
//			}
			
			new BukkitRunnable() {
	
				@Override
				public void run() {
					for (Location loc : firstBlockMaterials.keySet()) {
						loc.getBlock().setType(firstBlockMaterials.get(loc));
						loc.getBlock().setBlockData(firstBlockDatas.get(loc));
					}
				}
				
			}.runTaskLater(ProjectKorra.plugin, latency * 20); 
		}
	}
	
}
