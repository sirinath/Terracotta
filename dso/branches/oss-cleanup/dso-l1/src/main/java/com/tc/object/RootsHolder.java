/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.tc.net.GroupID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class RootsHolder implements ClearableCallback {
  private final Map<GroupID, Map<String, ObjectID>> groupToRoots                = new HashMap<GroupID, Map<String, ObjectID>>();
  private final Map<GroupID, Set<String>>           groupToRootLookupInProgress = new HashMap<GroupID, Set<String>>();

  private final GroupID[]                           groupIds;

  public RootsHolder(GroupID[] groupIds) {
    this.groupIds = groupIds;
  }

  @Override
  public synchronized void cleanup() {
    groupToRoots.clear();
    groupToRootLookupInProgress.clear();
  }

  public synchronized void addRoot(String name, ObjectID oid) {
    GroupID gid = new GroupID(oid.getGroupID());
    Map<String, ObjectID> roots = groupToRoots.get(gid);
    if (roots == null) {
      roots = new HashMap<String, ObjectID>();
      groupToRoots.put(gid, roots);
    }

    roots.put(name, oid);
  }

  public synchronized boolean markRootLookupInProgress(String name, GroupID gid) {
    Set<String> rootLookupInProgress = groupToRootLookupInProgress.get(gid);
    if (rootLookupInProgress == null) {
      rootLookupInProgress = new HashSet<String>();
      groupToRootLookupInProgress.put(gid, rootLookupInProgress);
    }

    return rootLookupInProgress.add(name);
  }

  public synchronized boolean unmarkRootLookupInProgress(String name, GroupID gid) {
    Set<String> rootLookupInProgress = groupToRootLookupInProgress.get(gid);
    if (rootLookupInProgress == null) { return false; }

    boolean isRemoved = rootLookupInProgress.remove(name);
    if (rootLookupInProgress.size() == 0) {
      groupToRootLookupInProgress.remove(gid);
    }
    return isRemoved;
  }

  public synchronized boolean isLookupInProgress(String name, GroupID gid) {
    Set<String> rootLookupInProgress = groupToRootLookupInProgress.get(gid);
    if (rootLookupInProgress == null) { return false; }

    return rootLookupInProgress.contains(name);
  }

  public synchronized ObjectID getRootIDForName(String name, GroupID gid) {
    Map<String, ObjectID> roots = groupToRoots.get(gid);
    if (roots == null) { return null; }

    return roots.get(name);
  }

  public synchronized GroupID getGroupIDForRoot(String name) {
    int hashCode = name.hashCode();
    int index = Math.abs(hashCode % groupIds.length);
    return groupIds[index];
  }

  public synchronized int size() {
    int size = 0;
    for (Map.Entry<GroupID, Map<String, ObjectID>> entry : groupToRoots.entrySet()) {
      size += entry.getValue().size();
    }

    return size;
  }


}