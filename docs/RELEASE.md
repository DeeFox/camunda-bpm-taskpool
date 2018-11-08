# Release Management

Release management has been set-up using Travis CI and Sonatype Nexus (= Maven Central)

## Continuous Integration

Travis CI is building all branches on commit hook.

## What modules get deployed

Every module is enabled by default. If you want to change this, please provide the property

     <skipNexusStagingDeployMojo>true</skipNexusStagingDeployMojo>
   
inside your `pom.xml`.


## Trigger a deploy

Travis will deploy the results of the build if and only if:
- the build is stable
- the branch is `release`
- the commit contains a git tag

So, to trigger a deploy:

- Merge your changes to `release` branch (to merge from master: `release> git merge master`)
- Tag the changes `release> git tag -a 1.1.0-SNAPSHOT -m "Deploy of 1.1.0-SNAPSHOT" && git push --tags`

## SNAPSHOT vs. STABLE

If you deploy a SNAPSHOT version, the artifact ends up in the snapshot repository. If you want a release
in the Maven Central Repository, make sure to create a STABLE version (e.g. 2.1.3) and deploy it. Don't 
forget to close the release repository in the OSS Nexus.


## Changing versions (SNAPSHOT)

Currently, no automatic version bumping is set-up. If you want a new version, make sure to bump it 
on your own:

    ./mvnw release:update-versions -DautoVersionSubmodules=true -DdevelopmentVersion=1.2.0-SNAPSHOT


## Changing version (STABLE)

    ./mvnw org.codehaus.mojo:versions-maven-plugin:2.5:set -DgenerateBackupPoms=false -DnewVersion=1.0.6 
    



## References

* https://www.phillip-kruger.com/post/continuous_integration_to_maven_central/ (primary)
* https://docs.travis-ci.com/user/deployment
* https://blog.travis-ci.com/2017-03-30-deploy-maven-travis-ci-packagecloud/


      
