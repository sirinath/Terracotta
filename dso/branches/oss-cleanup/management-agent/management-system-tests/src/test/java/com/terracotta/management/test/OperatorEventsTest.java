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
package com.terracotta.management.test;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

/**
 * OperatorEventsTest
 */
public class OperatorEventsTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 1;

  public OperatorEventsTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.setNumOfGroups(GROUP_COUNT);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[] {OperatorEventsTestClient.class});
  }

  public static class OperatorEventsTestClient extends AbstractTsaClient {

    public OperatorEventsTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getManagementPort(serverIndex),
            "/tc-management-api/agents/operatorEvents");
      }

      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getManagementPort(serverIndex),
                "/tc-management-api/agents/operatorEvents?sinceWhen=1377125095225&filterOutRead=true");
      }

    }
  }
}
