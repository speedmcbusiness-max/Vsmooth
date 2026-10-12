package com.example.smoothplay;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

public class SmoothPlay extends JavaPlugin implements Listener {

    private boolean instantCrystal;
    private float crystalPower;
    private boolean crystalBreaksBlocks;
    private boolean blockResync;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instantCrystal = getConfig().getBoolean("instant-crystal-break", true);
        crystalPower = (float) getConfig().getDouble("crystal-explosion-power", 6.0);
        crystalBreaksBlocks = getConfig().getBoolean("crystal-breaks-blocks", true);
        blockResync = getConfig().getBoolean("block-resync", true);
        getServer().getPluginManager().registerEvents(this, this);
    }

    /** Crystal hit: hide it from the attacker immediately, remove it and explode in the same tick. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalHit(EntityDamageByEntityEvent e) {
        if (!instantCrystal || !(e.getEntity() instanceof EnderCrystal crystal)) return;

        Player attacker = resolvePlayer(e.getDamager());
        if (attacker == null) return;

        e.setCancelled(true);
        if (crystal.isDead()) return;

        Location loc = crystal.getLocation();
        attacker.hideEntity(this, crystal); // sends the destroy packet right now
        crystal.remove();
        loc.getWorld().createExplosion(loc, crystalPower, false, crystalBreaksBlocks, attacker);
    }

    /** Fixes ghost blocks after placing blocks, anchors, etc. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent e) {
        if (!blockResync) return;
        Player p = e.getPlayer();
        Block placed = e.getBlockPlaced();
        getServer().getScheduler().runTask(this, () ->
                p.sendBlockChange(placed.getLocation(), placed.getBlockData()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) {
        if (!blockResync || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block clicked = e.getClickedBlock();
        if (clicked == null) return;
        Player p = e.getPlayer();
        getServer().getScheduler().runTask(this, () ->
                p.sendBlockChange(clicked.getLocation(), clicked.getBlockData())); // e.g. respawn anchor charge state
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource s = proj.getShooter();
            if (s instanceof Player p) return p;
        }
        return null;
    }
}
