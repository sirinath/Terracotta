package com.tc.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import com.tc.test.config.model.TestConfig;

/**
 * <p>
 * This Class is similar to Parameterized Test with Improvements in logging and simplification in method signature for
 * providing Configs
 * </p>
 */
public class TcTestRunner extends Suite {
  /**
   * Annotation for a method which provides parameters to be injected into the test class constructor by
   * <code>Parameterized</code>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public static @interface Configs {
    // Empty Annotation
  }

  private class TestClassRunnerForParameters extends BlockJUnit4ClassRunner {
    private final int              fParameterSetNumber;

    private final List<TestConfig> fParameterList;

    TestClassRunnerForParameters(Class<?> type, List<TestConfig> parameterList, int i) throws InitializationError {
      super(type);
      fParameterList = parameterList;
      fParameterSetNumber = i;
    }

    @Override
    public Object createTest() throws Exception {
      return getTestClass().getOnlyConstructor().newInstance(computeParams());
    }

    private TestConfig computeParams() throws Exception {
      try {
        return fParameterList.get(fParameterSetNumber);
      } catch (ClassCastException e) {
        throw new Exception(String.format("%s.%s() must return a Collection of arrays.", getTestClass().getName(),
                                          getParametersMethod(getTestClass()).getName()));
      }
    }

    @Override
    protected String getName() {
      return String.format(fParameterList.get(fParameterSetNumber).getConfigName());
    }

    @Override
    protected String testName(final FrameworkMethod method) {
      return String.format("%s[%s]", method.getName(), fParameterList.get(fParameterSetNumber).getConfigName());
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
      validateOnlyOneConstructor(errors);
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
      return childrenInvoker(notifier);
    }
  }

  private final ArrayList<Runner> runners = new ArrayList<Runner>();

  /**
   * Only called reflectively. Do not use programmatically.
   */
  public TcTestRunner(Class<?> klass) throws Throwable {
    super(klass, Collections.<Runner> emptyList());
    List<TestConfig> parametersList = getParametersList(getTestClass());
    for (int i = 0; i < parametersList.size(); i++)
      runners.add(new TestClassRunnerForParameters(getTestClass().getJavaClass(), parametersList, i));
  }

  @Override
  protected List<Runner> getChildren() {
    return runners;
  }

  @SuppressWarnings("unchecked")
  private List<TestConfig> getParametersList(TestClass klass) throws Throwable {
    return (List<TestConfig>) getParametersMethod(klass).invokeExplosively(null);
  }

  private FrameworkMethod getParametersMethod(TestClass testClass) throws Exception {
    List<FrameworkMethod> methods = testClass.getAnnotatedMethods(Configs.class);
    for (FrameworkMethod each : methods) {
      int modifiers = each.getMethod().getModifiers();
      if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) return each;
    }

    throw new Exception("No public static parameters method on class " + testClass.getName());
  }

}
