# ------------------------------------------------------------------------------------
# This script is to be executed after git checkout and before the 'mvn deploy'
# The purpose of the script is to prepare the release version name of the generated maven artefact.
# This is the release version "name" that is used when importing the generated maven artefact as a
# maven artefact dependency.
#
# The script do this using the following steps
# ------------------------------------------------------------------------------------
# 1. Create the release version name used to version the maven artefact.
# 2. Create a git branch using the release version name
# 3. Change the pom version of the parent and module poms to match the release version name
# ------------------------------------------------------------------------------------
echo "Azure token : ${azdevopsFeedToken}"
pom_version="$(mvn -Dmaven.repo.local=${MAVEN_CACHE_FOLDER} -DazureDevOpsToken=${azdevopsFeedToken} -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec|sed  's/-SNAPSHOT//g')"
date_str=$(date +"%Y.%m.%d.%H.%M.%S")
azure_str=az
project_build_version="${pom_version}.${azure_str}.${date_str}"
echo "${pom_version}"
echo "${date_str}"
echo "${project_build_version}"
git_user_email=$(git config user.email)
git_user_name=$(git config user.name)
git config user.email "docm.pipeline@maersk.com"
git config user.name " DOCM Azure DevOps"
git_version="azure.build/${project_build_version}"
git tag "${git_version}"
# git checkout -b "${git_version}"
mvn -DazureDevOpsToken=${azdevopsFeedToken} -q versions:set -DnewVersion="${project_build_version}"
# git commit -am "${project_build_version}"