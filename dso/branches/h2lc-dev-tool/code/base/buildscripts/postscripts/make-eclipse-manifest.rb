#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - ensure that the contents of the META-INF/MANIFEST.MF file for the
  #   ecliplse plugin contains the up-to-date information in it's 
  #   Bundle-ClassPath entry
  protected
  def postscript(ant, build_environment, product_directory, *args)
    relative_libpath     = args[0]
    eclipse_directory    = FilePath.new(product_directory.to_s, 'eclipse')
    dso_directory        = FilePath.new(eclipse_directory.to_s, 'org.terracotta.core')
    common_lib_directory = FilePath.new(dso_directory.to_s, *relative_libpath.split('/'))

    plugin_version = createVersionString(build_environment)

    meta_directory = FilePath.new(dso_directory, 'META-INF').ensure_directory
    manifest_path = FilePath.new(meta_directory, 'MANIFEST.MF')
    File.open(manifest_path.to_s, 'w') do |out|
      out.puts "Manifest-Version: 1.0"
      out.puts "Eclipse-LazyStart: true"
      out.puts "Bundle-ManifestVersion: 2"
      out.puts "Bundle-Name: Terracotta Core Plugin"
      out.puts "Bundle-SymbolicName: org.terracotta.core; singleton:=true"
      out.puts "Bundle-Version: " + plugin_version
      out.puts "Bundle-Vendor: Terracotta, Inc."
      out.puts "Bundle-RequiredExecutionEnvironment: J2SE-1.5"

      libfiles = Dir.entries(common_lib_directory.to_s).delete_if { |item| /\.jar$/ !~ item } << "resources/"
      libfiles.sort!
      out.puts "Bundle-ClassPath: #{relative_libpath}/#{libfiles.first},"
      libfiles[1..-1].each { |item| out.puts " #{relative_libpath}/#{item}," }
      out.puts " #{relative_libpath}/resources"

      out.puts "Export-Package: com.tc.admin,"
      out.puts " com.tc.admin.common,"
      out.puts " com.tc.asm,"
      out.puts " com.tc.aspectwerkz.expression,"
      out.puts " com.tc.aspectwerkz.reflect,"
      out.puts " com.tc.aspectwerkz.reflect.impl.java,"
      out.puts " com.tc.backport175.bytecode,"
      out.puts " com.tc.bundles,"
      out.puts " com.tc.config,"
      out.puts " com.tc.config.schema,"
      out.puts " com.tc.config.schema.builder,"
      out.puts " com.tc.config.schema.dynamic,"
      out.puts " com.tc.exception,"
      out.puts " com.tc.management.beans,"
      out.puts " com.tc.modules,"
      out.puts " com.tc.object,"
      out.puts " com.tc.object.appevent,"
      out.puts " com.tc.object.bytecode,"
      out.puts " com.tc.object.bytecode.aspectwerkz,"
      out.puts " com.tc.object.config,"
      out.puts " com.tc.object.config.schema,"
      out.puts " com.tc.object.logging,"
      out.puts " com.tc.object.tools,"
      out.puts " com.tc.object.util,"
      out.puts " com.tc.plugins,"
      out.puts " com.tc.properties,"
      out.puts " com.tc.server,"
      out.puts " com.tc.util,"
      out.puts " com.tc.util.concurrent,"
      out.puts " com.tc.util.event,"
      out.puts " com.tc.util.runtime,"
      out.puts " com.terracottatech.config,"
      out.puts " org.apache.commons.io,"
      out.puts " org.apache.commons.lang,"
      out.puts " org.apache.xmlbeans"
    end  

    destdir = dso_directory.to_s + "_" + plugin_version
    ant.move(:file => dso_directory.to_s, :tofile => destdir.to_s)
  end
  
  def createVersionString(build_environment)
    # eclipse plugin standard
    # 3.2.2.r322_v20070109
    raw_version = get_config(:version, build_environment.maven_version)
    if raw_version =~ /trunk/
      raw_version = build_environment.maven_version
    end
    tokens = raw_version.split(/-/).delete_if { |t| t =~ /rev/ }    
    if tokens.first =~ /^\d+\.\d+/
      version_number = tokens.first
      tokens.delete_at(0)
    else
      version_number = "1.0.0"
    end    
    
    # make sure version number has pattern /^\d+\.\d+\.\d+/
    version_number = "#{version_number}.0" unless version_number =~ /^\d+\.\d+\.\d+/
    
    # add revision number and timestamp
    tokens << "r#{build_environment.os_revision}"
    tokens << "v#{Time.now.strftime('%Y%m%d%H%M%S')}"
    
    version = version_number
    version = "#{version_number}.#{tokens.join('_')}"

    fail("version string #{version} doesn't conform to eclipse standard") unless version =~ /^\d+\.\d+\.\d+\.[^\.]/
    version
  end
  
end
