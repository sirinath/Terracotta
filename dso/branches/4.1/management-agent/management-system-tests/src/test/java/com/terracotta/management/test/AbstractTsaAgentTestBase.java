package com.terracotta.management.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.util.FileUtils;
import org.hamcrest.CoreMatchers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.tests.base.AbstractTestBase;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.util.ToolkitVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.management.beans.L2MBeanNames;
import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public abstract class AbstractTsaAgentTestBase extends AbstractTestBase {

  public AbstractTsaAgentTestBase(TestConfig testConfig) {
    super(testConfig);

    String warPath = fetchTsaWarLocation();
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.management.war=" + warPath);
    testConfig.getClientConfig().setParallelClients(false);
  }

  private String fetchTsaWarLocation() {
    InputStream in = null;
    try {
      in = AbstractTsaAgentTestBase.class.getResourceAsStream("/tsa-war-location.txt");
      if (in == null) { throw new RuntimeException("Couldn't find resource file tsa-war-location.txt"); }
      String warUrl = FileUtils.readFully(new InputStreamReader(in));
      return urlToFile(warUrl);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private static String urlToFile(String urlString) {
    URL url = null;
    File f;
    try {
      url = new URL(urlString);
      f = new File(url.toURI());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      f = new File(url.getPath());
    }
    return f.getAbsolutePath();
  }

  @Override
  protected String createClassPath(Class client) {
    String tk = TestBaseUtil.jarFor(ToolkitVersion.class);
    String common = TestBaseUtil.jarFor(Os.class);
    String expressRuntime = TestBaseUtil.jarFor(ToolkitFactory.class);
    String clientBase = TestBaseUtil.jarFor(AbstractTsaAgentTestBase.class);
    String l2Mbean = TestBaseUtil.jarFor(L2MBeanNames.class);
    String jsonParser = TestBaseUtil.jarFor(JSONValue.class);
    String ehCache = TestBaseUtil.jarFor(CacheManager.class);
    String slf4J = TestBaseUtil.jarFor(LoggerFactory.class);
    String commonsIo = TestBaseUtil.jarFor(IOUtils.class);
    String coreMatchers = TestBaseUtil.jarFor(CoreMatchers.class);
    return makeClasspath(tk, common, expressRuntime, clientBase, l2Mbean, jsonParser, ehCache, slf4J, commonsIo,
                         coreMatchers);
  }

  public abstract static class AbstractTsaClient extends AbstractClientBase {

    protected static final String TSA_TEST_CACHE = "tsaTest";

    protected String guessVersion() {
      return guessMavenArtifactVersion(ToolkitVersion.class);
    }

    @Override
    protected final void doTest() throws Throwable {
      // wait for the TSA agent to finish up initialization
      boolean initSuccessful = false;
      System.out.println("Starting test for " + getTerracottaUrl());
      for (int i = 0; i < 10; i++) {
        try {
          for (int j = 0; j < getGroupData(0).getServerCount(); j++) {
            httpGet("http://" + ConfigHelper.HOST + ":" + getGroupData(0).getTsaGroupPort(j)
                    + "/tc-management-api/agents");
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

    protected byte[] getTsaRawContent(String host, int port, String path, Map<String, String> headers)
        throws IOException {
      return httpRawGet("http://" + host + ":" + port + path, headers);
    }

    protected JSONArray getTsaJSONArrayContent(String host, int port, String path) throws IOException {
      String result = httpGet("http://" + host + ":" + port + path);
      System.out.println("Server ");
      System.out.println(result);
      return (JSONArray) JSONValue.parse(result);
    }

    protected byte[] httpRawGet(String urlString, Map<String, String> headers) throws IOException {
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

    protected String httpPost(String urlString) throws IOException {
      HttpURLConnection httpConnection = (HttpURLConnection) new URL(urlString).openConnection();
      try {
        httpConnection.setRequestMethod("POST");
        httpConnection.setDoOutput(true); // Triggers POST.
        String charset = "UTF-8";
        httpConnection.setRequestProperty("Accept-Charset", charset);
        httpConnection.setRequestProperty("Content-Type", "application/json;charset=" + charset);

        if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) { throw new RuntimeException(
                                                                                                        "Failed : HTTP error code : "
                                                                                                            + httpConnection
                                                                                                                .getResponseCode()); }

        BufferedReader br = new BufferedReader(new InputStreamReader((httpConnection.getInputStream())));

        StringBuffer sb = new StringBuffer();
        System.out.println("Output from Server .... \n");
        String output;
        while ((output = br.readLine()) != null) {
          sb.append(output).append('\n');
        }
        return sb.toString();
      } finally {
        httpConnection.disconnect();
      }

    }

    protected void waitUntilServerAgentUp(int port) {
      while (true) {
        try {
          httpGet("http://localhost:" + port + "/tc-management-api/agents");
          break;
        } catch (IOException e) {
          ThreadUtil.reallySleep(1000);
        }
      }
    }

    public AbstractTsaClient(String[] args) {
      super(args);
    }

    protected CacheManager createCacheManager(String host, String port) {
      Configuration configuration = new Configuration();
      TerracottaClientConfiguration terracottaClientConfiguration = new TerracottaClientConfiguration();
      terracottaClientConfiguration.url(host, port);

      configuration.addTerracottaConfig(terracottaClientConfiguration);

      configuration.addDefaultCache(new CacheConfiguration("default", 100).eternal(false));

      CacheConfiguration cacheConfiguration = new CacheConfiguration(TSA_TEST_CACHE, 100).eternal(false)
          .terracotta(new TerracottaConfiguration());

      configuration.addCache(cacheConfiguration);

      return new CacheManager(configuration);
    }

    static String guessMavenArtifactVersion(Class<?> clazz) {
      // e.g.
      // /home/userXYZ/.m2/repository/org/terracotta/terracotta-toolkit-runtime/3.8.0-SNAPSHOT/terracotta-toolkit-runtime-3.8.0-SNAPSHOT.jar
      String jar = TestBaseUtil.jarFor(clazz);
      if (jar == null) { throw new AssertionError("Cannot find JAR for class: " + clazz); }

      if (jar.endsWith(".jar")) {
        String[] pathes = jar.split("\\/");
        if (pathes.length > 2) { return pathes[pathes.length - 2]; }
        throw new AssertionError("Invalid JAR: " + jar);
      } else {
        // running from IDE? try to get the version from the pom file
        try {
          File fXmlFile = new File("pom.xml");
          DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
          Document doc = dBuilder.parse(fXmlFile);

          NodeList childNodes = doc.getDocumentElement().getChildNodes();
          for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if ("version".equals(node.getNodeName())) { return node.getTextContent(); }
          }
        } catch (Exception e) {
          // ignore
        }
        fail("cannot guess version");
        return null; // make the compiler happy
      }
    }

    protected boolean serverContainsAllOfThoseLogs(int group, int member, String... logs) throws IOException {
      JSONArray logsArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(group).getTsaGroupPort(member),
                                                   "/tc-management-api/agents/logs");
      boolean[] contains = new boolean[logs.length];

      for (Object aLogsArray : logsArray) {
        JSONObject o = (JSONObject) aLogsArray;
        String message = (String) o.get("message");

        for (int j = 0; j < logs.length; j++) {
          contains[j] |= message.contains(logs[j]);
        }

        boolean allTrue = true;
        for (boolean contain : contains) {
          allTrue &= contain;
        }
        if (allTrue) { return true; }
      }

      return false;
    }

    protected boolean serverContainsAnyOfThoseLogs(int group, int member, String... logs) throws IOException {
      JSONArray logsArray = getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(group).getTsaGroupPort(member),
                                                   "/tc-management-api/agents/logs");
      for (Object aLogsArray : logsArray) {
        JSONObject o = (JSONObject) aLogsArray;
        String message = (String) o.get("message");

        for (String log : logs) {
          if (message.contains(log)) { return true; }
        }
      }
      return false;
    }

    protected void assertTrueWithin(long timeInMs, Callable<Boolean> callable) throws Exception {
      Boolean success = false;

      while (timeInMs > 0) {
        success = callable.call();

        if (success != null && success) {
          break;
        }

        ThreadUtil.reallySleep(1000L);
        timeInMs -= 1000L;
      }

      assertThat(success, is(true));
    }
  }

}
