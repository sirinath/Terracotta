#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected
  def make_package(srcdir, destdir, filename, install_name, internal_name)
    puts "-"*80
    puts "pwd     : #{Dir.pwd}"
    puts "srcdir  : #{srcdir}"
    puts "destdir : #{destdir}"
    puts "filename: #{filename}"
    puts "filename: #{install_name}"
    puts "filename: #{internal_name}"
    puts @static_resources.izpack_installer_template.canonicalize.to_s
    puts "-"*80
    
    # build the IzPack installer definition file
    template = File.read(@static_resources.izpack_installer_template.canonicalize.to_s)
    files    = Dir.entries(srcdir.to_s).delete_if { |entry| entry =~ /^\./ }
    scripts  = scriptfiles(srcdir.to_s) { |entry| entry =~ /\.(sh|bat)$/ }
    scripts.collect! { |entry| FilePath.new(entry).relative_path_from(srcdir) }
    
    template = ERB.new(template, 0, "%<>").result(binding)
    config   = File.join(destdir.to_s, 'installer.xml')
    File.open(config, 'w') { |out| out << template }

    # install IzPack as an Ant task
    ant.taskdef(:name => 'izpack', :classname => 'com.izforge.izpack.ant.IzPackTask') 

    # build the package
    installer_package_name = FilePath.new(destdir.to_s, "#{filename}.jar")
    ant.izpack(
      :installerType    => 'standard', 
      :inheritAll       => true, 
      :compressionlevel => 9, 
      :compression      => 'deflate', 
      :basedir          => srcdir.to_s, 
      :input            => config, 
      :output           => installer_package_name)
  end
  
  private
  def scriptfiles(srcdir)
    result   = []
    excludes = []
    Find.find(srcdir) do |path|
      if FileTest.directory?(path)
        next unless excludes.include?(File.basename(path))
        Find.prune
      else
        next if block_given? && !yield(path) 
        result << path
      end
    end
    result.reverse
  end
end
