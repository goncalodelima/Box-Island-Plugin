package pt.gongas.box.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pt.gongas.box.model.box.Box;
import pt.gongas.box.model.box.service.BoxFoundationService;

import java.util.UUID;

public class PermissionListener implements Listener {

    private final BoxFoundationService boxService;

    public PermissionListener(BoxFoundationService boxService) {
        this.boxService = boxService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        Player damagedPlayer = (event.getEntity() instanceof Player victim) ? victim : null;
        Player damagerPlayer = (event.getDamager() instanceof Player attacker) ? attacker : null;

        boolean isDamagedPlayer = damagedPlayer != null;
        boolean isDamagerPlayer = damagerPlayer != null;

        if ((isDamagedPlayer && isInsideForeignBox(damagedPlayer))
                || (isDamagerPlayer && isInsideForeignBox(damagerPlayer))) {
            event.setCancelled(true);
        } else if (isDamagedPlayer && isDamagerPlayer && isInsideOwnBox(damagedPlayer) && isInsideOwnBox(damagerPlayer)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (isInsideForeignBox(player)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if (isInsideForeignBox(player)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (isInsideForeignBox(player)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {

        if (event.getEntity() instanceof Player player) {

            if (isInsideForeignBox(player)) {
                event.setCancelled(true);
            }

        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        if (isInsideForeignBox(player)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {

        if (event.getEntity() instanceof Player player) {

            if (isInsideForeignBox(player)) {
                System.out.println("entitydamageevent");
            }

        }

    }

    private boolean isInsideOwnBox(Player player) {

        try {
            UUID uuid = UUID.fromString(player.getWorld().getName());
            Box box = boxService.get(uuid);
            return box != null && box.isOwnerOrMember(player.getUniqueId());
        } catch (IllegalArgumentException ignored) {
            return false;
        }

    }

    private boolean isInsideForeignBox(Player player) {
        return !isInsideOwnBox(player);
    }

}
