package bone008.bukkit.deathcontrol.config.conditions;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import bone008.bukkit.deathcontrol.DeathControl;
import bone008.bukkit.deathcontrol.config.ConditionDescriptor;
import bone008.bukkit.deathcontrol.config.DeathContext;
import bone008.bukkit.deathcontrol.exceptions.DescriptorFormatException;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class RegionCondition extends ConditionDescriptor {

	private String regionName;

	public RegionCondition(List<String> args) throws DescriptorFormatException {
		if (args.isEmpty())
			throw new DescriptorFormatException("no region given");

		regionName = args.get(0);
	}

	@Override
	public boolean matches(DeathContext context) {
		WorldGuard wg = WorldGuard.getInstance();

		if (wg == null) {
			DeathControl.instance.log(Level.WARNING, "Region condition: WorldGuard is not installed on the server!");
			return false;
		}
		else {
			Location deathLoc = context.getDeathLocation();
			RegionManager regionManager = wg.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(deathLoc.getWorld()));

			ProtectedRegion region = regionManager.getRegion(regionName);
			if (region == null) {
				DeathControl.instance.log(Level.FINE, "Region condition: WorldGuard region " + regionName + " unexistant in world " + deathLoc.getWorld().getName() + "!");
				// not existant in this world
				return false;
			}

			return region.contains(BlockVector3.at(deathLoc.getX(), deathLoc.getY(), deathLoc.getZ()));
		}
	}

	@Override
	public List<String> toParameters() {
		return Arrays.asList(regionName);
	}

}
