module BuildData
  # Where should we put our build-data file?
  def build_data_file(build_results)
    FilePath.new(build_results.classes_directory(self))
  end

  # Creates a 'build data' file at the given location, putting into it a number
  # of properties that specify when, where, and how the code in it was compiled.
  def create_build_data(config_source, build_results, build_environment, destdir=build_data_file(build_results))
    File.open(FilePath.new(destdir, "build-data.txt").to_s, "w") do |file|
      file.puts("terracotta.build.productname=terracotta")
      file.puts("terracotta.build.version=#{build_environment.version}")
      file.puts("terracotta.build.maven.artifacts.version=#{build_environment.maven_version}")
      file.puts("terracotta.build.host=#{build_environment.build_hostname}")
      file.puts("terracotta.build.user=#{build_environment.build_username}")
      file.puts("terracotta.build.timestamp=#{build_environment.build_timestamp.strftime('%Y%m%d-%H%m%S')}")
      file.puts("terracotta.build.revision=#{build_environment.current_revision}")
      file.puts("terracotta.build.branch=#{build_environment.current_branch}")
      file.puts("terracotta.build.edition=#{build_environment.edition}")

      # extra info if built under EE branch
      if build_environment.ee_svninfo
        file.puts("terracotta.build.ee.revision=#{build_environment.ee_svninfo.current_revision}")
      end
    end
  end
end