#!/usr/bin/env python

import os
import shutil
import zipfile

from fabric.contrib.console import confirm
from fabric.api import env, local, hide
from fabric.context_managers import lcd, settings
from fabric.utils import abort
from fabric.tasks import execute
from fabric import colors

env.verbose = False
env.dirty = False
env.test = False
env.to_hide = ["stdout", "stderr", "running"]

# repos
env.public_repo_url = "git@github.com:card-io/card.io-Android-SDK.git"
env.public_repo_path = "distribution-repo"


# --- tasks ----
def verbose(verbose=True):
    env.verbose = verbose


def build(is_upload_archives):
    cmd = "./gradlew clean :card.io:assembleRelease releaseDoc"

    if is_upload_archives == True:
        cmd += " :card.io:uploadArchives"

    with lcd(env.top_root):
        print "running " + cmd
        local(cmd)


def sdk_setup():
    env.top_root = local("git rev-parse --show-toplevel", capture=True)

    if not os.path.isdir(env.public_repo_path):
        print(colors.blue("Creating public SDK repo"))
        local("git clone --origin public {public_repo_url} {public_repo_path}".format(**env))


def sdk_reset(warn_opt='warn'):
    if warn_opt != 'nowarn':
        print(colors.yellow("This step will fetch and reset the public repo to the latest version."))
        if not confirm("Proceed?"):
            abort("OK, fine. I understand. :(")

    execute(sdk_setup)

    with lcd(env.public_repo_path):
        local("git checkout master")

        # Merge in the internal repo's develop branch.
        local("git fetch public --prune")
        local("git reset --hard public/master")
        local("git clean -x -d -f")


def sdk_release(is_upload_archives=True):
    """
    Build library into public/card.io-Android-SDK.
    """

    execute(sdk_setup)

    version_str = _get_release_version()

    _confirm_tag_overwrite(env.top_root, version_str)
    local("git tag -f {0}".format(version_str))


    with settings(hide(*env.to_hide)):
        print(colors.blue("building sdk {version_str} ".format(**locals())))

        build(is_upload_archives)
        print(colors.blue("extracting sdk {version_str} to public repo".format(**locals())))

        release_path = os.path.join(env.top_root, "card.io", "build", "outputs", "aar", "card.io-release.aar")
        dest_file_name = "card.io-{version_str}.aar".format(**locals())

        with lcd(env.public_repo_path):
            # remove old everything
            local("rm -rf *")
            local("mkdir aars")
            local("cp {release_path} aars/{dest_file_name}".format(**locals()))

            # update all sdk files
            local("cp -r " + os.path.join(env.top_root, "sdk") + "/* .")
            local("cp -r " + os.path.join(env.top_root, "sdk") + "/.[!.]* .")

            # update sample app
            local("cp -R " + os.path.join(env.top_root, "SampleApp") + " .")

            local("sed -i '' 's/io.card:android-sdk:REPLACE_VERSION/io.card:android-sdk:{version_str}/g' ./SampleApp/build.gradle".format(**locals()))
            local("sed -i '' 's/io.card:android-sdk:REPLACE_VERSION/io.card:android-sdk:{version_str}/g' ./README.md".format(**locals()))

            # add everything to git and commit
            local("git add .")
            local("git add -u .")
            local("git commit -am \"Update library to {version_str}\"".format(**locals()))

        _confirm_tag_overwrite(env.public_repo_path, version_str)

        with lcd(env.public_repo_path):
            local("git tag -f {0}".format(version_str))

    print
    print(colors.white("Success!"))
    print "The distribution files are now available in {public_repo_path}".format(**env)
    print 
    if is_upload_archives == True:
        print "The aar file has been published to sonatype's mavenCentral staging repo.  Promote it!"
    print
    print "Commit proguard-data"
    print "Verify and merge back to master"
    print


# --- Helpers -------------------------------------------------------------


def _get_release_version():
    with settings(hide(*env.to_hide)):
        git_describe_cmd = 'git rev-parse --short --abbrev-ref HEAD'
        describe_results = local(git_describe_cmd, capture=True)
        if not "/" in describe_results:
            abort("cannot extract release version from branch.  Be sure you're on a branch with format \"release/1.1.1\"")
        version_str = describe_results.rsplit("/", 1)[1]
        return version_str


def _confirm_tag_overwrite(repo, tag):
    with settings(hide(*env.to_hide)):
        git_list_tag_command = 'git tag -l'
        with lcd(repo):
            tag_results = local(git_list_tag_command, capture=True)
            for row in tag_results.split('\n'):
                if tag == row:
                    print(colors.yellow("Tag {tag} already present in {repo}.".format(**locals())))
                    if not confirm("Proceed with overwriting tag?"):
                        abort("Tag not overwritten, aborted. ")