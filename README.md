# Aquila Project - Official Repo

## Build / run on Ubuntu 22.04
'git clone https://github.com/AquilaNetwork/Aquila.git
'cd Aquila'
- Requires Java 11. OpenJDK 11 recommended over Java SE.
- 'sudo apt install default-jre'
- 'sudo apt install default-jdk'
- Install Maven
- 'sudo apt install maven'
- Use Maven to fetch dependencies and build: `mvn clean package`
- Built JAR should be something like `target/aquila-1.0.jar`
- Create basic *settings.json* file: `echo '{}' > settings.json`
- Run JAR in same working directory as *settings.json*: `java -jar target/aquila-1.0.jar`
- Wrap in shell script, add JVM flags, redirection, backgrounding, etc. as necessary.
- Or use supplied example shell script: *start.sh*
