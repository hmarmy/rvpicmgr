find src -name "*.java" | xargs javac -d classes -encoding iso-8859-1 && jar cfm picDist2c.jar MANIFEST.MF -C classes/ .
cp picDist2c.jar ~
