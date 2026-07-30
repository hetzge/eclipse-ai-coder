mvn clean install -DskipTests -Dmaven.test.skip=true
rm -rf /home/hetzge/git/hetzge.github.io/aicoder/eclipse/beta1/*
cp -r ./site/target/repository/. /home/hetzge/git/hetzge.github.io/aicoder/eclipse/beta1/
cd /home/hetzge/git/hetzge.github.io/aicoder/eclipse/beta1
git add *
git commit -m "update ai coder"
git push