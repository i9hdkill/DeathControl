package bone008.bukkit.deathcontrol.util;

import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import bone008.bukkit.deathcontrol.DeathControl;

public final class EconomyUtil {

	private EconomyUtil() {
	}

	private static Economy vaultEconomy = null;

	/**
	 * Initializes the economy functionalities.
	 */
	public static void init() {
		// delay everything by one tick to make sure that all plugins are fully loaded. Softdepend doesn't always work!
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(DeathControl.instance, new Runnable() {
			@Override
			public void run() {
				try {
					setupVault();
				} catch (NoClassDefFoundError err) {
				}
			}
		});
	}

	/**
	 * Tries to hook Vault.
	 * 
	 * @throws NoClassDefFoundError Thrown when Vault is not loaded. Should be caught by the calling method because it is (more or less) intended behavior!
	 */
	private static void setupVault() {
		RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null) {
			vaultEconomy = economyProvider.getProvider();
		}
	}

	public static double calcCost(Player ply, double percentage) {
		return calcCost(ply.getName(), percentage);
	}

	public static double calcCost(String plyName, double percentage) {
		if (plyName == null)
			throw new IllegalArgumentException("null argument");
		if (percentage < 0 || percentage > 1)
			throw new IllegalArgumentException("percentage out of range");

		double currBalance;
		if (vaultEconomy != null) {
			currBalance = vaultEconomy.getBalance(plyName);
		}
		else {
			logNotice(plyName);
			return 0;
		}
		return currBalance * percentage;
	}

	/**
	 * Checks if the given player has enough money to pay the given cost.<br>
	 * Prints a warning if no way to manage economy was found.
	 * 
	 * @return true if the player has enough money or no economy management plugin was found, otherwise false
	 */
	public static boolean canAfford(Player ply, double cost) {
		return canAfford(ply.getName(), cost);
	}

	/**
	 * Checks if the given player has enough money to pay the given cost.<br>
	 * Prints a warning if no way to manage economy was found.
	 * 
	 * @return true if the player has enough money or no economy management plugin was found, otherwise false
	 */
	public static boolean canAfford(String plyName, double cost) {
		if (plyName == null)
			throw new IllegalArgumentException("null argument");
		if (cost <= 0)
			return true;

		if (vaultEconomy != null) {
			return vaultEconomy.has(plyName, cost);
		}
		else {
			logNotice(plyName);
			return true;
		}
	}

	public static boolean payCost(Player ply, double cost) {
		return payCost(ply.getName(), cost);
	}

	public static boolean payCost(String plyName, double cost) {
		if (plyName == null)
			throw new IllegalArgumentException("null argument");
		if (cost <= 0)
			return true;

		if (vaultEconomy != null) {
			return vaultEconomy.withdrawPlayer(plyName, cost).transactionSuccess();
		}
		else {
			logNotice(plyName);
			return true;
		}
	}

	public static String formatMoney(double cost) {
		if (vaultEconomy != null)
			return vaultEconomy.format(cost);
		else
			return Double.toString(cost);
	}

	private static void logNotice(String pName) {
		DeathControl.instance.log(Level.WARNING, "Couldn't calculate money for " + pName + " because no economy management plugin was found!");
	}

}
