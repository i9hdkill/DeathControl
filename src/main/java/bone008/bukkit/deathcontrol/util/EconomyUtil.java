package bone008.bukkit.deathcontrol.util;

import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

	public static double calcCost(OfflinePlayer player, double percentage) {
		if (player == null)
			throw new IllegalArgumentException("null argument");
		if (percentage < 0 || percentage > 1)
			throw new IllegalArgumentException("percentage out of range");

		double currBalance;
		if (vaultEconomy != null) {
			currBalance = vaultEconomy.getBalance(player);
		}
		else {
			logNotice(player);
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
	public static boolean canAfford(OfflinePlayer player, double cost) {
		if (player == null)
			throw new IllegalArgumentException("null argument");
		if (cost <= 0)
			return true;

		if (vaultEconomy != null) {
			return vaultEconomy.has(player, cost);
		}
		else {
			logNotice(player);
			return true;
		}
	}

	public static boolean payCost(OfflinePlayer player, double cost) {
		if (player == null)
			throw new IllegalArgumentException("null argument");
		if (cost <= 0)
			return true;

		if (vaultEconomy != null) {
			return vaultEconomy.withdrawPlayer(player, cost).transactionSuccess();
		}
		else {
			logNotice(player);
			return true;
		}
	}

	public static String formatMoney(double cost) {
		if (vaultEconomy != null)
			return vaultEconomy.format(cost);
		else
			return Double.toString(cost);
	}

	private static void logNotice(OfflinePlayer player) {
		DeathControl.instance.log(Level.WARNING, "Couldn't calculate money for " + player.getName() + " because no economy management plugin was found!");
	}

}
