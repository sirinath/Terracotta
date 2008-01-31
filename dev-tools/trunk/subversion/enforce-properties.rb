#!/usr/bin/env ruby

module Config
  # Where to find the svnlook program
  SVNLOOK = '/usr/bin/svnlook'

  # The error message displayed to the user if newly-added files in their commit
  # do not have the required properties
  ERROR_MESSAGE = "
  One or more files being added does not have the required properties.  The
  easiest way to ensure that newly added files have the required properties
  is to use the auto-props feature of the Subversion config file.  A good
  option is to use the Subversion config file in the Terracotta repository,
  as it has been pre-configured with the required property mappings:

    http://svn.terracotta.org/svn/tc/dev-tools/trunk/subversion/config
  "

  # File extensions for files that should have the svn:eol-style property set to
  # 'native'
  EOL_NATIVE_EXTENSIONS = %w(
    c c\\+\\+ cxx cpp h hxx hpp java jsp properties tld rb rbx rbw rhtml rxml rjs py
    html css js cgi fcgi xml dtd sql txt yaml yml y l xsd xsdconfig groovy)

  # Other file names that should  have the svn:eol-style property set to 'native'
  OTHER_EOL_NATIVE = %w(.project .classpath makefile rakefile readme)

  # An array of regular expressions constructed from EOL_NATIVE_EXTENSIONS and
  # OTHER_EOL_NATIVE.
  EOL_NATIVE_PATTERNS =
      EOL_NATIVE_EXTENSIONS.map { |ext| /\.#{ext}$/ } +
      OTHER_EOL_NATIVE.map { |filename| /^#{filename}$/ }

  # A mapping of file name patterns to the required value of svn:eol-style that
  # files matching the pattern must have.
  EOL_STYLE_MAP = {
    /\.sh$/ => 'LF',
    /\.bat$/ => 'CRLF'
  }
  EOL_NATIVE_PATTERNS.each do |pattern|
    EOL_STYLE_MAP[pattern] = 'native'
  end

  # A mapping of file name patterns to the required value of svn:mime-type that
  # files matching the pattern must have.
  MIME_TYPE_MAP = {
    /\.png$/ => 'image/png',
    /\.jpg$/ => 'image/jpeg',
    /\.gif$/ => 'image/gif'
  }
end # module Config

class SVNLook
  def initialize(svnlook_program, repository, opts = {})
    @svnlook = svnlook_program
    @repository = repository

    @args = ""
    if opts[:revision] && opts[:transaction]
      raise(ArgumentError, "Only one of :revision or :transaction may be used")
    elsif rev = opts[:revision]
      @args = "--revision #{rev}"
    elsif tx = opts[:transaction]
      @args = "--transaction #{tx}"
    end
  end

  ADDED_PATH = /^A...(.+)$/
  MODIFIED_PATH = /^U...(.+)$/
  DELETED_PATH = /^D...(.+)$/

  def changed_paths
    result = {
      :added => [], :modified => [], :deleted => []
    }
    changes = `#{@svnlook} changed #{@args} "#{@repository}"`
    changes.each do |line|
      if md = ADDED_PATH.match(line)
        result[:added] = md[1]
      elsif md = MODIFIED_PATH.match(line)
        result[:modified] = md[1]
      elsif md = DELETED_PATH.match(line)
        result[:deleted] = md[1]
      end
    end
    result
  end

  # Returns a Hash containing the property names and values for the given path.
  # If provided, the opts Hash should contain an entry for exactly one of
  # :revision or :transaction.
  def props_for_path(path)
    result = Hash.new
    cmd = "#{@svnlook} proplist #{@args} \"#{@repository}\" \"#{path}\""
    output = `#{cmd}`
    output.each do |name|
      name.strip!
      cmd = "#{@svnlook} propget #{@args} \"#{@repository}\" \"#{name}\" \"#{path}\""
      value = `#{cmd}`
      result[name] = value
    end
    result
  end
end

$config = Hash.new
require 'optparse'
opts = OptionParser.new
opts.on('-r', '--revision', '=REVISION') do |arg|
  $config[:revision] = arg
end

opts.on('-t', '--transaction', '=TRANSACTION') do |arg|
  $config[:transaction] = arg
end

rest = opts.parse(ARGV)
REPO = rest[0]
unless REPO
  raise("Repository must be specified")
end


if ($config[:revision] && $config[:transaction]) ||
    (!$config[:revision] && !$config[:transaction])
  raise("Exactly one of --revision or --transaction must be provided")
end


$svnlook = SVNLook.new(Config::SVNLOOK, REPO, $config)

$errors = Array.new

def enforce_eol_native(filename)
  Config::EOL_STYLE_MAP.each do |pattern, eol_style|
    if pattern.match(filename)
      unless $svnlook.props_for_path(filename)['svn:eol-style'] == eol_style
        $errors << "#{filename} must have svn:eol-style property set to '#{eol_style}'"
      end
    end
  end
end

def enforce_mime_type(filename)
  Config::MIME_TYPE_MAP.each do |pattern, mime_type|
    if pattern.match(filename)
      unless $svnlook.props_for_path(filename)['svn:mime-type'] == mime_type
        $errors << "#{filename} must have svn:mime-type property set to #{mime_type}"
        return
      end
    end
  end
end

$svnlook.changed_paths[:added].each do |path|
  enforce_eol_native(path)
  enforce_mime_type(path)
end

unless $errors.empty?
  STDERR.puts
  STDERR.puts($errors)
  STDERR.puts(Config::ERROR_MESSAGE)
  exit 1
end

