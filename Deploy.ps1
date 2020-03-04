mvn clean
mvn package
docker rm githubproxy -f
docker build -t 117503445/githubproxy .
docker run --name githubproxy -d -p 8080:8080 117503445/githubproxy