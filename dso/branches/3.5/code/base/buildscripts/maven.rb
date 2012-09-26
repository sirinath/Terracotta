module Maven
  def maven(goals, *args)
    mvn_cmd = [FilePath.new('mvn').batch_extension.to_s]
    mvn_cmd << goals
    mvn_cmd += args
    puts(mvn_cmd.join(' '))
    result = system(*mvn_cmd)
    unless result
      raise "mvn command failed: #{mvn_cmd.join(' ')}"
    end
  end
end
