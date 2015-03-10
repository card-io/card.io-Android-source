# Official card.io Android release process 

This is probably only useful for maintainers.

Public github address (https://github.com/card-io/card.io-Android-SDK)

1. Make sure you're using NDK r10d!  Ndk 8 is broken for some devices.  

1. Sanity check the master branch.  

1. Run `./gradlew clean :card.io:assembleRelease releaseDoc`, and review the unobfuscated aar and javadocs for any anomalies.
 
1. Deploy release version of SampleApp with `./gradlew installRelease` to a device and run a few sanity checks.  You will have to generate keystores to do this.

1. Run `git checkout -b release/1.2.3 master` with a release version

1. Update `release_notes.md` and commit.

1. Run `fab sdk_reset sdk_release`, it will reset the branches, build the sdk in the `distribution-repo` folder, tag the release, and deploy to mavenCentral.

1. Switch to master and merge release branch
	```bash
	    git checkout master
	    git merge --no-ff release/1.2.3
	```

1. Now push the changes to origin repo
	```bash
	    git push origin master --tags
	```

1. Wait for tests to complete and let's release to public, check the diffs to verify all is good
	1. Check the changes
		```
		    cd distribution-repo;
		    git show
		```
	2. Promote the repo in maven central
	    2. Open [Sonatype](https://oss.sonatype.org/), and
	    2. Follow [these instructions](http://central.sonatype.org/pages/releasing-the-deployment.html)
	    2. Only when the [card.io maven repo](https://repo1.maven.org/maven2/io/card/android-sdk/) lists this release, proceed to next step.  Otherwise, the sample app won't compile. 
	3. Push
		```
		    git push public master --tags
		```

1. Update javadocs
	1. On public repo, checkout special gh branch called gh-pages:
		2. `git checkout gh-pages`
		2. ``` rm -rf `ls | grep -v *.md` ```
	1. On private sdk rep, build javadoc and copy to public repo:
		2. `./gradlew releaseDoc`
		2. `cp -a card.io/build/docs/javadoc/release/* <public repo dir>`
	1. On public repo, push the changes:
		2. `git push` 

1. Check github that all is good.

1. Post that a new release is available using the same format previously used to:
	1. https://github.com/card-io/card.io-Android-SDK/releases
	2. Twitter (username=cardio/password=_ask for password_)
	3. Google Groups (send email to card-io-sdk-announce@googlegroups.com) 
