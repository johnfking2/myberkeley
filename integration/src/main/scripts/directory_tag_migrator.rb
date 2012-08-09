#!/usr/bin/env ruby

require 'rubygems'
require 'optparse'
require 'csv'
require 'json'
require 'digest/sha1'
require 'logger'
require 'nakamura'
require_relative 'ucb_data_loader'


module MyBerkeleyData
    TAG_SEARCH_URL = 'var/search/bytag.tidy.json?tag=directory*&page=0&items=100&_charset_=utf-8'
    SAKAI_TAGS = "sakai:tags"
    SLING_RESOURCE_TYPE = "sling:resourceType"
    POOLED_CONTENT = "sakai/pooled-content"
    USER_PROFILE = "sakai/user-profile"
    GROUP_PROFILE = "sakai/group-profile"
    
  class DirectoryTagMigrator
    
    def initialize(options)
      @log = Logger.new(STDOUT)
      @log.level = Logger::DEBUG
      @sling = Sling.new(options[:appserver], true)
      @user_manager = UserManager.new(@sling)
      real_admin = User.new("admin", options[:adminpwd])
      @sling.switch_user(real_admin)
      @sling.do_login
      @directory_tags_map = parse_mapping(options[:map_data_file])
      test_data_file = options[:testdatafile] 
      if test_data_file
        @tags_used = JSON.load(File.open(test_data_file, "r"))
      else
        @tags_used = get_directory_tags_used
      end
      @dry_run = options[:dryrun]
    end
      
    def parse_mapping(map_data_file)
      category_map = {}
      CSV.foreach(map_data_file) do |row|
        category_map[row[0].to_sym] = row[1]
      end
      return category_map
    end
    
    def get_directory_tags_used
      res = @sling.execute_get(@sling.url_for(TAG_SEARCH_URL))
      if (res.code == "200")
        @tags_used = JSON.parse res.body
      else
        @log.error "Not able to get used tags: #{res.code}, #{res.body}"
        return "Unknown"
      end
    end
    
    def process_tags
      results = @tags_used["results"]
      results.each do |result|
        resource_type = result[SLING_RESOURCE_TYPE]
        if POOLED_CONTENT.eql? resource_type
          migrate_content_tags result
        elsif (USER_PROFILE.eql? resource_type) || (GROUP_PROFILE.eql? resource_type)
          migrate_profile_tqgs result
        else
          @log.error("can't determing resourceType for item at path #{result[_path]}")
        end
      end
    end
      
    def migrate_content_tags(content_result)
      @log.debug("processing content: #{content_result}")
      old_tags = content_result[SAKAI_TAGS]
      path = "/p/" + content_result["_path"]
      @log.info("current tags at: #{path} are: #{old_tags}")
      if old_tags
        update_tags old_tags, path
        # need to use selectors so as not to dowmload the content itself
        new_tags_response = @sling.execute_get(@sling.url_for(path + ".tidy.json"))
        if ("200".eql? new_tags_response.code)
          content_json = JSON.parse new_tags_response.body
          @log.debug "full content json: #{content_json   }"
          new_tags = content_json[SAKAI_TAGS]
          @log.info("new tags at: #{path} are: #{new_tags}")
        else
          @log.error("could not retrieve new tags from #{path + '.tidy.json'}")
        end
      else
        @log.warn("No #{SAKAI_TAGS} at #{path}")
      end   
    end
    
    def migrate_profile_tqgs(profile_result)
      @log.debug("processing profile: #{profile_result}")
      profile_poth = profile_result["_path"]
      top_level_old_tags = profile_result[SAKAI_TAGS]
      if top_level_old_tags
        @log.info("curreent tags at: #{profile_poth} are: #{top_level_old_tags}")
        update_tags top_level_old_tags, profile_poth
        new_tags_response = @sling.execute_get(@sling.url_for(profile_poth))
        if ("200".eql? new_tags_response.code)
          profile_json = JSON.parse new_tags_response.body
          @log.debug "profile json: #{profile_json}"
          new_tags = profile_json[SAKAI_TAGS]
          @log.info("new tags at: #{profile_poth} are: #{new_tags}")
        else
          @log.error("could not retrieve new tags from #{profile_poth}")
        end
      else
        @log.warn("No #{SAKAI_TAGS} at #{path}")
      end
    end
    
    def update_tags old_tags, path
      i = 0;
      old_tags.each do |old_tag|
        new_tag = @directory_tags_map[old_tag.to_sym]
        if new_tag
          update_tag old_tag, new_tag, path
        end
      end
    end
    
    def update_tag(old_tag, new_tag, path)
      tags_url = @sling.url_for path
      delete_tag_params = {}
      delete_tag_params[":operation"] = "deletetag"
      delete_tag_params["key"] = old_tag
      
      add_tag_params = {}
      add_tag_params[":operation"] = "tag"
      add_tag_params["key"] = "/tags/" + new_tag
      if @dry_run
        @log.debug "dry run, would delete old_tag first at #{tags_url} with params #{delete_tag_params}"
        if "DELETE".eql? new_tag
          @log.debug "dry run,.new_tag is DELETE, only deleting old tag"
        else
          @log.debug "dry run, would add new_tag at #{tags_url} with params #{add_tag_params}"
        end
      else
        @log.debug("deleting tag at #{path} with params: #{delete_tag_params}")
        res = @sling.execute_post(@sling.url_for(path), delete_tag_params)
        if ("404".eql? res.code)
          @log.info("delete tag failed at #{path} with params: #{delete_tag_params} with response.code #{res.code}")
          #because tag does not exist in tag store so recreate it there by tagging item again
          # DeleteTagOperation.java refuses to delete a tag not already in the tag store
          recreate_params = {}
          recreate_params[":operation"] = "tag"
          recreate_params["key"] = old_tag
          @log.debug("recreating tag at #{path} with params: #{recreate_params}")
          res = @sling.execute_post(@sling.url_for(path), recreate_params)
          @log.debug("recreating post response.code #{res.code}")
          # now attempt to delete after recreation
          if ("200".eql? res.code)
            @log.debug("after recreations, deleting tag at #{path} with params: #{delete_tag_params}")
            res = @sling.execute_post(@sling.url_for(path), delete_tag_params)
          end
        end
        if ("200".eql? res.code)
          if "DELETE".eql? new_tag
            @log.debug "new_tag is DELETE, only deleting old tag"
          else
            @log.debug("adding tag at #{path} with params: #{add_tag_params}")
            res = @sling.execute_post(@sling.url_for(path), add_tag_params)
            if (!"200".eql? res.code)
              @log.error("add tag failed at #{path} with params: #{add_tag_params} with response.code #{res.code}")
            end
          end
        else
          @log.error("delete tag failed at #{path} with params: #{delete_tag_params} with response.code #{res.code}")
        end
      end
    end
  end  # class
end  # module

if ($PROGRAM_NAME.include? 'directory_tag_migrator.rb')
  options = {}
  optparser = OptionParser.new do |opts|
    opts.banner = "Usage: directory_tqg_migrator.rb [options]"

    # trailing slash is mandatory
    options[:appserver] = "http://localhost:8080/"
    opts.on("-a", "--appserver [APPSERVER]", "Application Server") do |as|
      options[:appserver] = as
    end

    options[:adminpwd] = "admin"
    opts.on("-q", "--adminpwd [ADMINPWD]", "Application Admin User Password") do |ap|
      options[:adminpwd] = ap
    end
    
    options[:dryrun] = "true"
    opts.on("-d", "--dryrun [DRYRUN]", "Dry run true means just report changes to be made but do not make them") do |dr|
      if "true".eql? dr
        options[:dryrun] = true
      elsif "false".eql? dr
        options[:dryrun] = false
      end
    end
    
    options[:map_data_file] = "category_map.csv"
    opts.on("-f", "--map_file [MAPFILE]", "csv file of new mappings, old directory tag to new directory tag") do |mf|
      options[:map_data_file] = mf
    end
    
    #options[:test_data_file] = "tag_test_data.json"
    opts.on("-t", "--testdatafile [DATAFILE]", String, "json file of sample tags used data for testing") do |td|
      options[:testdatafile] = td
    end
  end

  optparser.parse ARGV
    
  dtm = MyBerkeleyData::DirectoryTagMigrator.new options
  dtm.process_tags
end