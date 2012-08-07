package bone008.bukkit.deathcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import bone008.bukkit.deathcontrol.config.CauseData.HandlingMethod;

public class DeathManager {

	private boolean valid = true;

	private final String plyName;
	private final Location deathLocation;
	private final List<StoredItemStack> keptItems;
	private final int keptExp;
	private final int droppedExp;
	private final HandlingMethod method;
	private final double cost;
	private final int timeoutOnQuit;

	public DeathManager(Player ply, List<StoredItemStack> keptItems, int keptExp, int droppedExp, HandlingMethod method, double cost, int timeoutOnQuit) {
		this.plyName = ply.getName();
		this.deathLocation = ply.getLocation();
		this.keptItems = keptItems;
		this.keptExp = keptExp;
		this.droppedExp = keptExp;
		this.method = method;
		this.cost = cost;
		this.timeoutOnQuit = timeoutOnQuit;
	}

	public boolean expire(boolean showMessage) {
		if (!valid)
			return false;

		// drops items
		for (StoredItemStack storedStack : keptItems)
			Utilities.dropItem(deathLocation, storedStack.itemStack, true);

		// drops experience orbs
		Utilities.dropExp(deathLocation, droppedExp, true);

		// sends notification to the player
		if (showMessage) {
			Player ply = Bukkit.getPlayerExact(plyName);
			MessageHelper.sendMessage(ply, ChatColor.DARK_RED + "Time is up.");
			MessageHelper.sendMessage(ply, ChatColor.DARK_RED + "Your items are dropped at your death location.");
			// logs to console
			DeathControl.instance.log(Level.FINE, "Timer for " + plyName + " expired! Items dropped.");
		}

		unregister();
		return true;
	}

	public void respawned() {
		if (!valid)
			return;
		if (method == HandlingMethod.AUTO) {
			if (restore())
				DeathControl.instance.log(Level.FINE, plyName + " respawned and got back their items.");
			unregister();
		}
	}

	public boolean commandIssued() {
		if (method == HandlingMethod.COMMAND && this.valid) {
			Player ply = Bukkit.getPlayerExact(plyName);
			if (restore()) {
				MessageHelper.sendMessage(ply, "You got your items back!");
				DeathControl.instance.log(Level.FINE, ply.getName() + " got back their items via command.");
				unregister();
			} else {

			}
			return true;
		}
		return false;
	}

	private boolean restore() {
		if (!valid)
			return false;

		final Player ply = Bukkit.getPlayerExact(plyName);

		if (!DeathControl.instance.config.allowCrossworld && !DeathControl.instance.hasPermission(ply, DeathControl.PERMISSION_CROSSWORLD) && !ply.getWorld().equals(deathLocation.getWorld())) {
			MessageHelper.sendMessage(ply, ChatColor.DARK_RED + "You are in a different world, your items were dropped!");
			expire(false);
			return false;
		}

		boolean success = false;

		if (EconomyUtils.payCost(ply, cost)) {
			if (keptItems != null) {
				PlayerInventory inv = ply.getInventory();

				for (StoredItemStack storedStack : keptItems) {

					if (inv.getItem(storedStack.slot) == null) // slot is empty
						inv.setItem(storedStack.slot, storedStack.itemStack);

					else { // slot is occupied --> add it regularly and drop if necessary
						HashMap<Integer, ItemStack> leftovers = inv.addItem(storedStack.itemStack);
						if (leftovers.size() > 0)
							Utilities.dropItems(ply.getLocation(), leftovers, false);
					}

				}

				success = true;
			}

			if (keptExp > 0) {
				ExperienceUtils.changeExp(ply, keptExp);

				// manually send a Packet43SetExperience to properly update the client;
				// as of 1.3.1, the client doesn't seem to accept that directly after respawn, so we delay it further
				Bukkit.getScheduler().scheduleSyncDelayedTask(DeathControl.instance, new Runnable() {
					@Override
					public void run() {
						Utilities.updateExperience(ply);
					}
				}, 20L);

				success = true;
			}
		} else {
			MessageHelper.sendMessage(ply, "You don't have enough money to get back your items!", true);
		}

		return success;
	}

	private void unregister() {
		if (!valid)
			return;
		DeathControl.instance.removeManager(plyName);
		valid = false;
	}

	public int getTimeoutOnQuit() {
		return timeoutOnQuit;
	}

}
