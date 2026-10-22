package com.bbparcels.command

import org.bukkit.command.CommandSender

interface SubCommand {
    fun execute(sender: CommandSender, args: List<String>): Boolean
    fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}