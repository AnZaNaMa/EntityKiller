package com.anzanama.entitykiller;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.*;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import scala.collection.mutable.HashTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A mod that clears entities from the world
 *
 * @author Andrew Graber
 * @version 11/23/2017
 */
@Mod(name="Entity Killer", modid="entitykiller", version="1.0", serverSideOnly = true, acceptableRemoteVersions = "*")
public class EntityKiller {
    public static Configuration config;
    public static HashMap<String, Boolean> itemWhitelist = new HashMap<String, Boolean>();
    public static HashMap<String, Boolean> entityWhitelist = new HashMap<String, Boolean>();
    public static int timeBetween;
    public static int timer = 0;
    public static boolean messageSent = false;
    public static int killsPerTick = 0;
    public static final String DEFAULT_ITEM_WHITELIST = "minecraft:torch, minecraft:cobblestone";
    public static final String DEFAULT_ENTITY_WHITELIST = "minecraft:wolf, minecraft:skeleton";
    public static final String ITEM_WHITELIST_COMMENT = "A Comma separated list of items to not clear";
    public static final String ENTITY_WHITELIST_COMMENT = "A Comma separated list of entities to not clear";
    public static Stack<EntityLiving> entitiesToKill = new Stack<EntityLiving>();
    private static int numKilled = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        timer++;
        if(timer >= timeBetween) {
            killEntities();
            timer = 0;
            messageSent = false;
        } else if(timer >= timeBetween-1200 && !messageSent) {
            messageSent = true;
            FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString("" + TextFormatting.DARK_GREEN +
                    TextFormatting.BOLD + "[EntityKiller] " + TextFormatting.RESET + TextFormatting.GREEN +
                    "Clearing items and entities in 1 minute!"));
        }

        if(!entitiesToKill.empty()) {
            for(int i=0; i<killsPerTick; i++) {
                if(!entitiesToKill.empty()) {
                    Entity entity = entitiesToKill.pop();
                    if(entity != null && !entity.isDead) {
                        entity.setDead();
                    }
                }
            }
            if(entitiesToKill.empty()) {
                FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString("" + TextFormatting.DARK_GREEN +
                        TextFormatting.BOLD + "[EntityKiller] " + TextFormatting.RESET + TextFormatting.GREEN +
                        "cleared " + TextFormatting.YELLOW + numKilled + TextFormatting.GREEN + " entities."));
                numKilled = 0;
            }
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        syncConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new EntityKiller());
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new ClearEntitiesCommand());
    }

    public static void syncConfig() {
        try {
            config.load();

            Property prop = config.get(Configuration.CATEGORY_GENERAL, "clear_timer", "36000",
                    "The amount of time (in ticks) between each automatic clear");
            timeBetween = prop.getInt();

            prop = config.get(Configuration.CATEGORY_GENERAL, "kills_per_tick", "10",
                    "The amount of entities to kill every tick once the culling begins");
            killsPerTick = prop.getInt();

            String whitelistStr = config.get(Configuration.CATEGORY_GENERAL,
                    "item_whitelist", DEFAULT_ITEM_WHITELIST, ITEM_WHITELIST_COMMENT).getString();
            if(!whitelistStr.equals(DEFAULT_ITEM_WHITELIST)) {
                String[] whitelistStrings = whitelistStr.split(", ");
                for (String str : whitelistStrings) {
                    Item item = Item.REGISTRY.getObject(new ResourceLocation(str));
                    itemWhitelist.put(item.getRegistryName().toString(), true);
                }
            }

            whitelistStr = config.get(Configuration.CATEGORY_GENERAL, "entity_whitelist",
                    DEFAULT_ENTITY_WHITELIST, ENTITY_WHITELIST_COMMENT).getString();
            if(!whitelistStr.equals(DEFAULT_ENTITY_WHITELIST)) {
                String[] whitelistStrings = whitelistStr.split(", ");
                for (String str : whitelistStrings) {
                    String entity = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(str)).getName();
                    entityWhitelist.put(entity, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(config.hasChanged()) config.save();
        }
    }

    public static void killEntities() {
        World[] worlds = DimensionManager.getWorlds();
        for(World world : worlds) {
            List<Entity> entities = world.loadedEntityList;
            for(Entity entity : entities) {
                if(entity instanceof EntityItem && !isItemWhitelisted((EntityItem)entity)) {
                    EntityItem item = (EntityItem)entity;
                    item.lifespan = 0;
                    numKilled++;
                } else if((entity instanceof EntityAnimal || entity instanceof EntityMob) && !isMobWhitelisted(entity)) {
                    if(!entity.hasCustomName() && !(entity instanceof EntityTameable && ((EntityTameable)entity).isTamed())
                            && !(entity instanceof AbstractHorse && ((AbstractHorse)entity).isTame())) {
                        entitiesToKill.push((EntityLiving)entity);
                        numKilled++;
                    }
                }
            }
        }
    }

    private static boolean isItemWhitelisted(EntityItem item) {
        return itemWhitelist.containsKey(item.getItem().getItem().getRegistryName().toString());
    }

    private static boolean isMobWhitelisted(Entity entity) {
        return entityWhitelist.containsKey(entity.getName());
    }
}
