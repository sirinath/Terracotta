/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import java.awt.Font;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

public class AdminClientBundle extends ListResourceBundle {
  public AdminClientBundle() {
    super();
    setParent(ResourceBundle.getBundle("com.tc.admin.common.CommonBundle"));
  }

  @Override
  public Object[][] getContents() {
    return new Object[][] {
        { "console.guide.url", "http://www.terracotta.org/kit/reflector?kitID={0}&pageID={1}" },
        { "get.svt.url", "http://www.terracotta.org/kit/reflector?kitID={0}&pageID=GetSVT" },
        { "show.svt.label", "Show SVT..." },
        { "cluster.node.label", "Terracotta cluster" },
        { "rename.label", "Rename" },
        { "connect.label", "Connect" },
        { "disconnect.label", "Disconnect" },
        { "shutdown.label", "Shutdown" },
        { "shutdown.server.confirm", "Are you sure you want to shutdown {0}?" },
        { "stats.recorder.node.label", "Cluster statistics recorder" },
        { "stats.recording.suffix", " (on)" },
        { "quit.anyway", "Quit anyway?" },
        { "disconnect.anyway", "Disconnect anyway?" },
        { "recording.stats.msg", "There is an active statistic recording session.  {0}" },
        { "profiling.locks.msg", "Lock profiling is currently enabled.  {0}" },
        { "recording.stats.profiling.locks.msg",
            "<html>There is an active statistic recording session<br>and lock profiling is currently enabled.  {0}</html>" },
        { "sessions", "Sessions" },
        { "title", "Terracotta Developer Console" },
        { "messages", "Messages" },
        { "all.nodes", "All Nodes" },
        { "new.cluster.action.label", "New Cluster" },
        { "quit.action.label", "Quit" },
        { "connect.title", "Connect to JMX Server" },
        { "connecting.to", "Connecting to {0} ..." },
        { "connected.to", "Connected to {0}" },
        { "cannot.connect.to", "Unable to connect to {0}" },
        { "cannot.connect.to.extended", "Unable to connect to {0}: {1}" },
        { "service.unavailable", "Service Unavailable: {0}" },
        { "unknown.host", "Unknown host: {0}" },
        { "disconnecting.from", "Disconnecting from {0} ..." },
        { "disconnected.from", "Disconnected from {0}" },
        { "deleted.server", "Deleted {0}" },
        { "added.server", "Added {0}" },
        { "server.properties.headings", new String[] { "Name", "Value" } },
        { "server.activated.status", "{0} activated on {1}" },
        { "server.activated.label", "Activated on {0}" },
        { "server.started.status", "Started {0} on {1}" },
        { "server.started.label", "Started on {0}" },
        { "server.initializing.status", "Initializing {0} on {1}" },
        { "server.initializing.label", "Initializing on {0}" },
        { "server.standingby.status", "{0} standing by on {1}" },
        { "server.standingby.label", "Standing by on {0}" },
        { "server.disconnected.status", "{0} disconnected on {1}" },
        { "server.disconnected.label", "Disconnected on {0}" },
        {
            "server.non-restartable.warning",
            "<html>This Terracotta server array is configured for <code>temporary-swap-only</code> persistence "
                + "mode. In the event that all Terracotta servers in the array are taken down, all clustered data "
                + "will be lost and no existing clients will be allowed to rejoin the cluster. To ensure that in "
                + "the event of a full cluster restart data is preserved and clients may rejoin the cluster, change "
                + "the configured persistence mode to <code>permanent-store</code> and restart:</html>" },
        { "dso", "DSO" },
        { "dso.roots", "Object browser" },
        { "dso.heap", "Clustered heap" },
        { "dso.diagnostics", "Diagnostics" },
        { "dso.platform", "Platform" },
        { "dso.monitoring", "Monitoring" },
        { "cluster.topology", "Topology" },
        { "cluster.features", "My application" },
        { "dso.runtime.stats", "Runtime statistics" },
        { "dso.roots.suffix.singular", "root" },
        { "dso.roots.suffix.plural", "roots" },
        { "dso.client.roots", "Client objects" },
        { "dso.locks", "Lock profiler" },
        { "dso.locks.profiling.suffix", " (on)" },
        {
            "dso.locks.column.headings",
            new String[] { "Lock", "<html>Times<br>Requested</html>", "<html>Times<br>Hopped</html>",
                "<html>Average<br>Contenders</html>", "<html>Average<br>Acquire Time</html>",
                "<html>Average<br>Held Time</html>" } },
        {
            "dso.locks.column.tips",
            new String[] {
                "Lock identifier",
                "<html>Number of times this lock<br>was requested</html>",
                "<html>Times an acquired greedy lock was<br>retracted from holding client and<br>granted to another</html>",
                "<html>Average number of threads wishing<br>to acquire this lock at the time<br>it was requested</html>",
                "<html>Average time between lock<br>request and grant</html>",
                "<html>Average time grantee held<br>this lock</html>",
                "<html>Average number of outstanding<br>locks held by acquiring thread<br>at grant time</html>" } },
        { "refresh.name", "Refresh" },
        { "dso.roots.refreshing", "Refreshing roots..." },
        { "refreshing.field.pattern", "Refreshing field {0}..." },
        { "dso.deadlocks.detect", "Detect deadlocks" },
        { "dso.deadlocks.detecting", "Detecting deadlocks..." },
        { "dso.classes", "Instance counts" },
        { "dso.allClasses", "All classes" },
        { "dso.classes.refreshing", "Refreshing classes..." },
        { "dso.classes.className", "Class" },
        { "dso.classes.instanceCount", "Creation count since active server start" },
        { "dso.classes.config.desc",
            "This config snippet is constructed from the set of shared instances created since the server started." },
        { "dso.locks.refreshing", "Refreshing locks..." },
        { "dso.object.flush.rate", "Object Flush Rate" },
        { "dso.object.fault.rate", "Object Fault Rate" },
        { "dso.transaction.rate", "Transaction Rate" },
        { "dso.lock-recall.rate", "Lock Recall Rate" },
        { "dso.pending.client.transactions", "Unacknowledged Transaction Broadcasts" },
        { "dso.root.retrieving", "Retrieving new DSO root..." },
        { "dso.root.new", "Added new DSO root: " },
        { "cluster.thread.dumps", "Cluster dumps" },
        { "server.thread.dumps", "Server thread dumps" },
        { "client.thread.dumps", "Client thread dumps" },
        { "connected-clients", "Connected clients" },
        { "servers", "Servers" },
        { "server-groups", "Server Array" },
        { "mirror.group", "Mirror group" },
        { "dso.client.retrieving", "Retrieving new DSO client..." },
        { "dso.client.new", "Added new DSO client: " },
        { "dso.client.detaching", "Detaching DSO client..." },
        { "dso.client.detached", "Detached DSO client: " },
        { "dso.client.host", "Host" },
        { "dso.client.port", "Port" },
        { "dso.client.channelID", "ChannelID" },
        { "dso.client.clientID", "Client ID" },
        { "dso.cluster.objectManager", "Object Manager" },
        { "live.object.count", "Live Object Count" },
        { "end.object.count", "End Object Count" },
        { "dgc.tip", "Live Objects after DGC" },
        { "liveObjectCount.tip", "<html>Number of managed objects currently resident,<br>excluding literals</html>" },
        { "cluster.dashboard", "Dashboard" },
        { "dso.gcstats", "Garbage collection" },
        { "dso.gcstats.overview.pending", "Getting DGC settings: please wait..." },
        { "dso.gcstats.overview.enabled", "DGC is configured to run: every {0} seconds ({1} minutes)." },
        { "dso.gcstats.overview.disabled", "DGC is disabled in this server array instance's configuration." },
        { "dso.gcstats.overview.not-ready", "Cluster is not yet ready." },
        { "operator.events", "Operator Events" },
        { "operator.events.timeOfEvent", "Time of Event" },
        { "operator.events.node", "Node" },
        { "operator.events.eventType", "Event Type" },
        { "operator.events.system", "Event System" },
        { "operator.events.message", "Message" },
        { "operator.events.aggregate.view", "Aggregate View" },
        { "operator.events.level.all", "All Events" },
        { "operator.events.level.view", "View By Event Type" },
        { "operator.events.subsystem.view", "View By Event System" },
        { "map.entry", "MapEntry" },
        { "log.error", "ERROR" },
        { "log.warn", "WARN" },
        { "log.info", "INFO" },
        { "dso.cache.rate.domain.label", "Time" },
        { "dso.cache.rate.range.label", "Objects per second" },
        { "dso.transaction.rate.range.label", "Transactions per second" },
        { "dso.cache.activity", "Cache activity" },
        { "dso.cache.miss.rate", "Cache Miss Rate" },
        { "dso.cache.miss.rate.label", "Cache Misses per second" },
        { "dso.onheap.flush.rate", "OnHeap Flush Rate" },
        { "dso.onheap.fault.rate", "OnHeap Fault Rate" },
        { "dso.offheap.flush.rate", "OffHeap Flush Rate" },
        { "dso.offheap.fault.rate", "OffHeap Fault Rate" },
        { "dso.gcstats.iteration", "Iteration" },
        { "dso.gcstats.type", "Type" },
        { "dso.gcstats.status", "Status" },
        { "dso.gcstats.startTime", "<html>Start<br>time</html>" },
        { "dso.gcstats.elapsedTime", "<html>Total<br>elapsed<br>time (ms.)</html>" },
        { "dso.gcstats.beginObjectCount", "<html>Begin<br>count</html>" },
        { "dso.gcstats.endObjectCount", "<html>End<br>count</html>" },
        { "dso.gcstats.pausedStageTime", "<html>Paused<br>stage (ms.)</html>" },
        { "dso.gcstats.markStageTime", "<html>Mark<br>stage (ms.)</html>" },
        { "dso.gcstats.actualGarbageCount", "<html>Garbage<br>count</html>" },
        { "dso.gcstats.deleteStageTime", "<html>Delete<br>stage (ms.)</html>" },
        { "dso.gcstats.graph.elapsedTime", "Elapsed Time" },
        { "dso.gcstats.graph.freedObjectCount", "Freed Objects" },
        { "dso.all.statistics", "All statistics" },
        { "file.menu.label", "File" },
        { "tools.menu.label", "Tools" },
        { "help.menu.label", "Help" },
        { "help.item.label", "Developer Console Help..." },
        { "about.action.label", "About Terracotta Developer Console" },
        { "update-checker.control.label", "Check For Updates" },
        { "update-checker.action.label", "Update Checker..." },
        { "update-checker.connect.failed.msg", "Unable to connect to update site." },
        { "update-checker.current.msg", "Your software is up-to-date." },
        { "update-checker.updates.available.msg", "New Terracotta versions are now available." },
        { "update-checker.release-notes.label", "Release notes" },
        { "update-checker.action.title", "Terracotta Update Checker" },
        { "update-checker.last.checked.msg", "Last checked: {0}" },
        { "version.check.enable.label", "Check Server Version" },
        { "version.check.disable.label", "Disable version checks" },
        {
            "version.check.message",
            "<html><h3>Version mismatch for {0}.</h3><br>"
                + "<table border=0 cellspacing=1><tr><td align=right><b>Terracotta Server Instance Version:</b></td><td>{1}"
                + "</tr><tr><td align=right><b>Developer Console Version:</b</td><td>{2}"
                + "</td></tr></table><h3>Continue?</h3></html>" },
        { "aggregate.server.stats.flush.rate", "Client Flush Rate" },
        { "aggregate.server.stats.flush.rate.tip", "All clients -> Server Array" },
        { "aggregate.server.stats.fault.rate", "Client Fault Rate" },
        { "aggregate.server.stats.fault.rate.tip", "Server Array -> All connected clients" },
        { "aggregate.server.stats.transaction.rate", "Write Transaction Rate" },
        { "aggregate.server.stats.transaction.rate.tip", "All connected clients -> Server Array" },
        { "aggregate.server.stats.onheap.flushfault", "OnHeap Fault/Flush Rate" },
        { "aggregate.server.stats.onheap.flushfault.tip", "OnHeap <--> OffHeap Store or Disk Store" },
        { "aggregate.server.stats.offheap.flushfault", "OffHeap Fault/Flush Rate" },
        { "aggregate.server.stats.offheap.flushfault.tip", "OffHeap Store <--> Disk Store" },
        { "stats.cpu.load", "Host CPU Load" },
        { "stats.cpu.load.tip", "Total CPU load of the host system" },
        { "stats.cpu.usage", "Host CPU Usage" },
        { "stats.cpu.usage.tip", "Total CPU usage of the host system" },
        { "server.stats.flush.rate", "Client Flush Rate" },
        { "server.stats.flush.rate.tip", "All connected clients -> This server instance" },
        { "server.stats.fault.rate", "Client Fault Rate" },
        { "server.stats.fault.rate.tip", "This server instance -> All connected clients" },
        { "server.stats.transaction.rate", "Write Transaction Rate" },
        { "server.stats.transaction.rate.tip", "All connected clients -> This server instance" },
        { "server.stats.onheap.flushfault", "OnHeap Fault/Flush Rate" },
        { "server.stats.onheap.flushfault.tip", "OnHeap <--> OffHeap Store or Disk Store" },
        { "server.stats.offheap.flushfault", "OffHeap Fault/Flush Rate" },
        { "server.stats.offheap.flushfault.tip", "OffHeap Store <---> Disk Store" },
        { "server.stats.onheap.usage", "OnHeap Usage" },
        { "server.stats.onheap.usage.tip", "OnHeap utilization for this server instance" },
        { "server.stats.offheap.usage", "OffHeap Usage" },
        { "server.stats.offheap.usage.tip", "OffHeap utilization for this server instance" },
        { "onheap.usage.max", "onheap memory max" },
        { "onheap.usage.used", "onheap memory used" },
        { "offheap.usage.max", "offheap memory max" },
        { "offheap.usage.used", "offheap memory used" },
        { "offheap.map.usage", "OffHeap Map Allocated" },
        { "offheap.object.usage", "OffHeap Object Allocated" },
        { "heap.usage.max", "heap memory max" },
        { "heap.usage.used", "heap memory used" },
        { "client.stats.flush.rate", "Client Flush Rate" },
        { "client.stats.flush.rate.tip", "This client" },
        { "client.stats.fault.rate", "Client Fault Rate" },
        { "client.stats.fault.rate.tip", "Server Array -> This client" },
        { "client.stats.transaction.rate", "Write Transaction Rate" },
        { "client.stats.transaction.rate.tip", "Client -> Server Array" },
        { "client.stats.cache.miss.rate", "Cache Miss Rate" },
        { "client.stats.cache.miss.rate.tip", "Disk -> This server instance" },
        { "stats.heap.usage", "Heap Usage" },
        { "stats.heap.usage.tip", "Heap utilization for this client" },
        { "client.stats.pending.transactions", "Unacknowledged Transaction Broadcasts" },
        { "client.stats.pending.transactions.tip", "Server Array -> Client" },
        { "resident.object.message",
            "<html>Greyed-out items are not resident in this clients heap: <span style='color:#C0C0C0'>Not resident</span></html>" },
        { "thread.dump.timeout.msg", "Timed-out after {0} seconds." }, { "roots.inspect.show", "Show..." },
        { "thread.dump.export.as.text", "Export As Text..." }, { "thread.dump.take", "Take Thread Dump" },
        { "cluster.dump.take", "Take Cluster State Dump" }, { "take.thread.dump.for", "Take thread dump for:" },
        { "export.all.thread.dumps.dialog.title", "Export All Thread Dumps" },
        { "export.thread.dump.as.text.dialog.title", "Export Thread Dump As Text" }, { "classes.tabular", "Tabular" },
        { "classes.hierarchical", "Hierarchical" }, { "classes.map", "Map" },
        { "classes.config.snippet", "Config snippet" }, { "current.view.type", "Current View Type:" },
        { "select.view", "Select View:" }, { "aggregate.view", "Aggregate View" },
        { "object.browser.cluster.heap", "Cluster Heap" },
        { "runtime.stats.aggregate.server.stats", "Aggregate Server Stats" },
        { "runtime.stats.per.server.view", "Per Server View" }, { "runtime.stats.per.client.view", "Per Client View" },
        { "dashboard.txn-rate", "Write Txn/s" }, { "dashboard.lock-recall-rate", "Lock Recall/s" },
        { "dashboard.object-creation-rate", "Objects Created/s" }, { "dashboard.broadcast-rate", "Broadcasts/s" },
        { "dashboard.fault-rate", "Faults/s" }, { "dashboard.flush-rate", "Flushes/s" },
        { "dashboard.txn-size-rate", "Txn Size KB/s" }, { "dashboard.unacked-txns", "Unacked Txns" },
        { "dashboard.transactions", "Transactions" }, { "dashboard.impeding-factors", "Impeding Factors" },
        { "dashboard.dial.tip.format", "<html>{0}:<br>Max={1}, Average={2}</html>" },
        { "dashboard.header.label.font", new Font("SansSerif", Font.PLAIN, 12) }, };
  }
}
