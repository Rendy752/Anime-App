package com.luminoverse.animevibe.utils

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

class PrioritizeIPv4Dns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        return addresses.sortedBy { if (it is Inet4Address) 0 else 1 }
    }
}