package com.tc.object.partitions;

import com.tc.object.TCObject;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;

public class PartitionManager {

      public static final ThreadLocal partitionedClusterLocal = new ThreadLocal();

    private static Object getCurrentObjectManager() {
        String clusterId = (String)partitionedClusterLocal.get();
        if(clusterId == null)
        throw new TCNoPartitionError("Thread calling TC operation does not have partition context Set\n" +
            "Any shared data should be accessed only after getting it from contained partitioned\n" +
            "data structure");

          Manager rv = ClassProcessorHelper.getParitionedManager(clusterId);
          return rv.getObjectManager();
    }

    public static void assertSamePartition(TCObject tco) {
      if(!ClassProcessorHelper.USE_PARTITIONED_CONTEXT)
        return;
      if(!(getCurrentObjectManager() == tco.getTCClass().getObjectManager()))
        throw new AssertionError("Data of a different partition can not be joined/modified" +
          " from the context of another partition");
    }

    public static int setPartition(int partitionNumber) {
      if(!ClassProcessorHelper.USE_PARTITIONED_CONTEXT)
        return -1;
      String oldPartition = (String)partitionedClusterLocal.get();
      partitionedClusterLocal.set("Partition" + partitionNumber);
      if(oldPartition != null) {
        return new Integer(oldPartition.substring(9)).intValue();
      }
      return -1;
    }

    public static int getNumPartitions() {
      return ClassProcessorHelper.getNumPartitions();
    }

    public static Manager getPartitionManager() {
        String clusterId = (String)partitionedClusterLocal.get();
          return ClassProcessorHelper.getParitionedManager(clusterId);
    }
}
