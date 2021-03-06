#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module Resources
  HELP_MESSAGE = <<END_HELP_MESSAGE
usage: tcbuild <target>

Available targets are:

BASICS

clean
    Removes everything the buildsystem can build

clean_tests
    Removes all testruns (results of tests, temp files, etc.)

resolve_dependencies
    Invokes Ivy to download external libraries and install them into the
    dependencies project.  The compile target depends on this target, and
    therefore so do any targets that depend on compile.  To bypass the
    dependency resolution process, pass the --no-ivy option anywhere on the
    tcbuild command-line.

compile
    Compiles all the code.

smart_compile <project-name>
    Compiles the project and all of its dependencies

show_modules
    Prints a list of all the modules the system knows about.

show_module_dependencies <module-name>
    Prints the names of all modules that the named module depends on.

show_dependent_modules <module-name>
    Prints the names of all modules that depend on the named module.

USEFUL OPTIONS

compile_only=dso-container-tests,dso-spring-tests
    Only compile specified projects

--no-ivy
    Don't run ivy

--no-compile
    Don't compile

TESTING

check
    Runs all tests

check check_modules=dso-container-tests
    Run all tests under module(s) (comma separated list)

check_one <test_name>
    Runs a single named test where <test_name> is the class
    name of the Java class containing the test to run.  This
    target will scan all modules to find the test.

    Example: tcbuild check_one AssertTest --no-ivy compile_only=common

show_classpath <project> <subtree>
    Show the classpath of the subtree in this project
    Ex: tcbuild show_classpath dso-container-tets tests.system

check_prep <module> <type>
    Prepare to run tests from an external tool (e.g. Eclipse).
    <module> may be the name of a module as specified in modules.def.yml, or
    it may be the keyword 'all'.
    <type> may be either 'unit', 'system', or 'all'.
    Both <module> and <type> default to 'all', so simply running check_prep
    with no arguments will prepare all test subtrees in all modules.

    Examples:

      Prepare all subtress in all modules:
          tcbuild check_prep

      Prepare unit and system tests in common module:
          tcbuild check_prep common

      Prepare unit tests for all modules, but not system tests:
          tcbuild check_prep all unit

check_file <file>
    Runs set of tests specified in <file>.  Each line of the <file>
    can be blank, a comment (starting with '#'), or the fully
    qualified class name of the Java class containing a test.

check_list
    Runs a set of tests for each test subtree, as specified in a
    file called '<modulename</tests.(unit|system).lists.<listname>'
    for each module.

check_short
    Runs 'check_list short' - in other words, runs all tests in
    files called '<modulename>/tests.unit.lists.short' and
    '<modulename>/tests.system.lists.short'.

recheck
    Reruns whichever tests failed on last test run.

show_test_results <test_run_name>
    Prints analysis of exactly what failed in the named test run.
    <test_run_name> is the name of a test run (e.g., 'testrun-0007')
    or just 'latest'.

PACKAGING & DISTRIBUTION

NOTE: Output of binaries are placed under code/base/build/dist

dist
    Create distribution binaries

dist web
    Create eclipse plugin binaries

create_package <product_code>
    Assembles and packages the kit. Product codes: dso, web
    Default product_code is dso.

create_all_packages
    Assembles, packages, and publishes all possible kits, based on the
    configuration files found under the code/base/buildconfig/distribution
    directory.

dist_jars <product_code> <distribution_type>
    Acts like the dist target but will only build the jar files that will be found
    in a kit.

RUNNING SERVERS, CLASSES, ETC.

run_class <class_name> [args...]
    Runs the named class.  <class_name> must be a fully-qualified
    class name.  Any further arguments are passed to that class.

run_server
    Runs the Terracotta server. The 'jvmargs' parameter can be
    used to set JVM arguments for it, if you need any extra ones.


MISCELLANEOUS

boot_jar_path=/path/to/your/boot/jar
   Specify this option if you want to run your test with your own bootjar

create_boot_jar <jvm_type>
    Creates a boot JAR file for <jvm_type>
    <jvm_type> can be '1.4', '1.5', or the fully-qualified path
    to a Java home.

show_config
    Prints the configuration parameters that the build system
    itself is building with.

generate_config_classes
    Generates XMLBeans against the Terracotta schema


NOTE: To bypass Ivy dependency resolution, pass the --no-ivy option anywhere
on the tcbuild command-line.  For example:

    ./tcbuild --no-ivy compile
END_HELP_MESSAGE

end # module Resources
