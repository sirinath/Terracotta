package com.terracotta.management.test;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.tests.base.AbstractTestBase;
import org.terracotta.toolkit.ToolkitFactory;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.management.beans.L2MBeanNames;
import com.tc.test.config.model.TestConfig;
import org.terracotta.util.ToolkitVersion;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public abstract class AbstractTsaAgentTestBase extends AbstractTestBase {

  public AbstractTsaAgentTestBase(TestConfig testConfig) {
    super(testConfig);

    String war = guessWarLocation();
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.management.war=" + war);
  }

  private String guessWarLocation() {
    String m2Root = System.getProperty("user.home") + "/.m2/repository".replace('/', File.separatorChar);
    if (System.getProperty("maven.repo.local") != null) {
      m2Root = System.getProperty("maven.repo.local");
      System.out.println("Found maven.repo.local defined as a system property! Using m2root=" + m2Root);
    }
    String version = guessVersion();

    String agentDir = m2Root + "/org/terracotta/management-tsa-war/".replace('/', File.separatorChar) + version;

    List<String> files = Arrays.asList(new File(agentDir).list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".war") && !name.endsWith("-sources.jar") && !name.endsWith("-tests.jar");
      }
    }));
    if (files.isEmpty()) {
      throw new AssertionError("No agent WAR file found in [" + agentDir + "]");
    }
    Collections.sort(files);

    // always take the last one of the sorted list, it should be the latest version
    return agentDir + File.separator + files.get(files.size() - 1);
  }

  private String guessVersion() {
    // e.g. /home/userXYZ/.m2/repository/org/terracotta/terracotta-toolkit-runtime/3.8.0-SNAPSHOT/terracotta-toolkit-runtime-3.8.0-SNAPSHOT.jar
    String toolkitJar = TestBaseUtil.jarFor(ToolkitVersion.class);
    if (toolkitJar == null) {
      throw new AssertionError("Cannot find toolkit JAR");
    }

    String[] pathes = toolkitJar.split("\\/");
    if (pathes.length > 2) {
      return pathes[pathes.length - 2];
    }

    throw new AssertionError("Invalid toolkit JAR: " + toolkitJar);
  }

  @Override
  protected String createClassPath(Class client) throws IOException {
    String expressRuntime = TestBaseUtil.jarFor(ToolkitFactory.class);
    String clientBase = TestBaseUtil.jarFor(AbstractTsaAgentTestBase.class);
    String l2Mbean = TestBaseUtil.jarFor(L2MBeanNames.class);
    String jsonParser = TestBaseUtil.jarFor(JSONValue.class);
    String ehCache = TestBaseUtil.jarFor(CacheManager.class);
    String slf4J = TestBaseUtil.jarFor(LoggerFactory.class);
    String commonsIo = TestBaseUtil.jarFor(IOUtils.class);
    return makeClasspath(expressRuntime, clientBase, l2Mbean, jsonParser, ehCache, slf4J, commonsIo);
  }

  public abstract static class AbstractTsaClient extends AbstractClientBase {


    protected static final String TSA_TEST_CACHE = "tsaTest";

    @Override
    protected final void doTest() throws Throwable {
      // wait for the TSA agent to finish up initialization
      boolean initSuccessful =  false;
      System.out.println("Starting test for " + getTerracottaUrl());
      for (int i = 0; i < 10; i++) {
        try {
          for (int j = 0; j < getGroupData(0).getServerCount(); j++) {
            httpGet("http://" + ConfigHelper.HOST + ":" + getGroupData(0).getTsaGroupPort(j) + "/tc-management-api/agents");
          }
          initSuccessful = true;
          break;
        } catch (IOException ioe) {
          Thread.sleep(1000);
        }
      }
      assertThat("Server initialization issue", initSuccessful, is(true));

      doTsaTest();
    }

    protected abstract void doTsaTest() throws Throwable;

    protected byte[] getTsaRawContent(String host, int port, String path, Map<String,String> headers) throws IOException {
      return httpRawGet("http://" + host + ":" + port + path, headers);
    }

    protected JSONArray getTsaJSONArrayContent(String host, int port, String path) throws IOException {
      String result = httpGet("http://" + host + ":" + port + path);
      System.out.println("Server ");
      System.out.println(result);
      return (JSONArray)JSONValue.parse(result);
    }

    protected byte[] httpRawGet(String urlString, Map<String,String> headers) throws IOException {
      URL url = new URL(urlString);
      URLConnection urlConnection = url.openConnection();

      if (headers != null) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
          urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
      }

      InputStream inputStream = urlConnection.getInputStream();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      while (true) {
        int read = inputStream.read();
        if (read == -1) break;
        baos.write(read);
      }

      baos.close();
      return baos.toByteArray();
    }

    protected String httpGet(String urlString) throws IOException {
      URL url = new URL(urlString);
      InputStream inputStream = url.openStream();

      StringBuilder sb = new StringBuilder();
      BufferedReader bufferedReader = null;
      try {
        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        while (true) {
          String line = bufferedReader.readLine();
          if (line == null) {
            break;
          }
          sb.append(line).append("\n");
        }

      } finally {
        if (bufferedReader != null) {
          bufferedReader.close();
        }
      }

      return sb.toString();
    }

    protected void httpPost(String urlString) throws IOException {
      HttpURLConnection httpConnection = (HttpURLConnection) new URL(urlString).openConnection();
      try {
        httpConnection.setRequestMethod("POST");
        httpConnection.setDoOutput(true); // Triggers POST.
        String charset = "UTF-8";
        httpConnection.setRequestProperty("Accept-Charset", charset);
        httpConnection.setRequestProperty("Content-Type", "application/json;charset=" + charset);

        if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
          throw new RuntimeException("Failed : HTTP error code : " + httpConnection.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));

        String output;
        System.out.println("Output from Server .... \n");
        while ((output = br.readLine()) != null) {
          System.out.println(output);
        }
      } finally {
        httpConnection.disconnect();
      }

    }


    public AbstractTsaClient(String[] args) {
      super(args);
    }

    protected CacheManager createCacheManager(String host, String port) {Configuration configuration = new Configuration();
      TerracottaClientConfiguration terracottaClientConfiguration = new TerracottaClientConfiguration();
      terracottaClientConfiguration.url(host, port);

      configuration.addTerracottaConfig(terracottaClientConfiguration);

      configuration.addDefaultCache(new CacheConfiguration("default", 100).eternal(false));

      CacheConfiguration cacheConfiguration = new CacheConfiguration(TSA_TEST_CACHE, 100).eternal(false)
          .terracotta(new TerracottaConfiguration());

      configuration.addCache(cacheConfiguration);

      return new CacheManager(configuration);
    }
  }


}
