#!/usr/bin/env python

import os
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
env.public_repo_path = os.path.join("public", "card.io-Android-SDK")

# other important paths
env.public_repo_sample_app_path = os.path.join(env.public_repo_path, "SampleApp")


# --- tasks ----
def verbose(verbose=True):
    env.verbose = verbose


def build():
    """
    build dist for card.io
    """
    cmd = "ant dist"

    with lcd(os.path.join(env.top_root, "card.io")):
        local(cmd)


def sdk_setup():
    env.top_root = local("git rev-parse --show-toplevel", capture=True)

    if not os.path.isdir("public"):
        local("mkdir -p public")

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


def sdk_release():
    """
    Build library and header files into public/card.io-Android-SDK.
    """
    version_str = _get_release_version()
    cmd = "git tag sdk-{0}".format(version_str)
    # tag the build
    local(cmd)
    
    dist(version_str)

    print "Commit proguard-data"
    print "Verify and merge back to master"
    print


def dist(version_str):

    # execute setup
    execute(sdk_setup)

    # Add version
    with settings(hide(*env.to_hide)):

        print(colors.blue("building sdk {version_str} ".format(**locals())))
        # execute build
        execute(build)
        print(colors.blue("extracting sdk {version_str} to public repo".format(**locals())))
        # card.io-android-sdk-
        release_path = os.path.join(env.top_root, "card.io", "dist")
        zip_file = local("ls -t " + release_path + " | head -n 1", capture=True)
        zip_path = os.path.join(release_path, zip_file)

        dist_zip = zipfile.ZipFile(zip_path, 'r')
        dist_zip.extractall(path=env.public_repo_path)

        with lcd(env.public_repo_path):
            # update .md files with those in repo
            local("rm *.md")
            local("cp " + os.path.join(env.top_root, "sdk") + "/*.md .")

            # update sample app
            local("rm -rf SampleApp")
            local("cp -R " + os.path.join(env.top_root, "SampleApp") + " .")
            dist_zip.extractall(path=env.public_repo_sample_app_path)

            #add everything to git
            local("git add .")
            local("git add -u .")

    print
    io.success("Success!")
    print "The distribution files are now available in {public_repo_path}".format(**env)
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
