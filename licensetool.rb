#!/usr/bin/env ruby

require 'json'

unless ARGV[0] && ARGV[1]
  puts "Usage: #{__FILE__} <licenses.json> <output file>"
  exit 1
end

in_file = File.open(ARGV[0], 'r')
out_file = File.open(ARGV[1], 'w')

out_file.write <<~TEXT
  This project would not exist without the network of excellent open source
  packages it depends on.

TEXT

json = JSON.parse(in_file.read)

json
  .keys
  .sort
  .map { |k| [k, json[k]] }
  .each do |p_name, package|
  p_link = package['repository']
  p_publisher = package['publisher']
  p_licenses = package['licenses']
  p_license_file = package['licenseFile']

  text = <<~TEXT
    #{p_name}: #{p_publisher}
      Link:    #{p_link}
      License: #{p_licenses}
      License Text:
      -------------
      #{File.read(p_license_file)}
      -------------

  TEXT

  out_file.write(text)
end
