
Pod::Spec.new do |s|
  s.name         = "RNFileSystem"
  s.version      = "0.0.8"
  s.summary      = "RNFileSystem"
  s.description  = "React native File System"
  s.homepage     = "https://github.com/earthlingstd/react-native-file-system"
  s.license      = "MIT"
  s.author       = { "author" => "author@domain.cn" }
  s.platform     = :ios, "9.0"
  s.source       = { :git => "https://github.com/earthlingstd/react-native-file-system.git", :tag => "master" }
  s.source_files  = "ios/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"

end

