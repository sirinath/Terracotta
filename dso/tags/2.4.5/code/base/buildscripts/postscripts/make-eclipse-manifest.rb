#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
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
    dso_directory        = FilePath.new(eclipse_directory.to_s, 'org.terracotta.dso')
    common_lib_directory = FilePath.new(dso_directory.to_s, *relative_libpath.split('/'))

    plugin_version = createVersionString(build_environment)

    meta_directory = FilePath.new(dso_directory, 'META-INF').ensure_directory
    manifest_path = FilePath.new(meta_directory, 'MANIFEST.MF')
    File.open(manifest_path.to_s, 'w') do |out|
      out.puts "Manifest-Version: 1.0"
      out.puts "Eclipse-LazyStart: true"
      out.puts "Bundle-ManifestVersion: 2"
      out.puts "Bundle-Name: Terracotta Plugin"
      out.puts "Bundle-SymbolicName: org.terracotta.dso; singleton:=true"
      out.puts "Bundle-Version: " + plugin_version
      out.puts "Bundle-Vendor: Terracotta, Inc."
      out.puts "Bundle-Localization: plugin"
      out.puts "Require-Bundle: org.eclipse.ui.editors,"
      out.puts " org.eclipse.debug.core,"
      out.puts " org.eclipse.debug.ui,"
      out.puts " org.eclipse.jdt.core,"
      out.puts " org.eclipse.jdt.debug,"
      out.puts " org.eclipse.jdt.debug.ui,"
      out.puts " org.eclipse.jdt.launching,"
      out.puts " org.eclipse.ui.workbench.texteditor,"
      out.puts " org.eclipse.core.resources,"
      out.puts " org.eclipse.jface.text,"
      out.puts " org.eclipse.core.runtime,"
      out.puts " org.eclipse.ui,"
      out.puts " org.eclipse.ui.ide,"
      out.puts " org.eclipse.jdt.ui,"
      out.puts " org.eclipse.help,"
      out.puts " org.eclipse.update.core,"
      out.puts " org.eclipse.ltk.core.refactoring,"
      out.puts " org.eclipse.ui.console,"
      out.puts " org.eclipse.search,"
      out.puts " org.eclipse.pde.core,"
      out.puts " org.eclipse.pde.ui,"
      out.puts " org.junit,"
      out.puts " org.eclipse.jdt.junit,"
      out.puts " org.eclipse.core.variables"

      libfiles = Dir.entries(common_lib_directory.to_s).delete_if { |item| /\.jar$/ !~ item }
      out.puts "Bundle-ClassPath: #{relative_libpath}/#{libfiles.first},"
      libfiles[1..-2].each { |item| out.puts " #{relative_libpath}/#{item}," }
      out.puts " #{relative_libpath}/#{libfiles.last}"
      out.puts "Bundle-Activator: org.terracotta.dso.TcPlugin"
    end  

    destdir = dso_directory.to_s + "_" + plugin_version
    ant.move(:file => dso_directory.to_s, :tofile => destdir.to_s)
  end
  
  def createVersionString(build_environment)
    # eclipse plugin standard
    # 3.2.2.r322_v20070109
    raw_version = get_config(:version, "1.0.0")
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
    tokens << "r#{build_environment.current_revision}"
    tokens << "v#{Time.now.strftime('%Y%m%d')}"
    
    version = version_number
    version = "#{version_number}.#{tokens.join('_')}"

    fail("version string #{version} doesn't conform to eclipse standard") unless version =~ /^\d+\.\d+\.\d+\.[^\.]/
    version
  end
  
end
