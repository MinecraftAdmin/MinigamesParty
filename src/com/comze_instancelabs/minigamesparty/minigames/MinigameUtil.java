package com.comze_instancelabs.minigamesparty.minigames;

import org.bukkit.ChatColor;

import com.comze_instancelabs.minigamesparty.Minigame;

public class MinigameUtil {

	public static String nowPlaying(String name) {
		return ChatColor.GOLD + "You are now playing " + ChatColor.GREEN + name + ChatColor.GOLD + "!";
	}
	
	public static String description(Minigame m, String name){
		return ChatColor.GOLD + m.description;
	}
	
}
