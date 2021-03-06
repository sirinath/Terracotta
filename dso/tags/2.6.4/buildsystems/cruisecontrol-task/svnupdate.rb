
# this script is called by build-terracotta.xml to svn update
# it will try to call "svn update" 3 times before crapping out
require 'tmpdir'
require 'yaml'

class SvnUpdate

  def initialize(monkey_name)
    @monkey_name = monkey_name
    log(monkey_name)
    # get path to top folder of the repo
    @topdir = File.join(File.expand_path(File.dirname(__FILE__)), "..", "..")
    @topdir = File.join(@topdir, "..") if @topdir =~ /community/

    # default build-archive-dir in monkeys
    build_archive_dir = "/shares/monkeyoutput/archive"

    if ENV['OS'] =~ /win/i
      @topdir=`cygpath -u #{@topdir}`.chomp
      build_archive_dir = "o:/archive"
    end

    @svninfo = YAML::load(`svn info #{@topdir}`)
    @branch  = get_branch()

    @good_rev_file = File.join(build_archive_dir, "monkey-police", @branch, "good_rev.txt")

    clean_up_temp_dir
  end

  def get_branch    
    branch = case @svninfo['URL']
      when /trunk/: branch="trunk"
      when /branches\/private\/([^\/]+)/: $1
      when /branches\/([^\/]+)/: $1
      when /tags\/([^\/]+)/: $1
      else fail("Can't determine which branch I'm operating on")
    end
  end

  def log(msg)
    # things are good - turn off logging for now
    return
    File.open(File.join(Dir.tmpdir, "svnupdate.log"), "a") do |f|
      f.puts("#{Time.now}: #{msg}")
    end
  end

  def get_current_rev
    @svninfo["Last Changed Rev"].to_i
  end

  def svn_update_with_error_tolerant(revision)
    msg = ''
    command = "svn update -r#{revision} --non-interactive '#{@topdir}' 2>&1"
    3.downto(1) do
      log(command)
      msg = `#{command}`
      log(msg)
      return if $? == 0
      log("sleep 5 min after svn error")
      sleep(5*60)
      log("svn cleanup '#{@topdir}'")
      `svn cleanup '#{@topdir}'`
    end
    fail(msg)
  end

  def get_current_good_rev(file)
    currently_good_rev = 0
    begin
      File.open(file, "r") do | f |
        currently_good_rev = f.gets.to_i
      end
    rescue
      currently_good_rev = -1
    end
    currently_good_rev
  end

  def clean_up_temp_dir
    # clean out temp dir
    `rm -rf #{Dir.tmpdir}/terracotta*`
    `rm -rf /var/tmp/terracotta*`
    `rm -rf #{Dir.tmpdir}/open*`
    `rm -rf #{Dir.tmpdir}/*.dat`
    `rm -rf #{Dir.tmpdir}/sprint*`
    `rm -rf #{Dir.tmpdir}/asm*`
    `rm -rf #{Dir.tmpdir}/Jetty*`
  end

  def update
    while true
      current_rev = get_current_rev()
      current_good_rev = get_current_good_rev(@good_rev_file)

      log("curr: #{current_rev}")
      log("good: #{current_good_rev}")

      if @monkey_name == "monkey-police" || @monkey_name =~ /kit/ || current_good_rev == -1
        log("monkey-police - updating to HEAD")
        svn_update_with_error_tolerant("HEAD")
        exit(0)
      elsif current_rev < current_good_rev
        log("not monkey-police - updating to #{current_good_rev}")
        svn_update_with_error_tolerant(current_good_rev)
        exit(0)
      elsif current_rev == current_good_rev
        log("current_rev == currently_good_rev - no svn update needed")
        exit(0)
      else # I have a revision that is greater than a good known reivision, so I sleep and wait
        log("sleep 5 min waiting for good rev")
        sleep(5*60)
        log("awaken")
      end
    end
  end

end # class SvnUpdate

svn = SvnUpdate.new(ARGV[0])
svn.update
