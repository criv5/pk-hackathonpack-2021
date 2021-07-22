package hackathonpack;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.event.AbilityStartEvent;

import hackathonpack.air.AirSpray;

public class MainListener implements Listener {
	
	@EventHandler
	public void onAbilityStart(AbilityStartEvent event) {
		Player player = event.getAbility().getPlayer();
		BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

		if (event.isCancelled() || bPlayer == null)
			return;
		
		if (CoreAbility.hasAbility(player, AirSpray.class)) {
			AirSpray as = CoreAbility.getAbility(player, AirSpray.class);
			if (as.getTick() < as.getMaximumTick())
				event.setCancelled(true);
		}
	}
	
	@EventHandler
    public void onEntityChangeBlockEvent(EntityChangeBlockEvent e) {
        if(e.getEntityType().equals(EntityType.FALLING_BLOCK) && e.getEntity().hasMetadata("CrushFallingBlock")) {
            e.setCancelled(true);
        }
    }
	
}