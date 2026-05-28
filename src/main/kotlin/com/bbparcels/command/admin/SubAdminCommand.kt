package com.bbparcels.command.admin

import org.bukkit.command.CommandSender

interface SubAdminCommand {
    fun execute(sender: CommandSender, args: List<String>): Boolean
    fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}