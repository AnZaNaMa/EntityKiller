package com.anzanama.entitykiller;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.server.permission.DefaultPermissionLevel;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Class Description Here
 *
 * @author Andrew Graber
 * @version 11/23/2017
 */
public class ClearEntitiesCommand implements ICommand {
    public static final String NAME = "lag";
    public static final String USAGE = "/lag <clearall:butcher:clearitems:clearliving>";
    public static final ArrayList<String> ALIASES = new ArrayList<String>();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public List<String> getAliases() {
        return new ArrayList<String>();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityKiller.killEntities();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        if(!(sender instanceof EntityPlayer)) return true;
        EntityPlayer player = (EntityPlayer)sender;
        return player.canUseCommand(4, "entitykiller.base");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return null;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }
}
