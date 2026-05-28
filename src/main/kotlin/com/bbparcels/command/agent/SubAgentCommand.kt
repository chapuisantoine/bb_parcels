package com.bbparcels.command.agent

import org.bukkit.command.CommandSender

interface SubAgentCommand {
    fun execute(sender: CommandSender, args: List<String>): Boolean
    fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}