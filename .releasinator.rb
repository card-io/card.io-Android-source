#### releasinator config ####

# List of items to confirm from the person releasing.  Required, but empty list is ok.
configatron.prerelease_checklist_items = [
  "Test on a device in release mode.",
  "Sanity check the master branch.",
  "Review the unobfuscated aar and javadocs for any anomalies."
]

def validate_ndk_version()
  expected_release = "r10e (64-bit)"
  actual_release = `cat local.properties | sed 's/\\\./_/g' > .properties.file; . .properties.file; x=\`cat $ndk_dir/RELEASE.TXT\`; rm -rf .properties.file; echo $x`.strip
  if expected_release != actual_release
    abort("failed ndk version verification".red)
  else
    puts "validate ndk version: found expected version: '#{actual_release}'."
  end
end

# Custom validation methods.  Optional.
configatron.custom_validation_methods = [
  method(:validate_ndk_version)
]

# The directory where all distributed docs are.  Default is '.'.
configatron.base_docs_dir = 'sdk'

def build_cardio()
  command("./gradlew clean :card.io:assembleRelease releaseDoc", live_output=true)
end

# The method that builds the sdk.  Required.
configatron.build_method = method(:build_cardio)

def publish_to_maven(version)
  command("./gradlew :card.io:uploadArchives", live_output=true)
  command("./gradlew :card.io:closeRepository", live_output=true)
  command("sleep 60")
  command("./gradlew :card.io:promoteRepository", live_output=true)
  wait_for("wget -U \"non-empty-user-agent\" -qO- http://central.maven.org/maven2/io/card/android-sdk/ | grep #{version}")
end

# The method that publishes the sdk to the package manager.  Required.
configatron.publish_to_package_manager_method = method(:publish_to_maven)


def replace_version(new_tag)
  replace_string("./SampleApp/build.gradle", "REPLACE_VERSION", "#{new_tag}")
  replace_string("./README.md", "REPLACE_VERSION", "#{new_tag}")
end

def compile_sample_app()
  Dir.chdir("SampleApp") do
    command("gradlew clean assembleDebug", live_output=true)
  end
end

# Distribution GitHub repo if different from the source repo. Optional.
configatron.downstream_repos = [
  DownstreamRepo.new(
    name="card.io-Android-SDK",
    url="git@github.com:card-io/card.io-Android-SDK.git",
    branch="master",
    files_to_copy=[
      CopyFile.new("card.io/build/outputs/aar/card.io-release.aar", "card.io-__VERSION__.aar", "aars"),
      CopyFile.new("SampleApp", "SampleApp", ".")
    ],
    post_copy_methods=[
      method(:replace_version)
    ],
    build_methods=[
      method(:compile_sample_app)
    ]
  )
]

def build_docs()
  command("./gradlew releaseDoc", live_output=true)
end

configatron.doc_build_method = method(:build_docs)
configatron.doc_target_dir = "downstream_repos/card.io-Android-SDK"
configatron.doc_files_to_copy = [
  CopyFile.new("card.io/build/docs/javadoc/release/*", ".", ".")
]