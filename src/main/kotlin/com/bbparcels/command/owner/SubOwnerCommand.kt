package com.bbparcels.command.owner

import org.bukkit.command.CommandSender

interface SubOwnerCommand {
    fun execute(sender: CommandSender, args: List<String>): Boolean
    fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}
