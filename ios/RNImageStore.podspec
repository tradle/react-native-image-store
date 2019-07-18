
Pod::Spec.new do |s|
  s.name         = "RNImageStore"
  s.version      = "1.0.0"
  s.summary      = "RNImageStore"
  s.description  = <<-DESC
                  RNImageStore
                   DESC
  s.homepage     = "https://github.com/tradle/react-native-image-store"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNImageStore.git", :tag => "master" }
  s.source_files  = "RNImageStore/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  
