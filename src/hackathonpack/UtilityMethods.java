package hackathonpack;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;

public class UtilityMethods {

	public static String getVersion () {
		return "HackathonPack v1.0";
	}
	
	public static Vector rotateVectorAroundY(Vector vector, double degrees) {
	    double rad = Math.toRadians(degrees);
	   
	    double currentX = vector.getX();
	    double currentZ = vector.getZ();
	   
	    double cosine = Math.cos(rad);
	    double sine = Math.sin(rad);
	   
	    return new Vector((cosine * currentX - sine * currentZ), vector.getY(), (sine * currentX + cosine * currentZ));
	}
	
	public static boolean isInLineOfSight(Player player, Location target) {
		Vector dir = player.getLocation().getDirection();
        Vector otherVec = target.clone().toVector().subtract(player.getLocation().toVector());
        double angle = Math.acos( dir.dot(otherVec)  /  (dir.length() * otherVec.length()) );
        angle = Math.toDegrees (angle);
        if(angle < 60) {
        	return true;
        }
        return false;
	}
	
	public static Vector rotate(Vector vector, Location yawPitchLocation) {
		double yaw = Math.toRadians(-yawPitchLocation.getYaw());
        double pitch = Math.toRadians(yawPitchLocation.getPitch());
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
	
	public static boolean checkBlocks(LivingEntity e) {
		ArrayList<Block> blocks = new ArrayList<Block>();
		Block block = e.getLocation().getBlock();
		blocks.add(block.getRelative(BlockFace.UP).getRelative(BlockFace.EAST));
		blocks.add(block.getRelative(BlockFace.UP).getRelative(BlockFace.WEST));
		blocks.add(block.getRelative(BlockFace.UP).getRelative(BlockFace.NORTH));
		blocks.add(block.getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH));
		blocks.add(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.EAST));
		blocks.add(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.WEST));
		blocks.add(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.NORTH));
		blocks.add(block.getRelative(BlockFace.UP).getRelative(BlockFace.UP).getRelative(BlockFace.SOUTH));
		for (Block b : blocks) {
			if (GeneralMethods.isSolid(b)) {
				return true;
			}
		}
		return false;
	}
	
}