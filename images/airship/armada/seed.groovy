import groovy.json.JsonSlurper

def imagesJson = '''{ "airship":[{
                        "repo":"airship",
                        "images":[
                                  "armada"]
                        }]}'''

folder("images/airship")
folder("images/airship/armada")

def jsonSlurper = new JsonSlurper()
def object = jsonSlurper.parseText(imagesJson)

for (entry in object.airship) {
    for (image in entry.images) {
      pipelineJob("images/${entry.repo}/${image}/${image}") {
            configure {
                node -> node / 'properties' / 'jenkins.branch.RateLimitBranchProperty_-JobPropertyImpl'{
                    durationName 'hour'
                    count '4'
                }
            }
            triggers {
                gerritTrigger {
                    serverName('ATT-airship-CI')
                    gerritProjects {
                        gerritProject {
                            compareType('PLAIN')
                            pattern("openstack/airship-${image}")
                            branches {
                                branch {
                                compareType("ANT")
                                pattern("**")
                                }
                            }
                            customUrl("$NEXUS3_URL/repository/att-comdev-jenkins-logs/att-comdev/${image}/\$BUILD_NUMBER/${image}-\$BUILD_NUMBER")
                            disableStrictForbiddenFileVerification(false)
                        }
                    }
                    triggerOnEvents {
                        patchsetCreated {
                           excludeDrafts(false)
                           excludeTrivialRebase(false)
                           excludeNoCodeChange(false)
                        }
                        changeMerged()
                        commentAddedContains {
                            commentAddedCommentContains('recheck')
                        }
                    }
                }

                definition {
                    cps {
                      script(readFileFromWorkspace("images/${entry.repo}/${image}/Jenkinsfile"))
                        sandbox()
                    }
                }
            }
        }
    }
}
