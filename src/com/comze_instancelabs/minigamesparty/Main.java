package com.comze_instancelabs.minigamesparty;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import com.comze_instancelabs.minigamesparty.minigames.ColorMatch;
import com.comze_instancelabs.minigamesparty.minigames.MineField;
import com.comze_instancelabs.minigamesparty.minigames.Spleef;

public class Main extends JavaPlugin implements Listener {

	// ATTENTION: VERY RESOURCE INTENSIVE PLUGIN
	
	//TODO:
	// [HIGH] add all other minigames
	// [MEDIUM] add more commands and stats etc.
	// [LOW] add config support
	
	
	/* setup pattern:
	 * 
	 * # - - -
	 * # - - -
	 * # - - -
	 * # # # #
	 * 
	 * #=minigame
	 * 
	 * IMPORTANT: LOBBY SPAWN MUST BE ABOVE SPAWNS
	 */
	
	/*
	 * SETUP
	 * 
	 * 1. build main lobby
	 * 2. /mp setlobby
	 * 3. go to location somewhere UNDER lobby
	 * 4. /mp setup
	 * 5. reload server
	 * 
	 */
	
	public ArrayList<Minigame> minigames = new ArrayList<Minigame>();
	public ArrayList<Player> players = new ArrayList<Player>();
	public HashMap<Player, ItemStack[]> pinv = new HashMap<Player, ItemStack[]>();
	
	public int min_players = 1; //TODO: increment to more like 2 or 3
	public boolean running = false;

	public Location mainlobby = null;
	
	Main m;
	
	@Override
	public void onEnable(){
		getServer().getPluginManager().registerEvents(this, this);
		m = this;
		int id = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run(){
				if(isValidMinigame("ColorMatch") && isValidMinigame("Spleef") && isValidMinigame("MineField")){
					ColorMatch cm = new ColorMatch(m, m.getComponentForMinigame("ColorMatch", "spawn"), m.getLobby(), m.getComponentForMinigame("ColorMatch", "spectatorlobby"));
					minigames.add(cm);
					getServer().getPluginManager().registerEvents(cm, m);
					Spleef sp = new Spleef(m, m.getComponentForMinigame("Spleef", "spawn"), m.getLobby(), m.getComponentForMinigame("Spleef", "spectatorlobby"));
					minigames.add(sp);
					getServer().getPluginManager().registerEvents(sp, m);
					MineField mf = new MineField(m, m.getComponentForMinigame("MineField", "spawn"), m.getLobby(), m.getComponentForMinigame("MineField", "spectatorlobby"), m.getComponentForMinigame("MineField", "finishline"));
					minigames.add(mf);
					getServer().getPluginManager().registerEvents(mf, m);
				}
			}
		}, 40);
	}
	
	
	
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	if(cmd.getName().equalsIgnoreCase("minigamesparty") || cmd.getName().equalsIgnoreCase("mp")){
    		if(args.length > 0){
    			if(args[0].equalsIgnoreCase("setup")){
    				// setup all arenas and spawns and lobbies and spectatorlobbies and what not
    				if(sender.hasPermission("mp.setup")){
	    				final Player p = (Player) sender;
	    				Bukkit.getServer().getScheduler().runTask(this, new Runnable(){
	    					public void run(){
	    						setupAll(p.getLocation());
	    					}
	    				});
    				}
    			}else if(args[0].equalsIgnoreCase("setlobby")){
    				if(sender.hasPermission("mp.setlobby")){
	    				Player p = (Player)sender;
	    				getConfig().set("lobby.world", p.getLocation().getWorld().getName());
	    				getConfig().set("lobby.location.x", p.getLocation().getBlockX());
	    				getConfig().set("lobby.location.y", p.getLocation().getBlockY());
	    				getConfig().set("lobby.location.z", p.getLocation().getBlockZ());
	    				this.saveConfig();
	    				p.sendMessage("�2Saved Main lobby.");	
    				}
    			}else if(args[0].equalsIgnoreCase("setcomponent")){
    				// /mp setcomponent [minigame] [component]
    				if(sender.hasPermission("mp.setcomponent")){
    					Player p = (Player)sender;
	    				if(args.length > 2){
	    					this.saveComponentForMinigame(args[1], args[2], p.getLocation());
	    					p.sendMessage("�2Saved component");
	    				}else{
	    					p.sendMessage("�3Possible components: spectatorlobby, spawn");
	    				}
    				}
    			}else if(args[0].equalsIgnoreCase("stats")){
    				sender.sendMessage("�3-- �6Statistics �3--");
    				//TODO: add statistics
    			}else if(args[0].equalsIgnoreCase("list")){
    				sender.sendMessage("�3-- �6Minigames: �3--");
    				for(Minigame m : minigames){
    					sender.sendMessage("�3" + m.name);
    				}
    			}else if(args[0].equalsIgnoreCase("leave")){
    				final Player p = (Player)sender;
    				p.teleport(getLobby());
    				Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
    					public void run(){
    						p.teleport(getLobby());
    					}
    				}, 5);
    				p.getInventory().clear();
    				p.updateInventory();
    				p.getInventory().setContents(pinv.get(p));
    				p.updateInventory();
    				if(currentmg > -1){
    					minigames.get(currentmg).leave(p);
    				}
    				players.remove(p);
    				p.sendMessage("�cYou left the game.");
    				if(players.size() < min_players){
    					stopFull();
    				}
    			}else{
    				sender.sendMessage("�3Help: ");
    				sender.sendMessage("�3/mp setlobby");
    				sender.sendMessage("�3/mp setup");
    				sender.sendMessage("�3/mp stats");
    				sender.sendMessage("�3/mp list");
    				sender.sendMessage("�3/mp leave");
    				sender.sendMessage("�3/mp setcomponent");
    			}
    		}
    		return true;
    	}
    	return false;
    }
	
    
    //TODO: player quits and rejoins -> still in arena!
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
    	if(players.contains(event.getPlayer())){
    		players.remove(event.getPlayer());
    	}
    	
    	if(players.size() < min_players){
    		stopFull();
    	}
    }
    
	@EventHandler
	public void onSignUse(PlayerInteractEvent event)
	{	
	    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)
	    {
	    	if(event.hasBlock()){
		    	if (event.getClickedBlock().getType() == Material.SIGN_POST || event.getClickedBlock().getType() == Material.WALL_SIGN)
		        {
		            final Sign s = (Sign) event.getClickedBlock().getState();
	                if (s.getLine(1).equalsIgnoreCase(ChatColor.BOLD + "�5[PARTY]")){
	                	if(players.contains(event.getPlayer())){
	                		event.getPlayer().sendMessage("�6You can leave with /mp leave");
	                	}else{
		                	players.add(event.getPlayer());
		                	// if its the first player to join, start the whole minigame
		                	if(players.size() < min_players + 1){
		                		pinv.put(event.getPlayer(), event.getPlayer().getInventory().getContents());
		                		startNew();
		                	}else{ // else: just join the minigame
		                		try{
		                			pinv.put(event.getPlayer(), event.getPlayer().getInventory().getContents());
		                			minigames.get(currentmg).join(event.getPlayer());
		                		}catch(Exception e){
		                			
		                		}
		                	}	
	                	}
	                }
		        }	
	    	}
	    }else if(event.getAction().equals(Action.PHYSICAL)){
	    	if(event.getClickedBlock().getType() == Material.STONE_PLATE){
	    		if(players.contains(event.getPlayer())){
	    			final Player p = event.getPlayer();
	    			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						@Override
						public void run() {
							p.teleport(minigames.get(currentmg).spawn);
						}
					}, 5);
	    			event.getClickedBlock().setType(Material.AIR);
	    		}
	    	}
	    }
	}
	
	@EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player p = event.getPlayer();
        if(event.getLine(0).toLowerCase().contains("[party]") || event.getLine(1).toLowerCase().contains("[party]")){
        	if(event.getPlayer().hasPermission("mp.sign")){
        		event.setLine(0, "");
	        	event.setLine(1, ChatColor.BOLD + "�5[PARTY]");
        	}
        }
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onMove(PlayerMoveEvent event){
		try{
			if(m.players.contains(event.getPlayer())){
				if(currentmg > -1){
					Minigame current = minigames.get(currentmg);
					if(!current.lost.contains(event.getPlayer())){
						if(started){
							if(event.getPlayer().getLocation().getBlockY() + 2 < current.spawn.getBlockY()){
								current.lost.add(event.getPlayer());
								current.spectate(event.getPlayer());
							}
						}
					}else if(current.lost.contains(event.getPlayer())){
						if(started){
							if(event.getPlayer().getLocation().getBlockY() < current.spectatorlobby.getBlockY() || event.getPlayer().getLocation().getBlockY() > current.spectatorlobby.getBlockY()){
								//current.spectate(event.getPlayer());
								final Player p = event.getPlayer();
								final Minigame mg = current;
								final float b = p.getLocation().getYaw();
								final float c = p.getLocation().getPitch();
								Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
									@Override
									public void run() {
										p.setAllowFlight(true);
										p.setFlying(true);
										p.teleport(new Location(p.getWorld(), p.getLocation().getBlockX(), mg.spectatorlobby.getBlockY(), p.getLocation().getBlockZ(), b, c));
										//p.getLocation().setYaw(b);
										//p.getLocation().setPitch(c);
									}
								}, 5);
							}	
						}
					}	
				}
				
			}	
		}catch(Exception e){
			for(StackTraceElement et : e.getStackTrace()){
				System.out.println(et);
			}
		}
		
	}
	
	@EventHandler
    public void onEntityDamage(EntityDamageEvent event){
    	if(event.getEntity() instanceof Player){
    		Player p = (Player)event.getEntity();
    		if(players.contains(p)){
    			event.setCancelled(true);
    		}
    	}
    }
    
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event){
    	if(event.getEntity() instanceof Player){
    		Player p = (Player)event.getEntity();
    		if(players.contains(p)){
    			event.setCancelled(true);
    		}
    	}
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
    	if(players.contains(event.getPlayer())){
    		//SPLEEF
    		if(event.getBlock().getType() == Material.SNOW_BLOCK){
    			event.getPlayer().getInventory().addItem(new ItemStack(Material.SNOW_BALL, 2));
    			event.getPlayer().updateInventory();
    			event.getBlock().setType(Material.AIR);
    			event.setCancelled(true);
    		}else{
    			event.setCancelled(true);
    		}
    	}
    }
	
	/*public void nextMinigame(Player p){
		// get current minigame and make winners
		// get new minigame and tp all to the new one
		p.setAllowFlight(false);
		p.setFlying(false);
		minigames.get(currentmg).getWinner();
		if(currentmg < minigames.size() - 1){
			currentmg += 1;
		}else{
			stop(currentid);
		}
		for(Minigame mg : minigames){
			mg.lost.clear();
		}
		minigames.get(currentmg).join(p);
	}*/
	
	public void win(Player p){
		//TODO: add winning of stars and statistics and scoreboard
		p.sendMessage("�6You won this round!");
	}
	
	
	/**
	 * NEW TIMER PART
	 */
	int c = 0; // count
	int c_ = 0;
	boolean started = false;
	BukkitTask t = null;
	int currentmg = 0;
	BukkitTask currentid = null;
	public void secondsTick(){
		// update scoreboard
		updateScoreboard(60 - c);
		
		// stop the whole party after some rounds
		if(c_ > minigames.size() * 60 - 3){
			Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable(){
				public void run(){
					startNew();
				}
			}, 30 * 20); // 30 secs
			t.cancel();
			started = false;
			
			ArrayList<Player> remove = new ArrayList<Player>();
			for(Player p : players){
				if(p.isOnline()){
					minigames.get(minigames.size() - 1).leave(p);
					p.sendMessage("�6Next round in 30 seconds!");
					p.getInventory().clear();
					p.updateInventory();
				}else{
					remove.add(p);
				}
			}
			
			// removes players that aren't online anymore
			for(Player p : remove){
				players.remove(p);
			}
			
			remove.clear();
			
			currentmg = -1;
			currentid = null;
			
			// reset all:
			ColorMatch.reset(this.getComponentForMinigame("ColorMatch", "spawn"));
			Spleef.reset(this.getComponentForMinigame("Spleef", "spawn"));
			MineField.reset(this.getComponentForMinigame("MineField", "spawn"));
			
			c = 0;
			c_ = 0;
			if(currentid != null){
				currentid.cancel();
			}
		}
		
		// start the next minigame after 60 seconds
		if(c == 60){
			c = 0;
			if(currentid != null){
				currentid.cancel();
			}
			currentid = nextMinigame();
		}
		

		c += 1;
		c_ += 1;
	}

	public BukkitTask nextMinigame(){
		if(currentmg > -1){
			minigames.get(currentmg).getWinner();	
		}
		if(currentmg < minigames.size() - 1){
			currentmg += 1;
		}else{
			if(currentid != null){
				stop(currentid);
			}
		}
		for(Minigame mg : minigames){
			mg.lost.clear();
		}
		for(Player p : players){
			if(p.isOnline()){
				p.setAllowFlight(false);
				p.setFlying(false);
				
				minigames.get(currentmg).join(p);
			}
		}
		if(currentmg > -1){
			return minigames.get(currentmg).start();
		}else{
			return null;
		}
	}
	
	public void startNew(){
		if(!started){
			if(players.size() > min_players - 1){
				// reset all
				for(Minigame m : minigames){
					m.lost.clear();
				}
				currentmg = -1;
				currentid = null;
				
				// start first minigame
				currentid = nextMinigame();
				
				// start main timer
				t = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable(){
					public void run(){
						secondsTick();
					}
				}, 1, 20);
				
				started = true;
			}	
		}
	}
	/**
	 * NEW TIMER PART
	 */
	
	public HashMap<Player, Integer> currentscore = new HashMap<Player, Integer>();
	
	public void updateScoreboard(int c){
		ScoreboardManager manager = Bukkit.getScoreboardManager();
	    
		boolean isMineFieldRunning = false;
		if(minigames.get(currentmg).name.equalsIgnoreCase("MineField")){
			isMineFieldRunning = true;
		}
		
	    for(Player p : players){
	    	Scoreboard board = manager.getNewScoreboard();
	    	
	    	Objective objective = board.registerNewObjective("test", "dummy");
	        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

	        objective.setDisplayName("[" + Integer.toString(currentmg + 1) + "/" + Integer.toString(minigames.size()) + "] [" + Integer.toString(c) + "]");

	        for(Player p_ : players){
	        	if(isMineFieldRunning){
	        		int score = p_.getLocation().getBlockZ() - minigames.get(currentmg).finish.getBlockZ();
	        		if(currentscore.containsKey(p_)){
	        			int oldscore = currentscore.get(p_);
	        			if(score > oldscore){
	        				currentscore.put(p_, score);
	        			}else{
	        				score = oldscore;
	        			}
	        		}else{
	        			currentscore.put(p_, score);
	        		}
	        		objective.getScore(p_).setScore(score);
	        	}else{
	        		objective.getScore(p_).setScore(0);
	        	}
	        }

	        p.setScoreboard(board);
	    }
	}
	
	
	
	 /*public void start(){
		// if not running -> start
		// else just join current game
		//    if no current game, join into waiting lobby
		
		if(players.size() > min_players - 1){
			// reset all
			for(Minigame m : minigames){
				m.lost.clear();
			}
			currentmg = 0;
			currentid = 0;
			
			// every player joins again (or maybe first time)
			for(Player p : players){
				minigames.get(0).join(p);
			}
			final int stopid = minigames.get(0).start();
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				@Override
				public void run(){
					Bukkit.getServer().getScheduler().cancelTask(stopid);
				}
			}, 1200);
			
			// main running timer
			if(!running){
				final int id__ = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
					@Override
					public void run(){
						int count = 0;
						ArrayList<Player> remove = new ArrayList<Player>();
						for(Player p : players){
							if(p.isOnline()){
								nextMinigame(p);
								count += 1;
							}else{
								remove.add(p);
							}
						}
						
						for(Player p : remove){
							players.remove(p);
						}
						
						remove.clear();
						
						if(count < min_players){ // one player left
							stopFull();
						}
					}
				}, 1200, 1200); // each 60 seconds -> change minigame	
				
				currentid = id__;
				
				int id = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					@Override
					public void run(){
						start();
					}
				}, minigames.size() * 1200 + 20 * 30); // 20 * 30: wait 30 seconds after all games	
				
				int id_ = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					@Override
					public void run(){
						stop(id__);
					}
				}, minigames.size() * 1200 - 40);
				
				running = true;
			}
		}
	}*/
	
	public void stop(BukkitTask id){
		id.cancel();
		running = false;
		
		ArrayList<Player> remove = new ArrayList<Player>();
		for(Player p : players){
			if(p.isOnline()){
				minigames.get(minigames.size() - 1).leave(p);
				p.sendMessage("�6Next round in 30 seconds!");
				p.getInventory().clear();
				p.updateInventory();
			}else{
				remove.add(p);
			}
		}
		
		// removes players that arent online anymore
		for(Player p : remove){
			players.remove(p);
		}
		
		remove.clear();
		
		currentmg = 0;
		 
		// reset all:
		ColorMatch.reset(this.getComponentForMinigame("ColorMatch", "spawn"));
		Spleef.reset(this.getComponentForMinigame("Spleef", "spawn"));
		MineField.reset(this.getComponentForMinigame("MineField", "spawn"));
	}
	
	public void stopFull(){
		Bukkit.getServer().getScheduler().cancelAllTasks();
		
		for(Player p : players){
			if(p.isOnline()){
				minigames.get(minigames.size() - 1).leave(p);
				p.sendMessage("�4Stopping minigame.");
			}
		}
		
		running = false;
		started = false;
		players.clear();
		currentmg = 0;
	}
	
	public Location getLobby(){
		return new Location(getServer().getWorld(getConfig().getString("lobby.world")), getConfig().getInt("lobby.location.x"), getConfig().getInt("lobby.location.y"), getConfig().getInt("lobby.location.z"));
	}
	
	public Location getComponentForMinigame(String minigame, String component, String count){
		if(isValidMinigame(minigame)){
			String base = "minigames." + minigame + "." + component + count;
			return new Location(Bukkit.getWorld(getConfig().getString(base + ".world")), getConfig().getInt(base + ".location.x"), getConfig().getInt(base + ".location.y"), getConfig().getInt(base + ".location.z"));
		}
		return null;
	}
	
	public Location getComponentForMinigame(String minigame, String component){
		if(isValidMinigame(minigame)){
			String base = "minigames." + minigame + "." + component;
			return new Location(Bukkit.getWorld(getConfig().getString(base + ".world")), getConfig().getInt(base + ".location.x"), getConfig().getInt(base + ".location.y"), getConfig().getInt(base + ".location.z"));
		}
		return null;
	}
	
	public void saveComponentForMinigame(String minigame, String component, Location comploc){
		String base = "minigames." + minigame + "." + component;
		getConfig().set(base + ".world", comploc.getWorld().getName());
		getConfig().set(base + ".location.x", comploc.getBlockX());
		getConfig().set(base + ".location.y", comploc.getBlockY());
		getConfig().set(base + ".location.z", comploc.getBlockZ());
		this.saveConfig();
	}
	
	public boolean isValidMinigame(String minigame){
		if(getConfig().isSet("minigames." + minigame) && getConfig().isSet("minigames." + minigame + ".lobby") && getConfig().isSet("minigames." + minigame + ".spawn") && getConfig().isSet("minigames." + minigame + ".spectatorlobby")){
			return true;
		}
		return false;
	}
	
	@Deprecated
	public void setupAll(Location start){
		int x = start.getBlockX();
		int y = start.getBlockY();
		int z = start.getBlockZ();
		
		ColorMatch.setup(start, this, "ColorMatch");
		Spleef.setup(new Location(start.getWorld(), x, y, z + 64 + 20), this, "Spleef");
		MineField.setup(new Location(start.getWorld(), x, y, z + 64 * 2 + 20 * 2), this, "MineField");
		/*
		 * next minigame locations: (TODO FOR LATER USE)
		 * 
		 * new Location(start.getWorld(), x, y, z + 64 * 2 + 20 * 2) [MINEFIELD]
		 * new Location(start.getWorld(), x, y, z + 64 * 3 + 20 * 3)
		 * new Location(start.getWorld(), x + 64 + 20, y, z)
		 * new Location(start.getWorld(), x + 64 * 2 + 20 * 2, y, z)
		 * new Location(start.getWorld(), x + 64 * 3 + 20 * 3, y, z)
		 * 
		 * would create the following pattern:
		 * 
		 * # - - -
		 * # - - -
		 * # - - -
		 * # # # #
		 * 
		 * #=minigame
		 * 
		 * IMPORTANT: LOBBY SPAWN MUST BE ABOVE SPAWNS!
		 */ 
		
		minigames.add(new ColorMatch(this, this.getComponentForMinigame("ColorMatch", "spawn"), this.getComponentForMinigame("ColorMatch", "lobby"), this.getComponentForMinigame("ColorMatch", "spectatorlobby")));
		minigames.add(new ColorMatch(this, this.getComponentForMinigame("Spleef", "spawn"), this.getComponentForMinigame("Spleef", "lobby"), this.getComponentForMinigame("Spleef", "spectatorlobby")));
		minigames.add(new ColorMatch(this, this.getComponentForMinigame("MineField", "spawn"), this.getComponentForMinigame("MineField", "lobby"), this.getComponentForMinigame("MineField", "spectatorlobby")));

		
		getLogger().info("Finished Setup");
	}

}
