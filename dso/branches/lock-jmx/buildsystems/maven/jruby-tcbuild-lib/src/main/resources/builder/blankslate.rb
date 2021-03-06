#!/usr/bin/env ruby
#--
# Copyright 2004 by Jim Weirich (jim@weirichhouse.org).
# Copyright 2005 by Tim Azzopardi (tim@tigerfive.com).
# All rights reserved.

# Permission is granted for use, copying, modification, distribution,
# and distribution of modified versions of this work as long as the
# above copyright notice is included.
#++

module Builder

  # BlankSlate provides an abstract base class with no predefined
  # methods (except for <tt>\_\_send__</tt> and <tt>\_\_id__</tt>).
  # BlankSlate is useful as a base class when writing classes that
  # depend upon <tt>method_missing</tt> (e.g. dynamic proxies).
  class BlankSlate
    class << self
      def hide(name)
        undef_method name if
          instance_methods.include?(name.to_s) and 
           name !~ /^(__|instance_eval)/ and 
           name !~ /^(__|respond_to?)/
      end
    end

    instance_methods.each { |m| hide(m) }
  end
end

# Since Ruby is very dynamic, methods added to the ancestors of
# BlankSlate <em>after BlankSlate is defined</em> will show up in the
# list of available BlankSlate methods.  We handle this by defining a hook in the Object and Kernel classes that will hide any defined 
module Kernel
  class << self
    alias_method :blank_slate_method_added, :method_added
    def method_added(name)
      blank_slate_method_added(name)
      return if self != Kernel
      Builder::BlankSlate.hide(name)
    end
  end
end

class Object
  class << self
    alias_method :blank_slate_method_added, :method_added
    def method_added(name)
      blank_slate_method_added(name)
      return if self != Object
      Builder::BlankSlate.hide(name)
    end
  end
end
